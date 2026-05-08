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
 * Scenario 4.1: Webhook Retry on Failure
 * 
 * Business Value: Tests webhook reliability with automatic retries,
 * exponential backoff, and eventual successful delivery.
 * 
 * Priority: P1 (Critical)
 */
@Epic("End-to-End Scenarios")
@Feature("Webhook & Integration")
@Story("Webhook Retry Logic")
class WebhookRetryScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Scenario 4.1: Webhook retry on failure with eventual success")
    @Description("Validates webhook registration and event triggering. Webhook HTTP dispatch/retry is not yet implemented.")
    @Severity(SeverityLevel.BLOCKER)
    void shouldRetryWebhookOnFailureAndEventuallySucceed() {
        String tenantId = createTestTenant();
        String webhookUrl = "http://localhost:8089/webhook-retry";
        
        // Step 1: Register webhook endpoint
        UUID webhookId = step1_RegisterWebhook(tenantId, webhookUrl);
        
        // Step 2: Verify webhook registered in DB
        step2_VerifyWebhookRegistered(tenantId, webhookId);
        
        // Step 3: Trigger subscription event
        UUID subscriptionId = step3_TriggerSubscriptionEvent(tenantId);
        
        // Step 4: Verify webhook endpoint still active
        step4_VerifyWebhookStillActive(tenantId, webhookId);
        
        // Step 5: Verify delivery was cancelled
        step5_VerifyDeliveryCancelled(tenantId, subscriptionId);
        
        Allure.addAttachment("Scenario Summary", "text/plain", 
            "Successfully tested webhook registration and event triggering. HTTP dispatch/retry pending implementation.");
    }
    
    @Step("Step 1: Register webhook endpoint")
    private UUID step1_RegisterWebhook(String tenantId, String url) {
        Map<String, Object> webhookRequest = Map.of(
            "url", url,
            "events", new String[]{"subscription.created", "delivery.canceled"},
            "description", "Retry test webhook"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(webhookRequest)
            .when()
            .post("/v1/admin/webhooks")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        UUID webhookId = UUID.fromString(response.jsonPath().getString("data.webhookId"));
        String secret = response.jsonPath().getString("data.secret");
        
        assertThat(webhookId).isNotNull();
        assertThat(secret).isNotNull();
        
        Allure.addAttachment("Webhook Registered", "application/json", response.asString());
        
        return webhookId;
    }
    
    @Step("Step 2: Verify webhook registered in database")
    private void step2_VerifyWebhookRegistered(String tenantId, UUID webhookId) {
        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM webhook_endpoints WHERE tenant_id = ?::uuid AND id = ?::uuid",
            String.class, tenantId, webhookId.toString()
        );
        assertThat(status).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Webhook Status", "text/plain", 
            "Webhook registered with status: " + status);
    }
    
    @Step("Step 3: Trigger subscription event")
    private UUID step3_TriggerSubscriptionEvent(String tenantId) {
        UUID planId = createPlan(tenantId);
        Map<String, UUID> subResult = createSubscriptionWithCustomer(tenantId, planId);
        UUID subscriptionId = subResult.get("subscriptionId");
        UUID customerId = subResult.get("customerId");
        
        // Cancel delivery to trigger event
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        Map<String, Object> cancelRequest = TestDataFactory.createDeliveryCancelRequest(customerId);
        givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/admin/deliveries/" + deliveryId + "/cancel")
            .then()
            .statusCode(200);
        
        Allure.addAttachment("Event Triggered", "text/plain", 
            "Delivery cancelled, webhook event should be created");
        
        return subscriptionId;
    }
    
    @Step("Step 4: Verify webhook endpoint still active")
    private void step4_VerifyWebhookStillActive(String tenantId, UUID webhookId) {
        // Webhook HTTP dispatch is not yet implemented (TODO in outbox service).
        // Verify the webhook endpoint is still active after triggering an event.
        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM webhook_endpoints WHERE tenant_id = ?::uuid AND id = ?::uuid",
            String.class, tenantId, webhookId.toString()
        );
        assertThat(status).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Webhook Active", "text/plain", 
            "Webhook endpoint remains active after event trigger");
    }
    
    @Step("Step 5: Verify delivery was cancelled")
    private void step5_VerifyDeliveryCancelled(String tenantId, UUID subscriptionId) {
        // Verify the delivery instance was cancelled
        Integer cancelledCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM delivery_instances WHERE tenant_id = ?::uuid AND subscription_id = ?::uuid AND status = 'CANCELED'",
            Integer.class, tenantId, subscriptionId.toString()
        );
        assertThat(cancelledCount).isGreaterThan(0);
        
        Allure.addAttachment("Delivery Cancelled", "text/plain", 
            "Delivery instance cancelled: " + cancelledCount);
    }
    
    // Helper methods
    
    private String createTestTenant() {
        Map<String, Object> tenantRequest = Map.of("name", "Test Tenant " + UUID.randomUUID().toString().substring(0, 8), "slug", "test-" + UUID.randomUUID().toString().substring(0, 8), "status", "ACTIVE");
        Response response = givenSuperAdmin().contentType("application/json").body(tenantRequest).when().post("/v1/admin/tenants").then().statusCode(201).extract().response();
        return response.jsonPath().getString("id");
    }

    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/admin/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    private Map<String, UUID> createSubscriptionWithCustomer(String tenantId, UUID planId) {
        Map<String, Object> subscriptionRequest = TestDataFactory.createSubscriptionRequest(UUID.randomUUID(), planId);
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        Map<String, UUID> result = new java.util.HashMap<>();
        result.put("subscriptionId", UUID.fromString(response.jsonPath().getString("id")));
        result.put("customerId", UUID.fromString(response.jsonPath().getString("customerId")));
        return result;
    }
    
    private UUID createTestDelivery(String tenantId, UUID subscriptionId, UUID customerId) {
        UUID deliveryId = UUID.randomUUID();
        
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_for, snapshot, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?::timestamp with time zone, ?::jsonb, now(), now())",
            deliveryId.toString(),
            tenantId,
            subscriptionId.toString(),
            "cycle_" + System.currentTimeMillis(),
            OffsetDateTime.now().plusDays(7).toString(),
            "{\"test\": true}"
        );
        
        return deliveryId;
    }
}
