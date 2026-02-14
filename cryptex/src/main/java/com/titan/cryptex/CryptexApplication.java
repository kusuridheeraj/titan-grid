package com.titan.cryptex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Cryptex - Secure Storage Service
 * Handles zero-trust file uploads with streaming encryption.
 */
@EnableAsync
@SpringBootApplication
public class CryptexApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptexApplication.class, args);
    }
}
