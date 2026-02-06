package com.titan.aegis.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Immutable record representing a rate limit decision.
 * 
 * @param allowed Whether the request is allowed
 * @param currentCount Current number of requests in the window
 * @param limit Maximum requests allowed in the window
 * @param resetTime Time when the rate limit window resets
 * @param retryAfter Duration to wait before retrying (null if allowed)
 */
public record RateLimitDecision(
    boolean allowed,
    long currentCount,
    long limit,
    Instant resetTime,
    Duration retryAfter
) {
    
    /**
     * Creates a decision for an allowed request.
     */
    public static RateLimitDecision allowed(long currentCount, long limit, Instant resetTime) {
        return new RateLimitDecision(true, currentCount, limit, resetTime, null);
    }
    
    /**
     * Creates a decision for a denied (rate limited) request.
     */
    public static RateLimitDecision denied(long currentCount, long limit, Instant resetTime, Duration retryAfter) {
        return new RateLimitDecision(false, currentCount, limit, resetTime, retryAfter);
    }
    
    /**
     * Returns remaining requests in the current window.
     */
    public long remaining() {
        return Math.max(0, limit - currentCount);
    }
}
