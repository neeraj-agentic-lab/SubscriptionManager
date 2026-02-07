/**
 * Mock implementation of EntitlementAdapter for development and testing.
 * Simulates entitlement management without calling external services.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.integrations.entitlements.impl;

import com.subscriptionengine.integrations.entitlements.EntitlementAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock entitlement adapter that simulates entitlement management.
 * Useful for development and testing without external dependencies.
 */
@Service
public class MockEntitlementAdapter implements EntitlementAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(MockEntitlementAdapter.class);
    
    // In-memory storage for mock entitlements
    private final Map<String, EntitlementStatus> entitlementStatuses = new ConcurrentHashMap<>();
    
    @Override
    public EntitlementResult grantEntitlements(EntitlementRequest request) {
        logger.info("[MOCK_ENTITLEMENT] Granting entitlements for customer {} product {} (subscription: {})", 
                   request.customerId(), request.productId(), request.subscriptionId());
        
        try {
            String entitlementKey = generateEntitlementKey(request.customerId(), request.productId());
            String externalEntitlementRef = "mock_entitlement_" + UUID.randomUUID().toString().substring(0, 8);
            
            logger.info("[MOCK_ENTITLEMENT] Processing entitlement grant for period {} to {}", 
                       request.periodStart(), request.periodEnd());
            
            // Log entitlement payload details
            if (request.entitlementPayload() != null && !request.entitlementPayload().isEmpty()) {
                logger.debug("[MOCK_ENTITLEMENT] Entitlement payload: {}", request.entitlementPayload());
            }
            
            // Store entitlement status
            entitlementStatuses.put(entitlementKey, EntitlementStatus.ACTIVE);
            
            // Create provider data
            Map<String, Object> providerData = Map.of(
                "provider", "mock",
                "simulatedAt", System.currentTimeMillis(),
                "action", "GRANT",
                "customerId", request.customerId().toString(),
                "productId", request.productId(),
                "planId", request.planId(),
                "periodStart", request.periodStart().toString(),
                "periodEnd", request.periodEnd().toString(),
                "grantedAt", OffsetDateTime.now().toString()
            );
            
            logger.info("[MOCK_ENTITLEMENT] Entitlements granted successfully: {}", externalEntitlementRef);
            
            return EntitlementResult.success(externalEntitlementRef, EntitlementStatus.ACTIVE, providerData);
            
        } catch (Exception e) {
            logger.error("[MOCK_ENTITLEMENT] Failed to grant entitlements for customer {} product {}: {}", 
                        request.customerId(), request.productId(), e.getMessage(), e);
            return EntitlementResult.failure("mock_grant_error", "Mock entitlement grant failed: " + e.getMessage());
        }
    }
    
    @Override
    public EntitlementResult suspendEntitlements(EntitlementRequest request) {
        logger.info("[MOCK_ENTITLEMENT] Suspending entitlements for customer {} product {}", 
                   request.customerId(), request.productId());
        
        try {
            String entitlementKey = generateEntitlementKey(request.customerId(), request.productId());
            String externalEntitlementRef = "mock_suspend_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Update entitlement status
            entitlementStatuses.put(entitlementKey, EntitlementStatus.SUSPENDED);
            
            Map<String, Object> providerData = Map.of(
                "provider", "mock",
                "simulatedAt", System.currentTimeMillis(),
                "action", "SUSPEND",
                "customerId", request.customerId().toString(),
                "productId", request.productId(),
                "suspendedAt", OffsetDateTime.now().toString()
            );
            
            logger.info("[MOCK_ENTITLEMENT] Entitlements suspended successfully: {}", externalEntitlementRef);
            
            return EntitlementResult.success(externalEntitlementRef, EntitlementStatus.SUSPENDED, providerData);
            
        } catch (Exception e) {
            logger.error("[MOCK_ENTITLEMENT] Failed to suspend entitlements for customer {} product {}: {}", 
                        request.customerId(), request.productId(), e.getMessage(), e);
            return EntitlementResult.failure("mock_suspend_error", "Mock entitlement suspend failed: " + e.getMessage());
        }
    }
    
    @Override
    public EntitlementResult revokeEntitlements(EntitlementRequest request) {
        logger.info("[MOCK_ENTITLEMENT] Revoking entitlements for customer {} product {}", 
                   request.customerId(), request.productId());
        
        try {
            String entitlementKey = generateEntitlementKey(request.customerId(), request.productId());
            String externalEntitlementRef = "mock_revoke_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Update entitlement status
            entitlementStatuses.put(entitlementKey, EntitlementStatus.REVOKED);
            
            Map<String, Object> providerData = Map.of(
                "provider", "mock",
                "simulatedAt", System.currentTimeMillis(),
                "action", "REVOKE",
                "customerId", request.customerId().toString(),
                "productId", request.productId(),
                "revokedAt", OffsetDateTime.now().toString()
            );
            
            logger.info("[MOCK_ENTITLEMENT] Entitlements revoked successfully: {}", externalEntitlementRef);
            
            return EntitlementResult.success(externalEntitlementRef, EntitlementStatus.REVOKED, providerData);
            
        } catch (Exception e) {
            logger.error("[MOCK_ENTITLEMENT] Failed to revoke entitlements for customer {} product {}: {}", 
                        request.customerId(), request.productId(), e.getMessage(), e);
            return EntitlementResult.failure("mock_revoke_error", "Mock entitlement revoke failed: " + e.getMessage());
        }
    }
    
    @Override
    public EntitlementStatus getEntitlementStatus(UUID customerId, String productId) {
        String entitlementKey = generateEntitlementKey(customerId, productId);
        EntitlementStatus status = entitlementStatuses.getOrDefault(entitlementKey, EntitlementStatus.PENDING);
        
        logger.debug("[MOCK_ENTITLEMENT] Getting status for customer {} product {}: {}", 
                    customerId, productId, status);
        
        return status;
    }
    
    @Override
    public String getProviderName() {
        return "mock";
    }
    
    @Override
    public boolean supportsEntitlementType(String entitlementType) {
        // Mock adapter supports all entitlement types
        return true;
    }
    
    /**
     * Generate a unique key for entitlement tracking.
     */
    private String generateEntitlementKey(UUID customerId, String productId) {
        return customerId.toString() + ":" + productId;
    }
}
