package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for subscription modification operations.
 * Tests: Plan changes, quantity updates, address changes, payment method updates.
 */
@Epic("Subscription Management")
@Feature("Subscription Modification")
class SubscriptionModificationTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @DisplayName("Should change subscription plan")
    @Description("Tests changing from one plan to another")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Plan modification")
    void shouldChangeSubscriptionPlan() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID plan1 = createPlan(tenantId, "Basic Plan");
        UUID plan2 = createPlan(tenantId, "Premium Plan");
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), plan1);
        
        // Change plan
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customer.get("customerId"));
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("newPlanId", plan2.toString());
        
        Response response = givenAuthenticated(tenantId)
            .body(modifyRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        
        // Verify plan changed
        Response subResponse = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(subResponse.jsonPath().getString("planId")).isEqualTo(plan2.toString());
        
        Allure.addAttachment("Plan Change Response", "application/json", response.asString());
        Allure.addAttachment("Updated Subscription", "application/json", subResponse.asString());
    }
    
    @Test
    @DisplayName("Should update subscription quantity")
    @Description("Tests changing subscription item quantity")
    @Severity(SeverityLevel.NORMAL)
    @Story("Quantity modification")
    void shouldUpdateSubscriptionQuantity() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Update quantity
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customer.get("customerId"));
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("newQuantity", 5);
        
        Response response = givenAuthenticated(tenantId)
            .body(modifyRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Quantity Update Response", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should update shipping address")
    @Description("Tests changing subscription shipping address")
    @Severity(SeverityLevel.NORMAL)
    @Story("Address modification")
    void shouldUpdateShippingAddress() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Update address
        Map<String, Object> newAddress = new HashMap<>();
        newAddress.put("line1", "456 New St");
        newAddress.put("city", "New York");
        newAddress.put("state", "NY");
        newAddress.put("postalCode", "10001");
        newAddress.put("country", "US");
        
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customer.get("customerId"));
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("shippingAddress", newAddress);
        
        Response response = givenAuthenticated(tenantId)
            .body(modifyRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Address Update Response", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should update payment method")
    @Description("Tests changing subscription payment method")
    @Severity(SeverityLevel.NORMAL)
    @Story("Payment method modification")
    void shouldUpdatePaymentMethod() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Update payment method
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customer.get("customerId"));
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("paymentMethodRef", "pm_new_" + UUID.randomUUID().toString().substring(0, 8));
        
        Response response = givenAuthenticated(tenantId)
            .body(modifyRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Payment Method Update Response", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should not allow modification of canceled subscription")
    @Description("Verifies that canceled subscriptions cannot be modified")
    @Severity(SeverityLevel.NORMAL)
    @Story("Error handling")
    void shouldNotAllowModificationOfCanceledSubscription() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Cancel subscription
        cancelSubscription(tenantId, subscriptionId, customer.get("customerId"));
        
        // Try to modify
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customer.get("customerId"));
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("newQuantity", 5);
        
        Response response = givenAuthenticated(tenantId)
            .body(modifyRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isFalse();
        Allure.addAttachment("Modification Rejected", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should allow modification of paused subscription")
    @Description("Verifies that paused subscriptions can be modified (business rule: only canceled subscriptions cannot be modified)")
    @Severity(SeverityLevel.NORMAL)
    @Story("Paused subscription modification")
    void shouldAllowModificationOfPausedSubscription() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Pause subscription
        pauseSubscription(tenantId, subscriptionId, customer.get("customerId"));
        
        // Modify paused subscription (should succeed)
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customer.get("customerId"));
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("newQuantity", 5);
        
        Response response = givenAuthenticated(tenantId)
            .body(modifyRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Paused Subscription Modified", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should update multiple fields in single request")
    @Description("Tests modifying plan, quantity, and address together")
    @Severity(SeverityLevel.NORMAL)
    @Story("Bulk modification")
    void shouldUpdateMultipleFieldsTogether() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID plan1 = createPlan(tenantId, "Basic");
        UUID plan2 = createPlan(tenantId, "Premium");
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), plan1);
        
        // Update multiple fields
        Map<String, Object> newAddress = new HashMap<>();
        newAddress.put("line1", "789 Multi St");
        newAddress.put("city", "Boston");
        newAddress.put("state", "MA");
        newAddress.put("postalCode", "02101");
        newAddress.put("country", "US");
        
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customer.get("customerId"));
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("newPlanId", plan2.toString());
        modifyRequest.put("newQuantity", 3);
        modifyRequest.put("shippingAddress", newAddress);
        modifyRequest.put("paymentMethodRef", "pm_multi_" + UUID.randomUUID().toString().substring(0, 8));
        
        Response response = givenAuthenticated(tenantId)
            .body(modifyRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Bulk Modification Response", "application/json", response.asString());
    }
    
    // Helper methods
    
    @Step("Create customer")
    private Map<String, String> createCustomer(String tenantId) {
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String email = response.jsonPath().getString("data.email");
        String customerId = response.jsonPath().getString("data.customerId");
        return Map.of("email", email, "customerId", customerId);
    }
    
    @Step("Create plan with name: {name}")
    private UUID createPlan(String tenantId, String name) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest(name);
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create plan")
    private UUID createPlan(String tenantId) {
        return createPlan(tenantId, "Test Plan " + UUID.randomUUID().toString().substring(0, 8));
    }
    
    @Step("Create subscription")
    private UUID createSubscription(String tenantId, String customerEmail, UUID planId) {
        Map<String, Object> subscriptionRequest = new HashMap<>();
        subscriptionRequest.put("planId", planId.toString());
        subscriptionRequest.put("customerEmail", customerEmail);
        subscriptionRequest.put("startDate", java.time.OffsetDateTime.now().toString());
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Pause subscription")
    private void pauseSubscription(String tenantId, UUID subscriptionId, String customerId) {
        Map<String, Object> pauseRequest = Map.of(
            "operation", "PAUSE",
            "customerId", customerId
        );
        
        givenAuthenticated(tenantId)
            .body(pauseRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200);
    }
    
    @Step("Cancel subscription")
    private void cancelSubscription(String tenantId, UUID subscriptionId, String customerId) {
        Map<String, Object> cancelRequest = Map.of(
            "operation", "CANCEL",
            "customerId", customerId,
            "cancellationType", "immediate"
        );
        
        givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200);
    }
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-modification-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Subscription Modification",
            "slug", slug,
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return response.jsonPath().getString("id");
    }
}
