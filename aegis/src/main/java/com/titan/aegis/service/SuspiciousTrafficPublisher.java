package com.titan.aegis.service;

import com.titan.aegis.model.RateLimitDecision;
import com.titan.aegis.model.RateLimitRule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes suspicious traffic events to Redis Stream for real-time monitoring.
 * 
 * Architecture:
 * - Stream name: "suspicious_traffic"
 * - Consumer groups: security_monitor, analytics_processor, alert_manager
 * - Max stream length: 10,000 events (auto-trimmed)
 * - Events include severity classification and threat scoring
 * 
 * Severity Levels:
 * - LOW: 100-120% of limit (normal burst traffic)
 * - MEDIUM: 120-150% of limit (suspicious activity)
 * - HIGH: 150-300% of limit (likely attack)
 * - CRITICAL: >300% of limit (active attack)
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuspiciousTrafficPublisher {

    private static final String STREAM_KEY = "suspicious_traffic";
    private static final long MAX_STREAM_LENGTH = 10_000;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Publish a suspicious traffic event to Redis Stream.
     * Called asynchronously when a request is blocked.
     */
    @Async
    public void publishBlockedRequest(
            HttpServletRequest request,
            String clientId,
            RateLimitDecision decision,
            RateLimitRule rule,
            double decisionTimeMs) {
        try {
            String severity = calculateSeverity(decision.currentCount(), decision.limit());
            int threatScore = calculateThreatScore(decision.currentCount(), decision.limit(), severity);

            Map<String, String> eventData = new LinkedHashMap<>();
            eventData.put("event_id", java.util.UUID.randomUUID().toString());
            eventData.put("timestamp", Instant.now().toString());
            eventData.put("severity", severity);
            eventData.put("threat_score", String.valueOf(threatScore));
            eventData.put("client_id", clientId);
            eventData.put("ip_address", extractIpAddress(request));
            eventData.put("endpoint", request.getRequestURI());
            eventData.put("http_method", request.getMethod());
            eventData.put("violation_count", String.valueOf(decision.currentCount()));
            eventData.put("threshold", String.valueOf(decision.limit()));
            eventData.put("percentage_over", String.format("%.1f",
                    (double) decision.currentCount() / decision.limit() * 100));
            eventData.put("rule_source", rule.source().name());
            eventData.put("user_agent",
                    request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown");
            eventData.put("decision_time_ms", String.format("%.2f", decisionTimeMs));

            // Create stream record
            MapRecord<String, String, String> record = StreamRecords
                    .newRecord()
                    .ofMap(eventData)
                    .withStreamKey(STREAM_KEY);

            // Add to stream with auto-trimming
            stringRedisTemplate.opsForStream().add(record);

            // Trim stream to max length (approximate trimming for performance)
            stringRedisTemplate.opsForStream().trim(STREAM_KEY, MAX_STREAM_LENGTH);

            log.debug("Published suspicious traffic event: severity={}, client={}, endpoint={}, threat_score={}",
                    severity, clientId, request.getRequestURI(), threatScore);

        } catch (Exception e) {
            // Never let publishing failure affect the main request flow
            log.error("Failed to publish suspicious traffic event: {}", e.getMessage());
        }
    }

    /**
     * Calculate severity based on how much the limit was exceeded.
     */
    private String calculateSeverity(long currentCount, long limit) {
        if (limit <= 0)
            return "CRITICAL";

        double percentage = (double) currentCount / limit * 100.0;

        if (percentage > 300)
            return "CRITICAL";
        if (percentage > 150)
            return "HIGH";
        if (percentage > 120)
            return "MEDIUM";
        return "LOW";
    }

    /**
     * Calculate threat score (0-100) based on multiple factors.
     * 
     * Factors:
     * - How much the limit was exceeded (40% weight)
     * - Severity level (30% weight)
     * - Baseline score for being blocked (30% weight)
     */
    private int calculateThreatScore(long currentCount, long limit, String severity) {
        if (limit <= 0)
            return 100;

        // Factor 1: Excess percentage (40% weight)
        double excessRatio = Math.min((double) currentCount / limit, 5.0); // Cap at 5x
        int excessScore = (int) (excessRatio * 20); // 0-100 scaled to 0-40
        excessScore = Math.min(excessScore, 40);

        // Factor 2: Severity level (30% weight)
        int severityScore = switch (severity) {
            case "CRITICAL" -> 30;
            case "HIGH" -> 22;
            case "MEDIUM" -> 15;
            default -> 8;
        };

        // Factor 3: Baseline for being blocked (30% weight)
        int baselineScore = 30;

        return Math.min(excessScore + severityScore + baselineScore, 100);
    }

    /**
     * Extract IP address from request headers.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Initialize consumer groups for the suspicious_traffic stream.
     * Called on application startup.
     */
    public void initializeConsumerGroups() {
        try {
            String[] groups = { "security_monitor", "analytics_processor", "alert_manager" };

            for (String group : groups) {
                try {
                    stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, group);
                    log.info("Created consumer group '{}' on stream '{}'", group, STREAM_KEY);
                } catch (Exception e) {
                    // Group already exists, that's fine
                    log.debug("Consumer group '{}' already exists: {}", group, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to initialize consumer groups: {}", e.getMessage());
        }
    }

    /**
     * Get stream info for monitoring.
     */
    public Map<String, Object> getStreamInfo() {
        try {
            Long streamLength = stringRedisTemplate.opsForStream().size(STREAM_KEY);

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("streamName", STREAM_KEY);
            info.put("length", streamLength != null ? streamLength : 0);
            info.put("maxLength", MAX_STREAM_LENGTH);

            return info;
        } catch (Exception e) {
            log.error("Failed to get stream info: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}
