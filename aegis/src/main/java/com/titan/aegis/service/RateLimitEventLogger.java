package com.titan.aegis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.titan.aegis.entity.RateLimitEventEntity;
import com.titan.aegis.model.RateLimitDecision;
import com.titan.aegis.model.RateLimitRule;
import com.titan.aegis.repository.RateLimitEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Async event logger for rate limit decisions.
 * Logs blocked requests (and optionally allowed) to PostgreSQL for audit trail
 * and analytics.
 * 
 * Key design decisions:
 * - Async logging: Non-blocking, doesn't affect request latency
 * - Batch-ready: Individual inserts for now, can switch to batch with
 * Spring @Scheduled
 * - Severity classification: Calculates severity based on how much the limit
 * was exceeded
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitEventLogger {

    private final RateLimitEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log a blocked request asynchronously.
     * This is the primary logging method called from AegisFilter when a request is
     * denied.
     */
    @Async
    public void logBlockedRequest(
            HttpServletRequest request,
            String clientId,
            RateLimitDecision decision,
            RateLimitRule rule,
            double decisionTimeMs) {
        try {
            String severity = calculateSeverity(decision.currentCount(), decision.limit());

            RateLimitEventEntity event = RateLimitEventEntity.builder()
                    .eventTimestamp(Instant.now())
                    .clientId(clientId)
                    .ipAddress(extractIpAddress(request))
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .allowed(false)
                    .currentCount((int) decision.currentCount())
                    .limitThreshold((int) decision.limit())
                    .ruleSource(rule.source().name())
                    .severity(severity)
                    .userAgent(request.getHeader("User-Agent"))
                    .requestHeaders(extractSafeHeaders(request))
                    .decisionTimeMs(decisionTimeMs)
                    .build();

            eventRepository.save(event);

            log.debug("Logged blocked event: client={}, endpoint={}, severity={}",
                    clientId, request.getRequestURI(), severity);

        } catch (Exception e) {
            // Never let logging failure affect the main request flow
            log.error("Failed to log blocked request for client={}: {}", clientId, e.getMessage());
        }
    }

    /**
     * Log an allowed request (optional, for full audit trail).
     * Only called when verbose logging is enabled.
     */
    @Async
    public void logAllowedRequest(
            HttpServletRequest request,
            String clientId,
            RateLimitDecision decision,
            RateLimitRule rule,
            double decisionTimeMs) {
        try {
            RateLimitEventEntity event = RateLimitEventEntity.builder()
                    .eventTimestamp(Instant.now())
                    .clientId(clientId)
                    .ipAddress(extractIpAddress(request))
                    .endpoint(request.getRequestURI())
                    .httpMethod(request.getMethod())
                    .allowed(true)
                    .currentCount((int) decision.currentCount())
                    .limitThreshold((int) decision.limit())
                    .ruleSource(rule.source().name())
                    .severity(null)
                    .userAgent(request.getHeader("User-Agent"))
                    .decisionTimeMs(decisionTimeMs)
                    .build();

            eventRepository.save(event);

        } catch (Exception e) {
            log.error("Failed to log allowed request: {}", e.getMessage());
        }
    }

    /**
     * Calculate severity based on how much the limit was exceeded.
     * 
     * LOW: 100-120% of limit (normal burst traffic)
     * MEDIUM: 120-150% of limit (suspicious activity)
     * HIGH: 150-300% of limit (likely attack or misconfigured client)
     * CRITICAL: >300% of limit or rapid repeated violations (active attack)
     */
    public String calculateSeverity(long currentCount, long limit) {
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
     * Extract IP address from request, checking proxy headers.
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
     * Extract safe headers for logging (exclude sensitive ones).
     */
    private String extractSafeHeaders(HttpServletRequest request) {
        try {
            Map<String, String> headers = new LinkedHashMap<>();

            // Only log safe, non-sensitive headers
            String[] safeHeaders = { "User-Agent", "Accept", "Content-Type", "Origin",
                    "Referer", "X-Forwarded-For", "X-Real-IP", "X-API-Key" };

            for (String headerName : safeHeaders) {
                String value = request.getHeader(headerName);
                if (value != null) {
                    // Mask API key for security
                    if ("X-API-Key".equals(headerName) && value.length() > 8) {
                        value = value.substring(0, 4) + "****" + value.substring(value.length() - 4);
                    }
                    headers.put(headerName, value);
                }
            }

            return objectMapper.writeValueAsString(headers);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize headers: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Get analytics summary.
     */
    public Map<String, Object> getAnalyticsSummary(int hoursBack) {
        Instant since = Instant.now().minusSeconds(hoursBack * 3600L);

        long totalEvents = eventRepository.countByEventTimestampAfter(since);
        long blockedEvents = eventRepository.countByAllowedFalseAndEventTimestampAfter(since);
        List<Object[]> topBlockedIps = eventRepository.findTopBlockedIps(since, 10);
        List<Object[]> topEndpoints = eventRepository.findTopTargetedEndpoints(since, 10);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("timeRangeHours", hoursBack);
        summary.put("totalEvents", totalEvents);
        summary.put("blockedEvents", blockedEvents);
        summary.put("allowedEvents", totalEvents - blockedEvents);
        summary.put("blockRate",
                totalEvents > 0 ? String.format("%.2f%%", (double) blockedEvents / totalEvents * 100) : "0%");

        // Convert top IPs to maps
        List<Map<String, Object>> topIps = new ArrayList<>();
        for (Object[] row : topBlockedIps) {
            topIps.add(Map.of("ip", row[0].toString(), "count", ((Number) row[1]).longValue()));
        }
        summary.put("topBlockedIps", topIps);

        // Convert top endpoints to maps
        List<Map<String, Object>> topEps = new ArrayList<>();
        for (Object[] row : topEndpoints) {
            topEps.add(Map.of("endpoint", row[0].toString(), "count", ((Number) row[1]).longValue()));
        }
        summary.put("topTargetedEndpoints", topEps);

        return summary;
    }
}
