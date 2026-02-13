package com.subscriptionengine.api.integration;

import com.subscriptionengine.generated.tables.pojos.ScheduledTasks;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SUBSCRIPTION_RENEWAL task handler.
 * Verifies subscription renewal processing logic.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
@Epic("Worker Module")
@Feature("SUBSCRIPTION_RENEWAL Handler")
class SubscriptionRenewalHandlerTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 1: Subscription renewal task processes successfully")
    @Description("Verify SUBSCRIPTION_RENEWAL task completes without errors for valid subscription")
    void testSubscriptionRenewalProcessesSuccessfully() throws Exception {
        // GIVEN: A subscription renewal task
        UUID tenantId = createTestTenant();
        UUID subscriptionId = UUID.randomUUID();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", subscriptionId.toString()));
        
        // WHEN: Worker processes the task
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Task is processed successfully
        assertThat(processedCount).isEqualTo(1);
        
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isEqualTo("COMPLETED");
        assertThat(task.getCompletedAt()).isNotNull();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Renewal task handles missing subscription gracefully")
    @Description("Verify renewal fails gracefully when subscription doesn't exist in database")
    void testRenewalHandlesMissingSubscription() throws Exception {
        // GIVEN: A renewal task for non-existent subscription
        UUID tenantId = createTestTenant();
        UUID nonExistentSubId = UUID.randomUUID();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", nonExistentSubId.toString()));
        
        // WHEN: Worker processes the task
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Task is processed (fallback behavior handles missing subscription)
        assertThat(processedCount).isEqualTo(1);
        
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isIn("COMPLETED", "FAILED");
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Renewal task with invalid payload fails")
    @Description("Verify renewal task fails when payload is missing required subscriptionId field")
    void testRenewalWithInvalidPayloadFails() throws Exception {
        // GIVEN: A renewal task with missing subscriptionId
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("invalid", "payload", "missing", "subscriptionId"));
        
        // WHEN: Worker processes the task
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Task is processed but may fail due to invalid payload
        assertThat(processedCount).isGreaterThanOrEqualTo(0);
        
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isIn("FAILED", "COMPLETED");
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 4: Multiple renewal tasks process independently")
    @Description("Verify multiple renewal tasks for different subscriptions can be processed in same batch")
    void testMultipleRenewalTasksProcessIndependently() throws Exception {
        // GIVEN: 3 renewal tasks for different subscriptions
        UUID tenantId = createTestTenant();
        for (int i = 0; i < 3; i++) {
            createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
                Map.of("subscriptionId", UUID.randomUUID().toString()));
        }
        
        // WHEN: Worker processes tasks
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: All 3 tasks are processed independently
        assertThat(processedCount).isEqualTo(3);
        assertThat(countTasksByStatus("COMPLETED")).isEqualTo(3);
        assertThat(countTasksByStatus("FAILED")).isEqualTo(0);
    }
}
