package com.subscriptionengine.api.integration;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Authorization Aspects.
 * Tests AdminAuthorizationAspect and CustomerAuthorizationAspect.
 * 
 * @author Neeraj Yadav
 */
@DisplayName("Authorization Tests")
public class AuthorizationTest extends BaseIntegrationTest {
    
    private String tenantId;
    private String planId;
    private String subscriptionId;
    private String customerId;
    
    @BeforeEach
    void setUpAuthTest() {
        tenantId = generateUniqueTenantId();
        customerId = UUID.randomUUID().toString();
        
        // Create tenant and plan for tests
        createTenant(tenantId);
        planId = createPlan(tenantId);
        subscriptionId = createSubscription(tenantId, planId);
    }
    
    // Admin Authorization Tests
    
    @Test
    @DisplayName("SUPER_ADMIN should access any tenant's resources")
    void testSuperAdminCanAccessAnyTenant() {
        String superAdminJwt = JwtTestHelper.generateTokenWithRole(
            "different-tenant-id", 
            UUID.randomUUID().toString(), 
            "superadmin@example.com", 
            "SUPER_ADMIN", 
            null
        );
        
        given()
            .header("Authorization", "Bearer " + superAdminJwt)
            .queryParam("limit", 10)
        .when()
            .get("/v1/admin/subscriptions")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("TENANT_ADMIN should access their tenant's resources")
    void testTenantAdminCanAccessOwnTenant() {
        String tenantAdminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, 
            UUID.randomUUID().toString(), 
            "admin@example.com", 
            "TENANT_ADMIN", 
            null
        );
        
        given()
            .header("Authorization", "Bearer " + tenantAdminJwt)
            .queryParam("limit", 10)
        .when()
            .get("/v1/admin/subscriptions")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("TENANT_ADMIN should NOT access other tenant's resources")
    void testTenantAdminCannotAccessOtherTenant() {
        String otherTenantId = generateUniqueTenantId();
        String tenantAdminJwt = JwtTestHelper.generateTokenWithRole(
            otherTenantId, 
            UUID.randomUUID().toString(), 
            "admin@example.com", 
            "TENANT_ADMIN", 
            null
        );
        
        // Try to access subscription from different tenant
        given()
            .header("Authorization", "Bearer " + tenantAdminJwt)
            .pathParam("id", subscriptionId)
        .when()
            .get("/v1/admin/subscriptions/{id}")
        .then()
            .statusCode(anyOf(is(403), is(404))); // Forbidden or not found
    }
    
    @Test
    @DisplayName("STAFF should have limited admin access")
    void testStaffCanAccessAdminEndpoints() {
        String staffJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, 
            UUID.randomUUID().toString(), 
            "staff@example.com", 
            "STAFF", 
            null
        );
        
        given()
            .header("Authorization", "Bearer " + staffJwt)
            .queryParam("limit", 10)
        .when()
            .get("/v1/admin/subscriptions")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("CUSTOMER should NOT access admin endpoints")
    void testCustomerCannotAccessAdminEndpoints() {
        String customerJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, 
            UUID.randomUUID().toString(), 
            "customer@example.com", 
            "CUSTOMER", 
            customerId
        );
        
        given()
            .header("Authorization", "Bearer " + customerJwt)
            .queryParam("limit", 10)
        .when()
            .get("/v1/admin/subscriptions")
        .then()
            .statusCode(403); // Forbidden
    }
    
    @Test
    @DisplayName("User without role should be denied access")
    void testUserWithoutRoleDenied() {
        String jwt = JwtTestHelper.generateToken(tenantId);
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("limit", 10)
        .when()
            .get("/v1/admin/subscriptions")
        .then()
            .statusCode(anyOf(is(401), is(403))); // Unauthorized or Forbidden
    }
    
    // Customer Authorization Tests
    
    @Test
    @DisplayName("Customer should access their own subscriptions")
    void testCustomerCanAccessOwnSubscriptions() {
        String customerJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, 
            UUID.randomUUID().toString(), 
            "customer@example.com", 
            "CUSTOMER", 
            customerId
        );
        
        given()
            .header("Authorization", "Bearer " + customerJwt)
            .queryParam("customerId", customerId)
            .queryParam("limit", 20)
        .when()
            .get("/v1/customers/me/subscriptions")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("Customer should NOT access other customer's subscriptions")
    void testCustomerCannotAccessOtherCustomerSubscriptions() {
        String otherCustomerId = UUID.randomUUID().toString();
        String customerJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, 
            UUID.randomUUID().toString(), 
            "customer@example.com", 
            "CUSTOMER", 
            customerId
        );
        
