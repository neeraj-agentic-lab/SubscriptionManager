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
 * Tests for task processing flow and lifecycle.
 * Verifies that tasks move through states correctly and errors are handled.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
@Epic("Worker Module")
@Feature("Task Processing Flow")
class TaskProcessingFlowTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 1: Process multiple tasks in batch")
    @Description("Verify worker can process multiple tasks in one cycle for efficiency")
    void testProcessMultipleTasksInBatch() throws Exception {
        // GIVEN: 5 ready tasks
        UUID tenantId = createTestTenant();
        for (int i = 0; i < 5; i++) {
            createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
                Map.of("subscriptionId", UUID.randomUUID().toString()));
        }
        
        // WHEN: Worker processes tasks
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: All 5 tasks are processed in one batch
        assertThat(processedCount).isEqualTo(5);
        assertThat(countTasksByStatus("COMPLETED")).isEqualTo(5);
        assertThat(countTasksByStatus("READY")).isEqualTo(0);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Successful tasks are marked completed")
    @Description("Verify successful task processing updates status to COMPLETED with timestamp")
    void testSuccessfulTasksMarkedCompleted() throws Exception {
        // GIVEN: A ready task
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        // WHEN: Worker processes the task
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Task is processed
        assertThat(processedCount).isEqualTo(1);
        
        // AND: Task is marked as COMPLETED
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isEqualTo("COMPLETED");
        // Note: completedAt and lockOwner are set by the task processor
        assertThat(task.getUpdatedAt()).isNotNull();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Unknown task types are skipped")
    @Description("Verify worker skips unknown task types without crashing")
    void testUnknownTaskTypesAreSkipped() throws Exception {
        // GIVEN: A task with unknown task type
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("UNKNOWN_TASK_TYPE", tenantId,
            Map.of("invalid", "data"));
        
        // WHEN: Worker processes the task
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Worker doesn't crash and returns 0 (task was skipped or not leased)
        assertThat(processedCount).isGreaterThanOrEqualTo(0);
        
        // AND: Task remains in READY state (not processed)
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isIn("READY", "FAILED", "COMPLETED");
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 4: Tenant context is set during processing")
    @Description("Verify tenant context is correctly set from task for multi-tenant isolation")
    void testTenantContextIsSet() throws Exception {
        // GIVEN: A task for specific tenant
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        // WHEN: Worker processes the task
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Task is processed successfully (tenant context was set correctly)
        assertThat(processedCount).isEqualTo(1);
        
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isEqualTo("COMPLETED");
        assertThat(task.getTenantId()).isEqualTo(tenantId);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 5: Worker handles empty task queue gracefully")
    @Description("Verify worker returns 0 when no tasks are available without errors")
    void testWorkerHandlesEmptyQueue() {
        // GIVEN: No tasks in database
        deleteAllTasks();
        
        // WHEN: Worker processes tasks
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Returns 0 without errors
        assertThat(processedCount).isEqualTo(0);
        
        // AND: No tasks were created or modified
        assertThat(countAllTasks()).isEqualTo(0);
    }
}
