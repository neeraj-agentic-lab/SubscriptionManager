package com.subscriptionengine.api.integration;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Factory for creating test data objects.
 * 
 * @author Neeraj Yadav
 */
public class TestDataFactory {
    
    public static final String DEFAULT_TENANT_ID = "5aa82d8e-ebec-432b-b568-ac4ba61bb578";
    public static final String DEFAULT_CUSTOMER_ID = "04c07454-917b-4c6d-9944-ee9bbaeec0a4";
    
    /**
     * Create a subscription request with default values.
     */
    public static Map<String, Object> createSubscriptionRequest() {
        return createSubscriptionRequest(UUID.randomUUID(), UUID.randomUUID());
    }
    
    /**
     * Create a subscription request with specific customer and plan.
     */
    public static Map<String, Object> createSubscriptionRequest(UUID customerId, UUID planId) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId.toString());
        request.put("planId", planId.toString());
        request.put("startDate", OffsetDateTime.now().toString());
        request.put("paymentMethodRef", "pm_test_" + UUID.randomUUID().toString().substring(0, 8));
        
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("line1", "123 Test St");
        shippingAddress.put("city", "San Francisco");
        shippingAddress.put("state", "CA");
        shippingAddress.put("postalCode", "94102");
        shippingAddress.put("country", "US");
        request.put("shippingAddress", shippingAddress);
        
        return request;
    }
    
    /**
     * Create a plan request with default values.
     */
    public static Map<String, Object> createPlanRequest() {
        return createPlanRequest("Test Plan " + UUID.randomUUID().toString().substring(0, 8));
    }
    
    /**
     * Create a plan request with specific name.
     */
    public static Map<String, Object> createPlanRequest(String name) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("description", "Test plan for integration testing");
        request.put("basePriceCents", 2999);
        request.put("currency", "USD");
        request.put("billingInterval", "MONTHLY");
        request.put("trialPeriodDays", 0);
        request.put("active", true);
        
        return request;
    }
    
    /**
     * Create a customer request with default values.
     */
    public static Map<String, Object> createCustomerRequest() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return createCustomerRequest("test-" + uniqueId + "@example.com");
    }
    
    /**
     * Create a customer request with specific email.
     */
    public static Map<String, Object> createCustomerRequest(String email) {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("name", "Test Customer");
        request.put("externalCustomerRef", "cust_test_" + UUID.randomUUID().toString().substring(0, 8));
        
        return request;
    }
    
    /**
     * Create a webhook registration request.
     */
    public static Map<String, Object> createWebhookRequest(String url) {
        Map<String, Object> request = new HashMap<>();
        request.put("url", url);
        request.put("events", new String[]{"subscription.created", "subscription.canceled", "delivery.canceled"});
        request.put("description", "Test webhook endpoint");
        
        return request;
    }
    
    /**
     * Create a pause subscription request.
     */
    public static Map<String, Object> createPauseRequest(UUID customerId) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId.toString());
        request.put("operation", "PAUSE");
        request.put("reason", "Customer requested pause for testing");
        
        return request;
    }
    
    /**
     * Create a resume subscription request.
     */
    public static Map<String, Object> createResumeRequest(UUID customerId) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId.toString());
        request.put("operation", "RESUME");
        
        return request;
    }
    
    /**
     * Create a cancel subscription request.
     */
    public static Map<String, Object> createCancelRequest(UUID customerId, boolean immediate) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId.toString());
        request.put("operation", "CANCEL");
        request.put("cancellationType", immediate ? "IMMEDIATE" : "END_OF_PERIOD");
        request.put("reason", "Customer requested cancellation for testing");
        
        return request;
    }
    
    /**
     * Create a delivery cancellation request.
     */
    public static Map<String, Object> createDeliveryCancelRequest(UUID customerId) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId.toString());
        request.put("reason", "Customer requested delivery cancellation for testing");
        
        return request;
    }
}
