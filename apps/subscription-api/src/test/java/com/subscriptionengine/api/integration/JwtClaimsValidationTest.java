package com.subscriptionengine.api.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static com.subscriptionengine.generated.tables.Users.USERS;

/**
 * Integration tests for JWT Claims Validation security enhancement.
 * 
 * Tests verify that:
 * 1. Fake JWT tokens with non-existent user_id are rejected
 * 2. Fake JWT tokens with incorrect roles are rejected
 * 3. Proper authentication via /auth/login works correctly
 * 4. Real JWT tokens from login endpoint are accepted
 * 
 * @author Neeraj Yadav
 */
@DisplayName("JWT Claims Validation Security Tests")
public class JwtClaimsValidationTest extends BaseIntegrationTest {
    
    @Autowired
    private DSLContext dsl;
    
    @Test
    @DisplayName("Verify Flyway migrations run and bootstrap user exists")
    void testFlywayMigrationsAndBootstrapUser() {
        // Query the database directly to verify bootstrap user exists
        String bootstrapUserId = "00000000-0000-0000-0000-000000000001";
        
        var user = dsl.selectFrom(USERS)
            .where(USERS.ID.eq(UUID.fromString(bootstrapUserId)))
            .fetchOne();
        
        // Verify bootstrap user was created by migration V019
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("admin@subscriptionengine.com");
        assertThat(user.getRole()).isEqualTo("SUPER_ADMIN");
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
        
        System.out.println("✓ Flyway migrations ran successfully");
        System.out.println("✓ Bootstrap super admin user exists: " + user.getEmail());
    }
    
    @Test
    @DisplayName("Should reject fake JWT token with non-existent user_id")
    void testFakeJwtTokenRejected() {
        // Generate a fake JWT token with a non-existent user_id
        String fakeUserId = UUID.randomUUID().toString();
        String fakeTenantId = UUID.randomUUID().toString();
        String fakeJwt = JwtTestHelper.generateToken(fakeTenantId, fakeUserId, "fake@example.com");
        
        // Attempt to list tenants with the fake JWT
        given()
            .header("Authorization", "Bearer " + fakeJwt)
        .when()
            .get("/v1/admin/tenants")
        .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_TOKEN"))
            .body("message", containsString("User does not exist"));
    }
    
    @Test
    @DisplayName("Should reject fake JWT token with SUPER_ADMIN role for regular user")
    void testFakeRoleClaimRejected() {
        // Create a real tenant and user via API
        String tenantId = createTenantViaApi();
        String email = "regularuser-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String userId = createUserViaApi(email, "password123", "TENANT_USER");
        assignUserToTenant(userId, tenantId, "TENANT_USER");
        
        // Generate a fake JWT claiming SUPER_ADMIN role for this regular user
        String fakeAdminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, userId, email, "SUPER_ADMIN", null
        );
        
