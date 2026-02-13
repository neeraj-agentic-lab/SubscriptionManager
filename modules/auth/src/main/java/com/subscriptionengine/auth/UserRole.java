package com.subscriptionengine.auth;

/**
 * User roles for authorization.
 * 
 * @author Neeraj Yadav
 */
public enum UserRole {
    /**
     * Super admin with access to all tenants and system-wide operations
     */
    SUPER_ADMIN,
    
    /**
     * Tenant administrator with full access to their tenant's data
     */
    TENANT_ADMIN,
    
    /**
     * Staff member with limited admin access to their tenant
     */
    STAFF,
    
    /**
     * Customer with access only to their own subscriptions and data
     */
    CUSTOMER;
    
    /**
     * Check if this role is an admin role (SUPER_ADMIN, TENANT_ADMIN, or STAFF)
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == TENANT_ADMIN || this == STAFF;
    }
    
    /**
     * Check if this role is a customer role
     */
    public boolean isCustomer() {
        return this == CUSTOMER;
    }
    
    /**
     * Parse role from string, returns null if invalid
     */
    public static UserRole fromString(String roleStr) {
        if (roleStr == null) {
            return null;
        }
        
        try {
            return UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
