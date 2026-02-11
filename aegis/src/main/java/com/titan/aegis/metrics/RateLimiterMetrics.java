package com.titan.aegis.metrics;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom Prometheus metrics for the Aegis rate limiter.
 * 
 * Metrics exposed:
 * - aegis_requests_total: Counter of all rate limit checks
 * - aegis_allowed_total: Counter of allowed requests
 * - aegis_blocked_total: Counter of blocked requests (with severity tag)
 * - aegis_decision_duration_seconds: Histogram of decision latency
 * - aegis_active_clients: Gauge of unique active clients
 * - aegis_rule_evaluations_total: Counter of rule evaluations by source
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Component
public class RateLimiterMetrics {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter totalRequests;
    private final Counter allowedRequests;

    // Timer for decision latency
    private final Timer decisionTimer;

    public RateLimiterMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.totalRequests = Counter.builder("aegis_requests_total")
                .description("Total number of rate limit checks performed")
                .register(meterRegistry);

        this.allowedRequests = Counter.builder("aegis_allowed_total")
                .description("Total number of allowed requests")
                .register(meterRegistry);

        this.decisionTimer = Timer.builder("aegis_decision_duration_seconds")
                .description("Time taken to make rate limit decision")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        log.info("Initialized Aegis rate limiter metrics");
    }

    /**
     * Record a rate limit check.
     */
    public void recordRequest(String endpoint, String clientType, String ruleSource, boolean allowed) {
        totalRequests.increment();

        if (allowed) {
            allowedRequests.increment();

            Counter.builder("aegis_allowed_requests")
                    .description("Allowed requests by endpoint and source")
                    .tag("endpoint", normalizeEndpoint(endpoint))
                    .tag("client_type", clientType)
                    .tag("rule_source", ruleSource)
                    .register(meterRegistry)
                    .increment();
        }
    }

    /**
     * Record a blocked request with severity.
     */
    public void recordBlocked(String endpoint, String clientType, String ruleSource, String severity) {
        Counter.builder("aegis_blocked_total")
                .description("Total number of blocked requests")
                .tag("endpoint", normalizeEndpoint(endpoint))
                .tag("client_type", clientType)
                .tag("rule_source", ruleSource)
                .tag("severity", severity)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record decision time.
     */
    public void recordDecisionTime(double milliseconds, String ruleSource) {
        decisionTimer.record((long) (milliseconds * 1_000_000), TimeUnit.NANOSECONDS);

        Timer.builder("aegis_decision_duration_by_source")
                .description("Decision duration by rule source")
                .tag("rule_source", ruleSource)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record((long) (milliseconds * 1_000_000), TimeUnit.NANOSECONDS);
    }

    /**
     * Record rule evaluation count.
     */
    public void recordRuleEvaluation(String ruleSource) {
        Counter.builder("aegis_rule_evaluations_total")
                .description("Number of rule evaluations by source")
                .tag("rule_source", ruleSource)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record cache hit or miss.
     */
    public void recordCacheAccess(boolean hit) {
        Counter.builder("aegis_cache_accesses_total")
                .description("Cache access count")
                .tag("result", hit ? "hit" : "miss")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record Redis operation latency.
     */
    public void recordRedisLatency(double milliseconds) {
        Timer.builder("aegis_redis_operation_duration_seconds")
                .description("Redis operation latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
                .record((long) (milliseconds * 1_000_000), TimeUnit.NANOSECONDS);
    }

    /**
     * Normalize endpoint for metric tags.
     * Replace path parameters with placeholders to avoid high-cardinality tags.
     */
    private String normalizeEndpoint(String endpoint) {
        // Replace numeric path segments with {id}
        String normalized = endpoint.replaceAll("/\\d+", "/{id}");
        // Replace UUIDs with {uuid}
        normalized = normalized.replaceAll("/[a-f0-9-]{36}", "/{uuid}");
        return normalized;
    }
}
