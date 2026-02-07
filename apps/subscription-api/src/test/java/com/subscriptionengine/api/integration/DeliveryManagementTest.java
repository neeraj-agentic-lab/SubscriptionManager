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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Integration tests for delivery management.
 * Tests: Delivery creation, cancellation, and outbox event emission.
 */
@Epic("Delivery Management")
@Feature("Delivery Operations")
class DeliveryManagementTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @DisplayName("Should cancel delivery and emit outbox event")
    @Description("Tests delivery cancellation with reason and verifies outbox event emission")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Delivery cancellation")
    void shouldCancelDeliveryAndEmitEvent() {
        String tenantId = testTenantId;
        
        // Setup: Create subscription with delivery
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        // Cancel delivery
        Map<String, Object> cancelRequest = Map.of(
            "customerId", customerId.toString(),
            "reason", "Customer requested delivery cancellation for testing"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/deliveries/" + deliveryId + "/cancel")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Allure.addAttachment("Delivery Cancellation Response", "application/json", response.asString());
        
        // Verify delivery is canceled
        Response deliveryResponse = givenAuthenticated(tenantId)
            .queryParam("customerId", customerId.toString())
            .when()
            .get("/v1/deliveries/" + deliveryId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Map<String, Object> delivery = deliveryResponse.jsonPath().getMap("data");
        assertThat(delivery.get("status")).isEqualTo("CANCELED");
        assertThat(delivery.get("cancelledAt")).isNotNull();
        assertThat(delivery.get("cancellationReason")).isEqualTo("Customer requested delivery cancellation for testing");
        
        Allure.addAttachment("Canceled Delivery Details", "application/json", deliveryResponse.asString());
        
        // Verify outbox event was created
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE tenant_id = ?::uuid AND event_type = 'delivery.canceled' AND event_key = ?",
                Integer.class,
                tenantId,
                "delivery_" + deliveryId
            );
            assertThat(eventCount).isGreaterThan(0);
        });
    }
    
    @Test
    @DisplayName("Should retrieve upcoming deliveries for customer")
    @Description("Tests fetching upcoming deliveries with proper filtering")
    @Severity(SeverityLevel.NORMAL)
    @Story("Delivery retrieval")
    void shouldRetrieveUpcomingDeliveries() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        
        // Create test deliveries
        UUID delivery1 = createTestDelivery(tenantId, subscriptionId, customerId);
        UUID delivery2 = createTestDelivery(tenantId, subscriptionId, customerId);
        
        // Get upcoming deliveries
        Response response = givenAuthenticated(tenantId)
            .queryParam("customerId", customerId.toString())
            .queryParam("limit", 10)
            .when()
            .get("/v1/deliveries/upcoming")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> deliveries = response.jsonPath().getList("data.deliveries");
        assertThat(deliveries).hasSizeGreaterThanOrEqualTo(2);
        
        Allure.addAttachment("Upcoming Deliveries", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should not allow canceling already canceled delivery")
    @Description("Verifies that attempting to cancel an already canceled delivery fails")
    @Severity(SeverityLevel.NORMAL)
    @Story("Delivery cancellation")
    void shouldNotAllowCancelingCanceledDelivery() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        // Cancel delivery first time
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
        
        // Try to cancel again - should fail
        Response response = givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/deliveries/" + deliveryId + "/cancel")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isFalse();
        Allure.addAttachment("Second Cancellation Attempt", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should retrieve delivery details with cancellation info")
    @Description("Tests that delivery details include cancellation timestamp and reason")
    @Severity(SeverityLevel.NORMAL)
    @Story("Delivery retrieval")
    void shouldRetrieveDeliveryDetailsWithCancellationInfo() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        // Cancel delivery
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
        
        // Get delivery details
        Response response = givenAuthenticated(tenantId)
            .queryParam("customerId", customerId.toString())
            .when()
            .get("/v1/deliveries/" + deliveryId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Map<String, Object> delivery = response.jsonPath().getMap("data");
        assertThat(delivery)
            .containsEntry("status", "CANCELED")
            .containsKey("cancelledAt")
            .containsKey("cancellationReason")
            .containsEntry("canCancel", false);
        
        Allure.addAttachment("Delivery Details", "application/json", response.asString());
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
        
        return UUID.fromString(response.jsonPath().getString("id"));
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
        return Map.of("email", email, "customerId", customerId);
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
    
    @Step("Create test delivery in database")
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
        String slug = "test-tenant-delivery-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Delivery Management",
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
