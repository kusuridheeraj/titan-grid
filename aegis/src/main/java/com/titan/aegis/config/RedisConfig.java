package com.titan.aegis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * Redis configuration for Rate Limiter.
 * Sets up RedisTemplate and loads Lua scripts.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    /**
     * Configure RedisTemplate with proper serializers.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use generic serializer for values
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));

        template.afterPropertiesSet();

        log.info("RedisTemplate configured successfully");
        return template;
    }

    /**
     * Load and cache the sliding window Lua script.
     * This script is executed atomically on Redis server.
     */
    @Bean
    public DefaultRedisScript<List> slidingWindowScript() {
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/sliding_window.lua")));
        redisScript.setResultType(List.class);

        log.info("Loaded sliding window Lua script");
        return redisScript;
    }
}
