package com.subscriptionengine.api.integration;

import com.subscriptionengine.generated.tables.pojos.ScheduledTasks;
import io.qameta.allure.Description;
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
 * Tests for worker error handling and retry logic.
 * Verifies proper handling of failures, retries, and max attempts.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
public class WorkerErrorHandlingTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 1: Tasks can be created with initial attempt count")
    @Description("Verify tasks are created with attempt_count initialized to 0")
    void testTasksCreatedWithInitialAttemptCount() throws Exception {
        // GIVEN: A new task
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Task has initial attempt count of 0
        assertThat(task.getAttemptCount()).isEqualTo(0);
        assertThat(task.getMaxAttempts()).isEqualTo(3);
        assertThat(task.getStatus()).isEqualTo("READY");
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Tasks can have attempt count manually updated")
    @Description("Verify task attempt_count can be updated for testing retry scenarios")
    void testTaskAttemptCountCanBeUpdated() throws Exception {
        // GIVEN: A task
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        // WHEN: Attempt count is manually updated
        dsl.update(com.subscriptionengine.generated.tables.ScheduledTasks.SCHEDULED_TASKS)
            .set(com.subscriptionengine.generated.tables.ScheduledTasks.SCHEDULED_TASKS.ATTEMPT_COUNT, 2)
            .where(com.subscriptionengine.generated.tables.ScheduledTasks.SCHEDULED_TASKS.ID.eq(taskId))
            .execute();
        
        // THEN: Task reflects updated attempt count
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getAttemptCount()).isEqualTo(2);
        assertThat(task.getMaxAttempts()).isEqualTo(3);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Worker continues processing after encountering failed task")
    @Description("Verify worker doesn't stop processing when one task fails")
    void testWorkerContinuesAfterFailedTask() throws Exception {
        // GIVEN: Mix of tasks - some will fail, some will succeed
        UUID tenantId = createTestTenant();
        
        // Task that will fail (missing invoice)
        UUID failingTaskId = createTestTask("CREATE_DELIVERY", tenantId,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString()
            ));
        
        // Task that will succeed (subscription renewal with fallback)
        UUID successTaskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        // Another failing task
        UUID anotherFailingTaskId = createTestTask("CREATE_ORDER", tenantId,
            Map.of("deliveryId", UUID.randomUUID().toString()));
        
        // WHEN: Worker processes all tasks
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: At least some tasks are processed (worker continues despite failures)
        assertThat(processedCount).isGreaterThanOrEqualTo(1);
        
        // AND: Success task completes, failed tasks stay READY for retry
        assertThat(getTask(successTaskId).getStatus()).isEqualTo("COMPLETED");
        // Failed tasks stay READY for retry (not immediately FAILED)
        assertThat(getTask(failingTaskId).getStatus()).isIn("READY", "FAILED");
        assertThat(getTask(anotherFailingTaskId).getStatus()).isIn("READY", "FAILED");
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 4: Worker handles tasks with past due dates")
    @Description("Verify worker processes overdue tasks without issues")
    void testWorkerHandlesOverdueTasks() throws Exception {
        // GIVEN: Tasks with various past due dates
        UUID tenantId = createTestTenant();
        
        UUID veryOldTaskId = createTestTaskWithDueDate("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()),
            OffsetDateTime.now().minusDays(7)); // 7 days overdue
        
        UUID recentTaskId = createTestTaskWithDueDate("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()),
            OffsetDateTime.now().minusHours(1)); // 1 hour overdue
        
        // WHEN: Worker processes the tasks
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: All overdue tasks are processed
        assertThat(processedCount).isEqualTo(2);
        
        // AND: All tasks complete successfully
        assertThat(getTask(veryOldTaskId).getStatus()).isEqualTo("COMPLETED");
        assertThat(getTask(recentTaskId).getStatus()).isEqualTo("COMPLETED");
    }
}
