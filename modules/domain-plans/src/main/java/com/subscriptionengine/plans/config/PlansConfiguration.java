package com.subscriptionengine.plans.config;

import com.subscriptionengine.generated.tables.daos.PlansDao;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Plans domain module.
 * Provides beans for Plans DAO and other components.
 * 
 * @author Neeraj Yadav
 */
@Configuration
public class PlansConfiguration {
    
    /**
     * Create PlansDao bean for database operations.
     */
    @Bean
    public PlansDao plansDao(DSLContext dslContext) {
        return new PlansDao(dslContext.configuration());
    }
}
