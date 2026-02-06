package com.titan.aegis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration properties for Aegis Rate Limiter.
 * Binds to 'aegis.rate-limiter' properties from application.yml.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aegis.rate-limiter")
public class RateLimiterProperties {

    /**
     * Default rate limit (requests per window)
     */
    private int defaultLimit = 100;

    /**
     * Strict rate limit for sensitive endpoints
     */
    private int strictLimit = 10;

    /**
     * Window duration in seconds
     */
    private long windowDuration = 60;

    /**
     * Redis key prefix for rate limit entries
     */
    private String keyPrefix = "rate_limit";

    /**
     * Failure mode: ALLOW or DENY when Redis is unavailable
     */
    private FailureMode failureMode = FailureMode.ALLOW;

    /**
     * Get window duration as Duration object
     */
    public Duration getWindowDurationAsDuration() {
        return Duration.ofSeconds(windowDuration);
    }

    /**
     * Failure mode enum
     */
    public enum FailureMode {
        ALLOW, // Allow requests when Redis is down (fail open)
        DENY // Deny requests when Redis is down (fail closed)
    }
}
