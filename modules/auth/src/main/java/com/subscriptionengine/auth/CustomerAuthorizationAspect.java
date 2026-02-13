package com.subscriptionengine.auth;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Parameter;
import java.util.UUID;

/**
 * Aspect for enforcing customer authorization on customer-facing endpoints.
 * Verifies that customers can only access their own resources.
 * Allows admin override for support purposes.
 * 
 * Only active when CustomerSubscriptionsController is present (API module).
 * 
 * @author Neeraj Yadav
 */
@Aspect
@Component
@ConditionalOnClass(name = "com.subscriptionengine.api.controller.CustomerSubscriptionsController")
public class CustomerAuthorizationAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerAuthorizationAspect.class);
    
    private final JwtTenantExtractor jwtTenantExtractor;
    
    public CustomerAuthorizationAspect(JwtTenantExtractor jwtTenantExtractor) {
        this.jwtTenantExtractor = jwtTenantExtractor;
    }
    
    /**
     * Intercept all customer controller methods and verify customer authorization.
     * Applies to CustomerSubscriptionsController methods.
     */
    @Before("execution(* com.subscriptionengine.api.controller.CustomerSubscriptionsController.*(..)) && args(customerId,..)")
    public void checkCustomerAuthorization(JoinPoint joinPoint, UUID customerId) {
        String methodName = joinPoint.getSignature().toShortString();
        logger.debug("CustomerAuthorizationAspect: Checking authorization for {} with customerId {}", 
                    methodName, customerId);
        
        // Get current authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            logger.warn("CustomerAuthorizationAspect: No JWT authentication found for {}", methodName);
            return; // Let Spring Security handle authentication
        }
        
        Jwt jwt = jwtAuth.getToken();
        
        // Extract user role
        String roleStr = jwtTenantExtractor.extractUserRole(jwt);
        UserRole userRole = UserRole.fromString(roleStr);
        
        if (userRole == null) {
            logger.error("CustomerAuthorizationAspect: No valid role found in JWT for {}", methodName);
            throw new AccessDeniedException("User role not found in token");
        }
        
        // Admin users can access any customer's data (for support purposes)
        if (userRole.isAdmin()) {
            logger.debug("CustomerAuthorizationAspect: Admin access granted for {} accessing customer {}", 
                        userRole, customerId);
            return;
        }
        
        // For customer users, verify they own the resource
        if (userRole.isCustomer()) {
            UUID tokenCustomerId = jwtTenantExtractor.extractCustomerId(jwt);
            
            if (tokenCustomerId == null) {
                String userId = jwtTenantExtractor.extractUserId(jwt);
                logger.error("CustomerAuthorizationAspect: Customer {} has no customer_id in token", userId);
                throw new AccessDeniedException("Customer ID not found in token");
            }
            
            if (!tokenCustomerId.equals(customerId)) {
                logger.error("CustomerAuthorizationAspect: Customer {} attempted to access customer {}'s data", 
                            tokenCustomerId, customerId);
                throw new AccessDeniedException("Access denied: You can only access your own data");
            }
            
            logger.debug("CustomerAuthorizationAspect: Customer access granted for {} accessing own data", 
                        tokenCustomerId);
            return;
        }
        
        // Unknown role
        logger.error("CustomerAuthorizationAspect: Unknown role {} attempted to access customer endpoint", userRole);
        throw new AccessDeniedException("Invalid role for customer endpoint: " + userRole);
    }
}
