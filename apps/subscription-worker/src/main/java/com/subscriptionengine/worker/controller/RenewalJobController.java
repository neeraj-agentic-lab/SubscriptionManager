/**
 * REST API controller for managing subscription renewal jobs.
 * Provides endpoints for manual triggering and viewing execution history.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.worker.controller;

import com.subscriptionengine.scheduler.service.SubscriptionRenewalScheduler;
import com.subscriptionengine.scheduler.service.JobExecutionHistoryService;
import com.subscriptionengine.scheduler.service.JobConfigurationService;
import com.subscriptionengine.scheduler.service.DynamicSchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for renewal job management and monitoring.
 */
@RestController
@RequestMapping("/api/admin/renewal-jobs")
public class RenewalJobController {
    
    private static final Logger logger = LoggerFactory.getLogger(RenewalJobController.class);
    
    private final SubscriptionRenewalScheduler renewalScheduler;
    private final JobExecutionHistoryService jobHistoryService;
    private final JobConfigurationService jobConfigService;
    private final DynamicSchedulingService dynamicSchedulingService;
    
    public RenewalJobController(SubscriptionRenewalScheduler renewalScheduler,
                               JobExecutionHistoryService jobHistoryService,
                               JobConfigurationService jobConfigService,
                               DynamicSchedulingService dynamicSchedulingService) {
        this.renewalScheduler = renewalScheduler;
        this.jobHistoryService = jobHistoryService;
        this.jobConfigService = jobConfigService;
        this.dynamicSchedulingService = dynamicSchedulingService;
    }
    
