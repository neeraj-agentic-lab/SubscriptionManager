package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Integration tests for complete subscription lifecycle.
 * Tests: Create → Pause → Resume → Cancel flow.
 */
@Epic("Subscription Management")
@Feature("Subscription Lifecycle")
class SubscriptionLifecycleTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @DisplayName("Should complete full subscription lifecycle: create → pause → resume → cancel")
    @Description("Tests the complete subscription lifecycle from creation through cancellation")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Complete subscription flow")
    void shouldCompleteFullSubscriptionLifecycle() {
        String tenantId = testTenantId;
        
        // Step 1: Create a plan
        UUID planId = createPlan(tenantId);
        
        // Step 2: Create a customer
        Map<String, String> customer = createCustomer(tenantId);
        String customerEmail = customer.get("email");
        String customerId = customer.get("customerId");
        
        // Step 3: Create subscription
        UUID subscriptionId = createSubscription(tenantId, customerEmail, planId);
        
        // Step 4: Verify subscription is ACTIVE
        verifySubscriptionStatus(tenantId, subscriptionId, "ACTIVE");
        
        // Step 5: Pause subscription
        pauseSubscription(tenantId, subscriptionId, customerId);
        verifySubscriptionStatus(tenantId, subscriptionId, "PAUSED");
        
        // Step 6: Resume subscription
        resumeSubscription(tenantId, subscriptionId, customerId);
        verifySubscriptionStatus(tenantId, subscriptionId, "ACTIVE");
        
        // Step 7: Cancel subscription
        cancelSubscription(tenantId, subscriptionId, customerId);
        verifySubscriptionStatus(tenantId, subscriptionId, "CANCELED");
    }
    
    @Test
    @DisplayName("Should handle subscription pause with scheduled task cancellation")
    @Description("Verifies that pausing a subscription cancels pending renewal tasks")
    @Severity(SeverityLevel.NORMAL)
    @Story("Subscription pause")
    void shouldCancelScheduledTasksOnPause() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Pause subscription
        pauseSubscription(tenantId, subscriptionId, customer.get("customerId"));
        
        // Verify subscription is paused
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo("PAUSED");
        assertThat(response.jsonPath().getString("nextRenewalAt")).isNull();
    }
    
    @Test
    @DisplayName("Should handle subscription resume with task rescheduling")
    @Description("Verifies that resuming a subscription reschedules renewal tasks")
    @Severity(SeverityLevel.NORMAL)
    @Story("Subscription resume")
    void shouldRescheduleTasksOnResume() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Pause then resume
        pauseSubscription(tenantId, subscriptionId, customer.get("customerId"));
        resumeSubscription(tenantId, subscriptionId, customer.get("customerId"));
        
        // Verify subscription has next renewal scheduled
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo("ACTIVE");
        assertThat(response.jsonPath().getString("nextRenewalAt")).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle immediate cancellation")
    @Description("Verifies immediate subscription cancellation sets status to CANCELED")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Subscription cancellation")
    void shouldHandleImmediateCancellation() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Cancel immediately
        String customerId = customer.get("customerId");
        Map<String, Object> cancelRequest = Map.of(
            "operation", "CANCEL",
            "customerId", customerId.toString(),
            "cancellationType", "immediate"
        );
        
        givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // Verify canceled
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo("CANCELED");
        assertThat(response.jsonPath().getString("canceledAt")).isNotNull();
    }
    
    @Test
    @DisplayName("Should handle end-of-period cancellation")
    @Description("Verifies end-of-period cancellation sets cancel_at_period_end flag")
    @Severity(SeverityLevel.NORMAL)
    @Story("Subscription cancellation")
    void shouldHandleEndOfPeriodCancellation() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        // Cancel at end of period
        String customerId = customer.get("customerId");
        Map<String, Object> cancelRequest = Map.of(
            "operation", "CANCEL",
            "customerId", customerId.toString(),
            "cancellationType", "end_of_period"
        );
        
        givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // Verify still active but flagged for cancellation
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String status = response.jsonPath().getString("status");
        assertThat(status).isEqualTo("ACTIVE");
        assertThat(response.jsonPath().getBoolean("cancelAtPeriodEnd")).isTrue();
    }
    
    // Helper methods
    
    @Step("Create plan")
    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        String planId = response.jsonPath().getString("id");
        Allure.addAttachment("Plan Created", "application/json", response.asString());
        
        return UUID.fromString(planId);
    }
    
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
        Allure.addAttachment("Customer Created", "application/json", response.asString());
        
        return Map.of("email", email, "customerId", customerId);
    }
    
    @Step("Create subscription")
    private UUID createSubscription(String tenantId, String customerEmail, UUID planId) {
        Map<String, Object> subscriptionRequest = new java.util.HashMap<>();
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
        
        String subscriptionId = response.jsonPath().getString("id");
        Allure.addAttachment("Subscription Created", "application/json", response.asString());
        
        return UUID.fromString(subscriptionId);
    }
    
    @Step("Pause subscription")
    private void pauseSubscription(String tenantId, UUID subscriptionId, String customerId) {
        Map<String, Object> pauseRequest = Map.of(
            "operation", "PAUSE",
            "customerId", customerId
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(pauseRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Allure.addAttachment("Subscription Paused", "application/json", response.asString());
    }
    
    @Step("Resume subscription")
    private void resumeSubscription(String tenantId, UUID subscriptionId, String customerId) {
        Map<String, Object> resumeRequest = Map.of(
            "operation", "RESUME",
            "customerId", customerId
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(resumeRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Allure.addAttachment("Subscription Resumed", "application/json", response.asString());
    }
    
    @Step("Cancel subscription")
    private void cancelSubscription(String tenantId, UUID subscriptionId, String customerId) {
        Map<String, Object> cancelRequest = Map.of(
            "operation", "CANCEL",
            "customerId", customerId,
            "cancellationType", "immediate"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Allure.addAttachment("Subscription Canceled", "application/json", response.asString());
    }
    
    @Step("Verify subscription status is {expectedStatus}")
    private void verifySubscriptionStatus(String tenantId, UUID subscriptionId, String expectedStatus) {
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String actualStatus = response.jsonPath().getString("status");
        assertThat(actualStatus).isEqualTo(expectedStatus);
        
        Allure.addAttachment("Subscription Status Verified", "application/json", response.asString());
    }
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-lifecycle-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Subscription Lifecycle",
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
