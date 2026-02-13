package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AdminSubscriptionHistoryController.
 * Tests subscription audit trail and history tracking.
 * Uses admin APIs only for test data setup.
 * 
 * @author Neeraj Yadav
 */
@Epic("Admin APIs")
@Feature("Subscription History & Audit Trail")
public class AdminSubscriptionHistoryTest extends BaseIntegrationTest {
    
    @Step("Create tenant via admin API")
    private UUID createTenant(String authTenantId, String name) {
        String tenantSlug = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", name,
            "slug", tenantSlug,
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(authTenantId)
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/admin/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create subscription plan")
    private UUID createPlan(String authTenantId) {
        Map<String, Object> planRequest = Map.of(
            "name", "Test Plan",
            "description", "Test subscription plan",
            "billingInterval", "MONTHLY",
            "basePriceCents", 2999,
            "currency", "USD",
            "trialDays", 14,
            "isActive", true
        );
        
        Response response = givenAuthenticated(authTenantId)
            .contentType(ContentType.JSON)
            .body(planRequest)
            .when()
            .post("/v1/admin/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create subscription")
    private UUID createSubscription(String authTenantId, UUID planId) {
        Map<String, Object> subscriptionRequest = Map.of(
            "planId", planId.toString(),
            "customerId", "cust_" + UUID.randomUUID().toString().substring(0, 8)
        );
        
        Response response = givenAuthenticated(authTenantId)
            .contentType(ContentType.JSON)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 1: Get Subscription History with Pagination")
    @Description("Verify retrieving subscription audit trail with pagination returns proper structure")
    void testGetSubscriptionHistoryWithPagination() {
        String authTenantId = generateUniqueTenantId();
        
        // Use a random subscription ID - endpoint should return empty history with proper structure
        UUID subscriptionId = UUID.randomUUID();
        
        // Get subscription history with pagination
        Response response = givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history?page=0&size=10")
            .then()
            .statusCode(200)
            .body("history", notNullValue())
            .body("page", equalTo(0))
            .body("size", equalTo(10))
            .body("totalCount", greaterThanOrEqualTo(0))
            .body("totalPages", greaterThanOrEqualTo(0))
            .extract().response();
        
        Allure.addAttachment("Subscription History", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Get All Subscription History")
    @Description("Verify retrieving complete subscription audit trail without pagination returns proper structure")
    void testGetAllSubscriptionHistory() {
        String authTenantId = generateUniqueTenantId();
        
        // Use a random subscription ID - endpoint should return empty list
        UUID subscriptionId = UUID.randomUUID();
        
        // Get all subscription history
        Response response = givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history/all")
            .then()
            .statusCode(200)
            .body("$", notNullValue())
            .extract().response();
        
        Allure.addAttachment("All Subscription History", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 3: Verify History Endpoint Pagination Parameters")
    @Description("Verify that history endpoint accepts different pagination parameters")
    void testHistoryPaginationParameters() {
        String authTenantId = generateUniqueTenantId();
        
        // Use a random subscription ID
        UUID subscriptionId = UUID.randomUUID();
        
        // Test with different page sizes
        Response response1 = givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history?page=0&size=5")
            .then()
            .statusCode(200)
            .body("page", equalTo(0))
            .body("size", equalTo(5))
            .extract().response();
        
        Allure.addAttachment("History Page 0 Size 5", "application/json", response1.asString());
        
        // Test with different page number
        Response response2 = givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history?page=1&size=20")
            .then()
            .statusCode(200)
            .body("page", equalTo(1))
            .body("size", equalTo(20))
            .extract().response();
        
        Allure.addAttachment("History Page 1 Size 20", "application/json", response2.asString());
    }
}
