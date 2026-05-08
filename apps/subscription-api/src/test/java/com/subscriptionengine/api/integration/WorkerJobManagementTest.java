package com.subscriptionengine.api.integration;

import com.subscriptionengine.scheduler.service.JobConfigurationService;
import com.subscriptionengine.scheduler.service.JobExecutionHistoryService;
import com.subscriptionengine.scheduler.service.DynamicSchedulingService;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Worker Job Management functionality.
 * Tests the services that power the Worker Admin API endpoints.
 * 
 * These tests complete Phase 3 of the test roadmap by covering the
 * job management functionality that was previously untested.
 * 
 * Note: These tests verify the underlying services used by the Worker Admin APIs
 * on port 8081. The actual REST endpoints are tested manually or via E2E tests
 * since the worker runs as a separate application.
 * 
 * @author Neeraj Yadav
 * @created 2026-02-17
 */
@Epic("Worker Module")
@Feature("Job Management Services")
class WorkerJobManagementTest extends BaseWorkerTest {
    
    @Autowired(required = false)
    private JobConfigurationService jobConfigService;
    
    @Autowired(required = false)
    private JobExecutionHistoryService jobHistoryService;
    
    @Autowired(required = false)
    private DynamicSchedulingService dynamicSchedulingService;
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Test 1: Job configuration service retrieves job settings")
    @Description("Verify JobConfigurationService can retrieve job configuration from database")
    void testGetJobConfiguration() {
        // GIVEN: Job configuration exists in database (from Flyway migrations)
        
        // WHEN: Get job configuration
        if (jobConfigService != null) {
            Optional<Map<String, Object>> config = jobConfigService.getJobConfiguration("subscription_renewal");
            
            // THEN: Configuration is retrieved successfully
            assertThat(config).isPresent();
            assertThat(config.get()).containsKeys("job_name", "cron_expression", "enabled");
            assertThat(config.get().get("job_name")).isEqualTo("subscription_renewal");
        } else {
            // Service not available in test context - mark as skipped
            assertThat(jobConfigService).as("JobConfigurationService should be available").isNotNull();
        }
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 2: Job execution history service tracks executions")
    @Description("Verify JobExecutionHistoryService can retrieve execution history")
    void testGetJobExecutionHistory() {
        // WHEN: Get job execution history
        if (jobHistoryService != null) {
            List<Map<String, Object>> history = jobHistoryService.getRecentExecutions(null, 50);
            
            // THEN: History is retrieved (may be empty if no jobs have run)
            assertThat(history).isNotNull();
            assertThat(history.size()).isLessThanOrEqualTo(50);
        } else {
            // Service not available in test context - mark as skipped
            assertThat(jobHistoryService).as("JobExecutionHistoryService should be available").isNotNull();
        }
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 3: Job execution history supports filtering by job name")
    @Description("Verify history service can filter executions by specific job name")
    void testGetJobExecutionHistoryWithFilter() {
        // WHEN: Get history filtered by job name
        if (jobHistoryService != null) {
            List<Map<String, Object>> history = jobHistoryService.getRecentExecutions("subscription_renewal", 10);
            
            // THEN: Filtered history is retrieved
            assertThat(history).isNotNull();
            assertThat(history.size()).isLessThanOrEqualTo(10);
            
            // All returned executions should be for the specified job
            for (Map<String, Object> execution : history) {
                if (execution.containsKey("job_name")) {
                    assertThat(execution.get("job_name")).isEqualTo("subscription_renewal");
                }
            }
        } else {
            assertThat(jobHistoryService).as("JobExecutionHistoryService should be available").isNotNull();
        }
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 4: Job statistics service provides execution metrics")
    @Description("Verify JobExecutionHistoryService can calculate job statistics")
    void testGetJobStatistics() {
        // WHEN: Get job statistics
        if (jobHistoryService != null) {
            Map<String, Object> stats = jobHistoryService.getJobStatistics("subscription_renewal");
            
            // THEN: Statistics are calculated with expected keys
            assertThat(stats).isNotNull();
            assertThat(stats).containsKeys(
                "totalExecutions", 
                "totalErrors", 
                "avgExecutionTimeMs",
                "totalSubscriptionsProcessed",
                "totalTasksCreated",
                "statusBreakdown"
            );
            
            // Verify numeric values are present
            assertThat(stats.get("totalExecutions")).isInstanceOf(Number.class);
            assertThat(stats.get("totalErrors")).isInstanceOf(Number.class);
        } else {
            assertThat(jobHistoryService).as("JobExecutionHistoryService should be available").isNotNull();
        }
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Test 5: Dynamic scheduling service manages job schedules")
    @Description("Verify DynamicSchedulingService can refresh job schedules from database")
    void testRefreshJobSchedules() {
        // WHEN: Refresh job schedules
        if (dynamicSchedulingService != null) {
            // Trigger a refresh of job schedules
            dynamicSchedulingService.refreshAllJobSchedules();
            
            // THEN: No exception is thrown (refresh succeeds)
            // Note: Actual scheduling verification would require checking scheduled tasks
            assertThat(true).as("Schedule refresh completed without errors").isTrue();
        } else {
            assertThat(dynamicSchedulingService).as("DynamicSchedulingService should be available").isNotNull();
        }
    }
}
