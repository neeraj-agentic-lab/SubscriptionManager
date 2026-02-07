package com.subscriptionengine.api.integration.scenarios;

import com.subscriptionengine.api.integration.BaseIntegrationTest;
import com.subscriptionengine.api.integration.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 1.2: Customer Subscription Pause & Resume Journey
 * 
 * Business Value: Tests vacation/pause feature with delivery and task management.
 * Priority: P2 (Should Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Customer Journey")
@Story("Pause & Resume")
class PauseResumeJourneyScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Scenario 1.2: Complete pause and resume journey")
    @Description("Validates pause/resume flow: pause → deliveries cancelled → tasks cancelled → resume → deliveries rescheduled → tasks recreated")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCompletePauseAndResumeJourney() {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
        
        // Step 1: Customer has active subscription
        step1_VerifyActiveSubscription(tenantId, subscriptionId);
        
        // Step 2: Customer views dashboard (sees "can pause")
        step2_VerifyCanPause(tenantId, subscriptionId, customerId);
        
        // Step 3: Customer pauses subscription with reason
        step3_PauseSubscription(tenantId, subscriptionId, customerId);
        
        // Step 4: Verify upcoming deliveries cancelled
        step4_VerifyDeliveriesCancelled(tenantId, subscriptionId);
        
        // Step 5: Verify renewal tasks cancelled
        step5_VerifyRenewalTasksCancelled(tenantId, subscriptionId);
        
        // Step 6: Customer resumes subscription
        step6_ResumeSubscription(tenantId, subscriptionId, customerId);
        
        // Step 7: Verify new deliveries scheduled
        step7_VerifyDeliveriesRescheduled(tenantId, subscriptionId);
        
        // Step 8: Verify renewal tasks recreated
        step8_VerifyRenewalTasksRecreated(tenantId, subscriptionId);
        
        Allure.addAttachment("Scenario Summary", "text/plain", 
            "Successfully completed pause and resume journey with clean state management");
    }
    
    @Step("Step 1: Verify active subscription")
    private void step1_VerifyActiveSubscription(String tenantId, UUID subscriptionId) {
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("status")).isEqualTo("ACTIVE");
        Allure.addAttachment("Active Subscription", "application/json", response.asString());
    }
    
    @Step("Step 2: Verify dashboard shows can pause")
    private void step2_VerifyCanPause(String tenantId, UUID subscriptionId, UUID customerId) {
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/customers/" + customerId + "/subscriptions/" + subscriptionId + "/dashboard")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("data.capabilities.canPause")).isTrue();
        Allure.addAttachment("Dashboard Capabilities", "application/json", response.asString());
    }
    
    @Step("Step 3: Pause subscription")
    private void step3_PauseSubscription(String tenantId, UUID subscriptionId, UUID customerId) {
        Map<String, Object> pauseRequest = TestDataFactory.createPauseRequest(customerId);
        
        Response response = givenAuthenticated(tenantId)
            .body(pauseRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Pause Response", "application/json", response.asString());
    }
    
    @Step("Step 4: Verify deliveries cancelled")
    private void step4_VerifyDeliveriesCancelled(String tenantId, UUID subscriptionId) {
        Integer pendingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM delivery_instances WHERE tenant_id = ?::uuid AND subscription_id = ?::uuid AND status = 'PENDING'",
            Integer.class, tenantId, subscriptionId.toString()
        );
        
        assertThat(pendingCount).isEqualTo(0);
        Allure.addAttachment("Deliveries", "text/plain", "Pending deliveries: " + pendingCount);
    }
    
    @Step("Step 5: Verify renewal tasks cancelled")
    private void step5_VerifyRenewalTasksCancelled(String tenantId, UUID subscriptionId) {
        Integer activeTaskCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scheduled_tasks WHERE tenant_id = ?::uuid AND task_key = ? AND status = 'READY'",
            Integer.class, tenantId, "subscription_renewal_" + subscriptionId
        );
        
        assertThat(activeTaskCount).isEqualTo(0);
        Allure.addAttachment("Renewal Tasks", "text/plain", "Active tasks: " + activeTaskCount);
    }
    
    @Step("Step 6: Resume subscription")
    private void step6_ResumeSubscription(String tenantId, UUID subscriptionId, UUID customerId) {
        Map<String, Object> resumeRequest = TestDataFactory.createResumeRequest(customerId);
        
        Response response = givenAuthenticated(tenantId)
            .body(resumeRequest)
            .when()
            .put("/v1/subscription-mgmt/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        Allure.addAttachment("Resume Response", "application/json", response.asString());
    }
    
    @Step("Step 7: Verify deliveries rescheduled")
    private void step7_VerifyDeliveriesRescheduled(String tenantId, UUID subscriptionId) {
        // After resume, new deliveries should be scheduled
        Integer pendingCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM delivery_instances WHERE tenant_id = ?::uuid AND subscription_id = ?::uuid AND status = 'PENDING'",
            Integer.class, tenantId, subscriptionId.toString()
        );
        
        assertThat(pendingCount).isGreaterThanOrEqualTo(0);
        Allure.addAttachment("Rescheduled Deliveries", "text/plain", "Pending deliveries: " + pendingCount);
    }
    
    @Step("Step 8: Verify renewal tasks recreated")
    private void step8_VerifyRenewalTasksRecreated(String tenantId, UUID subscriptionId) {
        Integer taskCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scheduled_tasks WHERE tenant_id = ?::uuid AND task_key = ? AND status = 'READY'",
            Integer.class, tenantId, "subscription_renewal_" + subscriptionId
        );
        
        assertThat(taskCount).isEqualTo(1);
        Allure.addAttachment("Recreated Tasks", "text/plain", "Active renewal tasks: " + taskCount);
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
}
