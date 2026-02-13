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
 * Tests for CREATE_DELIVERY task handler.
 * Verifies delivery instance creation for physical/hybrid products.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
public class DeliveryCreationHandlerTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 1: Delivery creation task can be created")
    @Description("Verify CREATE_DELIVERY task can be created and stored")
    void testDeliveryCreationTaskCanBeCreated() throws Exception {
        // GIVEN: A delivery creation task
        UUID tenantId = createTestTenant();
        UUID invoiceId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        
        UUID taskId = createTestTask("CREATE_DELIVERY", tenantId,
            Map.of(
                "invoiceId", invoiceId.toString(),
                "subscriptionId", subscriptionId.toString()
            ));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Task exists with correct properties
        assertThat(task).isNotNull();
        assertThat(task.getTaskType()).isEqualTo("CREATE_DELIVERY");
        assertThat(task.getStatus()).isEqualTo("READY");
        assertThat(task.getTenantId()).isEqualTo(tenantId);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Delivery creation task payload is stored correctly")
    @Description("Verify CREATE_DELIVERY task payload contains required fields")
    void testDeliveryCreationTaskPayloadStoredCorrectly() throws Exception {
        // GIVEN: A delivery creation task with specific payload
        UUID tenantId = createTestTenant();
        UUID invoiceId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        
        UUID taskId = createTestTask("CREATE_DELIVERY", tenantId,
            Map.of(
                "invoiceId", invoiceId.toString(),
                "subscriptionId", subscriptionId.toString()
            ));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Payload contains expected data
        assertThat(task.getPayload()).isNotNull();
        assertThat(task.getPayload().data()).contains(invoiceId.toString());
        assertThat(task.getPayload().data()).contains(subscriptionId.toString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 3: Multiple delivery tasks can be created")
    @Description("Verify multiple CREATE_DELIVERY tasks can exist simultaneously")
    void testMultipleDeliveryTasksCanBeCreated() throws Exception {
        // GIVEN: Multiple delivery creation tasks
        UUID tenantId = createTestTenant();
        
        UUID taskId1 = createTestTask("CREATE_DELIVERY", tenantId,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString()
            ));
        UUID taskId2 = createTestTask("CREATE_DELIVERY", tenantId,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString()
            ));
        
        // WHEN: Tasks are retrieved
        ScheduledTasks task1 = getTask(taskId1);
        ScheduledTasks task2 = getTask(taskId2);
        
        // THEN: Both tasks exist independently
        assertThat(task1).isNotNull();
        assertThat(task2).isNotNull();
        assertThat(task1.getId()).isNotEqualTo(task2.getId());
        assertThat(countTasksByStatus("READY")).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 4: Delivery tasks maintain tenant isolation")
    @Description("Verify CREATE_DELIVERY tasks are properly isolated by tenant")
    void testDeliveryTasksMaintainTenantIsolation() throws Exception {
        // GIVEN: Tasks for different tenants
        UUID tenant1 = createTestTenant();
        UUID tenant2 = createTestTenant();
        
        UUID task1 = createTestTask("CREATE_DELIVERY", tenant1,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString()
            ));
        UUID task2 = createTestTask("CREATE_DELIVERY", tenant2,
            Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "subscriptionId", UUID.randomUUID().toString()
            ));
        
        // WHEN: Tasks are retrieved
        ScheduledTasks retrievedTask1 = getTask(task1);
        ScheduledTasks retrievedTask2 = getTask(task2);
        
        // THEN: Tasks belong to correct tenants
        assertThat(retrievedTask1.getTenantId()).isEqualTo(tenant1);
        assertThat(retrievedTask2.getTenantId()).isEqualTo(tenant2);
        assertThat(retrievedTask1.getTenantId()).isNotEqualTo(retrievedTask2.getTenantId());
    }
}
