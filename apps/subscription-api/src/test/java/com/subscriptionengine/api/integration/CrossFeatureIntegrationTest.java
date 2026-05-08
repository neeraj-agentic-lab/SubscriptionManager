package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for cross-feature scenarios.
 * Tests: End-to-end integration of plan validation, subscription creation, and history tracking.
 * 
 * @author Neeraj Yadav
 */
@Epic("Integration Testing")
@Feature("Cross-Feature Integration")
class CrossFeatureIntegrationTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("End-to-End: Plan Validation + Subscription + History")
    @Description("Tests complete integration of plan validation, subscription creation, and history tracking")
    @Story("Cross-Feature Integration")
    void testEndToEndPlanValidationWithHistory() {
        // Step 1: Admin creates HYBRID plan
        Map<String, Object> hybridPlanRequest = Map.of(
            "name", "Hybrid Coffee Subscription",
            "description", "Base subscription with optional add-ons",
            "basePriceCents", 1000, // $10 base
            "currency", "USD",
            "billingInterval", "MONTHLY",
            "trialPeriodDays", 0,
            "active", true,
            "planCategory", "HYBRID"
        );
        
        Response planResponse = givenAuthenticated(testTenantId)
            .body(hybridPlanRequest)
            .when()
            .post("/v1/admin/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID hybridPlanId = UUID.fromString(planResponse.jsonPath().getString("id"));
        
        // Verify plan validation flags
        assertThat(planResponse.jsonPath().getBoolean("requiresProducts")).isFalse();
        assertThat(planResponse.jsonPath().getBoolean("allowsProducts")).isTrue();
        assertThat(planResponse.jsonPath().getBoolean("basePriceRequired")).isTrue();
        
        Allure.addAttachment("HYBRID Plan Created", "application/json", planResponse.asString());
        
        // Step 2: Customer creates subscription with base + products
        String customerEmail = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        UUID customerId = createTestCustomer(testTenantId, customerEmail);
        
        Map<String, Object> subscriptionRequest = createEcommerceSubscriptionRequest(customerEmail, hybridPlanId);
        List<Map<String, Object>> products = new ArrayList<>();
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "productName", "Premium Coffee Beans",
            "quantity", 2,
            "unitPriceCents", 500,
            "currency", "USD",
            "planId", hybridPlanId.toString()
        ));
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "productName", "Coffee Grinder",
            "quantity", 1,
            "unitPriceCents", 300,
            "currency", "USD",
            "planId", hybridPlanId.toString()
        ));
        subscriptionRequest.put("products", products);
        
        Response subscriptionResponse = givenAuthenticated(testTenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID subscriptionId = UUID.fromString(subscriptionResponse.jsonPath().getString("id"));
        
        // Verify subscription created with correct plan
        assertThat(subscriptionResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        assertThat(subscriptionResponse.jsonPath().getString("planId")).isEqualTo(hybridPlanId.toString());
        
        Allure.addAttachment("Subscription Created", "application/json", subscriptionResponse.asString());
        
        // Step 3: Verify CREATED history entry
        Response historyAfterCreate = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> historyEntries = historyAfterCreate.jsonPath().getList("history");
        assertThat(historyEntries).hasSizeGreaterThanOrEqualTo(1);
        
        Map<String, Object> createdEntry = historyEntries.stream()
            .filter(e -> "CREATED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        
        assertThat(createdEntry.get("performedAt")).isNotNull();
        assertThat(createdEntry.get("performedBy")).isNotNull();
        
        Allure.addAttachment("History After Creation", "application/json", historyAfterCreate.asString());
        
        // Step 4: Customer pauses subscription
        Map<String, Object> pauseRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "PAUSE",
            "reason", "Going on vacation"
        );
        
        givenAuthenticated(testTenantId)
            .body(pauseRequest)
            .when()
            .put("/v1/admin/subscriptions/manage/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // Step 5: Verify PAUSED history entry with metadata
        Response historyAfterPause = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> pauseHistoryEntries = historyAfterPause.jsonPath().getList("history");
        Map<String, Object> pausedEntry = pauseHistoryEntries.stream()
            .filter(e -> "PAUSED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        
        Map<String, Object> pausedMetadata = (Map<String, Object>) pausedEntry.get("metadata");
        assertThat(pausedMetadata).isNotNull();
        assertThat(pausedMetadata.get("reason")).isEqualTo("Going on vacation");
        
        Allure.addAttachment("History After Pause", "application/json", historyAfterPause.asString());
        
        // Step 6: Resume the paused subscription
        Map<String, Object> resumeRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "RESUME"
        );
        
        givenAuthenticated(testTenantId)
            .body(resumeRequest)
            .when()
            .put("/v1/admin/subscriptions/manage/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // Step 7: Verify subscription is ACTIVE again
        Response afterResumeResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(afterResumeResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Subscription Resumed", "application/json", afterResumeResponse.asString());
        
        // Step 8: Retrieve complete history for subscription
        Response completeHistory = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history/all")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Verify all actions tracked correctly: CREATED + PAUSED + RESUMED
        List<Map<String, Object>> allHistoryEntries = completeHistory.jsonPath().getList("$");
        assertThat(allHistoryEntries).hasSizeGreaterThanOrEqualTo(3);
        
        // Verify reverse chronological order (newest first, as returned by API)
        for (int i = 0; i < allHistoryEntries.size() - 1; i++) {
            String timestamp1 = (String) allHistoryEntries.get(i).get("performedAt");
            String timestamp2 = (String) allHistoryEntries.get(i + 1).get("performedAt");
            assertThat(timestamp1).isGreaterThanOrEqualTo(timestamp2);
        }
        
        Allure.addAttachment("Complete History", "application/json", completeHistory.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("API Client Integration with User Management")
    @Description("Tests external system integration via API client for user management operations")
    @Story("API Client Integration")
    void testApiClientUserManagement() {
        // Note: This test requires API Client authentication to be implemented
        // For now, we'll test the user management operations that an API client would use
        
        // Step 1: Create API client (simulated - would use /v1/admin/api-clients)
        // For now, use regular authentication to simulate API client operations
        
        // Step 2: External system creates user via API
        String externalUserEmail = "external-hr-" + UUID.randomUUID().toString().substring(0, 8) + "@company.com";
        Map<String, Object> userRequest = Map.of(
            "email", externalUserEmail,
            "password", "GeneratedPassword123!",
            "firstName", "External",
            "lastName", "User",
            "role", "CUSTOMER"
        );
        
        Response createUserResponse = givenSuperAdmin()
            .body(userRequest)
            .when()
            .post("/v1/admin/users")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID userId = UUID.fromString(createUserResponse.jsonPath().getString("id"));
        assertThat(createUserResponse.jsonPath().getString("email")).isEqualTo(externalUserEmail);
        
        Allure.addAttachment("User Created via API", "application/json", createUserResponse.asString());
        
        // Step 3: External system assigns user to tenant
        String targetTenantId = createTestTenant("External Company Tenant");
        
        Map<String, Object> assignmentRequest = Map.of(
            "userId", userId.toString(),
            "tenantId", targetTenantId,
            "role", "TENANT_USER"
        );
        
        Response assignmentResponse = givenSuperAdmin()
            .body(assignmentRequest)
            .when()
            .post("/v1/admin/user-tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(assignmentResponse.jsonPath().getString("userId")).isEqualTo(userId.toString());
        assertThat(assignmentResponse.jsonPath().getString("tenantId")).isEqualTo(targetTenantId);
        
        Allure.addAttachment("User Assigned to Tenant", "application/json", assignmentResponse.asString());
        
        // Step 4: External system lists users with pagination
        Response listUsersResponse = givenSuperAdmin()
            .queryParam("page", 0)
            .queryParam("size", 20)
            .queryParam("status", "ACTIVE")
            .when()
            .get("/v1/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> users = listUsersResponse.jsonPath().getList("users");
        boolean userFound = users.stream()
            .anyMatch(u -> userId.toString().equals(u.get("id")));
        assertThat(userFound).isTrue();
        
        Allure.addAttachment("Users Listed via API", "application/json", listUsersResponse.asString());
        
        // Step 5: External system filters users by role
        Response filterByRoleResponse = givenSuperAdmin()
            .queryParam("role", "CUSTOMER")
            .queryParam("page", 0)
            .queryParam("size", 50)
            .when()
            .get("/v1/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> customerUsers = filterByRoleResponse.jsonPath().getList("users");
        boolean allCustomers = customerUsers.stream()
            .allMatch(u -> "CUSTOMER".equals(u.get("role")));
        assertThat(allCustomers).isTrue();
        
        Allure.addAttachment("Filtered Users", "application/json", filterByRoleResponse.asString());
        
        // Step 6: External system updates user role in tenant
        UUID assignmentId = UUID.fromString(assignmentResponse.jsonPath().getString("id"));
        
        Map<String, Object> updateRoleRequest = Map.of(
            "role", "TENANT_ADMIN"
        );
        
        Response updateRoleResponse = givenSuperAdmin()
            .body(updateRoleRequest)
            .when()
            .patch("/v1/admin/user-tenants/" + assignmentId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(updateRoleResponse.jsonPath().getString("role")).isEqualTo("TENANT_ADMIN");
        
        Allure.addAttachment("Role Updated", "application/json", updateRoleResponse.asString());
        
        // Step 7: Verify all operations would be audited in sensitive_operations_log
        // Note: This would require querying the audit log table
        // For now, verify that operations completed successfully
        
        // Verify user still exists and has correct role
        Response finalUserCheck = givenSuperAdmin()
            .when()
            .get("/v1/admin/users/" + userId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(finalUserCheck.jsonPath().getString("email")).isEqualTo(externalUserEmail);
        assertThat(finalUserCheck.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Final User State", "application/json", finalUserCheck.asString());
    }
    
    // Helper methods
    
    @Step("Create test tenant")
    private String createTestTenant() {
        return createTestTenant("Test Tenant");
    }
    
    @Step("Create test tenant: {name}")
    private String createTestTenant(String name) {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", name,
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
    
    @Step("Create test customer")
    private UUID createTestCustomer(String tenantId, String email) {
        Map<String, Object> customerRequest = Map.of(
            "email", email,
            "name", "Test Customer",
            "externalCustomerRef", "cust_test_" + UUID.randomUUID().toString().substring(0, 8)
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/admin/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
    }
    
    @Step("Create ecommerce subscription request")
    private Map<String, Object> createEcommerceSubscriptionRequest(String customerEmail, UUID planId) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerEmail", customerEmail);
        request.put("planId", planId.toString());
        request.put("paymentMethodRef", "pm_test_" + UUID.randomUUID().toString().substring(0, 8));
        
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("line1", "123 Test St");
        shippingAddress.put("city", "San Francisco");
        shippingAddress.put("state", "CA");
        shippingAddress.put("postalCode", "94102");
        shippingAddress.put("country", "US");
        request.put("shippingAddress", shippingAddress);
        
        return request;
    }
}
