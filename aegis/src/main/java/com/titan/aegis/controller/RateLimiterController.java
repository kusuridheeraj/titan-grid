package com.titan.aegis.controller;

import com.titan.aegis.annotation.RateLimit;
import com.titan.aegis.config.RateLimiterProperties;
import com.titan.aegis.model.RateLimitDecision;
import com.titan.aegis.model.RequestToken;
import com.titan.aegis.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * REST Controller for testing rate limiter functionality.
 * 
 * Provides test endpoints with different rate limits and admin endpoints
 * for querying and managing rate limits.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;
    private final RateLimiterProperties properties;

    /**
     * Test endpoint with default rate limit (100 requests/min)
     */
    @GetMapping("/test/limited")
    public ResponseEntity<?> testLimited(HttpServletRequest request) {
        String clientId = getClientId(request);
        String endpoint = "/api/test/limited";

        RequestToken token = new RequestToken(
                clientId,
                endpoint,
                properties.getDefaultLimit(),
                properties.getWindowDurationAsDuration());

        RateLimitDecision decision = rateLimiterService.isAllowed(token);

        if (decision.allowed()) {
            return ResponseEntity.ok(Map.of(
                    "message", "Request allowed",
                    "remaining", decision.remaining(),
                    "limit", decision.limit(),
                    "resetTime", decision.resetTime()));
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("X-RateLimit-Limit", String.valueOf(decision.limit()))
                    .header("X-RateLimit-Remaining", "0")
                    .header("X-RateLimit-Reset", decision.resetTime().toString())
                    .header("Retry-After", String.valueOf(decision.retryAfter().getSeconds()))
                    .body(Map.of(
                            "error", "Rate limit exceeded",
                            "message", "Too many requests. Please try again later.",
                            "retryAfter", decision.retryAfter().getSeconds()));
        }
    }

    /**
     * Test endpoint with strict rate limit (10 requests/min)
     */
    @GetMapping("/test/strict")
    public ResponseEntity<?> testStrict(HttpServletRequest request) {
        String clientId = getClientId(request);
        String endpoint = "/api/test/strict";

        RequestToken token = new RequestToken(
                clientId,
                endpoint,
                properties.getStrictLimit(),
                properties.getWindowDurationAsDuration());

        RateLimitDecision decision = rateLimiterService.isAllowed(token);

        if (decision.allowed()) {
            return ResponseEntity.ok(Map.of(
                    "message", "Strict endpoint - request allowed",
                    "remaining", decision.remaining(),
                    "limit", decision.limit()));
        } else {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "error", "Rate limit exceeded",
                            "retryAfter", decision.retryAfter().getSeconds()));
        }
    }

    /**
     * Admin endpoint to get rate limit info for a client
     */
    @GetMapping("/admin/info")
    public ResponseEntity<?> getRateLimitInfo(
            @RequestParam String clientId,
            @RequestParam String endpoint,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "60") long windowSeconds) {
        RateLimitDecision info = rateLimiterService.getRateLimitInfo(
                clientId,
                endpoint,
                limit,
                Duration.ofSeconds(windowSeconds));

        return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "endpoint", endpoint,
                "allowed", info.allowed(),
                "currentCount", info.currentCount(),
                "limit", info.limit(),
                "remaining", info.remaining(),
                "resetTime", info.resetTime(),
                "retryAfter", info.retryAfter() != null ? info.retryAfter().getSeconds() : 0));
    }

    /**
     * Admin endpoint to reset rate limit for a client
     */
    @PostMapping("/admin/reset")
    public ResponseEntity<?> resetRateLimit(
            @RequestParam String clientId,
            @RequestParam String endpoint) {
        rateLimiterService.reset(clientId, endpoint);

        return ResponseEntity.ok(Map.of(
                "message", "Rate limit reset successfully",
                "clientId", clientId,
                "endpoint", endpoint));
    }

    /**
     * Extract client ID from request (IP address for now)
     * In production, this would check API keys, user IDs, etc.
     */
    private String getClientId(HttpServletRequest request) {
        // Check for X-Forwarded-For header (reverse proxy)
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }

        // Fall back to remote address
        return request.getRemoteAddr();
    }
}
