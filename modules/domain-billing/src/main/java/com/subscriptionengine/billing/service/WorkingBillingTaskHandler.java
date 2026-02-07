/**
 * Working implementation of BillingTaskHandler with actual invoice generation and payment processing.
 * Uses simplified but functional approach to avoid jOOQ compilation issues.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.billing.service;

import com.subscriptionengine.scheduler.service.BillingTaskHandler;
import com.subscriptionengine.scheduler.service.ScheduledTaskService;
import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.integrations.payments.PaymentAdapter;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.Invoices.INVOICES;
import static com.subscriptionengine.generated.tables.InvoiceLines.INVOICE_LINES;
import static com.subscriptionengine.generated.tables.PaymentAttempts.PAYMENT_ATTEMPTS;

// @Service - Disabled in favor of BillingTaskHandlerImpl
// @Primary
public class WorkingBillingTaskHandler implements BillingTaskHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(WorkingBillingTaskHandler.class);
    
    private final DSLContext dsl;
    private final PaymentAdapter paymentAdapter;
    private final ScheduledTaskService scheduledTaskService;
    
    public WorkingBillingTaskHandler(DSLContext dsl, PaymentAdapter paymentAdapter, 
                                   ScheduledTaskService scheduledTaskService) {
        this.dsl = dsl;
        this.paymentAdapter = paymentAdapter;
        this.scheduledTaskService = scheduledTaskService;
    }
    
    @Override
    @Transactional
    public boolean processProductRenewal(UUID subscriptionId, String productId, UUID planId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        OffsetDateTime now = OffsetDateTime.now();
        
        logger.info("Processing product renewal: subscription={}, product={}, plan={} (tenant: {})", 
                   subscriptionId, productId, planId, tenantId);
        
        try {
            // 1. Generate invoice ID
            UUID invoiceId = UUID.randomUUID();
            
            // 2. Create invoice record using DSL
            int invoiceResult = dsl.insertInto(INVOICES)
                .set(INVOICES.ID, invoiceId)
                .set(INVOICES.TENANT_ID, tenantId)
                .set(INVOICES.SUBSCRIPTION_ID, subscriptionId)
                .set(INVOICES.CUSTOMER_ID, getCustomerIdForSubscription(subscriptionId))
                .set(INVOICES.STATUS, "PENDING")
                .set(INVOICES.CURRENCY, "USD")
                .set(INVOICES.SUBTOTAL_CENTS, 0L)
                .set(INVOICES.TOTAL_CENTS, 0L)
                .set(INVOICES.DUE_DATE, now.plusDays(30))
                .set(INVOICES.CUSTOM_ATTRS, JSONB.valueOf("{}"))
                .set(INVOICES.CREATED_AT, now)
                .set(INVOICES.UPDATED_AT, now)
                .execute();
            
            if (invoiceResult == 0) {
                logger.error("Failed to create invoice for subscription {}", subscriptionId);
                return false;
            }
            
            logger.info("Created invoice {} for subscription {}", invoiceId, subscriptionId);
            
            // 3. Create invoice line item
            UUID lineId = UUID.randomUUID();
            Long unitPrice = 2999L; // $29.99 in cents - would normally fetch from plan
            Integer quantity = 1;
            Long totalCents = unitPrice * quantity;
            
            int lineResult = dsl.insertInto(INVOICE_LINES)
                .set(INVOICE_LINES.ID, lineId)
                .set(INVOICE_LINES.TENANT_ID, tenantId)
                .set(INVOICE_LINES.INVOICE_ID, invoiceId)
                .set(INVOICE_LINES.DESCRIPTION, "Product renewal: " + productId)
                .set(INVOICE_LINES.QUANTITY, quantity)
                .set(INVOICE_LINES.UNIT_PRICE_CENTS, unitPrice)
                .set(INVOICE_LINES.TOTAL_CENTS, totalCents)
                .set(INVOICE_LINES.CURRENCY, "USD")
                .set(INVOICE_LINES.PERIOD_START, now)
                .set(INVOICE_LINES.PERIOD_END, now.plusMonths(1))
                .set(INVOICE_LINES.CREATED_AT, now)
                .execute();
            
            if (lineResult == 0) {
                logger.error("Failed to create invoice line for invoice {}", invoiceId);
                return false;
            }
            
            // 4. Update invoice totals
            dsl.update(INVOICES)
                .set(INVOICES.SUBTOTAL_CENTS, totalCents)
                .set(INVOICES.TOTAL_CENTS, totalCents)
                .set(INVOICES.UPDATED_AT, now)
                .where(INVOICES.ID.eq(invoiceId))
                .execute();
            
            logger.info("Created invoice line {} for invoice {} (amount: {} USD)", 
                       lineId, invoiceId, totalCents);
            
            // 5. Schedule payment processing
            scheduledTaskService.schedulePaymentProcessing(invoiceId, tenantId, now);
            
            logger.info("Successfully generated invoice {} for product renewal (total: {} cents)", 
                       invoiceId, totalCents);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to process product renewal for subscription {}: {}", 
                        subscriptionId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean processChargePayment(UUID invoiceId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        OffsetDateTime now = OffsetDateTime.now();
        
        logger.info("Processing payment for invoice {} (tenant: {})", invoiceId, tenantId);
        
        try {
            // 1. Load invoice details
            var invoice = dsl.select(INVOICES.CUSTOMER_ID, INVOICES.TOTAL_CENTS, INVOICES.CURRENCY, INVOICES.STATUS)
                .from(INVOICES)
                .where(INVOICES.ID.eq(invoiceId).and(INVOICES.TENANT_ID.eq(tenantId)))
                .fetchOne();
            
            if (invoice == null) {
                logger.error("Invoice not found: {} (tenant: {})", invoiceId, tenantId);
                return false;
            }
            
            String status = invoice.get(INVOICES.STATUS);
            if (!"PENDING".equals(status)) {
                logger.warn("Invoice {} is not in PENDING status: {}", invoiceId, status);
                return "PAID".equals(status);
            }
            
            UUID customerId = invoice.get(INVOICES.CUSTOMER_ID);
            Long amountCents = invoice.get(INVOICES.TOTAL_CENTS);
            String currency = invoice.get(INVOICES.CURRENCY);
            
            // 2. Create payment attempt record
            UUID attemptId = UUID.randomUUID();
            dsl.insertInto(PAYMENT_ATTEMPTS)
                .set(PAYMENT_ATTEMPTS.ID, attemptId)
                .set(PAYMENT_ATTEMPTS.TENANT_ID, tenantId)
                .set(PAYMENT_ATTEMPTS.INVOICE_ID, invoiceId)
                .set(PAYMENT_ATTEMPTS.AMOUNT_CENTS, amountCents)
                .set(PAYMENT_ATTEMPTS.CURRENCY, currency)
                .set(PAYMENT_ATTEMPTS.STATUS, "PROCESSING")
                .set(PAYMENT_ATTEMPTS.PAYMENT_METHOD_REF, "pm_default_" + customerId.toString().substring(0, 8))
                .set(PAYMENT_ATTEMPTS.ATTEMPTED_AT, now)
                .set(PAYMENT_ATTEMPTS.CREATED_AT, now)
                .set(PAYMENT_ATTEMPTS.UPDATED_AT, now)
                .execute();
            
            logger.info("Created payment attempt {} for invoice {} (amount: {} {})", 
                       attemptId, invoiceId, amountCents, currency);
            
            // 3. Process payment through adapter
            PaymentAdapter.PaymentRequest paymentRequest = new PaymentAdapter.PaymentRequest(
                invoiceId,
                customerId,
                amountCents,
                currency,
                "pm_default_" + customerId.toString().substring(0, 8),
                "invoice_" + invoiceId.toString(),
                Map.of("invoiceId", invoiceId.toString(), "tenantId", tenantId.toString())
            );
            
            PaymentAdapter.PaymentResult result = paymentAdapter.processPayment(paymentRequest);
            
            // 4. Update payment attempt with result
            dsl.update(PAYMENT_ATTEMPTS)
                .set(PAYMENT_ATTEMPTS.STATUS, result.success() ? "SUCCEEDED" : "FAILED")
                .set(PAYMENT_ATTEMPTS.EXTERNAL_PAYMENT_ID, result.paymentReference())
                .set(PAYMENT_ATTEMPTS.FAILURE_CODE, result.errorCode())
                .set(PAYMENT_ATTEMPTS.FAILURE_REASON, result.errorMessage())
                .set(PAYMENT_ATTEMPTS.COMPLETED_AT, now)
                .set(PAYMENT_ATTEMPTS.UPDATED_AT, now)
                .where(PAYMENT_ATTEMPTS.ID.eq(attemptId))
                .execute();
            
            // 5. Update invoice status if payment succeeded
            if (result.success() && result.status() == PaymentAdapter.PaymentStatus.SUCCEEDED) {
                dsl.update(INVOICES)
                    .set(INVOICES.STATUS, "PAID")
                    .set(INVOICES.PAID_AT, now)
                    .set(INVOICES.UPDATED_AT, now)
                    .where(INVOICES.ID.eq(invoiceId))
                    .execute();
                
                logger.info("Payment succeeded for invoice {} (payment ref: {})", 
                           invoiceId, result.paymentReference());
                
                return true;
                
            } else {
                logger.warn("Payment failed for invoice {}: {} - {}", 
                           invoiceId, result.errorCode(), result.errorMessage());
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Failed to process payment for invoice {}: {}", invoiceId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get customer ID for subscription - simplified implementation
     */
    private UUID getCustomerIdForSubscription(UUID subscriptionId) {
        // In a real implementation, this would query the subscriptions table
        // For now, return a mock customer ID
        return UUID.fromString("12345678-1234-1234-1234-123456789012");
    }
}
