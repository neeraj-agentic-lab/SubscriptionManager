package com.subscriptionengine.api.integration;

import io.restassured.response.Response;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

/**
 * Integration tests for Customer Self-Service APIs.
 * Tests all customer-facing endpoints under /v1/customers/me/*
 * 
 * @author Neeraj Yadav
 */
@DisplayName("Customer Self-Service API Tests")
public class CustomerSelfServiceTest extends BaseIntegrationTest {
    
    @Autowired
    private DSLContext dsl;
    
    private String tenantId;
    private String customerId;
    private String customerUserId;
    private String customerEmail;
    private String planId;
    private String subscriptionId;
    
    @BeforeEach
    void setUpCustomerTest() {
        tenantId = generateUniqueTenantId();
        customerId = UUID.randomUUID().toString();
        customerUserId = UUID.randomUUID().toString();
        // Use unique email per test to avoid conflicts
        customerEmail = "customer-" + tenantId.substring(0, 8) + "@example.com";
        
        // Create tenant
        createTenant(tenantId);
        
        // Create user record for the customer (required for foreign key constraints)
        createUser(tenantId, customerUserId, customerEmail, "CUSTOMER");
        
        // Create a plan for subscription tests
        planId = createPlan(tenantId);
    }
    
    @Test
    @DisplayName("Should get available plans for customer self-signup")
    void testGetAvailablePlans() {
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
        .when()
            .get("/v1/customers/me/plans")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.plans", notNullValue())
            .body("data.count", greaterThanOrEqualTo(0));
    }
    
    @Test
    @DisplayName("Should create customer subscription via self-signup")
    void testCreateCustomerSubscription() {
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Map<String, Object> request = new HashMap<>();
        request.put("planId", planId);
        request.put("customerEmail", customerEmail);
        request.put("customerFirstName", "John");
        request.put("customerLastName", "Doe");
        request.put("paymentMethodRef", "pm_test_123");
        
        Response response = given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .body(request)
        .when()
            .post("/v1/customers/me/subscriptions")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("status", is("ACTIVE"))
            .extract().response();
        
        subscriptionId = response.path("id");
    }
    
    @Test
    @DisplayName("Should get customer subscriptions")
    void testGetCustomerSubscriptions() {
        // Create a subscription first
        subscriptionId = createSubscriptionForCustomer(tenantId, customerId, planId);
        
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .queryParam("limit", 20)
        .when()
            .get("/v1/customers/me/subscriptions")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.subscriptions", notNullValue())
            .body("data.count", greaterThanOrEqualTo(1))
            .body("data.customerId", is(customerId));
    }
    
    @Test
    @DisplayName("Should get subscription dashboard")
    void testGetSubscriptionDashboard() {
        // Create a subscription first
        subscriptionId = createSubscriptionForCustomer(tenantId, customerId, planId);
        
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .pathParam("subscriptionId", subscriptionId)
        .when()
            .get("/v1/customers/me/subscriptions/{subscriptionId}/dashboard")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data", notNullValue());
    }
    
