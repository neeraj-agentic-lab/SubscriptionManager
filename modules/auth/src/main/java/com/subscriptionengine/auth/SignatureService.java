package com.subscriptionengine.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service for HMAC-SHA256 signature generation and verification.
 * Used for API Key authentication with request signing.
 * 
 * @author Neeraj Yadav
 */
@Service
public class SignatureService {
    
    private static final Logger logger = LoggerFactory.getLogger(SignatureService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int TIMESTAMP_TOLERANCE_SECONDS = 300; // 5 minutes
    
    /**
     * Generate canonical request string for signing.
     * Format: HTTP_METHOD\nREQUEST_PATH\nTIMESTAMP\nNONCE\nBODY_HASH
     */
    public String generateCanonicalRequest(
            String httpMethod,
            String requestPath,
            String timestamp,
            String nonce,
            String requestBody) {
        
        String bodyHash = hashRequestBody(requestBody);
        
        return String.join("\n",
                httpMethod.toUpperCase(),
                requestPath,
                timestamp,
                nonce,
                bodyHash
        );
    }
    
    /**
     * Generate canonical request string with query parameters.
     * Format: HTTP_METHOD\nREQUEST_PATH\nQUERY_STRING\nTIMESTAMP\nNONCE\nBODY_HASH
     */
    public String generateCanonicalRequestWithQuery(
            String httpMethod,
            String requestPath,
            Map<String, String> queryParams,
            String timestamp,
            String nonce,
            String requestBody) {
        
        String canonicalQueryString = buildCanonicalQueryString(queryParams);
        String bodyHash = hashRequestBody(requestBody);
        
        return String.join("\n",
                httpMethod.toUpperCase(),
                requestPath,
                canonicalQueryString,
                timestamp,
                nonce,
                bodyHash
        );
    }
    
    /**
     * Generate HMAC-SHA256 signature.
     */
    public String generateSignature(String canonicalRequest, String clientSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    clientSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            
            byte[] signatureBytes = mac.doFinal(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error generating HMAC signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }
    
    /**
     * Verify HMAC-SHA256 signature.
     */
    public boolean verifySignature(
            String providedSignature,
            String canonicalRequest,
            String clientSecret) {
        
        String expectedSignature = generateSignature(canonicalRequest, clientSecret);
        return MessageDigest.isEqual(
                providedSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
    
    /**
     * Validate timestamp is within acceptable window (5 minutes).
     */
    public boolean validateTimestamp(String timestamp) {
        try {
            long requestTimestamp = Long.parseLong(timestamp);
            long currentTimestamp = Instant.now().getEpochSecond();
            long difference = Math.abs(currentTimestamp - requestTimestamp);
            
            boolean isValid = difference <= TIMESTAMP_TOLERANCE_SECONDS;
            
            if (!isValid) {
                logger.warn("Timestamp validation failed. Difference: {} seconds (max: {})",
                        difference, TIMESTAMP_TOLERANCE_SECONDS);
            }
            
            return isValid;
            
        } catch (NumberFormatException e) {
            logger.error("Invalid timestamp format: {}", timestamp);
            return false;
        }
    }
    
    /**
     * Hash request body using SHA-256.
     */
    private String hashRequestBody(String requestBody) {
        if (requestBody == null || requestBody.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Error hashing request body", e);
            throw new RuntimeException("Failed to hash request body", e);
        }
    }
    
    /**
     * Build canonical query string from parameters.
     * Parameters are sorted alphabetically and URL-encoded.
     */
    private String buildCanonicalQueryString(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }
        
        TreeMap<String, String> sortedParams = new TreeMap<>(queryParams);
        StringBuilder canonical = new StringBuilder();
        
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (canonical.length() > 0) {
                canonical.append("&");
            }
            canonical.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }
        
        return canonical.toString();
    }
    
    /**
     * Generate current Unix timestamp.
     */
    public String getCurrentTimestamp() {
        return String.valueOf(Instant.now().getEpochSecond());
    }
    
    /**
     * Generate a random nonce for request uniqueness.
     */
    public String generateNonce() {
        return java.util.UUID.randomUUID().toString();
    }
}