    /**
     * Manually trigger subscription renewal job.
     * POST /api/admin/renewal-jobs/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerRenewalJob() {
        logger.info("[RENEWAL_API_TRIGGER] Manual renewal job trigger requested via API");
        
        try {
            renewalScheduler.triggerRenewalScheduling();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Subscription renewal job triggered successfully",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("[RENEWAL_API_TRIGGER_ERROR] Error triggering renewal job: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error triggering renewal job: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get recent job execution history.
     * GET /api/admin/renewal-jobs/history?limit=50
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getJobHistory(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String jobName) {
        
        logger.info("[RENEWAL_API_HISTORY] Job history requested (limit: {}, jobName: {})", limit, jobName);
        
        try {
            List<Map<String, Object>> history = jobHistoryService.getRecentExecutions(jobName, limit);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "executions", history,
                    "count", history.size(),
                    "limit", limit
                ),
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("[RENEWAL_API_HISTORY_ERROR] Error fetching job history: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error fetching job history: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get job execution statistics.
     * GET /api/admin/renewal-jobs/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getJobStatistics(
            @RequestParam(required = false) String jobName) {
        
        logger.info("[RENEWAL_API_STATS] Job statistics requested (jobName: {})", jobName);
        
        try {
            Map<String, Object> stats = jobHistoryService.getJobStatistics(jobName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("[RENEWAL_API_STATS_ERROR] Error fetching job statistics: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error fetching job statistics: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get current job status and next scheduled run.
     * GET /api/admin/renewal-jobs/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getJobStatus() {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        logger.info("[RENEWAL_API_STATUS_START] Job status requested - RequestId: {}", requestId);
        
        try {
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - Starting status endpoint processing", requestId);
            
            // Log service availability
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - Checking service dependencies", requestId);
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - renewalScheduler: {}", requestId, renewalScheduler != null ? "available" : "NULL");
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - jobHistoryService: {}", requestId, jobHistoryService != null ? "available" : "NULL");
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - jobConfigService: {}", requestId, jobConfigService != null ? "available" : "NULL");
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - dynamicSchedulingService: {}", requestId, dynamicSchedulingService != null ? "available" : "NULL");
            
            if (jobConfigService == null) {
                logger.error("[RENEWAL_API_STATUS_ERROR] RequestId: {} - jobConfigService is NULL", requestId);
                throw new IllegalStateException("JobConfigurationService is not available");
            }
            
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - Calling jobConfigService.getJobConfiguration()", requestId);
            
            // Get job configuration directly from JobConfigurationService
            Optional<Map<String, Object>> configOpt = jobConfigService.getJobConfiguration("subscription_renewal");
            
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - Job configuration retrieved: {}", requestId, configOpt.isPresent() ? "found" : "not found");
            
            if (configOpt.isPresent()) {
                logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - Configuration keys: {}", requestId, configOpt.get().keySet());
            }
            
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - Creating job status map", requestId);
            
            Map<String, Object> jobStatus = Map.of(
                "jobName", "subscription_renewal",
                "configExists", configOpt.isPresent(),
                "configuration", configOpt.orElse(Map.of()),
                "isScheduled", configOpt.isPresent() && (Boolean) configOpt.get().getOrDefault("enabled", false),
                "currentCronExpression", configOpt.map(config -> (String) config.get("cron_expression")).orElse("Not configured"),
                "requestId", requestId
            );
            
            logger.debug("[RENEWAL_API_STATUS_DEBUG] RequestId: {} - Job status map created successfully", requestId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "data", jobStatus,
                "timestamp", System.currentTimeMillis()
            );
            
            logger.info("[RENEWAL_API_STATUS_SUCCESS] RequestId: {} - Job status endpoint completed successfully", requestId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[RENEWAL_API_STATUS_ERROR] RequestId: {} - Error in job status endpoint", requestId, e);
            logger.error("[RENEWAL_API_STATUS_ERROR] RequestId: {} - Exception class: {}", requestId, e.getClass().getName());
            logger.error("[RENEWAL_API_STATUS_ERROR] RequestId: {} - Exception message: {}", requestId, e.getMessage());
            logger.error("[RENEWAL_API_STATUS_ERROR] RequestId: {} - Stack trace:", requestId, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error fetching job status: " + e.getMessage(),
                "timestamp", System.currentTimeMillis(),
                "requestId", requestId,
                "errorClass", e.getClass().getName()
            ));
        }
    }
    
    /**
     * Diagnostic endpoint to check service availability.
     * GET /api/admin/renewal-jobs/diagnostic
     */
    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> getDiagnostic() {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        logger.info("[RENEWAL_API_DIAGNOSTIC] RequestId: {} - Diagnostic endpoint called", requestId);
        
        Map<String, Object> diagnostic = Map.of(
            "renewalScheduler", renewalScheduler != null ? "available" : "NULL",
            "jobHistoryService", jobHistoryService != null ? "available" : "NULL", 
            "jobConfigService", jobConfigService != null ? "available" : "NULL",
            "dynamicSchedulingService", dynamicSchedulingService != null ? "available" : "NULL",
            "requestId", requestId,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("[RENEWAL_API_DIAGNOSTIC] RequestId: {} - Service availability: {}", requestId, diagnostic);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", diagnostic,
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Get available schedule presets.
     * GET /api/admin/renewal-jobs/schedule-presets
     */
    @GetMapping("/schedule-presets")
    public ResponseEntity<Map<String, Object>> getSchedulePresets() {
        logger.info("[RENEWAL_API_PRESETS] Schedule presets requested");
        
        try {
            Map<String, Object> presets = jobConfigService.getSchedulePresets();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", presets,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("[RENEWAL_API_PRESETS_ERROR] Error fetching schedule presets: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error fetching schedule presets: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Update job schedule configuration.
     * PUT /api/admin/renewal-jobs/schedule
     */
    @PutMapping("/schedule")
    public ResponseEntity<Map<String, Object>> updateJobSchedule(
            @RequestParam(defaultValue = "subscription_renewal") String jobName,
            @RequestParam String schedulePreset,
            @RequestParam(required = false) String customCronExpression,
            @RequestParam(defaultValue = "true") boolean enabled,
            @RequestParam(defaultValue = "admin") String updatedBy) {
        
        logger.info("[RENEWAL_API_SCHEDULE_UPDATE] Schedule update requested: job={}, preset={}, enabled={}", 
                   jobName, schedulePreset, enabled);
        
        try {
            boolean updated = dynamicSchedulingService.updateJobSchedule(jobName, schedulePreset, 
                customCronExpression, enabled, updatedBy);
            
            if (updated) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Job schedule updated successfully",
                    "jobName", jobName,
                    "schedulePreset", schedulePreset,
                    "enabled", enabled,
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "message", "Failed to update job schedule",
                    "timestamp", System.currentTimeMillis()
                ));
            }
            
        } catch (Exception e) {
            logger.error("[RENEWAL_API_SCHEDULE_UPDATE_ERROR] Error updating job schedule: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error updating job schedule: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get current job configuration.
     * GET /api/admin/renewal-jobs/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getJobConfiguration(
            @RequestParam(defaultValue = "subscription_renewal") String jobName) {
        
        logger.info("[RENEWAL_API_CONFIG] Job configuration requested for: {}", jobName);
        
        try {
            Map<String, Object> jobStatus = dynamicSchedulingService.getJobStatus(jobName);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", jobStatus,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("[RENEWAL_API_CONFIG_ERROR] Error fetching job configuration: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error fetching job configuration: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Refresh job schedules from database configuration.
     * POST /api/admin/renewal-jobs/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshJobSchedules() {
        logger.info("[RENEWAL_API_REFRESH] Job schedule refresh requested");
        
        try {
            dynamicSchedulingService.refreshAllJobSchedules();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Job schedules refreshed successfully",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("[RENEWAL_API_REFRESH_ERROR] Error refreshing job schedules: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error refreshing job schedules: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
