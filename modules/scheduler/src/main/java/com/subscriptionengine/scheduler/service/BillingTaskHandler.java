/**
 * Interface for handling billing-related tasks.
 * Provides abstraction to avoid circular dependencies between scheduler and billing modules.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.scheduler.service;

import java.util.UUID;

/**
 * Handler interface for billing-related scheduled tasks.
 * Implementations should be provided by the billing module.
 */
public interface BillingTaskHandler {
    
    /**
     * Process a product renewal task.
     * 
     * @param subscriptionId Subscription ID
     * @param productId Product ID
     * @param planId Plan ID
     * @return true if processing was successful
     */
    boolean processProductRenewal(UUID subscriptionId, String productId, UUID planId);
    
    /**
     * Process a charge payment task.
     * 
     * @param invoiceId Invoice ID to process payment for
     * @return true if payment processing was successful
     */
    boolean processChargePayment(UUID invoiceId);
}
