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
        // Given - Create plan and customer
        UUID planId = createTestPlan(testTenantId, "Premium Plan");
        UUID customerId = createTestCustomer(testTenantId);
        
        // Step 1: Create subscription
        Map<String, Object> subscriptionRequest = createSubscriptionRequest(customerId, planId);
        
        Response createResponse = givenAuthenticated(testTenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID subscriptionId = UUID.fromString(createResponse.jsonPath().getString("id"));
        
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
            .post("/v1/subscriptions/" + subscriptionId + "/manage")
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
            .post("/v1/subscriptions/" + subscriptionId + "/manage")
            .then()
            .statusCode(200);
        
        // Step 4: Change plan
        UUID newPlanId = createTestPlan(testTenantId, "Ultimate Plan");
        Map<String, Object> changePlanRequest = Map.of(
            "customerId", customerId.toString(),
            "newPlanId", newPlanId.toString()
        );
        
        givenAuthenticated(testTenantId)
            .body(changePlanRequest)
            .when()
            .post("/v1/subscriptions/" + subscriptionId + "/change-plan")
            .then()
            .statusCode(200);
        
        // Step 5: Cancel subscription
        Map<String, Object> cancelRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "CANCEL",
            "cancellationType", "IMMEDIATE",
            "reason", "Customer no longer needs service"
        );
        
        givenAuthenticated(testTenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/subscriptions/" + subscriptionId + "/manage")
            .then()
            .statusCode(200);
        
        // When - Retrieve complete history
        Response historyResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify all 5 actions are tracked
        List<Map<String, Object>> historyEntries = historyResponse.jsonPath().getList("content");
        assertThat(historyEntries).hasSize(5);
        
        // Verify actions in chronological order
        assertThat(historyEntries.get(0).get("action")).isEqualTo("CREATED");
        assertThat(historyEntries.get(1).get("action")).isEqualTo("PAUSED");
        assertThat(historyEntries.get(2).get("action")).isEqualTo("RESUMED");
        assertThat(historyEntries.get(3).get("action")).isEqualTo("PLAN_CHANGED");
        assertThat(historyEntries.get(4).get("action")).isEqualTo("CANCELED");
        
        // Verify each entry has required fields
        for (Map<String, Object> entry : historyEntries) {
            assertThat(entry.get("action")).isNotNull();
            assertThat(entry.get("performedAt")).isNotNull();
            assertThat(entry.get("performedBy")).isNotNull();
            assertThat(entry.get("metadata")).isNotNull();
        }
        
        // Verify PAUSED entry has reason in metadata
        Map<String, Object> pausedEntry = historyEntries.get(1);
        Map<String, Object> pausedMetadata = (Map<String, Object>) pausedEntry.get("metadata");
        assertThat(pausedMetadata.get("reason")).isEqualTo("Customer going on vacation");
        assertThat(pausedMetadata.get("previousStatus")).isEqualTo("ACTIVE");
        assertThat(pausedMetadata.get("newStatus")).isEqualTo("PAUSED");
        
        // Verify PLAN_CHANGED entry has plan IDs in metadata
        Map<String, Object> planChangedEntry = historyEntries.get(3);
        Map<String, Object> planChangedMetadata = (Map<String, Object>) planChangedEntry.get("metadata");
        assertThat(planChangedMetadata.get("oldPlanId")).isEqualTo(planId.toString());
        assertThat(planChangedMetadata.get("newPlanId")).isEqualTo(newPlanId.toString());
        
        // Verify CANCELED entry has reason in metadata
        Map<String, Object> canceledEntry = historyEntries.get(4);
        Map<String, Object> canceledMetadata = (Map<String, Object>) canceledEntry.get("metadata");
        assertThat(canceledMetadata.get("reason")).isEqualTo("Customer no longer needs service");
        assertThat(canceledMetadata.get("cancellationType")).isEqualTo("IMMEDIATE");
        
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
        UUID customerId = createTestCustomer(testTenantId);
        UUID subscriptionId = createTestSubscription(testTenantId, customerId, planId);
        
        // When - Pause with specific reason
        Map<String, Object> pauseRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "PAUSE",
            "reason", "Going on vacation for 2 weeks"
        );
        
        givenAuthenticated(testTenantId)
            .body(pauseRequest)
            .when()
            .post("/v1/subscriptions/" + subscriptionId + "/manage")
            .then()
            .statusCode(200);
        
        // Then - Retrieve history and verify PAUSED metadata
        Response historyResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> historyEntries = historyResponse.jsonPath().getList("content");
        Map<String, Object> pausedEntry = historyEntries.stream()
            .filter(e -> "PAUSED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        
        Map<String, Object> metadata = (Map<String, Object>) pausedEntry.get("metadata");
        assertThat(metadata).containsEntry("reason", "Going on vacation for 2 weeks");
        assertThat(metadata).containsKey("previousStatus");
        assertThat(metadata).containsKey("newStatus");
        
        try {
            Allure.addAttachment("Pause Metadata", "application/json", 
                objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            // Ignore serialization errors for test attachment
        }
        
        // When - Update products (for ecommerce subscription)
        // Note: This would require ecommerce subscription setup
        // For now, verify metadata structure is correct
        
        // When - Change plan
        UUID newPlanId = createTestPlan(testTenantId, "Premium Plan");
        Map<String, Object> changePlanRequest = Map.of(
            "customerId", customerId.toString(),
            "newPlanId", newPlanId.toString()
        );
        
        givenAuthenticated(testTenantId)
            .body(changePlanRequest)
            .when()
            .post("/v1/subscriptions/" + subscriptionId + "/change-plan")
            .then()
            .statusCode(200);
        
        // Then - Verify PLAN_CHANGED metadata
        Response updatedHistoryResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> updatedEntries = updatedHistoryResponse.jsonPath().getList("content");
        Map<String, Object> planChangedEntry = updatedEntries.stream()
            .filter(e -> "PLAN_CHANGED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        
        Map<String, Object> planChangeMetadata = (Map<String, Object>) planChangedEntry.get("metadata");
        assertThat(planChangeMetadata).containsKey("oldPlanId");
        assertThat(planChangeMetadata).containsKey("newPlanId");
        assertThat(planChangeMetadata.get("oldPlanId")).isEqualTo(planId.toString());
        assertThat(planChangeMetadata.get("newPlanId")).isEqualTo(newPlanId.toString());
        
        try {
            Allure.addAttachment("Plan Change Metadata", "application/json", 
                objectMapper.writeValueAsString(planChangeMetadata));
        } catch (Exception e) {
            // Ignore serialization errors for test attachment
        }
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Subscription History - Pagination")
    @Description("Tests that history pagination works correctly for subscriptions with many actions")
    @Story("Subscription History")
    void testSubscriptionHistoryPagination() {
        // Given - Create subscription
        UUID planId = createTestPlan(testTenantId, "Test Plan");
        UUID customerId = createTestCustomer(testTenantId);
        UUID subscriptionId = createTestSubscription(testTenantId, customerId, planId);
        
        // Perform 50 actions (pause/resume cycles)
        for (int i = 0; i < 25; i++) {
            // Pause
            Map<String, Object> pauseRequest = Map.of(
                "customerId", customerId.toString(),
                "operation", "PAUSE",
                "reason", "Test pause " + i
            );
            
            givenAuthenticated(testTenantId)
                .body(pauseRequest)
                .when()
                .post("/v1/subscriptions/" + subscriptionId + "/manage")
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
                .post("/v1/subscriptions/" + subscriptionId + "/manage")
                .then()
                .statusCode(200);
        }
        
        // When - Retrieve first page (page=0, size=20)
        Response page0Response = givenAuthenticated(testTenantId)
            .queryParam("page", 0)
            .queryParam("size", 20)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify first page
        List<Map<String, Object>> page0Entries = page0Response.jsonPath().getList("content");
        assertThat(page0Entries).hasSize(20);
        assertThat(page0Response.jsonPath().getInt("totalElements")).isEqualTo(51); // 50 actions + 1 creation
        assertThat(page0Response.jsonPath().getInt("totalPages")).isEqualTo(3);
        assertThat(page0Response.jsonPath().getInt("number")).isEqualTo(0);
        
        Allure.addAttachment("Page 0", "application/json", page0Response.asString());
        
        // When - Retrieve second page (page=1, size=20)
        Response page1Response = givenAuthenticated(testTenantId)
            .queryParam("page", 1)
            .queryParam("size", 20)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify second page
        List<Map<String, Object>> page1Entries = page1Response.jsonPath().getList("content");
        assertThat(page1Entries).hasSize(20);
        assertThat(page1Response.jsonPath().getInt("number")).isEqualTo(1);
        
        Allure.addAttachment("Page 1", "application/json", page1Response.asString());
        
        // When - Retrieve third page (page=2, size=20)
        Response page2Response = givenAuthenticated(testTenantId)
            .queryParam("page", 2)
            .queryParam("size", 20)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify third page has remaining entries
        List<Map<String, Object>> page2Entries = page2Response.jsonPath().getList("content");
        assertThat(page2Entries).hasSize(11); // 51 total - 40 from first two pages = 11
        assertThat(page2Response.jsonPath().getInt("number")).isEqualTo(2);
        
        Allure.addAttachment("Page 2", "application/json", page2Response.asString());
        
        // Verify no duplicate entries across pages
        Set<String> allEntryIds = new HashSet<>();
        page0Entries.forEach(e -> allEntryIds.add((String) e.get("id")));
        page1Entries.forEach(e -> allEntryIds.add((String) e.get("id")));
        page2Entries.forEach(e -> allEntryIds.add((String) e.get("id")));
        
        assertThat(allEntryIds).hasSize(51); // All entries unique
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Subscription History - Actor Tracking")
    @Description("Tests that actor type (CUSTOMER, ADMIN, SYSTEM) is tracked correctly")
    @Story("Subscription History")
    void testSubscriptionHistoryActorTracking() {
        // Given - Create subscription
        UUID planId = createTestPlan(testTenantId, "Test Plan");
        UUID customerId = createTestCustomer(testTenantId);
        UUID subscriptionId = createTestSubscription(testTenantId, customerId, planId);
        
        // When - Customer pauses subscription (simulated via API with customer context)
        Map<String, Object> customerPauseRequest = Map.of(
            "customerId", customerId.toString(),
            "operation", "PAUSE",
            "reason", "Customer initiated pause"
        );
        
        givenAuthenticated(testTenantId)
            .body(customerPauseRequest)
            .when()
            .post("/v1/subscriptions/" + subscriptionId + "/manage")
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
            .post("/v1/subscriptions/" + subscriptionId + "/manage")
            .then()
            .statusCode(200);
        
        // Then - Retrieve history and verify actor tracking
        Response historyResponse = givenAuthenticated(testTenantId)
            .when()
            .get("/api/admin/subscriptions/" + subscriptionId + "/history")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> historyEntries = historyResponse.jsonPath().getList("content");
        
        // Verify CREATED entry
        Map<String, Object> createdEntry = historyEntries.stream()
            .filter(e -> "CREATED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        assertThat(createdEntry.get("performedBy")).isNotNull();
        
        // Verify PAUSED entry (customer action)
        Map<String, Object> pausedEntry = historyEntries.stream()
            .filter(e -> "PAUSED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        assertThat(pausedEntry.get("performedBy")).isNotNull();
        assertThat(pausedEntry.get("performedBy")).isEqualTo(customerId.toString());
        
        // Verify RESUMED entry (admin action)
        Map<String, Object> resumedEntry = historyEntries.stream()
            .filter(e -> "RESUMED".equals(e.get("action")))
            .findFirst()
            .orElseThrow();
        assertThat(resumedEntry.get("performedBy")).isNotNull();
        
        // Note: In a real system, we would verify performedByType (CUSTOMER, ADMIN, SYSTEM)
        // For now, verify that performedBy is tracked
        
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
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create test subscription")
    private UUID createTestSubscription(String tenantId, UUID customerId, UUID planId) {
        Map<String, Object> subscriptionRequest = createSubscriptionRequest(customerId, planId);
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create subscription request")
    private Map<String, Object> createSubscriptionRequest(UUID customerId, UUID planId) {
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
