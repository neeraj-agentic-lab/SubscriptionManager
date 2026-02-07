package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for customer dashboard endpoints.
 * Tests: Customer subscription listing and detailed dashboard view.
 */
@Epic("Customer Dashboard")
@Feature("Dashboard APIs")
class CustomerDashboardTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @DisplayName("Should list all subscriptions for a customer")
    @Description("Tests GET /v1/customers/{customerId}/subscriptions endpoint")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Customer subscription listing")
    void shouldListCustomerSubscriptions() {
        String tenantId = testTenantId;
        
        // Create customer with multiple subscriptions
        Map<String, String> customer = createCustomer(tenantId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID planId1 = createPlan(tenantId, "Plan 1");
        UUID planId2 = createPlan(tenantId, "Plan 2");
        
        UUID sub1 = createSubscription(tenantId, customer.get("email"), planId1);
        UUID sub2 = createSubscription(tenantId, customer.get("email"), planId2);
        
        // List customer subscriptions
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/customers/" + customerId + "/subscriptions")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> subscriptions = response.jsonPath().getList("data.subscriptions");
        assertThat(subscriptions).hasSizeGreaterThanOrEqualTo(2);
        
        // Verify subscription data includes plan info
        Map<String, Object> firstSub = subscriptions.get(0);
        assertThat(firstSub).containsKeys("subscriptionId", "status", "plan");
        
        Map<String, Object> plan = (Map<String, Object>) firstSub.get("plan");
        assertThat(plan).containsKeys("planId", "name", "basePriceCents", "currency", "billingInterval");
        
        Allure.addAttachment("Customer Subscriptions", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should retrieve subscription dashboard with management capabilities")
    @Description("Tests GET /v1/customers/{customerId}/subscriptions/{id}/dashboard endpoint")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Subscription dashboard")
    void shouldRetrieveSubscriptionDashboard() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Get dashboard
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/customers/" + customerId + "/subscriptions/" + subscriptionId + "/dashboard")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Map<String, Object> dashboard = response.jsonPath().getMap("data");
        
        // Verify subscription details (flat structure)
        assertThat(dashboard).containsKeys("subscriptionId", "status", "canPause", "canResume", "canCancel", "upcomingDeliveries");
        assertThat(dashboard.get("status")).isEqualTo("ACTIVE");
        
        // Verify capabilities (flat structure)
        assertThat(dashboard)
            .containsEntry("canPause", true)
            .containsEntry("canResume", false)
            .containsEntry("canCancel", true);
        
        Allure.addAttachment("Subscription Dashboard", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should show correct capabilities for paused subscription")
    @Description("Verifies dashboard shows canResume=true for paused subscriptions")
    @Severity(SeverityLevel.NORMAL)
    @Story("Subscription dashboard")
    void shouldShowCorrectCapabilitiesForPausedSubscription() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Pause subscription
        pauseSubscription(tenantId, subscriptionId, customer.get("customerId"));
        
        // Get dashboard
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/customers/" + customerId + "/subscriptions/" + subscriptionId + "/dashboard")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Map<String, Object> dashboard = response.jsonPath().getMap("data");
        assertThat(dashboard)
            .containsEntry("canPause", false)
            .containsEntry("canResume", true)
            .containsEntry("canCancel", true);
        
        Allure.addAttachment("Paused Subscription Dashboard", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should show no capabilities for canceled subscription")
    @Description("Verifies dashboard shows all capabilities as false for canceled subscriptions")
    @Severity(SeverityLevel.NORMAL)
    @Story("Subscription dashboard")
    void shouldShowNoCapabilitiesForCanceledSubscription() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Cancel subscription
        cancelSubscription(tenantId, subscriptionId, customer.get("customerId"));
        
        // Get dashboard
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/customers/" + customerId + "/subscriptions/" + subscriptionId + "/dashboard")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Map<String, Object> dashboard = response.jsonPath().getMap("data");
        assertThat(dashboard)
            .containsEntry("canPause", false)
            .containsEntry("canResume", false)
            .containsEntry("canCancel", false);
        
        Allure.addAttachment("Canceled Subscription Dashboard", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should return 404 for non-existent subscription")
    @Description("Tests error handling when subscription doesn't exist")
    @Severity(SeverityLevel.NORMAL)
    @Story("Error handling")
    void shouldReturn404ForNonExistentSubscription() {
        String tenantId = testTenantId;
        Map<String, String> customer = createCustomer(tenantId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID nonExistentSubId = UUID.randomUUID();
        
        givenAuthenticated(tenantId)
            .when()
            .get("/v1/customers/" + customerId + "/subscriptions/" + nonExistentSubId + "/dashboard")
            .then()
            .statusCode(404);
    }
    
    @Test
    @DisplayName("Should enforce customer authorization")
    @Description("Tests that customers can only access their own subscriptions")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Security")
    void shouldEnforceCustomerAuthorization() {
        String tenantId = testTenantId;
        
        Map<String, String> customer1 = createCustomer(tenantId);
        Map<String, String> customer2 = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID sub1 = createSubscription(tenantId, customer1.get("email"), planId);
        
        // Try to access customer1's subscription as customer2
        givenAuthenticated(tenantId)
            .when()
            .get("/v1/customers/" + customer2.get("customerId") + "/subscriptions/" + sub1 + "/dashboard")
            .then()
            .statusCode(404);
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
    
    @Step("Create plan")
    private UUID createPlan(String tenantId) {
        return createPlan(tenantId, "Test Plan " + UUID.randomUUID().toString().substring(0, 8));
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
        String slug = "test-tenant-dashboard-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Customer Dashboard",
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
