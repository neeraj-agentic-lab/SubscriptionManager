package com.subscriptionengine.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Utility class to extract user information from the current security context.
 * Provides access to user ID, email, and role from JWT token.
 * 
 * @author Neeraj Yadav
 */
public class UserContext {
    
    /**
     * Get the current user ID from JWT token.
     * @return user ID or null if not authenticated
     */
    public static UUID getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            // Try 'sub' claim first (AuthController sets userId as subject)
            String userId = jwt.getSubject();
            if (userId == null || userId.isEmpty()) {
                // Fall back to 'user_id' claim (snake_case format)
                userId = jwt.getClaimAsString("user_id");
            }
            return userId != null ? UUID.fromString(userId) : null;
        }
        return null;
    }
    
    /**
     * Get the current user email from JWT token.
     * @return user email or null if not authenticated
     */
    public static String getUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("email");
        }
        return null;
    }
    
    /**
     * Get the current user role from JWT token.
     * @return user role or null if not authenticated
     */
    public static String getUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("role");
        }
        return null;
    }
    
    /**
     * Get the current customer ID from JWT token (for customer users).
     * @return customer ID or null if not a customer or not authenticated
     */
    public static UUID getCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String customerId = jwt.getClaimAsString("customer_id");
            return customerId != null ? UUID.fromString(customerId) : null;
        }
        return null;
    }
    
    /**
     * Check if current user is authenticated.
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof Jwt;
    }
}
