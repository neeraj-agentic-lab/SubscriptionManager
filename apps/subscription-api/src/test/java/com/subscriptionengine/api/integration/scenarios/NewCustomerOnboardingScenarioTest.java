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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Scenario 1.1: New Customer Onboarding Flow
 * 
 * Business Value: Validates complete customer acquisition from tenant creation
 * through subscription activation and delivery scheduling.
 * 
 * Priority: P1 (Critical)
 */
@Epic("End-to-End Scenarios")
@Feature("Customer Journey")
@Story("New Customer Onboarding")
class NewCustomerOnboardingScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Scenario 1.1: Complete new customer onboarding flow")
    @Description("Validates end-to-end customer acquisition: tenant → plan → customer → subscription → delivery → webhook → renewal task")
    @Severity(SeverityLevel.BLOCKER)
    void shouldCompleteNewCustomerOnboardingFlow() {
        
        // Step 1: Create new tenant (company signs up)
        UUID tenantId = step1_CreateTenant();
        
        // Step 2: Create subscription plan (monthly coffee delivery)
        UUID planId = step2_CreatePlan(tenantId.toString());
        
        // Step 3: Customer registers with email
        String customerEmail = step3_CreateCustomer(tenantId.toString());
        
        // Step 4: Customer creates subscription with payment method
        UUID subscriptionId = step4_CreateSubscription(tenantId.toString(), customerEmail, planId);
        
        // Step 5: Verify subscription is ACTIVE
        step5_VerifySubscriptionActive(tenantId.toString(), subscriptionId);
        
        // Step 6: Verify first delivery is scheduled
        step6_VerifyDeliveryScheduled(tenantId.toString(), subscriptionId);
        
        // Step 7: Verify webhook event sent (subscription.created)
        step7_VerifyWebhookEventCreated(tenantId.toString());
        
        // Step 8: Verify scheduled renewal task created
        step8_VerifyRenewalTaskScheduled(tenantId.toString(), subscriptionId);
        
        Allure.addAttachment("Scenario Summary", "text/plain", 
            "Successfully onboarded customer with subscription, delivery, and renewal task");
    }
    
    @Step("Step 1: Create new tenant (company signs up)")
    private UUID step1_CreateTenant() {
        String tenantName = "Onboarding Test Company " + UUID.randomUUID().toString().substring(0, 8);
        String tenantSlug = "onboarding-test-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", tenantName,
            "slug", tenantSlug,
            "status", "ACTIVE"
        );
        
        UUID tempTenantId = UUID.randomUUID();
        
        Response response = givenAuthenticated(tempTenantId.toString())
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID tenantId = UUID.fromString(response.jsonPath().getString("id"));
        
        assertThat(tenantId).isNotNull();
        assertThat(response.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Tenant Created", "application/json", response.asString());
        
        return tenantId;
    }
    
    @Step("Step 2: Create subscription plan (monthly coffee delivery)")
    private UUID step2_CreatePlan(String tenantId) {
        Map<String, Object> planRequest = Map.of(
            "name", "Monthly Coffee Delivery",
            "description", "Premium coffee beans delivered monthly",
            "basePriceCents", 2999,
            "currency", "USD",
            "billingInterval", "MONTHLY",
            "trialPeriodDays", 0,
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID planId = UUID.fromString(response.jsonPath().getString("id"));
        
        assertThat(planId).isNotNull();
        assertThat(response.jsonPath().getString("name")).isEqualTo("Monthly Coffee Delivery");
        assertThat(response.jsonPath().getInt("basePriceCents")).isEqualTo(2999);
        
        Allure.addAttachment("Plan Created", "application/json", response.asString());
        
        return planId;
    }
    
    @Step("Step 3: Customer registers with email")
    private String step3_CreateCustomer(String tenantId) {
        String email = "newcustomer-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        
        Map<String, Object> customerRequest = Map.of(
            "email", email,
            "firstName", "John",
            "lastName", "Doe",
            "externalCustomerId", "cust_onboarding_" + UUID.randomUUID().toString().substring(0, 8)
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String customerEmail = response.jsonPath().getString("data.email");
        
        assertThat(customerEmail).isNotNull();
        assertThat(customerEmail).isEqualTo(email);
        
        Allure.addAttachment("Customer Registered", "application/json", response.asString());
        
        return customerEmail;
    }
    
    @Step("Step 4: Customer creates subscription with payment method")
    private UUID step4_CreateSubscription(String tenantId, String customerEmail, UUID planId) {
        Map<String, Object> subscriptionRequest = Map.of(
            "planId", planId.toString(),
            "customerEmail", customerEmail,
            "startDate", java.time.OffsetDateTime.now().toString()
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID subscriptionId = UUID.fromString(response.jsonPath().getString("id"));
        
        assertThat(subscriptionId).isNotNull();
        
        Allure.addAttachment("Subscription Created", "application/json", response.asString());
        
        return subscriptionId;
    }
    
    @Step("Step 5: Verify subscription is ACTIVE")
    private void step5_VerifySubscriptionActive(String tenantId, UUID subscriptionId) {
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String status = response.jsonPath().getString("status");
        
        assertThat(status).isEqualTo("ACTIVE");
        assertThat(response.jsonPath().getString("id")).isEqualTo(subscriptionId.toString());
        
        Allure.addAttachment("Subscription Status Verified", "application/json", response.asString());
    }
    
    @Step("Step 6: Verify first delivery is scheduled")
    private void step6_VerifyDeliveryScheduled(String tenantId, UUID subscriptionId) {
        // Query database for delivery instances
        Integer deliveryCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM delivery_instances WHERE tenant_id = ?::uuid AND subscription_id = ?::uuid AND status = 'PENDING'",
            Integer.class,
            tenantId,
            subscriptionId.toString()
        );
        
        assertThat(deliveryCount).isGreaterThan(0);
        
        Allure.addAttachment("Delivery Verification", "text/plain", 
            "Found " + deliveryCount + " pending delivery(ies) for subscription");
    }
    
    @Step("Step 7: Verify webhook event sent (subscription.created)")
    private void step7_VerifyWebhookEventCreated(String tenantId) {
        // Note: This verifies outbox event creation
        // Actual webhook delivery depends on registered endpoints
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE tenant_id = ?::uuid AND event_type = 'subscription.created' AND published = false",
                Integer.class,
                tenantId
            );
            
            assertThat(eventCount).isGreaterThan(0);
        });
        
        Allure.addAttachment("Webhook Event Verification", "text/plain", 
            "Outbox event created for subscription.created");
    }
    
    @Step("Step 8: Verify scheduled renewal task created")
    private void step8_VerifyRenewalTaskScheduled(String tenantId, UUID subscriptionId) {
        // Query for scheduled renewal task
        Integer taskCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM scheduled_tasks WHERE tenant_id = ?::uuid AND task_key = ? AND status = 'READY'",
            Integer.class,
            tenantId,
            "subscription_renewal_" + subscriptionId
        );
        
        assertThat(taskCount).isEqualTo(1);
        
        Allure.addAttachment("Renewal Task Verification", "text/plain", 
            "Scheduled renewal task created for subscription");
    }
}
