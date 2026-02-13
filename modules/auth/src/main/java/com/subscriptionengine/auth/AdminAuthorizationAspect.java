package com.subscriptionengine.auth;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect for enforcing admin authorization on admin endpoints.
 * Verifies that the user has an admin role (SUPER_ADMIN, TENANT_ADMIN, or STAFF)
 * and has access to the requested tenant.
 * 
 * @author Neeraj Yadav
 */
@Aspect
@Component
public class AdminAuthorizationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthorizationAspect.class);
    
    private final JwtTenantExtractor jwtTenantExtractor;
    
    public AdminAuthorizationAspect(JwtTenantExtractor jwtTenantExtractor) {
        this.jwtTenantExtractor = jwtTenantExtractor;
    }
    
    /**
     * Intercept all admin controller methods and verify admin authorization.
     * Applies to all methods in controllers under com.subscriptionengine.api.controller
     * that have paths starting with /v1/admin/
     */
    @Before("execution(* com.subscriptionengine.api.controller.*.*(..)) && " +
            "@within(org.springframework.web.bind.annotation.RestController) && " +
            "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void checkAdminAuthorization(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        logger.debug("AdminAuthorizationAspect: Checking authorization for {}", methodName);
        
        // Get current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            logger.warn("AdminAuthorizationAspect: No JWT authentication found for {}", methodName);
            return; // Let Spring Security handle authentication
        }
        
        Jwt jwt = jwtAuth.getToken();
        
        // Extract user role
        String roleStr = jwtTenantExtractor.extractUserRole(jwt);
        UserRole userRole = UserRole.fromString(roleStr);
        
        if (userRole == null) {
            logger.error("AdminAuthorizationAspect: No valid role found in JWT for {}", methodName);
            throw new AccessDeniedException("User role not found in token");
        }
        
        // Check if user has admin role
        if (!userRole.isAdmin()) {
            String userId = jwtTenantExtractor.extractUserId(jwt);
            logger.error("AdminAuthorizationAspect: User {} with role {} attempted to access admin endpoint {}", 
                        userId, userRole, methodName);
            throw new AccessDeniedException("Admin access required. User role: " + userRole);
        }
        
        // Verify tenant access
        UUID tokenTenantId = jwtTenantExtractor.extractTenantId(jwt);
        UUID contextTenantId = TenantContext.getTenantId();
        
        // SUPER_ADMIN can access any tenant
        if (userRole == UserRole.SUPER_ADMIN) {
            logger.debug("AdminAuthorizationAspect: SUPER_ADMIN access granted for {}", methodName);
            return;
        }
        
        // TENANT_ADMIN and STAFF must match tenant
        if (contextTenantId != null && !tokenTenantId.equals(contextTenantId)) {
            String userId = jwtTenantExtractor.extractUserId(jwt);
            logger.error("AdminAuthorizationAspect: User {} with role {} attempted to access different tenant. Token tenant: {}, Context tenant: {}", 
                        userId, userRole, tokenTenantId, contextTenantId);
            throw new AccessDeniedException("Access denied to tenant: " + contextTenantId);
        }
        
        String userId = jwtTenantExtractor.extractUserId(jwt);
        logger.debug("AdminAuthorizationAspect: Admin access granted for user {} with role {} on {}", 
                    userId, userRole, methodName);
    }
}
