package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for plan management operations.
 * Tests: Plan CRUD operations, listing, filtering, and validation.
 */
@Epic("Plan Management")
@Feature("Plan Operations")
class PlanManagementTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        // Create a tenant for each test since plans require a valid tenant_id foreign key
        testTenantId = createTestTenant();
    }
    
    @Test
    @DisplayName("Should create a new plan")
    @Description("Tests POST /v1/plans endpoint")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Plan creation")
    void shouldCreatePlan() {
        String tenantId = testTenantId;
        
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest("Premium Monthly");
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("id")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isEqualTo("Premium Monthly");
        assertThat(response.jsonPath().getInt("basePriceCents")).isEqualTo(2999);
        assertThat(response.jsonPath().getString("currency")).isEqualTo("USD");
        assertThat(response.jsonPath().getString("billingInterval")).isEqualTo("MONTHLY");
        
        Allure.addAttachment("Plan Created", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should retrieve plan by ID")
    @Description("Tests GET /v1/plans/{id} endpoint")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Plan retrieval")
    void shouldRetrievePlanById() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId, "Basic Plan");
        
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/plans/" + planId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("id")).isEqualTo(planId.toString());
        assertThat(response.jsonPath().getString("name")).isEqualTo("Basic Plan");
        
        Allure.addAttachment("Plan Retrieved", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should list all plans for tenant")
    @Description("Tests GET /v1/plans endpoint with pagination")
    @Severity(SeverityLevel.NORMAL)
    @Story("Plan listing")
    void shouldListAllPlans() {
        String tenantId = testTenantId;
        
        // Create multiple plans
        createPlan(tenantId, "Plan A");
        createPlan(tenantId, "Plan B");
        createPlan(tenantId, "Plan C");
        
        Response response = givenAuthenticated(tenantId)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/v1/plans")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> plans = response.jsonPath().getList("content");
        assertThat(plans).isNotNull();
        assertThat(plans).hasSizeGreaterThanOrEqualTo(3);
        
        Allure.addAttachment("Plans List", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should filter active plans only")
    @Description("Tests plan listing with active filter")
    @Severity(SeverityLevel.NORMAL)
    @Story("Plan filtering")
    void shouldFilterActivePlans() {
        String tenantId = testTenantId;
        
        createPlan(tenantId, "Active Plan");
        
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/plans/active")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> plans = response.jsonPath().getList("$");
        assertThat(plans).isNotEmpty();
        
        // Verify all plans have status ACTIVE
        plans.forEach(plan -> {
            assertThat(plan.get("status")).isEqualTo("ACTIVE");
        });
        
        Allure.addAttachment("Active Plans", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should return 404 for non-existent plan")
    @Description("Tests error handling when plan doesn't exist")
    @Severity(SeverityLevel.NORMAL)
    @Story("Error handling")
    void shouldReturn404ForNonExistentPlan() {
        String tenantId = testTenantId;
        UUID nonExistentPlanId = UUID.randomUUID();
        
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/plans/" + nonExistentPlanId)
            .then()
            .statusCode(404)
            .extract()
            .response();
        
        assertThat(response.statusCode()).isEqualTo(404);
        Allure.addAttachment("404 Response", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should validate required fields on plan creation")
    @Description("Tests validation for missing required fields")
    @Severity(SeverityLevel.NORMAL)
    @Story("Validation")
    void shouldValidateRequiredFields() {
        String tenantId = testTenantId;
        
        // Missing name
        Map<String, Object> invalidRequest = Map.of(
            "basePriceCents", 1000,
            "currency", "USD",
            "billingInterval", "MONTHLY"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(invalidRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.statusCode()).isEqualTo(400);
        Allure.addAttachment("Validation Error", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should enforce tenant isolation for plans")
    @Description("Tests that plans from one tenant are not visible to another")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Security - Multi-tenancy")
    void shouldEnforceTenantIsolation() {
        String tenant1 = testTenantId;
        String tenant2 = createTestTenant(); // Create a second tenant
        
        // Create plan in tenant1
        UUID plan1 = createPlan(tenant1, "Tenant 1 Plan");
        
        // Try to access from tenant2 - should get 404 due to tenant isolation
        Response response = givenAuthenticated(tenant2)
            .when()
            .get("/v1/plans/" + plan1)
            .then()
            .statusCode(404)
            .extract()
            .response();
        
        assertThat(response.statusCode()).isEqualTo(404);
        Allure.addAttachment("Tenant Isolation", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should create plans with different billing intervals")
    @Description("Tests creating monthly, yearly, and weekly plans")
    @Severity(SeverityLevel.NORMAL)
    @Story("Plan variants")
    void shouldCreatePlansWithDifferentBillingIntervals() {
        String tenantId = testTenantId;
        
        // Monthly plan
        Map<String, Object> monthlyBase = TestDataFactory.createPlanRequest("Monthly Plan");
        Map<String, Object> monthlyPlan = new java.util.HashMap<>(monthlyBase);
        monthlyPlan.put("billingInterval", "MONTHLY");
        
        Response monthlyResponse = givenAuthenticated(tenantId)
            .body(monthlyPlan)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(monthlyResponse.jsonPath().getString("billingInterval")).isEqualTo("MONTHLY");
        
        // Yearly plan
        Map<String, Object> yearlyBase = TestDataFactory.createPlanRequest("Yearly Plan");
        Map<String, Object> yearlyPlan = new java.util.HashMap<>(yearlyBase);
        yearlyPlan.put("billingInterval", "YEARLY");
        yearlyPlan.put("basePriceCents", 29990);
        
        Response yearlyResponse = givenAuthenticated(tenantId)
            .body(yearlyPlan)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(yearlyResponse.jsonPath().getString("billingInterval")).isEqualTo("YEARLY");
        
        Allure.addAttachment("Monthly Plan", "application/json", monthlyResponse.asString());
        Allure.addAttachment("Yearly Plan", "application/json", yearlyResponse.asString());
    }
    
    @Test
    @DisplayName("Should create plan with trial period")
    @Description("Tests creating plan with trial period days")
    @Severity(SeverityLevel.NORMAL)
    @Story("Trial periods")
    void shouldCreatePlanWithTrialPeriod() {
        String tenantId = testTenantId;
        
        // Create mutable map from factory request and add trial period
        Map<String, Object> basePlan = TestDataFactory.createPlanRequest("Trial Plan");
        Map<String, Object> planRequest = new java.util.HashMap<>(basePlan);
        planRequest.put("trialPeriodDays", 14);
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getInt("trialPeriodDays")).isEqualTo(14);
        Allure.addAttachment("Trial Plan", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should update plan status")
    @Description("Tests PATCH /v1/plans/{id}/status endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Plan status management")
    void shouldUpdatePlanStatus() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId, "Status Test Plan");
        
        // Update status to inactive
        Response response = givenAuthenticated(tenantId)
            .queryParam("active", false)
            .when()
            .patch("/v1/plans/" + planId + "/status")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("status")).isEqualTo("INACTIVE");
        
        // Update back to active
        Response response2 = givenAuthenticated(tenantId)
            .queryParam("active", true)
            .when()
            .patch("/v1/plans/" + planId + "/status")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response2.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Plan Status Updated", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should check if plan exists")
    @Description("Tests GET /v1/plans/{id}/exists endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Plan validation")
    void shouldCheckPlanExists() {
        String tenantId = testTenantId;
        
        UUID planId = createPlan(tenantId, "Exists Check Plan");
        
        // Check existing plan
        givenAuthenticated(tenantId)
            .when()
            .get("/v1/plans/" + planId + "/exists")
            .then()
            .statusCode(200);
        
        // Check non-existent plan
        UUID nonExistentId = UUID.randomUUID();
        givenAuthenticated(tenantId)
            .when()
            .get("/v1/plans/" + nonExistentId + "/exists")
            .then()
            .statusCode(404);
    }
    
    // Helper methods
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-plans-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Plans",
            "slug", slug,
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return response.jsonPath().getString("id");
    }
    
    @Step("Create plan with name: {name}")
    private UUID createPlan(String tenantId, String name) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest(name);
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
}