        // Try to access another customer's subscriptions
        given()
            .header("Authorization", "Bearer " + customerJwt)
            .queryParam("customerId", otherCustomerId)
            .queryParam("limit", 20)
        .when()
            .get("/v1/customers/me/subscriptions")
        .then()
            .statusCode(403); // Forbidden
    }
    
    @Test
    @DisplayName("Admin should be able to access customer endpoints for support")
    void testAdminCanAccessCustomerEndpointsForSupport() {
        String adminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, 
            UUID.randomUUID().toString(), 
            "admin@example.com", 
            "TENANT_ADMIN", 
            null
        );
        
        given()
            .header("Authorization", "Bearer " + adminJwt)
            .queryParam("customerId", customerId)
            .queryParam("limit", 20)
        .when()
            .get("/v1/customers/me/subscriptions")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("Customer without customer_id claim should be denied")
    void testCustomerWithoutCustomerIdDenied() {
        String customerJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, 
            UUID.randomUUID().toString(), 
            "customer@example.com", 
            "CUSTOMER", 
            null // No customer_id
        );
        
        given()
            .header("Authorization", "Bearer " + customerJwt)
            .queryParam("customerId", customerId)
            .queryParam("limit", 20)
        .when()
            .get("/v1/customers/me/subscriptions")
        .then()
            .statusCode(anyOf(is(401), is(403))); // Unauthorized or Forbidden
    }
    
    @Test
    @DisplayName("Customer should NOT manage other customer's subscription")
    void testCustomerCannotManageOtherCustomerSubscription() {
        // Create subscription for customer1
        String customer1Id = UUID.randomUUID().toString();
        String subscription1Id = createSubscriptionForCustomer(tenantId, customer1Id, planId);
        
        // Try to pause with customer2's JWT
        String customer2Id = UUID.randomUUID().toString();
        String customer2Jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, 
            UUID.randomUUID().toString(), 
            "customer2@example.com", 
            "CUSTOMER", 
            customer2Id
        );
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", "PAUSE");
        request.put("reason", "Unauthorized attempt");
        
        given()
            .header("Authorization", "Bearer " + customer2Jwt)
            .queryParam("customerId", customer2Id)
            .pathParam("subscriptionId", subscription1Id)
            .body(request)
        .when()
            .patch("/v1/customers/me/subscriptions/{subscriptionId}")
        .then()
            .statusCode(anyOf(is(403), is(404))); // Forbidden or not found
    }
    
    // Helper methods
    
    private void createTenant(String tenantId) {
        String superAdminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, UUID.randomUUID().toString(), "superadmin@example.com", "SUPER_ADMIN", null
        );
        
        Map<String, Object> tenant = new HashMap<>();
        tenant.put("id", tenantId);
        tenant.put("name", "Test Tenant");
        tenant.put("slug", "test-tenant-" + UUID.randomUUID().toString().substring(0, 8));
        
        given()
            .header("Authorization", "Bearer " + superAdminJwt)
            .body(tenant)
        .when()
            .post("/v1/admin/tenants")
        .then()
            .statusCode(anyOf(is(200), is(201)));
    }
    
    private String createPlan(String tenantId) {
        String adminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, UUID.randomUUID().toString(), "admin@example.com", "TENANT_ADMIN", null
        );
        
        Map<String, Object> plan = new HashMap<>();
        plan.put("name", "Test Plan");
        plan.put("description", "Test plan for authorization tests");
        plan.put("basePriceCents", 999);
        plan.put("currency", "USD");
        plan.put("billingInterval", "MONTHLY");
        plan.put("billingIntervalCount", 1);
        plan.put("planCategory", "DIGITAL");
        
        Response response = given()
            .header("Authorization", "Bearer " + adminJwt)
            .body(plan)
        .when()
            .post("/v1/admin/plans")
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract().response();
        
        return response.path("id");
    }
    
    private String createSubscription(String tenantId, String planId) {
        String adminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, UUID.randomUUID().toString(), "admin@example.com", "TENANT_ADMIN", null
        );
        
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("planId", planId);
        subscription.put("customerEmail", "customer@example.com");
        subscription.put("customerFirstName", "Test");
        subscription.put("customerLastName", "Customer");
        subscription.put("paymentMethodRef", "pm_test_123");
        
        Response response = given()
            .header("Authorization", "Bearer " + adminJwt)
            .body(subscription)
        .when()
            .post("/v1/admin/subscriptions")
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract().response();
        
        return response.path("data.subscription.id");
    }
    
    private String createSubscriptionForCustomer(String tenantId, String customerId, String planId) {
        String adminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, UUID.randomUUID().toString(), "admin@example.com", "TENANT_ADMIN", null
        );
        
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("planId", planId);
        subscription.put("customerEmail", "customer-" + customerId + "@example.com");
        subscription.put("customerFirstName", "Test");
        subscription.put("customerLastName", "Customer");
        subscription.put("paymentMethodRef", "pm_test_" + UUID.randomUUID().toString().substring(0, 8));
        
        Response response = given()
            .header("Authorization", "Bearer " + adminJwt)
            .body(subscription)
        .when()
            .post("/v1/admin/subscriptions")
        .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract().response();
        
        return response.path("data.subscription.id");
    }
}
