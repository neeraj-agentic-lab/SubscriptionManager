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

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for task leasing and locking mechanism.
 * Verifies that distributed locking prevents duplicate task processing.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
@Epic("Worker Module")
@Feature("Task Leasing & Locking")
class TaskLeasingTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        // Clean up tasks after each test for isolation
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 1: Task leasing prevents double processing")
    @Description("Verify that only one worker can lease and process a task, preventing duplicate billing")
    void testTaskLeasingPreventsDoubleProcessing() throws Exception {
        // GIVEN: A ready task in the database
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId, 
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        // WHEN: Two workers try to process simultaneously
        int worker1Count = taskProcessorService.processAvailableTasks();
        int worker2Count = taskProcessorService.processAvailableTasks();
        
        // THEN: Only one worker processes the task (total = 1)
        assertThat(worker1Count + worker2Count).isEqualTo(1);
        
        // AND: Task is marked as completed
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isEqualTo("COMPLETED");
        assertThat(task.getCompletedAt()).isNotNull();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Only due tasks are leased")
    @Description("Verify that tasks with future due dates are not processed until they are due")
    void testOnlyDueTasksAreLeased() throws Exception {
        // GIVEN: One task due now and one task due in future
        UUID tenantId = createTestTenant();
        
        UUID dueTaskId = createTestTaskWithDueDate("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()),
            OffsetDateTime.now().minusMinutes(1)); // Due 1 minute ago
        
        UUID futureTaskId = createTestTaskWithDueDate("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()),
            OffsetDateTime.now().plusHours(1)); // Due in 1 hour
        
        // WHEN: Worker processes tasks
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Only the due task is processed
        assertThat(processedCount).isEqualTo(1);
        
        ScheduledTasks dueTask = getTask(dueTaskId);
        assertThat(dueTask.getStatus()).isEqualTo("COMPLETED");
        
        ScheduledTasks futureTask = getTask(futureTaskId);
        assertThat(futureTask.getStatus()).isEqualTo("READY");
        assertThat(futureTask.getCompletedAt()).isNull();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Batch size limit is respected")
    @Description("Verify that worker processes at most BATCH_SIZE tasks per cycle to prevent overload")
    void testBatchSizeLimitIsRespected() throws Exception {
        // GIVEN: 15 ready tasks (batch size is 10)
        UUID tenantId = createTestTenant();
        for (int i = 0; i < 15; i++) {
            createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
                Map.of("subscriptionId", UUID.randomUUID().toString()));
        }
        
        // WHEN: Worker processes tasks once
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: At most 10 tasks are processed (batch size limit)
        assertThat(processedCount).isLessThanOrEqualTo(10);
        
        // AND: Remaining tasks are still ready for next cycle
        int remainingReady = countTasksByStatus("READY");
        assertThat(remainingReady).isGreaterThanOrEqualTo(5);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 4: Active locks prevent leasing")
    @Description("Verify that tasks with active locks are not processed by other workers")
    void testActiveLocksPreventLeasing() throws Exception {
        // GIVEN: A task with active lock (being processed by another worker)
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("CHARGE_PAYMENT", tenantId,
            Map.of("invoiceId", UUID.randomUUID().toString()));
        
        // Simulate another worker has claimed this task
        setActiveLock(taskId, 5); // Lock for 5 more minutes
        
        String originalLockOwner = getTask(taskId).getLockOwner();
        
        // WHEN: Another worker tries to process
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Task is not processed (still locked)
        assertThat(processedCount).isEqualTo(0);
        
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isEqualTo("CLAIMED");
        assertThat(task.getLockOwner()).isEqualTo(originalLockOwner);
        assertThat(task.getLockedUntil()).isNotNull();
        assertThat(task.getCompletedAt()).isNull();
    }
}
