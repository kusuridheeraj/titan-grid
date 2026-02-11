package com.titan.aegis.controller;

import com.titan.aegis.repository.RateLimitEventRepository;
import com.titan.aegis.service.RateLimitEventLogger;
import com.titan.aegis.service.SuspiciousTrafficPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin analytics controller for monitoring rate limiter activity.
 * Provides real-time analytics, suspicious traffic info, and event queries.
 * 
 * @author Titan Grid Team
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics and reporting endpoints")
public class AnalyticsController {

    private final RateLimitEventLogger eventLogger;
    private final SuspiciousTrafficPublisher trafficPublisher;
    private final RateLimitEventRepository eventRepository;

    /**
     * Get analytics summary for the last N hours.
     */
    @Operation(summary = "Get analytics summary", description = "Returns rate limiting analytics including blocked IPs, targeted endpoints, and traffic stats")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics summary retrieved successfully")
    })
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary(
            @Parameter(description = "Number of hours to look back", example = "24") @RequestParam(defaultValue = "24") int hours) {
        log.info("Fetching analytics summary for last {} hours", hours);

        Map<String, Object> summary = eventLogger.getAnalyticsSummary(hours);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get Redis Stream info for suspicious traffic monitoring.
     */
    @Operation(summary = "Get suspicious traffic stream info", description = "Returns Redis Stream metadata including length and consumer groups")
    @GetMapping("/stream")
    public ResponseEntity<Map<String, Object>> getStreamInfo() {
        Map<String, Object> streamInfo = trafficPublisher.getStreamInfo();
        return ResponseEntity.ok(streamInfo);
    }

    /**
     * Get blocked events for a specific IP address.
     */
    @Operation(summary = "Get blocked events by IP", description = "Returns all blocked events for a specific IP address")
    @GetMapping("/blocked/ip/{ipAddress}")
    public ResponseEntity<?> getBlockedByIp(
            @Parameter(description = "IP address to query", example = "192.168.1.100") @PathVariable String ipAddress) {
        var events = eventRepository.findByIpAddressAndAllowedFalseOrderByEventTimestampDesc(ipAddress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ipAddress", ipAddress);
        result.put("totalBlocked", events.size());
        result.put("events", events);

        return ResponseEntity.ok(result);
    }

    /**
     * Get blocked events for a specific client ID.
     */
    @Operation(summary = "Get blocked events by client ID", description = "Returns all blocked events for a specific client identifier")
    @GetMapping("/blocked/client/{clientId}")
    public ResponseEntity<?> getBlockedByClient(
            @Parameter(description = "Client identifier", example = "apikey:abc123") @PathVariable String clientId) {
        var events = eventRepository.findByClientIdAndAllowedFalseOrderByEventTimestampDesc(clientId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clientId", clientId);
        result.put("totalBlocked", events.size());
        result.put("events", events);

        return ResponseEntity.ok(result);
    }

    /**
     * Get events by severity level.
     */
    @Operation(summary = "Get events by severity", description = "Returns blocked events filtered by severity level (LOW, MEDIUM, HIGH, CRITICAL)")
    @GetMapping("/severity/{severity}")
    public ResponseEntity<?> getEventsBySeverity(
            @Parameter(description = "Severity level", example = "HIGH") @PathVariable String severity,
            @Parameter(description = "Hours to look back", example = "24") @RequestParam(defaultValue = "24") int hours) {
        Instant since = Instant.now().minusSeconds(hours * 3600L);
        var events = eventRepository.findBySeverityAndEventTimestampAfterOrderByEventTimestampDesc(
                severity.toUpperCase(), since);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("severity", severity.toUpperCase());
        result.put("timeRangeHours", hours);
        result.put("totalEvents", events.size());
        result.put("events", events);

        return ResponseEntity.ok(result);
    }

    /**
     * Get overall system health and stats.
     */
    @Operation(summary = "Get system health dashboard", description = "Returns overall system health including total events, block rate, and recent activity")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthDashboard() {
        Instant last1h = Instant.now().minusSeconds(3600);
        Instant last24h = Instant.now().minusSeconds(86400);

        long total1h = eventRepository.countByEventTimestampAfter(last1h);
        long blocked1h = eventRepository.countByAllowedFalseAndEventTimestampAfter(last1h);
        long total24h = eventRepository.countByEventTimestampAfter(last24h);
        long blocked24h = eventRepository.countByAllowedFalseAndEventTimestampAfter(last24h);

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "OPERATIONAL");
        health.put("timestamp", Instant.now().toString());
        health.put("last1Hour", Map.of(
                "totalRequests", total1h,
                "blockedRequests", blocked1h,
                "blockRate", total1h > 0 ? String.format("%.2f%%", (double) blocked1h / total1h * 100) : "0%"));
        health.put("last24Hours", Map.of(
                "totalRequests", total24h,
                "blockedRequests", blocked24h,
                "blockRate", total24h > 0 ? String.format("%.2f%%", (double) blocked24h / total24h * 100) : "0%"));
        health.put("streamInfo", trafficPublisher.getStreamInfo());

        return ResponseEntity.ok(health);
    }
}
