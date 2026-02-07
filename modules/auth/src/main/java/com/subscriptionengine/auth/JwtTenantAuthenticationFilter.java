package com.subscriptionengine.auth;

import com.subscriptionengine.auth.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

/**
 * Filter that extracts tenant information from JWT tokens and sets up TenantContext.
 * This filter runs after JWT authentication to ensure tenant isolation.
 * 
 * @author Neeraj Yadav
 */
@Component
public class JwtTenantAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtTenantAuthenticationFilter.class);
    
    private final JwtTenantExtractor jwtTenantExtractor;
    
    public JwtTenantAuthenticationFilter(JwtTenantExtractor jwtTenantExtractor) {
        this.jwtTenantExtractor = jwtTenantExtractor;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        try {
            logger.info("=== JwtTenantAuthenticationFilter.doFilterInternal() START - {} {} ===", 
                       request.getMethod(), request.getRequestURI());
            // Get the current authentication from Spring Security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            logger.info("Authentication type: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");
            
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                logger.info("JWT token found, extracting tenant information...");
                
                try {
                    // Extract tenant ID from JWT
                    UUID tenantId = jwtTenantExtractor.extractTenantId(jwt);
                    logger.info("Extracted tenant ID from JWT: {}", tenantId);
                    
                    // Set tenant context for this request
                    TenantContext.setTenantId(tenantId);
                    logger.info("Set tenant context: {}", tenantId);
                    
                    // Verify tenant context was set
                    UUID verifyTenantId = TenantContext.getTenantId();
                    logger.info("Verified tenant context: {}", verifyTenantId);
                    
                    // Log tenant context for debugging
                    String userId = jwtTenantExtractor.extractUserId(jwt);
                    String userEmail = jwtTenantExtractor.extractUserEmail(jwt);
                    logger.info("JwtTenantAuthenticationFilter: SUCCESSFULLY set tenant context - tenantId: {}, userId: {}, userEmail: {}, thread: {}", 
                               tenantId, userId, userEmail, Thread.currentThread().getName());
                    
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to extract tenant ID from JWT token: {}", e.getMessage(), e);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\":\"Invalid tenant information in token\"}");
                    response.setContentType("application/json");
                    return;
                }
            } else {
                logger.warn("No JWT authentication found in security context. Authentication: {}", authentication);
            }
            
            logger.info("Continuing with filter chain...");
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("=== JwtTenantAuthenticationFilter.doFilterInternal() EXCEPTION: {} ===", e.getMessage(), e);
            throw e;
        } finally {
            logger.info("=== JwtTenantAuthenticationFilter.doFilterInternal() END ===");
            // Don't clear tenant context here - let TenantContextCleanupFilter handle it
            // This ensures tenant context is available during method-level security evaluation
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Skip filter for health check and public endpoints
        String path = request.getRequestURI();
        boolean shouldSkip = path.startsWith("/actuator/health") || 
                           path.startsWith("/v1/public/") ||
                           path.equals("/favicon.ico");
        
        logger.info("JwtTenantAuthenticationFilter.shouldNotFilter() - Path: {}, ShouldSkip: {}", path, shouldSkip);
        return shouldSkip;
    }
}
