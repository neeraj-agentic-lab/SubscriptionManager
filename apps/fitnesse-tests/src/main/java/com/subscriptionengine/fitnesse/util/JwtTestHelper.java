package com.subscriptionengine.fitnesse.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JwtTestHelper {
    
    private static final String SECRET = "dev-secret-key-not-for-production";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    
    public static String generateToken(String tenantId) {
        return generateToken(tenantId, UUID.randomUUID().toString(), "test@example.com");
    }
    
    public static String generateToken(String tenantId, String userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant_id", tenantId);
        claims.put("user_id", userId);
        claims.put("email", email);
        claims.put("roles", new String[]{"USER"});
        
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiration = new Date(nowMillis + 3600000);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(KEY, SignatureAlgorithm.HS256)
            .compact();
    }
    
    public static String generateAdminToken(String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant_id", tenantId);
        claims.put("user_id", UUID.randomUUID().toString());
        claims.put("email", "admin@example.com");
        claims.put("role", "SUPER_ADMIN");
        
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiration = new Date(nowMillis + 3600000);
        
        return Jwts.builder()
            .setClaims(claims)
            .setSubject("admin")
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(KEY, SignatureAlgorithm.HS256)
            .compact();
    }
}
