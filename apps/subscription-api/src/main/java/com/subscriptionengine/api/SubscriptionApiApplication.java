package com.subscriptionengine.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Subscription API Application - Main REST API server.
 * Provides endpoints for subscription management, billing, and customer operations.
 * 
 * @author Neeraj Yadav
 */
@SpringBootApplication(scanBasePackages = "com.subscriptionengine")
public class SubscriptionApiApplication {
    
    /**
     * Enable scheduling only when not explicitly disabled (e.g., in tests).
     * Tests can disable scheduling by setting spring.task.scheduling.enabled=false
     */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "spring.task.scheduling.enabled", havingValue = "true", matchIfMissing = true)
    static class SchedulingConfiguration {
    }
    
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionApiApplication.class, args);
    }
}
