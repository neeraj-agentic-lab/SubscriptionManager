package com.subscriptionengine.api.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for generating JWT tokens in tests.
 * 
 * @author Neeraj Yadav
 */
public class JwtTestHelper {
    
    // Must match the secret in SecurityConfig and test-jwt-token.py
    private static final String SECRET = "dev-secret-key-not-for-production";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    
    /**
     * Generate a JWT token for testing with the given tenant ID.
     */
    public static String generateToken(String tenantId) {
        return generateToken(tenantId, UUID.randomUUID().toString(), "test@example.com");
    }
    
    /**
     * Generate a JWT token with custom claims.
     */
    public static String generateToken(String tenantId, String userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant_id", tenantId);
        claims.put("user_id", userId);
        claims.put("email", email);
        claims.put("roles", new String[]{"USER"});
        
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiration = new Date(nowMillis + 3600000); // 1 hour
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(KEY, SignatureAlgorithm.HS256)
            .compact();
    }
}
