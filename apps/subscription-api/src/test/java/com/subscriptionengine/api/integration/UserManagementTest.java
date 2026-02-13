package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for user management and tenant assignment.
 * Tests: User creation, authentication, tenant assignment, role updates, and lifecycle.
 * 
 * @author Neeraj Yadav
 */
@Epic("User Management")
@Feature("User & Tenant Management")
class UserManagementTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("User Creation with BCrypt Password Hashing")
    @Description("Tests user creation with secure password hashing and email uniqueness")
    @Story("User Management")
    void testUserCreationAndAuthentication() {
        // Given - User creation request
        String uniqueEmail = "admin-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Map<String, Object> userRequest = Map.of(
            "email", uniqueEmail,
            "password", "SecurePassword123!",
            "firstName", "John",
            "lastName", "Doe",
            "role", "TENANT_ADMIN"
        );
        
        // When - Create user
        Response createResponse = givenAuthenticated(testTenantId)
            .body(userRequest)
            .when()
            .post("/api/admin/users")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Then - Verify user created
        UUID userId = UUID.fromString(createResponse.jsonPath().getString("id"));
        assertThat(createResponse.jsonPath().getString("email")).isEqualTo(uniqueEmail);
        assertThat(createResponse.jsonPath().getString("firstName")).isEqualTo("John");
        assertThat(createResponse.jsonPath().getString("lastName")).isEqualTo("Doe");
        assertThat(createResponse.jsonPath().getString("role")).isEqualTo("TENANT_ADMIN");
        assertThat(createResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        // Verify password NOT returned in response
        assertThat(createResponse.jsonPath().getString("password")).isNull();
        assertThat(createResponse.jsonPath().getString("passwordHash")).isNull();
        
        Allure.addAttachment("User Created", "application/json", createResponse.asString());
        
        // When - Attempt to create duplicate email
        Map<String, Object> duplicateRequest = Map.of(
            "email", uniqueEmail,
            "password", "AnotherPassword456!",
            "firstName", "Jane",
            "lastName", "Smith",
            "role", "CUSTOMER"
        );
        
        Response duplicateResponse = givenAuthenticated(testTenantId)
            .body(duplicateRequest)
            .when()
            .post("/api/admin/users")
            .then()
            .statusCode(409)
            .extract()
            .response();
        
        // Then - Verify conflict error
        String errorMessage = duplicateResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("email")
                                 .containsIgnoringCase("exists");
        
        Allure.addAttachment("Duplicate Email Error", "application/json", duplicateResponse.asString());
        
        // When - List users
        Response listResponse = givenAuthenticated(testTenantId)
            .queryParam("page", 0)
            .queryParam("size", 20)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify new user appears in list
        List<Map<String, Object>> users = listResponse.jsonPath().getList("content");
        boolean userFound = users.stream()
            .anyMatch(u -> userId.toString().equals(u.get("id")));
        assertThat(userFound).isTrue();
        
        Allure.addAttachment("User List", "application/json", listResponse.asString());
        
        // When - Get user by ID
        Response getUserResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/users/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify user details
        assertThat(getUserResponse.jsonPath().getString("id")).isEqualTo(userId.toString());
        assertThat(getUserResponse.jsonPath().getString("email")).isEqualTo(uniqueEmail);
        
        Allure.addAttachment("User Details", "application/json", getUserResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("User-Tenant Assignment & Multi-Tenancy")
    @Description("Tests user assignment to multiple tenants with different roles")
    @Story("Tenant Assignment")
    void testUserTenantAssignment() {
        // Given - Create user
        UUID userId = createTestUser(testTenantId, "user@example.com");
        
        // Given - Create two tenants
        String tenantAId = createTestTenant("Tenant A");
        String tenantBId = createTestTenant("Tenant B");
        
        // When - Assign user to Tenant A with ADMIN role
        Map<String, Object> assignmentARequest = Map.of(
            "userId", userId.toString(),
            "tenantId", tenantAId,
            "role", "ADMIN"
        );
        
        Response assignmentAResponse = givenAuthenticated(testTenantId)
            .body(assignmentARequest)
            .when()
            .post("/api/admin/user-tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Then - Verify assignment created
        UUID assignmentAId = UUID.fromString(assignmentAResponse.jsonPath().getString("id"));
        assertThat(assignmentAResponse.jsonPath().getString("userId")).isEqualTo(userId.toString());
        assertThat(assignmentAResponse.jsonPath().getString("tenantId")).isEqualTo(tenantAId);
        assertThat(assignmentAResponse.jsonPath().getString("role")).isEqualTo("ADMIN");
        assertThat(assignmentAResponse.jsonPath().getString("assignedAt")).isNotNull();
        
        Allure.addAttachment("Tenant A Assignment", "application/json", assignmentAResponse.asString());
        
        // When - Assign same user to Tenant B with MEMBER role
        Map<String, Object> assignmentBRequest = Map.of(
            "userId", userId.toString(),
            "tenantId", tenantBId,
            "role", "MEMBER"
        );
        
        Response assignmentBResponse = givenAuthenticated(testTenantId)
            .body(assignmentBRequest)
            .when()
            .post("/api/admin/user-tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Then - Verify second assignment created
        assertThat(assignmentBResponse.jsonPath().getString("userId")).isEqualTo(userId.toString());
        assertThat(assignmentBResponse.jsonPath().getString("tenantId")).isEqualTo(tenantBId);
        assertThat(assignmentBResponse.jsonPath().getString("role")).isEqualTo("MEMBER");
        
        Allure.addAttachment("Tenant B Assignment", "application/json", assignmentBResponse.asString());
        
        // When - Retrieve user's tenants
        Response userTenantsResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/user-tenants/user/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify both tenants returned
        List<Map<String, Object>> userTenants = userTenantsResponse.jsonPath().getList("$");
        assertThat(userTenants).hasSize(2);
        
        // Verify Tenant A assignment
        Map<String, Object> tenantAAssignment = userTenants.stream()
            .filter(t -> tenantAId.equals(t.get("tenantId")))
            .findFirst()
            .orElseThrow();
        assertThat(tenantAAssignment.get("role")).isEqualTo("ADMIN");
        assertThat(tenantAAssignment.get("tenantName")).isEqualTo("Tenant A");
        
        // Verify Tenant B assignment
        Map<String, Object> tenantBAssignment = userTenants.stream()
            .filter(t -> tenantBId.equals(t.get("tenantId")))
            .findFirst()
            .orElseThrow();
        assertThat(tenantBAssignment.get("role")).isEqualTo("MEMBER");
        assertThat(tenantBAssignment.get("tenantName")).isEqualTo("Tenant B");
        
        Allure.addAttachment("User's Tenants", "application/json", userTenantsResponse.asString());
        
        // When - Retrieve Tenant A's users
        Response tenantAUsersResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/user-tenants/tenant/" + tenantAId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify user appears in Tenant A's user list
        List<Map<String, Object>> tenantAUsers = tenantAUsersResponse.jsonPath().getList("$");
        boolean userFoundInTenantA = tenantAUsers.stream()
            .anyMatch(u -> userId.toString().equals(u.get("userId")));
        assertThat(userFoundInTenantA).isTrue();
        
        Allure.addAttachment("Tenant A Users", "application/json", tenantAUsersResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("User Lifecycle - Suspend & Activate")
    @Description("Tests user account suspension and activation")
    @Story("User Lifecycle")
    void testUserSuspendAndActivate() {
        // Given - Create active user
        UUID userId = createTestUser(testTenantId, "active-user@example.com");
        
        // Verify user is active
        Response getUserResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/users/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(getUserResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        // When - Suspend user
        Response suspendResponse = givenAuthenticated(testTenantId)
            .when()
            .post("/api/admin/users/" + userId + "/suspend")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify status changed to SUSPENDED
        assertThat(suspendResponse.jsonPath().getString("status")).isEqualTo("SUSPENDED");
        
        Allure.addAttachment("User Suspended", "application/json", suspendResponse.asString());
        
        // Verify suspended status persists
        Response suspendedUserResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/users/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(suspendedUserResponse.jsonPath().getString("status")).isEqualTo("SUSPENDED");
        
        // When - Activate user
        Response activateResponse = givenAuthenticated(testTenantId)
            .when()
            .post("/api/admin/users/" + userId + "/activate")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify status changed back to ACTIVE
        assertThat(activateResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("User Activated", "application/json", activateResponse.asString());
        
        // Verify active status persists
        Response activeUserResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/users/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(activeUserResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("User-Tenant Role Updates")
    @Description("Tests updating user roles within a tenant")
    @Story("Tenant Assignment")
    void testUserTenantRoleUpdate() {
        // Given - Create user and assign to tenant with VIEWER role
        UUID userId = createTestUser(testTenantId, "viewer@example.com");
        String tenantId = createTestTenant("Test Tenant");
        
        Map<String, Object> assignmentRequest = Map.of(
            "userId", userId.toString(),
            "tenantId", tenantId,
            "role", "VIEWER"
        );
        
        Response assignmentResponse = givenAuthenticated(testTenantId)
            .body(assignmentRequest)
            .when()
            .post("/api/admin/user-tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID assignmentId = UUID.fromString(assignmentResponse.jsonPath().getString("id"));
        assertThat(assignmentResponse.jsonPath().getString("role")).isEqualTo("VIEWER");
        
        Allure.addAttachment("Initial Assignment - VIEWER", "application/json", assignmentResponse.asString());
        
        // When - Update role to ADMIN
        Map<String, Object> updateRequest = Map.of(
            "role", "ADMIN"
        );
        
        Response updateResponse = givenAuthenticated(testTenantId)
            .body(updateRequest)
            .when()
            .patch("/api/admin/user-tenants/" + assignmentId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify role updated
        assertThat(updateResponse.jsonPath().getString("role")).isEqualTo("ADMIN");
        assertThat(updateResponse.jsonPath().getString("id")).isEqualTo(assignmentId.toString());
        
        Allure.addAttachment("Updated Assignment - ADMIN", "application/json", updateResponse.asString());
        
        // Verify role update persists
        Response userTenantsResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/user-tenants/user/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> userTenants = userTenantsResponse.jsonPath().getList("$");
        Map<String, Object> assignment = userTenants.stream()
            .filter(t -> tenantId.equals(t.get("tenantId")))
            .findFirst()
            .orElseThrow();
        
        assertThat(assignment.get("role")).isEqualTo("ADMIN");
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("User-Tenant Removal & Access Revocation")
    @Description("Tests removing user from tenant while maintaining access to other tenants")
    @Story("Tenant Assignment")
    void testUserTenantRemoval() {
        // Given - Create user and assign to two tenants
        UUID userId = createTestUser(testTenantId, "multi-tenant@example.com");
        String tenantAId = createTestTenant("Tenant A");
        String tenantBId = createTestTenant("Tenant B");
        
        // Assign to Tenant A
        Map<String, Object> assignmentARequest = Map.of(
            "userId", userId.toString(),
            "tenantId", tenantAId,
            "role", "MEMBER"
        );
        
        Response assignmentAResponse = givenAuthenticated(testTenantId)
            .body(assignmentARequest)
            .when()
            .post("/api/admin/user-tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID assignmentAId = UUID.fromString(assignmentAResponse.jsonPath().getString("id"));
        
        // Assign to Tenant B
        Map<String, Object> assignmentBRequest = Map.of(
            "userId", userId.toString(),
            "tenantId", tenantBId,
            "role", "MEMBER"
        );
        
        givenAuthenticated(testTenantId)
            .body(assignmentBRequest)
            .when()
            .post("/api/admin/user-tenants")
            .then()
            .statusCode(201);
        
        // Verify user has 2 tenant assignments
        Response beforeRemovalResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/user-tenants/user/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(beforeRemovalResponse.jsonPath().getList("$")).hasSize(2);
        
        // When - Remove user from Tenant A
        givenAuthenticated(testTenantId)
            .when()
            .delete("/api/admin/user-tenants/" + assignmentAId)
            .then()
            .statusCode(204);
        
        Allure.addAttachment("Removed from Tenant A", "User removed from Tenant A");
        
        // Then - Verify user no longer has access to Tenant A
        Response afterRemovalResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/user-tenants/user/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> remainingTenants = afterRemovalResponse.jsonPath().getList("$");
        assertThat(remainingTenants).hasSize(1);
        
        // Verify only Tenant B remains
        Map<String, Object> remainingTenant = remainingTenants.get(0);
        assertThat(remainingTenant.get("tenantId")).isEqualTo(tenantBId);
        assertThat(remainingTenant.get("tenantName")).isEqualTo("Tenant B");
        
        Allure.addAttachment("Remaining Tenants", "application/json", afterRemovalResponse.asString());
        
        // Verify Tenant A no longer lists this user
        Response tenantAUsersResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/user-tenants/tenant/" + tenantAId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> tenantAUsers = tenantAUsersResponse.jsonPath().getList("$");
        boolean userStillInTenantA = tenantAUsers.stream()
            .anyMatch(u -> userId.toString().equals(u.get("userId")));
        assertThat(userStillInTenantA).isFalse();
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("User Listing with Pagination & Filtering")
    @Description("Tests user listing with pagination and status/role filters")
    @Story("User Management")
    void testUserListingAndFiltering() {
        // Given - Create multiple users with different roles and statuses
        List<UUID> userIds = new ArrayList<>();
        
        // Create 10 TENANT_ADMIN users (active)
        for (int i = 0; i < 10; i++) {
            UUID userId = createTestUser(testTenantId, "admin" + i + "@example.com", "TENANT_ADMIN");
            userIds.add(userId);
        }
        
        // Create 10 CUSTOMER users (active)
        for (int i = 0; i < 10; i++) {
            UUID userId = createTestUser(testTenantId, "customer" + i + "@example.com", "CUSTOMER");
            userIds.add(userId);
        }
        
        // Suspend 5 CUSTOMER users
        for (int i = 0; i < 5; i++) {
            UUID userId = userIds.get(10 + i);
            givenAuthenticated(testTenantId)
                .when()
                .post("/api/admin/users/" + userId + "/suspend")
                .then()
                .statusCode(200);
        }
        
        // When - List users with pagination (page=0, size=20)
        Response page0Response = givenAuthenticated(testTenantId)
            .queryParam("page", 0)
            .queryParam("size", 20)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify pagination
        List<Map<String, Object>> page0Users = page0Response.jsonPath().getList("content");
        assertThat(page0Users).hasSizeGreaterThanOrEqualTo(20);
        assertThat(page0Response.jsonPath().getInt("totalElements")).isGreaterThanOrEqualTo(20);
        
        Allure.addAttachment("Page 0 - All Users", "application/json", page0Response.asString());
        
        // When - Filter by status=ACTIVE
        Response activeUsersResponse = givenAuthenticated(testTenantId)
            .queryParam("status", "ACTIVE")
            .queryParam("page", 0)
            .queryParam("size", 50)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify only active users returned
        List<Map<String, Object>> activeUsers = activeUsersResponse.jsonPath().getList("content");
        boolean allActive = activeUsers.stream()
            .allMatch(u -> "ACTIVE".equals(u.get("status")));
        assertThat(allActive).isTrue();
        
        Allure.addAttachment("Active Users Only", "application/json", activeUsersResponse.asString());
        
        // When - Filter by role=TENANT_ADMIN
        Response adminUsersResponse = givenAuthenticated(testTenantId)
            .queryParam("role", "TENANT_ADMIN")
            .queryParam("page", 0)
            .queryParam("size", 50)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify only admins returned
        List<Map<String, Object>> adminUsers = adminUsersResponse.jsonPath().getList("content");
        boolean allAdmins = adminUsers.stream()
            .allMatch(u -> "TENANT_ADMIN".equals(u.get("role")));
        assertThat(allAdmins).isTrue();
        
        Allure.addAttachment("Admin Users Only", "application/json", adminUsersResponse.asString());
        
        // When - Filter by status=SUSPENDED and role=CUSTOMER
        Response suspendedCustomersResponse = givenAuthenticated(testTenantId)
            .queryParam("status", "SUSPENDED")
            .queryParam("role", "CUSTOMER")
            .queryParam("page", 0)
            .queryParam("size", 50)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify combined filters work
        List<Map<String, Object>> suspendedCustomers = suspendedCustomersResponse.jsonPath().getList("content");
        boolean allSuspendedCustomers = suspendedCustomers.stream()
            .allMatch(u -> "SUSPENDED".equals(u.get("status")) && "CUSTOMER".equals(u.get("role")));
        assertThat(allSuspendedCustomers).isTrue();
        assertThat(suspendedCustomers).hasSizeGreaterThanOrEqualTo(5);
        
        Allure.addAttachment("Suspended Customers", "application/json", suspendedCustomersResponse.asString());
    }
    
    // Helper methods
    
    @Step("Create test tenant")
    private String createTestTenant() {
        return createTestTenant("Test Tenant");
    }
    
    @Step("Create test tenant: {name}")
    private String createTestTenant(String name) {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
        
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
        
        return response.jsonPath().getString("id");
    }
    
    @Step("Create test user with email: {email}")
    private UUID createTestUser(String tenantId, String email) {
        return createTestUser(tenantId, email, "CUSTOMER");
    }
    
    @Step("Create test user with email: {email} and role: {role}")
    private UUID createTestUser(String tenantId, String email, String role) {
        Map<String, Object> userRequest = Map.of(
            "email", email,
            "password", "TestPassword123!",
            "firstName", "Test",
            "lastName", "User",
            "role", role
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(userRequest)
            .when()
            .post("/api/admin/users")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
}
