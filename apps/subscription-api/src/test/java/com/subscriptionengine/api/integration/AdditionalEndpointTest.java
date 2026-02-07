package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for additional API endpoints not covered in other test classes.
 * Tests: Subscription listing, subscription management details, delivery can-cancel check.
 */
@Epic("Additional API Coverage")
@Feature("Remaining Endpoints")
class AdditionalEndpointTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @DisplayName("Should list all subscriptions with pagination")
    @Description("Tests GET /v1/subscriptions endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Subscription listing")
    void shouldListAllSubscriptions() {
        String tenantId = testTenantId;
        
        // Create multiple subscriptions
        Map<String, String> customer = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        createSubscription(tenantId, customer.get("email"), planId);
        createSubscription(tenantId, customer.get("email"), planId);
        
        Response response = givenAuthenticated(tenantId)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/v1/subscriptions")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getList("content")).isNotEmpty();
        assertThat(response.jsonPath().getInt("totalElements")).isGreaterThanOrEqualTo(2);
        
        Allure.addAttachment("Subscriptions List", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should retrieve subscription management details")
    @Description("Tests GET /v1/subscription-mgmt/{id} endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Subscription management")
    void shouldRetrieveSubscriptionManagementDetails() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        
        Response response = givenAuthenticated(tenantId)
            .queryParam("customerId", customer.get("customerId"))
            .when()
            .get("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        assertThat(response.jsonPath().getString("data.subscriptionId")).isNotNull();
        assertThat(response.jsonPath().getBoolean("data.canPause")).isNotNull();
        
        Allure.addAttachment("Management Details", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should check delivery cancellation eligibility")
    @Description("Tests GET /v1/deliveries/{id}/can-cancel endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Delivery operations")
    void shouldCheckDeliveryCancellationEligibility() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        Response response = givenAuthenticated(tenantId)
            .queryParam("customerId", customerId.toString())
            .when()
            .get("/v1/deliveries/" + deliveryId + "/can-cancel")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        assertThat(response.jsonPath().getBoolean("data.canCancel")).isNotNull();
        assertThat(response.jsonPath().getString("data.status")).isNotNull();
        
        Allure.addAttachment("Can Cancel Check", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should return false for can-cancel on canceled delivery")
    @Description("Tests that canceled deliveries cannot be canceled again")
    @Severity(SeverityLevel.NORMAL)
    @Story("Delivery operations")
    void shouldReturnFalseForCanceledDelivery() {
        String tenantId = testTenantId;
        
        Map<String, String> customer = createCustomer(tenantId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        // Cancel the delivery
        Map<String, Object> cancelRequest = Map.of(
            "customerId", customerId.toString(),
            "reason", "Customer requested delivery cancellation for testing"
        );
        givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/deliveries/" + deliveryId + "/cancel")
            .then()
            .statusCode(200);
        
        // Check can-cancel
        Response response = givenAuthenticated(tenantId)
            .queryParam("customerId", customerId.toString())
            .when()
            .get("/v1/deliveries/" + deliveryId + "/can-cancel")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("data.canCancel")).isFalse();
        assertThat(response.jsonPath().getString("data.status")).isEqualTo("CANCELED");
        
        Allure.addAttachment("Cannot Cancel Again", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should paginate subscription list correctly")
    @Description("Tests pagination parameters for subscription listing")
    @Severity(SeverityLevel.NORMAL)
    @Story("Subscription listing")
    void shouldPaginateSubscriptionList() {
        String tenantId = testTenantId;
        
        // Create multiple subscriptions
        Map<String, String> customer = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        for (int i = 0; i < 5; i++) {
            createSubscription(tenantId, customer.get("email"), planId);
        }
        
        // Get first page
        Response page1 = givenAuthenticated(tenantId)
            .queryParam("page", 0)
            .queryParam("size", 2)
            .when()
            .get("/v1/subscriptions")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(page1.jsonPath().getList("content")).hasSize(2);
        assertThat(page1.jsonPath().getInt("number")).isEqualTo(0);
        
        // Get second page
        Response page2 = givenAuthenticated(tenantId)
            .queryParam("page", 1)
            .queryParam("size", 2)
            .when()
            .get("/v1/subscriptions")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(page2.jsonPath().getInt("number")).isEqualTo(1);
        
        Allure.addAttachment("Page 1", "application/json", page1.asString());
        Allure.addAttachment("Page 2", "application/json", page2.asString());
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
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        
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
        subscriptionRequest.put("startDate", OffsetDateTime.now().toString());
        
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
    
    @Step("Create test delivery")
    private UUID createTestDelivery(String tenantId, UUID subscriptionId, UUID customerId) {
        UUID deliveryId = UUID.randomUUID();
        
        String snapshot = "{\"subscriptionId\":\"" + subscriptionId + "\",\"customerId\":\"" + customerId + "\"}";
        
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_for, snapshot, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?::timestamp with time zone, ?::jsonb, now(), now())",
            deliveryId.toString(),
            tenantId,
            subscriptionId.toString(),
            "cycle_" + System.currentTimeMillis(),
            OffsetDateTime.now().plusDays(7).toString(),
            snapshot
        );
        
        return deliveryId;
    }
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-additional-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Additional Tests",
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
