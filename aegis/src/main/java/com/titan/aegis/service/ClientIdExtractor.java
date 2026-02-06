package com.titan.aegis.service;

import com.titan.aegis.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Extracts client identifier from HTTP requests based on ClientType.
 * 
 * Extraction priority for IP type:
 * 1. X-Forwarded-For header (reverse proxy)
 * 2. X-Real-IP header
 * 3. request.getRemoteAddr()
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Service
public class ClientIdExtractor {

    /**
     * Extract client ID based on the specified client type.
     * 
     * @param request    HTTP request
     * @param clientType Type of client identification
     * @param customKey  Custom key expression (for CUSTOM type)
     * @return Client identifier string
     */
    public String extractClientId(HttpServletRequest request, RateLimit.ClientType clientType, String customKey) {
        return switch (clientType) {
            case API_KEY -> extractFromApiKey(request);
            case USER_ID -> extractFromAuth(request);
            case CUSTOM -> extractCustom(request, customKey);
            default -> extractFromIp(request);
        };
    }

    /**
     * Extract client ID from X-API-Key header.
     * Returns "apikey:{key}" or falls back to IP if not present.
     */
    private String extractFromApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");

        if (apiKey != null && !apiKey.isBlank()) {
            log.debug("Extracted API key: {}", maskApiKey(apiKey));
            return "apikey:" + apiKey;
        }

        // Fall back to IP if no API key
        log.debug("No X-API-Key header, falling back to IP");
        return extractFromIp(request);
    }

    /**
     * Extract user ID from Authorization header (JWT).
     * Returns "user:{userId}" or falls back to IP if not authenticated.
     */
    private String extractFromAuth(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userId = extractUserIdFromJwt(token);

            if (userId != null) {
                log.debug("Extracted user ID from JWT: {}", userId);
                return "user:" + userId;
            }
        }

        // Fall back to IP if not authenticated
        log.debug("No valid JWT, falling back to IP");
        return extractFromIp(request);
    }

    /**
     * Extract IP address from request headers or remote address.
     * Checks X-Forwarded-For for proxy scenarios.
     */
    private String extractFromIp(HttpServletRequest request) {
        // Check X-Forwarded-For (reverse proxy, load balancer)
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Take first IP if multiple proxies
            String clientIp = forwardedFor.split(",")[0].trim();
            log.debug("Extracted IP from X-Forwarded-For: {}", clientIp);
            return "ip:" + clientIp;
        }

        // Check X-Real-IP (some proxies use this)
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            log.debug("Extracted IP from X-Real-IP: {}", realIp);
            return "ip:" + realIp;
        }

        // Fall back to remote address
        String remoteAddr = request.getRemoteAddr();
        log.debug("Extracted IP from remote address: {}", remoteAddr);
        return "ip:" + remoteAddr;
    }

    /**
     * Extract custom identifier using custom key expression.
     * For now, just extracts from header. In future, can support SpEL.
     */
    private String extractCustom(HttpServletRequest request, String customKey) {
        if (customKey == null || customKey.isBlank()) {
            log.warn("Custom client type but no customKey specified, falling back to IP");
            return extractFromIp(request);
        }

        // Simple implementation: treat customKey as header name
        String value = request.getHeader(customKey);
        if (value != null && !value.isBlank()) {
            log.debug("Extracted custom ID from header '{}': {}", customKey, value);
            return "custom:" + value;
        }

        log.warn("Custom header '{}' not found, falling back to IP", customKey);
        return extractFromIp(request);
    }

    /**
     * Extract user ID from JWT token.
     * Simple implementation - in production, use proper JWT library.
     */
    private String extractUserIdFromJwt(String token) {
        try {
            // TODO: Implement proper JWT parsing (Day 10+)
            // For now, just return a placeholder
            // In production, decode JWT and extract 'sub' or 'user_id' claim

            // Simple mock: if token contains "user_", extract after it
            if (token.contains("user_")) {
                int start = token.indexOf("user_") + 5;
                int end = Math.min(start + 10, token.length());
                return token.substring(start, end);
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to extract user ID from JWT", e);
            return null;
        }
    }

    /**
     * Mask API key for logging (show first 4 and last 4 characters).
     */
    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
