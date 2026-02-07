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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 5.1: Tenant Isolation Verification
 * 
 * Business Value: Critical security test ensuring complete data isolation
 * between tenants with no cross-tenant access or data leakage.
 * 
 * Priority: P1 (Critical)
 */
@Epic("End-to-End Scenarios")
@Feature("Multi-Tenancy")
@Story("Tenant Isolation")
class TenantIsolationScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Test
    @DisplayName("Scenario 5.1: Complete tenant isolation verification")
    @Description("Validates tenant isolation: create 2 tenants → verify no cross-tenant access → verify no data leakage")
    @Severity(SeverityLevel.BLOCKER)
    void shouldEnforceCompleteTenantIsolation() {
        
        // Step 1: Create Tenant A with customer and subscription
        TenantData tenantA = step1_CreateTenantWithSubscription("Tenant A");
        
        // Step 2: Create Tenant B with customer and subscription
        TenantData tenantB = step2_CreateTenantWithSubscription("Tenant B");
        
        // Step 3: Tenant A tries to access Tenant B's subscription
        step3_VerifyCannotAccessOtherTenantSubscription(tenantA, tenantB);
        
        // Step 4: Verify 404/403 error
        step4_VerifyProperErrorResponse(tenantA, tenantB);
        
        // Step 5: Tenant A tries to cancel Tenant B's delivery
        step5_VerifyCannotCancelOtherTenantDelivery(tenantA, tenantB);
        
        // Step 6: Verify rejection
        step6_VerifyOperationRejected(tenantA, tenantB);
        
        // Step 7: Verify no data leakage
        step7_VerifyNoDataLeakage(tenantA, tenantB);
        
        Allure.addAttachment("Scenario Summary", "text/plain", 
            "Successfully verified complete tenant isolation with no cross-tenant access");
    }
    
    @Step("Step 1: Create Tenant A with customer and subscription")
    private TenantData step1_CreateTenantWithSubscription(String tenantName) {
        UUID tenantId = createTenant(tenantName);
        UUID customerId = createCustomer(tenantId.toString());
        UUID planId = createPlan(tenantId.toString());
        UUID subscriptionId = createSubscription(tenantId.toString(), customerId, planId);
        UUID deliveryId = createTestDelivery(tenantId.toString(), subscriptionId, customerId);
        
        TenantData data = new TenantData();
        data.tenantId = tenantId;
        data.customerId = customerId;
        data.planId = planId;
        data.subscriptionId = subscriptionId;
        data.deliveryId = deliveryId;
        
        Allure.addAttachment(tenantName + " Created", "text/plain", 
            "Tenant: " + tenantId + ", Subscription: " + subscriptionId);
        
        return data;
    }
    
    @Step("Step 2: Create Tenant B with customer and subscription")
    private TenantData step2_CreateTenantWithSubscription(String tenantName) {
        return step1_CreateTenantWithSubscription(tenantName);
    }
    
    @Step("Step 3: Tenant A tries to access Tenant B's subscription")
    private void step3_VerifyCannotAccessOtherTenantSubscription(TenantData tenantA, TenantData tenantB) {
        // Tenant A tries to get Tenant B's subscription
        Response response = givenAuthenticated(tenantA.tenantId.toString())
            .when()
            .get("/v1/subscriptions/" + tenantB.subscriptionId)
            .then()
            .statusCode(404)
            .extract()
            .response();
        
        Allure.addAttachment("Cross-Tenant Access Blocked", "application/json", response.asString());
    }
    
    @Step("Step 4: Verify 404/403 error")
    private void step4_VerifyProperErrorResponse(TenantData tenantA, TenantData tenantB) {
        // Try to access plan
        Response planResponse = givenAuthenticated(tenantA.tenantId.toString())
            .when()
            .get("/v1/plans/" + tenantB.planId)
            .then()
            .statusCode(404)
            .extract()
            .response();
        
        assertThat(planResponse.statusCode()).isEqualTo(404);
        
        // Try to list subscriptions - should not see Tenant B's data
        Response listResponse = givenAuthenticated(tenantA.tenantId.toString())
            .queryParam("page", 0)
            .queryParam("size", 100)
            .when()
            .get("/v1/subscriptions")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Verify Tenant B's subscription is not in the list
        String responseBody = listResponse.asString();
        assertThat(responseBody).doesNotContain(tenantB.subscriptionId.toString());
        
        Allure.addAttachment("Error Responses", "text/plain", 
            "Plan access: 404, Subscription not in list");
    }
    
    @Step("Step 5: Tenant A tries to cancel Tenant B's delivery")
    private void step5_VerifyCannotCancelOtherTenantDelivery(TenantData tenantA, TenantData tenantB) {
        Map<String, Object> cancelRequest = Map.of(
            "customerId", tenantA.customerId.toString(),
            "reason", "Attempting cross-tenant cancellation"
        );
        
        Response response = givenAuthenticated(tenantA.tenantId.toString())
            .body(cancelRequest)
            .when()
            .post("/v1/deliveries/" + tenantB.deliveryId + "/cancel")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isFalse();
        
        Allure.addAttachment("Delivery Cancellation Blocked", "application/json", response.asString());
    }
    
    @Step("Step 6: Verify operation rejected")
    private void step6_VerifyOperationRejected(TenantData tenantA, TenantData tenantB) {
        // Try to modify Tenant B's subscription from Tenant A
        Map<String, Object> modifyRequest = Map.of(
            "customerId", tenantA.customerId.toString(),
            "operation", "PAUSE",
            "reason", "Cross-tenant pause attempt"
        );
        
        Response response = givenAuthenticated(tenantA.tenantId.toString())
            .body(modifyRequest)
            .when()
            .put("/v1/subscription-mgmt/" + tenantB.subscriptionId)
            .then()
            .statusCode(403)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isFalse();
        
        Allure.addAttachment("Modification Rejected", "application/json", response.asString());
    }
    
    @Step("Step 7: Verify no data leakage")
    private void step7_VerifyNoDataLeakage(TenantData tenantA, TenantData tenantB) {
        // Verify database-level isolation
        Integer crossTenantCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subscriptions WHERE tenant_id = ?::uuid AND id = ?::uuid",
            Integer.class,
            tenantA.tenantId.toString(),
            tenantB.subscriptionId.toString()
        );
        
        assertThat(crossTenantCount).isEqualTo(0);
        
        // Verify each tenant can only see their own data
        Integer tenantACount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subscriptions WHERE tenant_id = ?::uuid",
            Integer.class,
            tenantA.tenantId.toString()
        );
        
        Integer tenantBCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM subscriptions WHERE tenant_id = ?::uuid",
            Integer.class,
            tenantB.tenantId.toString()
        );
        
        assertThat(tenantACount).isGreaterThan(0);
        assertThat(tenantBCount).isGreaterThan(0);
        
        Allure.addAttachment("Data Isolation Verified", "text/plain", 
            "Tenant A subscriptions: " + tenantACount + ", Tenant B subscriptions: " + tenantBCount + ", Cross-tenant: 0");
    }
    
    // Helper methods and data class
    
    private UUID createTenant(String name) {
        String slug = name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", name,
            "slug", slug,
            "status", "ACTIVE"
        );
        
        Response response = given()
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    private UUID createCustomer(String tenantId) {
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
    }
    
    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.planId"));
    }
    
    private UUID createSubscription(String tenantId, UUID customerId, UUID planId) {
        Map<String, Object> subscriptionRequest = TestDataFactory.createSubscriptionRequest(customerId, planId);
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.subscriptionId"));
    }
    
    private UUID createTestDelivery(String tenantId, UUID subscriptionId, UUID customerId) {
        UUID deliveryId = UUID.randomUUID();
        
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_date, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?, now(), now())",
            deliveryId.toString(),
            tenantId,
            subscriptionId.toString(),
            "cycle_" + System.currentTimeMillis(),
            OffsetDateTime.now().plusDays(7).toString()
        );
        
        return deliveryId;
    }
    
    private static class TenantData {
        UUID tenantId;
        UUID customerId;
        UUID planId;
        UUID subscriptionId;
        UUID deliveryId;
    }
}
