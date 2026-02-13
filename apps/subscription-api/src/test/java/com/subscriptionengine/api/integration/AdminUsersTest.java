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
 * Integration tests for AdminUsersController.
 * Tests all CRUD operations and user management functionality.
 * Uses admin APIs only for test data setup.
 * 
 * @author Neeraj Yadav
 */
@Epic("Admin APIs")
@Feature("User Management")
public class AdminUsersTest extends BaseIntegrationTest {
    
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
    
    @Step("Create user via admin API")
    private UUID createUser(String authTenantId, String email, String firstName, String lastName, String role) {
        Map<String, Object> userRequest = Map.of(
            "email", email,
            "password", "TestPassword123!",
            "firstName", firstName,
            "lastName", lastName,
            "role", role
        );
        
        Response response = givenAuthenticated(authTenantId)
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
    @DisplayName("Test 1: Create User")
    @Description("Verify admin can create a new user with all required fields")
    void testCreateUser() {
        String authTenantId = generateUniqueTenantId();
        createTenant(authTenantId, "Test Tenant");
        
        String email = "newuser-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> createRequest = Map.of(
            "email", email,
            "password", "SecurePass123!",
            "firstName", "John",
            "lastName", "Doe",
            "role", "CUSTOMER"
        );
        
        Response response = givenAuthenticated(authTenantId)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/v1/admin/users")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("email", equalTo(email))
            .body("firstName", equalTo("John"))
            .body("lastName", equalTo("Doe"))
            .body("role", equalTo("CUSTOMER"))
            .body("status", equalTo("ACTIVE"))
            .body("createdAt", notNullValue())
            .body("updatedAt", notNullValue())
            .extract().response();
        
        UUID userId = UUID.fromString(response.jsonPath().getString("id"));
        assertThat(userId).isNotNull();
        
        Allure.addAttachment("Create User Response", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Prevent Duplicate Email")
    @Description("Verify creating user with duplicate email returns 409 Conflict")
    void testPreventDuplicateEmail() {
        String authTenantId = generateUniqueTenantId();
        createTenant(authTenantId, "Test Tenant");
        
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        
        // Create first user
        Map<String, Object> createRequest = Map.of(
            "email", email,
            "password", "SecurePass123!",
            "firstName", "Jane",
            "lastName", "Smith",
            "role", "CUSTOMER"
        );
        
        givenAuthenticated(authTenantId)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/v1/admin/users")
            .then()
            .statusCode(201);
        
        // Attempt to create duplicate - should fail with 409
        givenAuthenticated(authTenantId)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/v1/admin/users")
            .then()
            .statusCode(409);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Get User by ID")
    @Description("Verify retrieving user details by ID")
    void testGetUserById() {
        String authTenantId = generateUniqueTenantId();
        createTenant(authTenantId, "Test Tenant");
        
        String email = "getuser-" + UUID.randomUUID() + "@example.com";
        UUID userId = createUser(authTenantId, email, "Alice", "Johnson", "TENANT_ADMIN");
        
        Response response = givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/users/" + userId)
            .then()
            .statusCode(200)
            .body("id", equalTo(userId.toString()))
            .body("email", equalTo(email))
            .body("firstName", equalTo("Alice"))
            .body("lastName", equalTo("Johnson"))
            .body("role", equalTo("TENANT_ADMIN"))
            .body("status", equalTo("ACTIVE"))
            .extract().response();
        
        Allure.addAttachment("Get User Response", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 4: List Users with Pagination and Filters")
    @Description("Verify listing users with pagination, status, and role filters")
    void testListUsersWithFilters() {
        String authTenantId = generateUniqueTenantId();
        createTenant(authTenantId, "Test Tenant");
        
        // Create multiple users with different roles
        createUser(authTenantId, "user1-" + UUID.randomUUID() + "@example.com", "User", "One", "CUSTOMER");
        createUser(authTenantId, "user2-" + UUID.randomUUID() + "@example.com", "User", "Two", "CUSTOMER");
        createUser(authTenantId, "admin1-" + UUID.randomUUID() + "@example.com", "Admin", "One", "TENANT_ADMIN");
        
        // List all users
        Response allUsersResponse = givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/users?page=0&size=10")
            .then()
            .statusCode(200)
            .body("users", hasSize(greaterThanOrEqualTo(3)))
            .body("page", equalTo(0))
            .body("size", equalTo(10))
            .body("totalCount", greaterThanOrEqualTo(3))
            .extract().response();
        
        Allure.addAttachment("All Users", "application/json", allUsersResponse.asString());
        
        // Filter by role
        Response customerResponse = givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/users?role=CUSTOMER")
            .then()
            .statusCode(200)
            .body("users", hasSize(greaterThanOrEqualTo(2)))
            .body("users[0].role", equalTo("CUSTOMER"))
            .extract().response();
        
        Allure.addAttachment("Customers Only", "application/json", customerResponse.asString());
        
        // Filter by status
        givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/users?status=ACTIVE")
            .then()
            .statusCode(200)
            .body("users", not(empty()))
            .body("users[0].status", equalTo("ACTIVE"));
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 5: Update User")
    @Description("Verify updating user information")
    void testUpdateUser() {
        String authTenantId = generateUniqueTenantId();
        createTenant(authTenantId, "Test Tenant");
        
        String email = "updateuser-" + UUID.randomUUID() + "@example.com";
        UUID userId = createUser(authTenantId, email, "Bob", "Smith", "CUSTOMER");
        
        // Update user details
        Map<String, Object> updateRequest = Map.of(
            "firstName", "Robert",
            "lastName", "Johnson",
            "role", "TENANT_ADMIN"
        );
        
        Response response = givenAuthenticated(authTenantId)
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .when()
            .patch("/v1/admin/users/" + userId)
            .then()
            .statusCode(200)
            .body("id", equalTo(userId.toString()))
            .body("firstName", equalTo("Robert"))
            .body("lastName", equalTo("Johnson"))
            .body("role", equalTo("TENANT_ADMIN"))
            .body("email", equalTo(email))
            .extract().response();
        
        Allure.addAttachment("Update User Response", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 6: Suspend and Activate User")
    @Description("Verify suspending and activating user accounts")
    void testSuspendAndActivateUser() {
        String authTenantId = generateUniqueTenantId();
        createTenant(authTenantId, "Test Tenant");
        
        String email = "suspenduser-" + UUID.randomUUID() + "@example.com";
        UUID userId = createUser(authTenantId, email, "Charlie", "Brown", "CUSTOMER");
        
        // Suspend user
        Response suspendResponse = givenAuthenticated(authTenantId)
            .when()
            .post("/v1/admin/users/" + userId + "/suspend")
            .then()
            .statusCode(200)
            .body("id", equalTo(userId.toString()))
            .body("status", equalTo("SUSPENDED"))
            .extract().response();
        
        Allure.addAttachment("Suspend User Response", "application/json", suspendResponse.asString());
        
        // Verify user is suspended
        givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/users/" + userId)
            .then()
            .statusCode(200)
            .body("status", equalTo("SUSPENDED"));
        
        // Activate user
        Response activateResponse = givenAuthenticated(authTenantId)
            .when()
            .post("/v1/admin/users/" + userId + "/activate")
            .then()
            .statusCode(200)
            .body("id", equalTo(userId.toString()))
            .body("status", equalTo("ACTIVE"))
            .extract().response();
        
        Allure.addAttachment("Activate User Response", "application/json", activateResponse.asString());
        
        // Verify user is active again
        givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/users/" + userId)
            .then()
            .statusCode(200)
            .body("status", equalTo("ACTIVE"));
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 7: Delete User (Soft Delete)")
    @Description("Verify soft deleting a user account")
    void testDeleteUser() {
        String authTenantId = generateUniqueTenantId();
        createTenant(authTenantId, "Test Tenant");
        
        String email = "deleteuser-" + UUID.randomUUID() + "@example.com";
        UUID userId = createUser(authTenantId, email, "David", "Wilson", "CUSTOMER");
        
        // Delete user (soft delete)
        givenAuthenticated(authTenantId)
            .when()
            .delete("/v1/admin/users/" + userId)
            .then()
            .statusCode(204);
        
        // Verify user is marked as DELETED
        givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/users/" + userId)
            .then()
            .statusCode(200)
            .body("status", equalTo("DELETED"));
        
        // Verify deleted user doesn't appear in active users list
        givenAuthenticated(authTenantId)
            .when()
            .get("/v1/admin/users?status=ACTIVE")
            .then()
            .statusCode(200)
            .body("users.id", not(hasItem(userId.toString())));
    }
}
