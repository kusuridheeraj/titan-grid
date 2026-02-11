package com.titan.aegis.repository;

import com.titan.aegis.entity.RateLimitEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for rate_limit_events audit trail.
 * Provides methods for querying blocked requests and traffic patterns.
 * 
 * @author Titan Grid Team
 */
@Repository
public interface RateLimitEventRepository extends JpaRepository<RateLimitEventEntity, Long> {

    /**
     * Find blocked requests for a specific client.
     */
    List<RateLimitEventEntity> findByClientIdAndAllowedFalseOrderByEventTimestampDesc(String clientId);

    /**
     * Find blocked requests for a specific IP.
     */
    List<RateLimitEventEntity> findByIpAddressAndAllowedFalseOrderByEventTimestampDesc(String ipAddress);

    /**
     * Find events after a specific timestamp.
     */
    List<RateLimitEventEntity> findByEventTimestampAfterOrderByEventTimestampDesc(Instant timestamp);

    /**
     * Find blocked events with specific severity.
     */
    List<RateLimitEventEntity> findBySeverityAndEventTimestampAfterOrderByEventTimestampDesc(
            String severity, Instant timestamp);

    /**
     * Count blocked requests in a time window.
     */
    long countByAllowedFalseAndEventTimestampAfter(Instant timestamp);

    /**
     * Count total events in a time window.
     */
    long countByEventTimestampAfter(Instant timestamp);

    /**
     * Top blocked IPs in last N hours.
     */
    @Query(value = "SELECT e.ip_address, COUNT(*) as block_count " +
            "FROM aegis.rate_limit_events e " +
            "WHERE e.allowed = false AND e.event_timestamp > :since " +
            "GROUP BY e.ip_address " +
            "ORDER BY block_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopBlockedIps(@Param("since") Instant since, @Param("limit") int limit);

    /**
     * Top targeted endpoints in last N hours.
     */
    @Query(value = "SELECT e.endpoint, COUNT(*) as block_count " +
            "FROM aegis.rate_limit_events e " +
            "WHERE e.allowed = false AND e.event_timestamp > :since " +
            "GROUP BY e.endpoint " +
            "ORDER BY block_count DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopTargetedEndpoints(@Param("since") Instant since, @Param("limit") int limit);
}