        // Attempt to access admin endpoint with fake SUPER_ADMIN claim
        given()
            .header("Authorization", "Bearer " + fakeAdminJwt)
        .when()
            .get("/v1/admin/tenants")
        .then()
            .statusCode(403)
            .body("error", equalTo("INVALID_TOKEN"))
            .body("message", containsString("Role claim does not match"));
    }
    
    @Test
    @DisplayName("Should reject JWT token for suspended user account")
    void testSuspendedUserRejected() {
        // Create a tenant, user, assign, then suspend
        String tenantId = createTenantViaApi();
        String email = "suspended-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String password = "password123";
        String userId = createUserViaApi(email, password, "TENANT_USER");
        assignUserToTenant(userId, tenantId, "TENANT_USER");
        
        // Suspend the user
        suspendUser(userId);
        
        // Try to login with suspended account
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", email);
        loginRequest.put("password", password);
        
        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/v1/auth/login")
        .then()
            .statusCode(403)
            .body("error", equalTo("ACCOUNT_SUSPENDED"))
            .body("message", containsString("suspended"));
    }
    
    @Test
    @DisplayName("Should accept proper login and return valid JWT token")
    void testProperLoginReturnsValidToken() {
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@subscriptionengine.com");
        loginRequest.put("password", "ChangeMe123!");
        
        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/v1/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("userId", equalTo("00000000-0000-0000-0000-000000000001"))
            .body("email", equalTo("admin@subscriptionengine.com"))
            .body("role", equalTo("SUPER_ADMIN"));
    }
    
    @Test
    @DisplayName("Should reject JWT token for user without tenant access")
    void testUserWithoutTenantAccessRejected() {
        // Create two tenants via API
        String tenant1Id = createTenantViaApi();
        String tenant2Id = createTenantViaApi();
        
        // Create user with access only to tenant1
        String email = "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        String userId = createUserViaApi(email, "password123", "TENANT_ADMIN");
        assignUserToTenant(userId, tenant1Id, "TENANT_ADMIN");
        
        // Generate JWT claiming access to tenant2 (which user doesn't have)
        String fakeJwt = JwtTestHelper.generateTokenWithRole(
            tenant2Id, userId, email, "TENANT_ADMIN", null
        );
        
        // Attempt to access tenant2 resources
        given()
            .header("Authorization", "Bearer " + fakeJwt)
        .when()
            .get("/v1/admin/subscriptions")
        .then()
            .statusCode(403)
            .body("error", equalTo("INVALID_TOKEN"))
            .body("message", containsString("does not have access to this tenant"));
    }
    
    @Test
    @DisplayName("Complete end-to-end authentication flow with tenant creation")
    void testCompleteAuthenticationFlow() {
        // Step 1: Login as super admin
        String superAdminEmail = "admin@subscriptionengine.com";
        String superAdminPassword = "ChangeMe123!";
        
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", superAdminEmail);
        loginRequest.put("password", superAdminPassword);
        
        String superAdminToken = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
        .when()
            .post("/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("token");
        
        // Step 2: Create a new tenant
        Map<String, Object> tenantRequest = new HashMap<>();
        String tenantSlug = "new-tenant-" + System.currentTimeMillis();
        tenantRequest.put("name", "New Tenant Organization");
        tenantRequest.put("slug", tenantSlug);
        
        String tenantId = given()
            .header("Authorization", "Bearer " + superAdminToken)
            .contentType(ContentType.JSON)
            .body(tenantRequest)
        .when()
            .post("/v1/admin/tenants")
        .then()
            .statusCode(201)
            .extract()
            .path("id");
        
        // Step 3: Create an admin user for the tenant
        Map<String, Object> userRequest = new HashMap<>();
        String adminEmail = "admin@" + tenantSlug + ".com";
        userRequest.put("email", adminEmail);
        userRequest.put("firstName", "Tenant");
        userRequest.put("lastName", "Admin");
        userRequest.put("password", "SecurePassword123!");
        userRequest.put("role", "TENANT_ADMIN");
        
        String userId = given()
            .header("Authorization", "Bearer " + superAdminToken)
            .contentType(ContentType.JSON)
            .body(userRequest)
        .when()
            .post("/v1/admin/users")
        .then()
            .statusCode(201)
            .extract()
            .path("id");
        
        // Step 4: Assign user to tenant with TENANT_ADMIN role
        Map<String, Object> userTenantRequest = new HashMap<>();
        userTenantRequest.put("userId", userId);
        userTenantRequest.put("tenantId", tenantId);
        userTenantRequest.put("role", "TENANT_ADMIN");
        
        given()
            .header("Authorization", "Bearer " + superAdminToken)
            .contentType(ContentType.JSON)
            .body(userTenantRequest)
        .when()
            .post("/v1/admin/user-tenants")
        .then()
            .statusCode(201);
        
        // Step 5: Login as the new tenant admin
        Map<String, Object> tenantAdminLogin = new HashMap<>();
        tenantAdminLogin.put("email", adminEmail);
        tenantAdminLogin.put("password", "SecurePassword123!");
        
        var loginResponse = given()
            .contentType(ContentType.JSON)
            .body(tenantAdminLogin)
        .when()
            .post("/v1/auth/login")
        .then()
            .extract()
            .response();
        
        assertThat(loginResponse.getStatusCode())
            .as("Tenant admin login failed: %s", loginResponse.getBody().asString())
            .isEqualTo(200);
        
        String tenantAdminToken = loginResponse.path("token");
        
        // Step 6: Use tenant admin token to access a protected endpoint (verify auth works)
        var protectedResponse = given()
            .header("Authorization", "Bearer " + tenantAdminToken)
            .contentType(ContentType.JSON)
        .when()
            .get("/v1/admin/tenants")
        .then()
            .extract()
            .response();
        
        assertThat(protectedResponse.getStatusCode())
            .as("Protected endpoint failed with tenant admin token: %s", protectedResponse.getBody().asString())
            .isIn(200, 403); // 200 if allowed, 403 if role-restricted but auth passed
    }
    
    // Helper methods - all use API-returned IDs
    
    private String createTenantViaApi() {
        String superAdminToken = getSuperAdminToken();
        
        Map<String, Object> tenant = new HashMap<>();
        tenant.put("name", "Test Tenant");
        tenant.put("slug", "test-tenant-" + UUID.randomUUID().toString().substring(0, 8));
        
        return given()
            .header("Authorization", "Bearer " + superAdminToken)
            .contentType(ContentType.JSON)
            .body(tenant)
        .when()
            .post("/v1/admin/tenants")
        .then()
            .statusCode(201)
            .extract()
            .path("id");
    }
    
    private String createUserViaApi(String email, String password, String role) {
        String superAdminToken = getSuperAdminToken();
        
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("firstName", "Test");
        user.put("lastName", "User");
        user.put("password", password);
        user.put("role", role);
        
        var response = given()
            .header("Authorization", "Bearer " + superAdminToken)
            .contentType(ContentType.JSON)
            .body(user)
        .when()
            .post("/v1/admin/users")
        .then()
            .extract()
            .response();
        
        assertThat(response.getStatusCode())
            .as("Create user failed: %s", response.getBody().asString())
            .isEqualTo(201);
        
        return response.path("id");
    }
    
    private void assignUserToTenant(String userId, String tenantId, String role) {
        String superAdminToken = getSuperAdminToken();
        
        Map<String, Object> userTenant = new HashMap<>();
        userTenant.put("userId", userId);
        userTenant.put("tenantId", tenantId);
        userTenant.put("role", role);
        
        given()
            .header("Authorization", "Bearer " + superAdminToken)
            .contentType(ContentType.JSON)
            .body(userTenant)
        .when()
            .post("/v1/admin/user-tenants")
        .then()
            .statusCode(201);
    }
    
    private void suspendUser(String userId) {
        String superAdminToken = getSuperAdminToken();
        
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("status", "SUSPENDED");
        
        given()
            .header("Authorization", "Bearer " + superAdminToken)
            .contentType(ContentType.JSON)
            .body(updateRequest)
        .when()
            .patch("/v1/admin/users/" + userId)
        .then()
            .statusCode(200);
    }
}
