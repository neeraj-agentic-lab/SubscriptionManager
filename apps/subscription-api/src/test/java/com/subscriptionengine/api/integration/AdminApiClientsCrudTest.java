package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AdminApiClientsController.
 * Tests all CRUD operations for API client management.
 * Uses admin APIs only for test data setup.
 * 
 * @author Neeraj Yadav
 */
@Epic("Admin APIs")
@Feature("API Client Management")
public class AdminApiClientsCrudTest extends BaseIntegrationTest {
    
    @Step("Create tenant via admin API")
    private UUID createTenant(String authTenantId) {
        String tenantName = "Test Tenant " + UUID.randomUUID().toString().substring(0, 8);
        String tenantSlug = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", tenantName,
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
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 1: Create API Client with API_KEY auth method")
    @Description("Verify API client creation returns client_id and client_secret (shown only once)")
    void testCreateApiClient() {
        String tenantIdStr = generateUniqueTenantId();
        UUID tenantId = createTenant(tenantIdStr);
        
        Map<String, Object> createRequest = Map.of(
            "tenantId", tenantId.toString(),
            "name", "Test Integration Client",
            "clientType", "SERVER",
            "authMethod", "API_KEY",
            "scopes", List.of("subscriptions:read", "subscriptions:write"),
            "rateLimitPerHour", 5000,
            "description", "Integration test API client"
        );
        
        Response response = givenAuthenticated(tenantIdStr)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .body("clientId", notNullValue())
            .body("clientSecret", notNullValue())
            .body("clientSecret", startsWith("sk_"))
            .body("name", equalTo("Test Integration Client"))
            .body("authMethod", equalTo("API_KEY"))
            .body("status", equalTo("ACTIVE"))
            .body("scopes", hasSize(2))
            .extract().response();
        
        String clientId = response.jsonPath().getString("clientId");
        String clientSecret = response.jsonPath().getString("clientSecret");
        UUID id = UUID.fromString(response.jsonPath().getString("id"));
        
        assertThat(clientId).isNotNull().contains("test_integration_client");
        assertThat(clientSecret).isNotNull().hasSize(67); // sk_ + 64 hex chars
        assertThat(id).isNotNull();
        
        Allure.addAttachment("Create Response", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: List API Clients with pagination")
    @Description("Verify listing API clients returns paginated results without secrets")
    void testListApiClients() {
        String tenantIdStr = generateUniqueTenantId();
        UUID tenantId = createTenant(tenantIdStr);
        
        // Create 3 API clients
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> createRequest = Map.of(
                "tenantId", tenantId.toString(),
                "name", "Client " + i,
                "clientType", "SERVER",
                "authMethod", "API_KEY"
            );
            
            givenAuthenticated(tenantIdStr)
                .contentType(ContentType.JSON)
                .body(createRequest)
                .post("/v1/admin/api-clients")
                .then()
                .statusCode(201);
        }
        
        // List clients
        Response response = givenAuthenticated(tenantIdStr)
            .queryParam("tenantId", tenantId.toString())
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/v1/admin/api-clients")
            .then()
            .statusCode(200)
            .body("content", hasSize(3))
            .body("totalElements", equalTo(3))
            .body("content[0].clientId", notNullValue())
            .body("content[0].name", notNullValue())
            .body("content[0].status", notNullValue())
            .extract().response();
        
        // Verify secrets are NOT included in list response
        List<Map<String, Object>> clients = response.jsonPath().getList("content");
        for (Map<String, Object> client : clients) {
            assertThat(client).doesNotContainKey("clientSecret");
            assertThat(client).doesNotContainKey("clientSecretHash");
        }
        
        Allure.addAttachment("List Response", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Get API Client by ID")
    @Description("Verify fetching API client details by ID returns complete info without secret")
    void testGetApiClientById() {
        String tenantIdStr = generateUniqueTenantId();
        UUID tenantId = createTenant(tenantIdStr);
        
        // Create API client
        Map<String, Object> createRequest = Map.of(
            "tenantId", tenantId.toString(),
            "name", "Get Test Client",
            "clientType", "SERVER",
            "authMethod", "API_KEY",
            "scopes", List.of("plans:read"),
            "allowedIps", List.of("192.168.1.1", "10.0.0.1"),
            "rateLimitPerHour", 2000
        );
        
        Response createResponse = givenAuthenticated(tenantIdStr)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .extract().response();
        
        UUID clientId = UUID.fromString(createResponse.jsonPath().getString("id"));
        
        // Get client by ID
        Response getResponse = givenAuthenticated(tenantIdStr)
            .when()
            .get("/v1/admin/api-clients/" + clientId)
            .then()
            .statusCode(200)
            .body("id", equalTo(clientId.toString()))
            .body("name", equalTo("Get Test Client"))
            .body("clientType", equalTo("SERVER"))
            .body("authMethod", equalTo("API_KEY"))
            .body("scopes", hasSize(1))
            .body("allowedIps", hasSize(2))
            .body("rateLimitPerHour", equalTo(2000))
            .body("status", equalTo("ACTIVE"))
            .extract().response();
        
        // Verify secret is NOT included
        Map<String, Object> client = getResponse.jsonPath().getMap("$");
        assertThat(client).doesNotContainKey("clientSecret");
        assertThat(client).doesNotContainKey("clientSecretHash");
        
        Allure.addAttachment("Get Response", "application/json", getResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 4: Update API Client and Rotate Secret")
    @Description("Verify updating API client settings and rotating secret returns new secret only once")
    void testUpdateApiClientAndRotateSecret() {
        String tenantIdStr = generateUniqueTenantId();
        UUID tenantId = createTenant(tenantIdStr);
        
        // Create API client
        Map<String, Object> createRequest = Map.of(
            "tenantId", tenantId.toString(),
            "name", "Update Test Client",
            "clientType", "SERVER",
            "authMethod", "API_KEY",
            "scopes", List.of("subscriptions:read")
        );
        
        Response createResponse = givenAuthenticated(tenantIdStr)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .extract().response();
        
        UUID clientId = UUID.fromString(createResponse.jsonPath().getString("id"));
        String originalSecret = createResponse.jsonPath().getString("clientSecret");
        
        // Update client with secret rotation
        Map<String, Object> updateRequest = Map.of(
            "status", "SUSPENDED",
            "scopes", List.of("subscriptions:read", "subscriptions:write", "plans:read"),
            "rateLimitPerHour", 10000,
            "rotateSecret", true
        );
        
        Response updateResponse = givenAuthenticated(tenantIdStr)
            .contentType(ContentType.JSON)
            .body(updateRequest)
            .when()
            .patch("/v1/admin/api-clients/" + clientId)
            .then()
            .statusCode(200)
            .body("message", containsString("secret rotated"))
            .body("newClientSecret", notNullValue())
            .body("newClientSecret", startsWith("sk_"))
            .body("client.status", equalTo("SUSPENDED"))
            .body("client.scopes", hasSize(3))
            .body("client.rateLimitPerHour", equalTo(10000))
            .extract().response();
        
        String newSecret = updateResponse.jsonPath().getString("newClientSecret");
        
        // Verify new secret is different from original
        assertThat(newSecret).isNotEqualTo(originalSecret);
        assertThat(newSecret).hasSize(67);
        
        // Verify subsequent GET does not return secret
        givenAuthenticated(tenantIdStr)
            .when()
            .get("/v1/admin/api-clients/" + clientId)
            .then()
            .statusCode(200)
            .body("status", equalTo("SUSPENDED"))
            .body("$", not(hasKey("clientSecret")))
            .body("$", not(hasKey("newClientSecret")));
        
        Allure.addAttachment("Update Response", "application/json", updateResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 5: Delete (Revoke) API Client")
    @Description("Verify revoking API client sets status to REVOKED (soft delete)")
    void testDeleteApiClient() {
        String tenantIdStr = generateUniqueTenantId();
        UUID tenantId = createTenant(tenantIdStr);
        
        // Create API client
        Map<String, Object> createRequest = Map.of(
            "tenantId", tenantId.toString(),
            "name", "Delete Test Client",
            "clientType", "SERVER",
            "authMethod", "API_KEY"
        );
        
        Response createResponse = givenAuthenticated(tenantIdStr)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .extract().response();
        
        UUID clientId = UUID.fromString(createResponse.jsonPath().getString("id"));
        String clientIdString = createResponse.jsonPath().getString("clientId");
        
        // Delete (revoke) client
        Response deleteResponse = givenAuthenticated(tenantIdStr)
            .when()
            .delete("/v1/admin/api-clients/" + clientId)
            .then()
            .statusCode(200)
            .body("message", containsString("revoked successfully"))
            .body("clientId", equalTo(clientIdString))
            .extract().response();
        
        // Verify client status is REVOKED (soft delete)
        givenAuthenticated(tenantIdStr)
            .when()
            .get("/v1/admin/api-clients/" + clientId)
            .then()
            .statusCode(200)
            .body("status", equalTo("REVOKED"));
        
        Allure.addAttachment("Delete Response", "application/json", deleteResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 6: Get Non-Existent API Client Returns 404")
    @Description("Verify fetching non-existent API client returns proper error")
    void testGetNonExistentApiClient() {
        String tenantIdStr = generateUniqueTenantId();
        UUID nonExistentId = UUID.randomUUID();
        
        givenAuthenticated(tenantIdStr)
            .when()
            .get("/v1/admin/api-clients/" + nonExistentId)
            .then()
            .statusCode(404)
            .body("error", equalTo("API_CLIENT_NOT_FOUND"))
            .body("message", containsString("not found"));
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 7: Create API Client with OAUTH auth method")
    @Description("Verify OAuth API client creation includes redirect URIs")
    void testCreateOAuthApiClient() {
        String tenantIdStr = generateUniqueTenantId();
        UUID tenantId = createTenant(tenantIdStr);
        
        Map<String, Object> createRequest = Map.of(
            "tenantId", tenantId.toString(),
            "name", "OAuth Test Client",
            "clientType", "SPA",
            "authMethod", "OAUTH",
            "scopes", List.of("openid", "profile", "email"),
            "redirectUris", List.of("https://example.com/callback", "https://app.example.com/auth")
        );
        
        Response response = givenAuthenticated(tenantIdStr)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .body("clientSecret", notNullValue())
            .body("authMethod", equalTo("OAUTH"))
            .body("status", equalTo("ACTIVE"))
            .extract().response();
        
        assertThat(response.jsonPath().getString("clientSecret")).startsWith("sk_");
        
        Allure.addAttachment("OAuth Client Response", "application/json", response.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 8: Create API Client with MTLS auth method")
    @Description("Verify mTLS API client creation sets status to PENDING_CERTIFICATE")
    void testCreateMtlsApiClient() {
        String tenantIdStr = generateUniqueTenantId();
        UUID tenantId = createTenant(tenantIdStr);
        
        Map<String, Object> createRequest = Map.of(
            "tenantId", tenantId.toString(),
            "name", "mTLS Test Client",
            "clientType", "SERVER",
            "authMethod", "MTLS",
            "scopes", List.of("subscriptions:read", "subscriptions:write")
        );
        
        Response response = givenAuthenticated(tenantIdStr)
            .contentType(ContentType.JSON)
            .body(createRequest)
            .when()
            .post("/v1/admin/api-clients")
            .then()
            .statusCode(201)
            .body("authMethod", equalTo("MTLS"))
            .body("status", equalTo("PENDING_CERTIFICATE"))
            .extract().response();
        
        // mTLS clients don't get a secret (certificate-based auth)
        assertThat(response.jsonPath().getString("clientSecret")).isNull();
        
        Allure.addAttachment("mTLS Client Response", "application/json", response.asString());
    }
}
