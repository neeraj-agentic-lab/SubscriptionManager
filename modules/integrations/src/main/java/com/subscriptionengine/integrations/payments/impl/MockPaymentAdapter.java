/**
 * Mock payment adapter for testing and development.
 * Simulates payment processing without external API calls.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.integrations.payments.impl;

import com.subscriptionengine.integrations.payments.PaymentAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock implementation of PaymentAdapter for testing and development.
 * Configurable to simulate various payment scenarios.
 */
@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentAdapter implements PaymentAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(MockPaymentAdapter.class);
    
    // Configuration for testing different scenarios
    private final double successRate;
    private final long processingDelayMs;
    
    public MockPaymentAdapter() {
        // Default configuration - 90% success rate, 100ms delay
        this.successRate = 0.9;
        this.processingDelayMs = 100;
    }
    
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        logger.info("Processing mock payment for invoice {} amount {} {} (customer: {})", 
                   request.invoiceId(), request.amountCents(), request.currency(), request.customerId());
        
        // Simulate processing delay
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate payment outcome based on success rate
        boolean isSuccess = ThreadLocalRandom.current().nextDouble() < successRate;
        
        if (isSuccess) {
            String paymentReference = "mock_payment_" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> providerData = Map.of(
                "provider", "mock",
                "processingTime", processingDelayMs,
                "mockTransactionId", paymentReference,
                "simulatedAt", System.currentTimeMillis()
            );
            
            logger.info("Mock payment succeeded: {} for invoice {} (amount: {} {})", 
                       paymentReference, request.invoiceId(), request.amountCents(), request.currency());
            
            return PaymentResult.success(paymentReference, PaymentStatus.SUCCEEDED, providerData);
        } else {
            // Simulate different failure reasons
            String[] errorCodes = {"insufficient_funds", "card_declined", "expired_card", "processing_error"};
            String[] errorMessages = {
                "Insufficient funds in account",
                "Card was declined by issuer", 
                "Card has expired",
                "Payment processing error occurred"
            };
            
            int errorIndex = ThreadLocalRandom.current().nextInt(errorCodes.length);
            String errorCode = errorCodes[errorIndex];
            String errorMessage = errorMessages[errorIndex];
            
            Map<String, Object> providerData = Map.of(
                "provider", "mock",
                "processingTime", processingDelayMs,
                "simulatedAt", System.currentTimeMillis(),
                "failureReason", errorCode
            );
            
            logger.warn("Mock payment failed: {} for invoice {} (amount: {} {})", 
                       errorCode, request.invoiceId(), request.amountCents(), request.currency());
            
            return PaymentResult.failure(errorCode, errorMessage, providerData);
        }
    }
    
    @Override
    public PaymentStatus getPaymentStatus(String paymentReference) {
        logger.debug("Getting mock payment status for reference: {}", paymentReference);
        
        // Mock payments are immediately final
        if (paymentReference.startsWith("mock_payment_")) {
            return PaymentStatus.SUCCEEDED;
        }
        
        return PaymentStatus.FAILED;
    }
    
    @Override
    public PaymentResult cancelPayment(String paymentReference) {
        logger.info("Cancelling mock payment: {}", paymentReference);
        
        Map<String, Object> providerData = Map.of(
            "provider", "mock",
            "cancelledAt", System.currentTimeMillis()
        );
        
        return PaymentResult.success(paymentReference, PaymentStatus.CANCELLED, providerData);
    }
    
    @Override
    public PaymentResult refundPayment(String paymentReference, Long amountCents, String reason) {
        logger.info("Refunding mock payment: {} amount: {} reason: {}", 
                   paymentReference, amountCents, reason);
        
        Map<String, Object> providerData = Map.of(
            "provider", "mock",
            "refundedAt", System.currentTimeMillis(),
            "refundAmount", amountCents != null ? amountCents : "full",
            "refundReason", reason != null ? reason : "no_reason_provided"
        );
        
        return PaymentResult.success(paymentReference, PaymentStatus.REFUNDED, providerData);
    }
    
    @Override
    public String getProviderName() {
        return "mock";
    }
    
    @Override
    public boolean supportsPaymentMethod(String paymentMethodType) {
        // Mock adapter supports all payment method types
        return true;
    }
}
