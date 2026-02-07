/**
 * Service for processing payments using configured payment adapters.
 * Handles payment attempts, retries, and status updates.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.billing.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.InvoicesDao;
import com.subscriptionengine.generated.tables.daos.PaymentAttemptsDao;
import com.subscriptionengine.generated.tables.pojos.Invoices;
import com.subscriptionengine.generated.tables.pojos.PaymentAttempts;
import com.subscriptionengine.integrations.payments.PaymentAdapter;
import com.subscriptionengine.scheduler.service.ScheduledTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSONB;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.Invoices.INVOICES;
import static com.subscriptionengine.generated.tables.PaymentAttempts.PAYMENT_ATTEMPTS;
import static com.subscriptionengine.generated.tables.Subscriptions.SUBSCRIPTIONS;
import static com.subscriptionengine.generated.tables.SubscriptionItems.SUBSCRIPTION_ITEMS;

import com.subscriptionengine.generated.tables.pojos.Subscriptions;
import com.subscriptionengine.generated.tables.pojos.SubscriptionItems;
import com.subscriptionengine.generated.tables.daos.SubscriptionsDao;
import java.util.List;

@Service
public class PaymentProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProcessingService.class);
    
    private final PaymentAdapter paymentAdapter;
    private final InvoicesDao invoicesDao;
    private final PaymentAttemptsDao paymentAttemptsDao;
    private final SubscriptionsDao subscriptionsDao;
    private final ScheduledTaskService scheduledTaskService;
    private final ObjectMapper objectMapper;
    private final DSLContext dsl;
    
    public PaymentProcessingService(PaymentAdapter paymentAdapter, 
                                  InvoicesDao invoicesDao,
                                  PaymentAttemptsDao paymentAttemptsDao,
                                  SubscriptionsDao subscriptionsDao,
                                  ScheduledTaskService scheduledTaskService,
                                  ObjectMapper objectMapper,
                                  DSLContext dsl) {
        this.paymentAdapter = paymentAdapter;
        this.invoicesDao = invoicesDao;
        this.paymentAttemptsDao = paymentAttemptsDao;
        this.subscriptionsDao = subscriptionsDao;
        this.scheduledTaskService = scheduledTaskService;
        this.objectMapper = objectMapper;
        this.dsl = dsl;
    }
    
    /**
     * Process payment for an invoice.
     * Creates payment attempt record and processes payment through adapter.
     */
    @Transactional
    public boolean processPayment(UUID invoiceId) {
        logger.info("[PAYMENT_PROC_START] Processing payment for invoice {}", invoiceId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            UUID tenantId = TenantContext.getRequiredTenantId();
            OffsetDateTime now = OffsetDateTime.now();
            
            logger.info("Processing payment for invoice {} (tenant: {})", invoiceId, tenantId);
            
            // 1. Load invoice details
            logger.info("[PAYMENT_PROC_STEP_1] Loading invoice details for {}", invoiceId);
            Invoices invoice = invoicesDao.fetchOneById(invoiceId);
            if (invoice == null) {
                logger.error("[PAYMENT_PROC_ERROR] Invoice not found: {}", invoiceId);
                return false;
            }
            logger.info("[PAYMENT_PROC_STEP_1_SUCCESS] Loaded invoice: status={}, total={} {}", 
                       invoice.getStatus(), invoice.getTotalCents(), invoice.getCurrency());
            
            // 2. Check if payment already processed
            logger.info("[PAYMENT_PROC_STEP_2] Checking invoice payment status");
            if ("PAID".equals(invoice.getStatus())) {
                logger.info("[PAYMENT_PROC_IDEMPOTENT] Invoice {} already paid, skipping payment processing", invoiceId);
                return true;
            }
            logger.info("[PAYMENT_PROC_STEP_2_SUCCESS] Invoice status is {}, proceeding with payment", invoice.getStatus());
            
            // 3. Create payment attempt record
            logger.info("[PAYMENT_PROC_STEP_3] Creating payment attempt record");
            PaymentAttempts attempt = new PaymentAttempts();
            UUID attemptId = UUID.randomUUID();
            attempt.setId(attemptId);
            attempt.setTenantId(invoice.getTenantId());
            attempt.setInvoiceId(invoiceId);
            attempt.setAmountCents(invoice.getTotalCents());
            attempt.setCurrency(invoice.getCurrency());
            attempt.setStatus("PENDING");
            attempt.setCreatedAt(now);
            logger.info("[PAYMENT_PROC_STEP_3_SUCCESS] Created payment attempt: id={}, amount={} {}", 
                       attemptId, invoice.getTotalCents(), invoice.getCurrency());
            
            // Get payment method from subscription (simplified)
            String paymentMethodRef = getPaymentMethodRef(invoice);
            attempt.setPaymentMethodRef(paymentMethodRef);
            
            // Insert payment attempt
            logger.info("[PAYMENT_PROC_STEP_4] Inserting payment attempt into database");
            try {
                logger.debug("[PAYMENT_PROC_STEP_4_DB] Executing payment attempt insert query");
                int attemptInsertedRows = dsl.insertInto(PAYMENT_ATTEMPTS)
                        .set(PAYMENT_ATTEMPTS.ID, attempt.getId())
                        .set(PAYMENT_ATTEMPTS.TENANT_ID, attempt.getTenantId())
                        .set(PAYMENT_ATTEMPTS.INVOICE_ID, attempt.getInvoiceId())
                        .set(PAYMENT_ATTEMPTS.AMOUNT_CENTS, attempt.getAmountCents())
                        .set(PAYMENT_ATTEMPTS.CURRENCY, attempt.getCurrency())
                        .set(PAYMENT_ATTEMPTS.STATUS, attempt.getStatus())
                        .set(PAYMENT_ATTEMPTS.CREATED_AT, attempt.getCreatedAt())
                        .execute();
                logger.info("[PAYMENT_PROC_STEP_4_SUCCESS] Payment attempt inserted: {} rows affected", attemptInsertedRows);
            } catch (Exception e) {
                logger.error("[PAYMENT_PROC_STEP_4_ERROR] Failed to insert payment attempt: {}", e.getMessage(), e);
                throw new RuntimeException("Payment attempt creation failed", e);
            }
            
            // 4. Process payment through adapter
            logger.info("[PAYMENT_PROC_STEP_5] Processing payment through payment adapter");
            Map<String, Object> paymentMetadata = Map.of(
                    "invoiceId", invoiceId.toString(),
                    "subscriptionId", invoice.getSubscriptionId().toString()
            );
            logger.debug("[PAYMENT_PROC_STEP_5_CALL] Calling paymentAdapter.processPayment with amount={} {}, customer={}", 
                        invoice.getTotalCents(), invoice.getCurrency(), invoice.getCustomerId());
            
            PaymentAdapter.PaymentRequest paymentRequest = new PaymentAdapter.PaymentRequest(
                    invoiceId,
                    invoice.getCustomerId(),
                    invoice.getTotalCents(),
                    invoice.getCurrency(),
                    paymentMethodRef,
                    "invoice_" + invoiceId.toString(),
                    paymentMetadata
            );
            
            PaymentAdapter.PaymentResult result = paymentAdapter.processPayment(paymentRequest);
            
            logger.info("[PAYMENT_PROC_STEP_5_SUCCESS] Payment adapter returned: success={}, reference={}", 
                       result.success(), result.paymentReference());
            
            // 5. Update payment attempt with result
            logger.info("[PAYMENT_PROC_STEP_6] Updating payment attempt with result: success={}", result.success());
            if (result.success()) {
                // Payment successful
                logger.debug("[PAYMENT_PROC_STEP_6_SUCCESS_DB] Updating payment attempt as COMPLETED");
                try {
                    int successUpdateRows = dsl.update(PAYMENT_ATTEMPTS)
                            .set(PAYMENT_ATTEMPTS.STATUS, "SUCCEEDED")
                            .set(PAYMENT_ATTEMPTS.EXTERNAL_PAYMENT_ID, result.paymentReference())
                            .set(PAYMENT_ATTEMPTS.COMPLETED_AT, now)
                            .set(PAYMENT_ATTEMPTS.CUSTOM_ATTRS, JSONB.valueOf(objectMapper.writeValueAsString(result.providerData())))
                            .where(PAYMENT_ATTEMPTS.ID.eq(attempt.getId()))
                            .execute();
                    logger.info("[PAYMENT_PROC_STEP_6_SUCCESS] Payment attempt updated as COMPLETED: {} rows affected", successUpdateRows);
                } catch (Exception e) {
                    logger.error("[PAYMENT_PROC_STEP_6_ERROR] Failed to update successful payment attempt: {}", e.getMessage(), e);
                    throw new RuntimeException("Payment attempt update failed", e);
                }
                
                // 5. Update invoice status
                logger.info("[PAYMENT_PROC_STEP_7] Updating invoice status to PAID");
                try {
                    logger.debug("[PAYMENT_PROC_STEP_7_DB] Executing invoice status update query");
                    int invoiceUpdateRows = dsl.update(INVOICES)
                            .set(INVOICES.STATUS, "PAID")
                            .set(INVOICES.UPDATED_AT, now)
                            .where(INVOICES.ID.eq(invoiceId))
                            .execute();
                    logger.info("[PAYMENT_PROC_STEP_7_SUCCESS] Invoice status updated to PAID: {} rows affected", invoiceUpdateRows);
                } catch (Exception e) {
                    logger.error("[PAYMENT_PROC_STEP_7_ERROR] Failed to update invoice status: {}", e.getMessage(), e);
                    throw new RuntimeException("Invoice status update failed", e);
                }
                
                // 8. Schedule delivery and entitlement tasks for successful payments
                logger.info("[PAYMENT_PROC_STEP_8] Scheduling delivery and entitlement tasks");
                try {
                    schedulePostPaymentTasks(invoice, tenantId, now);
                    logger.info("[PAYMENT_PROC_STEP_8_SUCCESS] Post-payment tasks scheduled successfully");
                } catch (Exception e) {
                    logger.error("[PAYMENT_PROC_STEP_8_ERROR] Failed to schedule post-payment tasks: {}", e.getMessage(), e);
                    // Don't fail the payment for task scheduling issues
                }
                
                long successTime = System.currentTimeMillis() - startTime;
                logger.info("[PAYMENT_PROC_SUCCESS] Payment successful for invoice {} (reference: {}) in {}ms", 
                           invoiceId, result.paymentReference(), successTime);
                return true;
                
            } else {
                long failureTime = System.currentTimeMillis() - startTime;
                logger.warn("[PAYMENT_PROC_FAILURE_END] Payment processing failed for invoice {} after {}ms", invoiceId, failureTime);
                return false;
            }
            
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis() - startTime;
            logger.error("[PAYMENT_PROC_ERROR] Error processing payment for invoice {} after {}ms: {}", 
                        invoiceId, errorTime, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Schedule post-payment tasks for deliveries and entitlements.
     */
    private void schedulePostPaymentTasks(Invoices invoice, UUID tenantId, OffsetDateTime now) {
        logger.info("[PAYMENT_PROC_POST_TASKS] Scheduling post-payment tasks for invoice {}", invoice.getId());
        
        try {
            // Load subscription to determine product types
            Subscriptions subscription = subscriptionsDao.fetchOneById(invoice.getSubscriptionId());
            if (subscription == null) {
                logger.warn("[PAYMENT_PROC_POST_TASKS] Subscription not found: {}", invoice.getSubscriptionId());
                return;
            }
            
            // Load subscription items to determine what tasks to schedule
            List<SubscriptionItems> items = dsl.selectFrom(SUBSCRIPTION_ITEMS)
                    .where(SUBSCRIPTION_ITEMS.SUBSCRIPTION_ID.eq(invoice.getSubscriptionId()))
                    .and(SUBSCRIPTION_ITEMS.TENANT_ID.eq(tenantId))
                    .fetchInto(SubscriptionItems.class);
            
            boolean hasPhysicalProducts = false;
            boolean hasDigitalProducts = false;
            
            // Analyze subscription items to determine product types
            for (SubscriptionItems item : items) {
                // For now, assume all products are hybrid (both physical and digital)
                // In a real implementation, you'd check the plan or product configuration
                hasPhysicalProducts = true;
                hasDigitalProducts = true;
            }
            
            // Schedule CREATE_DELIVERY task for physical/hybrid products
            if (hasPhysicalProducts) {
                logger.info("[PAYMENT_PROC_POST_TASKS] Scheduling CREATE_DELIVERY task");
                try {
                    UUID deliveryTaskId = scheduledTaskService.scheduleDeliveryCreation(
                        invoice.getId(),
                        invoice.getSubscriptionId(), 
                        invoice.getCustomerId(),
                        tenantId,
                        now // Process immediately after payment success
                    );
                    logger.info("[PAYMENT_PROC_POST_TASKS] CREATE_DELIVERY task scheduled successfully: {}", deliveryTaskId);
                } catch (Exception e) {
                    logger.error("[PAYMENT_PROC_POST_TASKS] Failed to schedule CREATE_DELIVERY task: {}", e.getMessage(), e);
                    // Don't fail the payment for task scheduling issues
                }
            }
            
            // Schedule ENTITLEMENT_GRANT task for digital/hybrid products
            if (hasDigitalProducts) {
                logger.info("[PAYMENT_PROC_POST_TASKS] Scheduling ENTITLEMENT_GRANT task");
                try {
                    UUID entitlementTaskId = scheduledTaskService.scheduleEntitlementGrant(
                        invoice.getId(),
                        invoice.getSubscriptionId(),
                        invoice.getCustomerId(),
                        tenantId,
                        now // Process immediately after payment success
                    );
                    logger.info("[PAYMENT_PROC_POST_TASKS] ENTITLEMENT_GRANT task scheduled successfully: {}", entitlementTaskId);
                } catch (Exception e) {
                    logger.error("[PAYMENT_PROC_POST_TASKS] Failed to schedule ENTITLEMENT_GRANT task: {}", e.getMessage(), e);
                    // Don't fail the payment for task scheduling issues
                }
            }
            
            logger.info("[PAYMENT_PROC_POST_TASKS] Post-payment task scheduling completed for invoice {}", invoice.getId());
            
        } catch (Exception e) {
            logger.error("[PAYMENT_PROC_POST_TASKS] Error scheduling post-payment tasks for invoice {}: {}", 
                        invoice.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get payment method reference for invoice.
     */
    private String getPaymentMethodRef(Invoices invoice) {
        return "pm_default_" + invoice.getCustomerId().toString().substring(0, 8);
    }
    
    /**
     * Schedule payment retry with exponential backoff.
     */
    private void schedulePaymentRetry(UUID invoiceId, UUID paymentAttemptId, UUID tenantId) {
        logger.info("Payment retry would be scheduled for invoice {} (attempt: {})", invoiceId, paymentAttemptId);
    }
}
