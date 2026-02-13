package com.subscriptionengine.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for multi-tier authentication.
 * Supports: JWT (OAuth 2.0), API Key + HMAC, and mTLS.
 * 
 * @author Neeraj Yadav
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;
    
    @Value("${jwt.secret:dev-secret-key-not-for-production}")
    private String jwtSecret;
    
    private final JwtTenantAuthenticationFilter jwtTenantAuthenticationFilter;
    private final ApiKeyAuthFilter apiKeyAuthFilter;
    
    public SecurityConfig(
            JwtTenantAuthenticationFilter jwtTenantAuthenticationFilter,
            ApiKeyAuthFilter apiKeyAuthFilter) {
        this.jwtTenantAuthenticationFilter = jwtTenantAuthenticationFilter;
        this.apiKeyAuthFilter = apiKeyAuthFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (health checks, documentation)
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs.yaml").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Configure OAuth 2.0 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                )
            )
            
            // Add API Key authentication filter before JWT authentication
            // This allows API Key auth to take precedence when X-Client-ID header is present
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Add tenant extraction filter after OAuth2 JWT authentication
            .addFilterAfter(jwtTenantAuthenticationFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // For development/testing: Use symmetric key validation
        // In production, use proper JWK Set URI or issuer URI
        SecretKeySpec secretKey = new SecretKeySpec(
            jwtSecret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}
