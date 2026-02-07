package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for customer management operations.
 * Tests: Customer creation, retrieval, and validation.
 */
@Epic("Customer Management")
@Feature("Customer Operations")
class CustomerManagementTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @DisplayName("Should create a new customer")
    @Description("Tests POST /v1/customers endpoint")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Customer creation")
    void shouldCreateCustomer() {
        String tenantId = testTenantId;
        String email = "newcustomer-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest(email);
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        assertThat(response.jsonPath().getString("data.customerId")).isNotNull();
        assertThat(response.jsonPath().getString("data.email")).isEqualTo(email);
        
        Allure.addAttachment("Customer Created", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should validate email format on customer creation")
    @Description("Tests email validation")
    @Severity(SeverityLevel.NORMAL)
    @Story("Input validation")
    void shouldValidateEmailFormat() {
        String tenantId = testTenantId;
        
        Map<String, Object> invalidCustomer = Map.of(
            "email", "not-a-valid-email",
            "name", "Test Customer",
            "externalCustomerRef", "cust_test_123"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(invalidCustomer)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.statusCode()).isEqualTo(400);
        
        Allure.addAttachment("Email Validation Error", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should prevent duplicate customer emails within tenant")
    @Description("Tests unique email constraint per tenant")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Data integrity")
    void shouldPreventDuplicateEmails() {
        String tenantId = testTenantId;
        String email = "duplicate-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        
        // Create first customer
        Map<String, Object> customer1 = TestDataFactory.createCustomerRequest(email);
        givenAuthenticated(tenantId)
            .body(customer1)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200);
        
        // Try to create duplicate
        Map<String, Object> customer2 = TestDataFactory.createCustomerRequest(email);
        Response response = givenAuthenticated(tenantId)
            .body(customer2)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(409)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isFalse();
        
        Allure.addAttachment("Duplicate Email Error", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should allow same email across different tenants")
    @Description("Tests that email uniqueness is per-tenant")
    @Severity(SeverityLevel.NORMAL)
    @Story("Multi-tenancy")
    void shouldAllowSameEmailAcrossTenants() {
        String tenant1 = testTenantId;
        String tenant2 = createTestTenant(); // Create a second tenant
        String email = "cross-tenant-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        
        // Create customer in tenant1
        Map<String, Object> customer1 = TestDataFactory.createCustomerRequest(email);
        Response response1 = givenAuthenticated(tenant1)
            .body(customer1)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response1.jsonPath().getBoolean("success")).isTrue();
        
        // Create customer with same email in tenant2 - should succeed
        Map<String, Object> customer2 = TestDataFactory.createCustomerRequest(email);
        Response response2 = givenAuthenticated(tenant2)
            .body(customer2)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response2.jsonPath().getBoolean("success")).isTrue();
        
        Allure.addAttachment("Tenant 1 Customer", "application/json", response1.asString());
        Allure.addAttachment("Tenant 2 Customer", "application/json", response2.asString());
    }
    
    @Test
    @DisplayName("Should create customer with external reference")
    @Description("Tests customer creation with external system reference")
    @Severity(SeverityLevel.NORMAL)
    @Story("Customer creation")
    void shouldCreateCustomerWithExternalRef() {
        String tenantId = testTenantId;
        String externalRef = "stripe_cust_" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        ((Map<String, Object>) customerRequest).put("externalCustomerRef", externalRef);
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        
        Allure.addAttachment("Customer with External Ref", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should require email field")
    @Description("Tests that email is a required field")
    @Severity(SeverityLevel.NORMAL)
    @Story("Input validation")
    void shouldRequireEmailField() {
        String tenantId = testTenantId;
        
        Map<String, Object> invalidCustomer = Map.of(
            "name", "Test Customer"
            // Missing email
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(invalidCustomer)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.statusCode()).isEqualTo(400);
        
        Allure.addAttachment("Missing Email Error", "application/json", response.asString());
    }
    
    // Helper methods
    
    @io.qameta.allure.Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-customers-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Customers",
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
}
