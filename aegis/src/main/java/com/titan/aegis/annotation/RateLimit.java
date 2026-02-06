package com.titan.aegis.annotation;

import java.lang.annotation.*;

/**
 * Rate limiting annotation for controller methods and classes.
 * 
 * This annotation has the HIGHEST PRIORITY in the hybrid rate limiting system.
 * When present, it overrides database rules and YAML configuration.
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 * &#64;GetMapping("/api/critical")
 * @RateLimit(limit = 5, windowSeconds = 60, clientType = ClientType.API_KEY)
 * public String criticalEndpoint() {
 *     return "Protected resource";
 * }
 * }
 * </pre>
 * 
 * @author Titan Grid Team
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Maximum number of requests allowed in the time window.
     */
    int limit();

    /**
     * Time window duration in seconds.
     * Default: 60 seconds (1 minute)
     */
    int windowSeconds() default 60;

    /**
     * Client identification type for rate limiting.
     * Default: IP address
     */
    ClientType clientType() default ClientType.IP;

    /**
     * Custom key expression for CUSTOM client type.
     * Supports SpEL expressions like: "#request.getHeader('X-Custom-Id')"
     */
    String customKey() default "";

    /**
     * Enable or disable this rate limit.
     * Useful for temporarily disabling without removing annotation.
     */
    boolean enabled() default true;

    /**
     * Client identification types for rate limiting.
     */
    enum ClientType {
        /**
         * Rate limit by IP address (X-Forwarded-For or remote address)
         */
        IP,

        /**
         * Rate limit by API key from X-API-Key header
         */
        API_KEY,

        /**
         * Rate limit by authenticated user ID (from JWT or session)
         */
        USER_ID,

        /**
         * Rate limit by custom expression (uses customKey field)
         */
        CUSTOM
    }
}
