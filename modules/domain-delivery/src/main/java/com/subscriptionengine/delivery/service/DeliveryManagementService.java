package com.subscriptionengine.delivery.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.pojos.DeliveryInstances;
import com.subscriptionengine.generated.tables.pojos.ScheduledTasks;
import com.subscriptionengine.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.DeliveryInstances.DELIVERY_INSTANCES;
import static com.subscriptionengine.generated.tables.ScheduledTasks.SCHEDULED_TASKS;
import static com.subscriptionengine.generated.tables.Subscriptions.SUBSCRIPTIONS;

/**
 * Service for managing customer delivery instances.
 * Handles viewing upcoming deliveries and cancellation logic.
 * 
 * @author Neeraj Yadav
 */
@Service
public class DeliveryManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeliveryManagementService.class);
    
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;
    
    public DeliveryManagementService(DSLContext dsl, ObjectMapper objectMapper, OutboxService outboxService) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
    }
    
    /**
     * Get upcoming deliveries for a customer.
     * Returns deliveries that are not yet cancelled or completed.
     */
    public List<Map<String, Object>> getUpcomingDeliveries(UUID customerId, int limit) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("[DELIVERY_UPCOMING_START] RequestId: {} - Getting upcoming deliveries for customer {} (tenant: {})", 
                   requestId, customerId, tenantId);
        
        try {
            var deliveries = dsl.select(
                    DELIVERY_INSTANCES.ID,
                    DELIVERY_INSTANCES.SUBSCRIPTION_ID,
                    DELIVERY_INSTANCES.CYCLE_KEY,
                    DELIVERY_INSTANCES.STATUS,
                    DELIVERY_INSTANCES.DELIVERY_TYPE,
                    DELIVERY_INSTANCES.SNAPSHOT,
                    DELIVERY_INSTANCES.EXTERNAL_ORDER_REF,
                    DELIVERY_INSTANCES.CREATED_AT,
                    DELIVERY_INSTANCES.UPDATED_AT
                )
                .from(DELIVERY_INSTANCES)
                .join(SUBSCRIPTIONS).on(SUBSCRIPTIONS.ID.eq(DELIVERY_INSTANCES.SUBSCRIPTION_ID))
                .where(DELIVERY_INSTANCES.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
                .and(DELIVERY_INSTANCES.STATUS.in("PENDING", "PROCESSING"))
                .orderBy(DELIVERY_INSTANCES.CREATED_AT.desc())
                .limit(limit)
                .fetch();
            
            List<Map<String, Object>> result = deliveries.stream()
                .map(record -> {
                    try {
                        Map<String, Object> snapshot = objectMapper.readValue(
                            record.get(DELIVERY_INSTANCES.SNAPSHOT).data(), Map.class);
                        
                        Map<String, Object> deliveryMap = new java.util.HashMap<>();
                        deliveryMap.put("deliveryId", record.get(DELIVERY_INSTANCES.ID).toString());
                        deliveryMap.put("subscriptionId", record.get(DELIVERY_INSTANCES.SUBSCRIPTION_ID).toString());
                        deliveryMap.put("cycleKey", record.get(DELIVERY_INSTANCES.CYCLE_KEY));
                        deliveryMap.put("status", record.get(DELIVERY_INSTANCES.STATUS));
                        deliveryMap.put("deliveryType", record.get(DELIVERY_INSTANCES.DELIVERY_TYPE));
                        deliveryMap.put("externalOrderRef", record.get(DELIVERY_INSTANCES.EXTERNAL_ORDER_REF));
                        deliveryMap.put("canCancel", canCancelDelivery(record.get(DELIVERY_INSTANCES.STATUS)));
                        deliveryMap.put("snapshot", snapshot);
                        deliveryMap.put("createdAt", record.get(DELIVERY_INSTANCES.CREATED_AT).toString());
                        deliveryMap.put("updatedAt", record.get(DELIVERY_INSTANCES.UPDATED_AT).toString());
                        return deliveryMap;
                    } catch (Exception e) {
                        logger.error("[DELIVERY_UPCOMING_ERROR] RequestId: {} - Failed to parse snapshot for delivery {}: {}", 
                                   requestId, record.get(DELIVERY_INSTANCES.ID), e.getMessage());
                        Map<String, Object> errorMap = new java.util.HashMap<>();
                        errorMap.put("deliveryId", record.get(DELIVERY_INSTANCES.ID).toString());
                        errorMap.put("status", record.get(DELIVERY_INSTANCES.STATUS));
                        errorMap.put("error", "Failed to parse delivery details");
                        return errorMap;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
            
            logger.info("[DELIVERY_UPCOMING_SUCCESS] RequestId: {} - Found {} upcoming deliveries for customer {}", 
                       requestId, result.size(), customerId);
            
            return result;
            
        } catch (Exception e) {
            logger.error("[DELIVERY_UPCOMING_ERROR] RequestId: {} - Error getting upcoming deliveries for customer {}: {}", 
                        requestId, customerId, e.getMessage(), e);
            throw new RuntimeException("Failed to get upcoming deliveries", e);
        }
    }
    
    /**
     * Cancel a specific delivery instance.
     * Only allows cancellation if the delivery hasn't started order processing.
     */
    @Transactional
    public boolean cancelDelivery(UUID deliveryId, UUID customerId, String reason) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("[DELIVERY_CANCEL_START] RequestId: {} - Cancelling delivery {} for customer {} (tenant: {})", 
                   requestId, deliveryId, customerId, tenantId);
        
        try {
            // 1. Load and validate delivery instance
            logger.debug("[DELIVERY_CANCEL_STEP_1] RequestId: {} - Loading delivery instance", requestId);
            
            var delivery = dsl.select(
                    DELIVERY_INSTANCES.ID,
                    DELIVERY_INSTANCES.SUBSCRIPTION_ID,
                    DELIVERY_INSTANCES.STATUS,
                    DELIVERY_INSTANCES.EXTERNAL_ORDER_REF,
                    SUBSCRIPTIONS.CUSTOMER_ID
                )
                .from(DELIVERY_INSTANCES)
                .join(SUBSCRIPTIONS).on(SUBSCRIPTIONS.ID.eq(DELIVERY_INSTANCES.SUBSCRIPTION_ID))
                .where(DELIVERY_INSTANCES.ID.eq(deliveryId))
                .and(DELIVERY_INSTANCES.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
                .fetchOne();
            
            if (delivery == null) {
                logger.warn("[DELIVERY_CANCEL_NOT_FOUND] RequestId: {} - Delivery {} not found for customer {}", 
                           requestId, deliveryId, customerId);
                return false;
            }
            
            String currentStatus = delivery.get(DELIVERY_INSTANCES.STATUS);
            String externalOrderRef = delivery.get(DELIVERY_INSTANCES.EXTERNAL_ORDER_REF);
            UUID subscriptionId = delivery.get(DELIVERY_INSTANCES.SUBSCRIPTION_ID);
            
            logger.info("[DELIVERY_CANCEL_STEP_1_SUCCESS] RequestId: {} - Loaded delivery: status={}, orderRef={}", 
                       requestId, currentStatus, externalOrderRef);
            
            // 2. Validate cancellation is allowed
            logger.debug("[DELIVERY_CANCEL_STEP_2] RequestId: {} - Validating cancellation eligibility", requestId);
            
            if (!canCancelDelivery(currentStatus)) {
                logger.warn("[DELIVERY_CANCEL_NOT_ALLOWED] RequestId: {} - Cannot cancel delivery {} with status {}", 
                           requestId, deliveryId, currentStatus);
                return false;
            }
            
            if (externalOrderRef != null && !externalOrderRef.trim().isEmpty()) {
                logger.warn("[DELIVERY_CANCEL_ORDER_EXISTS] RequestId: {} - Cannot cancel delivery {} - order already created: {}", 
                           requestId, deliveryId, externalOrderRef);
                return false;
            }
            
            logger.info("[DELIVERY_CANCEL_STEP_2_SUCCESS] RequestId: {} - Cancellation validation passed", requestId);
            
            // 3. Cancel any pending CREATE_ORDER tasks for this delivery
            logger.debug("[DELIVERY_CANCEL_STEP_3] RequestId: {} - Cancelling pending CREATE_ORDER tasks", requestId);
            
            int cancelledTasks = dsl.update(SCHEDULED_TASKS)
                .set(SCHEDULED_TASKS.STATUS, "CANCELLED")
                .set(SCHEDULED_TASKS.LAST_ERROR, "Delivery cancelled by customer: " + reason)
                .set(SCHEDULED_TASKS.UPDATED_AT, OffsetDateTime.now())
                .where(SCHEDULED_TASKS.TENANT_ID.eq(tenantId))
                .and(SCHEDULED_TASKS.TASK_TYPE.eq("CREATE_ORDER"))
                .and(SCHEDULED_TASKS.STATUS.in("READY", "CLAIMED"))
                .and(SCHEDULED_TASKS.PAYLOAD.cast(String.class).like("%\"deliveryId\":\"" + deliveryId + "\"%"))
                .execute();
            
            logger.info("[DELIVERY_CANCEL_STEP_3_SUCCESS] RequestId: {} - Cancelled {} pending CREATE_ORDER tasks", 
                       requestId, cancelledTasks);
            
            // 4. Update delivery instance status to CANCELLED
            logger.debug("[DELIVERY_CANCEL_STEP_4] RequestId: {} - Updating delivery status to CANCELLED", requestId);
            
            int updatedRows = dsl.update(DELIVERY_INSTANCES)
                .set(DELIVERY_INSTANCES.STATUS, "CANCELED")
                .set(DELIVERY_INSTANCES.CANCELLATION_REASON, reason)
                .set(DELIVERY_INSTANCES.CANCELLED_AT, OffsetDateTime.now())
                .set(DELIVERY_INSTANCES.UPDATED_AT, OffsetDateTime.now())
                .where(DELIVERY_INSTANCES.ID.eq(deliveryId))
                .and(DELIVERY_INSTANCES.TENANT_ID.eq(tenantId))
                .execute();
            
            if (updatedRows == 0) {
                logger.error("[DELIVERY_CANCEL_STEP_4_ERROR] RequestId: {} - Failed to update delivery status - no rows affected", requestId);
                return false;
            }
            
            logger.info("[DELIVERY_CANCEL_STEP_4_SUCCESS] RequestId: {} - Updated delivery status to CANCELED: {} rows affected", 
                       requestId, updatedRows);
            
            // 5. Emit outbox event for delivery cancellation
            logger.debug("[DELIVERY_CANCEL_STEP_5] RequestId: {} - Emitting outbox event: delivery.canceled", requestId);
            
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("deliveryId", deliveryId.toString());
            eventPayload.put("customerId", customerId.toString());
            eventPayload.put("subscriptionId", subscriptionId.toString());
            eventPayload.put("reason", reason);
            eventPayload.put("canceledAt", OffsetDateTime.now().toString());
            
            outboxService.emitEvent("delivery.canceled", eventPayload, "delivery_" + deliveryId);
            
            logger.info("[DELIVERY_CANCEL_STEP_5_SUCCESS] RequestId: {} - Emitted outbox event: delivery.canceled", requestId);
            
            logger.info("[DELIVERY_CANCEL_SUCCESS] RequestId: {} - Successfully cancelled delivery {} for customer {}", 
                       requestId, deliveryId, customerId);
            
            return true;
            
        } catch (Exception e) {
            logger.error("[DELIVERY_CANCEL_ERROR] RequestId: {} - Error cancelling delivery {} for customer {}: {}", 
                        requestId, deliveryId, customerId, e.getMessage(), e);
            throw new RuntimeException("Failed to cancel delivery", e);
        }
    }
    
    /**
     * Get details of a specific delivery instance.
     */
    public Optional<Map<String, Object>> getDeliveryDetails(UUID deliveryId, UUID customerId) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("[DELIVERY_DETAILS_START] RequestId: {} - Getting delivery details {} for customer {} (tenant: {})", 
                   requestId, deliveryId, customerId, tenantId);
        
        try {
            var delivery = dsl.select()
                .from(DELIVERY_INSTANCES)
                .join(SUBSCRIPTIONS).on(SUBSCRIPTIONS.ID.eq(DELIVERY_INSTANCES.SUBSCRIPTION_ID))
                .where(DELIVERY_INSTANCES.ID.eq(deliveryId))
                .and(DELIVERY_INSTANCES.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
                .fetchOne();
            
            if (delivery == null) {
                logger.warn("[DELIVERY_DETAILS_NOT_FOUND] RequestId: {} - Delivery {} not found for customer {}", 
                           requestId, deliveryId, customerId);
                return Optional.empty();
            }
            
            Map<String, Object> snapshot = objectMapper.readValue(
                delivery.get(DELIVERY_INSTANCES.SNAPSHOT).data(), Map.class);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("deliveryId", delivery.get(DELIVERY_INSTANCES.ID).toString());
            result.put("subscriptionId", delivery.get(DELIVERY_INSTANCES.SUBSCRIPTION_ID).toString());
            result.put("cycleKey", delivery.get(DELIVERY_INSTANCES.CYCLE_KEY));
            result.put("status", delivery.get(DELIVERY_INSTANCES.STATUS));
            result.put("deliveryType", delivery.get(DELIVERY_INSTANCES.DELIVERY_TYPE));
            result.put("externalOrderRef", delivery.get(DELIVERY_INSTANCES.EXTERNAL_ORDER_REF));
            result.put("canCancel", canCancelDelivery(delivery.get(DELIVERY_INSTANCES.STATUS)));
            result.put("cancellationReason", delivery.get(DELIVERY_INSTANCES.CANCELLATION_REASON));
            result.put("cancelledAt", delivery.get(DELIVERY_INSTANCES.CANCELLED_AT));
            result.put("snapshot", snapshot);
            result.put("createdAt", delivery.get(DELIVERY_INSTANCES.CREATED_AT).toString());
            result.put("updatedAt", delivery.get(DELIVERY_INSTANCES.UPDATED_AT).toString());
            
            logger.info("[DELIVERY_DETAILS_SUCCESS] RequestId: {} - Retrieved delivery details for {}", 
                       requestId, deliveryId);
            
            return Optional.of(result);
            
        } catch (Exception e) {
            logger.error("[DELIVERY_DETAILS_ERROR] RequestId: {} - Error getting delivery details {} for customer {}: {}", 
                        requestId, deliveryId, customerId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Check if a delivery can be cancelled based on its current status.
     */
    private boolean canCancelDelivery(String status) {
        // Can only cancel deliveries that are PENDING (not yet processed)
        // Cannot cancel if already PROCESSING, ORDER_CREATED, SHIPPED, DELIVERED, or CANCELLED
        return "PENDING".equals(status);
    }
}
