package com.titan.aegis.config;

import com.titan.aegis.service.SuspiciousTrafficPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Async configuration for non-blocking event logging and stream publishing.
 * Also initializes Redis Stream consumer groups on startup.
 * 
 * @author Titan Grid Team
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {

    private final SuspiciousTrafficPublisher suspiciousTrafficPublisher;

    /**
     * Initialize Redis Stream consumer groups after application is fully started.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready - initializing Redis Stream consumer groups");
        suspiciousTrafficPublisher.initializeConsumerGroups();
    }
}