    @Test
    @DisplayName("Should pause customer subscription")
    void testPauseSubscription() {
        // Create a subscription first
        subscriptionId = createSubscriptionForCustomer(tenantId, customerId, planId);
        
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", "PAUSE");
        request.put("reason", "Going on vacation");
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .pathParam("subscriptionId", subscriptionId)
            .body(request)
        .when()
            .patch("/v1/customers/me/subscriptions/{subscriptionId}")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("Should resume customer subscription")
    void testResumeSubscription() {
        // Create and pause a subscription first
        subscriptionId = createSubscriptionForCustomer(tenantId, customerId, planId);
        pauseSubscription(tenantId, customerId, subscriptionId);
        
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", "RESUME");
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .pathParam("subscriptionId", subscriptionId)
            .body(request)
        .when()
            .patch("/v1/customers/me/subscriptions/{subscriptionId}")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("Should cancel customer subscription")
    void testCancelSubscription() {
        // Create a subscription first
        subscriptionId = createSubscriptionForCustomer(tenantId, customerId, planId);
        
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", "CANCEL");
        request.put("reason", "No longer needed");
        request.put("feedback", "Service was great");
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .pathParam("subscriptionId", subscriptionId)
            .body(request)
        .when()
            .patch("/v1/customers/me/subscriptions/{subscriptionId}")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("Should get customer deliveries")
    void testGetCustomerDeliveries() {
        // Create a subscription first (which creates deliveries)
        subscriptionId = createSubscriptionForCustomer(tenantId, customerId, planId);
        
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .queryParam("limit", 10)
        .when()
            .get("/v1/customers/me/deliveries")
        .then()
            .statusCode(200)
            .body("success", is(true))
            .body("data.deliveries", notNullValue());
    }
    
    @Test
    @DisplayName("Should skip delivery")
    void testSkipDelivery() {
        // Create a subscription and get a delivery
        subscriptionId = createSubscriptionForCustomer(tenantId, customerId, planId);
        String deliveryId = getFirstDeliveryId(tenantId, customerId, subscriptionId);
        
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", "SKIP");
        request.put("reason", "Out of town this week");
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .pathParam("deliveryId", deliveryId)
            .body(request)
        .when()
            .patch("/v1/customers/me/deliveries/{deliveryId}")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("Should reschedule delivery")
    void testRescheduleDelivery() {
        // Create a subscription and get a delivery
        subscriptionId = createSubscriptionForCustomer(tenantId, customerId, planId);
        String deliveryId = getFirstDeliveryId(tenantId, customerId, subscriptionId);
        
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", "RESCHEDULE");
        request.put("newDate", "2026-03-15");
        request.put("reason", "Need delivery on weekend");
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customerId)
            .pathParam("deliveryId", deliveryId)
            .body(request)
        .when()
            .patch("/v1/customers/me/deliveries/{deliveryId}")
        .then()
            .statusCode(200)
            .body("success", is(true));
    }
    
    @Test
    @DisplayName("Should deny access when customer tries to access another customer's subscription")
    void testCustomerCannotAccessOtherCustomerData() {
        // Create subscription for customer1
        String customer1Id = UUID.randomUUID().toString();
        String subscription1Id = createSubscriptionForCustomer(tenantId, customer1Id, planId);
        
        // Try to access with customer2's JWT
        String customer2Id = UUID.randomUUID().toString();
        String customer2UserId = UUID.randomUUID().toString();
        String jwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customer2UserId, "customer2@example.com", "CUSTOMER", customer2Id
        );
        
        given()
            .header("Authorization", "Bearer " + jwt)
            .queryParam("customerId", customer2Id)
            .pathParam("subscriptionId", subscription1Id)
        .when()
            .get("/v1/customers/me/subscriptions/{subscriptionId}/dashboard")
        .then()
            .statusCode(anyOf(is(403), is(404))); // Either forbidden or not found
    }
    
    // Helper methods
    
    private void createTenant(String tenantId) {
        String adminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, UUID.randomUUID().toString(), "admin@example.com", "SUPER_ADMIN", null
        );
        
        Map<String, Object> tenant = new HashMap<>();
        tenant.put("id", tenantId);
        tenant.put("name", "Test Tenant");
        tenant.put("slug", "test-tenant-" + UUID.randomUUID().toString().substring(0, 8));
        
        given()
            .header("Authorization", "Bearer " + adminJwt)
            .body(tenant)
        .when()
            .post("/v1/admin/tenants")
        .then()
            .statusCode(anyOf(is(200), is(201)));
    }
    
    private void createUser(String tenantId, String userId, String email, String role) {
        // Insert user directly into database to avoid API validation issues during test setup
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("TestPassword123!");
        
        dsl.insertInto(table("users"))
            .set(field("id"), UUID.fromString(userId))
            .set(field("email"), email)
            .set(field("full_name"), "Test User")
            .set(field("first_name"), "Test")
            .set(field("last_name"), "User")
            .set(field("password_hash"), hashedPassword)
            .set(field("role"), role)
            .set(field("status"), "ACTIVE")
            .set(field("must_change_password"), false)
            .set(field("failed_login_attempts"), 0)
            .set(field("created_at"), LocalDateTime.now())
            .set(field("updated_at"), LocalDateTime.now())
            .onDuplicateKeyIgnore()
            .execute();
        
        // Also insert into user_tenants to associate user with tenant
        // Note: user_tenants.role uses tenant-level permissions (OWNER, ADMIN, MEMBER, VIEWER)
        // while users.role uses system-level roles (SUPER_ADMIN, TENANT_ADMIN, STAFF, CUSTOMER)
        String tenantRole = role.equals("CUSTOMER") ? "MEMBER" : "ADMIN";
        
        dsl.insertInto(table("user_tenants"))
            .set(field("id"), UUID.randomUUID())
            .set(field("user_id"), UUID.fromString(userId))
            .set(field("tenant_id"), UUID.fromString(tenantId))
            .set(field("role"), tenantRole)
            .set(field("assigned_at"), LocalDateTime.now())
            .set(field("created_at"), LocalDateTime.now())
            .set(field("updated_at"), LocalDateTime.now())
            .onDuplicateKeyIgnore()
            .execute();
    }
    
    private String createPlan(String tenantId) {
        String adminJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, UUID.randomUUID().toString(), "admin@example.com", "TENANT_ADMIN", null
        );
        
        Map<String, Object> plan = new HashMap<>();
        plan.put("name", "Test Plan");
        plan.put("description", "Test plan for customer tests");
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
        
        // Debug: print response body
        System.out.println("=== Plan Creation Response ===");
        System.out.println(response.asString());
        System.out.println("==============================");
        
        String planId = response.path("id");
        if (planId == null) {
            throw new IllegalStateException("Failed to extract plan ID from response: " + response.asString());
        }
        return planId;
    }
    
