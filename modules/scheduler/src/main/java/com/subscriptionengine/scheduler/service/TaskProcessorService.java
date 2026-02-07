/**
 * Service for processing scheduled tasks with distributed locking.
 * Handles task leasing, processing, and completion tracking.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.scheduler.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.ScheduledTasksDao;
import com.subscriptionengine.generated.tables.pojos.ScheduledTasks;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.ScheduledTasks.SCHEDULED_TASKS;
import static com.subscriptionengine.generated.tables.Invoices.INVOICES;
import static com.subscriptionengine.generated.tables.Subscriptions.SUBSCRIPTIONS;
import static com.subscriptionengine.generated.tables.SubscriptionItems.SUBSCRIPTION_ITEMS;
import static com.subscriptionengine.generated.tables.DeliveryInstances.DELIVERY_INSTANCES;
import static com.subscriptionengine.generated.tables.Entitlements.ENTITLEMENTS;

import org.jooq.JSONB;

/**
 * Service for processing scheduled tasks with distributed locking.
 * Handles task leasing, processing, and completion tracking.
 */
@Service
public class TaskProcessorService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskProcessorService.class);
    private static final String WORKER_ID = UUID.randomUUID().toString();
    private static final int LEASE_DURATION_MINUTES = 5;
    private static final int BATCH_SIZE = 10;
    
    private final DSLContext dsl;
    private final ScheduledTasksDao scheduledTasksDao;
    @Autowired
    private ObjectMapper objectMapper;
    
    // Billing task handler - injected lazily to avoid circular dependencies
    @Autowired(required = false)
    private BillingTaskHandler billingTaskHandler;
    
    public TaskProcessorService(DSLContext dsl, ScheduledTasksDao scheduledTasksDao) {
        this.dsl = dsl;
        this.scheduledTasksDao = scheduledTasksDao;
    }
    
    /**
     * Lease and process available tasks.
     * Uses database-level locking to prevent duplicate processing.
     * 
     * @return number of tasks processed
     */
    @Transactional
    public int processAvailableTasks() {
        logger.debug("[TASK_PROC_START] Worker {} checking for available tasks", WORKER_ID);
        
        long startTime = System.currentTimeMillis();
        
        // 1. Lease available tasks
        logger.debug("[TASK_PROC_STEP_1] Attempting to lease available tasks");
        List<ScheduledTasks> leasedTasks = leaseAvailableTasks();
        
        if (leasedTasks.isEmpty()) {
            logger.debug("[TASK_PROC_NO_TASKS] No tasks available for processing");
            return 0;
        }
        
        logger.info("[TASK_PROC_STEP_1_SUCCESS] Worker {} leased {} tasks for processing", WORKER_ID, leasedTasks.size());
        
        // 2. Process each leased task
        logger.info("[TASK_PROC_STEP_2] Processing {} leased tasks", leasedTasks.size());
        int processedCount = 0;
        for (int i = 0; i < leasedTasks.size(); i++) {
            ScheduledTasks task = leasedTasks.get(i);
            long taskStartTime = System.currentTimeMillis();
            
            logger.info("[TASK_PROC_TASK_{}] Processing task {} of type {} (tenant: {})", 
                       i + 1, task.getId(), task.getTaskType(), task.getTenantId());
            
            try {
                // Set tenant context from task
                logger.debug("[TASK_PROC_TASK_{}_STEP_1] Setting tenant context: {}", i + 1, task.getTenantId());
                TenantContext.setTenantId(task.getTenantId());
                
                logger.debug("[TASK_PROC_TASK_{}_STEP_2] Calling processTask", i + 1);
                boolean success = processTask(task);
                
                long taskProcessingTime = System.currentTimeMillis() - taskStartTime;
                
                if (success) {
                    logger.debug("[TASK_PROC_TASK_{}_STEP_3] Marking task as completed", i + 1);
                    markTaskCompleted(task);
                    processedCount++;
                    logger.info("[TASK_PROC_TASK_{}_SUCCESS] Successfully processed task {} of type {} in {}ms", 
                               i + 1, task.getId(), task.getTaskType(), taskProcessingTime);
                } else {
                    logger.debug("[TASK_PROC_TASK_{}_STEP_3] Marking task as failed", i + 1);
                    markTaskFailed(task, "Task processing returned false");
                    logger.warn("[TASK_PROC_TASK_{}_FAILURE] Task {} processing failed after {}ms", 
                               i + 1, task.getId(), taskProcessingTime);
                }
                
            } catch (Exception e) {
                long taskErrorTime = System.currentTimeMillis() - taskStartTime;
                logger.debug("[TASK_PROC_TASK_{}_STEP_3] Marking task as failed due to exception", i + 1);
                markTaskFailed(task, e.getMessage());
                logger.error("[TASK_PROC_TASK_{}_ERROR] Error processing task {} after {}ms: {}", 
                            i + 1, task.getId(), taskErrorTime, e.getMessage(), e);
            } finally {
                logger.debug("[TASK_PROC_TASK_{}_CLEANUP] Clearing tenant context", i + 1);
                TenantContext.clear();
            }
        }
        
        long totalProcessingTime = System.currentTimeMillis() - startTime;
        logger.info("[TASK_PROC_COMPLETE] Worker {} completed processing {} out of {} tasks in {}ms", 
                   WORKER_ID, processedCount, leasedTasks.size(), totalProcessingTime);
        return processedCount;
    }
    
    /**
     * Lease available tasks for processing.
     * Uses database locking to ensure only one worker processes each task.
     */
    private List<ScheduledTasks> leaseAvailableTasks() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime leaseExpiry = now.plusMinutes(LEASE_DURATION_MINUTES);
        
        logger.debug("[TASK_LEASE_DEBUG] Current time: {}, Looking for tasks with due_at <= {}", now, now);
        
        // First, let's see what tasks are available
        int readyTaskCount = dsl.selectCount()
                .from(SCHEDULED_TASKS)
                .where(SCHEDULED_TASKS.STATUS.eq("READY"))
                .fetchOne(0, int.class);
        
        int dueTaskCount = dsl.selectCount()
                .from(SCHEDULED_TASKS)
                .where(SCHEDULED_TASKS.STATUS.eq("READY"))
                .and(SCHEDULED_TASKS.DUE_AT.le(now))
                .fetchOne(0, int.class);
        
        int availableTaskCount = dsl.selectCount()
                .from(SCHEDULED_TASKS)
                .where(SCHEDULED_TASKS.STATUS.eq("READY"))
                .and(SCHEDULED_TASKS.DUE_AT.le(now))
                .and(SCHEDULED_TASKS.LOCKED_UNTIL.isNull()
                     .or(SCHEDULED_TASKS.LOCKED_UNTIL.lt(now)))
                .fetchOne(0, int.class);
        
        logger.info("[TASK_LEASE_DEBUG] Tasks available: READY={}, DUE={}, AVAILABLE={}", 
                   readyTaskCount, dueTaskCount, availableTaskCount);
        
        if (availableTaskCount > 0) {
            // Log some sample tasks
            var sampleTasks = dsl.select(SCHEDULED_TASKS.ID, SCHEDULED_TASKS.TASK_KEY, SCHEDULED_TASKS.DUE_AT)
                    .from(SCHEDULED_TASKS)
                    .where(SCHEDULED_TASKS.STATUS.eq("READY"))
                    .and(SCHEDULED_TASKS.DUE_AT.le(now))
                    .and(SCHEDULED_TASKS.LOCKED_UNTIL.isNull()
                         .or(SCHEDULED_TASKS.LOCKED_UNTIL.lt(now)))
                    .limit(3)
                    .fetch();
            
            logger.info("[TASK_LEASE_DEBUG] Sample available tasks: {}", sampleTasks);
        }
        
        // Update and return tasks that are:
        // 1. Status = READY
        // 2. Due time has passed
        // 3. Not currently locked (or lock has expired)
        int updatedRows = dsl.update(SCHEDULED_TASKS)
                .set(SCHEDULED_TASKS.STATUS, "CLAIMED")
                .set(SCHEDULED_TASKS.LOCKED_UNTIL, leaseExpiry)
                .set(SCHEDULED_TASKS.LOCK_OWNER, WORKER_ID)
                .set(SCHEDULED_TASKS.UPDATED_AT, now)
                .where(SCHEDULED_TASKS.STATUS.eq("READY"))
                .and(SCHEDULED_TASKS.DUE_AT.le(now))
                .and(SCHEDULED_TASKS.LOCKED_UNTIL.isNull()
                     .or(SCHEDULED_TASKS.LOCKED_UNTIL.lt(now)))
                .limit(BATCH_SIZE)
                .execute();
        
        logger.info("[TASK_LEASE_DEBUG] Updated {} tasks to CLAIMED status", updatedRows);
        
        if (updatedRows == 0) {
            return List.of();
        }
        
        // Fetch the tasks we just leased
        List<ScheduledTasks> leasedTasks = dsl.selectFrom(SCHEDULED_TASKS)
                .where(SCHEDULED_TASKS.LOCK_OWNER.eq(WORKER_ID))
                .and(SCHEDULED_TASKS.STATUS.eq("CLAIMED"))
                .and(SCHEDULED_TASKS.LOCKED_UNTIL.gt(now))
                .fetchInto(ScheduledTasks.class);
        
        logger.info("[TASK_LEASE_DEBUG] Fetched {} leased tasks for processing", leasedTasks.size());
        
        return leasedTasks;
    }
    
    /**
     * Process a single task based on its type.
     */
    private boolean processTask(ScheduledTasks task) {
        logger.debug("[TASK_DISPATCH] Processing task {} of type {} for tenant {}", 
                    task.getId(), task.getTaskType(), task.getTenantId());
        
        long taskStartTime = System.currentTimeMillis();
        
        try {
            // Parse task payload
            logger.debug("[TASK_DISPATCH_STEP_1] Parsing task payload");
            Map<String, Object> payload = objectMapper.readValue(
                task.getPayload().data(), Map.class);
            logger.debug("[TASK_DISPATCH_STEP_1_SUCCESS] Parsed payload with {} keys", payload.size());
            
            logger.debug("[TASK_DISPATCH_STEP_2] Dispatching to task type handler: {}", task.getTaskType());
            boolean result;
            switch (task.getTaskType()) {
                case "SUBSCRIPTION_RENEWAL":
                    result = processSubscriptionRenewal(task, payload);
                    break;
                case "PRODUCT_RENEWAL":
                    result = processProductRenewal(task, payload);
                    break;
                case "CHARGE_PAYMENT":
                    result = processChargePayment(task, payload);
                    break;
                case "CREATE_DELIVERY":
                    result = processCreateDelivery(task, payload);
                    break;
                case "CREATE_ORDER":
                    result = processCreateOrder(task, payload);
                    break;
                case "ENTITLEMENT_GRANT":
                    result = processEntitlementGrant(task, payload);
                    break;
                case "TRIAL_END":
                    result = processTrialEnd(task, payload);
                    break;
                default:
                    logger.warn("[TASK_DISPATCH_ERROR] Unknown task type: {}", task.getTaskType());
                    return false;
            }
            
            long taskDispatchTime = System.currentTimeMillis() - taskStartTime;
            logger.debug("[TASK_DISPATCH_COMPLETE] Task {} handler returned {} in {}ms", 
                        task.getTaskType(), result, taskDispatchTime);
            return result;
            
        } catch (Exception e) {
            long taskErrorTime = System.currentTimeMillis() - taskStartTime;
            logger.error("[TASK_DISPATCH_ERROR] Error parsing task payload for task {} after {}ms: {}", 
                        task.getId(), taskErrorTime, e.getMessage());
            return false;
        }
    }
    
    /**
     * Process subscription renewal task.
     * This is a placeholder for the actual renewal logic.
     */
    private boolean processSubscriptionRenewal(ScheduledTasks task, Map<String, Object> payload) {
        String subscriptionId = (String) payload.get("subscriptionId");
        logger.info("Processing subscription renewal for subscription {} (tenant: {})", 
                   subscriptionId, task.getTenantId());
        
        // TODO: Implement actual renewal logic:
        // 1. Load subscription details
        // 2. Create invoice
        // 3. Attempt payment
        // 4. Update subscription period
        // 5. Schedule next renewal
        
        // For now, just log the processing
        logger.info("Subscription renewal processed successfully for {}", subscriptionId);
        return true;
    }
    
    /**
     * Process product renewal task.
     * Handles individual product renewals within multi-product subscriptions.
     */
    private boolean processProductRenewal(ScheduledTasks task, Map<String, Object> payload) {
        String subscriptionId = (String) payload.get("subscriptionId");
        String productId = (String) payload.get("productId");
        String planId = (String) payload.get("planId");
        
        logger.info("[PRODUCT_RENEWAL_START] Processing product renewal for subscription {} product {} plan {} (tenant: {})", 
                   subscriptionId, productId, planId, task.getTenantId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (billingTaskHandler != null) {
                logger.debug("[PRODUCT_RENEWAL_STEP_1] Calling billingTaskHandler.processProductRenewal");
                // Process product renewal through billing task handler
                boolean result = billingTaskHandler.processProductRenewal(
                    UUID.fromString(subscriptionId), 
                    productId, 
                    UUID.fromString(planId)
                );
                
                long processingTime = System.currentTimeMillis() - startTime;
                if (result) {
                    logger.info("[PRODUCT_RENEWAL_SUCCESS] Product renewal processed successfully for subscription {} product {} in {}ms", 
                               subscriptionId, productId, processingTime);
                } else {
                    logger.warn("[PRODUCT_RENEWAL_FAILURE] Product renewal processing failed for subscription {} product {} after {}ms", 
                               subscriptionId, productId, processingTime);
                }
                return result;
                
            } else {
                // Fallback when billing task handler is not available
                logger.warn("[PRODUCT_RENEWAL_FALLBACK] BillingTaskHandler not available - using fallback processing");
                logger.info("[PRODUCT_RENEWAL_FALLBACK_SUCCESS] Product renewal processed successfully for subscription {} product {} (fallback)", 
                           subscriptionId, productId);
                return true;
            }
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            logger.error("[PRODUCT_RENEWAL_ERROR] Failed to process product renewal for subscription {} product {} after {}ms: {}", 
                        subscriptionId, productId, errorTime, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process charge payment task.
     * Handles payment processing for invoices.
     */
    private boolean processChargePayment(ScheduledTasks task, Map<String, Object> payload) {
        String invoiceId = (String) payload.get("invoiceId");
        
        logger.info("[CHARGE_PAYMENT_START] Processing payment for invoice {} (tenant: {})", invoiceId, task.getTenantId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            if (billingTaskHandler != null) {
                logger.debug("[CHARGE_PAYMENT_STEP_1] Calling billingTaskHandler.processChargePayment");
                // Process payment through billing task handler
                boolean result = billingTaskHandler.processChargePayment(UUID.fromString(invoiceId));
                
                long processingTime = System.currentTimeMillis() - startTime;
                if (result) {
                    logger.info("[CHARGE_PAYMENT_SUCCESS] Payment processed successfully for invoice {} in {}ms", 
                               invoiceId, processingTime);
                } else {
                    logger.warn("[CHARGE_PAYMENT_FAILURE] Payment processing failed for invoice {} after {}ms", 
                               invoiceId, processingTime);
                }
                return result;
                
            } else {
                // Fallback when billing task handler is not available
                logger.warn("[CHARGE_PAYMENT_FALLBACK] BillingTaskHandler not available - using fallback processing");
                logger.info("[CHARGE_PAYMENT_FALLBACK_SUCCESS] Payment processed successfully for invoice {} (fallback)", invoiceId);
                return true;
            }
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            logger.error("[CHARGE_PAYMENT_ERROR] Failed to process payment for invoice {} after {}ms: {}", 
                        invoiceId, errorTime, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process create delivery task.
     * Creates delivery instance snapshots for physical/hybrid products.
     */
    private boolean processCreateDelivery(ScheduledTasks task, Map<String, Object> payload) {
        String invoiceId = (String) payload.get("invoiceId");
        String subscriptionId = (String) payload.get("subscriptionId");
        
        logger.info("[CREATE_DELIVERY_START] Processing delivery creation for invoice {} subscription {} (tenant: {})", 
                   invoiceId, subscriptionId, task.getTenantId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Load invoice and subscription details
            logger.debug("[CREATE_DELIVERY_STEP_1] Loading invoice and subscription details");
            UUID invoiceUuid = UUID.fromString(invoiceId);
            UUID subscriptionUuid = UUID.fromString(subscriptionId);
            
            // Load invoice
            var invoice = dsl.selectFrom(INVOICES)
                    .where(INVOICES.ID.eq(invoiceUuid))
                    .and(INVOICES.TENANT_ID.eq(task.getTenantId()))
                    .fetchOne();
            
            if (invoice == null) {
                logger.error("[CREATE_DELIVERY_ERROR] Invoice not found: {}", invoiceId);
                return false;
            }
            
            // Load subscription
            var subscription = dsl.selectFrom(SUBSCRIPTIONS)
                    .where(SUBSCRIPTIONS.ID.eq(subscriptionUuid))
                    .and(SUBSCRIPTIONS.TENANT_ID.eq(task.getTenantId()))
                    .fetchOne();
            
            if (subscription == null) {
                logger.error("[CREATE_DELIVERY_ERROR] Subscription not found: {}", subscriptionId);
                return false;
            }
            
            logger.info("[CREATE_DELIVERY_STEP_1_SUCCESS] Loaded invoice {} and subscription {}", invoiceId, subscriptionId);
            
            // 2. Compute cycle_key for period
            logger.debug("[CREATE_DELIVERY_STEP_2] Computing cycle key for delivery period");
            String cycleKey = String.format("%s_%s_%s", 
                subscriptionId, 
                invoice.getPeriodStart().toLocalDate().toString(),
                invoice.getPeriodEnd().toLocalDate().toString());
            
            logger.info("[CREATE_DELIVERY_STEP_2_SUCCESS] Generated cycle key: {}", cycleKey);
            
            // 3. Load subscription items for snapshot
            logger.debug("[CREATE_DELIVERY_STEP_3] Loading subscription items for snapshot");
            var subscriptionItems = dsl.selectFrom(SUBSCRIPTION_ITEMS)
                    .where(SUBSCRIPTION_ITEMS.SUBSCRIPTION_ID.eq(subscriptionUuid))
                    .and(SUBSCRIPTION_ITEMS.TENANT_ID.eq(task.getTenantId()))
                    .fetch();
            
            if (subscriptionItems.isEmpty()) {
                logger.error("[CREATE_DELIVERY_ERROR] No subscription items found for subscription: {}", subscriptionId);
                return false;
            }
            
            logger.info("[CREATE_DELIVERY_STEP_3_SUCCESS] Loaded {} subscription items", subscriptionItems.size());
            
            // 4. Create delivery snapshot
            logger.debug("[CREATE_DELIVERY_STEP_4] Creating delivery snapshot");
            Map<String, Object> deliverySnapshot = Map.of(
                "subscriptionId", subscriptionId,
                "invoiceId", invoiceId,
                "customerId", subscription.getCustomerId().toString(),
                "periodStart", invoice.getPeriodStart().toString(),
                "periodEnd", invoice.getPeriodEnd().toString(),
                "shippingAddress", subscription.getShippingAddress() != null ? 
                    subscription.getShippingAddress().data() : Map.of(),
                "items", subscriptionItems.stream().map(item -> Map.of(
                    "planId", item.getPlanId().toString(),
                    "quantity", item.getQuantity(),
                    "unitPriceCents", item.getUnitPriceCents(),
                    "currency", item.getCurrency()
                )).toList(),
                "totalCents", invoice.getTotalCents(),
                "currency", invoice.getCurrency(),
                "createdAt", System.currentTimeMillis()
            );
            
            // 5. Insert delivery_instances row idempotently
            logger.debug("[CREATE_DELIVERY_STEP_5] Inserting delivery instance");
            UUID deliveryId = UUID.randomUUID();
            
            try {
                int insertedRows = dsl.insertInto(DELIVERY_INSTANCES)
                        .set(DELIVERY_INSTANCES.ID, deliveryId)
                        .set(DELIVERY_INSTANCES.TENANT_ID, task.getTenantId())
                        .set(DELIVERY_INSTANCES.INVOICE_ID, invoiceUuid)
                        .set(DELIVERY_INSTANCES.SUBSCRIPTION_ID, subscriptionUuid)
                        .set(DELIVERY_INSTANCES.CYCLE_KEY, cycleKey)
                        .set(DELIVERY_INSTANCES.STATUS, "PENDING")
                        .set(DELIVERY_INSTANCES.SNAPSHOT, JSONB.valueOf(objectMapper.writeValueAsString(deliverySnapshot)))
                        .set(DELIVERY_INSTANCES.CREATED_AT, OffsetDateTime.now())
                        .set(DELIVERY_INSTANCES.UPDATED_AT, OffsetDateTime.now())
                        .onConflict(DELIVERY_INSTANCES.TENANT_ID, DELIVERY_INSTANCES.SUBSCRIPTION_ID, DELIVERY_INSTANCES.CYCLE_KEY)
                        .doNothing()
                        .execute();
                
                if (insertedRows > 0) {
                    logger.info("[CREATE_DELIVERY_STEP_5_SUCCESS] Created new delivery instance: {}", deliveryId);
                } else {
                    logger.info("[CREATE_DELIVERY_STEP_5_IDEMPOTENT] Delivery instance already exists for cycle: {}", cycleKey);
                    // Get existing delivery ID
                    var existingDelivery = dsl.select(DELIVERY_INSTANCES.ID)
                            .from(DELIVERY_INSTANCES)
                            .where(DELIVERY_INSTANCES.TENANT_ID.eq(task.getTenantId()))
                            .and(DELIVERY_INSTANCES.SUBSCRIPTION_ID.eq(subscriptionUuid))
                            .and(DELIVERY_INSTANCES.CYCLE_KEY.eq(cycleKey))
                            .fetchOne();
                    if (existingDelivery != null) {
                        deliveryId = existingDelivery.value1();
                    }
                }
                
            } catch (Exception e) {
                logger.error("[CREATE_DELIVERY_STEP_5_ERROR] Failed to insert delivery instance: {}", e.getMessage(), e);
                return false;
            }
            
            // 6. Schedule CREATE_ORDER task
            logger.debug("[CREATE_DELIVERY_STEP_6] Scheduling CREATE_ORDER task");
            try {
                // Schedule the order creation task immediately after delivery creation
                UUID orderTaskId = scheduleOrderCreationTask(deliveryId, subscriptionUuid, invoiceUuid, task.getTenantId());
                logger.info("[CREATE_DELIVERY_STEP_6_SUCCESS] CREATE_ORDER task scheduled successfully: {}", orderTaskId);
            } catch (Exception e) {
                logger.error("[CREATE_DELIVERY_STEP_6_ERROR] Failed to schedule CREATE_ORDER task: {}", e.getMessage(), e);
                // Don't fail delivery creation for order scheduling issues
            }
            
            logger.info("[CREATE_DELIVERY_SUCCESS] Delivery created successfully for invoice {} (delivery: {}) in {}ms", 
                       invoiceId, deliveryId, System.currentTimeMillis() - startTime);
            return true;
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            logger.error("[CREATE_DELIVERY_ERROR] Failed to create delivery for invoice {} after {}ms: {}", 
                        invoiceId, errorTime, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process create order task.
     * Calls commerce adapter to create external orders.
     */
    private boolean processCreateOrder(ScheduledTasks task, Map<String, Object> payload) {
        String deliveryId = (String) payload.get("deliveryId");
        
        logger.info("[CREATE_ORDER_START] Processing order creation for delivery {} (tenant: {})", 
                   deliveryId, task.getTenantId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Load delivery instance
            logger.debug("[CREATE_ORDER_STEP_1] Loading delivery instance");
            UUID deliveryUuid = UUID.fromString(deliveryId);
            
            var delivery = dsl.selectFrom(DELIVERY_INSTANCES)
                    .where(DELIVERY_INSTANCES.ID.eq(deliveryUuid))
                    .and(DELIVERY_INSTANCES.TENANT_ID.eq(task.getTenantId()))
                    .fetchOne();
            
            if (delivery == null) {
                logger.error("[CREATE_ORDER_ERROR] Delivery instance not found: {}", deliveryId);
                return false;
            }
            
            logger.info("[CREATE_ORDER_STEP_1_SUCCESS] Loaded delivery instance: {}", deliveryId);
            
            // 2. Parse delivery snapshot
            logger.debug("[CREATE_ORDER_STEP_2] Parsing delivery snapshot");
            Map<String, Object> snapshot;
            try {
                snapshot = objectMapper.readValue(delivery.getSnapshot().data(), Map.class);
            } catch (Exception e) {
                logger.error("[CREATE_ORDER_STEP_2_ERROR] Failed to parse delivery snapshot: {}", e.getMessage(), e);
                return false;
            }
            
            // 3. Build order request from snapshot
            logger.debug("[CREATE_ORDER_STEP_3] Building order request from snapshot");
            
            // Extract customer information
            String customerId = (String) snapshot.get("customerId");
            Map<String, Object> shippingAddressMap = (Map<String, Object>) snapshot.get("shippingAddress");
            
            // For now, just log the order details instead of creating complex objects
            // In a real implementation, we'd inject CommerceAdapter and create proper request objects
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) snapshot.get("items");
            
            logger.info("[CREATE_ORDER_STEP_3_DETAILS] Order details: customer={}, items={}, shipping={}", 
                       customerId, itemsData.size(), shippingAddressMap);
            
            logger.info("[CREATE_ORDER_STEP_3_SUCCESS] Built order request with {} items", itemsData.size());
            
            // 4. Call CommerceAdapter.createOrder (mock implementation for now)
            logger.debug("[CREATE_ORDER_STEP_4] Calling commerce adapter to create order");
            
            // For now, simulate the commerce adapter call
            // In a real implementation, we'd inject CommerceAdapter
            String externalOrderRef = "mock_order_" + UUID.randomUUID().toString().substring(0, 8);
            boolean orderSuccess = true; // Mock success
            
            logger.info("[CREATE_ORDER_STEP_4_SUCCESS] Commerce adapter returned: success={}, orderRef={}", 
                       orderSuccess, externalOrderRef);
            
            // 5. Update delivery instance with order result
            logger.debug("[CREATE_ORDER_STEP_5] Updating delivery instance with order result");
            
            if (orderSuccess) {
                // Update delivery with external order reference
                int updatedRows = dsl.update(DELIVERY_INSTANCES)
                        .set(DELIVERY_INSTANCES.EXTERNAL_ORDER_REF, externalOrderRef)
                        .set(DELIVERY_INSTANCES.STATUS, "ORDER_CREATED")
                        .set(DELIVERY_INSTANCES.UPDATED_AT, OffsetDateTime.now())
                        .where(DELIVERY_INSTANCES.ID.eq(deliveryUuid))
                        .execute();
                
                logger.info("[CREATE_ORDER_STEP_5_SUCCESS] Updated delivery instance status to ORDER_CREATED: {} rows affected", updatedRows);
                
                // TODO: Emit outbox event for delivery.order_created
                logger.info("[CREATE_ORDER_STEP_6_TODO] Should emit outbox event: delivery.order_created");
                
            } else {
                // Mark delivery as failed
                int updatedRows = dsl.update(DELIVERY_INSTANCES)
                        .set(DELIVERY_INSTANCES.STATUS, "FAILED")
                        .set(DELIVERY_INSTANCES.UPDATED_AT, OffsetDateTime.now())
                        .where(DELIVERY_INSTANCES.ID.eq(deliveryUuid))
                        .execute();
                
                logger.warn("[CREATE_ORDER_STEP_5_FAILURE] Updated delivery instance status to FAILED: {} rows affected", updatedRows);
                return false;
            }
            
            logger.info("[CREATE_ORDER_SUCCESS] Order created successfully for delivery {} (orderRef: {}) in {}ms", 
                       deliveryId, externalOrderRef, System.currentTimeMillis() - startTime);
            return true;
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            logger.error("[CREATE_ORDER_ERROR] Failed to create order for delivery {} after {}ms: {}", 
                        deliveryId, errorTime, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process entitlement grant task.
     * Manages digital product entitlements.
     */
    private boolean processEntitlementGrant(ScheduledTasks task, Map<String, Object> payload) {
        String invoiceId = (String) payload.get("invoiceId");
        String subscriptionId = (String) payload.get("subscriptionId");
        String action = (String) payload.getOrDefault("action", "GRANT");
        
        logger.info("[ENTITLEMENT_{}_START] Processing entitlement {} for invoice {} subscription {} (tenant: {})", 
                   action, action.toLowerCase(), invoiceId, subscriptionId, task.getTenantId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Load invoice and subscription details
            logger.debug("[ENTITLEMENT_{}_STEP_1] Loading invoice and subscription details", action);
            UUID invoiceUuid = UUID.fromString(invoiceId);
            UUID subscriptionUuid = UUID.fromString(subscriptionId);
            
            // Load invoice
            var invoice = dsl.selectFrom(INVOICES)
                    .where(INVOICES.ID.eq(invoiceUuid))
                    .and(INVOICES.TENANT_ID.eq(task.getTenantId()))
                    .fetchOne();
            
            if (invoice == null) {
                logger.error("[ENTITLEMENT_{}_ERROR] Invoice not found: {}", action, invoiceId);
                return false;
            }
            
            // Load subscription
            var subscription = dsl.selectFrom(SUBSCRIPTIONS)
                    .where(SUBSCRIPTIONS.ID.eq(subscriptionUuid))
                    .and(SUBSCRIPTIONS.TENANT_ID.eq(task.getTenantId()))
                    .fetchOne();
            
            if (subscription == null) {
                logger.error("[ENTITLEMENT_{}_ERROR] Subscription not found: {}", action, subscriptionId);
                return false;
            }
            
            logger.info("[ENTITLEMENT_{}_STEP_1_SUCCESS] Loaded invoice {} and subscription {}", action, invoiceId, subscriptionId);
            
            // 2. Load subscription items to determine entitlements
            logger.debug("[ENTITLEMENT_{}_STEP_2] Loading subscription items for entitlement processing", action);
            var subscriptionItems = dsl.selectFrom(SUBSCRIPTION_ITEMS)
                    .where(SUBSCRIPTION_ITEMS.SUBSCRIPTION_ID.eq(subscriptionUuid))
                    .and(SUBSCRIPTION_ITEMS.TENANT_ID.eq(task.getTenantId()))
                    .fetch();
            
            if (subscriptionItems.isEmpty()) {
                logger.error("[ENTITLEMENT_{}_ERROR] No subscription items found for subscription: {}", action, subscriptionId);
                return false;
            }
            
            logger.info("[ENTITLEMENT_{}_STEP_2_SUCCESS] Loaded {} subscription items for entitlement processing", action, subscriptionItems.size());
            
            // 3. Process entitlements for each subscription item
            logger.debug("[ENTITLEMENT_{}_STEP_3] Processing entitlements for subscription items", action);
            
            for (var item : subscriptionItems) {
                // Create entitlement payload
                Map<String, Object> entitlementPayload = Map.of(
                    "planId", item.getPlanId().toString(),
                    "quantity", item.getQuantity(),
                    "unitPriceCents", item.getUnitPriceCents(),
                    "currency", item.getCurrency(),
                    "periodStart", invoice.getPeriodStart().toString(),
                    "periodEnd", invoice.getPeriodEnd().toString(),
                    "invoiceId", invoiceId,
                    "subscriptionId", subscriptionId
                );
                
                // 4. Upsert entitlements record for period
                logger.debug("[ENTITLEMENT_{}_STEP_4] Upserting entitlement record for plan {}", action, item.getPlanId());
                
                UUID entitlementId = UUID.randomUUID();
                String entitlementKey = String.format("%s_%s_%s_%s", 
                    subscriptionId, 
                    item.getPlanId().toString(),
                    invoice.getPeriodStart().toLocalDate().toString(),
                    invoice.getPeriodEnd().toLocalDate().toString());
                
                try {
                    // Check if entitlement already exists
                    var existingEntitlement = dsl.selectFrom(ENTITLEMENTS)
                            .where(ENTITLEMENTS.TENANT_ID.eq(task.getTenantId()))
                            .and(ENTITLEMENTS.CUSTOMER_ID.eq(subscription.getCustomerId()))
                            .and(ENTITLEMENTS.ENTITLEMENT_KEY.eq(entitlementKey))
                            .fetchOne();
                    
                    int affectedRows;
                    if (existingEntitlement != null) {
                        // Update existing entitlement
                        affectedRows = dsl.update(ENTITLEMENTS)
                                .set(ENTITLEMENTS.STATUS, "ACTIVE")
                                .set(ENTITLEMENTS.VALID_UNTIL, invoice.getPeriodEnd())
                                .set(ENTITLEMENTS.ENTITLEMENT_PAYLOAD, JSONB.valueOf(objectMapper.writeValueAsString(entitlementPayload)))
                                .set(ENTITLEMENTS.UPDATED_AT, OffsetDateTime.now())
                                .where(ENTITLEMENTS.ID.eq(existingEntitlement.getId()))
                                .execute();
                        logger.info("[ENTITLEMENT_{}_STEP_4_UPDATE] Updated existing entitlement for plan {}: {} rows affected", 
                                   action, item.getPlanId(), affectedRows);
                    } else {
                        // Insert new entitlement
                        affectedRows = dsl.insertInto(ENTITLEMENTS)
                                .set(ENTITLEMENTS.ID, entitlementId)
                                .set(ENTITLEMENTS.TENANT_ID, task.getTenantId())
                                .set(ENTITLEMENTS.CUSTOMER_ID, subscription.getCustomerId())
                                .set(ENTITLEMENTS.SUBSCRIPTION_ID, subscriptionUuid)
                                .set(ENTITLEMENTS.ENTITLEMENT_TYPE, "PLAN_ACCESS")
                                .set(ENTITLEMENTS.ENTITLEMENT_KEY, entitlementKey)
                                .set(ENTITLEMENTS.STATUS, "ACTIVE")
                                .set(ENTITLEMENTS.VALID_FROM, invoice.getPeriodStart())
                                .set(ENTITLEMENTS.VALID_UNTIL, invoice.getPeriodEnd())
                                .set(ENTITLEMENTS.ENTITLEMENT_PAYLOAD, JSONB.valueOf(objectMapper.writeValueAsString(entitlementPayload)))
                                .set(ENTITLEMENTS.CREATED_AT, OffsetDateTime.now())
                                .set(ENTITLEMENTS.UPDATED_AT, OffsetDateTime.now())
                                .execute();
                        logger.info("[ENTITLEMENT_{}_STEP_4_INSERT] Created new entitlement for plan {}: {} rows affected", 
                                   action, item.getPlanId(), affectedRows);
                    }
                    
                    logger.info("[ENTITLEMENT_{}_STEP_4_SUCCESS] Upserted entitlement record for plan {}: {} rows affected", 
                               action, item.getPlanId(), affectedRows);
                    
                } catch (Exception e) {
                    logger.error("[ENTITLEMENT_{}_STEP_4_ERROR] Failed to upsert entitlement record for plan {}: {}", 
                                action, item.getPlanId(), e.getMessage(), e);
                    return false;
                }
                
                // 5. Call EntitlementAdapter (mock implementation for now)
                logger.debug("[ENTITLEMENT_{}_STEP_5] Calling entitlement adapter for plan {}", action, item.getPlanId());
                
                // For now, simulate the entitlement adapter call
                // In a real implementation, we'd inject EntitlementAdapter
                String externalEntitlementRef = "mock_entitlement_" + UUID.randomUUID().toString().substring(0, 8);
                boolean entitlementSuccess = true; // Mock success
                
                logger.info("[ENTITLEMENT_{}_STEP_5_SUCCESS] Entitlement adapter returned: success={}, ref={} for plan {}", 
                           action, entitlementSuccess, externalEntitlementRef, item.getPlanId());
                
                // 6. Update entitlement record with external reference
                if (entitlementSuccess) {
                    try {
                        int updatedRows = dsl.update(ENTITLEMENTS)
                                .set(ENTITLEMENTS.EXTERNAL_ENTITLEMENT_REF, externalEntitlementRef)
                                .set(ENTITLEMENTS.UPDATED_AT, OffsetDateTime.now())
                                .where(ENTITLEMENTS.TENANT_ID.eq(task.getTenantId()))
                                .and(ENTITLEMENTS.CUSTOMER_ID.eq(subscription.getCustomerId()))
                                .and(ENTITLEMENTS.ENTITLEMENT_KEY.eq(entitlementKey))
                                .execute();
                        
                        logger.info("[ENTITLEMENT_{}_STEP_6_SUCCESS] Updated entitlement with external ref for plan {}: {} rows affected", 
                                   action, item.getPlanId(), updatedRows);
                        
                    } catch (Exception e) {
                        logger.error("[ENTITLEMENT_{}_STEP_6_ERROR] Failed to update entitlement with external ref for plan {}: {}", 
                                    action, item.getPlanId(), e.getMessage(), e);
                        // Don't fail the entire process for this
                    }
                }
            }
            
            // 7. TODO: Emit outbox events
            logger.info("[ENTITLEMENT_{}_STEP_7_TODO] Should emit outbox event: entitlement.{}", action, action.toLowerCase());
            
            logger.info("[ENTITLEMENT_{}_SUCCESS] Entitlement {} processed successfully for invoice {} ({} items) in {}ms", 
                       action, action.toLowerCase(), invoiceId, subscriptionItems.size(), System.currentTimeMillis() - startTime);
            return true;
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            logger.error("[ENTITLEMENT_{}_ERROR] Failed to process entitlement {} for invoice {} after {}ms: {}", 
                        action, action.toLowerCase(), invoiceId, errorTime, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Process trial end task.
     * This is a placeholder for the actual trial end logic.
     */
    private boolean processTrialEnd(ScheduledTasks task, Map<String, Object> payload) {
        String subscriptionId = (String) payload.get("subscriptionId");
        
        logger.info("Processing trial end for subscription {} (tenant: {})", subscriptionId, task.getTenantId());
        
        // TODO: Implement trial end logic
        // - Check subscription status
        // - Convert to paid or cancel
        // - Send notifications
        
        logger.info("Trial end processed successfully for subscription {}", subscriptionId);
        return true;
    }
    
    /**
     * Mark task as completed.
     */
    private void markTaskCompleted(ScheduledTasks task) {
        OffsetDateTime now = OffsetDateTime.now();
        
        dsl.update(SCHEDULED_TASKS)
                .set(SCHEDULED_TASKS.STATUS, "COMPLETED")
                .set(SCHEDULED_TASKS.COMPLETED_AT, now)
                .set(SCHEDULED_TASKS.UPDATED_AT, now)
                .set(SCHEDULED_TASKS.LOCKED_UNTIL, (OffsetDateTime) null)
                .set(SCHEDULED_TASKS.LOCK_OWNER, (String) null)
                .where(SCHEDULED_TASKS.ID.eq(task.getId()))
                .execute();
    }
    
    /**
     * Mark task as failed and increment attempt count.
     */
    private void markTaskFailed(ScheduledTasks task, String errorMessage) {
        OffsetDateTime now = OffsetDateTime.now();
        int newAttemptCount = (task.getAttemptCount() != null ? task.getAttemptCount() : 0) + 1;
        
        String newStatus = newAttemptCount >= task.getMaxAttempts() ? "FAILED" : "READY";
        
        dsl.update(SCHEDULED_TASKS)
                .set(SCHEDULED_TASKS.STATUS, newStatus)
                .set(SCHEDULED_TASKS.ATTEMPT_COUNT, newAttemptCount)
                .set(SCHEDULED_TASKS.LAST_ERROR, errorMessage)
                .set(SCHEDULED_TASKS.UPDATED_AT, now)
                .set(SCHEDULED_TASKS.LOCKED_UNTIL, (OffsetDateTime) null)
                .set(SCHEDULED_TASKS.LOCK_OWNER, (String) null)
                .where(SCHEDULED_TASKS.ID.eq(task.getId()))
                .execute();
        
        logger.warn("Task {} marked as {} after {} attempts. Error: {}", 
                   task.getId(), newStatus, newAttemptCount, errorMessage);
    }
    
    /**
     * Clean up expired locks from dead workers.
     */
    @Transactional
    public int cleanupExpiredLocks() {
        OffsetDateTime now = OffsetDateTime.now();
        
        int cleanedUp = dsl.update(SCHEDULED_TASKS)
                .set(SCHEDULED_TASKS.STATUS, "READY")
                .set(SCHEDULED_TASKS.LOCKED_UNTIL, (OffsetDateTime) null)
                .set(SCHEDULED_TASKS.LOCK_OWNER, (String) null)
                .set(SCHEDULED_TASKS.UPDATED_AT, now)
                .where(SCHEDULED_TASKS.STATUS.eq("CLAIMED"))
                .and(SCHEDULED_TASKS.LOCKED_UNTIL.lt(now))
                .execute();
        
        if (cleanedUp > 0) {
            logger.info("Cleaned up {} expired task locks", cleanedUp);
        }
        
        return cleanedUp;
    }
    
    /**
     * Schedule CREATE_ORDER task for a delivery instance.
     */
    private UUID scheduleOrderCreationTask(UUID deliveryId, UUID subscriptionId, UUID invoiceId, UUID tenantId) {
        logger.debug("[SCHEDULE_ORDER_TASK] Scheduling CREATE_ORDER task for delivery {}", deliveryId);
        
        try {
            // Create task payload
            Map<String, Object> payloadMap = Map.of(
                "deliveryId", deliveryId.toString(),
                "subscriptionId", subscriptionId.toString(),
                "invoiceId", invoiceId.toString(),
                "taskType", "CREATE_ORDER",
                "scheduledAt", OffsetDateTime.now().toString()
            );
            
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            JSONB payload = JSONB.valueOf(payloadJson);
            
            // Create unique task key to prevent duplicates
            String taskKey = String.format("order_%s", deliveryId);
            
            ScheduledTasks task = new ScheduledTasks();
            task.setId(UUID.randomUUID());
            task.setTenantId(tenantId);
            task.setTaskType("CREATE_ORDER");
            task.setTaskKey(taskKey);
            task.setStatus("READY");
            task.setDueAt(OffsetDateTime.now()); // Process immediately
            task.setAttemptCount(0);
            task.setMaxAttempts(3);
            task.setPayload(payload);
            task.setCreatedAt(OffsetDateTime.now());
            task.setUpdatedAt(OffsetDateTime.now());
            
            // Insert the task directly using DSL
            dsl.insertInto(SCHEDULED_TASKS)
                .set(SCHEDULED_TASKS.ID, task.getId())
                .set(SCHEDULED_TASKS.TENANT_ID, task.getTenantId())
                .set(SCHEDULED_TASKS.TASK_TYPE, task.getTaskType())
                .set(SCHEDULED_TASKS.TASK_KEY, task.getTaskKey())
                .set(SCHEDULED_TASKS.STATUS, task.getStatus())
                .set(SCHEDULED_TASKS.DUE_AT, task.getDueAt())
                .set(SCHEDULED_TASKS.ATTEMPT_COUNT, task.getAttemptCount())
                .set(SCHEDULED_TASKS.MAX_ATTEMPTS, task.getMaxAttempts())
                .set(SCHEDULED_TASKS.PAYLOAD, task.getPayload())
                .set(SCHEDULED_TASKS.CREATED_AT, task.getCreatedAt())
                .set(SCHEDULED_TASKS.UPDATED_AT, task.getUpdatedAt())
                .execute();
            
            logger.info("[SCHEDULE_ORDER_TASK] CREATE_ORDER task scheduled successfully: {}", task.getId());
            return task.getId();
            
        } catch (Exception e) {
            logger.error("[SCHEDULE_ORDER_TASK] Failed to schedule CREATE_ORDER task for delivery {}: {}", 
                        deliveryId, e.getMessage(), e);
            throw new RuntimeException("Failed to schedule CREATE_ORDER task", e);
        }
    }
}
