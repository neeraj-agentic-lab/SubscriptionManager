package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for tenant management operations.
 * Tests: Tenant CRUD operations, validation, and data integrity.
 */
@Epic("Tenant Management")
@Feature("Tenant Operations")
class TenantManagementTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Should create a new tenant")
    @Description("Tests POST /v1/tenants endpoint")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Tenant creation")
    void shouldCreateTenant() {
        String tenantName = "Test Tenant " + UUID.randomUUID().toString().substring(0, 8);
        String tenantSlug = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
        String tenantId = UUID.randomUUID().toString();
        
        Map<String, Object> tenantRequest = Map.of(
            "name", tenantName,
            "slug", tenantSlug,
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(tenantId)
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("id")).isNotNull();
        assertThat(response.jsonPath().getString("name")).isEqualTo(tenantName);
        assertThat(response.jsonPath().getString("slug")).isEqualTo(tenantSlug);
        assertThat(response.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Tenant Created", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should retrieve tenant by ID")
    @Description("Tests GET /v1/tenants/{id} endpoint")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Tenant retrieval")
    void shouldRetrieveTenantById() {
        // Create tenant first
        UUID tenantId = createTenant("Retrieve Test Tenant");
        
        Response response = givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .when()
            .get("/v1/tenants/" + tenantId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("id")).isEqualTo(tenantId.toString());
        assertThat(response.jsonPath().getString("name")).contains("Retrieve Test Tenant");
        
        Allure.addAttachment("Tenant Retrieved", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should list all tenants with pagination")
    @Description("Tests GET /v1/tenants endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Tenant listing")
    void shouldListAllTenants() {
        // Create multiple tenants
        UUID tenantId1 = createTenant("List Test Tenant 1");
        createTenant("List Test Tenant 2");
        
        Response response = givenAuthenticated(tenantId1.toString())
            .contentType("application/json")
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/v1/tenants")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> tenants = response.jsonPath().getList("content");
        assertThat(tenants).isNotEmpty();
        
        Allure.addAttachment("Tenants List", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should update tenant information")
    @Description("Tests PUT /v1/tenants/{id} endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Tenant update")
    void shouldUpdateTenant() {
        UUID tenantId = createTenant("Original Tenant Name");
        
        Map<String, Object> updateRequest = Map.of(
            "name", "Updated Tenant Name",
            "slug", "updated-tenant-" + UUID.randomUUID().toString().substring(0, 8),
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .body(updateRequest)
            .when()
            .put("/v1/tenants/" + tenantId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("name")).isEqualTo("Updated Tenant Name");
        
        Allure.addAttachment("Tenant Updated", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should delete tenant without data")
    @Description("Tests DELETE /v1/tenants/{id} endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Tenant deletion")
    void shouldDeleteTenantWithoutData() {
        UUID tenantId = createTenant("Tenant To Delete");
        
        Response response = givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .when()
            .delete("/v1/tenants/" + tenantId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("message")).contains("deleted successfully");
        
        // Verify tenant is deleted
        givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .when()
            .get("/v1/tenants/" + tenantId)
            .then()
            .statusCode(404);
        
        Allure.addAttachment("Tenant Deleted", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should prevent deletion of tenant with subscriptions")
    @Description("Tests that tenants with active subscriptions cannot be deleted")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Data integrity")
    void shouldPreventDeletionOfTenantWithSubscriptions() {
        // Create a tenant first
        UUID tenantId = createTenant("Tenant With Subscriptions");
        
        // Create plan and subscription for this tenant
        UUID planId = createPlan(tenantId.toString());
        createSubscriptionDirectly(tenantId.toString(), planId);
        
        // Try to delete tenant - should fail because it has subscriptions
        Response response = givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .when()
            .delete("/v1/tenants/" + tenantId)
            .then()
            .statusCode(409)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("error")).isEqualTo("TENANT_HAS_SUBSCRIPTIONS");
        
        Allure.addAttachment("Deletion Prevented", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should return 404 for non-existent tenant")
    @Description("Tests error handling when tenant doesn't exist")
    @Severity(SeverityLevel.NORMAL)
    @Story("Error handling")
    void shouldReturn404ForNonExistentTenant() {
        UUID nonExistentId = UUID.randomUUID();
        UUID anyTenantId = createTenant("Any Tenant");
        
        Response response = givenAuthenticated(anyTenantId.toString())
            .contentType("application/json")
            .when()
            .get("/v1/tenants/" + nonExistentId)
            .then()
            .statusCode(404)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("error")).isEqualTo("TENANT_NOT_FOUND");
        
        Allure.addAttachment("404 Response", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should create tenant with custom ID")
    @Description("Tests tenant creation with pre-specified UUID")
    @Severity(SeverityLevel.NORMAL)
    @Story("Tenant creation")
    void shouldCreateTenantWithCustomId() {
        UUID customId = UUID.randomUUID();
        String tenantName = "Custom ID Tenant";
        String tenantSlug = "custom-id-tenant-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "id", customId.toString(),
            "name", tenantName,
            "slug", tenantSlug
        );
        
        Response response = givenAuthenticated(customId.toString())
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("id")).isEqualTo(customId.toString());
        
        Allure.addAttachment("Custom ID Tenant", "application/json", response.asString());
    }
    
    // Helper methods
    
    @Step("Create tenant with name: {name}")
    private UUID createTenant(String name) {
        UUID tenantId = UUID.randomUUID();
        String slug = name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", name,
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
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create plan")
    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = Map.of(
            "name", "Test Plan",
            "description", "Test plan for integration testing",
            "basePriceCents", 2999L,  // $29.99 in cents
            "currency", "USD",
            "billingInterval", "MONTHLY",
            "billingIntervalCount", 1,
            "planType", "RECURRING",
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
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create subscription with customer info")
    private UUID createSubscriptionDirectly(String tenantId, UUID planId) {
        // In this API, customers are created implicitly when creating subscriptions
        Map<String, Object> subscriptionRequest = Map.of(
            "planId", planId.toString(),
            "customerEmail", "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
            "customerFirstName", "Test",
            "customerLastName", "Customer",
            "paymentMethodRef", "pm_test_" + UUID.randomUUID().toString().substring(0, 8)
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
}
