package com.titan.aegis.model;

import java.time.Duration;

/**
 * Immutable record representing a rate limit request token.
 * Identifies a unique rate limit key based on client and endpoint.
 * 
 * @param clientId Client identifier (IP address, API key, user ID)
 * @param endpoint API endpoint being accessed
 * @param limit Maximum requests allowed in the window
 * @param window Time window duration
 */
public record RequestToken(
    String clientId,
    String endpoint,
    int limit,
    Duration window
) {
    
    /**
     * Generates the Redis key for this rate limit entry.
     * Format: rate_limit:{clientId}:{endpoint}
     */
    public String toRedisKey(String keyPrefix) {
        return String.format("%s:%s:%s", keyPrefix, clientId, endpoint);
    }
    
    /**
     * Creates a request token with default window (60 seconds).
     */
    public static RequestToken of(String clientId, String endpoint, int limit) {
        return new RequestToken(clientId, endpoint, limit, Duration.ofSeconds(60));
    }
}
