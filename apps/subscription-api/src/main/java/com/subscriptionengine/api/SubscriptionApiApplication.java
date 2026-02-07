package com.subscriptionengine.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Subscription API Application - Main REST API server.
 * Provides endpoints for subscription management, billing, and customer operations.
 * 
 * @author Neeraj Yadav
 */
@SpringBootApplication(scanBasePackages = "com.subscriptionengine")
@EnableScheduling
public class SubscriptionApiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionApiApplication.class, args);
    }
}
