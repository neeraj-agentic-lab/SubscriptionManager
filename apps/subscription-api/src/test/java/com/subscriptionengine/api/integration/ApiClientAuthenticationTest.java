package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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
    @DisplayName("API Client HMAC Authentication - Valid Signature")
    @Description("Tests API client creation and HMAC signature authentication flow")
    @Story("API Client Authentication")
    void testApiClientHmacAuthentication() {
        // Given - Admin creates API client
        Map<String, Object> apiClientRequest = Map.of(
            "name", "Test API Client",
            "description", "Integration test client",
            "rateLimit", 1000
        );
        
        Response createClientResponse = givenAuthenticated(testTenantId)
            .body(apiClientRequest)
            .when()
            .post("/api/admin/api-clients")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        String clientId = createClientResponse.jsonPath().getString("clientId");
        String clientSecret = createClientResponse.jsonPath().getString("clientSecret");
        
        // Verify client_id and client_secret returned
        assertThat(clientId).isNotNull();
        assertThat(clientSecret).isNotNull();
        assertThat(clientSecret).hasSize(64); // 32 bytes hex encoded
        
        Allure.addAttachment("API Client Created", "application/json", createClientResponse.asString());
        
        // When - Generate HMAC signature
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        String method = "GET";
        String path = "/api/admin/users";
        String body = "";
        
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body;
        String signature = generateHmacSignature(message, clientSecret);
        
        // When - Make authenticated request with valid signature
        Response validResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", signature)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify request succeeds
        assertThat(validResponse.jsonPath().getList("content")).isNotNull();
        
        Allure.addAttachment("Valid Signature Response", "application/json", validResponse.asString());
        
        // When - Attempt request with invalid signature
        String invalidSignature = "invalid_signature_12345";
        
        Response invalidResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", invalidSignature)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", UUID.randomUUID().toString())
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(401)
            .extract()
            .response();
        
        // Then - Verify request rejected
        String errorMessage = invalidResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("signature")
                                 .containsIgnoringCase("invalid");
        
        Allure.addAttachment("Invalid Signature Error", "application/json", invalidResponse.asString());
        
        // When - Attempt request with expired timestamp (5 minutes old)
        String expiredTimestamp = String.valueOf(Instant.now().minusSeconds(301).getEpochSecond());
        String expiredMessage = method + "\n" + path + "\n" + expiredTimestamp + "\n" + nonce + "\n" + body;
        String expiredSignature = generateHmacSignature(expiredMessage, clientSecret);
        
        Response expiredResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", expiredSignature)
            .header("X-Timestamp", expiredTimestamp)
            .header("X-Nonce", UUID.randomUUID().toString())
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(401)
            .extract()
            .response();
        
        // Then - Verify request rejected
        String expiredError = expiredResponse.jsonPath().getString("message");
        assertThat(expiredError).containsIgnoringCase("timestamp")
                                 .containsIgnoringCase("expired");
        
        Allure.addAttachment("Expired Timestamp Error", "application/json", expiredResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Nonce Replay Attack Prevention")
    @Description("Tests that duplicate nonces are rejected to prevent replay attacks")
    @Story("API Client Security")
    void testNonceReplayPrevention() {
        // Given - Create API client
        String clientId = createApiClient(testTenantId);
        String clientSecret = getClientSecret(testTenantId, clientId);
        
        // Given - Generate request with specific nonce
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "test-nonce-" + UUID.randomUUID().toString();
        String method = "GET";
        String path = "/api/admin/users";
        String body = "";
        
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body;
        String signature = generateHmacSignature(message, clientSecret);
        
        // When - Make first request with nonce
        Response firstResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", signature)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify request succeeds
        assertThat(firstResponse.jsonPath().getList("content")).isNotNull();
        
        Allure.addAttachment("First Request Success", "application/json", firstResponse.asString());
        
        // When - Replay exact same request (same nonce, signature, timestamp)
        Response replayResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", signature)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(401)
            .extract()
            .response();
        
        // Then - Verify request rejected (nonce already used)
        String errorMessage = replayResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("nonce")
                                 .containsIgnoringCase("used");
        
        Allure.addAttachment("Replay Attack Rejected", "application/json", replayResponse.asString());
        
        // When - Make new request with different nonce
        String newNonce = "test-nonce-" + UUID.randomUUID().toString();
        String newTimestamp = String.valueOf(Instant.now().getEpochSecond());
        String newMessage = method + "\n" + path + "\n" + newTimestamp + "\n" + newNonce + "\n" + body;
        String newSignature = generateHmacSignature(newMessage, clientSecret);
        
        Response newResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", newSignature)
            .header("X-Timestamp", newTimestamp)
            .header("X-Nonce", newNonce)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify new request succeeds
        assertThat(newResponse.jsonPath().getList("content")).isNotNull();
        
        Allure.addAttachment("New Nonce Success", "application/json", newResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("API Client Secret Rotation")
    @Description("Tests secret rotation invalidates old secrets immediately")
    @Story("API Client Security")
    void testApiClientSecretRotation() {
        // Given - Create API client
        String clientId = createApiClient(testTenantId);
        String oldSecret = getClientSecret(testTenantId, clientId);
        
        // When - Make successful request with old secret
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        String method = "GET";
        String path = "/api/admin/users";
        String body = "";
        
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body;
        String signature = generateHmacSignature(message, oldSecret);
        
        Response oldSecretResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", signature)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(oldSecretResponse.jsonPath().getList("content")).isNotNull();
        
        Allure.addAttachment("Request with Old Secret", "application/json", oldSecretResponse.asString());
        
        // When - Rotate secret
        Response rotateResponse = givenAuthenticated(testTenantId)
            .when()
            .post("/api/admin/api-clients/" + clientId + "/rotate-secret")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String newSecret = rotateResponse.jsonPath().getString("clientSecret");
        
        // Then - Verify new secret returned
        assertThat(newSecret).isNotNull();
        assertThat(newSecret).isNotEqualTo(oldSecret);
        
        Allure.addAttachment("Secret Rotated", "application/json", rotateResponse.asString());
        
        // When - Attempt request with old secret
        String newTimestamp = String.valueOf(Instant.now().getEpochSecond());
        String newNonce = UUID.randomUUID().toString();
        String oldMessage = method + "\n" + path + "\n" + newTimestamp + "\n" + newNonce + "\n" + body;
        String oldSignature = generateHmacSignature(oldMessage, oldSecret);
        
        Response oldSecretFailResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", oldSignature)
            .header("X-Timestamp", newTimestamp)
            .header("X-Nonce", newNonce)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(401)
            .extract()
            .response();
        
        // Then - Verify request rejected
        String errorMessage = oldSecretFailResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("signature")
                                 .containsIgnoringCase("invalid");
        
        Allure.addAttachment("Old Secret Rejected", "application/json", oldSecretFailResponse.asString());
        
        // When - Make request with new secret
        String newMessage = method + "\n" + path + "\n" + newTimestamp + "\n" + newNonce + "\n" + body;
        String newSignature = generateHmacSignature(newMessage, newSecret);
        
        Response newSecretResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", newSignature)
            .header("X-Timestamp", newTimestamp)
            .header("X-Nonce", newNonce)
            .queryParam("page", 0)
            .queryParam("size", 10)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify request succeeds
        assertThat(newSecretResponse.jsonPath().getList("content")).isNotNull();
        
        Allure.addAttachment("New Secret Success", "application/json", newSecretResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("API Client Rate Limiting")
    @Description("Tests that rate limits are enforced per API client")
    @Story("API Client Security")
    void testApiClientRateLimiting() {
        // Given - Create API client with low rate limit (10 req/min for testing)
        Map<String, Object> apiClientRequest = Map.of(
            "name", "Rate Limited Client",
            "description", "Test rate limiting",
            "rateLimit", 10
        );
        
        Response createResponse = givenAuthenticated(testTenantId)
            .body(apiClientRequest)
            .when()
            .post("/api/admin/api-clients")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        String clientId = createResponse.jsonPath().getString("clientId");
        String clientSecret = createResponse.jsonPath().getString("clientSecret");
        
        Allure.addAttachment("Rate Limited Client Created", "application/json", createResponse.asString());
        
        // When - Make 10 successful requests
        for (int i = 0; i < 10; i++) {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String nonce = UUID.randomUUID().toString();
            String method = "GET";
            String path = "/api/admin/users";
            String body = "";
            
            String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body;
            String signature = generateHmacSignature(message, clientSecret);
            
            given()
                .header("X-API-Key", clientId)
                .header("X-Signature", signature)
                .header("X-Timestamp", timestamp)
                .header("X-Nonce", nonce)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/admin/users")
                .then()
                .statusCode(200);
        }
        
        // When - Make 11th request (should be rate limited)
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = UUID.randomUUID().toString();
        String method = "GET";
        String path = "/api/admin/users";
        String body = "";
        
        String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body;
        String signature = generateHmacSignature(message, clientSecret);
        
        Response rateLimitedResponse = given()
            .header("X-API-Key", clientId)
            .header("X-Signature", signature)
            .header("X-Timestamp", timestamp)
            .header("X-Nonce", nonce)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(429)
            .extract()
            .response();
        
        // Then - Verify 429 Too Many Requests
        String errorMessage = rateLimitedResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("rate")
                                 .containsIgnoringCase("limit");
        
        Allure.addAttachment("Rate Limit Exceeded", "application/json", rateLimitedResponse.asString());
        
        // Note: Testing rate limit reset would require waiting 1 minute
        // For integration tests, we verify the rate limit is enforced
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
    
    @Step("Create API client")
    private String createApiClient(String tenantId) {
        Map<String, Object> apiClientRequest = Map.of(
            "name", "Test API Client",
            "description", "Integration test client",
            "rateLimit", 1000
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(apiClientRequest)
            .when()
            .post("/api/admin/api-clients")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return response.jsonPath().getString("clientId");
    }
    
    @Step("Get client secret")
    private String getClientSecret(String tenantId, String clientId) {
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/api/admin/api-clients/" + clientId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return response.jsonPath().getString("clientSecret");
    }
    
    @Step("Generate HMAC-SHA256 signature")
    private String generateHmacSignature(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC signature", e);
        }
    }
}
