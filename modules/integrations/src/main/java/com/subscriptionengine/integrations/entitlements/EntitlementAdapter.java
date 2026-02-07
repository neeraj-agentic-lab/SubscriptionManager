/**
 * Entitlement adapter interface for managing digital product access.
 * Provides extensible design for integrating with various entitlement providers.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.integrations.entitlements;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for entitlement management adapters.
 * Implementations handle integration with specific entitlement providers.
 */
public interface EntitlementAdapter {
    
    /**
     * Grant entitlements for a subscription period.
     * 
     * @param request Entitlement grant request
     * @return Entitlement operation result
     */
    EntitlementResult grantEntitlements(EntitlementRequest request);
    
    /**
     * Suspend entitlements (temporary revocation).
     * 
     * @param request Entitlement suspend request
     * @return Entitlement operation result
     */
    EntitlementResult suspendEntitlements(EntitlementRequest request);
    
    /**
     * Revoke entitlements permanently.
     * 
     * @param request Entitlement revoke request
     * @return Entitlement operation result
     */
    EntitlementResult revokeEntitlements(EntitlementRequest request);
    
    /**
     * Get current entitlement status.
     * 
     * @param customerId Customer ID
     * @param productId Product ID
     * @return Current entitlement status
     */
    EntitlementStatus getEntitlementStatus(UUID customerId, String productId);
    
    /**
     * Get the provider name for this adapter.
     * 
     * @return Provider name (e.g., "auth0", "firebase", "mock")
     */
    String getProviderName();
    
    /**
     * Check if this adapter supports the given entitlement type.
     * 
     * @param entitlementType Entitlement type (e.g., "api_access", "feature_flag", "license")
     * @return true if supported
     */
    boolean supportsEntitlementType(String entitlementType);
    
    /**
     * Entitlement operation request.
     */
    record EntitlementRequest(
        UUID customerId,
        UUID subscriptionId,
        UUID invoiceId,
        String customerEmail,
        String productId,
        String planId,
        EntitlementAction action,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        Map<String, Object> entitlementPayload,
        String idempotencyKey,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Entitlement operation result.
     */
    record EntitlementResult(
        boolean success,
        String externalEntitlementRef,
        EntitlementStatus status,
        String errorCode,
        String errorMessage,
        Map<String, Object> providerData
    ) {
        
        /**
         * Create successful entitlement result.
         */
        public static EntitlementResult success(String externalEntitlementRef, EntitlementStatus status, Map<String, Object> providerData) {
            return new EntitlementResult(true, externalEntitlementRef, status, null, null, providerData);
        }
        
        /**
         * Create failed entitlement result.
         */
        public static EntitlementResult failure(String errorCode, String errorMessage) {
            return new EntitlementResult(false, null, EntitlementStatus.FAILED, errorCode, errorMessage, Map.of());
        }
        
        /**
         * Create failed entitlement result with provider data.
         */
        public static EntitlementResult failure(String errorCode, String errorMessage, Map<String, Object> providerData) {
            return new EntitlementResult(false, null, EntitlementStatus.FAILED, errorCode, errorMessage, providerData);
        }
    }
    
    /**
     * Entitlement action enumeration.
     */
    enum EntitlementAction {
        GRANT,      // Grant new entitlements
        SUSPEND,    // Temporarily suspend entitlements
        REVOKE,     // Permanently revoke entitlements
        RENEW       // Renew existing entitlements
    }
    
    /**
     * Entitlement status enumeration.
     */
    enum EntitlementStatus {
        ACTIVE,     // Entitlements are active
        SUSPENDED,  // Entitlements are temporarily suspended
        REVOKED,    // Entitlements are permanently revoked
        EXPIRED,    // Entitlements have expired
        PENDING,    // Entitlements are being processed
        FAILED      // Entitlement operation failed
    }
}
