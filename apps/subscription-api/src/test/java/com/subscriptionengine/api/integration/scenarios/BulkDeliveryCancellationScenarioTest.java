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
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Scenario 3.2: Bulk Delivery Cancellation on Subscription Cancel
 * Priority: P2 (Should Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Delivery Management")
@Story("Bulk Cancellation")
class BulkDeliveryCancellationScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Scenario 3.2: Bulk delivery cancellation on subscription cancel")
    @Description("Validates cascade operations: cancel subscription → all deliveries cancelled → webhook events sent")
    @Severity(SeverityLevel.NORMAL)
    void shouldCancelAllDeliveriesOnSubscriptionCancel() {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
        
        // Create 5 upcoming deliveries
        for (int i = 0; i < 5; i++) {
            createDelivery(tenantId, subscriptionId, i + 1);
        }
        
        step1_VerifyMultipleDeliveries(tenantId, subscriptionId);
        step2_CancelSubscription(tenantId, subscriptionId, customerId);
        step3_VerifyAllDeliveriesCancelled(tenantId, subscriptionId);
        step4_VerifyWebhookEvents(tenantId);
        
        Allure.addAttachment("Scenario Summary", "text/plain", "Successfully cancelled all deliveries in cascade");
    }
    
    @Step("Step 1: Verify 5 upcoming deliveries")
    private void step1_VerifyMultipleDeliveries(String tenantId, UUID subscriptionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM delivery_instances WHERE tenant_id = ?::uuid AND subscription_id = ?::uuid AND status = 'PENDING'",
            Integer.class, tenantId, subscriptionId.toString()
        );
        assertThat(count).isEqualTo(5);
        Allure.addAttachment("Deliveries", "text/plain", "Pending deliveries: " + count);
    }
    
    @Step("Step 2: Cancel subscription")
    private void step2_CancelSubscription(String tenantId, UUID subscriptionId, UUID customerId) {
        Map<String, Object> cancelRequest = TestDataFactory.createCancelRequest(customerId, true);
        Response response = givenAuthenticated(tenantId).body(cancelRequest).when().put("/v1/subscription-mgmt/" + subscriptionId).then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Cancellation Response", "application/json", response.asString());
    }
    
    @Step("Step 3: Verify all 5 deliveries cancelled")
    private void step3_VerifyAllDeliveriesCancelled(String tenantId, UUID subscriptionId) {
        Integer cancelledCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM delivery_instances WHERE tenant_id = ?::uuid AND subscription_id = ?::uuid AND status = 'CANCELED'",
            Integer.class, tenantId, subscriptionId.toString()
        );
        assertThat(cancelledCount).isEqualTo(5);
        
        Integer pendingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM delivery_instances WHERE tenant_id = ?::uuid AND subscription_id = ?::uuid AND status = 'PENDING'",
            Integer.class, tenantId, subscriptionId.toString()
        );
        assertThat(pendingCount).isEqualTo(0);
        
        Allure.addAttachment("Bulk Cancellation", "text/plain", "Cancelled: " + cancelledCount + ", Pending: " + pendingCount);
    }
    
    @Step("Step 4: Verify webhook events sent")
    private void step4_VerifyWebhookEvents(String tenantId) {
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE tenant_id = ?::uuid AND event_type IN ('delivery.canceled', 'subscription.canceled')",
                Integer.class, tenantId
            );
            assertThat(eventCount).isGreaterThan(0);
        });
        Allure.addAttachment("Webhook Events", "text/plain", "Outbox events created for cancellations");
    }
    
    private UUID createCustomer(String tenantId) {
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        Response response = givenAuthenticated(tenantId).body(customerRequest).when().post("/v1/customers").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
    }
    
    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        Response response = givenAuthenticated(tenantId).body(planRequest).when().post("/v1/plans").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.planId"));
    }
    
    private UUID createSubscription(String tenantId, UUID customerId, UUID planId) {
        Map<String, Object> subscriptionRequest = TestDataFactory.createSubscriptionRequest(customerId, planId);
        Response response = givenAuthenticated(tenantId).body(subscriptionRequest).when().post("/v1/subscriptions").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.subscriptionId"));
    }
    
    private void createDelivery(String tenantId, UUID subscriptionId, int daysAhead) {
        UUID deliveryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_date, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?, now(), now())",
            deliveryId.toString(), tenantId, subscriptionId.toString(), "cycle_" + daysAhead,
            OffsetDateTime.now().plusDays(daysAhead * 7).toString()
        );
    }
}
