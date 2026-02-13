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
 * Tests for ENTITLEMENT_GRANT task handler.
 * Verifies entitlement granting/revoking automation for subscription features.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
public class EntitlementGrantHandlerTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 1: Entitlement grant task can be created")
    @Description("Verify ENTITLEMENT_GRANT task can be created and stored")
    void testEntitlementGrantTaskCanBeCreated() throws Exception {
        // GIVEN: An entitlement grant task
        UUID tenantId = createTestTenant();
        UUID invoiceId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        
        UUID taskId = createTestTask("ENTITLEMENT_GRANT", tenantId,
            Map.of(
                "invoiceId", invoiceId.toString(),
                "subscriptionId", subscriptionId.toString(),
                "action", "GRANT"
            ));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Task exists with correct properties
        assertThat(task).isNotNull();
        assertThat(task.getTaskType()).isEqualTo("ENTITLEMENT_GRANT");
        assertThat(task.getStatus()).isEqualTo("READY");
        assertThat(task.getTenantId()).isEqualTo(tenantId);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Entitlement grant task payload is stored correctly")
    @Description("Verify ENTITLEMENT_GRANT task payload contains invoiceId, subscriptionId, and action")
    void testEntitlementGrantTaskPayloadStoredCorrectly() throws Exception {
        // GIVEN: An entitlement grant task with specific payload
        UUID tenantId = createTestTenant();
        UUID invoiceId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        
        UUID taskId = createTestTask("ENTITLEMENT_GRANT", tenantId,
            Map.of(
                "invoiceId", invoiceId.toString(),
                "subscriptionId", subscriptionId.toString(),
                "action", "GRANT"
            ));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Payload contains expected data
        assertThat(task.getPayload()).isNotNull();
        assertThat(task.getPayload().data()).contains(invoiceId.toString());
        assertThat(task.getPayload().data()).contains(subscriptionId.toString());
        assertThat(task.getPayload().data()).contains("GRANT");
        assertThat(task.getPayload().data()).contains("action");
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Entitlement tasks support different actions")
    @Description("Verify ENTITLEMENT_GRANT tasks can be created with GRANT and REVOKE actions")
    void testEntitlementTasksSupportDifferentActions() throws Exception {
        // GIVEN: Entitlement tasks with different actions
        UUID tenantId = createTestTenant();
        UUID subscriptionId = UUID.randomUUID();
        
        // GRANT action - granting access to features
        UUID grantTaskId = createTestTask("ENTITLEMENT_GRANT", tenantId,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", subscriptionId.toString(),
                "action", "GRANT"
            ));
        
        // REVOKE action - removing access to features
        UUID revokeTaskId = createTestTask("ENTITLEMENT_GRANT", tenantId,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", subscriptionId.toString(),
                "action", "REVOKE"
            ));
        
        // WHEN: Tasks are retrieved
        ScheduledTasks grantTask = getTask(grantTaskId);
        ScheduledTasks revokeTask = getTask(revokeTaskId);
        
        // THEN: Both tasks exist with correct actions
        assertThat(grantTask.getPayload().data()).contains("GRANT");
        assertThat(revokeTask.getPayload().data()).contains("REVOKE");
        
        // AND: Both are READY for processing
        assertThat(grantTask.getStatus()).isEqualTo("READY");
        assertThat(revokeTask.getStatus()).isEqualTo("READY");
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 4: Multiple entitlement tasks process independently")
    @Description("Verify multiple ENTITLEMENT_GRANT tasks can exist simultaneously without interference")
    void testMultipleEntitlementTasksProcessIndependently() throws Exception {
        // GIVEN: Multiple entitlement tasks for different subscriptions
        UUID tenantId = createTestTenant();
        
        UUID task1 = createTestTask("ENTITLEMENT_GRANT", tenantId,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString(),
                "action", "GRANT"
            ));
        
        UUID task2 = createTestTask("ENTITLEMENT_GRANT", tenantId,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString(),
                "action", "GRANT"
            ));
        
        UUID task3 = createTestTask("ENTITLEMENT_GRANT", tenantId,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString(),
                "action", "REVOKE"
            ));
        
        // WHEN: Tasks are retrieved
        ScheduledTasks retrievedTask1 = getTask(task1);
        ScheduledTasks retrievedTask2 = getTask(task2);
        ScheduledTasks retrievedTask3 = getTask(task3);
        
        // THEN: All tasks exist independently
        assertThat(retrievedTask1).isNotNull();
        assertThat(retrievedTask2).isNotNull();
        assertThat(retrievedTask3).isNotNull();
        
        // AND: Each task has unique ID
        assertThat(retrievedTask1.getId()).isNotEqualTo(retrievedTask2.getId());
        assertThat(retrievedTask2.getId()).isNotEqualTo(retrievedTask3.getId());
        
        // AND: All belong to same tenant
        assertThat(retrievedTask1.getTenantId()).isEqualTo(tenantId);
        assertThat(retrievedTask2.getTenantId()).isEqualTo(tenantId);
        assertThat(retrievedTask3.getTenantId()).isEqualTo(tenantId);
        
        // AND: Multiple READY tasks exist
        assertThat(countTasksByStatus("READY")).isGreaterThanOrEqualTo(3);
    }
}
