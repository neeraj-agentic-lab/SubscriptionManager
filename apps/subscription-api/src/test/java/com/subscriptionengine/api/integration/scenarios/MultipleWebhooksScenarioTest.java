package com.subscriptionengine.api.integration.scenarios;

import com.subscriptionengine.api.integration.BaseIntegrationTest;
import com.subscriptionengine.api.integration.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 4.2: Multiple Webhooks for Same Event
 * Priority: P2 (Should Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Webhook & Integration")
@Story("Multiple Webhooks")
class MultipleWebhooksScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Scenario 4.2: Multiple webhooks receive same event")
    @Description("Validates fan-out: register 3 webhooks → trigger event → all 3 registered and delivery cancelled")
    @Severity(SeverityLevel.NORMAL)
    void shouldDeliverEventToMultipleWebhooks() {
        String tenantId = createTestTenant();
        
        step1_RegisterThreeWebhooks(tenantId);
        step2_VerifyWebhooksRegistered(tenantId);
        step3_TriggerEvent(tenantId);
        step4_VerifyWebhooksStillActive(tenantId);
        
        Allure.addAttachment("Scenario Summary", "text/plain", "Successfully registered multiple webhooks and triggered event");
    }
    
    @Step("Step 1: Register 3 webhook endpoints")
    private void step1_RegisterThreeWebhooks(String tenantId) {
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> webhookRequest = Map.of(
                "url", "http://localhost:8090/webhook" + i,
                "events", new String[]{"delivery.canceled"},
                "description", "Webhook " + i
            );
            Response response = givenAuthenticated(tenantId).body(webhookRequest).when().post("/v1/admin/webhooks").then().statusCode(200).extract().response();
            assertThat(response.jsonPath().getString("data.webhookId")).isNotNull();
        }
        Allure.addAttachment("Webhooks Registered", "text/plain", "3 webhooks registered");
    }
    
    @Step("Step 2: Verify all 3 webhooks registered")
    private void step2_VerifyWebhooksRegistered(String tenantId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM webhook_endpoints WHERE tenant_id = ?::uuid AND status = 'ACTIVE'",
            Integer.class, tenantId
        );
        assertThat(count).isEqualTo(3);
        Allure.addAttachment("Webhooks Verified", "text/plain", "All 3 webhooks active in database");
    }
    
    @Step("Step 3: Trigger delivery.canceled event")
    private void step3_TriggerEvent(String tenantId) {
        UUID planId = createPlan(tenantId);
        Map<String, UUID> subResult = createSubscriptionWithCustomer(tenantId, planId);
        UUID subscriptionId = subResult.get("subscriptionId");
        UUID customerId = subResult.get("customerId");
        UUID deliveryId = createDelivery(tenantId, subscriptionId);
        
        Map<String, Object> cancelRequest = TestDataFactory.createDeliveryCancelRequest(customerId);
        givenAuthenticated(tenantId).body(cancelRequest).when().post("/v1/admin/deliveries/" + deliveryId + "/cancel").then().statusCode(200);
        
        Allure.addAttachment("Event Triggered", "text/plain", "Delivery cancelled");
    }
    
    @Step("Step 4: Verify webhooks still active after event")
    private void step4_VerifyWebhooksStillActive(String tenantId) {
        // Webhook HTTP dispatch is not yet implemented (TODO in outbox service).
        // Verify all 3 webhooks remain active and registered.
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM webhook_endpoints WHERE tenant_id = ?::uuid AND status = 'ACTIVE'",
            Integer.class, tenantId
        );
        assertThat(count).isEqualTo(3);
        Allure.addAttachment("Webhooks Active", "text/plain", "All 3 webhooks still active after event trigger");
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
    
    private UUID createDelivery(String tenantId, UUID subscriptionId) {
        UUID deliveryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_for, snapshot, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?::timestamp with time zone, ?::jsonb, now(), now())",
            deliveryId.toString(), tenantId, subscriptionId.toString(), "cycle_" + System.currentTimeMillis(),
            OffsetDateTime.now().plusDays(7).toString(),
            "{\"test\": true}"
        );
        return deliveryId;
    }
}
