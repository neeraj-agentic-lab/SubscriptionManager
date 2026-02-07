package com.subscriptionengine.auth;

import com.subscriptionengine.auth.TenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to clean up tenant context after request processing is complete.
 * This runs with the lowest precedence to ensure it executes after all other processing.
 * 
 * @author Neeraj Yadav
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class TenantContextCleanupFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantContextCleanupFilter.class);
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        try {
            // Continue with the filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Always clear tenant context after request processing is complete
            if (TenantContext.isSet()) {
                logger.debug("Clearing tenant context after request completion");
                TenantContext.clear();
            }
        }
    }
}
