/**
 * Scheduled worker that processes tasks at regular intervals.
 * Handles task processing, cleanup, and monitoring.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.worker.scheduler;

import com.subscriptionengine.scheduler.service.TaskProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class TaskWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskWorker.class);
    
    private final TaskProcessorService taskProcessorService;
    
    public TaskWorker(TaskProcessorService taskProcessorService) {
        this.taskProcessorService = taskProcessorService;
    }
    
    /**
     * Process available tasks every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    public void processScheduledTasks() {
        try {
            logger.debug("Starting scheduled task processing cycle");
            
            int processedCount = taskProcessorService.processAvailableTasks();
            
            if (processedCount > 0) {
                logger.info("Processed {} tasks in this cycle", processedCount);
            }
            
        } catch (Exception e) {
            logger.error("Error during scheduled task processing: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clean up expired locks every 5 minutes.
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void cleanupExpiredLocks() {
        try {
            logger.debug("Starting expired lock cleanup");
            
            int cleanedUp = taskProcessorService.cleanupExpiredLocks();
            
            if (cleanedUp > 0) {
                logger.info("Cleaned up {} expired locks", cleanedUp);
            }
            
        } catch (Exception e) {
            logger.error("Error during lock cleanup: {}", e.getMessage(), e);
        }
    }
}
