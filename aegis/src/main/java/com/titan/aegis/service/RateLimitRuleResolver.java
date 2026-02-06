package com.titan.aegis.service;

import com.titan.aegis.annotation.RateLimit;
import com.titan.aegis.config.RateLimiterProperties;
import com.titan.aegis.entity.RateLimitRuleEntity;
import com.titan.aegis.model.RateLimitRule;
import com.titan.aegis.repository.RateLimitRuleRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Optional;

/**
 * Resolves rate limit rules using 3-tier priority system:
 * 1. @RateLimit annotation (highest priority)
 * 2. Database rules (runtime configurable)
 * 3. YAML configuration (default fallback)
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitRuleResolver {

    private final RateLimitRuleRepository ruleRepository;
    private final RateLimiterProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Resolve rate limit rule for a request with priority system.
     * 
     * @param request       HTTP request
     * @param handlerMethod Controller method (may be null for non-controller
     *                      requests)
     * @return Resolved rate limit rule
     */
    public RateLimitRule resolveRule(HttpServletRequest request, HandlerMethod handlerMethod) {
        String endpoint = request.getRequestURI();

        // Priority 1: Check @RateLimit annotation on method or class
        if (handlerMethod != null) {
            RateLimitRule annotationRule = checkAnnotation(handlerMethod);
            if (annotationRule != null) {
                log.debug("Using @RateLimit annotation for: {} (limit: {}, source: ANNOTATION)",
                        endpoint, annotationRule.limit());
                return annotationRule;
            }
        }

        // Priority 2: Check database rules
        Optional<RateLimitRule> databaseRule = checkDatabaseRules(endpoint);
        if (databaseRule.isPresent()) {
            log.debug("Using database rule for: {} (limit: {}, source: DATABASE)",
                    endpoint, databaseRule.get().limit());
            return databaseRule.get();
        }

        // Priority 3: Fall back to YAML configuration
        RateLimitRule yamlRule = getDefaultRule();
        log.debug("Using YAML default for: {} (limit: {}, source: YAML)",
                endpoint, yamlRule.limit());
        return yamlRule;
    }

    /**
     * Check for @RateLimit annotation on method or class level.
     * Method-level annotation takes precedence over class-level.
     */
    private RateLimitRule checkAnnotation(HandlerMethod handlerMethod) {
        // Check method-level annotation first
        RateLimit methodAnnotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (methodAnnotation != null && methodAnnotation.enabled()) {
            return RateLimitRule.fromAnnotation(methodAnnotation);
        }

        // Check class-level annotation
        RateLimit classAnnotation = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        if (classAnnotation != null && classAnnotation.enabled()) {
            return RateLimitRule.fromAnnotation(classAnnotation);
        }

        return null;
    }

    /**
     * Check database for matching rate limit rules.
     * Cached for 60 seconds to reduce DB queries.
     */
    @Cacheable(value = "rateLimitRules", key = "#endpoint", unless = "#result.isEmpty()")
    private Optional<RateLimitRule> checkDatabaseRules(String endpoint) {
        try {
            // Get all enabled rules ordered by priority
            List<RateLimitRuleEntity> allRules = ruleRepository.findByEnabledTrueOrderByPriorityDescIdAsc();

            // Find first matching pattern (highest priority)
            for (RateLimitRuleEntity rule : allRules) {
                if (pathMatcher.match(rule.getEndpointPattern(), endpoint)) {
                    log.debug("Matched database pattern '{}' for endpoint '{}'",
                            rule.getEndpointPattern(), endpoint);
                    return Optional.of(RateLimitRule.fromEntity(rule));
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            log.warn("Error checking database rules for endpoint: {}", endpoint, e);
            return Optional.empty();
        }
    }

    /**
     * Get default rate limit rule from YAML configuration.
     */
    private RateLimitRule getDefaultRule() {
        return RateLimitRule.fromYaml(
                properties.getDefaultLimit(),
                properties.getWindowDurationAsDuration(),
                RateLimit.ClientType.IP);
    }

    /**
     * Check if endpoint should be excluded from rate limiting.
     * Useful for health checks, actuator endpoints, etc.
     */
    public boolean shouldExclude(String endpoint) {
        // Exclude Spring Actuator endpoints
        if (endpoint.startsWith("/actuator/")) {
            return true;
        }

        // Exclude Swagger/API docs
        if (endpoint.startsWith("/swagger-ui/") || endpoint.startsWith("/v3/api-docs/")) {
            return true;
        }

        // Exclude static resources
        if (endpoint.startsWith("/static/") || endpoint.startsWith("/public/")) {
            return true;
        }

        return false;
    }
}
