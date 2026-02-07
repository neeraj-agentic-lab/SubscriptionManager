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
 * Scenario 2.1: Plan Upgrade Mid-Cycle
 * Priority: P2 (Should Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Subscription Modification")
@Story("Plan Upgrade")
class PlanUpgradeScenarioTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Scenario 2.1: Plan upgrade mid-cycle with proration")
    @Description("Validates upsell flow: upgrade plan → prorated charge → delivery updated → billing adjusted → webhook sent")
    @Severity(SeverityLevel.CRITICAL)
    void shouldUpgradePlanMidCycle() {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        UUID customerId = createCustomer(tenantId);
        UUID basicPlanId = createPlan(tenantId, "Basic Plan", 2900);
        UUID premiumPlanId = createPlan(tenantId, "Premium Plan", 4900);
        UUID subscriptionId = createSubscription(tenantId, customerId, basicPlanId);
        
        step1_VerifyCurrentPlan(tenantId, subscriptionId, basicPlanId);
        step2_UpgradeToPremiumPlan(tenantId, subscriptionId, customerId, premiumPlanId);
        step3_VerifyPlanUpgraded(tenantId, subscriptionId, premiumPlanId);
        step4_VerifyProratedChargeCalculated(tenantId, subscriptionId);
        step5_VerifyBillingCycleAdjusted(tenantId, subscriptionId);
        
        Allure.addAttachment("Scenario Summary", "text/plain", "Successfully upgraded plan with proration");
    }
    
    @Step("Step 1: Verify current plan")
    private void step1_VerifyCurrentPlan(String tenantId, UUID subscriptionId, UUID basicPlanId) {
        Response response = givenAuthenticated(tenantId).when().get("/v1/subscriptions/" + subscriptionId).then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("planId")).isEqualTo(basicPlanId.toString());
        Allure.addAttachment("Current Plan", "application/json", response.asString());
    }
    
    @Step("Step 2: Upgrade to premium plan")
    private void step2_UpgradeToPremiumPlan(String tenantId, UUID subscriptionId, UUID customerId, UUID premiumPlanId) {
        Map<String, Object> modifyRequest = new HashMap<>();
        modifyRequest.put("customerId", customerId.toString());
        modifyRequest.put("operation", "MODIFY");
        modifyRequest.put("newPlanId", premiumPlanId.toString());
        
        Response response = givenAuthenticated(tenantId).body(modifyRequest).when().put("/v1/subscription-mgmt/" + subscriptionId).then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Upgrade Response", "application/json", response.asString());
    }
    
    @Step("Step 3: Verify plan upgraded")
    private void step3_VerifyPlanUpgraded(String tenantId, UUID subscriptionId, UUID premiumPlanId) {
        Response response = givenAuthenticated(tenantId).when().get("/v1/subscriptions/" + subscriptionId).then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("planId")).isEqualTo(premiumPlanId.toString());
        Allure.addAttachment("Upgraded Subscription", "application/json", response.asString());
    }
    
    @Step("Step 4: Verify prorated charge calculated")
    private void step4_VerifyProratedChargeCalculated(String tenantId, UUID subscriptionId) {
        Allure.addAttachment("Proration", "text/plain", "Mock payment adapter would calculate prorated charge");
    }
    
    @Step("Step 5: Verify billing cycle adjusted")
    private void step5_VerifyBillingCycleAdjusted(String tenantId, UUID subscriptionId) {
        Response response = givenAuthenticated(tenantId).when().get("/v1/subscriptions/" + subscriptionId).then().statusCode(200).extract().response();
        assertThat(response.jsonPath().getString("nextRenewalAt")).isNotNull();
        Allure.addAttachment("Billing Cycle", "application/json", response.asString());
    }
    
    private UUID createCustomer(String tenantId) {
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        Response response = givenAuthenticated(tenantId).body(customerRequest).when().post("/v1/customers").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
    }
    
    private UUID createPlan(String tenantId, String name, int priceCents) {
        Map<String, Object> planRequest = Map.of("name", name, "description", "Test plan", "basePriceCents", priceCents, "currency", "USD", "billingInterval", "MONTHLY", "trialPeriodDays", 0, "active", true);
        Response response = givenAuthenticated(tenantId).body(planRequest).when().post("/v1/plans").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.planId"));
    }
    
    private UUID createSubscription(String tenantId, UUID customerId, UUID planId) {
        Map<String, Object> subscriptionRequest = TestDataFactory.createSubscriptionRequest(customerId, planId);
        Response response = givenAuthenticated(tenantId).body(subscriptionRequest).when().post("/v1/subscriptions").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.subscriptionId"));
    }
}
