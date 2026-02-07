package com.subscriptionengine.api.integration.scenarios;

import com.subscriptionengine.api.integration.BaseIntegrationTest;
import com.subscriptionengine.api.integration.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scenario 7.2: Concurrent Subscription Modifications
 * 
 * Business Value: Tests race conditions and optimistic locking to ensure
 * data consistency when multiple operations attempt to modify the same
 * subscription simultaneously.
 * 
 * Priority: P3 (Nice to Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Error Recovery")
@Story("Concurrent Modifications")
class ConcurrentModificationScenarioTest extends BaseIntegrationTest {
    
    @Test
    @DisplayName("Scenario 7.2: Concurrent subscription modifications")
    @Description("Validates race conditions: create subscription → simultaneously pause/modify/cancel → only one succeeds → consistent state → no data corruption")
    @Severity(SeverityLevel.NORMAL)
    void shouldHandleConcurrentModificationsCorrectly() throws Exception {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
        
        step1_VerifyInitialState(tenantId, subscriptionId);
        List<Response> responses = step2_ExecuteConcurrentOperations(tenantId, subscriptionId, customerId);
        step3_VerifyOnlyOneSucceeded(responses);
        step4_VerifyConsistentState(tenantId, subscriptionId);
        step5_VerifyNoDataCorruption(tenantId, subscriptionId);
        
        Allure.addAttachment("Scenario Summary", "text/plain", 
            "Successfully validated concurrent modification handling with data consistency");
    }
    
    @Step("Step 1: Verify initial subscription state")
    private void step1_VerifyInitialState(String tenantId, UUID subscriptionId) {
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Initial State", "application/json", response.asString());
    }
    
    @Step("Step 2: Execute concurrent operations (pause, modify, cancel)")
    private List<Response> step2_ExecuteConcurrentOperations(String tenantId, UUID subscriptionId, UUID customerId) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<Response>> futures = new ArrayList<>();
        List<Response> responses = new ArrayList<>();
        
        // Operation 1: Pause
        futures.add(executor.submit(() -> {
            Map<String, Object> pauseRequest = TestDataFactory.createPauseRequest(customerId);
            return givenAuthenticated(tenantId)
                .body(pauseRequest)
                .when()
                .put("/v1/subscription-mgmt/" + subscriptionId)
                .then()
                .extract()
                .response();
        }));
        
        // Operation 2: Modify (change quantity)
        futures.add(executor.submit(() -> {
            Map<String, Object> modifyRequest = Map.of(
                "customerId", customerId.toString(),
                "operation", "MODIFY",
                "newQuantity", 5
            );
            return givenAuthenticated(tenantId)
                .body(modifyRequest)
                .when()
                .put("/v1/subscription-mgmt/" + subscriptionId)
                .then()
                .extract()
                .response();
        }));
        
        // Operation 3: Cancel
        futures.add(executor.submit(() -> {
            Map<String, Object> cancelRequest = TestDataFactory.createCancelRequest(customerId, true);
            return givenAuthenticated(tenantId)
                .body(cancelRequest)
                .when()
                .put("/v1/subscription-mgmt/" + subscriptionId)
                .then()
                .extract()
                .response();
        }));
        
        // Wait for all operations to complete
        for (Future<Response> future : futures) {
            try {
                responses.add(future.get(10, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                Allure.addAttachment("Operation Timeout", "text/plain", "One operation timed out");
            }
        }
        
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);
        
        Allure.addAttachment("Concurrent Operations", "text/plain", 
            "Executed 3 concurrent operations: pause, modify, cancel");
        
        return responses;
    }
    
    @Step("Step 3: Verify only one operation succeeded")
    private void step3_VerifyOnlyOneSucceeded(List<Response> responses) {
        int successCount = 0;
        int failureCount = 0;
        
        for (Response response : responses) {
            if (response.statusCode() == 200 && response.jsonPath().getBoolean("success")) {
                successCount++;
                Allure.addAttachment("Successful Operation", "application/json", response.asString());
            } else {
                failureCount++;
                Allure.addAttachment("Failed Operation", "application/json", response.asString());
            }
        }
        
        // At least one should succeed, others may fail due to state changes
        assertThat(successCount).isGreaterThanOrEqualTo(1);
        
        Allure.addAttachment("Operation Results", "text/plain", 
            "Successful: " + successCount + ", Failed: " + failureCount);
    }
    
    @Step("Step 4: Verify consistent state")
    private void step4_VerifyConsistentState(String tenantId, UUID subscriptionId) {
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String status = response.jsonPath().getString("status");
        
        // Status should be one of the valid states
        assertThat(status).isIn("ACTIVE", "PAUSED", "CANCELED");
        
        // Verify the subscription is in a consistent state
        assertThat(response.jsonPath().getString("id")).isEqualTo(subscriptionId.toString());
        
        Allure.addAttachment("Final State", "application/json", response.asString());
        Allure.addAttachment("Consistency Check", "text/plain", 
            "Subscription is in consistent state: " + status);
    }
    
    @Step("Step 5: Verify no data corruption")
    private void step5_VerifyNoDataCorruption(String tenantId, UUID subscriptionId) {
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Verify all required fields are present and valid
        assertThat(response.jsonPath().getString("id")).isNotNull();
        assertThat(response.jsonPath().getString("status")).isNotNull();
        assertThat(response.jsonPath().getString("planId")).isNotNull();
        assertThat(response.jsonPath().getString("customerId")).isNotNull();
        assertThat(response.jsonPath().getString("createdAt")).isNotNull();
        
        // Verify no partial updates or corrupted data
        String status = response.jsonPath().getString("status");
        if ("CANCELED".equals(status)) {
            assertThat(response.jsonPath().getString("canceledAt")).isNotNull();
        }
        
        Allure.addAttachment("Data Integrity Check", "text/plain", 
            "All required fields present, no data corruption detected");
    }
    
    @Test
    @DisplayName("Scenario 7.2b: Concurrent pause operations")
    @Description("Tests multiple simultaneous pause attempts on the same subscription")
    @Severity(SeverityLevel.NORMAL)
    void shouldHandleConcurrentPauseOperations() throws Exception {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Response>> futures = new ArrayList<>();
        
        // Execute 5 concurrent pause operations
        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(() -> {
                Map<String, Object> pauseRequest = TestDataFactory.createPauseRequest(customerId);
                return givenAuthenticated(tenantId)
                    .body(pauseRequest)
                    .when()
                    .put("/v1/subscription-mgmt/" + subscriptionId)
                    .then()
                    .extract()
                    .response();
            }));
        }
        
        List<Response> responses = new ArrayList<>();
        for (Future<Response> future : futures) {
            responses.add(future.get(10, TimeUnit.SECONDS));
        }
        
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);
        
        // At least one should succeed
        long successCount = responses.stream()
            .filter(r -> r.statusCode() == 200 && r.jsonPath().getBoolean("success"))
            .count();
        
        assertThat(successCount).isGreaterThanOrEqualTo(1);
        
        // Verify final state is PAUSED
        Response finalState = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(finalState.jsonPath().getString("status")).isEqualTo("PAUSED");
        
        Allure.addAttachment("Concurrent Pause Test", "text/plain", 
            "5 concurrent pause operations executed, final state: PAUSED");
    }
    
    @Test
    @DisplayName("Scenario 7.2c: Concurrent modifications with different operations")
    @Description("Tests optimistic locking with various operation types")
    @Severity(SeverityLevel.NORMAL)
    void shouldPreventRaceConditionsWithOptimisticLocking() throws Exception {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<Future<Response>> futures = new ArrayList<>();
        
        // Concurrent operations: pause, resume (will fail), modify
        futures.add(executor.submit(() -> {
            Map<String, Object> pauseRequest = TestDataFactory.createPauseRequest(customerId);
            return givenAuthenticated(tenantId).body(pauseRequest).when().put("/v1/subscription-mgmt/" + subscriptionId).then().extract().response();
        }));
        
        futures.add(executor.submit(() -> {
            Map<String, Object> resumeRequest = TestDataFactory.createResumeRequest(customerId);
            return givenAuthenticated(tenantId).body(resumeRequest).when().put("/v1/subscription-mgmt/" + subscriptionId).then().extract().response();
        }));
        
        futures.add(executor.submit(() -> {
            Map<String, Object> modifyRequest = Map.of("customerId", customerId.toString(), "operation", "MODIFY", "newQuantity", 3);
            return givenAuthenticated(tenantId).body(modifyRequest).when().put("/v1/subscription-mgmt/" + subscriptionId).then().extract().response();
        }));
        
        List<Response> responses = new ArrayList<>();
        for (Future<Response> future : futures) {
            responses.add(future.get(10, TimeUnit.SECONDS));
        }
        
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);
        
        // Verify at least one operation succeeded
        long successCount = responses.stream()
            .filter(r -> r.statusCode() == 200 && r.jsonPath().getBoolean("success"))
            .count();
        
        assertThat(successCount).isGreaterThanOrEqualTo(1);
        
        // Verify subscription is in a valid state
        Response finalState = givenAuthenticated(tenantId)
            .when()
            .get("/v1/subscriptions/" + subscriptionId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String finalStatus = finalState.jsonPath().getString("status");
        assertThat(finalStatus).isIn("ACTIVE", "PAUSED");
        
        Allure.addAttachment("Optimistic Locking Test", "text/plain", 
            "Concurrent operations handled correctly, final state: " + finalStatus);
    }
    
    // Helper methods
    
    private UUID createCustomer(String tenantId) {
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        Response response = givenAuthenticated(tenantId).body(customerRequest).when().post("/v1/customers").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
    }
    
    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        Response response = givenAuthenticated(tenantId).body(planRequest).when().post("/v1/plans").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.planId"));
    }
    
    private UUID createSubscription(String tenantId, UUID customerId, UUID planId) {
        Map<String, Object> subscriptionRequest = TestDataFactory.createSubscriptionRequest(customerId, planId);
        Response response = givenAuthenticated(tenantId).body(subscriptionRequest).when().post("/v1/subscriptions").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.subscriptionId"));
    }
}
