/**
 * Service for automatically scheduling subscription renewal tasks.
 * Runs daily to find subscriptions due for renewal and creates PRODUCT_RENEWAL tasks.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.scheduler.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.pojos.Subscriptions;
import com.subscriptionengine.generated.tables.pojos.SubscriptionItems;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.Subscriptions.SUBSCRIPTIONS;
import static com.subscriptionengine.generated.tables.SubscriptionItems.SUBSCRIPTION_ITEMS;

/**
 * Automated scheduler for subscription renewals.
 * Runs daily at 6 AM to find subscriptions due for renewal.
 */
@Service
public class SubscriptionRenewalScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionRenewalScheduler.class);
    
    private final DSLContext dsl;
    private final ScheduledTaskService scheduledTaskService;
    private final JobExecutionHistoryService jobHistoryService;
    private final ObjectMapper objectMapper;
    
    public SubscriptionRenewalScheduler(DSLContext dsl, 
                                       ScheduledTaskService scheduledTaskService,
                                       JobExecutionHistoryService jobHistoryService,
                                       ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.scheduledTaskService = scheduledTaskService;
        this.jobHistoryService = jobHistoryService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Schedule renewal tasks for subscriptions due for renewal.
     * Now called dynamically based on database configuration.
     */
    @Transactional
    public void scheduleRenewalTasks() {
        executeRenewalScheduling("SCHEDULED", "CRON", "system");
    }
    
    /**
     * Execute renewal scheduling with job history tracking.
     */
    private void executeRenewalScheduling(String jobType, String triggerSource, String triggeredBy) {
        logger.info("[RENEWAL_SCHEDULER_START] Starting subscription renewal task scheduling (type: {}, source: {})", jobType, triggerSource);
        
        // Start job execution tracking
        UUID executionId = jobHistoryService.startJobExecution("subscription_renewal", jobType, triggerSource, triggeredBy);
        
        long startTime = System.currentTimeMillis();
        OffsetDateTime now = OffsetDateTime.now();
        
        int subscriptionsFound = 0;
        int subscriptionsProcessed = 0;
        int tasksCreated = 0;
        int errorsCount = 0;
        
        try {
            // Find all subscriptions due for renewal across all tenants
            logger.debug("[RENEWAL_SCHEDULER_STEP_1] Finding subscriptions due for renewal");
            
            List<Subscriptions> dueSubscriptions = dsl.selectFrom(SUBSCRIPTIONS)
                    .where(SUBSCRIPTIONS.NEXT_RENEWAL_AT.le(now))
                    .and(SUBSCRIPTIONS.STATUS.eq("ACTIVE"))
                    .fetchInto(Subscriptions.class);
            
            subscriptionsFound = dueSubscriptions.size();
            
            if (dueSubscriptions.isEmpty()) {
                logger.info("[RENEWAL_SCHEDULER_NO_RENEWALS] No subscriptions due for renewal today");
                // Complete job execution tracking with zero metrics
                jobHistoryService.completeJobExecution(executionId, 0, 0, 0, 0, 
                    Map.of("message", "No subscriptions due for renewal"));
                return;
            }
            
            logger.info("[RENEWAL_SCHEDULER_STEP_1_SUCCESS] Found {} subscriptions due for renewal", subscriptionsFound);
            
            // Process each subscription due for renewal
            for (Subscriptions subscription : dueSubscriptions) {
                try {
                    // Set tenant context for this subscription
                    TenantContext.setTenantId(subscription.getTenantId());
                    
                    logger.debug("[RENEWAL_SCHEDULER_STEP_2] Processing subscription {} for tenant {}", 
                                subscription.getId(), subscription.getTenantId());
                    
                    // Get subscription items for this subscription
                    List<SubscriptionItems> subscriptionItems = dsl.selectFrom(SUBSCRIPTION_ITEMS)
                            .where(SUBSCRIPTION_ITEMS.SUBSCRIPTION_ID.eq(subscription.getId()))
                            .and(SUBSCRIPTION_ITEMS.TENANT_ID.eq(subscription.getTenantId()))
                            .fetchInto(SubscriptionItems.class);
                    
                    if (subscriptionItems.isEmpty()) {
                        logger.warn("[RENEWAL_SCHEDULER_WARNING] No subscription items found for subscription {}", 
                                   subscription.getId());
                        continue;
                    }
                    
                    // Create PRODUCT_RENEWAL task for each subscription item
                    for (SubscriptionItems item : subscriptionItems) {
                        try {
                            logger.debug("[RENEWAL_SCHEDULER_STEP_3] Creating PRODUCT_RENEWAL task for subscription {} item {}", 
                                        subscription.getId(), item.getId());
                            
                            // Schedule product renewal task to run immediately
                            UUID taskId = scheduledTaskService.scheduleProductRenewal(
                                subscription.getId(),
                                extractProductIdFromItem(item),
                                item.getPlanId(),
                                now // Process immediately
                            );
                            
                            logger.info("[RENEWAL_SCHEDULER_TASK_CREATED] Created PRODUCT_RENEWAL task {} for subscription {} plan {}", 
                                       taskId, subscription.getId(), item.getPlanId());
                            tasksCreated++;
                            
                        } catch (Exception e) {
                            logger.error("[RENEWAL_SCHEDULER_TASK_ERROR] Failed to create renewal task for subscription {} item {}: {}", 
                                        subscription.getId(), item.getId(), e.getMessage(), e);
                            errorsCount++;
                        }
                    }
                    
                    subscriptionsProcessed++;
                    
                } catch (Exception e) {
                    logger.error("[RENEWAL_SCHEDULER_SUBSCRIPTION_ERROR] Failed to process subscription {}: {}", 
                                subscription.getId(), e.getMessage(), e);
                    errorsCount++;
                } finally {
                    TenantContext.clear();
                }
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("[RENEWAL_SCHEDULER_SUCCESS] Completed renewal task scheduling: {} subscriptions found, {} processed, {} tasks created, {} errors in {}ms", 
                       subscriptionsFound, subscriptionsProcessed, tasksCreated, errorsCount, processingTime);
            
            // Complete job execution tracking
            Map<String, Object> executionDetails = Map.of(
                "processingTimeMs", processingTime,
                "subscriptionIds", dueSubscriptions.stream().map(s -> s.getId().toString()).toList()
            );
            jobHistoryService.completeJobExecution(executionId, subscriptionsFound, subscriptionsProcessed, 
                tasksCreated, errorsCount, executionDetails);
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            logger.error("[RENEWAL_SCHEDULER_ERROR] Error during renewal task scheduling after {}ms: {}", 
                        errorTime, e.getMessage(), e);
            
            // Mark job execution as failed
            Map<String, Object> errorDetails = Map.of(
                "processingTimeMs", errorTime,
                "subscriptionsFound", subscriptionsFound,
                "subscriptionsProcessed", subscriptionsProcessed,
                "tasksCreated", tasksCreated,
                "errorsCount", errorsCount
            );
            jobHistoryService.failJobExecution(executionId, e.getMessage(), errorDetails);
        }
    }
    
    /**
     * Extract product ID from subscription item.
     * For now, we'll derive it from the item configuration or use a default pattern.
     */
    private String extractProductIdFromItem(SubscriptionItems item) {
        try {
            // Try to extract from item_config JSON if available
            if (item.getItemConfig() != null) {
                Map<String, Object> config = objectMapper.readValue(item.getItemConfig().data(), Map.class);
                String productId = (String) config.get("productId");
                if (productId != null) {
                    return productId;
                }
            }
            
            // Fallback: generate product ID from plan ID
            return "product_" + item.getPlanId().toString().substring(0, 8);
            
        } catch (Exception e) {
            logger.warn("[RENEWAL_SCHEDULER_PRODUCT_ID_FALLBACK] Could not extract product ID from item {}, using fallback: {}", 
                       item.getId(), e.getMessage());
            return "product_" + item.getPlanId().toString().substring(0, 8);
        }
    }
    
    /**
     * Manual trigger for renewal scheduling (for testing purposes).
     * Can be called via management endpoint or admin interface.
     */
    public void triggerRenewalScheduling() {
        logger.info("[RENEWAL_SCHEDULER_MANUAL_TRIGGER] Manual renewal scheduling triggered");
        executeRenewalScheduling("MANUAL", "API", "admin");
    }
}
