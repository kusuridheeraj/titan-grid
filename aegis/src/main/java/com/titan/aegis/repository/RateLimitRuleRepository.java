package com.titan.aegis.repository;

import com.titan.aegis.annotation.RateLimit;
import com.titan.aegis.entity.RateLimitRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing rate_limit_rules database table.
 * Provides methods to query runtime-configurable rate limiting rules.
 * 
 * @author Titan Grid Team
 */
@Repository
public interface RateLimitRuleRepository extends JpaRepository<RateLimitRuleEntity, Long> {

    /**
     * Find all enabled rules ordered by priority (highest first).
     * Used for pattern matching against incoming requests.
     */
    List<RateLimitRuleEntity> findByEnabledTrueOrderByPriorityDescIdAsc();

    /**
     * Find a specific rule by exact endpoint pattern.
     */
    Optional<RateLimitRuleEntity> findByEndpointPatternAndEnabledTrue(String endpointPattern);

    /**
     * Find rules by client type.
     */
    List<RateLimitRuleEntity> findByClientTypeAndEnabledTrue(RateLimit.ClientType clientType);

    /**
     * Custom query to find matching patterns using LIKE.
     * Note: This is for simple patterns. Complex pattern matching done in service
     * layer.
     */
    @Query("SELECT r FROM RateLimitRuleEntity r WHERE r.enabled = true " +
            "AND :endpoint LIKE REPLACE(REPLACE(r.endpointPattern, '**', '%'), '*', '%') " +
            "ORDER BY r.priority DESC, r.id ASC")
    List<RateLimitRuleEntity> findMatchingPatterns(@Param("endpoint") String endpoint);

    /**
     * Count total enabled rules.
     */
    long countByEnabledTrue();
}
