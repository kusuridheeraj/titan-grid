package com.titan.aegis.model;

import com.titan.aegis.annotation.RateLimit;
import com.titan.aegis.entity.RateLimitRuleEntity;
import lombok.Builder;

import java.time.Duration;

/**
 * Unified rate limit rule model.
 * Can be created from annotations, database entities, or YAML config.
 * 
 * @param limit      Maximum requests allowed
 * @param window     Time window duration
 * @param clientType How to identify clients
 * @param customKey  Custom key expression (for CUSTOM client type)
 * @param source     Source of this rule (ANNOTATION, DATABASE, or YAML)
 * @param priority   Rule priority (higher = more important)
 * 
 * @author Titan Grid Team
 */
@Builder
public record RateLimitRule(
        int limit,
        Duration window,
        RateLimit.ClientType clientType,
        String customKey,
        RuleSource source,
        int priority) {

    /**
     * Create rule from @RateLimit annotation.
     * These have the highest priority (1000+).
     */
    public static RateLimitRule fromAnnotation(RateLimit annotation) {
        return RateLimitRule.builder()
                .limit(annotation.limit())
                .window(Duration.ofSeconds(annotation.windowSeconds()))
                .clientType(annotation.clientType())
                .customKey(annotation.customKey())
                .source(RuleSource.ANNOTATION)
                .priority(1000) // Highest priority
                .build();
    }

    /**
     * Create rule from database entity.
     * These have medium priority (from DB).
     */
    public static RateLimitRule fromEntity(RateLimitRuleEntity entity) {
        return RateLimitRule.builder()
                .limit(entity.getLimitCount())
                .window(Duration.ofSeconds(entity.getWindowSeconds()))
                .clientType(entity.getClientType())
                .customKey(entity.getCustomKey())
                .source(RuleSource.DATABASE)
                .priority(entity.getPriority()) // From database
                .build();
    }

    /**
     * Create rule from YAML configuration.
     * These have the lowest priority (0).
     */
    public static RateLimitRule fromYaml(int limit, Duration window, RateLimit.ClientType clientType) {
        return RateLimitRule.builder()
                .limit(limit)
                .window(window)
                .clientType(clientType != null ? clientType : RateLimit.ClientType.IP)
                .customKey("")
                .source(RuleSource.YAML)
                .priority(0) // Lowest priority
                .build();
    }

    /**
     * Source of rate limit rule.
     */
    public enum RuleSource {
        ANNOTATION, // From @RateLimit annotation (Priority 1)
        DATABASE, // From PostgreSQL (Priority 2)
        YAML // From application.yml (Priority 3)
    }
}
