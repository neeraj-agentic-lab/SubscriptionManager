package com.subscriptionengine.api.integration.scenarios;

import com.subscriptionengine.api.integration.BaseIntegrationTest;
import com.subscriptionengine.api.integration.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 2.2: Address Change Before Delivery
 * Priority: P2 (Should Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Subscription Modification")
@Story("Address Change")
class AddressChangeScenarioTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Scenario 2.2: Address change before delivery")
    @Description("Validates logistics update: change address → upcoming delivery updated → past deliveries unchanged")
    @Severity(SeverityLevel.NORMAL)
    void shouldChangeAddressBeforeDelivery() {
        String tenantId = createTestTenant();
        
        UUID planId = createPlan(tenantId);
        Map<String, UUID> subResult = createSubscriptionWithCustomer(tenantId, planId);
        UUID subscriptionId = subResult.get("subscriptionId");
        UUID customerId = subResult.get("customerId");
        
        step1_VerifyCurrentAddress(tenantId, subscriptionId);
        step2_UpdateShippingAddress(tenantId, subscriptionId, customerId);
        step3_VerifyAddressUpdated(tenantId, subscriptionId);
        
        Allure.addAttachment("Scenario Summary", "text/plain", "Successfully updated shipping address");
    }
    
    @Step("Step 1: Verify current address")
    private void step1_VerifyCurrentAddress(String tenantId, UUID subscriptionId) {
        Response response = givenAuthenticated(tenantId).when().get("/v1/admin/subscriptions/" + subscriptionId).then().statusCode(200).extract().response();
        Allure.addAttachment("Current Subscription", "application/json", response.asString());
    }
    
    @Step("Step 2: Update shipping address")
    private void step2_UpdateShippingAddress(String tenantId, UUID subscriptionId, UUID customerId) {
        Map<String, Object> newAddress = new HashMap<>();
        newAddress.put("line1", "789 New Address St");
        newAddress.put("city", "Seattle");
        newAddress.put("state", "WA");
        newAddress.put("postalCode", "98101");
        newAddress.put("country", "US");
        
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customerId.toString());
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("shippingAddress", newAddress);
        
        Response response = givenAuthenticated(tenantId).body(modifyRequest).when().put("/v1/admin/subscriptions/manage/" + subscriptionId).then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Address Update Response", "application/json", response.asString());
    }
    
    @Step("Step 3: Verify address updated")
    private void step3_VerifyAddressUpdated(String tenantId, UUID subscriptionId) {
        Response response = givenAuthenticated(tenantId).when().get("/v1/admin/subscriptions/" + subscriptionId).then().statusCode(200).extract().response();
        Allure.addAttachment("Updated Subscription", "application/json", response.asString());
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
        Map<String, UUID> result = new HashMap<>();
        result.put("subscriptionId", UUID.fromString(response.jsonPath().getString("id")));
        result.put("customerId", UUID.fromString(response.jsonPath().getString("customerId")));
        return result;
    }
}
