package com.subscriptionengine.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionengine.generated.tables.daos.ScheduledTasksDao;
import com.subscriptionengine.generated.tables.pojos.ScheduledTasks;
import com.subscriptionengine.scheduler.service.TaskProcessorService;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.ScheduledTasks.SCHEDULED_TASKS;

/**
 * Base class for worker module tests.
 * Provides test infrastructure for worker tests without web server.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-13
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.profiles.active=test"
)
public abstract class BaseWorkerTest {
    
    // Use singleton container that survives across all test classes
    protected static final PostgreSQLContainer<?> postgres = PostgresTestContainer.getInstance();
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }
    
    @Autowired
    protected DSLContext dsl;
    
    @Autowired
    protected TaskProcessorService taskProcessorService;
    
    @Autowired
    protected ScheduledTasksDao scheduledTasksDao;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    /**
     * Create a test tenant for testing.
     * 
     * @return Created tenant ID
     */
    protected UUID createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-" + tenantId.toString().substring(0, 8);
        dsl.insertInto(org.jooq.impl.DSL.table("tenants"))
            .columns(
                org.jooq.impl.DSL.field("id"),
                org.jooq.impl.DSL.field("name"),
                org.jooq.impl.DSL.field("slug"),
                org.jooq.impl.DSL.field("status"),
                org.jooq.impl.DSL.field("created_at"),
                org.jooq.impl.DSL.field("updated_at")
            )
            .values(
                tenantId,
                "Test Tenant " + tenantId,
                slug,
                "ACTIVE",
                java.time.OffsetDateTime.now(),
                java.time.OffsetDateTime.now()
            )
            .execute();
        return tenantId;
    }
    
    /**
     * Create a test task in READY status with current due date.
     * 
     * @param taskType Type of task (e.g., SUBSCRIPTION_RENEWAL, CHARGE_PAYMENT)
     * @param tenantId Tenant ID for the task
     * @param payload Task payload as map
     * @return Created task ID
     */
    protected UUID createTestTask(String taskType, UUID tenantId, Map<String, Object> payload) throws Exception {
        return createTestTaskWithDueDate(taskType, tenantId, payload, OffsetDateTime.now());
    }
    
    /**
     * Create a test task with custom due date.
     * 
     * @param taskType Type of task
     * @param tenantId Tenant ID for the task
     * @param payload Task payload as map
     * @param dueAt When the task should be due
     * @return Created task ID
     */
    protected UUID createTestTaskWithDueDate(String taskType, UUID tenantId, 
                                             Map<String, Object> payload, OffsetDateTime dueAt) throws Exception {
        UUID taskId = UUID.randomUUID();
        ScheduledTasks task = new ScheduledTasks();
        task.setId(taskId);
        task.setTenantId(tenantId);
        task.setTaskType(taskType);
        task.setTaskKey(taskType + "_" + UUID.randomUUID());
        task.setStatus("READY");
        task.setDueAt(dueAt);
        task.setAttemptCount(0);
        task.setMaxAttempts(3);
        task.setPayload(JSONB.valueOf(objectMapper.writeValueAsString(payload)));
        task.setCreatedAt(OffsetDateTime.now());
        task.setUpdatedAt(OffsetDateTime.now());
        
        scheduledTasksDao.insert(task);
        return taskId;
    }
    
    /**
     * Get task by ID from database.
     * 
     * @param taskId Task ID to retrieve
     * @return Task object or null if not found
     */
    protected ScheduledTasks getTask(UUID taskId) {
        return scheduledTasksDao.findById(taskId);
    }
    
    /**
     * Set expired lock on a task (for reaper testing).
     * Simulates a task that was claimed but the worker died.
     * 
     * @param taskId Task ID to lock
     * @param minutesAgo How many minutes ago the lock expired
     */
    protected void setExpiredLock(UUID taskId, int minutesAgo) {
        dsl.update(SCHEDULED_TASKS)
            .set(SCHEDULED_TASKS.STATUS, "CLAIMED")
            .set(SCHEDULED_TASKS.LOCKED_UNTIL, OffsetDateTime.now().minusMinutes(minutesAgo))
            .set(SCHEDULED_TASKS.LOCK_OWNER, "old-worker-" + UUID.randomUUID())
            .where(SCHEDULED_TASKS.ID.eq(taskId))
            .execute();
    }
    
    /**
     * Set active lock on a task.
     * Simulates a task currently being processed by another worker.
     * 
     * @param taskId Task ID to lock
     * @param minutesFromNow How many minutes the lock should remain active
     */
    protected void setActiveLock(UUID taskId, int minutesFromNow) {
        dsl.update(SCHEDULED_TASKS)
            .set(SCHEDULED_TASKS.STATUS, "CLAIMED")
            .set(SCHEDULED_TASKS.LOCKED_UNTIL, OffsetDateTime.now().plusMinutes(minutesFromNow))
            .set(SCHEDULED_TASKS.LOCK_OWNER, "active-worker-" + UUID.randomUUID())
            .where(SCHEDULED_TASKS.ID.eq(taskId))
            .execute();
    }
    
    /**
     * Wait for async task processing (if needed).
     * Use sparingly - prefer synchronous testing when possible.
     * 
     * @param milliseconds Time to wait in milliseconds
     */
    protected void waitForTaskProcessing(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }
    
    /**
     * Count tasks by status.
     * Useful for verifying batch processing results.
     * 
     * @param status Task status to count (READY, CLAIMED, COMPLETED, FAILED)
     * @return Number of tasks with given status
     */
    protected int countTasksByStatus(String status) {
        return dsl.selectCount()
            .from(SCHEDULED_TASKS)
            .where(SCHEDULED_TASKS.STATUS.eq(status))
            .fetchOne(0, int.class);
    }
    
    /**
     * Count all tasks in database.
     * Useful for cleanup verification.
     * 
     * @return Total number of tasks
     */
    protected int countAllTasks() {
        return dsl.selectCount()
            .from(SCHEDULED_TASKS)
            .fetchOne(0, int.class);
    }
    
    /**
     * Delete all tasks for cleanup between tests.
     * Use in @AfterEach if needed for test isolation.
     */
    protected void deleteAllTasks() {
        dsl.deleteFrom(SCHEDULED_TASKS).execute();
    }
}
