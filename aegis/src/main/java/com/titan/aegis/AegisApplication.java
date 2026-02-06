package com.titan.aegis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aegis - Distributed Rate Limiter
 * 
 * High-performance rate limiting service using Redis and Sliding Window algorithm.
 * Prevents API abuse and ensures fair resource allocation across distributed systems.
 * 
 * @author Titan Grid Team
 * @version 1.0.0
 */
@SpringBootApplication
public class AegisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AegisApplication.class, args);
    }
}
