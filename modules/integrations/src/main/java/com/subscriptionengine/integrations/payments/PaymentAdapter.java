/**
 * Payment adapter interface for processing subscription payments.
 * Provides extensible design for integrating with various payment providers.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.integrations.payments;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for payment processing adapters.
 * Implementations handle integration with specific payment providers (Stripe, Adyen, etc.).
 */
public interface PaymentAdapter {
    
    /**
     * Process a payment for an invoice.
     * 
     * @param request Payment processing request
     * @return Payment processing result
     */
    PaymentResult processPayment(PaymentRequest request);
    
    /**
     * Retrieve the status of a payment.
     * 
     * @param paymentReference External payment reference from the provider
     * @return Current payment status
     */
    PaymentStatus getPaymentStatus(String paymentReference);
    
    /**
     * Cancel or void a payment if possible.
     * 
     * @param paymentReference External payment reference from the provider
     * @return Cancellation result
     */
    PaymentResult cancelPayment(String paymentReference);
    
    /**
     * Refund a completed payment.
     * 
     * @param paymentReference External payment reference from the provider
     * @param amountCents Amount to refund in cents (null for full refund)
     * @param reason Refund reason
     * @return Refund result
     */
    PaymentResult refundPayment(String paymentReference, Long amountCents, String reason);
    
    /**
     * Get the provider name for this adapter.
     * 
     * @return Provider name (e.g., "stripe", "adyen", "mock")
     */
    String getProviderName();
    
    /**
     * Check if this adapter supports the given payment method type.
     * 
     * @param paymentMethodType Payment method type (e.g., "card", "bank_transfer", "wallet")
     * @return true if supported
     */
    boolean supportsPaymentMethod(String paymentMethodType);
    
    /**
     * Payment processing request.
     */
    record PaymentRequest(
        UUID invoiceId,
        UUID customerId,
        Long amountCents,
        String currency,
        String paymentMethodRef,
        String idempotencyKey,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Payment processing result.
     */
    record PaymentResult(
        boolean success,
        String paymentReference,
        PaymentStatus status,
        String errorCode,
        String errorMessage,
        Map<String, Object> providerData
    ) {
        
        /**
         * Create successful payment result.
         */
        public static PaymentResult success(String paymentReference, PaymentStatus status, Map<String, Object> providerData) {
            return new PaymentResult(true, paymentReference, status, null, null, providerData);
        }
        
        /**
         * Create failed payment result.
         */
        public static PaymentResult failure(String errorCode, String errorMessage) {
            return new PaymentResult(false, null, PaymentStatus.FAILED, errorCode, errorMessage, Map.of());
        }
        
        /**
         * Create failed payment result with provider data.
         */
        public static PaymentResult failure(String errorCode, String errorMessage, Map<String, Object> providerData) {
            return new PaymentResult(false, null, PaymentStatus.FAILED, errorCode, errorMessage, providerData);
        }
    }
    
    /**
     * Payment status enumeration.
     */
    enum PaymentStatus {
        PENDING,     // Payment is being processed
        SUCCEEDED,   // Payment completed successfully
        FAILED,      // Payment failed
        CANCELLED,   // Payment was cancelled
        REFUNDED,    // Payment was refunded
        REQUIRES_ACTION  // Payment requires additional customer action (3DS, etc.)
    }
}
