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
 * Tests for reaper functionality - expired lock cleanup.
 * Verifies that stuck tasks are recovered and can be reprocessed.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
@Epic("Worker Module")
@Feature("Reaper - Lock Cleanup")
class ReaperLockCleanupTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 1: Reaper releases expired locks")
    @Description("Verify reaper releases locks that have expired and resets tasks to READY for reprocessing")
    void testReaperReleasesExpiredLocks() throws Exception {
        // GIVEN: A task with expired lock (simulating worker that died 10 minutes ago)
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        setExpiredLock(taskId, 10); // Lock expired 10 minutes ago
        
        // Verify task is in CLAIMED state with expired lock
        ScheduledTasks beforeReaper = getTask(taskId);
        assertThat(beforeReaper.getStatus()).isEqualTo("CLAIMED");
        assertThat(beforeReaper.getLockOwner()).isNotNull();
        
        // WHEN: Reaper runs
        int cleanedUp = taskProcessorService.cleanupExpiredLocks();
        
        // THEN: Lock is released
        assertThat(cleanedUp).isGreaterThanOrEqualTo(1);
        
        // AND: Task is reset to READY state
        ScheduledTasks afterReaper = getTask(taskId);
        assertThat(afterReaper.getStatus()).isEqualTo("READY");
        assertThat(afterReaper.getLockedUntil()).isNull();
        assertThat(afterReaper.getLockOwner()).isNull();
        
        // AND: Task can now be processed by another worker
        int processedCount = taskProcessorService.processAvailableTasks();
        assertThat(processedCount).isEqualTo(1);
        
        ScheduledTasks afterProcessing = getTask(taskId);
        assertThat(afterProcessing.getStatus()).isEqualTo("COMPLETED");
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Reaper does not touch active locks")
    @Description("Verify reaper leaves tasks with active locks untouched to prevent interference")
    void testReaperDoesNotTouchActiveLocks() throws Exception {
        // GIVEN: A task with active lock (being processed by another worker)
        UUID tenantId = createTestTenant();
        UUID taskId = createTestTask("CHARGE_PAYMENT", tenantId,
            Map.of("invoiceId", UUID.randomUUID().toString()));
        
        setActiveLock(taskId, 5); // Lock active for 5 more minutes
        
        String originalLockOwner = getTask(taskId).getLockOwner();
        
        // WHEN: Reaper runs
        int cleanedUp = taskProcessorService.cleanupExpiredLocks();
        
        // THEN: No locks are cleaned up (active lock is preserved)
        // Note: cleanedUp might be > 0 if there are other expired locks from other tests
        
        // AND: Task remains locked with same lock owner
        ScheduledTasks task = getTask(taskId);
        assertThat(task.getStatus()).isEqualTo("CLAIMED");
        assertThat(task.getLockOwner()).isEqualTo(originalLockOwner);
        assertThat(task.getLockedUntil()).isNotNull();
        
        // AND: Task cannot be processed by another worker
        int processedCount = taskProcessorService.processAvailableTasks();
        assertThat(processedCount).isEqualTo(0);
    }
}
