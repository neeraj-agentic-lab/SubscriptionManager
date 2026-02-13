package com.subscriptionengine.subscriptions.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.scheduler.service.ScheduledTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

import static com.subscriptionengine.generated.tables.Plans.PLANS;
import static com.subscriptionengine.generated.tables.SubscriptionItems.SUBSCRIPTION_ITEMS;
import static com.subscriptionengine.generated.tables.Subscriptions.SUBSCRIPTIONS;
import static com.subscriptionengine.generated.tables.ScheduledTasks.SCHEDULED_TASKS;

/**
 * Service for managing subscription lifecycle operations like pause, resume, and cancellation.
 * 
 * @author Neeraj Yadav
 */
@Service
public class SubscriptionManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionManagementService.class);
    
    private final DSLContext dsl;
    private final ScheduledTaskService scheduledTaskService;
    private final ObjectMapper objectMapper;
    private final SubscriptionHistoryService subscriptionHistoryService;
    
    public SubscriptionManagementService(DSLContext dsl, 
                                       ScheduledTaskService scheduledTaskService,
                                       ObjectMapper objectMapper,
                                       SubscriptionHistoryService subscriptionHistoryService) {
        this.dsl = dsl;
        this.scheduledTaskService = scheduledTaskService;
        this.objectMapper = objectMapper;
        this.subscriptionHistoryService = subscriptionHistoryService;
    }

    @Transactional
    public boolean modifySubscription(
        UUID subscriptionId,
        UUID customerId,
        UUID newPlanId,
        Integer newQuantity,
        Map<String, Object> newShippingAddress,
        String newPaymentMethodRef
    ) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = TenantContext.getTenantId();

        logger.info("[SUBSCRIPTION_MODIFY_START] RequestId: {} - Modifying subscription {} for customer {} in tenant {}",
            requestId, subscriptionId, customerId, tenantId);

        try {
            var subscription = dsl.select(
                    SUBSCRIPTIONS.ID,
                    SUBSCRIPTIONS.STATUS,
                    SUBSCRIPTIONS.CANCEL_AT_PERIOD_END,
                    SUBSCRIPTIONS.CURRENT_PERIOD_END,
                    SUBSCRIPTIONS.NEXT_RENEWAL_AT,
                    SUBSCRIPTIONS.PLAN_ID,
                    SUBSCRIPTIONS.CUSTOM_ATTRS,
                    SUBSCRIPTIONS.CUSTOMER_ID
                )
                .from(SUBSCRIPTIONS)
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
                .fetchOne();

            if (subscription == null) {
                logger.warn("[SUBSCRIPTION_MODIFY_NOT_FOUND] RequestId: {} - Subscription {} not found for customer {}",
                    requestId, subscriptionId, customerId);
                return false;
            }

            String status = subscription.get(SUBSCRIPTIONS.STATUS);
            Boolean cancelAtPeriodEnd = subscription.get(SUBSCRIPTIONS.CANCEL_AT_PERIOD_END);

            if (!Arrays.asList("ACTIVE", "PAUSED").contains(status)) {
                logger.warn("[SUBSCRIPTION_MODIFY_INVALID_STATUS] RequestId: {} - Cannot modify subscription {} with status {}",
                    requestId, subscriptionId, status);
                return false;
            }

            if (Boolean.TRUE.equals(cancelAtPeriodEnd)) {
                logger.warn("[SUBSCRIPTION_MODIFY_CANCEL_AT_PERIOD_END] RequestId: {} - Cannot modify subscription {} when cancel_at_period_end is true",
                    requestId, subscriptionId);
                return false;
            }

            UUID currentPlanId = subscription.get(SUBSCRIPTIONS.PLAN_ID);
            OffsetDateTime currentPeriodEnd = subscription.get(SUBSCRIPTIONS.CURRENT_PERIOD_END);
            OffsetDateTime nextRenewalAt = subscription.get(SUBSCRIPTIONS.NEXT_RENEWAL_AT);

            if (newPlanId != null) {
                boolean planValid = dsl.fetchExists(
                    dsl.selectOne()
                        .from(PLANS)
                        .where(PLANS.ID.eq(newPlanId))
                        .and(PLANS.TENANT_ID.eq(tenantId))
                );

                if (!planValid) {
                    logger.warn("[SUBSCRIPTION_MODIFY_INVALID_PLAN] RequestId: {} - Plan {} not found for tenant {}",
                        requestId, newPlanId, tenantId);
                    return false;
                }
            }

            Map<String, Object> customAttrs = new HashMap<>();
            try {
                if (subscription.get(SUBSCRIPTIONS.CUSTOM_ATTRS) != null) {
                    String existingJson = subscription.get(SUBSCRIPTIONS.CUSTOM_ATTRS).data();
                    if (existingJson != null && !existingJson.equals("{}")) {
                        customAttrs.putAll(objectMapper.readValue(existingJson, Map.class));
                    }
                }
            } catch (Exception ignored) {
            }

            customAttrs.put("modifiedAt", OffsetDateTime.now().toString());
            customAttrs.put("modifiedBy", "customer");
            if (newPlanId != null) {
                customAttrs.put("previousPlanId", currentPlanId != null ? currentPlanId.toString() : null);
                customAttrs.put("newPlanId", newPlanId.toString());
            }
            if (newQuantity != null) {
                customAttrs.put("newQuantity", newQuantity);
            }
            if (newPaymentMethodRef != null) {
                customAttrs.put("paymentMethodUpdated", true);
            }
            if (newShippingAddress != null) {
                customAttrs.put("shippingAddressUpdated", true);
            }

            var update = dsl.update(SUBSCRIPTIONS)
                .set(SUBSCRIPTIONS.CUSTOM_ATTRS, org.jooq.JSONB.valueOf(objectMapper.writeValueAsString(customAttrs)))
                .set(SUBSCRIPTIONS.UPDATED_AT, OffsetDateTime.now());

            if (newPlanId != null) {
                update = update.set(SUBSCRIPTIONS.PLAN_ID, newPlanId);
            }
            if (newPaymentMethodRef != null) {
                update = update.set(SUBSCRIPTIONS.PAYMENT_METHOD_REF, newPaymentMethodRef);
            }
            if (newShippingAddress != null) {
                update = update.set(SUBSCRIPTIONS.SHIPPING_ADDRESS, org.jooq.JSONB.valueOf(objectMapper.writeValueAsString(newShippingAddress)));
            }

            int updated = update
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .execute();
            if (updated == 0) {
                logger.error("[SUBSCRIPTION_MODIFY_UPDATE_FAILED] RequestId: {} - Failed to update subscription {}",
                    requestId, subscriptionId);
                return false;
            }

            if (newPlanId != null) {
                dsl.update(SUBSCRIPTION_ITEMS)
                    .set(SUBSCRIPTION_ITEMS.PLAN_ID, newPlanId)
                    .set(SUBSCRIPTION_ITEMS.UPDATED_AT, OffsetDateTime.now())
                    .where(SUBSCRIPTION_ITEMS.TENANT_ID.eq(tenantId))
                    .and(SUBSCRIPTION_ITEMS.SUBSCRIPTION_ID.eq(subscriptionId))
                    .execute();
            }

            if (newQuantity != null) {
                dsl.update(SUBSCRIPTION_ITEMS)
                    .set(SUBSCRIPTION_ITEMS.QUANTITY, newQuantity)
                    .set(SUBSCRIPTION_ITEMS.UPDATED_AT, OffsetDateTime.now())
                    .where(SUBSCRIPTION_ITEMS.TENANT_ID.eq(tenantId))
                    .and(SUBSCRIPTION_ITEMS.SUBSCRIPTION_ID.eq(subscriptionId))
                    .execute();
            }

            if (nextRenewalAt == null) {
                nextRenewalAt = currentPeriodEnd != null ? currentPeriodEnd.plusMonths(1) : OffsetDateTime.now().plusMonths(1);
                dsl.update(SUBSCRIPTIONS)
                    .set(SUBSCRIPTIONS.NEXT_RENEWAL_AT, nextRenewalAt)
                    .set(SUBSCRIPTIONS.UPDATED_AT, OffsetDateTime.now())
                    .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                    .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                    .execute();
            }

            scheduledTaskService.scheduleSubscriptionRenewal(subscriptionId, nextRenewalAt);

            logger.info("[SUBSCRIPTION_MODIFY_SUCCESS] RequestId: {} - Modified subscription {} (plan: {} -> {}, qty: {})",
                requestId,
                subscriptionId,
                currentPlanId != null ? currentPlanId : null,
                newPlanId != null ? newPlanId : currentPlanId,
                newQuantity);

            return true;
        } catch (Exception e) {
            logger.error("[SUBSCRIPTION_MODIFY_ERROR] RequestId: {} - Error modifying subscription {}: {}",
                requestId, subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to modify subscription", e);
        }
    }

    @Transactional
    public boolean cancelSubscription(UUID subscriptionId, UUID customerId, boolean immediate, String reason) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = TenantContext.getTenantId();

        logger.info("[SUBSCRIPTION_CANCEL_START] RequestId: {} - Cancelling subscription {} for customer {} (immediate: {}) in tenant {}",
            requestId, subscriptionId, customerId, immediate, tenantId);

        try {
            logger.debug("[SUBSCRIPTION_CANCEL_STEP_1] RequestId: {} - Validating subscription can be cancelled", requestId);

            var subscription = dsl.select(
                    SUBSCRIPTIONS.ID,
                    SUBSCRIPTIONS.STATUS,
                    SUBSCRIPTIONS.CURRENT_PERIOD_END,
                    SUBSCRIPTIONS.CUSTOM_ATTRS,
                    SUBSCRIPTIONS.CUSTOMER_ID
                )
                .from(SUBSCRIPTIONS)
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
                .fetchOne();

            if (subscription == null) {
                logger.warn("[SUBSCRIPTION_CANCEL_NOT_FOUND] RequestId: {} - Subscription {} not found for customer {}",
                    requestId, subscriptionId, customerId);
                return false;
            }

            String currentStatus = subscription.get(SUBSCRIPTIONS.STATUS);
            if (!Arrays.asList("ACTIVE", "PAUSED").contains(currentStatus)) {
                logger.warn("[SUBSCRIPTION_CANCEL_INVALID_STATUS] RequestId: {} - Cannot cancel subscription {} with status {}",
                    requestId, subscriptionId, currentStatus);
                return false;
            }

            logger.debug("[SUBSCRIPTION_CANCEL_STEP_2] RequestId: {} - Cancelling scheduled tasks", requestId);

            String subscriptionRenewalTaskKey = String.format("subscription_renewal_%s", subscriptionId);

            int cancelledTasks = dsl.update(SCHEDULED_TASKS)
                .set(SCHEDULED_TASKS.STATUS, "FAILED")
                .set(SCHEDULED_TASKS.COMPLETED_AT, OffsetDateTime.now())
                .set(SCHEDULED_TASKS.LAST_ERROR, "Subscription cancelled by customer")
                .set(SCHEDULED_TASKS.UPDATED_AT, OffsetDateTime.now())
                .where(SCHEDULED_TASKS.TENANT_ID.eq(tenantId))
                .and(SCHEDULED_TASKS.STATUS.in("READY", "CLAIMED"))
                .and(
                    SCHEDULED_TASKS.TASK_KEY.eq(subscriptionRenewalTaskKey)
                        .or(
                            SCHEDULED_TASKS.TASK_TYPE.eq("PRODUCT_RENEWAL")
                                .and(SCHEDULED_TASKS.PAYLOAD.cast(String.class)
                                    .like("%\"subscriptionId\":\"" + subscriptionId + "\"%"))
                        )
                )
                .execute();

            logger.info("[SUBSCRIPTION_CANCEL_STEP_2_SUCCESS] RequestId: {} - Cancelled {} scheduled tasks",
                requestId, cancelledTasks);

            logger.debug("[SUBSCRIPTION_CANCEL_STEP_3] RequestId: {} - Updating subscription cancellation fields", requestId);

            Map<String, Object> customAttrs = new HashMap<>();
            try {
                if (subscription.get(SUBSCRIPTIONS.CUSTOM_ATTRS) != null) {
                    String existingJson = subscription.get(SUBSCRIPTIONS.CUSTOM_ATTRS).data();
                    if (existingJson != null && !existingJson.equals("{}")) {
                        customAttrs.putAll(objectMapper.readValue(existingJson, Map.class));
                    }
                }
            } catch (Exception ignored) {
            }

            customAttrs.put("cancellationRequestedAt", OffsetDateTime.now().toString());
            customAttrs.put("cancellationReason", reason);
            customAttrs.put("cancellationType", immediate ? "IMMEDIATE" : "END_OF_PERIOD");
            customAttrs.put("cancelledBy", "customer");

            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime currentPeriodEnd = subscription.get(SUBSCRIPTIONS.CURRENT_PERIOD_END);

            if (immediate) {
                int updated = dsl.update(SUBSCRIPTIONS)
                    .set(SUBSCRIPTIONS.STATUS, "CANCELED")
                    .set(SUBSCRIPTIONS.CANCEL_AT_PERIOD_END, false)
                    .set(SUBSCRIPTIONS.CANCELED_AT, now)
                    .set(SUBSCRIPTIONS.CANCELLATION_REASON, reason)
                    .set(SUBSCRIPTIONS.NEXT_RENEWAL_AT, (OffsetDateTime) null)
                    .set(SUBSCRIPTIONS.CUSTOM_ATTRS, org.jooq.JSONB.valueOf(objectMapper.writeValueAsString(customAttrs)))
                    .set(SUBSCRIPTIONS.UPDATED_AT, now)
                    .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                    .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                    .execute();

                if (updated == 0) {
                    logger.error("[SUBSCRIPTION_CANCEL_STEP_3_ERROR] RequestId: {} - Failed to update subscription cancellation", requestId);
                    return false;
                }

                logger.info("[SUBSCRIPTION_CANCEL_SUCCESS] RequestId: {} - Immediately cancelled subscription {}",
                    requestId, subscriptionId);
                
                // Record cancellation in history
                subscriptionHistoryService.recordCancellation(tenantId, subscriptionId, null, "CUSTOMER", reason);
            } else {
                int updated = dsl.update(SUBSCRIPTIONS)
                    .set(SUBSCRIPTIONS.CANCEL_AT_PERIOD_END, true)
                    .set(SUBSCRIPTIONS.CANCELED_AT, (OffsetDateTime) null)
                    .set(SUBSCRIPTIONS.CANCELLATION_REASON, reason)
                    .set(SUBSCRIPTIONS.NEXT_RENEWAL_AT, (OffsetDateTime) null)
                    .set(SUBSCRIPTIONS.CUSTOM_ATTRS, org.jooq.JSONB.valueOf(objectMapper.writeValueAsString(customAttrs)))
                    .set(SUBSCRIPTIONS.UPDATED_AT, now)
                    .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                    .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                    .execute();

                if (updated == 0) {
                    logger.error("[SUBSCRIPTION_CANCEL_STEP_3_ERROR] RequestId: {} - Failed to update subscription cancellation", requestId);
                    return false;
                }

                logger.info("[SUBSCRIPTION_CANCEL_SUCCESS] RequestId: {} - Marked subscription {} to cancel at period end {}",
                    requestId, subscriptionId, currentPeriodEnd);
                
                // Record cancellation in history
                subscriptionHistoryService.recordCancellation(tenantId, subscriptionId, null, "CUSTOMER", reason);
            }

            return true;

        } catch (Exception e) {
            logger.error("[SUBSCRIPTION_CANCEL_ERROR] RequestId: {} - Error cancelling subscription {}: {}",
                requestId, subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel subscription", e);
        }
    }
    
    /**
     * Pause a subscription - stops billing and deliveries until resumed.
     * 
     * @param subscriptionId the subscription ID to pause
     * @param customerId the customer ID for authorization
     * @param reason optional reason for pausing
     * @return true if successfully paused
     */
    @Transactional
    public boolean pauseSubscription(UUID subscriptionId, UUID customerId, String reason) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = TenantContext.getTenantId();
        
        logger.info("[SUBSCRIPTION_PAUSE_START] RequestId: {} - Pausing subscription {} for customer {} in tenant {}", 
                   requestId, subscriptionId, customerId, tenantId);
        
        try {
            // 1. Validate subscription exists and belongs to customer
            logger.debug("[SUBSCRIPTION_PAUSE_STEP_1] RequestId: {} - Validating subscription ownership", requestId);
            
            var subscription = dsl.select(
                    SUBSCRIPTIONS.ID,
                    SUBSCRIPTIONS.STATUS,
                    SUBSCRIPTIONS.NEXT_RENEWAL_AT,
                    SUBSCRIPTIONS.CUSTOMER_ID
                )
                .from(SUBSCRIPTIONS)
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
                .fetchOne();
            
            if (subscription == null) {
                logger.warn("[SUBSCRIPTION_PAUSE_NOT_FOUND] RequestId: {} - Subscription {} not found for customer {}", 
                           requestId, subscriptionId, customerId);
                return false;
            }
            
            String currentStatus = subscription.get(SUBSCRIPTIONS.STATUS);
            if (!"ACTIVE".equals(currentStatus)) {
                logger.warn("[SUBSCRIPTION_PAUSE_INVALID_STATUS] RequestId: {} - Cannot pause subscription {} with status {}", 
                           requestId, subscriptionId, currentStatus);
                return false;
            }
            
            logger.debug("[SUBSCRIPTION_PAUSE_STEP_1_SUCCESS] RequestId: {} - Subscription validation passed", requestId);
            
            // 2. Cancel any pending renewal tasks
            logger.debug("[SUBSCRIPTION_PAUSE_STEP_2] RequestId: {} - Cancelling pending renewal tasks", requestId);
            
            int cancelledTasks = dsl.update(SCHEDULED_TASKS)
                .set(SCHEDULED_TASKS.STATUS, "FAILED")
                .set(SCHEDULED_TASKS.COMPLETED_AT, OffsetDateTime.now())
                .set(SCHEDULED_TASKS.LAST_ERROR, "Subscription paused by customer")
                .set(SCHEDULED_TASKS.UPDATED_AT, OffsetDateTime.now())
                .where(SCHEDULED_TASKS.TENANT_ID.eq(tenantId))
                .and(SCHEDULED_TASKS.TASK_TYPE.eq("PRODUCT_RENEWAL"))
                .and(SCHEDULED_TASKS.STATUS.in("READY", "CLAIMED"))
                .and(SCHEDULED_TASKS.PAYLOAD.cast(String.class).like("%\"subscriptionId\":\"" + subscriptionId + "\"%"))
                .execute();
            
            logger.info("[SUBSCRIPTION_PAUSE_STEP_2_SUCCESS] RequestId: {} - Cancelled {} pending renewal tasks", 
                       requestId, cancelledTasks);
            
            // 3. Update subscription status to PAUSED
            logger.debug("[SUBSCRIPTION_PAUSE_STEP_3] RequestId: {} - Updating subscription status to PAUSED", requestId);
            
            Map<String, Object> customAttrs = new HashMap<>();
            customAttrs.put("pausedAt", OffsetDateTime.now().toString());
            customAttrs.put("pauseReason", reason != null ? reason : "Customer requested pause");
            customAttrs.put("pausedBy", "customer");
            
            int updatedRows = dsl.update(SUBSCRIPTIONS)
                .set(SUBSCRIPTIONS.STATUS, "PAUSED")
                .set(SUBSCRIPTIONS.NEXT_RENEWAL_AT, (OffsetDateTime) null)
                .set(SUBSCRIPTIONS.CUSTOM_ATTRS, org.jooq.JSONB.valueOf(objectMapper.writeValueAsString(customAttrs)))
                .set(SUBSCRIPTIONS.UPDATED_AT, OffsetDateTime.now())
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .execute();
            
            if (updatedRows == 0) {
                logger.error("[SUBSCRIPTION_PAUSE_STEP_3_ERROR] RequestId: {} - Failed to update subscription status", requestId);
                return false;
            }
            
            logger.info("[SUBSCRIPTION_PAUSE_SUCCESS] RequestId: {} - Successfully paused subscription {}", 
                       requestId, subscriptionId);
            
            // Record pause action in history
            subscriptionHistoryService.recordPause(tenantId, subscriptionId, null, "CUSTOMER", 
                reason != null ? reason : "Customer requested pause");
            
            // TODO: Emit outbox event for subscription.paused
            
            return true;
            
        } catch (Exception e) {
            logger.error("[SUBSCRIPTION_PAUSE_ERROR] RequestId: {} - Error pausing subscription {}: {}", 
                        requestId, subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to pause subscription", e);
        }
    }
    
    /**
     * Resume a paused subscription - restarts billing and deliveries.
     * 
     * @param subscriptionId the subscription ID to resume
     * @param customerId the customer ID for authorization
     * @return true if successfully resumed
     */
    @Transactional
    public boolean resumeSubscription(UUID subscriptionId, UUID customerId) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = TenantContext.getTenantId();
        
        logger.info("[SUBSCRIPTION_RESUME_START] RequestId: {} - Resuming subscription {} for customer {} in tenant {}", 
                   requestId, subscriptionId, customerId, tenantId);
        
        try {
            // 1. Validate subscription exists and is paused
            logger.debug("[SUBSCRIPTION_RESUME_STEP_1] RequestId: {} - Validating subscription can be resumed", requestId);
            
            var subscription = dsl.select(
                    SUBSCRIPTIONS.ID,
                    SUBSCRIPTIONS.STATUS,
                    SUBSCRIPTIONS.CURRENT_PERIOD_END,
                    SUBSCRIPTIONS.CUSTOMER_ID,
                    SUBSCRIPTIONS.PLAN_ID
                )
                .from(SUBSCRIPTIONS)
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
                .fetchOne();
            
            if (subscription == null) {
                logger.warn("[SUBSCRIPTION_RESUME_NOT_FOUND] RequestId: {} - Subscription {} not found for customer {}", 
                           requestId, subscriptionId, customerId);
                return false;
            }
            
            String currentStatus = subscription.get(SUBSCRIPTIONS.STATUS);
            if (!"PAUSED".equals(currentStatus)) {
                logger.warn("[SUBSCRIPTION_RESUME_INVALID_STATUS] RequestId: {} - Cannot resume subscription {} with status {}", 
                           requestId, subscriptionId, currentStatus);
                return false;
            }
            
            logger.debug("[SUBSCRIPTION_RESUME_STEP_1_SUCCESS] RequestId: {} - Subscription validation passed", requestId);
            
            // 2. Calculate next renewal date (extend from current period end)
            logger.debug("[SUBSCRIPTION_RESUME_STEP_2] RequestId: {} - Calculating next renewal date", requestId);
            
            OffsetDateTime currentPeriodEnd = subscription.get(SUBSCRIPTIONS.CURRENT_PERIOD_END);
            OffsetDateTime nextRenewalAt = currentPeriodEnd.plusMonths(1); // Default to monthly, should be configurable
            
            // 3. Update subscription status to ACTIVE and set next renewal
            logger.debug("[SUBSCRIPTION_RESUME_STEP_3] RequestId: {} - Updating subscription status to ACTIVE", requestId);
            
            Map<String, Object> customAttrs = new HashMap<>();
            customAttrs.put("resumedAt", OffsetDateTime.now().toString());
            customAttrs.put("resumedBy", "customer");
            
            int updatedRows = dsl.update(SUBSCRIPTIONS)
                .set(SUBSCRIPTIONS.STATUS, "ACTIVE")
                .set(SUBSCRIPTIONS.NEXT_RENEWAL_AT, nextRenewalAt)
                .set(SUBSCRIPTIONS.CUSTOM_ATTRS, org.jooq.JSONB.valueOf(objectMapper.writeValueAsString(customAttrs)))
                .set(SUBSCRIPTIONS.UPDATED_AT, OffsetDateTime.now())
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .execute();
            
            if (updatedRows == 0) {
                logger.error("[SUBSCRIPTION_RESUME_STEP_3_ERROR] RequestId: {} - Failed to update subscription status", requestId);
                return false;
            }
            
            logger.debug("[SUBSCRIPTION_RESUME_STEP_3_SUCCESS] RequestId: {} - Updated subscription status", requestId);
            
            // 4. Schedule next renewal task
            logger.debug("[SUBSCRIPTION_RESUME_STEP_4] RequestId: {} - Scheduling next renewal task", requestId);
            
            scheduledTaskService.scheduleSubscriptionRenewal(subscriptionId, nextRenewalAt);
            
            logger.info("[SUBSCRIPTION_RESUME_SUCCESS] RequestId: {} - Successfully resumed subscription {} with next renewal at {}", 
                       requestId, subscriptionId, nextRenewalAt);
            
            // Record resume action in history
            subscriptionHistoryService.recordResume(tenantId, subscriptionId, null, "CUSTOMER");
            
            // TODO: Emit outbox event for subscription.resumed
            
            return true;
            
        } catch (Exception e) {
            logger.error("[SUBSCRIPTION_RESUME_ERROR] RequestId: {} - Error resuming subscription {}: {}", 
                        requestId, subscriptionId, e.getMessage(), e);
            throw new RuntimeException("Failed to resume subscription", e);
        }
    }
    
    /**
     * Get subscription management details including pause/resume eligibility.
     * 
     * @param subscriptionId the subscription ID
     * @param customerId the customer ID for authorization
     * @return subscription management details
     */
    public Optional<Map<String, Object>> getSubscriptionManagementDetails(UUID subscriptionId, UUID customerId) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = TenantContext.getTenantId();
        
        logger.info("[SUBSCRIPTION_MGMT_DETAILS_START] RequestId: {} - Getting management details for subscription {} customer {}", 
                   requestId, subscriptionId, customerId);
        
        try {
            var subscription = dsl.select(
                    SUBSCRIPTIONS.ID,
                    SUBSCRIPTIONS.STATUS,
                    SUBSCRIPTIONS.CURRENT_PERIOD_START,
                    SUBSCRIPTIONS.CURRENT_PERIOD_END,
                    SUBSCRIPTIONS.NEXT_RENEWAL_AT,
                    SUBSCRIPTIONS.CANCEL_AT_PERIOD_END,
                    SUBSCRIPTIONS.CANCELED_AT,
                    SUBSCRIPTIONS.CUSTOM_ATTRS,
                    SUBSCRIPTIONS.CREATED_AT,
                    SUBSCRIPTIONS.UPDATED_AT
                )
                .from(SUBSCRIPTIONS)
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
                .fetchOne();
            
            if (subscription == null) {
                logger.warn("[SUBSCRIPTION_MGMT_DETAILS_NOT_FOUND] RequestId: {} - Subscription {} not found for customer {}", 
                           requestId, subscriptionId, customerId);
                return Optional.empty();
            }
            
            String status = subscription.get(SUBSCRIPTIONS.STATUS);
            
            Map<String, Object> result = new HashMap<>();
            result.put("subscriptionId", subscriptionId.toString());
            result.put("status", status);
            result.put("currentPeriodStart", subscription.get(SUBSCRIPTIONS.CURRENT_PERIOD_START).toString());
            result.put("currentPeriodEnd", subscription.get(SUBSCRIPTIONS.CURRENT_PERIOD_END).toString());
            result.put("nextRenewalAt", subscription.get(SUBSCRIPTIONS.NEXT_RENEWAL_AT) != null ? 
                      subscription.get(SUBSCRIPTIONS.NEXT_RENEWAL_AT).toString() : null);
            result.put("cancelAtPeriodEnd", subscription.get(SUBSCRIPTIONS.CANCEL_AT_PERIOD_END));
            result.put("cancelledAt", subscription.get(SUBSCRIPTIONS.CANCELED_AT) != null ? 
                      subscription.get(SUBSCRIPTIONS.CANCELED_AT).toString() : null);
            result.put("createdAt", subscription.get(SUBSCRIPTIONS.CREATED_AT).toString());
            result.put("updatedAt", subscription.get(SUBSCRIPTIONS.UPDATED_AT).toString());
            
            // Management capabilities
            result.put("canPause", "ACTIVE".equals(status));
            result.put("canResume", "PAUSED".equals(status));
            result.put("canCancel", Arrays.asList("ACTIVE", "PAUSED").contains(status));
            
            // Parse custom attributes for pause/resume history
            String customAttrsJson = subscription.get(SUBSCRIPTIONS.CUSTOM_ATTRS).data();
            if (customAttrsJson != null && !customAttrsJson.equals("{}")) {
                Map<String, Object> customAttrs = objectMapper.readValue(customAttrsJson, Map.class);
                result.put("managementHistory", customAttrs);
            }
            
            logger.info("[SUBSCRIPTION_MGMT_DETAILS_SUCCESS] RequestId: {} - Retrieved management details for subscription {}", 
                       requestId, subscriptionId);
            
            return Optional.of(result);
            
        } catch (Exception e) {
            logger.error("[SUBSCRIPTION_MGMT_DETAILS_ERROR] RequestId: {} - Error getting management details for subscription {}: {}", 
                        requestId, subscriptionId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}
