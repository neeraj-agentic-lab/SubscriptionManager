package com.subscriptionengine.auth;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.RateLimitBuckets.RATE_LIMIT_BUCKETS;

/**
 * PostgreSQL-backed rate limiter using sliding window algorithm.
 * Tracks API requests per client and enforces rate limits.
 * 
 * @author Neeraj Yadav
 */
@Service
public class RateLimiter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);
    private static final int WINDOW_SIZE_MINUTES = 60; // 1 hour sliding window
    
    private final DSLContext dsl;
    
    public RateLimiter(DSLContext dsl) {
        this.dsl = dsl;
    }
    
    /**
     * Check if request is allowed under rate limit.
     * Returns true if request is allowed.
     * Returns false if rate limit exceeded (429 should be returned).
     */
    public boolean allowRequest(String clientId, int rateLimitPerHour) {
        try {
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(WINDOW_SIZE_MINUTES);
            
            // Count requests in current window
            long requestCount = dsl.selectCount()
                    .from(RATE_LIMIT_BUCKETS)
                    .where(RATE_LIMIT_BUCKETS.CLIENT_ID.eq(clientId))
                    .and(RATE_LIMIT_BUCKETS.WINDOW_START.ge(windowStart))
                    .fetchOne(0, Long.class);
            
            if (requestCount >= rateLimitPerHour) {
                logger.warn("Rate limit exceeded for client: {}. Count: {}, Limit: {}", 
                        clientId, requestCount, rateLimitPerHour);
                return false;
            }
            
            // Record this request
            recordRequest(clientId);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for client: {}", clientId, e);
            // Fail open - allow request on error (configurable based on security requirements)
            return true;
        }
    }
    
    /**
     * Record a request in the rate limit bucket.
     */
    private void recordRequest(String clientId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime windowStart = now.withMinute(0).withSecond(0).withNano(0);
            
            dsl.insertInto(RATE_LIMIT_BUCKETS)
                    .set(RATE_LIMIT_BUCKETS.ID, UUID.randomUUID())
                    .set(RATE_LIMIT_BUCKETS.CLIENT_ID, clientId)
                    .set(RATE_LIMIT_BUCKETS.WINDOW_START, windowStart)
                    .set(RATE_LIMIT_BUCKETS.REQUEST_COUNT, 1)
                    .execute();
            
            logger.debug("Recorded request for client: {} at window: {}", clientId, windowStart);
            
        } catch (Exception e) {
            logger.error("Error recording request for client: {}", clientId, e);
        }
    }
    
    /**
     * Get current request count for a client in the current window.
     */
    public long getCurrentRequestCount(String clientId) {
        try {
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(WINDOW_SIZE_MINUTES);
            
            return dsl.selectCount()
                    .from(RATE_LIMIT_BUCKETS)
                    .where(RATE_LIMIT_BUCKETS.CLIENT_ID.eq(clientId))
                    .and(RATE_LIMIT_BUCKETS.WINDOW_START.ge(windowStart))
                    .fetchOne(0, Long.class);
            
        } catch (Exception e) {
            logger.error("Error getting request count for client: {}", clientId, e);
            return 0;
        }
    }
    
    /**
     * Get remaining requests for a client in the current window.
     */
    public long getRemainingRequests(String clientId, int rateLimitPerHour) {
        long currentCount = getCurrentRequestCount(clientId);
        return Math.max(0, rateLimitPerHour - currentCount);
    }
    
    /**
     * Cleanup old rate limit buckets.
     * Runs every hour to remove entries older than 2 hours.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupOldBuckets() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
            
            int deleted = dsl.deleteFrom(RATE_LIMIT_BUCKETS)
                    .where(RATE_LIMIT_BUCKETS.WINDOW_START.lt(cutoff))
                    .execute();
            
            if (deleted > 0) {
                logger.info("Cleaned up {} old rate limit buckets", deleted);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up old rate limit buckets", e);
        }
    }
    
    /**
     * Reset rate limit for a specific client (e.g., for testing or manual override).
     */
    public void resetClientRateLimit(String clientId) {
        try {
            int deleted = dsl.deleteFrom(RATE_LIMIT_BUCKETS)
                    .where(RATE_LIMIT_BUCKETS.CLIENT_ID.eq(clientId))
                    .execute();
            
            logger.info("Reset rate limit for client: {}. Deleted {} buckets", clientId, deleted);
        } catch (Exception e) {
            logger.error("Error resetting rate limit for client: {}", clientId, e);
        }
    }
}
