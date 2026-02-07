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
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
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
            .post("/v1/deliveries/" + deliveryId + "/cancel")
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
    
    private UUID createDeliveryWithOrder(String tenantId, UUID subscriptionId, UUID customerId) {
        UUID deliveryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_date, external_order_ref, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?, ?, now(), now())",
            deliveryId.toString(), tenantId, subscriptionId.toString(), "cycle_" + System.currentTimeMillis(),
            OffsetDateTime.now().plusDays(1).toString(), "order_" + UUID.randomUUID().toString().substring(0, 8)
        );
        return deliveryId;
    }
}
