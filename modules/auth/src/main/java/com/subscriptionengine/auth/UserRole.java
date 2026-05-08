package com.subscriptionengine.auth;

/**
 * Unified user roles used across the entire system.
 * These roles are used in: users table, user_tenants table, JWT claims, and authorization aspects.
 * 
 * @author Neeraj Yadav
 */
public enum UserRole {
    /**
     * Super admin with access to all tenants and system-wide operations.
     * Only assigned at the users table level, not in user_tenants.
     */
    SUPER_ADMIN,
    
    /**
     * Tenant administrator with full access to their tenant's data
     */
    TENANT_ADMIN,
    
    /**
     * Tenant user with standard access to their tenant
     */
    TENANT_USER,
    
    /**
     * Customer with access only to their own subscriptions and data
     */
    CUSTOMER;
    
    /**
     * Check if this role is an admin role (SUPER_ADMIN, TENANT_ADMIN, or TENANT_USER)
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == TENANT_ADMIN || this == TENANT_USER;
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
