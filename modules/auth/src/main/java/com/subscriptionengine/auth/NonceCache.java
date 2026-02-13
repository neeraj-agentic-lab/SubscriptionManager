package com.subscriptionengine.auth;

import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.RequestNonces.REQUEST_NONCES;

/**
 * PostgreSQL-backed nonce cache for replay attack prevention.
 * Stores nonces with 10-minute TTL to prevent request replay.
 * 
 * @author Neeraj Yadav
 */
@Service
public class NonceCache {
    
    private static final Logger logger = LoggerFactory.getLogger(NonceCache.class);
    private static final int NONCE_TTL_MINUTES = 10;
    
    private final DSLContext dsl;
    
    public NonceCache(DSLContext dsl) {
        this.dsl = dsl;
    }
    
    /**
     * Check if nonce exists and store it if not.
     * Returns true if nonce is new (request allowed).
     * Returns false if nonce already exists (replay attack detected).
     */
    public boolean checkAndStore(String clientId, String nonce) {
        try {
            // Check if nonce already exists
            boolean exists = dsl.fetchExists(
                    dsl.selectFrom(REQUEST_NONCES)
                            .where(REQUEST_NONCES.CLIENT_ID.eq(clientId))
                            .and(REQUEST_NONCES.NONCE.eq(nonce))
            );
            
            if (exists) {
                logger.warn("Replay attack detected! Nonce already used: {} for client: {}", nonce, clientId);
                return false;
            }
            
            // Store new nonce
            dsl.insertInto(REQUEST_NONCES)
                    .set(REQUEST_NONCES.ID, UUID.randomUUID())
                    .set(REQUEST_NONCES.CLIENT_ID, clientId)
                    .set(REQUEST_NONCES.NONCE, nonce)
                    .set(REQUEST_NONCES.TIMESTAMP, LocalDateTime.now())
                    .set(REQUEST_NONCES.EXPIRES_AT, LocalDateTime.now().plusMinutes(NONCE_TTL_MINUTES))
                    .execute();
            
            logger.debug("Stored new nonce: {} for client: {}", nonce, clientId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking/storing nonce for client: {}", clientId, e);
            // Fail closed - reject request on error
            return false;
        }
    }
    
    /**
     * Cleanup expired nonces.
     * Runs every 5 minutes to remove old entries.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredNonces() {
        try {
            int deleted = dsl.deleteFrom(REQUEST_NONCES)
                    .where(REQUEST_NONCES.EXPIRES_AT.lt(LocalDateTime.now()))
                    .execute();
            
            if (deleted > 0) {
                logger.info("Cleaned up {} expired nonces", deleted);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired nonces", e);
        }
    }
    
    /**
     * Remove all nonces for a specific client (e.g., when client is revoked).
     */
    public void removeClientNonces(String clientId) {
        try {
            int deleted = dsl.deleteFrom(REQUEST_NONCES)
                    .where(REQUEST_NONCES.CLIENT_ID.eq(clientId))
                    .execute();
            
            logger.info("Removed {} nonces for client: {}", deleted, clientId);
        } catch (Exception e) {
            logger.error("Error removing nonces for client: {}", clientId, e);
        }
    }
}
