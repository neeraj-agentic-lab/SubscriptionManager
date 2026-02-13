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
            .post("/v1/plans")
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
        UUID customerId = createTestCustomer(testTenantId);
        
        Map<String, Object> subscriptionRequest = createEcommerceSubscriptionRequest(customerId, hybridPlanId);
        List<Map<String, Object>> products = new ArrayList<>();
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "name", "Premium Coffee Beans",
            "quantity", 2,
            "priceCents", 500
        ));
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "name", "Coffee Grinder",
            "quantity", 1,
            "priceCents", 300
        ));
        subscriptionRequest.put("products", products);
        
        Response subscriptionResponse = givenAuthenticated(testTenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID subscriptionId = UUID.fromString(subscriptionResponse.jsonPath().getString("id"));
        
        // Verify plan validation passed and pricing calculated correctly
        // Base (1000) + Products (500 + 500 + 300) = 2300
        int expectedTotal = 1000 + (2 * 500) + 300;
        assertThat(subscriptionResponse.jsonPath().getInt("totalPriceCents")).isEqualTo(expectedTotal);
        
        Allure.addAttachment("Subscription Created", "application/json", subscriptionResponse.asString());
        
        // Step 3: Verify CREATED history entry
        Response historyAfterCreate = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> historyEntries = historyAfterCreate.jsonPath().getList("content");
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
            .post("/v1/subscriptions/" + subscriptionId + "/manage")
            .then()
            .statusCode(200);
        
        // Step 5: Verify PAUSED history entry with metadata
        Response historyAfterPause = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> pauseHistoryEntries = historyAfterPause.jsonPath().getList("content");
        Map<String, Object> pausedEntry = pauseHistoryEntries.stream()
            .filter(e -> "PAUSED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        
        Map<String, Object> pausedMetadata = (Map<String, Object>) pausedEntry.get("metadata");
        assertThat(pausedMetadata.get("reason")).isEqualTo("Going on vacation");
        assertThat(pausedMetadata.get("previousStatus")).isEqualTo("ACTIVE");
        assertThat(pausedMetadata.get("newStatus")).isEqualTo("PAUSED");
        
        Allure.addAttachment("History After Pause", "application/json", historyAfterPause.asString());
        
        // Step 6: Admin attempts to change plan to DIGITAL (should fail - has products)
        Map<String, Object> digitalPlanRequest = Map.of(
            "name", "Digital Only Plan",
            "description", "No products allowed",
            "basePriceCents", 1500,
            "currency", "USD",
            "billingInterval", "MONTHLY",
            "trialPeriodDays", 0,
            "active", true,
            "planCategory", "DIGITAL"
        );
        
        Response digitalPlanResponse = givenAuthenticated(testTenantId)
            .body(digitalPlanRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID digitalPlanId = UUID.fromString(digitalPlanResponse.jsonPath().getString("id"));
        
        // Attempt to change to DIGITAL plan (should fail because subscription has products)
        Map<String, Object> changeToDigitalRequest = Map.of(
            "customerId", customerId.toString(),
            "newPlanId", digitalPlanId.toString()
        );
        
        Response changePlanErrorResponse = givenAuthenticated(testTenantId)
            .body(changeToDigitalRequest)
            .when()
            .post("/v1/subscriptions/" + subscriptionId + "/change-plan")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        // Verify plan validation prevented the change
        String errorMessage = changePlanErrorResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("DIGITAL")
                                 .containsIgnoringCase("products");
        
        Allure.addAttachment("Plan Change Rejected", "application/json", changePlanErrorResponse.asString());
        
        // Step 7: Customer removes products
        // Note: This would require a product removal endpoint
        // For now, we'll create a new subscription without products to demonstrate
        
        // Step 8: Create new DIGITAL subscription (without products)
        UUID customerId2 = createTestCustomer(testTenantId);
        Map<String, Object> digitalSubscriptionRequest = createEcommerceSubscriptionRequest(customerId2, digitalPlanId);
        digitalSubscriptionRequest.remove("products"); // No products
        
        Response digitalSubResponse = givenAuthenticated(testTenantId)
            .body(digitalSubscriptionRequest)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID digitalSubId = UUID.fromString(digitalSubResponse.jsonPath().getString("id"));
        
        // Verify DIGITAL subscription created successfully without products
        assertThat(digitalSubResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("DIGITAL Subscription Created", "application/json", digitalSubResponse.asString());
        
        // Step 9: Change DIGITAL subscription to HYBRID plan (should succeed)
        Map<String, Object> changeToHybridRequest = Map.of(
            "customerId", customerId2.toString(),
            "newPlanId", hybridPlanId.toString()
        );
        
        givenAuthenticated(testTenantId)
            .body(changeToHybridRequest)
            .when()
            .post("/v1/subscriptions/" + digitalSubId + "/change-plan")
            .then()
            .statusCode(200);
        
        // Step 10: Verify PLAN_CHANGED history entry
        Response historyAfterPlanChange = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/subscriptions/" + digitalSubId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> planChangeHistoryEntries = historyAfterPlanChange.jsonPath().getList("content");
        Map<String, Object> planChangedEntry = planChangeHistoryEntries.stream()
            .filter(e -> "PLAN_CHANGED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        
        Map<String, Object> planChangeMetadata = (Map<String, Object>) planChangedEntry.get("metadata");
        assertThat(planChangeMetadata.get("oldPlanId")).isEqualTo(digitalPlanId.toString());
        assertThat(planChangeMetadata.get("newPlanId")).isEqualTo(hybridPlanId.toString());
        
        Allure.addAttachment("History After Plan Change", "application/json", historyAfterPlanChange.asString());
        
        // Step 11: Retrieve complete history for first subscription
        Response completeHistory = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history/all")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Verify all actions tracked correctly
        List<Map<String, Object>> allHistoryEntries = completeHistory.jsonPath().getList("$");
        assertThat(allHistoryEntries).hasSizeGreaterThanOrEqualTo(2); // CREATED + PAUSED
        
        // Verify chronological order
        for (int i = 0; i < allHistoryEntries.size() - 1; i++) {
            String timestamp1 = (String) allHistoryEntries.get(i).get("performedAt");
            String timestamp2 = (String) allHistoryEntries.get(i + 1).get("performedAt");
            assertThat(timestamp1).isLessThanOrEqualTo(timestamp2);
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
        
        // Step 1: Create API client (simulated - would use /api/admin/api-clients)
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
        
        Response createUserResponse = givenAuthenticated(testTenantId)
            .body(userRequest)
            .when()
            .post("/api/admin/users")
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
            "role", "MEMBER"
        );
        
        Response assignmentResponse = givenAuthenticated(testTenantId)
            .body(assignmentRequest)
            .when()
            .post("/api/admin/user-tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(assignmentResponse.jsonPath().getString("userId")).isEqualTo(userId.toString());
        assertThat(assignmentResponse.jsonPath().getString("tenantId")).isEqualTo(targetTenantId);
        
        Allure.addAttachment("User Assigned to Tenant", "application/json", assignmentResponse.asString());
        
        // Step 4: External system lists users with pagination
        Response listUsersResponse = givenAuthenticated(testTenantId)
            .queryParam("page", 0)
            .queryParam("size", 20)
            .queryParam("status", "ACTIVE")
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> users = listUsersResponse.jsonPath().getList("content");
        boolean userFound = users.stream()
            .anyMatch(u -> userId.toString().equals(u.get("id")));
        assertThat(userFound).isTrue();
        
        Allure.addAttachment("Users Listed via API", "application/json", listUsersResponse.asString());
        
        // Step 5: External system filters users by role
        Response filterByRoleResponse = givenAuthenticated(testTenantId)
            .queryParam("role", "CUSTOMER")
            .queryParam("page", 0)
            .queryParam("size", 50)
            .when()
            .get("/api/admin/users")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> customerUsers = filterByRoleResponse.jsonPath().getList("content");
        boolean allCustomers = customerUsers.stream()
            .allMatch(u -> "CUSTOMER".equals(u.get("role")));
        assertThat(allCustomers).isTrue();
        
        Allure.addAttachment("Filtered Users", "application/json", filterByRoleResponse.asString());
        
        // Step 6: External system updates user role in tenant
        UUID assignmentId = UUID.fromString(assignmentResponse.jsonPath().getString("id"));
        
        Map<String, Object> updateRoleRequest = Map.of(
            "role", "ADMIN"
        );
        
        Response updateRoleResponse = givenAuthenticated(testTenantId)
            .body(updateRoleRequest)
            .when()
            .patch("/api/admin/user-tenants/" + assignmentId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(updateRoleResponse.jsonPath().getString("role")).isEqualTo("ADMIN");
        
        Allure.addAttachment("Role Updated", "application/json", updateRoleResponse.asString());
        
        // Step 7: Verify all operations would be audited in sensitive_operations_log
        // Note: This would require querying the audit log table
        // For now, verify that operations completed successfully
        
        // Verify user still exists and has correct role
        Response finalUserCheck = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/users/" + userId)
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
    
    @Step("Create test customer")
    private UUID createTestCustomer(String tenantId) {
        String uniqueEmail = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Map<String, Object> customerRequest = Map.of(
            "email", uniqueEmail,
            "name", "Test Customer",
            "externalCustomerRef", "cust_test_" + UUID.randomUUID().toString().substring(0, 8)
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create ecommerce subscription request")
    private Map<String, Object> createEcommerceSubscriptionRequest(UUID customerId, UUID planId) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId.toString());
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
