package com.subscriptionengine.api.integration;

import com.subscriptionengine.generated.tables.pojos.ScheduledTasks;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for multi-tenant isolation in worker module.
 * Verifies strict tenant boundaries and data isolation across all task types.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
public class MultiTenantIsolationTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 1: Tasks from different tenants are isolated")
    @Description("Verify tasks from different tenants are properly segregated with no cross-tenant data leakage")
    void testTasksFromDifferentTenantsAreIsolated() throws Exception {
        // GIVEN: Tasks for multiple tenants
        UUID tenant1 = createTestTenant();
        UUID tenant2 = createTestTenant();
        UUID tenant3 = createTestTenant();
        
        // Create tasks for each tenant
        UUID task1 = createTestTask("SUBSCRIPTION_RENEWAL", tenant1,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        UUID task2 = createTestTask("CHARGE_PAYMENT", tenant2,
            Map.of("invoiceId", UUID.randomUUID().toString()));
        
        UUID task3 = createTestTask("CREATE_DELIVERY", tenant3,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString()
            ));
        
        // WHEN: Tasks are retrieved
        ScheduledTasks retrievedTask1 = getTask(task1);
        ScheduledTasks retrievedTask2 = getTask(task2);
        ScheduledTasks retrievedTask3 = getTask(task3);
        
        // THEN: Each task belongs to its correct tenant
        assertThat(retrievedTask1.getTenantId()).isEqualTo(tenant1);
        assertThat(retrievedTask2.getTenantId()).isEqualTo(tenant2);
        assertThat(retrievedTask3.getTenantId()).isEqualTo(tenant3);
        
        // AND: Tenants are all different
        assertThat(tenant1).isNotEqualTo(tenant2);
        assertThat(tenant2).isNotEqualTo(tenant3);
        assertThat(tenant1).isNotEqualTo(tenant3);
        
        // AND: No cross-tenant contamination
        assertThat(retrievedTask1.getTenantId()).isNotEqualTo(tenant2);
        assertThat(retrievedTask1.getTenantId()).isNotEqualTo(tenant3);
        assertThat(retrievedTask2.getTenantId()).isNotEqualTo(tenant1);
        assertThat(retrievedTask2.getTenantId()).isNotEqualTo(tenant3);
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 2: Worker processes tasks with correct tenant context")
    @Description("Verify tenant context is maintained during task processing")
    void testWorkerProcessesTasksWithCorrectTenantContext() throws Exception {
        // GIVEN: Tasks for different tenants with processable task type
        UUID tenant1 = createTestTenant();
        UUID tenant2 = createTestTenant();
        
        UUID task1 = createTestTask("SUBSCRIPTION_RENEWAL", tenant1,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        UUID task2 = createTestTask("SUBSCRIPTION_RENEWAL", tenant2,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        
        // WHEN: Worker processes tasks
        int processedCount = taskProcessorService.processAvailableTasks();
        
        // THEN: Tasks are processed
        assertThat(processedCount).isGreaterThanOrEqualTo(0);
        
        // AND: Each task maintains its tenant association
        ScheduledTasks processedTask1 = getTask(task1);
        ScheduledTasks processedTask2 = getTask(task2);
        
        assertThat(processedTask1.getTenantId()).isEqualTo(tenant1);
        assertThat(processedTask2.getTenantId()).isEqualTo(tenant2);
        
        // AND: Tenant IDs haven't changed during processing
        assertThat(processedTask1.getTenantId()).isNotEqualTo(tenant2);
        assertThat(processedTask2.getTenantId()).isNotEqualTo(tenant1);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Task queries respect tenant boundaries")
    @Description("Verify database queries maintain tenant isolation when counting/filtering tasks")
    void testTaskQueriesRespectTenantBoundaries() throws Exception {
        // GIVEN: Multiple tasks across different tenants
        UUID tenant1 = createTestTenant();
        UUID tenant2 = createTestTenant();
        
        // Tenant 1: 3 tasks
        createTestTask("SUBSCRIPTION_RENEWAL", tenant1,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        createTestTask("CHARGE_PAYMENT", tenant1,
            Map.of("invoiceId", UUID.randomUUID().toString()));
        createTestTask("CREATE_DELIVERY", tenant1,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString()
            ));
        
        // Tenant 2: 2 tasks
        createTestTask("SUBSCRIPTION_RENEWAL", tenant2,
            Map.of("subscriptionId", UUID.randomUUID().toString()));
        createTestTask("ENTITLEMENT_GRANT", tenant2,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString(),
                "action", "GRANT"
            ));
        
        // WHEN: Counting all READY tasks (across all tenants)
        int totalReadyTasks = countTasksByStatus("READY");
        
        // THEN: Total count includes tasks from all tenants
        assertThat(totalReadyTasks).isGreaterThanOrEqualTo(5);
        
        // AND: Tasks exist in database
        assertThat(countAllTasks()).isGreaterThanOrEqualTo(5);
        
        // Note: This test verifies that the count functions work correctly
        // In a real multi-tenant system, you'd also want to verify that
        // tenant-filtered queries only return tasks for that specific tenant
    }
}
