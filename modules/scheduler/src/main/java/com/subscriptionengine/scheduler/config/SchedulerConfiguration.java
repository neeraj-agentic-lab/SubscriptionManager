package com.subscriptionengine.scheduler.config;

import com.subscriptionengine.generated.tables.daos.ScheduledTasksDao;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for Scheduler module.
 * Provides beans for ScheduledTasks DAO and TaskScheduler.
 * 
 * @author Neeraj Yadav
 */
@Configuration
public class SchedulerConfiguration {
    
    /**
     * Create ScheduledTasksDao bean for database operations.
     */
    @Bean
    public ScheduledTasksDao scheduledTasksDao(DSLContext dslContext) {
        return new ScheduledTasksDao(dslContext.configuration());
    }
    
    /**
     * Create TaskScheduler bean for dynamic scheduling.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("dynamic-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }
}
