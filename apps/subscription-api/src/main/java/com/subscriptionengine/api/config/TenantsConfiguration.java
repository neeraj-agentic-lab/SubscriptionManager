package com.subscriptionengine.api.config;

import com.subscriptionengine.generated.tables.daos.TenantsDao;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Tenants API components.
 * 
 * @author Neeraj Yadav
 */
@Configuration
public class TenantsConfiguration {
    
    /**
     * Create TenantsDao bean for database operations.
     */
    @Bean
    public TenantsDao tenantsDao(DSLContext dslContext) {
        return new TenantsDao(dslContext.configuration());
    }
}
