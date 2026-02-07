/**
 * Mock implementation of CommerceAdapter for development and testing.
 * Simulates order creation without calling external services.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.integrations.commerce.impl;

import com.subscriptionengine.integrations.commerce.CommerceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock commerce adapter that simulates order creation.
 * Useful for development and testing without external dependencies.
 */
@Service
public class MockCommerceAdapter implements CommerceAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(MockCommerceAdapter.class);
    
    // In-memory storage for mock orders
    private final Map<String, OrderStatus> orderStatuses = new ConcurrentHashMap<>();
    
    @Override
    public OrderResult createOrder(OrderRequest request) {
        logger.info("[MOCK_COMMERCE] Creating order for delivery {} customer {} with {} items", 
                   request.deliveryId(), request.customerId(), request.items().size());
        
        try {
            // Simulate order processing
            String externalOrderRef = "mock_order_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Calculate total for logging
            long totalCents = request.items().stream()
                    .mapToLong(OrderItem::totalCents)
                    .sum();
            
            logger.info("[MOCK_COMMERCE] Processing order: {} items, total: {} {}", 
                       request.items().size(), totalCents, request.currency());
            
            // Log order items
            for (OrderItem item : request.items()) {
                logger.debug("[MOCK_COMMERCE] Item: {} x{} @ {} {} = {} {}", 
                           item.productName(), item.quantity(), 
                           item.unitPriceCents(), request.currency(),
                           item.totalCents(), request.currency());
            }
            
            // Log shipping address
            if (request.shippingAddress() != null) {
                logger.info("[MOCK_COMMERCE] Shipping to: {}, {}, {} {}", 
                           request.shippingAddress().line1(),
                           request.shippingAddress().city(),
                           request.shippingAddress().state(),
                           request.shippingAddress().postalCode());
            }
            
            // Store order status
            orderStatuses.put(externalOrderRef, OrderStatus.CONFIRMED);
            
            // Create provider data
            Map<String, Object> providerData = Map.of(
                "provider", "mock",
                "simulatedAt", System.currentTimeMillis(),
                "processingTime", "instant",
                "orderTotal", totalCents,
                "currency", request.currency(),
                "itemCount", request.items().size(),
                "createdAt", OffsetDateTime.now().toString()
            );
            
            logger.info("[MOCK_COMMERCE] Order created successfully: {}", externalOrderRef);
            
            return OrderResult.success(externalOrderRef, OrderStatus.CONFIRMED, providerData);
            
        } catch (Exception e) {
            logger.error("[MOCK_COMMERCE] Failed to create order for delivery {}: {}", 
                        request.deliveryId(), e.getMessage(), e);
            return OrderResult.failure("mock_error", "Mock order creation failed: " + e.getMessage());
        }
    }
    
    @Override
    public OrderStatus getOrderStatus(String externalOrderRef) {
        logger.debug("[MOCK_COMMERCE] Getting status for order: {}", externalOrderRef);
        return orderStatuses.getOrDefault(externalOrderRef, OrderStatus.PENDING);
    }
    
    @Override
    public OrderResult cancelOrder(String externalOrderRef) {
        logger.info("[MOCK_COMMERCE] Cancelling order: {}", externalOrderRef);
        
        OrderStatus currentStatus = orderStatuses.get(externalOrderRef);
        if (currentStatus == null) {
            return OrderResult.failure("order_not_found", "Order not found: " + externalOrderRef);
        }
        
        if (currentStatus == OrderStatus.SHIPPED || currentStatus == OrderStatus.DELIVERED) {
            return OrderResult.failure("cannot_cancel", "Cannot cancel order in status: " + currentStatus);
        }
        
        orderStatuses.put(externalOrderRef, OrderStatus.CANCELLED);
        
        Map<String, Object> providerData = Map.of(
            "provider", "mock",
            "cancelledAt", System.currentTimeMillis(),
            "previousStatus", currentStatus.toString()
        );
        
        return OrderResult.success(externalOrderRef, OrderStatus.CANCELLED, providerData);
    }
    
    @Override
    public String getProviderName() {
        return "mock";
    }
    
    @Override
    public boolean supportsProductType(String productType) {
        // Mock adapter supports all product types
        return true;
    }
}
