package com.subscriptionengine.api.integration;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton Postgres container that is shared across all test classes.
 * This prevents the container from stopping between test classes.
 */
public class PostgresTestContainer {
    
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
    
    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("subscription_engine_test")
            .withUsername("test")
            .withPassword("test");
        
        POSTGRES_CONTAINER.start();
    }
    
    public static PostgreSQLContainer<?> getInstance() {
        return POSTGRES_CONTAINER;
    }
}
