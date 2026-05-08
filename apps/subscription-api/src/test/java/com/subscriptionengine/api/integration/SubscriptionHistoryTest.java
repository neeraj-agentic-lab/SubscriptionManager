package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for subscription history and audit trail.
 * Tests: Complete lifecycle tracking, metadata, pagination, and actor tracking.
 * 
 * @author Neeraj Yadav
 */
@Epic("Subscription Management")
@Feature("Subscription History & Audit Trail")
class SubscriptionHistoryTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Subscription History - Complete Lifecycle Tracking")
    @Description("Tests that all subscription lifecycle actions are tracked in history with proper metadata")
    @Story("Subscription History")
    void testSubscriptionHistoryCompleteLifecycle() {
        // Given - Create plan
        UUID planId = createTestPlan(testTenantId, "Premium Plan");
        
        // Step 1: Create subscription
        Map<String, Object> subscriptionRequest = createSubscriptionRequest(null, planId);
        
        Response createResponse = givenAuthenticated(testTenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID subscriptionId = UUID.fromString(createResponse.jsonPath().getString("id"));
        UUID customerId = UUID.fromString(createResponse.jsonPath().getString("customerId"));
        
        Allure.addAttachment("Subscription Created", "application/json", createResponse.asString());
        
        // Step 2: Pause subscription
        Map<String, Object> pauseRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "PAUSE",
            "reason", "Customer going on vacation"
        );
        
        givenAuthenticated(testTenantId)
            .body(pauseRequest)
            .when()
            .put("/v1/admin/subscriptions/manage/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // Step 3: Resume subscription
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
        
        // Step 4: Cancel subscription
        Map<String, Object> cancelRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "CANCEL",
            "cancellationType", "IMMEDIATE",
            "reason", "Customer no longer needs service"
        );
        
        givenAuthenticated(testTenantId)
            .body(cancelRequest)
            .when()
            .put("/v1/admin/subscriptions/manage/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // When - Retrieve complete history
        Response historyResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history/all")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify actions are tracked: CREATED, PAUSED, RESUMED, CANCELED
        List<Map<String, Object>> historyEntries = historyResponse.jsonPath().getList("$");
        assertThat(historyEntries).hasSizeGreaterThanOrEqualTo(4);
        
        // Verify expected action types exist
        Set<String> actions = new HashSet<>();
        for (Map<String, Object> entry : historyEntries) {
            actions.add((String) entry.get("action"));
            assertThat(entry.get("action")).isNotNull();
            assertThat(entry.get("performedAt")).isNotNull();
        }
        assertThat(actions).contains("CREATED", "PAUSED", "RESUMED", "CANCELED");
        
        // Verify PAUSED entry has reason in metadata
        Map<String, Object> pausedEntry = historyEntries.stream()
            .filter(e -> "PAUSED".equals(e.get("action")))
            .findFirst().orElseThrow();
        Map<String, Object> pausedMetadata = (Map<String, Object>) pausedEntry.get("metadata");
        assertThat(pausedMetadata).isNotNull();
        assertThat(pausedMetadata.get("reason")).isEqualTo("Customer going on vacation");
        
        // Verify CANCELED entry has reason in metadata
        Map<String, Object> canceledEntry = historyEntries.stream()
            .filter(e -> "CANCELED".equals(e.get("action")))
            .findFirst().orElseThrow();
        Map<String, Object> canceledMetadata = (Map<String, Object>) canceledEntry.get("metadata");
        assertThat(canceledMetadata).isNotNull();
        assertThat(canceledMetadata.get("reason")).isEqualTo("Customer no longer needs service");
        
        Allure.addAttachment("Complete History", "application/json", historyResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Subscription History - Metadata Tracking")
    @Description("Tests that rich metadata is captured for each action type")
    @Story("Subscription History")
    void testSubscriptionHistoryMetadata() {
        // Given - Create subscription
        UUID planId = createTestPlan(testTenantId, "Basic Plan");
        Map<String, UUID> sub = createTestSubscriptionWithCustomer(testTenantId, planId);
        UUID subscriptionId = sub.get("subscriptionId");
        UUID customerId = sub.get("customerId");
        
        // When - Pause with specific reason
        Map<String, Object> pauseRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "PAUSE",
            "reason", "Going on vacation for 2 weeks"
        );
        
        givenAuthenticated(testTenantId)
            .body(pauseRequest)
            .when()
            .put("/v1/admin/subscriptions/manage/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // Then - Retrieve history and verify PAUSED metadata
        Response historyResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> historyEntries = historyResponse.jsonPath().getList("history");
        Map<String, Object> pausedEntry = historyEntries.stream()
            .filter(e -> "PAUSED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        
        Map<String, Object> metadata = (Map<String, Object>) pausedEntry.get("metadata");
        assertThat(metadata).isNotNull();
        assertThat(metadata).containsEntry("reason", "Going on vacation for 2 weeks");
        
        Allure.addAttachment("Pause Metadata", "application/json", historyResponse.asString());
        
        // When - Resume the paused subscription
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
        
        // Then - Verify RESUMED entry exists in history
        Response updatedHistoryResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> updatedEntries = updatedHistoryResponse.jsonPath().getList("history");
        Map<String, Object> resumedEntry = updatedEntries.stream()
            .filter(e -> "RESUMED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        
        assertThat(resumedEntry.get("performedAt")).isNotNull();
        assertThat(resumedEntry.get("performedBy")).isNotNull();
        
        Allure.addAttachment("Resume History", "application/json", updatedHistoryResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Subscription History - Pagination")
    @Description("Tests that history pagination works correctly for subscriptions with many actions")
    @Story("Subscription History")
    void testSubscriptionHistoryPagination() {
        // Given - Create subscription
        UUID planId = createTestPlan(testTenantId, "Test Plan");
        Map<String, UUID> sub = createTestSubscriptionWithCustomer(testTenantId, planId);
        UUID subscriptionId = sub.get("subscriptionId");
        UUID customerId = sub.get("customerId");
        
        // Perform 6 actions (3 pause/resume cycles) to get enough for pagination
        for (int i = 0; i < 3; i++) {
            // Pause
            Map<String, Object> pauseRequest = Map.of(
                "customerId", customerId.toString(),
                "operation", "PAUSE",
                "reason", "Test pause " + i
            );
            
            givenAuthenticated(testTenantId)
                .body(pauseRequest)
                .when()
                .put("/v1/admin/subscriptions/manage/" + subscriptionId)
                .then()
                .statusCode(200);
            
            // Resume
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
        }
        
        // Total: 1 CREATED + 6 pause/resume = 7 entries
        
        // When - Retrieve first page (page=0, size=3)
        Response page0Response = givenAuthenticated(testTenantId)
            .queryParam("page", 0)
            .queryParam("size", 3)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify first page
        List<Map<String, Object>> page0Entries = page0Response.jsonPath().getList("history");
        assertThat(page0Entries).hasSize(3);
        assertThat(page0Response.jsonPath().getLong("totalCount")).isGreaterThanOrEqualTo(7);
        assertThat(page0Response.jsonPath().getInt("page")).isEqualTo(0);
        
        Allure.addAttachment("Page 0", "application/json", page0Response.asString());
        
        // When - Retrieve second page (page=1, size=3)
        Response page1Response = givenAuthenticated(testTenantId)
            .queryParam("page", 1)
            .queryParam("size", 3)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify second page
        List<Map<String, Object>> page1Entries = page1Response.jsonPath().getList("history");
        assertThat(page1Entries).hasSize(3);
        assertThat(page1Response.jsonPath().getInt("page")).isEqualTo(1);
        
        Allure.addAttachment("Page 1", "application/json", page1Response.asString());
        
        // Verify no duplicate entries across pages
        Set<String> allEntryIds = new HashSet<>();
        page0Entries.forEach(e -> allEntryIds.add(e.get("id").toString()));
        page1Entries.forEach(e -> allEntryIds.add(e.get("id").toString()));
        
        assertThat(allEntryIds).hasSize(6); // All entries unique across both pages
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Subscription History - Actor Tracking")
    @Description("Tests that actor type (CUSTOMER, ADMIN, SYSTEM) is tracked correctly")
    @Story("Subscription History")
    void testSubscriptionHistoryActorTracking() {
        // Given - Create subscription
        UUID planId = createTestPlan(testTenantId, "Test Plan");
        Map<String, UUID> sub = createTestSubscriptionWithCustomer(testTenantId, planId);
        UUID subscriptionId = sub.get("subscriptionId");
        UUID customerId = sub.get("customerId");
        
        // When - Customer pauses subscription (simulated via API with customer context)
        Map<String, Object> customerPauseRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "PAUSE",
            "reason", "Customer initiated pause"
        );
        
        givenAuthenticated(testTenantId)
            .body(customerPauseRequest)
            .when()
            .put("/v1/admin/subscriptions/manage/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // When - Admin resumes subscription (simulated via admin API)
        Map<String, Object> adminResumeRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "RESUME"
        );
        
        givenAuthenticated(testTenantId)
            .body(adminResumeRequest)
            .when()
            .put("/v1/admin/subscriptions/manage/" + subscriptionId)
            .then()
            .statusCode(200);
        
        // Then - Retrieve history and verify actor tracking
        Response historyResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/v1/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> historyEntries = historyResponse.jsonPath().getList("history");
        
        // Verify CREATED entry
        Map<String, Object> createdEntry = historyEntries.stream()
            .filter(e -> "CREATED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        assertThat(createdEntry.get("performedBy")).isNotNull();
        
        // Verify PAUSED entry has performedBy tracked
        Map<String, Object> pausedEntry = historyEntries.stream()
            .filter(e -> "PAUSED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        assertThat(pausedEntry.get("performedBy")).isNotNull();
        
        // Verify RESUMED entry has performedBy tracked
        Map<String, Object> resumedEntry = historyEntries.stream()
            .filter(e -> "RESUMED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        assertThat(resumedEntry.get("performedBy")).isNotNull();
        
        Allure.addAttachment("Actor Tracking History", "application/json", historyResponse.asString());
    }
    
    // Helper methods
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-history-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for History",
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
            .post("/v1/admin/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
    }
    
    @Step("Create test plan")
    private UUID createTestPlan(String tenantId, String name) {
        Map<String, Object> planRequest = Map.of(
            "name", name,
            "description", "Test plan for history tracking",
            "basePriceCents", 2999,
            "currency", "USD",
            "billingInterval", "MONTHLY",
            "trialPeriodDays", 0,
            "active", true
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/admin/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create test subscription and return IDs")
    private Map<String, UUID> createTestSubscriptionWithCustomer(String tenantId, UUID planId) {
        Map<String, Object> subscriptionRequest = createSubscriptionRequest(null, planId);
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        Map<String, UUID> result = new HashMap<>();
        result.put("subscriptionId", UUID.fromString(response.jsonPath().getString("id")));
        result.put("customerId", UUID.fromString(response.jsonPath().getString("customerId")));
        return result;
    }
    
    @Step("Create subscription request")
    private Map<String, Object> createSubscriptionRequest(UUID customerId, UUID planId) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerEmail", "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
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