    private String createSubscriptionForCustomer(String tenantId, String customerId, String planId) {
        // Create customer user with the exact customerId we want
        String customerUserId = UUID.randomUUID().toString();
        String customerEmail = "customer-" + customerId.substring(0, 8) + "@example.com";
        
        createUser(tenantId, customerUserId, customerEmail, "CUSTOMER");
        
        // Create customer record in customers table with the exact customerId
        dsl.insertInto(table("customers"))
            .set(field("id"), UUID.fromString(customerId))
            .set(field("tenant_id"), UUID.fromString(tenantId))
            .set(field("email"), customerEmail)
            .set(field("first_name"), "Test")
            .set(field("last_name"), "Customer")
            .set(field("status"), "ACTIVE")
            .set(field("created_at"), LocalDateTime.now())
            .set(field("updated_at"), LocalDateTime.now())
            .onDuplicateKeyIgnore()
            .execute();
        
        // Use customer self-service endpoint to create subscription
        String customerJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Map<String, Object> subscription = new HashMap<>();
        subscription.put("planId", planId);
        subscription.put("customerEmail", customerEmail);
        subscription.put("customerFirstName", "Test");
        subscription.put("customerLastName", "Customer");
        subscription.put("paymentMethodRef", "pm_test_" + UUID.randomUUID().toString().substring(0, 8));
        
        Response response = given()
            .header("Authorization", "Bearer " + customerJwt)
            .queryParam("customerId", customerId)
            .body(subscription)
        .when()
            .post("/v1/customers/me/subscriptions")
        .then()
            .log().ifValidationFails()
            .statusCode(201)
            .extract().response();
        
        return response.path("id");
    }
    
    private void pauseSubscription(String tenantId, String customerId, String subscriptionId) {
        // Use customer self-service endpoint to pause subscription
        String customerUserId = UUID.randomUUID().toString();
        String customerEmail = "customer-pause-" + customerId.substring(0, 8) + "@example.com";
        
        createUser(tenantId, customerUserId, customerEmail, "CUSTOMER");
        
        String customerJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Map<String, Object> request = new HashMap<>();
        request.put("action", "PAUSE");
        
        given()
            .header("Authorization", "Bearer " + customerJwt)
            .queryParam("customerId", customerId)
            .pathParam("subscriptionId", subscriptionId)
            .body(request)
        .when()
            .patch("/v1/customers/me/subscriptions/{subscriptionId}")
        .then()
            .statusCode(200);
    }
    
    private String getFirstDeliveryId(String tenantId, String customerId, String subscriptionId) {
        // Use customer self-service endpoint to get deliveries
        String customerUserId = UUID.randomUUID().toString();
        String customerEmail = "customer-delivery-" + customerId.substring(0, 8) + "@example.com";
        
        createUser(tenantId, customerUserId, customerEmail, "CUSTOMER");
        
        String customerJwt = JwtTestHelper.generateTokenWithRole(
            tenantId, customerUserId, customerEmail, "CUSTOMER", customerId
        );
        
        Response response = given()
            .header("Authorization", "Bearer " + customerJwt)
            .queryParam("customerId", customerId)
            .queryParam("subscriptionId", subscriptionId)
        .when()
            .get("/v1/customers/me/deliveries")
        .then()
            .statusCode(200)
            .extract().response();
        
        List<Map<String, Object>> deliveries = response.path("data.deliveries");
        if (deliveries != null && !deliveries.isEmpty()) {
            return (String) deliveries.get(0).get("id");
        }
        
        return UUID.randomUUID().toString(); // Fallback
    }
}
