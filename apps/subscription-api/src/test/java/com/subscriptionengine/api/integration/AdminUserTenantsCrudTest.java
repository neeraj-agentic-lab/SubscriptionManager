package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AdminUserTenantsController.
 * Tests all CRUD operations for user-tenant assignment management.
 * Uses admin APIs only for test data setup.
 * 
 * @author Neeraj Yadav
 */
@Epic("Admin APIs")
@Feature("User-Tenant Assignment Management")
public class AdminUserTenantsCrudTest extends BaseIntegrationTest {
    
    @Step("Create tenant via admin API")
    private UUID createTenant(String name) {
        String tenantSlug = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", name,
            "slug", tenantSlug,
            "status", "ACTIVE"
        );
        
        Response response = givenSuperAdmin()
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
    
    @Step("Create user via admin API")
    private UUID createUser(String email) {
        Map<String, Object> userRequest = Map.of(
            "email", email,
            "password", "TestPassword123!",
            "firstName", "Test",
            "lastName", "User",
            "role", "CUSTOMER"
        );
        
        Response response = givenSuperAdmin()
            .contentType("application/json")
            .body(userRequest)
            .when()
            .post("/v1/admin/users")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 1: Assign User to Tenant")
    @Description("Verify user can be assigned to a tenant with a specific role")
    void testAssignUserToTenant() {
        // Create tenant and user
        UUID tenantId = createTenant("Test Tenant 1");
        UUID userId = createUser("user1-" + UUID.randomUUID() + "@example.com");
        
        // Assign user to tenant
        Map<String, Object> assignRequest = Map.of(
            "userId", userId.toString(),
            "tenantId", tenantId.toString(),
            "role", "TENANT_ADMIN"
        );
        
        Response response = givenSuperAdmin()
            .contentType(ContentType.JSON)
            .body(assignRequest)
            .when()
            .post("/v1/admin/user-tenants")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("userId", equalTo(userId.toString()))
            .body("tenantId", equalTo(tenantId.toString()))
            .body("role", equalTo("TENANT_ADMIN"))
            .body("userEmail", notNullValue())
            .body("tenantName", equalTo("Test Tenant 1"))
            .body("assignedAt", notNullValue())
            .extract().response();
        
        UUID assignmentId = UUID.fromString(response.jsonPath().getString("id"));
        assertThat(assignmentId).isNotNull();
        
        Allure.addAttachment("Assignment Response", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Prevent Duplicate User-Tenant Assignment")
    @Description("Verify duplicate assignment returns 409 Conflict")
    void testPreventDuplicateAssignment() {
        // Create tenant and user
        UUID tenantId = createTenant("Test Tenant 2");
        UUID userId = createUser("user2-" + UUID.randomUUID() + "@example.com");
        
        // First assignment - should succeed
        Map<String, Object> assignRequest = Map.of(
            "userId", userId.toString(),
            "tenantId", tenantId.toString(),
            "role", "TENANT_USER"
        );
        
        givenSuperAdmin()
            .contentType(ContentType.JSON)
            .body(assignRequest)
            .when()
            .post("/v1/admin/user-tenants")
            .then()
            .statusCode(201);
        
        // Duplicate assignment - should fail with 409
        givenSuperAdmin()
            .contentType(ContentType.JSON)
            .body(assignRequest)
            .when()
            .post("/v1/admin/user-tenants")
            .then()
            .statusCode(409);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Get User's Tenants")
    @Description("Verify listing all tenants assigned to a user")
    void testGetUserTenants() {
        // Create user and multiple tenants
        UUID userId = createUser("user3-" + UUID.randomUUID() + "@example.com");
        UUID tenant1 = createTenant("Tenant A");
        UUID tenant2 = createTenant("Tenant B");
        UUID tenant3 = createTenant("Tenant C");
        
        // Assign user to all three tenants
        for (UUID tenantId : new UUID[]{tenant1, tenant2, tenant3}) {
            Map<String, Object> assignRequest = Map.of(
                "userId", userId.toString(),
                "tenantId", tenantId.toString(),
                "role", "TENANT_USER"
            );
            
            givenSuperAdmin()
                .contentType(ContentType.JSON)
                .body(assignRequest)
                .post("/v1/admin/user-tenants")
                .then()
                .statusCode(201);
        }
        
        // Get user's tenants
        Response response = givenSuperAdmin()
            .when()
            .get("/v1/admin/user-tenants/user/" + userId)
            .then()
            .statusCode(200)
            .body("$", hasSize(3))
            .body("[0].userId", equalTo(userId.toString()))
            .body("[0].role", equalTo("TENANT_USER"))
            .body("[0].userEmail", notNullValue())
            .body("[0].tenantName", notNullValue())
            .extract().response();
        
        Allure.addAttachment("User Tenants", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 4: Get Tenant's Users")
    @Description("Verify listing all users assigned to a tenant")
    void testGetTenantUsers() {
        // Create tenant and multiple users
        UUID tenantId = createTenant("Test Tenant 4");
        UUID user1 = createUser("user4a-" + UUID.randomUUID() + "@example.com");
        UUID user2 = createUser("user4b-" + UUID.randomUUID() + "@example.com");
        UUID user3 = createUser("user4c-" + UUID.randomUUID() + "@example.com");
        
        // Assign all users to tenant with different roles
        String[] roles = {"TENANT_ADMIN", "TENANT_USER", "CUSTOMER"};
        UUID[] users = {user1, user2, user3};
        
        for (int i = 0; i < users.length; i++) {
            Map<String, Object> assignRequest = Map.of(
                "userId", users[i].toString(),
                "tenantId", tenantId.toString(),
                "role", roles[i]
            );
            
            givenSuperAdmin()
                .contentType(ContentType.JSON)
                .body(assignRequest)
                .post("/v1/admin/user-tenants")
                .then()
                .statusCode(201);
        }
        
        // Get tenant's users
        Response response = givenSuperAdmin()
            .when()
            .get("/v1/admin/user-tenants/tenant/" + tenantId)
            .then()
            .statusCode(200)
            .body("$", hasSize(3))
            .body("[0].tenantId", equalTo(tenantId.toString()))
            .body("[0].tenantName", equalTo("Test Tenant 4"))
            .extract().response();
        
        // Verify different roles are present
        String responseBody = response.asString();
        assertThat(responseBody).contains("TENANT_ADMIN");
        assertThat(responseBody).contains("TENANT_USER");
        assertThat(responseBody).contains("CUSTOMER");
        
        Allure.addAttachment("Tenant Users", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 5: Update User Role and Remove Assignment")
    @Description("Verify updating user role in tenant and removing assignment")
    void testUpdateRoleAndRemoveAssignment() {
        // Create tenant and user
        UUID tenantId = createTenant("Test Tenant 5");
        UUID userId = createUser("user5-" + UUID.randomUUID() + "@example.com");
        
        // Assign user to tenant
        Map<String, Object> assignRequest = Map.of(
            "userId", userId.toString(),
            "tenantId", tenantId.toString(),
            "role", "TENANT_USER"
        );
        
        Response assignResponse = givenSuperAdmin()
            .contentType(ContentType.JSON)
            .body(assignRequest)
            .post("/v1/admin/user-tenants")
            .then()
            .statusCode(201)
            .extract().response();
        
        UUID assignmentId = UUID.fromString(assignResponse.jsonPath().getString("id"));
        
        // Update role from TENANT_USER to TENANT_ADMIN
        Map<String, Object> updateRequest = Map.of("role", "TENANT_ADMIN");
        
        Response updateResponse = givenSuperAdmin()
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .when()
            .patch("/v1/admin/user-tenants/" + assignmentId)
            .then()
            .statusCode(200)
            .body("id", equalTo(assignmentId.toString()))
            .body("role", equalTo("TENANT_ADMIN"))
            .body("userId", equalTo(userId.toString()))
            .body("tenantId", equalTo(tenantId.toString()))
            .extract().response();
        
        Allure.addAttachment("Update Role Response", "application/json", updateResponse.asString());
        
        // Remove assignment
        givenSuperAdmin()
            .when()
            .delete("/v1/admin/user-tenants/" + assignmentId)
            .then()
            .statusCode(204);
        
        // Verify assignment is removed - update should return 404
        givenSuperAdmin()
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .when()
            .patch("/v1/admin/user-tenants/" + assignmentId)
            .then()
            .statusCode(404);
        
        // Verify user has no tenants
        givenSuperAdmin()
            .when()
            .get("/v1/admin/user-tenants/user/" + userId)
            .then()
            .statusCode(200)
            .body("$", hasSize(0));
    }
}
