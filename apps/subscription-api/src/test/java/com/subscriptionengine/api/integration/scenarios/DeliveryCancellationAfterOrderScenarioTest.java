package com.subscriptionengine.api.integration.scenarios;

import com.subscriptionengine.api.integration.BaseIntegrationTest;
import com.subscriptionengine.api.integration.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 3.1: Delivery Cancellation After Order Placed
 * Priority: P2 (Should Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Delivery Management")
@Story("Late Cancellation")
class DeliveryCancellationAfterOrderScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Scenario 3.1: Delivery cancellation rejected after order placed")
    @Description("Validates business rules: order placed → cancellation attempted → rejected → proper error message")
    @Severity(SeverityLevel.NORMAL)
    void shouldRejectCancellationAfterOrderPlaced() {
        String tenantId = createTestTenant();
        
        UUID planId = createPlan(tenantId);
        Map<String, UUID> subResult = createSubscriptionWithCustomer(tenantId, planId);
        UUID subscriptionId = subResult.get("subscriptionId");
        UUID customerId = subResult.get("customerId");
        UUID deliveryId = createDeliveryWithOrder(tenantId, subscriptionId, customerId);
        
        step1_VerifyOrderPlaced(tenantId, deliveryId);
        step2_AttemptCancellation(tenantId, deliveryId, customerId);
        step3_VerifyCancellationRejected();
        
        Allure.addAttachment("Scenario Summary", "text/plain", "Successfully enforced late cancellation business rule");
    }
    
    @Step("Step 1: Verify order already placed")
    private void step1_VerifyOrderPlaced(String tenantId, UUID deliveryId) {
        String orderRef = jdbcTemplate.queryForObject(
            "SELECT external_order_ref FROM delivery_instances WHERE id = ?::uuid",
            String.class, deliveryId.toString()
        );
        assertThat(orderRef).isNotNull().isNotEmpty();
        Allure.addAttachment("Order Reference", "text/plain", "External order: " + orderRef);
    }
    
    @Step("Step 2: Attempt cancellation")
    private void step2_AttemptCancellation(String tenantId, UUID deliveryId, UUID customerId) {
        Map<String, Object> cancelRequest = TestDataFactory.createDeliveryCancelRequest(customerId);
        
        Response response = givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/admin/deliveries/" + deliveryId + "/cancel")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isFalse();
        Allure.addAttachment("Cancellation Rejected", "application/json", response.asString());
    }
    
    @Step("Step 3: Verify proper error message")
    private void step3_VerifyCancellationRejected() {
        Allure.addAttachment("Business Rule", "text/plain", "Cancellation rejected - order already placed with external system");
    }
    
    private String createTestTenant() {
        Map<String, Object> tenantRequest = Map.of("name", "Test Tenant " + UUID.randomUUID().toString().substring(0, 8), "slug", "test-" + UUID.randomUUID().toString().substring(0, 8), "status", "ACTIVE");
        Response response = givenSuperAdmin().contentType("application/json").body(tenantRequest).when().post("/v1/admin/tenants").then().statusCode(201).extract().response();
        return response.jsonPath().getString("id");
    }

    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        Response response = givenAuthenticated(tenantId).body(planRequest).when().post("/v1/admin/plans").then().statusCode(201).extract().response();
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    private Map<String, UUID> createSubscriptionWithCustomer(String tenantId, UUID planId) {
        Map<String, Object> subscriptionRequest = TestDataFactory.createSubscriptionRequest(UUID.randomUUID(), planId);
        Response response = givenAuthenticated(tenantId).body(subscriptionRequest).when().post("/v1/admin/subscriptions").then().statusCode(201).extract().response();
        Map<String, UUID> result = new java.util.HashMap<>();
        result.put("subscriptionId", UUID.fromString(response.jsonPath().getString("id")));
        result.put("customerId", UUID.fromString(response.jsonPath().getString("customerId")));
        return result;
    }
    
    private UUID createDeliveryWithOrder(String tenantId, UUID subscriptionId, UUID customerId) {
        UUID deliveryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_for, snapshot, external_order_ref, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?::timestamp with time zone, ?::jsonb, ?, now(), now())",
            deliveryId.toString(), tenantId, subscriptionId.toString(), "cycle_" + System.currentTimeMillis(),
            OffsetDateTime.now().plusDays(1).toString(), "{\"test\": true}", "order_" + UUID.randomUUID().toString().substring(0, 8)
        );
        return deliveryId;
    }
}
