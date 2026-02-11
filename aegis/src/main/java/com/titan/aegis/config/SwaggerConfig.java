package com.titan.aegis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 3.0 configuration for Aegis Rate Limiter.
 * 
 * Access Swagger UI at: http://localhost:8080/swagger-ui.html
 * Access OpenAPI JSON at: http://localhost:8080/v3/api-docs
 * 
 * @author Titan Grid Team
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI aegisOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Aegis - Distributed Rate Limiter API")
                        .description(
                                """
                                        **Aegis** is a production-grade distributed rate limiter built with Spring Boot and Redis.

                                        ## Features
                                        - **Hybrid Rate Limiting**: 3-tier priority system (Annotation > Database > YAML)
                                        - **Sliding Window Algorithm**: Accurate rate limiting using Redis Sorted Sets
                                        - **Multi-tier Client Identification**: API Key, JWT, IP Address
                                        - **Real-time Monitoring**: Redis Stream for suspicious traffic
                                        - **Audit Trail**: PostgreSQL logging for all blocked requests
                                        - **Prometheus Metrics**: Full observability with custom metrics

                                        ## Rate Limit Headers
                                        All responses include standard rate limit headers:
                                        - `X-RateLimit-Limit`: Maximum requests allowed
                                        - `X-RateLimit-Remaining`: Remaining requests in window
                                        - `X-RateLimit-Reset`: When the window resets
                                        - `Retry-After`: Seconds to wait (only on 429 responses)
                                        """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Titan Grid Team")
                                .url("https://github.com/kusuridheeraj/titan-grid"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development"),
                        new Server()
                                .url("https://api.titan-grid.com")
                                .description("Production")))
                .tags(List.of(
                        new Tag()
                                .name("Rate Limiting")
                                .description("Endpoints for testing rate limiting behavior"),
                        new Tag()
                                .name("Admin")
                                .description("Administrative endpoints for monitoring and management"),
                        new Tag()
                                .name("Analytics")
                                .description("Analytics and reporting endpoints")));
    }
}
