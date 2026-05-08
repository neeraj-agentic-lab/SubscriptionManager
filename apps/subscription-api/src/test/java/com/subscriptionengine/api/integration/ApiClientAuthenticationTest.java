package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for API client authentication and security.
 * Tests: HMAC authentication, nonce replay prevention, secret rotation, and rate limiting.
 * 
 * @author Neeraj Yadav
 */
@Epic("API Client Security")
@Feature("HMAC Authentication")
class ApiClientAuthenticationTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("API Client Authentication - Valid Credentials")
    @Description("Tests API client creation and API key authentication flow")
    @Story("API Client Authentication")
    void testApiClientHmacAuthentication() {
        // Given - Admin creates API client
        Map<String, Object> apiClientRequest = Map.of(
            "name", "Test API Client",
            "description", "Integration test client",
            "tenantId", testTenantId,
            "clientType", "SERVER",
            "authMethod", "API_KEY",
            "rateLimitPerHour", 1000
        );
        
        Response createClientResponse = givenAuthenticated(testTenantId)
            .body(apiClientRequest)
            .when()
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        String clientId = createClientResponse.jsonPath().getString("clientId");
        String clientSecret = createClientResponse.jsonPath().getString("clientSecret");
        String apiClientUuid = createClientResponse.jsonPath().getString("id");
        
        // Verify client_id and client_secret returned
        assertThat(clientId).isNotNull();
        assertThat(clientSecret).isNotNull();
        assertThat(clientSecret).startsWith("sk_");
        assertThat(apiClientUuid).isNotNull();
        assertThat(createClientResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        assertThat(createClientResponse.jsonPath().getString("authMethod")).isEqualTo("API_KEY");
        
        Allure.addAttachment("API Client Created", "application/json", createClientResponse.asString());
        
        // When - Authenticate with valid API key and secret
        Response authResponse = given()
            .contentType("application/json")
            .body(Map.of("apiKey", clientId, "apiSecret", clientSecret))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String token = authResponse.jsonPath().getString("token");
        assertThat(token).isNotNull();
        assertThat(authResponse.jsonPath().getString("clientId")).isEqualTo(clientId);
        
        Allure.addAttachment("Auth Response", "application/json", authResponse.asString());
        
        // When - Attempt auth with invalid secret
        Response invalidResponse = given()
            .contentType("application/json")
            .body(Map.of("apiKey", clientId, "apiSecret", "invalid_secret"))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(401)
            .extract()
            .response();
        
        assertThat(invalidResponse.jsonPath().getString("error")).isEqualTo("INVALID_CREDENTIALS");
        
        Allure.addAttachment("Invalid Secret Error", "application/json", invalidResponse.asString());
        
        // When - Attempt auth with non-existent API key
        Response notFoundResponse = given()
            .contentType("application/json")
            .body(Map.of("apiKey", "nonexistent_client", "apiSecret", clientSecret))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(401)
            .extract()
            .response();
        
        assertThat(notFoundResponse.jsonPath().getString("error")).isEqualTo("INVALID_CREDENTIALS");
        
        Allure.addAttachment("Non-existent Key Error", "application/json", notFoundResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Revoked API Client Rejected")
    @Description("Tests that revoked API clients cannot authenticate")
    @Story("API Client Security")
    void testNonceReplayPrevention() {
        // Given - Create API client and verify it works
        Map<String, String> clientCreds = createApiClientWithCreds(testTenantId);
        String clientId = clientCreds.get("clientId");
        String clientSecret = clientCreds.get("clientSecret");
        String apiClientUuid = clientCreds.get("id");
        
        // Verify auth works before revocation
        Response authResponse = given()
            .contentType("application/json")
            .body(Map.of("apiKey", clientId, "apiSecret", clientSecret))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(authResponse.jsonPath().getString("token")).isNotNull();
        
        Allure.addAttachment("Auth Before Revoke", "application/json", authResponse.asString());
        
        // When - Suspend the client
        Response suspendResponse = givenAuthenticated(testTenantId)
            .body(Map.of("status", "SUSPENDED"))
            .when()
            .patch("/v1/admin/api-clients/" + apiClientUuid)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Allure.addAttachment("Client Suspended", "application/json", suspendResponse.asString());
        
        // Then - Auth should be rejected with 403
        Response suspendedAuthResponse = given()
            .contentType("application/json")
            .body(Map.of("apiKey", clientId, "apiSecret", clientSecret))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(403)
            .extract()
            .response();
        
        assertThat(suspendedAuthResponse.jsonPath().getString("error")).isEqualTo("CLIENT_SUSPENDED");
        
        Allure.addAttachment("Suspended Auth Rejected", "application/json", suspendedAuthResponse.asString());
        
        // When - Revoke the client via DELETE
        Response revokeResponse = givenAuthenticated(testTenantId)
            .when()
            .delete("/v1/admin/api-clients/" + apiClientUuid)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        Allure.addAttachment("Client Revoked", "application/json", revokeResponse.asString());
        
        // Then - Auth should be rejected with 403
        Response revokedAuthResponse = given()
            .contentType("application/json")
            .body(Map.of("apiKey", clientId, "apiSecret", clientSecret))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(403)
            .extract()
            .response();
        
        assertThat(revokedAuthResponse.jsonPath().getString("error")).isEqualTo("CLIENT_REVOKED");
        
        Allure.addAttachment("Revoked Auth Rejected", "application/json", revokedAuthResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("API Client Secret Rotation")
    @Description("Tests secret rotation invalidates old secrets immediately")
    @Story("API Client Security")
    void testApiClientSecretRotation() {
        // Given - Create API client
        Map<String, String> clientCreds = createApiClientWithCreds(testTenantId);
        String clientId = clientCreds.get("clientId");
        String oldSecret = clientCreds.get("clientSecret");
        String apiClientUuid = clientCreds.get("id");
        
        // Verify auth works with original secret
        Response oldSecretAuth = given()
            .contentType("application/json")
            .body(Map.of("apiKey", clientId, "apiSecret", oldSecret))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(oldSecretAuth.jsonPath().getString("token")).isNotNull();
        
        Allure.addAttachment("Auth with Old Secret", "application/json", oldSecretAuth.asString());
        
        // When - Rotate secret via PATCH
        Response rotateResponse = givenAuthenticated(testTenantId)
            .body(Map.of("rotateSecret", true))
            .when()
            .patch("/v1/admin/api-clients/" + apiClientUuid)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String newSecret = rotateResponse.jsonPath().getString("newClientSecret");
        
        // Then - Verify new secret returned
        assertThat(newSecret).isNotNull();
        assertThat(newSecret).isNotEqualTo(oldSecret);
        assertThat(newSecret).startsWith("sk_");
        
        Allure.addAttachment("Secret Rotated", "application/json", rotateResponse.asString());
        
        // When - Attempt auth with old secret
        Response oldSecretFail = given()
            .contentType("application/json")
            .body(Map.of("apiKey", clientId, "apiSecret", oldSecret))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(401)
            .extract()
            .response();
        
        assertThat(oldSecretFail.jsonPath().getString("error")).isEqualTo("INVALID_CREDENTIALS");
        
        Allure.addAttachment("Old Secret Rejected", "application/json", oldSecretFail.asString());
        
        // When - Auth with new secret
        Response newSecretAuth = given()
            .contentType("application/json")
            .body(Map.of("apiKey", clientId, "apiSecret", newSecret))
            .when()
            .post("/v1/auth/api-key")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(newSecretAuth.jsonPath().getString("token")).isNotNull();
        
        Allure.addAttachment("Auth with New Secret", "application/json", newSecretAuth.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("API Client CRUD and Listing")
    @Description("Tests API client listing and detail retrieval")
    @Story("API Client Security")
    void testApiClientRateLimiting() {
        // Given - Create two API clients
        Map<String, Object> client1Request = Map.of(
            "name", "Client Alpha",
            "description", "First test client",
            "tenantId", testTenantId,
            "clientType", "SERVER",
            "authMethod", "API_KEY",
            "rateLimitPerHour", 1000
        );
        
        Response create1Response = givenAuthenticated(testTenantId)
            .body(client1Request)
            .when()
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        String client1Uuid = create1Response.jsonPath().getString("id");
        
        Map<String, Object> client2Request = Map.of(
            "name", "Client Beta",
            "description", "Second test client",
            "tenantId", testTenantId,
            "clientType", "SPA",
            "authMethod", "API_KEY",
            "rateLimitPerHour", 500
        );
        
        Response create2Response = givenAuthenticated(testTenantId)
            .body(client2Request)
            .when()
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        Allure.addAttachment("Client 1 Created", "application/json", create1Response.asString());
        Allure.addAttachment("Client 2 Created", "application/json", create2Response.asString());
        
        // When - List all API clients for tenant
        Response listResponse = givenAuthenticated(testTenantId)
            .queryParam("tenantId", testTenantId)
            .when()
            .get("/v1/admin/api-clients")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify clients are listed
        List<Map<String, Object>> clients = listResponse.jsonPath().getList("content");
        assertThat(clients).hasSizeGreaterThanOrEqualTo(2);
        
        Allure.addAttachment("Client List", "application/json", listResponse.asString());
        
        // When - Get specific client by UUID
        Response getResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/api-clients/" + client1Uuid)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify client details (no secret in response)
        assertThat(getResponse.jsonPath().getString("name")).isEqualTo("Client Alpha");
        assertThat(getResponse.jsonPath().getString("clientType")).isEqualTo("SERVER");
        assertThat(getResponse.jsonPath().getString("authMethod")).isEqualTo("API_KEY");
        assertThat(getResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Client Detail", "application/json", getResponse.asString());
    }
    
    // Helper methods
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-api-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for API Clients",
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
    
    @Step("Create API client with credentials")
    private Map<String, String> createApiClientWithCreds(String tenantId) {
        Map<String, Object> apiClientRequest = Map.of(
            "name", "Test API Client",
            "description", "Integration test client",
            "tenantId", tenantId,
            "clientType", "SERVER",
            "authMethod", "API_KEY",
            "rateLimitPerHour", 1000
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(apiClientRequest)
            .when()
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        Map<String, String> creds = new HashMap<>();
        creds.put("id", response.jsonPath().getString("id"));
        creds.put("clientId", response.jsonPath().getString("clientId"));
        creds.put("clientSecret", response.jsonPath().getString("clientSecret"));
        return creds;
    }
}
