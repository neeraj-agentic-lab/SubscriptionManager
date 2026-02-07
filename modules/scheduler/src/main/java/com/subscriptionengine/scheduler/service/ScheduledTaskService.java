package com.subscriptionengine.scheduler.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.ScheduledTasksDao;
import com.subscriptionengine.generated.tables.pojos.ScheduledTasks;
import com.subscriptionengine.generated.tables.records.ScheduledTasksRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing scheduled tasks with tenant isolation.
 * Handles task creation, scheduling, and lifecycle management.
 * 
 * @author Neeraj Yadav
 */
@Service
public class ScheduledTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    
    private final DSLContext dsl;
    private final ScheduledTasksDao scheduledTasksDao;
    private final ObjectMapper objectMapper;
    
    public ScheduledTaskService(DSLContext dsl, ScheduledTasksDao scheduledTasksDao, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.scheduledTasksDao = scheduledTasksDao;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Schedule a subscription renewal task.
     * 
     * @param subscriptionId the subscription ID
     * @param renewalDate when the renewal should be processed
     * @return the created scheduled task
     */
    @Transactional
    public ScheduledTasks scheduleSubscriptionRenewal(UUID subscriptionId, OffsetDateTime renewalDate) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("Scheduling subscription renewal for subscription {} at {} (tenant: {})", 
                   subscriptionId, renewalDate, tenantId);
        
        // Create task payload with subscription information
        Map<String, Object> payloadMap = Map.of(
            "subscriptionId", subscriptionId.toString(),
            "taskType", "SUBSCRIPTION_RENEWAL",
            "scheduledAt", OffsetDateTime.now().toString()
        );
        
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize task payload", e);
        }
        JSONB payload = JSONB.valueOf(payloadJson);
        
        // Create unique task key to prevent duplicates
        String taskKey = String.format("subscription_renewal_%s", subscriptionId);

        try {
            var t = com.subscriptionengine.generated.tables.ScheduledTasks.SCHEDULED_TASKS;

            ScheduledTasksRecord record = dsl.update(t)
                .set(t.STATUS, "READY")
                .set(t.DUE_AT, renewalDate)
                .set(t.ATTEMPT_COUNT, 0)
                .set(t.MAX_ATTEMPTS, 3)
                .set(t.PAYLOAD, payload)
                .set(t.UPDATED_AT, OffsetDateTime.now())
                .where(t.TENANT_ID.eq(tenantId))
                .and(t.TASK_KEY.eq(taskKey))
                .returning()
                .fetchOne();

            if (record == null) {
                ScheduledTasks task = new ScheduledTasks();
                task.setId(UUID.randomUUID());
                task.setTenantId(tenantId);
                task.setTaskType("SUBSCRIPTION_RENEWAL");
                task.setTaskKey(taskKey);
                task.setStatus("READY");
                task.setDueAt(renewalDate);
                task.setAttemptCount(0);
                task.setMaxAttempts(3);
                task.setPayload(payload);
                task.setCreatedAt(OffsetDateTime.now());
                task.setUpdatedAt(OffsetDateTime.now());

                try {
                    scheduledTasksDao.insert(task);
                    record = dsl.selectFrom(t)
                        .where(t.ID.eq(task.getId()))
                        .fetchOne();
                } catch (DataAccessException insertException) {
                    record = dsl.update(t)
                        .set(t.STATUS, "READY")
                        .set(t.DUE_AT, renewalDate)
                        .set(t.ATTEMPT_COUNT, 0)
                        .set(t.MAX_ATTEMPTS, 3)
                        .set(t.PAYLOAD, payload)
                        .set(t.UPDATED_AT, OffsetDateTime.now())
                        .where(t.TENANT_ID.eq(tenantId))
                        .and(t.TASK_KEY.eq(taskKey))
                        .returning()
                        .fetchOne();

                    if (record == null) {
                        throw insertException;
                    }
                }
            }

            ScheduledTasks task = record.into(ScheduledTasks.class);

            logger.info("Successfully scheduled renewal task {} for subscription {} (tenant: {})", 
                       task.getId(), subscriptionId, tenantId);

            return task;
        } catch (DataAccessException e) {
            logger.error("Failed to schedule subscription renewal for subscription {} (tenant: {}) due to DB error: {}",
                subscriptionId, tenantId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Schedule a subscription trial end task.
     * 
     * @param subscriptionId the subscription ID
     * @param trialEndDate when the trial ends
     * @return the created scheduled task
     */
    @Transactional
    public ScheduledTasks scheduleTrialEnd(UUID subscriptionId, OffsetDateTime trialEndDate) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("Scheduling trial end for subscription {} at {} (tenant: {})", 
                   subscriptionId, trialEndDate, tenantId);
        
        Map<String, Object> payloadMap = Map.of(
            "subscriptionId", subscriptionId.toString(),
            "taskType", "TRIAL_END",
            "scheduledAt", OffsetDateTime.now().toString()
        );
        
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize task payload", e);
        }
        JSONB payload = JSONB.valueOf(payloadJson);
        String taskKey = String.format("trial_end_%s", subscriptionId);
        
        ScheduledTasks task = new ScheduledTasks();
        task.setId(UUID.randomUUID());
        task.setTenantId(tenantId);
        task.setTaskType("TRIAL_END");
        task.setTaskKey(taskKey);
        task.setStatus("READY");
        task.setDueAt(trialEndDate);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        task.setPayload(payload);
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        
        scheduledTasksDao.insert(task);
        
        logger.info("Successfully scheduled trial end task {} for subscription {} (tenant: {})", 
                   task.getId(), subscriptionId, tenantId);
        
        return task;
    }
    
    /**
     * Cancel scheduled tasks for a subscription.
     * Used when subscription is cancelled or modified.
     * 
     * @param subscriptionId the subscription ID
     * @param taskType the specific task type to cancel, or null for all
     */
    @Transactional
    public void cancelSubscriptionTasks(UUID subscriptionId, String taskType) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("Cancelling scheduled tasks for subscription {} (tenant: {})", subscriptionId, tenantId);
        
        // Implementation would update tasks to CANCELLED status
        // This is a placeholder for the actual cancellation logic
        logger.debug("Task cancellation logic would be implemented here");
    }
    
    /**
     * Schedule a product renewal task for a specific product within a subscription.
     * Each product can have its own billing schedule.
     */
    @Transactional
    public UUID scheduleProductRenewal(UUID subscriptionId, String productId, UUID planId, OffsetDateTime renewalAt) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        String taskKey = String.format("product_renewal_%s_%s_%s", tenantId, subscriptionId, productId);
        
        logger.info("Scheduling product renewal task for subscription {} product {} at {} (tenant: {})", 
                   subscriptionId, productId, renewalAt, tenantId);
        
        ScheduledTasks task = new ScheduledTasks();
        task.setId(UUID.randomUUID());
        task.setTenantId(tenantId);
        task.setTaskType("PRODUCT_RENEWAL");
        task.setTaskKey(taskKey);
        task.setStatus("READY");
        task.setDueAt(renewalAt);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        
        // Create payload with subscription, product, and plan details
        Map<String, Object> payloadMap = Map.of(
            "subscriptionId", subscriptionId.toString(),
            "productId", productId,
            "planId", planId.toString(),
            "taskType", "PRODUCT_RENEWAL",
            "scheduledAt", OffsetDateTime.now().toString()
        );
        
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize task payload", e);
        }
        JSONB payload = JSONB.valueOf(payloadJson);
        
        task.setPayload(payload);
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        
        scheduledTasksDao.insert(task);
        
        logger.info("Successfully scheduled product renewal task {} for subscription {} product {} (tenant: {})", 
                   task.getId(), subscriptionId, productId, tenantId);
        
        return task.getId();
    }
    
    /**
     * Schedule payment processing for an invoice.
     * Creates a CHARGE_PAYMENT task to process payment for the given invoice.
     */
    public UUID schedulePaymentProcessing(UUID invoiceId, UUID tenantId, OffsetDateTime processAt) {
        logger.info("Scheduling payment processing for invoice {} at {} (tenant: {})", 
                   invoiceId, processAt, tenantId);
        
        // Create task payload
        Map<String, Object> payloadMap = Map.of(
            "invoiceId", invoiceId.toString(),
            "taskType", "CHARGE_PAYMENT",
            "scheduledAt", processAt.toString()
        );
        
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize payment task payload", e);
        }
        JSONB payload = JSONB.valueOf(payloadJson);
        
        // Create unique task key to prevent duplicates
        String taskKey = String.format("payment_%s", invoiceId);
        
        ScheduledTasks task = new ScheduledTasks();
        task.setId(UUID.randomUUID());
        task.setTenantId(tenantId);
        task.setTaskType("CHARGE_PAYMENT");
        task.setTaskKey(taskKey);
        task.setStatus("READY");
        task.setDueAt(processAt);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        task.setPayload(payload);
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        
        scheduledTasksDao.insert(task);
        
        logger.info("Successfully scheduled payment processing task {} for invoice {} at {} (tenant: {})", 
                   task.getId(), invoiceId, processAt, tenantId);
        
        return task.getId();
    }
    
    /**
     * Schedule delivery creation for an invoice.
     * Creates a CREATE_DELIVERY task to create delivery instances for physical/hybrid products.
     */
    public UUID scheduleDeliveryCreation(UUID invoiceId, UUID subscriptionId, UUID customerId, UUID tenantId, OffsetDateTime processAt) {
        logger.info("Scheduling delivery creation for invoice {} subscription {} at {} (tenant: {})", 
                   invoiceId, subscriptionId, processAt, tenantId);
        
        // Create task payload
        Map<String, Object> payloadMap = Map.of(
            "invoiceId", invoiceId.toString(),
            "subscriptionId", subscriptionId.toString(),
            "customerId", customerId.toString(),
            "taskType", "CREATE_DELIVERY",
            "scheduledAt", processAt.toString()
        );
        
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize delivery task payload", e);
        }
        JSONB payload = JSONB.valueOf(payloadJson);
        
        // Create unique task key to prevent duplicates
        String taskKey = String.format("delivery_%s", invoiceId);
        
        ScheduledTasks task = new ScheduledTasks();
        task.setId(UUID.randomUUID());
        task.setTenantId(tenantId);
        task.setTaskType("CREATE_DELIVERY");
        task.setTaskKey(taskKey);
        task.setStatus("READY");
        task.setDueAt(processAt);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        task.setPayload(payload);
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        
        scheduledTasksDao.insert(task);
        
        logger.info("Successfully scheduled delivery creation task {} for invoice {} at {} (tenant: {})", 
                   task.getId(), invoiceId, processAt, tenantId);
        
        return task.getId();
    }
    
    /**
     * Schedule order creation for a delivery instance.
     * Creates a CREATE_ORDER task to create external orders via commerce adapter.
     */
    public UUID scheduleOrderCreation(UUID deliveryId, UUID subscriptionId, UUID invoiceId, UUID tenantId, OffsetDateTime processAt) {
        logger.info("Scheduling order creation for delivery {} at {} (tenant: {})", 
                   deliveryId, processAt, tenantId);
        
        // Create task payload
        Map<String, Object> payloadMap = Map.of(
            "deliveryId", deliveryId.toString(),
            "subscriptionId", subscriptionId.toString(),
            "invoiceId", invoiceId.toString(),
            "taskType", "CREATE_ORDER",
            "scheduledAt", processAt.toString()
        );
        
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order task payload", e);
        }
        JSONB payload = JSONB.valueOf(payloadJson);
        
        // Create unique task key to prevent duplicates
        String taskKey = String.format("order_%s", deliveryId);
        
        ScheduledTasks task = new ScheduledTasks();
        task.setId(UUID.randomUUID());
        task.setTenantId(tenantId);
        task.setTaskType("CREATE_ORDER");
        task.setTaskKey(taskKey);
        task.setStatus("READY");
        task.setDueAt(processAt);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        task.setPayload(payload);
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        
        scheduledTasksDao.insert(task);
        
        logger.info("Successfully scheduled order creation task {} for delivery {} at {} (tenant: {})", 
                   task.getId(), deliveryId, processAt, tenantId);
        
        return task.getId();
    }
    
    /**
     * Schedule entitlement grant for an invoice.
     * Creates an ENTITLEMENT_GRANT task to grant access to digital products.
     */
    public UUID scheduleEntitlementGrant(UUID invoiceId, UUID subscriptionId, UUID customerId, UUID tenantId, OffsetDateTime processAt) {
        logger.info("Scheduling entitlement grant for invoice {} subscription {} at {} (tenant: {})", 
                   invoiceId, subscriptionId, processAt, tenantId);
        
        // Create task payload
        Map<String, Object> payloadMap = Map.of(
            "invoiceId", invoiceId.toString(),
            "subscriptionId", subscriptionId.toString(),
            "customerId", customerId.toString(),
            "action", "GRANT",
            "taskType", "ENTITLEMENT_GRANT",
            "scheduledAt", processAt.toString()
        );
        
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize entitlement task payload", e);
        }
        JSONB payload = JSONB.valueOf(payloadJson);
        
        // Create unique task key to prevent duplicates
        String taskKey = String.format("entitlement_grant_%s", invoiceId);
        
        ScheduledTasks task = new ScheduledTasks();
        task.setId(UUID.randomUUID());
        task.setTenantId(tenantId);
        task.setTaskType("ENTITLEMENT_GRANT");
        task.setTaskKey(taskKey);
        task.setStatus("READY");
        task.setDueAt(processAt);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        task.setPayload(payload);
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        
        scheduledTasksDao.insert(task);
        
        logger.info("Successfully scheduled entitlement grant task {} for invoice {} at {} (tenant: {})", 
                   task.getId(), invoiceId, processAt, tenantId);
        
        return task.getId();
    }
}
