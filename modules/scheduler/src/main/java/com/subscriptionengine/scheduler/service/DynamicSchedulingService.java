/**
 * Service for dynamic scheduling that reads configuration from database.
 * Allows runtime changes to job schedules without application restart.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages dynamic scheduling of jobs based on database configuration.
 */
@Service
public class DynamicSchedulingService {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicSchedulingService.class);
    
    private final TaskScheduler taskScheduler;
    private final JobConfigurationService jobConfigService;
    private final SubscriptionRenewalScheduler renewalScheduler;
    
    // Track currently scheduled tasks
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, String> currentCronExpressions = new ConcurrentHashMap<>();
    
    public DynamicSchedulingService(TaskScheduler taskScheduler,
                                   JobConfigurationService jobConfigService,
                                   SubscriptionRenewalScheduler renewalScheduler) {
        this.taskScheduler = taskScheduler;
        this.jobConfigService = jobConfigService;
        this.renewalScheduler = renewalScheduler;
    }
    
    /**
     * Initialize dynamic scheduling for all configured jobs.
     * Called at application startup.
     */
    public void initializeDynamicScheduling() {
        logger.info("[DYNAMIC_SCHEDULING_INIT] Initializing dynamic scheduling for all jobs");
        
        try {
            // Schedule subscription renewal job based on database configuration
            scheduleJobFromConfig("subscription_renewal");
            
            logger.info("[DYNAMIC_SCHEDULING_INIT_SUCCESS] Dynamic scheduling initialized successfully");
            
        } catch (Exception e) {
            logger.error("[DYNAMIC_SCHEDULING_INIT_ERROR] Error initializing dynamic scheduling: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Schedule or reschedule a job based on its database configuration.
     */
    public boolean scheduleJobFromConfig(String jobName) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        try {
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Scheduling job from config: {}", requestId, jobName);
            
            Optional<Map<String, Object>> configOpt = jobConfigService.getJobConfiguration(jobName);
            if (configOpt.isEmpty()) {
                logger.warn("[DYNAMIC_SCHEDULING_NO_CONFIG] RequestId: {} - No configuration found for job: {}", requestId, jobName);
                return false;
            }
            
            Map<String, Object> config = configOpt.get();
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Retrieved config for {}: {}", requestId, jobName, config.keySet());
            
            String cronExpression = (String) config.get("cron_expression");
            Boolean enabled = (Boolean) config.get("enabled");
            
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Job {}: cron={}, enabled={}", requestId, jobName, cronExpression, enabled);
            
            if (!Boolean.TRUE.equals(enabled)) {
                logger.info("[DYNAMIC_SCHEDULING_DISABLED] RequestId: {} - Job {} is disabled, unscheduling if running", requestId, jobName);
                unscheduleJob(jobName);
                return true;
            }
            
            if (cronExpression == null || cronExpression.trim().isEmpty()) {
                logger.error("[DYNAMIC_SCHEDULING_NO_CRON] RequestId: {} - No cron expression found for job: {}", requestId, jobName);
                return false;
            }
            
            // Check if schedule has changed
            String currentCron = currentCronExpressions.get(jobName);
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Current cron: {}, New cron: {}", requestId, currentCron, cronExpression);
            
            if (cronExpression.equals(currentCron)) {
                logger.debug("[DYNAMIC_SCHEDULING_NO_CHANGE] RequestId: {} - Schedule unchanged for job {}: {}", requestId, jobName, cronExpression);
                return true;
            }
            
            // Unschedule existing task if any
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Unscheduling existing task for job: {}", requestId, jobName);
            unscheduleJob(jobName);
            
            // Validate cron expression
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Validating cron expression: {}", requestId, cronExpression);
            if (!isValidCronExpression(cronExpression)) {
                logger.error("[DYNAMIC_SCHEDULING_INVALID_CRON] RequestId: {} - Invalid cron expression for job {}: {}", requestId, jobName, cronExpression);
                return false;
            }
            
            // Schedule the job
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Getting job task for: {}", requestId, jobName);
            Runnable jobTask = getJobTask(jobName);
            if (jobTask == null) {
                logger.error("[DYNAMIC_SCHEDULING_NO_TASK] RequestId: {} - No task implementation found for job: {}", requestId, jobName);
                return false;
            }
            
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Parsing cron expression and scheduling task", requestId);
            CronExpression cron = CronExpression.parse(cronExpression);
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(jobTask, triggerContext -> {
                Instant lastExecution = triggerContext.lastActualExecution();
                // Convert Instant to LocalDateTime for proper cron calculation
                java.time.LocalDateTime lastDateTime = lastExecution != null 
                    ? java.time.LocalDateTime.ofInstant(lastExecution, java.time.ZoneId.systemDefault())
                    : java.time.LocalDateTime.now();
                
                java.time.LocalDateTime nextDateTime = cron.next(lastDateTime);
                return nextDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
            });
            
            // Track the scheduled task
            logger.debug("[DYNAMIC_SCHEDULING_CONFIG] RequestId: {} - Tracking scheduled task for job: {}", requestId, jobName);
            scheduledTasks.put(jobName, scheduledTask);
            currentCronExpressions.put(jobName, cronExpression);
            
            logger.info("[DYNAMIC_SCHEDULING_SUCCESS] RequestId: {} - Successfully scheduled job {} with cron: {}", requestId, jobName, cronExpression);
            return true;
            
        } catch (Exception e) {
            logger.error("[DYNAMIC_SCHEDULING_ERROR] RequestId: {} - Error scheduling job {}", requestId, jobName, e);
            logger.error("[DYNAMIC_SCHEDULING_ERROR] RequestId: {} - Exception class: {}", requestId, e.getClass().getName());
            logger.error("[DYNAMIC_SCHEDULING_ERROR] RequestId: {} - Exception message: {}", requestId, e.getMessage());
            logger.error("[DYNAMIC_SCHEDULING_ERROR] RequestId: {} - Stack trace:", requestId, e);
            return false;
        }
    }
    
    /**
     * Unschedule a job.
     */
    public void unscheduleJob(String jobName) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.remove(jobName);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            currentCronExpressions.remove(jobName);
            logger.info("[DYNAMIC_SCHEDULING_UNSCHEDULED] Unscheduled job: {}", jobName);
        }
    }
    
    /**
     * Update job schedule at runtime.
     */
    public boolean updateJobSchedule(String jobName, String schedulePreset, String customCronExpression, 
                                   boolean enabled, String updatedBy) {
        try {
            logger.info("[DYNAMIC_SCHEDULING_UPDATE] Updating schedule for job {}: preset={}, enabled={}", 
                       jobName, schedulePreset, enabled);
            
            // Update configuration in database
            boolean configUpdated = jobConfigService.updateJobSchedule(jobName, schedulePreset, 
                customCronExpression, enabled, updatedBy);
            
            if (!configUpdated) {
                logger.error("[DYNAMIC_SCHEDULING_UPDATE_ERROR] Failed to update job configuration for: {}", jobName);
                return false;
            }
            
            // Reschedule the job with new configuration
            boolean rescheduled = scheduleJobFromConfig(jobName);
            
            if (rescheduled) {
                logger.info("[DYNAMIC_SCHEDULING_UPDATE_SUCCESS] Successfully updated and rescheduled job: {}", jobName);
            } else {
                logger.error("[DYNAMIC_SCHEDULING_UPDATE_ERROR] Failed to reschedule job after config update: {}", jobName);
            }
            
            return rescheduled;
            
        } catch (Exception e) {
            logger.error("[DYNAMIC_SCHEDULING_UPDATE_ERROR] Error updating job schedule for {}: {}", 
                        jobName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get current job status.
     */
    public Map<String, Object> getJobStatus(String jobName) {
        try {
            Optional<Map<String, Object>> configOpt = jobConfigService.getJobConfiguration(jobName);
            boolean isScheduled = scheduledTasks != null && scheduledTasks.containsKey(jobName);
            String currentCron = currentCronExpressions != null ? currentCronExpressions.get(jobName) : null;
            
            return Map.of(
                "jobName", jobName != null ? jobName : "unknown",
                "isScheduled", isScheduled,
                "currentCronExpression", currentCron != null ? currentCron : "Not scheduled",
                "configExists", configOpt != null && configOpt.isPresent(),
                "configuration", configOpt != null ? configOpt.orElse(Map.of()) : Map.of()
            );
            
        } catch (Exception e) {
            logger.error("[DYNAMIC_SCHEDULING_STATUS_ERROR] Error getting job status for {}: {}", 
                        jobName, e.getMessage(), e);
            return Map.of(
                "jobName", jobName,
                "error", e.getMessage()
            );
        }
    }
    
    /**
     * Get all scheduled jobs status.
     */
    public Map<String, Object> getAllJobsStatus() {
        return Map.of(
            "scheduledJobs", scheduledTasks.keySet(),
            "totalScheduled", scheduledTasks.size(),
            "jobDetails", scheduledTasks.keySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    jobName -> jobName,
                    this::getJobStatus
                ))
        );
    }
    
    /**
     * Get the actual job task implementation for a job name.
     */
    private Runnable getJobTask(String jobName) {
        return switch (jobName) {
            case "subscription_renewal" -> () -> {
                try {
                    renewalScheduler.scheduleRenewalTasks();
                } catch (Exception e) {
                    logger.error("[DYNAMIC_SCHEDULING_TASK_ERROR] Error executing subscription_renewal job: {}", 
                                e.getMessage(), e);
                }
            };
            // Add more job implementations here as needed
            default -> null;
        };
    }
    
    /**
     * Validate cron expression.
     */
    private boolean isValidCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (Exception e) {
            logger.warn("[DYNAMIC_SCHEDULING_CRON_INVALID] Invalid cron expression: {} - {}", 
                       cronExpression, e.getMessage());
            return false;
        }
    }
    
    /**
     * Refresh all job schedules from database configuration.
     * Useful for picking up configuration changes.
     */
    public void refreshAllJobSchedules() {
        logger.info("[DYNAMIC_SCHEDULING_REFRESH] Refreshing all job schedules from database");
        
        try {
            // Get all job configurations
            var allConfigs = jobConfigService.getAllJobConfigurations();
            
            for (Map<String, Object> config : allConfigs) {
                String jobName = (String) config.get("job_name");
                scheduleJobFromConfig(jobName);
            }
            
            logger.info("[DYNAMIC_SCHEDULING_REFRESH_SUCCESS] Refreshed {} job schedules", allConfigs.size());
            
        } catch (Exception e) {
            logger.error("[DYNAMIC_SCHEDULING_REFRESH_ERROR] Error refreshing job schedules: {}", e.getMessage(), e);
        }
    }
}
