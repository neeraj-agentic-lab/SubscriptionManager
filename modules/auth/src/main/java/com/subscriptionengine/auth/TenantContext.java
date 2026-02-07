package com.subscriptionengine.auth;

import java.util.UUID;

/**
 * Thread-local context for tenant isolation in multi-tenant requests.
 * Stores the current tenant ID extracted from JWT token.
 * 
 * @author Neeraj Yadav
 */
public class TenantContext {
    
    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    
    /**
     * Set the tenant ID for the current request thread
     */
    public static void setTenantId(UUID tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    /**
     * Get the tenant ID for the current request thread
     * @return tenant ID or null if not set
     */
    public static UUID getTenantId() {
        return TENANT_ID.get();
    }
    
    /**
     * Get the tenant ID for the current request thread, throwing exception if not set
     * @return tenant ID
     * @throws IllegalStateException if tenant ID is not set
     */
    public static UUID getRequiredTenantId() {
        UUID tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID not set in current context");
        }
        return tenantId;
    }
    
    /**
     * Clear the tenant context for the current thread
     */
    public static void clear() {
        TENANT_ID.remove();
    }
    
    /**
     * Check if tenant context is set for the current thread
     */
    public static boolean isSet() {
        return TENANT_ID.get() != null;
    }
}
