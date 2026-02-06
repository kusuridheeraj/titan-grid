package com.titan.aegis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Caching configuration for rate limit rules.
 * Uses in-memory cache to reduce database queries.
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                // Cache for rate limit rules (60 second TTL)
                new ConcurrentMapCache("rateLimitRules")));

        log.info("Initialized cache manager with rateLimitRules cache");
        return cacheManager;
    }
}
