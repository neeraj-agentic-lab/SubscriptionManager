package com.subscriptionengine.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Subscription Worker Application - Background task processor.
 * Handles scheduled tasks for renewals, payments, and deliveries.
 * 
 * @author Neeraj Yadav
 */
@SpringBootApplication(scanBasePackages = "com.subscriptionengine")
@EnableScheduling
public class SubscriptionWorkerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SubscriptionWorkerApplication.class, args);
    }
}
