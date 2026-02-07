package com.subscriptionengine.subscriptions.config;

import com.subscriptionengine.generated.tables.daos.CustomersDao;
import com.subscriptionengine.generated.tables.daos.PlansDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionItemsDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionsDao;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Subscriptions domain module.
 * Provides beans for Subscriptions, Customers, and Plans DAOs.
 * 
 * @author Neeraj Yadav
 */
@Configuration
public class SubscriptionsConfiguration {
    
    /**
     * Create SubscriptionsDao bean for database operations.
     */
    @Bean
    public SubscriptionsDao subscriptionsDao(DSLContext dslContext) {
        return new SubscriptionsDao(dslContext.configuration());
    }
    
    /**
     * Create CustomersDao bean for database operations.
     */
    @Bean
    public CustomersDao customersDao(DSLContext dslContext) {
        return new CustomersDao(dslContext.configuration());
    }
    
    /**
     * Create SubscriptionItemsDao bean for database operations.
     */
    @Bean
    public SubscriptionItemsDao subscriptionItemsDao(DSLContext dslContext) {
        return new SubscriptionItemsDao(dslContext.configuration());
    }
    
}
