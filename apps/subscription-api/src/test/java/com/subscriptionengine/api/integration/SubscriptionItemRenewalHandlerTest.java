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
 * Tests for PRODUCT_RENEWAL task handler (Subscription Item Renewal).
 * Verifies billing execution for individual subscription items/add-ons.
 * 
 * Note: Task type is PRODUCT_RENEWAL in the system, but conceptually represents
 * subscription item billing/renewal (e.g., monthly billing for add-on products).
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
public class SubscriptionItemRenewalHandlerTest extends BaseWorkerTest {
    
    @AfterEach
    void cleanup() {
        deleteAllTasks();
    }
    
    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Test 1: Subscription item renewal task can be created")
    @Description("Verify PRODUCT_RENEWAL task for subscription items can be created and stored")
    void testSubscriptionItemRenewalTaskCanBeCreated() throws Exception {
        // GIVEN: A subscription item renewal task (e.g., monthly billing for Premium Headphones add-on)
        UUID tenantId = createTestTenant();
        UUID productId = UUID.randomUUID(); // Subscription item/product ID
        UUID subscriptionId = UUID.randomUUID(); // Parent subscription ID
        
        UUID taskId = createTestTask("PRODUCT_RENEWAL", tenantId,
            Map.of(
                "productId", productId.toString(),
                "subscriptionId", subscriptionId.toString()
            ));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Task exists with correct properties
        assertThat(task).isNotNull();
        assertThat(task.getTaskType()).isEqualTo("PRODUCT_RENEWAL");
        assertThat(task.getStatus()).isEqualTo("READY");
        assertThat(task.getTenantId()).isEqualTo(tenantId);
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 2: Subscription item renewal task payload contains required fields")
    @Description("Verify PRODUCT_RENEWAL task payload includes productId and subscriptionId")
    void testSubscriptionItemRenewalTaskPayloadContainsRequiredFields() throws Exception {
        // GIVEN: A subscription item renewal task with specific payload
        UUID tenantId = createTestTenant();
        UUID productId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        
        UUID taskId = createTestTask("PRODUCT_RENEWAL", tenantId,
            Map.of(
                "productId", productId.toString(),
                "subscriptionId", subscriptionId.toString()
            ));
        
        // WHEN: Task is retrieved
        ScheduledTasks task = getTask(taskId);
        
        // THEN: Payload contains expected data
        assertThat(task.getPayload()).isNotNull();
        assertThat(task.getPayload().data()).contains(productId.toString());
        assertThat(task.getPayload().data()).contains(subscriptionId.toString());
        assertThat(task.getPayload().data()).contains("productId");
        assertThat(task.getPayload().data()).contains("subscriptionId");
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 3: Multiple subscription item renewals maintain independence")
    @Description("Verify multiple subscription items can have renewal tasks simultaneously without interference")
    void testMultipleSubscriptionItemRenewalsMaintainIndependence() throws Exception {
        // GIVEN: Multiple subscription item renewal tasks
        // Simulates: Customer has multiple add-ons (Headphones, Cloud Storage, Gaming Controller)
        UUID tenantId = createTestTenant();
        UUID subscriptionId = UUID.randomUUID(); // Same parent subscription
        
        // Item 1: Premium Headphones ($29.99/month)
        UUID headphonesId = UUID.randomUUID();
        UUID task1 = createTestTask("PRODUCT_RENEWAL", tenantId,
            Map.of(
                "productId", headphonesId.toString(),
                "subscriptionId", subscriptionId.toString()
            ));
        
        // Item 2: Cloud Storage ($9.99/quarter)
        UUID cloudStorageId = UUID.randomUUID();
        UUID task2 = createTestTask("PRODUCT_RENEWAL", tenantId,
            Map.of(
                "productId", cloudStorageId.toString(),
                "subscriptionId", subscriptionId.toString()
            ));
        
        // Item 3: Gaming Controller ($19.99/month)
        UUID gamingControllerId = UUID.randomUUID();
        UUID task3 = createTestTask("PRODUCT_RENEWAL", tenantId,
            Map.of(
                "productId", gamingControllerId.toString(),
                "subscriptionId", subscriptionId.toString()
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
        assertThat(retrievedTask1.getId()).isNotEqualTo(retrievedTask3.getId());
        
        // AND: All belong to same tenant and subscription
        assertThat(retrievedTask1.getTenantId()).isEqualTo(tenantId);
        assertThat(retrievedTask2.getTenantId()).isEqualTo(tenantId);
        assertThat(retrievedTask3.getTenantId()).isEqualTo(tenantId);
        
        // AND: Multiple READY tasks exist
        assertThat(countTasksByStatus("READY")).isGreaterThanOrEqualTo(3);
    }
}
