package com.subscriptionengine.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for tenant-based security validation.
 * Validates that the current user has access to tenant resources.
 * 
 * @author Neeraj Yadav
 */
@Service
public class TenantSecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantSecurityService.class);
    
    private final JwtTenantExtractor jwtTenantExtractor;
    
    public TenantSecurityService(JwtTenantExtractor jwtTenantExtractor) {
        this.jwtTenantExtractor = jwtTenantExtractor;
    }
    
    /**
     * Check if the current user has access to the tenant in context.
     * This method is used by @TenantSecured annotation.
     * 
     * @return true if user has access to the current tenant
     */
    public boolean hasAccess() {
        try {
            logger.info("=== TenantSecurityService.hasAccess() START - Thread: {} ===", Thread.currentThread().getName());
            UUID tenantId = TenantContext.getTenantId();
            logger.info("TenantSecurityService: Retrieved tenant ID from context: {}", tenantId);
            
            // If tenant context is not set, try to extract directly from JWT as fallback
            if (tenantId == null) {
                logger.warn("TenantSecurityService: No tenant ID in context, attempting direct JWT extraction...");
                tenantId = extractTenantIdFromJwt();
                if (tenantId != null) {
                    logger.info("TenantSecurityService: Successfully extracted tenant ID from JWT: {}", tenantId);
                    // Set it in context for future use in this request
                    TenantContext.setTenantId(tenantId);
                } else {
                    logger.error("TenantSecurityService: TENANT ACCESS DENIED - No tenant ID available from context or JWT - Thread: {}", 
                               Thread.currentThread().getName());
                    return false;
                }
            }
            
            // In a real implementation, you might check:
            // 1. User's tenant memberships from database
            // 2. User's roles and permissions for the tenant
            // 3. Tenant status (active, suspended, etc.)
            
            // For now, we trust that if tenant ID was extracted from JWT, user has access
            logger.debug("Tenant access granted for tenant: {}", tenantId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error during tenant access check", e);
            return false;
        }
    }
    
    /**
     * Extract tenant ID directly from JWT token in SecurityContext as fallback.
     * 
     * @return tenant ID from JWT or null if not available
     */
    private UUID extractTenantIdFromJwt() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            logger.info("TenantSecurityService: Attempting JWT extraction, authentication type: {}", 
                       authentication != null ? authentication.getClass().getSimpleName() : "null");
            
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                logger.info("TenantSecurityService: JWT token found, extracting tenant ID...");
                UUID tenantId = jwtTenantExtractor.extractTenantId(jwt);
                logger.info("TenantSecurityService: Extracted tenant ID from JWT: {}", tenantId);
                return tenantId;
            } else {
                logger.warn("TenantSecurityService: No JWT authentication token found in security context");
                return null;
            }
            
        } catch (Exception e) {
            logger.error("TenantSecurityService: Failed to extract tenant ID from JWT: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Check if the current user has access to a specific tenant.
     * 
     * @param tenantId the tenant ID to check access for
     * @return true if user has access to the specified tenant
     */
    public boolean hasAccessToTenant(UUID tenantId) {
        if (tenantId == null) {
            return false;
        }
        
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            logger.warn("Tenant access check failed: No tenant ID in context");
            return false;
        }
        
        boolean hasAccess = currentTenantId.equals(tenantId);
        if (!hasAccess) {
            logger.warn("Tenant access denied: Current tenant {} does not match requested tenant {}", 
                       currentTenantId, tenantId);
        }
        
        return hasAccess;
    }
    
    /**
     * Validate that a resource belongs to the current tenant.
     * Throws SecurityException if validation fails.
     * 
     * @param resourceTenantId the tenant ID of the resource
     * @throws SecurityException if resource doesn't belong to current tenant
     */
    public void validateTenantAccess(UUID resourceTenantId) {
        if (!hasAccessToTenant(resourceTenantId)) {
            throw new SecurityException("Access denied: Resource does not belong to current tenant");
        }
    }
}
