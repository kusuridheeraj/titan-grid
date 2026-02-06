package com.titan.aegis.entity;

import com.titan.aegis.annotation.RateLimit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA Entity for rate_limit_rules table.
 * Stores runtime-configurable rate limiting rules (Priority 2 in hybrid
 * system).
 * 
 * @author Titan Grid Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rate_limit_rules", schema = "aegis")
public class RateLimitRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "endpoint_pattern", nullable = false, unique = true)
    private String endpointPattern;

    @Column(name = "limit_count", nullable = false)
    private Integer limitCount;

    @Column(name = "window_seconds", nullable = false)
    private Integer windowSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private RateLimit.ClientType clientType;

    @Column(name = "custom_key")
    private String customKey;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (enabled == null) {
            enabled = true;
        }
        if (priority == null) {
            priority = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
