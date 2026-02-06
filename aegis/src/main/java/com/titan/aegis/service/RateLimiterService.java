package com.titan.aegis.service;

import com.titan.aegis.config.RateLimiterProperties;
import com.titan.aegis.model.RateLimitDecision;
import com.titan.aegis.model.RequestToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Core Rate Limiter Service.
 * 
 * Implements sliding window rate limiting algorithm using Redis and Lua
 * scripts.
 * All operations are atomic to prevent race conditions in distributed systems.
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> slidingWindowScript;
    private final RateLimiterProperties properties;

    /**
     * Check if a request is allowed based on rate limit.
     * 
     * @param token Request token containing client ID, endpoint, limit, and window
     * @return RateLimitDecision with allow/deny status and metadata
     */
    public RateLimitDecision isAllowed(RequestToken token) {
        try {
            // Generate Redis key
            String redisKey = token.toRedisKey(properties.getKeyPrefix());

            // Current timestamp in seconds
            long currentTime = Instant.now().getEpochSecond();

            // Window duration in seconds
            long windowSeconds = token.window().getSeconds();

            // Generate unique request ID
            String requestId = UUID.randomUUID().toString();

            // Execute Lua script atomically
            List<Object> result = redisTemplate.execute(
                    slidingWindowScript,
                    Collections.singletonList(redisKey),
                    currentTime,
                    windowSeconds,
                    token.limit(),
                    requestId);

            if (result == null || result.size() < 3) {
                log.error("Unexpected result from Lua script: {}", result);
                return handleRedisFailure(token);
            }

            // Parse Lua script result
            boolean allowed = ((Number) result.get(0)).intValue() == 1;
            long currentCount = ((Number) result.get(1)).longValue();
            long ttlSeconds = ((Number) result.get(2)).longValue();

            Instant resetTime = Instant.now().plusSeconds(ttlSeconds);
            Duration retryAfter = allowed ? null : Duration.ofSeconds(ttlSeconds);

            log.debug("Rate limit check - Key: {}, Allowed: {}, Count: {}/{}, Reset: {}",
                    redisKey, allowed, currentCount, token.limit(), resetTime);

            return allowed
                    ? RateLimitDecision.allowed(currentCount, token.limit(), resetTime)
                    : RateLimitDecision.denied(currentCount, token.limit(), resetTime, retryAfter);

        } catch (Exception e) {
            log.error("Error checking rate limit for token: {}", token, e);
            return handleRedisFailure(token);
        }
    }

    /**
     * Handle Redis connection failures based on configured failure mode.
     * 
     * @param token Request token
     * @return RateLimitDecision based on failure mode
     */
    private RateLimitDecision handleRedisFailure(RequestToken token) {
        boolean allow = properties.getFailureMode() == RateLimiterProperties.FailureMode.ALLOW;

        log.warn("Redis unavailable, failure mode: {}, allowing request: {}",
                properties.getFailureMode(), allow);

        Instant resetTime = Instant.now().plus(token.window());

        return allow
                ? RateLimitDecision.allowed(0, token.limit(), resetTime)
                : RateLimitDecision.denied(token.limit(), token.limit(), resetTime, token.window());
    }

    /**
     * Get current rate limit information for a client/endpoint.
     * Does not modify the count.
     * 
     * @param clientId Client identifier
     * @param endpoint API endpoint
     * @param limit    Rate limit
     * @return Current rate limit info
     */
    public RateLimitDecision getRateLimitInfo(String clientId, String endpoint, int limit, Duration window) {
        try {
            String redisKey = new RequestToken(clientId, endpoint, limit, window)
                    .toRedisKey(properties.getKeyPrefix());

            // Get current count without modifying
            Long count = redisTemplate.opsForZSet().zCard(redisKey);
            long currentCount = count != null ? count : 0;

            // Get TTL
            Long ttl = redisTemplate.getExpire(redisKey);
            long ttlSeconds = ttl != null && ttl > 0 ? ttl : window.getSeconds();

            Instant resetTime = Instant.now().plusSeconds(ttlSeconds);

            boolean allowed = currentCount < limit;
            Duration retryAfter = allowed ? null : Duration.ofSeconds(ttlSeconds);

            return allowed
                    ? RateLimitDecision.allowed(currentCount, limit, resetTime)
                    : RateLimitDecision.denied(currentCount, limit, resetTime, retryAfter);

        } catch (Exception e) {
            log.error("Error getting rate limit info", e);
            return handleRedisFailure(new RequestToken(clientId, endpoint, limit, window));
        }
    }

    /**
     * Reset rate limit for a specific client/endpoint.
     * Used for testing or manual intervention.
     * 
     * @param clientId Client identifier
     * @param endpoint API endpoint
     */
    public void reset(String clientId, String endpoint) {
        try {
            String redisKey = new RequestToken(clientId, endpoint, 0, Duration.ZERO)
                    .toRedisKey(properties.getKeyPrefix());

            redisTemplate.delete(redisKey);
            log.info("Reset rate limit for key: {}", redisKey);

        } catch (Exception e) {
            log.error("Error resetting rate limit", e);
        }
    }
}
