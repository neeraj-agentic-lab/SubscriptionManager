package com.subscriptionengine.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.Users.USERS;
import static com.subscriptionengine.generated.tables.UserTenants.USER_TENANTS;

/**
 * Filter to validate JWT claims against the database.
 * This ensures that JWT tokens contain valid user_id, role, and tenant access.
 * 
 * Security: Prevents fake JWT tokens with fabricated claims from being accepted.
 * 
 * @author Neeraj Yadav
 */
@Component
public class JwtClaimsValidationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtClaimsValidationFilter.class);
    
    private final DSLContext dsl;
    
    public JwtClaimsValidationFilter(DSLContext dsl) {
        this.dsl = dsl;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Only validate JWT tokens, not API Key authentication
            if (authentication instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
                Jwt jwt = jwtAuth.getToken();
                
                logger.info("=== JWT Claims Validation START - {} {} ===", request.getMethod(), request.getRequestURI());
                
                // Extract claims - support both claim formats
                // AuthController uses subject for userId and "tenantId" (camelCase)
                // JwtTestHelper uses "user_id" and "tenant_id" (snake_case)
                String userId = jwt.getSubject();
                if (userId == null || userId.isEmpty()) {
                    userId = jwt.getClaimAsString("user_id");
                }
                String email = jwt.getClaimAsString("email");
                String role = jwt.getClaimAsString("role");
                String tenantId = jwt.getClaimAsString("tenantId");
                if (tenantId == null || tenantId.isEmpty()) {
                    tenantId = jwt.getClaimAsString("tenant_id");
                }
                
                logger.info("JWT Claims - user_id: {}, email: {}, role: {}, tenant_id: {}", userId, email, role, tenantId);
                
                // Validate user_id is present
                if (userId == null || userId.isEmpty()) {
                    logger.error("JWT validation failed: user_id claim is missing");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"JWT token missing user_id claim\"}");
                    return;
                }
                
                UUID userUuid;
                try {
                    userUuid = UUID.fromString(userId);
                } catch (IllegalArgumentException e) {
                    logger.error("JWT validation failed: invalid user_id format: {}", userId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Invalid user_id format in JWT\"}");
                    return;
                }
                
                // Query database to validate user exists
                var userRecord = dsl.selectFrom(USERS)
                    .where(USERS.ID.eq(userUuid))
                    .fetchOne();
                
                if (userRecord == null) {
                    logger.error("JWT validation failed: user_id {} does not exist in database", userId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"User does not exist\"}");
                    return;
                }
                
                // Validate user status is ACTIVE
                if (!"ACTIVE".equals(userRecord.getStatus())) {
                    logger.error("JWT validation failed: user {} status is {}", userId, userRecord.getStatus());
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"ACCOUNT_SUSPENDED\",\"message\":\"User account is " + userRecord.getStatus().toLowerCase() + "\"}");
                    return;
                }
                
                // Validate role matches (if role claim is present)
                if (role != null && !role.isEmpty()) {
                    // For SUPER_ADMIN, check the users table role
                    if ("SUPER_ADMIN".equals(role)) {
                        if (!"SUPER_ADMIN".equals(userRecord.getRole())) {
                            logger.error("JWT validation failed: user {} claims SUPER_ADMIN but actual role is {}", userId, userRecord.getRole());
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Role claim does not match user role\"}");
                            return;
                        }
                    } else {
                        // For tenant-specific roles, validate against user_tenants table
                        if (tenantId != null && !tenantId.isEmpty()) {
                            UUID tenantUuid;
                            try {
                                tenantUuid = UUID.fromString(tenantId);
                            } catch (IllegalArgumentException e) {
                                logger.error("JWT validation failed: invalid tenant_id format: {}", tenantId);
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Invalid tenant_id format in JWT\"}");
                                return;
                            }
                            
                            var userTenantRecord = dsl.selectFrom(USER_TENANTS)
                                .where(USER_TENANTS.USER_ID.eq(userUuid))
                                .and(USER_TENANTS.TENANT_ID.eq(tenantUuid))
                                .fetchOne();
                            
                            if (userTenantRecord == null) {
                                logger.error("JWT validation failed: user {} does not have access to tenant {}", userId, tenantId);
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"User does not have access to this tenant\"}");
                                return;
                            }
                            
                            if (!role.equals(userTenantRecord.getRole())) {
                                logger.error("JWT validation failed: user {} claims role {} but actual role for tenant {} is {}", 
                                    userId, role, tenantId, userTenantRecord.getRole());
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Role claim does not match user's tenant role\"}");
                                return;
                            }
                        }
                    }
                }
                
                logger.info("JWT claims validation PASSED for user: {}", userId);
            }
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("JWT claims validation error: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"VALIDATION_ERROR\",\"message\":\"Error validating JWT claims\"}");
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip validation for public endpoints
        boolean shouldSkip = path.startsWith("/actuator/") ||
                           path.startsWith("/v3/api-docs") ||
                           path.startsWith("/swagger-ui") ||
                           path.startsWith("/v1/auth/login") ||
                           path.startsWith("/v1/public/") ||
                           path.equals("/favicon.ico");
        
        return shouldSkip;
    }
}
