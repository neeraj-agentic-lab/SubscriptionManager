package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AuthController endpoints.
 * Tests: User login, API key authentication, tenant switching, and user context.
 * 
 * @author Neeraj Yadav
 */
@Epic("Authentication")
@Feature("Auth Endpoints")
class AuthControllerTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private String testTenantId;
    private String testUserId;
    private String testUserEmail;
    private String testUserPassword = "SecurePassword123!";
    private String apiClientId;
    private String apiKey;
    private String apiSecret;
    
    @BeforeEach
    void setupTestData() {
        testUserEmail = "testuser-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        testTenantId = createTestTenant();
        
        // Create test user
        String passwordHash = new BCryptPasswordEncoder(12).encode(testUserPassword);
        testUserId = jdbcTemplate.queryForObject(
            "INSERT INTO users (id, email, password_hash, first_name, last_name, full_name, role, status, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) RETURNING id::text",
            String.class,
            UUID.randomUUID(),
            testUserEmail,
            passwordHash,
            "Test",
            "User",
            "Test User",
            "TENANT_USER",
            "ACTIVE"
        );
        
        // Assign user to tenant
        jdbcTemplate.update(
            "INSERT INTO user_tenants (id, user_id, tenant_id, role, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, NOW(), NOW())",
            UUID.randomUUID(),
            UUID.fromString(testUserId),
            UUID.fromString(testTenantId),
            "TENANT_ADMIN"
        );
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /v1/auth/login - Successful user login")
    @Description("Tests user login with valid email and password, returns JWT token")
    @Story("User Authentication")
    void testUserLoginSuccess() {
        Map<String, Object> loginRequest = Map.of(
            "email", testUserEmail,
            "password", testUserPassword
        );
        
        Response response = given()
            .contentType("application/json")
            .body(loginRequest)
            .when()
            .post("/v1/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("userId", equalTo(testUserId))
            .body("email", equalTo(testUserEmail))
            .body("tenantId", equalTo(testTenantId))
            .body("role", equalTo("TENANT_ADMIN"))
            .body("expiresAt", notNullValue())
            .extract()
            .response();
        
        String token = response.path("token");
        assertThat(token).isNotEmpty();
        assertThat(token).startsWith("eyJ"); // JWT format
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /v1/auth/login - Invalid credentials")
    @Description("Tests login with invalid password returns 401")
    @Story("User Authentication")
    void testUserLoginInvalidPassword() {
        Map<String, Object> loginRequest = Map.of(
            "email", testUserEmail,
            "password", "WrongPassword123!"
        );
        
        given()
            .contentType("application/json")
            .body(loginRequest)
            .when()
            .post("/v1/auth/login")
            .then()
            .statusCode(401)
            .body("error", equalTo("INVALID_CREDENTIALS"))
            .body("message", equalTo("Invalid email or password"));
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /v1/auth/login - Suspended account")
    @Description("Tests login with suspended account returns 403")
    @Story("User Authentication")
    void testUserLoginSuspendedAccount() {
        // Suspend the user
        jdbcTemplate.update(
            "UPDATE users SET status = 'SUSPENDED' WHERE id = ?",
            UUID.fromString(testUserId)
        );
        
        Map<String, Object> loginRequest = Map.of(
            "email", testUserEmail,
            "password", testUserPassword
        );
        
        given()
            .contentType("application/json")
            .body(loginRequest)
            .when()
            .post("/v1/auth/login")
            .then()
            .statusCode(403)
            .body("error", equalTo("ACCOUNT_SUSPENDED"))
            .body("message", equalTo("Account is suspended"));
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /v1/auth/api-key - Successful API key authentication")
    @Description("Tests API key authentication with valid credentials, returns JWT token")
    @Story("API Key Authentication")
    void testApiKeyAuthenticationSuccess() {
        // Create API client
        createTestApiClient();
        
        Map<String, Object> authRequest = Map.of(
            "apiKey", apiKey,
            "apiSecret", apiSecret
        );
        
        Response response = given()
            .contentType("application/json")
            .body(authRequest)
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("clientId", equalTo(apiKey))
            .body("tenantId", equalTo(testTenantId))
            .body("scopes", notNullValue())
            .body("expiresAt", notNullValue())
            .extract()
            .response();
        
        String token = response.path("token");
        assertThat(token).isNotEmpty();
        assertThat(token).startsWith("eyJ");
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /v1/auth/api-key - Revoked client")
    @Description("Tests API key authentication with revoked client returns 403")
    @Story("API Key Authentication")
    void testApiKeyAuthenticationRevokedClient() {
        createTestApiClient();
        
        // Revoke the client
        jdbcTemplate.update(
            "UPDATE api_clients SET status = 'REVOKED' WHERE client_id = ?",
            apiKey
        );
        
        Map<String, Object> authRequest = Map.of(
            "apiKey", apiKey,
            "apiSecret", apiSecret
        );
        
        given()
            .contentType("application/json")
            .body(authRequest)
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(403)
            .body("error", equalTo("CLIENT_REVOKED"))
            .body("message", equalTo("API client has been revoked"));
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /v1/auth/switch-tenant - Successful tenant switch")
    @Description("Tests switching tenant context for a user with multiple tenant access")
    @Story("Tenant Switching")
    void testSwitchTenantSuccess() {
        // Create second tenant
        String secondTenantId = createTestTenant();
        
        // Assign user to second tenant
        jdbcTemplate.update(
            "INSERT INTO user_tenants (id, user_id, tenant_id, role, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, NOW(), NOW())",
            UUID.randomUUID(),
            UUID.fromString(testUserId),
            UUID.fromString(secondTenantId),
            "CUSTOMER"
        );
        
        Map<String, Object> switchRequest = Map.of(
            "tenantId", secondTenantId
        );
        
        Response response = given()
            .contentType("application/json")
            .header("X-User-Id", testUserId)
            .body(switchRequest)
            .when()
            .post("/v1/auth/switch-tenant")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("tenantId", equalTo(secondTenantId))
            .body("role", equalTo("CUSTOMER"))
            .body("expiresAt", notNullValue())
            .extract()
            .response();
        
        String token = response.path("token");
        assertThat(token).isNotEmpty();
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/auth/me - Get current user context")
    @Description("Tests retrieving current authenticated user's context")
    @Story("User Context")
    void testGetCurrentUserContext() {
        given()
            .header("X-User-Id", testUserId)
            .header("X-Tenant-Id", testTenantId)
            .when()
            .get("/v1/auth/me")
            .then()
            .statusCode(200)
            .body("userId", equalTo(testUserId))
            .body("email", equalTo(testUserEmail))
            .body("tenantId", equalTo(testTenantId))
            .body("role", equalTo("TENANT_ADMIN"));
    }
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-auth-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Auth",
            "slug", slug,
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
        
        return response.jsonPath().getString("id");
    }
    
    private void createTestApiClient() {
        apiKey = "test-client-" + UUID.randomUUID().toString().substring(0, 8);
        apiSecret = "sk_" + UUID.randomUUID().toString().replace("-", "");
        
        String secretHash = new BCryptPasswordEncoder(12).encode(apiSecret);
        
        apiClientId = jdbcTemplate.queryForObject(
            "INSERT INTO api_clients (id, tenant_id, client_id, client_name, client_type, auth_method, " +
            "client_secret_hash, allowed_scopes, rate_limit_per_hour, status, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) RETURNING id::text",
            String.class,
            UUID.randomUUID(),
            UUID.fromString(testTenantId),
            apiKey,
            "Test API Client",
            "SERVER",
            "API_KEY",
            secretHash,
            new String[]{"read", "write"},
            1000,
            "ACTIVE"
        );
    }
}
