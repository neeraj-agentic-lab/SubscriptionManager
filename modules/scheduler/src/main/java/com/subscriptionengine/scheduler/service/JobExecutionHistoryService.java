/**
 * Service for tracking job execution history.
 * Essential for production monitoring and debugging.
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
import java.util.UUID;

import static com.subscriptionengine.generated.tables.JobExecutionHistory.JOB_EXECUTION_HISTORY;

/**
 * Tracks execution history of scheduled jobs for monitoring and debugging.
 */
@Service
public class JobExecutionHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobExecutionHistoryService.class);
    
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    
    public JobExecutionHistoryService(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Start tracking a job execution.
     */
    @Transactional
    public UUID startJobExecution(String jobName, String jobType, String triggerSource, String triggeredBy) {
        UUID executionId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        
        try {
            dsl.insertInto(JOB_EXECUTION_HISTORY)
                    .set(JOB_EXECUTION_HISTORY.ID, executionId)
                    .set(JOB_EXECUTION_HISTORY.JOB_NAME, jobName)
                    .set(JOB_EXECUTION_HISTORY.JOB_TYPE, jobType)
                    .set(JOB_EXECUTION_HISTORY.STATUS, "RUNNING")
                    .set(JOB_EXECUTION_HISTORY.STARTED_AT, now)
                    .set(JOB_EXECUTION_HISTORY.TRIGGER_SOURCE, triggerSource)
                    .set(JOB_EXECUTION_HISTORY.TRIGGERED_BY, triggeredBy)
                    .set(JOB_EXECUTION_HISTORY.CREATED_AT, now)
                    .set(JOB_EXECUTION_HISTORY.UPDATED_AT, now)
                    .execute();
            
            logger.info("[JOB_HISTORY_START] Started tracking job execution: {} (ID: {}, type: {}, source: {})", 
                       jobName, executionId, jobType, triggerSource);
            
            return executionId;
            
        } catch (Exception e) {
            logger.error("[JOB_HISTORY_START_ERROR] Failed to start job execution tracking: {}", e.getMessage(), e);
            return executionId; // Return ID even if tracking fails
        }
    }
    
    /**
     * Complete a job execution with success metrics.
     */
    @Transactional
    public void completeJobExecution(UUID executionId, int subscriptionsFound, int subscriptionsProcessed, 
                                   int tasksCreated, int errorsCount, Map<String, Object> executionDetails) {
        OffsetDateTime now = OffsetDateTime.now();
        
        try {
            // Get start time to calculate execution time
            var startTime = dsl.select(JOB_EXECUTION_HISTORY.STARTED_AT)
                    .from(JOB_EXECUTION_HISTORY)
                    .where(JOB_EXECUTION_HISTORY.ID.eq(executionId))
                    .fetchOne(JOB_EXECUTION_HISTORY.STARTED_AT);
            
            long executionTimeMs = 0;
            if (startTime != null) {
                executionTimeMs = java.time.Duration.between(startTime, now).toMillis();
            }
            
            String executionDetailsJson = null;
            if (executionDetails != null && !executionDetails.isEmpty()) {
                executionDetailsJson = objectMapper.writeValueAsString(executionDetails);
            }
            
            dsl.update(JOB_EXECUTION_HISTORY)
                    .set(JOB_EXECUTION_HISTORY.STATUS, "COMPLETED")
                    .set(JOB_EXECUTION_HISTORY.COMPLETED_AT, now)
                    .set(JOB_EXECUTION_HISTORY.EXECUTION_TIME_MS, executionTimeMs)
                    .set(JOB_EXECUTION_HISTORY.SUBSCRIPTIONS_FOUND, subscriptionsFound)
                    .set(JOB_EXECUTION_HISTORY.SUBSCRIPTIONS_PROCESSED, subscriptionsProcessed)
                    .set(JOB_EXECUTION_HISTORY.TASKS_CREATED, tasksCreated)
                    .set(JOB_EXECUTION_HISTORY.ERRORS_COUNT, errorsCount)
                    .set(JOB_EXECUTION_HISTORY.EXECUTION_DETAILS, executionDetailsJson != null ? JSONB.valueOf(executionDetailsJson) : null)
                    .set(JOB_EXECUTION_HISTORY.UPDATED_AT, now)
                    .where(JOB_EXECUTION_HISTORY.ID.eq(executionId))
                    .execute();
            
            logger.info("[JOB_HISTORY_COMPLETE] Completed job execution tracking: {} (found: {}, processed: {}, tasks: {}, errors: {}, time: {}ms)", 
                       executionId, subscriptionsFound, subscriptionsProcessed, tasksCreated, errorsCount, executionTimeMs);
            
        } catch (Exception e) {
            logger.error("[JOB_HISTORY_COMPLETE_ERROR] Failed to complete job execution tracking {}: {}", 
                        executionId, e.getMessage(), e);
        }
    }
    
    /**
     * Mark a job execution as failed.
     */
    @Transactional
    public void failJobExecution(UUID executionId, String errorMessage, Map<String, Object> executionDetails) {
        OffsetDateTime now = OffsetDateTime.now();
        
        try {
            // Get start time to calculate execution time
            var startTime = dsl.select(JOB_EXECUTION_HISTORY.STARTED_AT)
                    .from(JOB_EXECUTION_HISTORY)
                    .where(JOB_EXECUTION_HISTORY.ID.eq(executionId))
                    .fetchOne(JOB_EXECUTION_HISTORY.STARTED_AT);
            
            long executionTimeMs = 0;
            if (startTime != null) {
                executionTimeMs = java.time.Duration.between(startTime, now).toMillis();
            }
            
            String executionDetailsJson = null;
            if (executionDetails != null && !executionDetails.isEmpty()) {
                executionDetailsJson = objectMapper.writeValueAsString(executionDetails);
            }
            
            dsl.update(JOB_EXECUTION_HISTORY)
                    .set(JOB_EXECUTION_HISTORY.STATUS, "FAILED")
                    .set(JOB_EXECUTION_HISTORY.COMPLETED_AT, now)
                    .set(JOB_EXECUTION_HISTORY.EXECUTION_TIME_MS, executionTimeMs)
                    .set(JOB_EXECUTION_HISTORY.ERROR_MESSAGE, errorMessage)
                    .set(JOB_EXECUTION_HISTORY.EXECUTION_DETAILS, executionDetailsJson != null ? JSONB.valueOf(executionDetailsJson) : null)
                    .set(JOB_EXECUTION_HISTORY.UPDATED_AT, now)
                    .where(JOB_EXECUTION_HISTORY.ID.eq(executionId))
                    .execute();
            
            logger.error("[JOB_HISTORY_FAILED] Failed job execution tracking: {} (time: {}ms, error: {})", 
                        executionId, executionTimeMs, errorMessage);
            
        } catch (Exception e) {
            logger.error("[JOB_HISTORY_FAIL_ERROR] Failed to mark job execution as failed {}: {}", 
                        executionId, e.getMessage(), e);
        }
    }
    
    /**
     * Get recent job execution history.
     */
    public List<Map<String, Object>> getRecentExecutions(String jobName, int limit) {
        try {
            if (jobName != null) {
                return dsl.select(
                        JOB_EXECUTION_HISTORY.ID,
                        JOB_EXECUTION_HISTORY.JOB_NAME,
                        JOB_EXECUTION_HISTORY.JOB_TYPE,
                        JOB_EXECUTION_HISTORY.STATUS,
                        JOB_EXECUTION_HISTORY.STARTED_AT,
                        JOB_EXECUTION_HISTORY.COMPLETED_AT,
                        JOB_EXECUTION_HISTORY.EXECUTION_TIME_MS,
                        JOB_EXECUTION_HISTORY.SUBSCRIPTIONS_FOUND,
                        JOB_EXECUTION_HISTORY.SUBSCRIPTIONS_PROCESSED,
                        JOB_EXECUTION_HISTORY.TASKS_CREATED,
                        JOB_EXECUTION_HISTORY.ERRORS_COUNT,
                        JOB_EXECUTION_HISTORY.TRIGGER_SOURCE,
                        JOB_EXECUTION_HISTORY.TRIGGERED_BY,
                        JOB_EXECUTION_HISTORY.ERROR_MESSAGE
                    )
                    .from(JOB_EXECUTION_HISTORY)
                    .where(JOB_EXECUTION_HISTORY.JOB_NAME.eq(jobName))
                    .orderBy(JOB_EXECUTION_HISTORY.STARTED_AT.desc())
                    .limit(limit)
                    .fetchMaps();
            } else {
                return dsl.select(
                        JOB_EXECUTION_HISTORY.ID,
                        JOB_EXECUTION_HISTORY.JOB_NAME,
                        JOB_EXECUTION_HISTORY.JOB_TYPE,
                        JOB_EXECUTION_HISTORY.STATUS,
                        JOB_EXECUTION_HISTORY.STARTED_AT,
                        JOB_EXECUTION_HISTORY.COMPLETED_AT,
                        JOB_EXECUTION_HISTORY.EXECUTION_TIME_MS,
                        JOB_EXECUTION_HISTORY.SUBSCRIPTIONS_FOUND,
                        JOB_EXECUTION_HISTORY.SUBSCRIPTIONS_PROCESSED,
                        JOB_EXECUTION_HISTORY.TASKS_CREATED,
                        JOB_EXECUTION_HISTORY.ERRORS_COUNT,
                        JOB_EXECUTION_HISTORY.TRIGGER_SOURCE,
                        JOB_EXECUTION_HISTORY.TRIGGERED_BY,
                        JOB_EXECUTION_HISTORY.ERROR_MESSAGE
                    )
                    .from(JOB_EXECUTION_HISTORY)
                    .orderBy(JOB_EXECUTION_HISTORY.STARTED_AT.desc())
                    .limit(limit)
                    .fetchMaps();
            }
            
        } catch (Exception e) {
            logger.error("[JOB_HISTORY_QUERY_ERROR] Failed to query job execution history: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Get job execution statistics.
     */
    public Map<String, Object> getJobStatistics(String jobName) {
        try {
            // Use DSL functions instead of deprecated field methods
            var stats = jobName != null ?
                dsl.select(
                        org.jooq.impl.DSL.count().as("total_executions"),
                        org.jooq.impl.DSL.avg(JOB_EXECUTION_HISTORY.EXECUTION_TIME_MS).as("avg_execution_time_ms"),
                        org.jooq.impl.DSL.sum(JOB_EXECUTION_HISTORY.SUBSCRIPTIONS_PROCESSED).as("total_subscriptions_processed"),
                        org.jooq.impl.DSL.sum(JOB_EXECUTION_HISTORY.TASKS_CREATED).as("total_tasks_created"),
                        org.jooq.impl.DSL.sum(JOB_EXECUTION_HISTORY.ERRORS_COUNT).as("total_errors")
                    )
                    .from(JOB_EXECUTION_HISTORY)
                    .where(JOB_EXECUTION_HISTORY.JOB_NAME.eq(jobName))
                    .fetchOne() :
                dsl.select(
                        org.jooq.impl.DSL.count().as("total_executions"),
                        org.jooq.impl.DSL.avg(JOB_EXECUTION_HISTORY.EXECUTION_TIME_MS).as("avg_execution_time_ms"),
                        org.jooq.impl.DSL.sum(JOB_EXECUTION_HISTORY.SUBSCRIPTIONS_PROCESSED).as("total_subscriptions_processed"),
                        org.jooq.impl.DSL.sum(JOB_EXECUTION_HISTORY.TASKS_CREATED).as("total_tasks_created"),
                        org.jooq.impl.DSL.sum(JOB_EXECUTION_HISTORY.ERRORS_COUNT).as("total_errors")
                    )
                    .from(JOB_EXECUTION_HISTORY)
                    .fetchOne();
            
            // Get status breakdown
            var statusBreakdownRecords = jobName != null ?
                dsl.select(
                        JOB_EXECUTION_HISTORY.STATUS,
                        org.jooq.impl.DSL.count().as("count")
                    )
                    .from(JOB_EXECUTION_HISTORY)
                    .where(JOB_EXECUTION_HISTORY.JOB_NAME.eq(jobName))
                    .groupBy(JOB_EXECUTION_HISTORY.STATUS)
                    .fetch() :
                dsl.select(
                        JOB_EXECUTION_HISTORY.STATUS,
                        org.jooq.impl.DSL.count().as("count")
                    )
                    .from(JOB_EXECUTION_HISTORY)
                    .groupBy(JOB_EXECUTION_HISTORY.STATUS)
                    .fetch();
            
            Map<String, Object> statusBreakdown = new java.util.HashMap<>();
            for (var record : statusBreakdownRecords) {
                statusBreakdown.put(record.get(JOB_EXECUTION_HISTORY.STATUS), record.get("count"));
            }
            
            return Map.of(
                "totalExecutions", stats.get("total_executions") != null ? stats.get("total_executions") : 0,
                "avgExecutionTimeMs", stats.get("avg_execution_time_ms") != null ? stats.get("avg_execution_time_ms") : 0.0,
                "totalSubscriptionsProcessed", stats.get("total_subscriptions_processed") != null ? stats.get("total_subscriptions_processed") : 0,
                "totalTasksCreated", stats.get("total_tasks_created") != null ? stats.get("total_tasks_created") : 0,
                "totalErrors", stats.get("total_errors") != null ? stats.get("total_errors") : 0,
                "statusBreakdown", statusBreakdown
            );
            
        } catch (Exception e) {
            logger.error("[JOB_HISTORY_STATS_ERROR] Failed to get job statistics: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }
}
