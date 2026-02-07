/**
 * Implementation of BillingTaskHandler for processing billing-related scheduled tasks.
 * Bridges the scheduler module with billing services to avoid circular dependencies.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.billing.service;

import com.subscriptionengine.scheduler.service.BillingTaskHandler;
import com.subscriptionengine.generated.tables.daos.SubscriptionsDao;
import com.subscriptionengine.generated.tables.pojos.Subscriptions;
import com.subscriptionengine.generated.tables.pojos.Plans;
import com.subscriptionengine.generated.tables.daos.PlansDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Primary
public class BillingTaskHandlerImpl implements BillingTaskHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingTaskHandlerImpl.class);
    
    private final InvoiceGenerationService invoiceGenerationService;
    private final PaymentProcessingService paymentProcessingService;
    private final SubscriptionsDao subscriptionsDao;
    private final PlansDao plansDao;
    
    public BillingTaskHandlerImpl(InvoiceGenerationService invoiceGenerationService,
                                 PaymentProcessingService paymentProcessingService,
                                 SubscriptionsDao subscriptionsDao,
                                 PlansDao plansDao) {
        this.invoiceGenerationService = invoiceGenerationService;
        this.paymentProcessingService = paymentProcessingService;
        this.subscriptionsDao = subscriptionsDao;
        this.plansDao = plansDao;
        logger.info("[BILLING_HANDLER_INIT] BillingTaskHandlerImpl initialized with InvoiceGenerationService, PaymentProcessingService, and DAOs");
    }
    
    @Override
    public boolean processProductRenewal(UUID subscriptionId, String productId, UUID planId) {
        logger.info("[BILLING_FLOW_START] Processing product renewal: subscription={}, product={}, plan={}", 
                   subscriptionId, productId, planId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("[BILLING_FLOW_STEP_1] Calling InvoiceGenerationService.generateInvoiceForProductRenewal");
            
            // Generate invoice for this product renewal
            UUID invoiceId = invoiceGenerationService.generateInvoiceForProductRenewal(
                subscriptionId, productId, planId);
            
            long invoiceGenerationTime = System.currentTimeMillis() - startTime;
            logger.info("[BILLING_FLOW_SUCCESS] Generated invoice {} for product renewal (subscription: {}, product: {}) in {}ms", 
                       invoiceId, subscriptionId, productId, invoiceGenerationTime);
            
            logger.debug("[BILLING_FLOW_STEP_2] Invoice generation completed, payment processing will be handled by CHARGE_PAYMENT task");
            
            // Update subscription's next_renewal_at for the next billing cycle
            logger.debug("[BILLING_FLOW_STEP_3] Updating subscription next_renewal_at for next billing cycle");
            updateSubscriptionRenewalDate(subscriptionId, planId);
            
            // Payment processing will be handled by the CHARGE_PAYMENT task that was scheduled
            logger.info("[BILLING_FLOW_END] Product renewal processing completed successfully for subscription {} in {}ms", 
                       subscriptionId, System.currentTimeMillis() - startTime);
            return true;
            
        } catch (Exception e) {
            long failureTime = System.currentTimeMillis() - startTime;
            logger.error("[BILLING_FLOW_ERROR] Failed to process product renewal for subscription {} product {} after {}ms: {}", 
                        subscriptionId, productId, failureTime, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean processChargePayment(UUID invoiceId) {
        logger.info("[PAYMENT_FLOW_START] Processing payment through billing handler: invoice={}", invoiceId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("[PAYMENT_FLOW_STEP_1] Calling PaymentProcessingService.processPayment");
            
            boolean success = paymentProcessingService.processPayment(invoiceId);
            
            long paymentProcessingTime = System.currentTimeMillis() - startTime;
            
            if (success) {
                logger.info("[PAYMENT_FLOW_SUCCESS] Payment processed successfully for invoice {} in {}ms", 
                           invoiceId, paymentProcessingTime);
            } else {
                logger.warn("[PAYMENT_FLOW_FAILURE] Payment processing failed for invoice {} after {}ms", 
                           invoiceId, paymentProcessingTime);
            }
            
            logger.info("[PAYMENT_FLOW_END] Payment processing completed for invoice {} with result: {} in {}ms", 
                       invoiceId, success, paymentProcessingTime);
            return success;
            
        } catch (Exception e) {
            long failureTime = System.currentTimeMillis() - startTime;
            logger.error("[PAYMENT_FLOW_ERROR] Error processing payment for invoice {} after {}ms: {}", 
                        invoiceId, failureTime, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Update subscription's next_renewal_at to the next billing cycle.
     */
    private void updateSubscriptionRenewalDate(UUID subscriptionId, UUID planId) {
        try {
            logger.debug("[BILLING_RENEWAL_UPDATE] Updating next_renewal_at for subscription {} plan {}", subscriptionId, planId);
            
            // Load subscription
            Subscriptions subscription = subscriptionsDao.fetchOneById(subscriptionId);
            if (subscription == null) {
                logger.error("[BILLING_RENEWAL_UPDATE_ERROR] Subscription not found: {}", subscriptionId);
                return;
            }
            
            // Load plan to get billing interval
            Plans plan = plansDao.fetchOneById(planId);
            if (plan == null) {
                logger.error("[BILLING_RENEWAL_UPDATE_ERROR] Plan not found: {}", planId);
                return;
            }
            
            // Calculate next renewal date based on plan's billing interval
            OffsetDateTime currentRenewalAt = subscription.getNextRenewalAt();
            if (currentRenewalAt == null) {
                currentRenewalAt = subscription.getCurrentPeriodEnd();
            }
            
            OffsetDateTime nextRenewalAt = calculateNextRenewalDate(currentRenewalAt, 
                plan.getBillingInterval(), plan.getBillingIntervalCount());
            
            // Update subscription
            subscription.setNextRenewalAt(nextRenewalAt);
            subscription.setUpdatedAt(OffsetDateTime.now());
            
            subscriptionsDao.update(subscription);
            
            logger.info("[BILLING_RENEWAL_UPDATE_SUCCESS] Updated subscription {} next_renewal_at to {}", 
                       subscriptionId, nextRenewalAt);
            
        } catch (Exception e) {
            logger.error("[BILLING_RENEWAL_UPDATE_ERROR] Failed to update renewal date for subscription {}: {}", 
                        subscriptionId, e.getMessage(), e);
        }
    }
    
    /**
     * Calculate the next renewal date based on billing interval.
     */
    private OffsetDateTime calculateNextRenewalDate(OffsetDateTime currentDate, String billingInterval, Integer intervalCount) {
        if (intervalCount == null || intervalCount <= 0) {
            intervalCount = 1;
        }
        
        switch (billingInterval.toUpperCase()) {
            case "DAILY":
                return currentDate.plusDays(intervalCount);
            case "WEEKLY":
                return currentDate.plusWeeks(intervalCount);
            case "MONTHLY":
                return currentDate.plusMonths(intervalCount);
            case "QUARTERLY":
                return currentDate.plusMonths(3L * intervalCount);
            case "YEARLY":
                return currentDate.plusYears(intervalCount);
            default:
                logger.warn("[BILLING_RENEWAL_CALC] Unknown billing interval: {}, defaulting to monthly", billingInterval);
                return currentDate.plusMonths(intervalCount);
        }
    }
}
