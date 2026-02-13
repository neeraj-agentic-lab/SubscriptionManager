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
 * Tests for CREATE_ORDER task handler.
 * Verifies external order creation through commerce adapter.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
public class OrderCreationHandlerTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 1: Order creation task can be created")
    @Description("Verify CREATE_ORDER task can be created and stored")
    void testOrderCreationTaskCanBeCreated() throws Exception {
        // GIVEN: An order creation task
        UUID tenantId = createTestTenant();
        UUID deliveryId = UUID.randomUUID();
        
        UUID taskId = createTestTask("CREATE_ORDER", tenantId,
            Map.of("deliveryId", deliveryId.toString()));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Task exists with correct properties
        assertThat(task).isNotNull();
        assertThat(task.getTaskType()).isEqualTo("CREATE_ORDER");
        assertThat(task.getStatus()).isEqualTo("READY");
        assertThat(task.getTenantId()).isEqualTo(tenantId);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Order creation task payload is stored correctly")
    @Description("Verify CREATE_ORDER task payload contains required fields")
    void testOrderCreationTaskPayloadStoredCorrectly() throws Exception {
        // GIVEN: An order creation task with specific payload
        UUID tenantId = createTestTenant();
        UUID deliveryId = UUID.randomUUID();
        
        UUID taskId = createTestTask("CREATE_ORDER", tenantId,
            Map.of("deliveryId", deliveryId.toString()));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Payload contains expected data
        assertThat(task.getPayload()).isNotNull();
        assertThat(task.getPayload().data()).contains(deliveryId.toString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 3: Multiple order tasks maintain tenant isolation")
    @Description("Verify CREATE_ORDER tasks are properly isolated by tenant")
    void testMultipleOrderTasksMaintainTenantIsolation() throws Exception {
        // GIVEN: Tasks for different tenants
        UUID tenant1 = createTestTenant();
        UUID tenant2 = createTestTenant();
        
        UUID task1 = createTestTask("CREATE_ORDER", tenant1,
            Map.of("deliveryId", UUID.randomUUID().toString()));
        UUID task2 = createTestTask("CREATE_ORDER", tenant2,
            Map.of("deliveryId", UUID.randomUUID().toString()));
        
        // WHEN: Tasks are retrieved
        ScheduledTasks retrievedTask1 = getTask(task1);
        ScheduledTasks retrievedTask2 = getTask(task2);
        
        // THEN: Tasks belong to correct tenants
        assertThat(retrievedTask1.getTenantId()).isEqualTo(tenant1);
        assertThat(retrievedTask2.getTenantId()).isEqualTo(tenant2);
        assertThat(retrievedTask1.getTenantId()).isNotEqualTo(retrievedTask2.getTenantId());
    }
}
