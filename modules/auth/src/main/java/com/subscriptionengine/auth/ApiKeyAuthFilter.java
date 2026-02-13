package com.subscriptionengine.auth;

import com.subscriptionengine.generated.tables.pojos.ApiClients;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.subscriptionengine.generated.tables.ApiClients.API_CLIENTS;

/**
 * Spring Security filter for API Key + HMAC authentication.
 * Validates client credentials, signature, timestamp, nonce, IP whitelist, and rate limits.
 * 
 * @author Neeraj Yadav
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    
    private static final String HEADER_CLIENT_ID = "X-Client-ID";
    private static final String HEADER_TIMESTAMP = "X-Timestamp";
    private static final String HEADER_NONCE = "X-Nonce";
    private static final String HEADER_SIGNATURE = "X-Signature";
    
    private final DSLContext dsl;
    private final SignatureService signatureService;
    private final NonceCache nonceCache;
    private final RateLimiter rateLimiter;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public ApiKeyAuthFilter(
            DSLContext dsl,
            SignatureService signatureService,
            NonceCache nonceCache,
            RateLimiter rateLimiter) {
        this.dsl = dsl;
        this.signatureService = signatureService;
        this.nonceCache = nonceCache;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Extract API Key headers
        String clientId = request.getHeader(HEADER_CLIENT_ID);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String signature = request.getHeader(HEADER_SIGNATURE);
        
        // Skip if not API Key authentication
        if (clientId == null || timestamp == null || nonce == null || signature == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // 1. Fetch API client
            ApiClients client = dsl.selectFrom(API_CLIENTS)
                    .where(API_CLIENTS.CLIENT_ID.eq(clientId))
                    .fetchOneInto(ApiClients.class);
            
            if (client == null) {
                logger.warn("API client not found: {}", clientId);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid client credentials");
                return;
            }
            
            // 2. Check client status
            if (!"ACTIVE".equals(client.getStatus())) {
                logger.warn("API client not active: {} (status: {})", clientId, client.getStatus());
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Client is not active");
                return;
            }
            
            // 3. Check auth method
            if (!"API_KEY".equals(client.getAuthMethod())) {
                logger.warn("Invalid auth method for client: {} (expected API_KEY, got: {})", 
                        clientId, client.getAuthMethod());
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication method");
                return;
            }
            
            // 4. Validate timestamp (prevent old requests)
            if (!signatureService.validateTimestamp(timestamp)) {
                logger.warn("Invalid timestamp for client: {}", clientId);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Request timestamp expired");
                return;
            }
            
            // 5. Check nonce (prevent replay attacks)
            if (!nonceCache.checkAndStore(clientId, nonce)) {
                logger.warn("Replay attack detected for client: {}", clientId);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Request already processed");
                return;
            }
            
            // 6. Verify HMAC signature
            String requestBody = getRequestBody(request);
            String canonicalRequest = signatureService.generateCanonicalRequest(
                    request.getMethod(),
                    request.getRequestURI(),
                    timestamp,
                    nonce,
                    requestBody
            );
            
            if (!signatureService.verifySignature(signature, canonicalRequest, client.getClientSecretHash())) {
                logger.warn("Invalid signature for client: {}", clientId);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                return;
            }
            
            // 7. Check IP whitelist (if configured)
            if (client.getAllowedIps() != null && client.getAllowedIps().length > 0) {
                String clientIp = getClientIp(request);
                boolean ipAllowed = Arrays.stream(client.getAllowedIps())
                        .anyMatch(allowedIp -> allowedIp.toString().equals(clientIp));
                
                if (!ipAllowed) {
                    logger.warn("IP not whitelisted for client: {}. IP: {}", clientId, clientIp);
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "IP address not allowed");
                    return;
                }
            }
            
            // 8. Check rate limit
            if (!rateLimiter.allowRequest(clientId, client.getRateLimitPerHour())) {
                long remaining = rateLimiter.getRemainingRequests(clientId, client.getRateLimitPerHour());
                logger.warn("Rate limit exceeded for client: {}", clientId);
                response.setHeader("X-RateLimit-Limit", String.valueOf(client.getRateLimitPerHour()));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                sendErrorResponse(response, 429, "Rate limit exceeded");
                return;
            }
            
            // 9. Validate scopes for requested endpoint (basic implementation)
            // TODO: Implement more granular scope checking based on endpoint
            
            // 10. Set authentication context
            List<SimpleGrantedAuthority> authorities = Arrays.stream(client.getAllowedScopes())
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());
            
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    clientId,
                    null,
                    authorities
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 11. Update last_used_at timestamp
            updateLastUsedAt(client.getId());
            
            // Add rate limit headers to response
            long remaining = rateLimiter.getRemainingRequests(clientId, client.getRateLimitPerHour());
            response.setHeader("X-RateLimit-Limit", String.valueOf(client.getRateLimitPerHour()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            
            logger.debug("API Key authentication successful for client: {}", clientId);
            
        } catch (Exception e) {
            logger.error("Error during API Key authentication", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Get request body (for signature verification).
     */
    private String getRequestBody(HttpServletRequest request) {
        // TODO: Implement request body reading with caching
        // For now, return empty string (works for GET requests)
        return "";
    }
    
    /**
     * Get client IP address from request.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Update last_used_at timestamp for API client.
     */
    private void updateLastUsedAt(java.util.UUID clientId) {
        try {
            dsl.update(API_CLIENTS)
                    .set(API_CLIENTS.LAST_USED_AT, LocalDateTime.now())
                    .set(API_CLIENTS.TOTAL_REQUESTS, API_CLIENTS.TOTAL_REQUESTS.plus(1))
                    .where(API_CLIENTS.ID.eq(clientId))
                    .execute();
        } catch (Exception e) {
            logger.error("Error updating last_used_at for client: {}", clientId, e);
        }
    }
    
    /**
     * Send error response.
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }
}
