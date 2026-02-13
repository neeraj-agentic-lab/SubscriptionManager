package com.subscriptionengine.auth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Extracts tenant information from JWT tokens for multi-tenant isolation.
 * Supports both direct tenant_id claims and organization-based tenant mapping.
 * 
 * @author Neeraj Yadav
 */
@Component
public class JwtTenantExtractor {
    
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String ORG_ID_CLAIM = "org_id";
    private static final String ORGANIZATION_CLAIM = "organization";
    
    /**
     * Extract tenant ID from JWT token
     * @param jwt the JWT token
     * @return tenant ID
     * @throws IllegalArgumentException if tenant ID cannot be extracted
     */
    public UUID extractTenantId(Jwt jwt) {
        // Try direct tenant_id claim first
        String tenantIdStr = jwt.getClaimAsString(TENANT_ID_CLAIM);
        if (tenantIdStr != null) {
            return parseUuid(tenantIdStr, TENANT_ID_CLAIM);
        }
        
        // Try org_id claim
        String orgIdStr = jwt.getClaimAsString(ORG_ID_CLAIM);
        if (orgIdStr != null) {
            return parseUuid(orgIdStr, ORG_ID_CLAIM);
        }
        
        // Try organization claim
        String organizationStr = jwt.getClaimAsString(ORGANIZATION_CLAIM);
        if (organizationStr != null) {
            return parseUuid(organizationStr, ORGANIZATION_CLAIM);
        }
        
        throw new IllegalArgumentException(
            "JWT token must contain a valid tenant identifier in one of: " +
            TENANT_ID_CLAIM + ", " + ORG_ID_CLAIM + ", " + ORGANIZATION_CLAIM
        );
    }
    
    /**
     * Extract user ID from JWT token
     * @param jwt the JWT token
     * @return user ID or null if not present
     */
    public String extractUserId(Jwt jwt) {
        // Try standard 'sub' claim first
        String subject = jwt.getSubject();
        if (subject != null && !subject.isEmpty()) {
            return subject;
        }
        
        // Try 'user_id' claim
        return jwt.getClaimAsString("user_id");
    }
    
    /**
     * Extract user email from JWT token
     * @param jwt the JWT token
     * @return user email or null if not present
     */
    public String extractUserEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }
    
    /**
     * Extract user role from JWT token
     * @param jwt the JWT token
     * @return user role (SUPER_ADMIN, TENANT_ADMIN, STAFF, CUSTOMER) or null if not present
     */
    public String extractUserRole(Jwt jwt) {
        // Try 'role' claim first
        String role = jwt.getClaimAsString("role");
        if (role != null) {
            return role;
        }
        
        // Try 'roles' array claim
        Object rolesObj = jwt.getClaim("roles");
        if (rolesObj instanceof java.util.List<?> roles && !roles.isEmpty()) {
            return roles.get(0).toString();
        }
        
        return null;
    }
    
    /**
     * Extract customer ID from JWT token (for customer-facing APIs)
     * @param jwt the JWT token
     * @return customer ID or null if not present
     */
    public UUID extractCustomerId(Jwt jwt) {
        String customerIdStr = jwt.getClaimAsString("customer_id");
        if (customerIdStr != null) {
            return parseUuid(customerIdStr, "customer_id");
        }
        return null;
    }
    
    /**
     * Check if the JWT token has required tenant claims
     * @param jwt the JWT token
     * @return true if tenant can be extracted
     */
    public boolean hasTenantClaim(Jwt jwt) {
        return jwt.getClaimAsString(TENANT_ID_CLAIM) != null ||
               jwt.getClaimAsString(ORG_ID_CLAIM) != null ||
               jwt.getClaimAsString(ORGANIZATION_CLAIM) != null;
    }
    
    private UUID parseUuid(String uuidStr, String claimName) {
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid UUID format in JWT claim '" + claimName + "': " + uuidStr, e
            );
        }
    }
}
