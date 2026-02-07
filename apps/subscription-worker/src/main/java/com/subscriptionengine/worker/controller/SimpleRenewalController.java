/**
 * Simple REST API controller for testing subscription renewal jobs.
 * Basic version without configurable scheduling features.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.worker.controller;

import com.subscriptionengine.scheduler.service.SubscriptionRenewalScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simple controller for renewal job testing.
 */
@RestController
@RequestMapping("/api/admin/simple-renewal")
public class SimpleRenewalController {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleRenewalController.class);
    
    private final SubscriptionRenewalScheduler renewalScheduler;
    
    public SimpleRenewalController(SubscriptionRenewalScheduler renewalScheduler) {
        this.renewalScheduler = renewalScheduler;
    }
    
    /**
     * Manually trigger subscription renewal job.
     * POST /api/admin/simple-renewal/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerRenewalJob() {
        logger.info("[SIMPLE_RENEWAL_TRIGGER] Manual renewal job trigger requested");
        
        try {
            renewalScheduler.triggerRenewalScheduling();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Subscription renewal job triggered successfully",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("[SIMPLE_RENEWAL_TRIGGER_ERROR] Error triggering renewal job: {}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error triggering renewal job: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get basic job status.
     * GET /api/admin/simple-renewal/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getJobStatus() {
        logger.info("[SIMPLE_RENEWAL_STATUS] Job status requested");
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "jobName", "subscription_renewal",
                "status", "AVAILABLE",
                "description", "Basic renewal job - can be triggered manually",
                "endpoint", "/api/admin/simple-renewal/trigger"
            ),
            "timestamp", System.currentTimeMillis()
        ));
    }
}
