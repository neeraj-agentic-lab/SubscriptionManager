/**
 * Commerce adapter interface for creating orders with external commerce platforms.
 * Provides extensible design for integrating with various commerce providers.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.integrations.commerce;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for commerce platform adapters.
 * Implementations handle integration with specific commerce platforms (Shopify, WooCommerce, etc.).
 */
public interface CommerceAdapter {
    
    /**
     * Create an order in the external commerce platform.
     * 
     * @param request Order creation request
     * @return Order creation result
     */
    OrderResult createOrder(OrderRequest request);
    
    /**
     * Get the status of an existing order.
     * 
     * @param externalOrderRef External order reference from the provider
     * @return Current order status
     */
    OrderStatus getOrderStatus(String externalOrderRef);
    
    /**
     * Cancel an order if possible.
     * 
     * @param externalOrderRef External order reference from the provider
     * @return Cancellation result
     */
    OrderResult cancelOrder(String externalOrderRef);
    
    /**
     * Get the provider name for this adapter.
     * 
     * @return Provider name (e.g., "shopify", "woocommerce", "mock")
     */
    String getProviderName();
    
    /**
     * Check if this adapter supports the given product type.
     * 
     * @param productType Product type (e.g., "physical", "digital", "hybrid")
     * @return true if supported
     */
    boolean supportsProductType(String productType);
    
    /**
     * Order creation request.
     */
    record OrderRequest(
        UUID deliveryId,
        UUID customerId,
        UUID subscriptionId,
        String customerEmail,
        String customerName,
        ShippingAddress shippingAddress,
        java.util.List<OrderItem> items,
        String currency,
        String idempotencyKey,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Shipping address information.
     */
    record ShippingAddress(
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        String phone
    ) {}
    
    /**
     * Order item information.
     */
    record OrderItem(
        String productId,
        String productName,
        String planId,
        String planName,
        Integer quantity,
        Long unitPriceCents,
        Long totalCents,
        String currency,
        Map<String, Object> productMetadata
    ) {}
    
    /**
     * Order creation result.
     */
    record OrderResult(
        boolean success,
        String externalOrderRef,
        OrderStatus status,
        String errorCode,
        String errorMessage,
        Map<String, Object> providerData
    ) {
        
        /**
         * Create successful order result.
         */
        public static OrderResult success(String externalOrderRef, OrderStatus status, Map<String, Object> providerData) {
            return new OrderResult(true, externalOrderRef, status, null, null, providerData);
        }
        
        /**
         * Create failed order result.
         */
        public static OrderResult failure(String errorCode, String errorMessage) {
            return new OrderResult(false, null, OrderStatus.FAILED, errorCode, errorMessage, Map.of());
        }
        
        /**
         * Create failed order result with provider data.
         */
        public static OrderResult failure(String errorCode, String errorMessage, Map<String, Object> providerData) {
            return new OrderResult(false, null, OrderStatus.FAILED, errorCode, errorMessage, providerData);
        }
    }
    
    /**
     * Order status enumeration.
     */
    enum OrderStatus {
        PENDING,     // Order is being processed
        CONFIRMED,   // Order confirmed and ready for fulfillment
        SHIPPED,     // Order has been shipped
        DELIVERED,   // Order has been delivered
        CANCELLED,   // Order was cancelled
        FAILED,      // Order creation/processing failed
        REFUNDED     // Order was refunded
    }
}
