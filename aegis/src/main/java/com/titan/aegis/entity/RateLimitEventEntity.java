package com.titan.aegis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA Entity for rate_limit_events audit trail table.
 * Records every rate limit decision for analytics and security monitoring.
 * 
 * @author Titan Grid Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rate_limit_events", schema = "aegis")
public class RateLimitEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private Instant eventTimestamp;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "endpoint", nullable = false)
    private String endpoint;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "allowed", nullable = false)
    private Boolean allowed;

    @Column(name = "current_count", nullable = false)
    private Integer currentCount;

    @Column(name = "limit_threshold", nullable = false)
    private Integer limitThreshold;

    @Column(name = "rule_source", nullable = false)
    private String ruleSource;

    @Column(name = "severity")
    private String severity;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "request_headers", columnDefinition = "jsonb")
    private String requestHeaders;

    @Column(name = "decision_time_ms")
    private Double decisionTimeMs;

    @Column(name = "server_instance")
    private String serverInstance;

    @PrePersist
    protected void onCreate() {
        if (eventTimestamp == null) {
            eventTimestamp = Instant.now();
        }
        if (serverInstance == null) {
            serverInstance = "aegis-1";
        }
    }
}
