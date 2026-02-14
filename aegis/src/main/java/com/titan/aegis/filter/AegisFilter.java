package com.titan.aegis.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.titan.aegis.metrics.RateLimiterMetrics;
import com.titan.aegis.model.RateLimitDecision;
import com.titan.aegis.model.RateLimitRule;
import com.titan.aegis.model.RequestToken;
import com.titan.aegis.service.ClientIdExtractor;
import com.titan.aegis.service.RateLimitEventLogger;
import com.titan.aegis.service.RateLimitRuleResolver;
import com.titan.aegis.service.RateLimiterService;
import com.titan.aegis.service.SuspiciousTrafficPublisher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.util.Map;

/**
 * Servlet filter for automatic rate limiting of all HTTP requests.
 * 
 * This filter intercepts every request and applies rate limiting using the
 * 3-tier hybrid rule system:
 * 1. @RateLimit annotation (if present on controller method)
 * 2. Database rules (if matching pattern found)
 * 3. YAML configuration (default fallback)
 * 
 * Integrates with:
 * - PostgreSQL event logger (async audit trail)
 * - Redis Stream publisher (real-time suspicious traffic monitoring)
 * - Prometheus metrics (performance and analytics)
 * 
 * Runs once per request (OncePerRequestFilter) and applies limits BEFORE
 * the request reaches the controller.
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Component
@Order(1) // Run early in filter chain
public class AegisFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final RateLimitRuleResolver ruleResolver;
    private final ClientIdExtractor clientIdExtractor;
    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;

    // Day 6-7: Observability components
    private final RateLimitEventLogger eventLogger;
    private final SuspiciousTrafficPublisher trafficPublisher;
    private final RateLimiterMetrics metrics;

    public AegisFilter(
            RateLimiterService rateLimiterService,
            RateLimitRuleResolver ruleResolver,
            ClientIdExtractor clientIdExtractor,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            ObjectMapper objectMapper,
            RateLimitEventLogger eventLogger,
            SuspiciousTrafficPublisher trafficPublisher,
            RateLimiterMetrics metrics) {
        this.rateLimiterService = rateLimiterService;
        this.ruleResolver = ruleResolver;
        this.clientIdExtractor = clientIdExtractor;
        this.handlerMapping = handlerMapping;
        this.objectMapper = objectMapper;
        this.eventLogger = eventLogger;
        this.trafficPublisher = trafficPublisher;
        this.metrics = metrics;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for excluded endpoints
        if (ruleResolver.shouldExclude(endpoint)) {
            log.debug("Skipping rate limit for excluded endpoint: {}", endpoint);
            filterChain.doFilter(request, response);
            return;
        }

        // Start decision timer
        long startTime = System.nanoTime();

        try {
            // Get handler method (may be null for non-controller requests)
            HandlerMethod handlerMethod = getHandlerMethod(request);

            // Resolve rate limit rule (annotation > database > YAML)
            RateLimitRule rule = ruleResolver.resolveRule(request, handlerMethod);

            // Extract client ID based on rule's client type
            String clientId = clientIdExtractor.extractClientId(
                    request,
                    rule.clientType(),
                    rule.customKey());

            // Create request token
            RequestToken token = new RequestToken(
                    clientId,
                    endpoint,
                    rule.limit(),
                    rule.window());

            // Check rate limit
            RateLimitDecision decision = rateLimiterService.isAllowed(token);

            // Calculate decision time
            double decisionTimeMs = (System.nanoTime() - startTime) / 1_000_000.0;

            // Add rate limit headers to response
            addRateLimitHeaders(response, decision);

            // Record metrics
            metrics.recordRequest(endpoint, rule.clientType().name(), rule.source().name(), decision.allowed());
            metrics.recordDecisionTime(decisionTimeMs, rule.source().name());
            metrics.recordRuleEvaluation(rule.source().name());

            if (decision.allowed()) {
                // Request allowed - proceed
                log.debug("Rate limit OK: {} {} by {} ({}/{} used, source: {}, time: {}ms)",
                        method, endpoint, clientId, decision.currentCount(), decision.limit(),
                        rule.source(), String.format("%.2f", decisionTimeMs));

                filterChain.doFilter(request, response);
            } else {
                // Request blocked - return 429
                log.warn("Rate limit EXCEEDED: {} {} by {} ({}/{}, reset in {}s, source: {}, time: {}ms)",
                        method, endpoint, clientId, decision.currentCount(), decision.limit(),
                        decision.retryAfter().getSeconds(), rule.source(), String.format("%.2f", decisionTimeMs));

                // Record blocked metric with severity
                String severity = eventLogger.calculateSeverity(decision.currentCount(), decision.limit());
                metrics.recordBlocked(endpoint, rule.clientType().name(), rule.source().name(), severity);

                // Async: Log to PostgreSQL
                eventLogger.logBlockedRequest(request, clientId, decision, rule, decisionTimeMs);

                // Async: Publish to Redis Stream
                trafficPublisher.publishBlockedRequest(request, clientId, decision, rule, decisionTimeMs);

                sendRateLimitExceeded(response, decision, clientId, endpoint);
            }

        } catch (Exception e) {
            log.error("Error in rate limit filter for {} {}", method, endpoint, e);
            // On error, allow request (fail-open for availability)
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Get HandlerMethod for the current request.
     * Returns null if request doesn't map to a controller method.
     */
    private HandlerMethod getHandlerMethod(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod) {
                return (HandlerMethod) chain.getHandler();
            }
        } catch (Exception e) {
            log.debug("Could not get handler method for {}", request.getRequestURI());
        }
        return null;
    }

    /**
     * Add standard rate limit headers to response.
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.addHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.addHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.addHeader("X-RateLimit-Reset", decision.resetTime().toString());

        if (!decision.allowed() && decision.retryAfter() != null) {
            response.addHeader("Retry-After", String.valueOf(decision.retryAfter().getSeconds()));
        }
    }

    /**
     * Send HTTP 429 Too Many Requests response.
     */
    private void sendRateLimitExceeded(
            HttpServletResponse response,
            RateLimitDecision decision,
            String clientId,
            String endpoint) throws IOException {

        response.setStatus(429); // HTTP 429 Too Many Requests
        response.setContentType("application/json");

        Map<String, Object> errorBody = Map.of(
                "error", "Rate Limit Exceeded",
                "message", "Too many requests. Please try again later.",
                "status", 429,
                "endpoint", endpoint,
                "limit", decision.limit(),
                "window", "60 seconds",
                "retryAfter", decision.retryAfter().getSeconds() + " seconds",
                "resetTime", decision.resetTime().toString());

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
        response.getWriter().flush();
    }
}
