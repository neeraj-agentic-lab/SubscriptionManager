package com.subscriptionengine.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for integration tests.
 * Provides Testcontainers setup with PostgreSQL and REST Assured configuration.
 * 
 * @author Neeraj Yadav
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.profiles.active=test"
)
public abstract class BaseIntegrationTest {
    
    // Use singleton container that survives across all test classes
    protected static final PostgreSQLContainer<?> postgres = PostgresTestContainer.getInstance();
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected RequestSpecification requestSpec;
    
    // Cached super admin token to avoid repeated logins
    private String cachedSuperAdminToken;
    
    // Cached tenant-scoped tokens: tenantId -> JWT token
    private final Map<String, String> cachedTenantTokens = new HashMap<>();
    
    private static final String TENANT_ADMIN_PASSWORD = "TestPassword123!";
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api";  // Match production context path
        
        // Enable detailed logging for debugging
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        requestSpec = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .log(LogDetail.ALL)
            .build();
        
        cachedSuperAdminToken = null;
        cachedTenantTokens.clear();
    }
    
    /**
     * Get base request specification with a real tenant-scoped JWT.
     * Creates a tenant-admin user for the tenant, logs in, and returns a JWT with tenant claims.
     * Tokens are cached per tenantId within a test.
     */
    protected RequestSpecification givenAuthenticated(String tenantId) {
        String jwt = getTenantScopedToken(tenantId);
        return RestAssured.given(requestSpec)
            .header("Authorization", "Bearer " + jwt);
    }
    
    /**
     * Get base request specification with the super admin token.
     * Use this for admin-only endpoints that don't require tenant context
     * (e.g. creating tenants, managing users).
     */
    protected RequestSpecification givenSuperAdmin() {
        String jwt = getSuperAdminToken();
        return RestAssured.given(requestSpec)
            .header("Authorization", "Bearer " + jwt);
    }
    
    /**
     * Get base request specification without authentication.
     */
    protected RequestSpecification given() {
        return RestAssured.given(requestSpec);
    }
    
    /**
     * Login as the bootstrap super admin and return the JWT token.
     */
    protected String getSuperAdminToken() {
        if (cachedSuperAdminToken != null) {
            return cachedSuperAdminToken;
        }
        
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@subscriptionengine.com");
        loginRequest.put("password", "ChangeMe123!");
        
        cachedSuperAdminToken = RestAssured.given(requestSpec)
            .body(loginRequest)
        .when()
            .post("/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("token");
        
        return cachedSuperAdminToken;
    }
    
    /**
     * Get a real tenant-scoped JWT by creating a tenant-admin user, assigning them
     * to the tenant, and logging in. The resulting JWT contains the tenantId claim.
     * Tokens are cached per tenantId within a test.
     */
    protected String getTenantScopedToken(String tenantId) {
        if (cachedTenantTokens.containsKey(tenantId)) {
            return cachedTenantTokens.get(tenantId);
        }
        
        String superAdminToken = getSuperAdminToken();
        String email = "tenant-admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        
        // Create a user via admin API (uses super admin token)
        Map<String, Object> userRequest = new HashMap<>();
        userRequest.put("email", email);
        userRequest.put("password", TENANT_ADMIN_PASSWORD);
        userRequest.put("firstName", "Tenant");
        userRequest.put("lastName", "Admin");
        userRequest.put("role", "TENANT_ADMIN");
        
        String userId = RestAssured.given(requestSpec)
            .header("Authorization", "Bearer " + superAdminToken)
            .body(userRequest)
        .when()
            .post("/v1/admin/users")
        .then()
            .statusCode(201)
            .extract()
            .path("id");
        
        // Assign user to tenant with TENANT_ADMIN role
        Map<String, Object> assignRequest = new HashMap<>();
        assignRequest.put("userId", userId);
        assignRequest.put("tenantId", tenantId);
        assignRequest.put("role", "TENANT_ADMIN");
        
        RestAssured.given(requestSpec)
            .header("Authorization", "Bearer " + superAdminToken)
            .body(assignRequest)
        .when()
            .post("/v1/admin/user-tenants")
        .then()
            .statusCode(201);
        
        // Login as the tenant admin to get a JWT with real tenant claims
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", email);
        loginRequest.put("password", TENANT_ADMIN_PASSWORD);
        
        String token = RestAssured.given(requestSpec)
            .body(loginRequest)
        .when()
            .post("/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("token");
        
        cachedTenantTokens.put(tenantId, token);
        return token;
    }
    
    /**
     * Generate a unique tenant ID for test isolation.
     * Each test class should use this to avoid database state pollution.
     */
    protected String generateUniqueTenantId() {
        return UUID.randomUUID().toString();
    }
}
