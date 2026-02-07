/**
 * Service for managing job configuration settings.
 * Allows dynamic configuration of job schedules and settings.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.subscriptionengine.generated.tables.JobConfiguration.JOB_CONFIGURATION;

/**
 * Manages job configuration settings for dynamic scheduling.
 */
@Service
public class JobConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobConfigurationService.class);
    
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    
    // Predefined schedule presets
    private static final Map<String, String> SCHEDULE_PRESETS = Map.of(
        "DAILY_6AM", "0 0 6 * * *",
        "DAILY_MIDNIGHT", "0 0 0 * * *",
        "HOURLY", "0 0 * * * *",
        "EVERY_30_MINUTES", "0 */30 * * * *",
        "EVERY_15_MINUTES", "0 */15 * * * *",
        "EVERY_5_MINUTES", "0 */5 * * * *",
        "EVERY_MINUTE", "0 * * * * *",
        "WEEKLY_SUNDAY_6AM", "0 0 6 * * SUN"
    );
    
    public JobConfigurationService(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Get job configuration by job name.
     */
    public Optional<Map<String, Object>> getJobConfiguration(String jobName) {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        logger.debug("[JOB_CONFIG_GET_START] RequestId: {} - Getting job configuration for: {}", requestId, jobName);
        
        try {
            // Check if DSL context is available
            if (dsl == null) {
                logger.error("[JOB_CONFIG_GET_ERROR] RequestId: {} - DSL context is NULL", requestId);
                throw new IllegalStateException("DSL context is not available");
            }
            
            logger.debug("[JOB_CONFIG_GET_DEBUG] RequestId: {} - DSL context available, executing query", requestId);
            
            var config = dsl.select(
                    JOB_CONFIGURATION.JOB_NAME,
                    JOB_CONFIGURATION.JOB_DESCRIPTION,
                    JOB_CONFIGURATION.CRON_EXPRESSION,
                    JOB_CONFIGURATION.SCHEDULE_TYPE,
                    JOB_CONFIGURATION.TIME_ZONE,
                    JOB_CONFIGURATION.ENABLED,
                    JOB_CONFIGURATION.SCHEDULE_PRESET,
                    JOB_CONFIGURATION.MAX_CONCURRENT_EXECUTIONS,
                    JOB_CONFIGURATION.TIMEOUT_MINUTES,
                    JOB_CONFIGURATION.JOB_CONFIG,
                    JOB_CONFIGURATION.UPDATED_AT
                )
                .from(JOB_CONFIGURATION)
                .where(JOB_CONFIGURATION.JOB_NAME.eq(jobName))
                .fetchOne();
            
            logger.debug("[JOB_CONFIG_GET_DEBUG] RequestId: {} - Query executed, result: {}", requestId, config != null ? "found" : "not found");
            
            if (config == null) {
                logger.warn("[JOB_CONFIG_NOT_FOUND] RequestId: {} - Job configuration not found for: {}", requestId, jobName);
                return Optional.empty();
            }
            
            logger.debug("[JOB_CONFIG_GET_DEBUG] RequestId: {} - Converting record to map", requestId);
            Map<String, Object> configMap = config.intoMap();
            logger.debug("[JOB_CONFIG_GET_DEBUG] RequestId: {} - Configuration map keys: {}", requestId, configMap.keySet());
            
            // Handle JSONB serialization for Jackson
            if (configMap.containsKey("job_config") && configMap.get("job_config") != null) {
                Object jobConfigValue = configMap.get("job_config");
                logger.debug("[JOB_CONFIG_GET_DEBUG] RequestId: {} - job_config type: {}", requestId, jobConfigValue.getClass().getName());
                
                if (jobConfigValue instanceof org.jooq.JSONB) {
                    // Convert JSONB to String for JSON serialization
                    String jsonString = ((org.jooq.JSONB) jobConfigValue).data();
                    logger.debug("[JOB_CONFIG_GET_DEBUG] RequestId: {} - Converting JSONB to string: {}", requestId, jsonString);
                    configMap.put("job_config", jsonString);
                }
            }
            
            logger.info("[JOB_CONFIG_GET_SUCCESS] RequestId: {} - Job configuration retrieved successfully for: {}", requestId, jobName);
            
            return Optional.of(configMap);
            
        } catch (Exception e) {
            logger.error("[JOB_CONFIG_GET_ERROR] RequestId: {} - Error getting job configuration for {}", requestId, jobName, e);
            logger.error("[JOB_CONFIG_GET_ERROR] RequestId: {} - Exception class: {}", requestId, e.getClass().getName());
            logger.error("[JOB_CONFIG_GET_ERROR] RequestId: {} - Exception message: {}", requestId, e.getMessage());
            logger.error("[JOB_CONFIG_GET_ERROR] RequestId: {} - Stack trace:", requestId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Update job schedule configuration.
     */
    @Transactional
    public boolean updateJobSchedule(String jobName, String schedulePreset, String customCronExpression, 
                                   boolean enabled, String updatedBy) {
        try {
            String cronExpression;
            String actualPreset;
            
            if ("CUSTOM".equals(schedulePreset) && customCronExpression != null) {
                cronExpression = customCronExpression;
                actualPreset = "CUSTOM";
            } else if (SCHEDULE_PRESETS.containsKey(schedulePreset)) {
                cronExpression = SCHEDULE_PRESETS.get(schedulePreset);
                actualPreset = schedulePreset;
            } else {
                logger.error("[JOB_CONFIG_UPDATE_ERROR] Invalid schedule preset: {}", schedulePreset);
                return false;
            }
            
            int updatedRows = dsl.update(JOB_CONFIGURATION)
                    .set(JOB_CONFIGURATION.CRON_EXPRESSION, cronExpression)
                    .set(JOB_CONFIGURATION.SCHEDULE_PRESET, actualPreset)
                    .set(JOB_CONFIGURATION.ENABLED, enabled)
                    .set(JOB_CONFIGURATION.UPDATED_BY, updatedBy)
                    .set(JOB_CONFIGURATION.UPDATED_AT, OffsetDateTime.now())
                    .where(JOB_CONFIGURATION.JOB_NAME.eq(jobName))
                    .execute();
            
            if (updatedRows > 0) {
                logger.info("[JOB_CONFIG_UPDATE_SUCCESS] Updated job configuration for {}: preset={}, cron={}, enabled={}", 
                           jobName, actualPreset, cronExpression, enabled);
                return true;
            } else {
                logger.warn("[JOB_CONFIG_UPDATE_NOT_FOUND] Job configuration not found for update: {}", jobName);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("[JOB_CONFIG_UPDATE_ERROR] Error updating job configuration for {}: {}", jobName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get all available schedule presets.
     */
    public Map<String, Object> getSchedulePresets() {
        return Map.of(
            "presets", SCHEDULE_PRESETS.entrySet().stream()
                .map(entry -> Map.of(
                    "name", entry.getKey(),
                    "cronExpression", entry.getValue(),
                    "description", getPresetDescription(entry.getKey())
                ))
                .toList(),
            "customOption", Map.of(
                "name", "CUSTOM",
                "description", "Use custom cron expression"
            )
        );
    }
    
    /**
     * Get all job configurations.
     */
    public List<Map<String, Object>> getAllJobConfigurations() {
        try {
            return dsl.select(
                    JOB_CONFIGURATION.JOB_NAME,
                    JOB_CONFIGURATION.JOB_DESCRIPTION,
                    JOB_CONFIGURATION.CRON_EXPRESSION,
                    JOB_CONFIGURATION.SCHEDULE_PRESET,
                    JOB_CONFIGURATION.ENABLED,
                    JOB_CONFIGURATION.UPDATED_AT,
                    JOB_CONFIGURATION.UPDATED_BY
                )
                .from(JOB_CONFIGURATION)
                .orderBy(JOB_CONFIGURATION.JOB_NAME)
                .fetchMaps();
                
        } catch (Exception e) {
            logger.error("[JOB_CONFIG_LIST_ERROR] Error getting all job configurations: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Validate cron expression format.
     */
    public boolean isValidCronExpression(String cronExpression) {
        try {
            // Basic validation - Spring's CronExpression will validate this
            if (cronExpression == null || cronExpression.trim().isEmpty()) {
                return false;
            }
            
            String[] parts = cronExpression.trim().split("\\s+");
            // Spring cron supports 6 fields: second minute hour day month dayOfWeek
            return parts.length == 6;
            
        } catch (Exception e) {
            logger.warn("[JOB_CONFIG_CRON_INVALID] Invalid cron expression: {}", cronExpression);
            return false;
        }
    }
    
    /**
     * Get human-readable description for schedule presets.
     */
    private String getPresetDescription(String preset) {
        return switch (preset) {
            case "DAILY_6AM" -> "Daily at 6:00 AM";
            case "DAILY_MIDNIGHT" -> "Daily at midnight (12:00 AM)";
            case "HOURLY" -> "Every hour at the top of the hour";
            case "EVERY_30_MINUTES" -> "Every 30 minutes";
            case "EVERY_15_MINUTES" -> "Every 15 minutes";
            case "EVERY_5_MINUTES" -> "Every 5 minutes";
            case "EVERY_MINUTE" -> "Every minute";
            case "WEEKLY_SUNDAY_6AM" -> "Weekly on Sunday at 6:00 AM";
            default -> "Custom schedule";
        };
    }
    
    /**
     * Create or update job configuration.
     */
    @Transactional
    public boolean createOrUpdateJobConfiguration(String jobName, String description, String schedulePreset,
                                                String customCronExpression, boolean enabled, String createdBy) {
        try {
            String cronExpression;
            String actualPreset;
            
            if ("CUSTOM".equals(schedulePreset) && customCronExpression != null) {
                if (!isValidCronExpression(customCronExpression)) {
                    logger.error("[JOB_CONFIG_CREATE_ERROR] Invalid custom cron expression: {}", customCronExpression);
                    return false;
                }
                cronExpression = customCronExpression;
                actualPreset = "CUSTOM";
            } else if (SCHEDULE_PRESETS.containsKey(schedulePreset)) {
                cronExpression = SCHEDULE_PRESETS.get(schedulePreset);
                actualPreset = schedulePreset;
            } else {
                logger.error("[JOB_CONFIG_CREATE_ERROR] Invalid schedule preset: {}", schedulePreset);
                return false;
            }
            
            OffsetDateTime now = OffsetDateTime.now();
            
            // Use upsert (INSERT ... ON CONFLICT)
            int affectedRows = dsl.insertInto(JOB_CONFIGURATION)
                    .set(JOB_CONFIGURATION.JOB_NAME, jobName)
                    .set(JOB_CONFIGURATION.JOB_DESCRIPTION, description)
                    .set(JOB_CONFIGURATION.CRON_EXPRESSION, cronExpression)
                    .set(JOB_CONFIGURATION.SCHEDULE_TYPE, "CRON")
                    .set(JOB_CONFIGURATION.SCHEDULE_PRESET, actualPreset)
                    .set(JOB_CONFIGURATION.ENABLED, enabled)
                    .set(JOB_CONFIGURATION.CREATED_BY, createdBy)
                    .set(JOB_CONFIGURATION.UPDATED_BY, createdBy)
                    .set(JOB_CONFIGURATION.CREATED_AT, now)
                    .set(JOB_CONFIGURATION.UPDATED_AT, now)
                    .onConflict(JOB_CONFIGURATION.JOB_NAME)
                    .doUpdate()
                    .set(JOB_CONFIGURATION.JOB_DESCRIPTION, description)
                    .set(JOB_CONFIGURATION.CRON_EXPRESSION, cronExpression)
                    .set(JOB_CONFIGURATION.SCHEDULE_PRESET, actualPreset)
                    .set(JOB_CONFIGURATION.ENABLED, enabled)
                    .set(JOB_CONFIGURATION.UPDATED_BY, createdBy)
                    .set(JOB_CONFIGURATION.UPDATED_AT, now)
                    .execute();
            
            logger.info("[JOB_CONFIG_UPSERT_SUCCESS] Created/updated job configuration for {}: preset={}, cron={}, enabled={}", 
                       jobName, actualPreset, cronExpression, enabled);
            return true;
            
        } catch (Exception e) {
            logger.error("[JOB_CONFIG_UPSERT_ERROR] Error creating/updating job configuration for {}: {}", 
                        jobName, e.getMessage(), e);
            return false;
        }
    }
}
