package com.subscriptionengine.fitnesse.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * REST API client for FitNesse fixtures
 * 
 * Provides methods to interact with the Subscription Manager API
 */
@Slf4j
@Component
public class ApiClient {
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private String authToken;
    private String apiKey;
    private String apiSecret;
    private String tenantContext;
    private int lastStatusCode;
    private String lastErrorMessage;
    
    public ApiClient(WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public void setAuthToken(String token) {
        this.authToken = token;
    }
    
    public void setApiCredentials(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    public void setTenantContext(String tenantId) {
        this.tenantContext = tenantId;
    }
    
    public int getLastStatusCode() {
        return lastStatusCode;
    }
    
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
    
    public void clearAuth() {
        this.authToken = null;
        this.apiKey = null;
        this.apiSecret = null;
        this.tenantContext = null;
    }
    
    public <T> T get(String path, Class<T> responseType) {
        log.debug("GET {}", path);
        
        try {
            T result = webClient.get()
                .uri(path)
                .headers(this::addAuthHeaders)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    lastStatusCode = response.statusCode().value();
                    return response.bodyToMono(String.class)
                        .doOnNext(body -> lastErrorMessage = body)
                        .then(Mono.error(new RuntimeException("HTTP " + lastStatusCode)));
                })
                .bodyToMono(responseType)
                .timeout(Duration.ofSeconds(30))
                .block();
            lastStatusCode = 200;
            return result;
        } catch (Exception e) {
            log.error("GET {} failed: {}", path, e.getMessage());
            throw e;
        }
    }
    
    public <T> T post(String path, Object request, Class<T> responseType) {
        log.info("=== API REQUEST ===");
        log.info("POST {}", path);
        try {
            String json = objectMapper.writeValueAsString(request);
            log.info("Request Body: {}", json);
        } catch (Exception e) {
            log.info("Request Body: {}", request);
        }
        String authInfo = authToken != null ? "Bearer " + authToken.substring(0, Math.min(20, authToken.length())) + "..." : "None";
        log.info("Auth Token: {}", authInfo);
        
        try {
            T result = webClient.post()
                .uri(path)
                .headers(this::addAuthHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    lastStatusCode = response.statusCode().value();
                    return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            lastErrorMessage = body;
                            log.error("=== API ERROR RESPONSE ===");
                            log.error("Status: {}", lastStatusCode);
                            log.error("Error Body: {}", body);
                            return Mono.error(new RuntimeException("HTTP " + lastStatusCode + ": " + body));
                        });
                })
                .bodyToMono(responseType)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            lastStatusCode = 200;
            log.info("=== API SUCCESS RESPONSE ===");
            log.info("Status: 200");
            try {
                String json = objectMapper.writeValueAsString(result);
                log.info("Response Body: {}", json);
            } catch (Exception ex) {
                log.info("Response Body: {}", result);
            }
            return result;
        } catch (Exception e) {
            log.error("POST {} failed: {}", path, e.getMessage(), e);
            throw e;
        }
    }
    
    public <T> T put(String path, Object request, Class<T> responseType) {
        log.debug("PUT {} with body: {}", path, request);
        
        return webClient.put()
            .uri(path)
            .headers(this::addAuthHeaders)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(responseType)
            .timeout(Duration.ofSeconds(30))
            .block();
    }
    
    public void delete(String path) {
        log.debug("DELETE {}", path);
        
        webClient.delete()
            .uri(path)
            .headers(this::addAuthHeaders)
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(30))
            .block();
    }
    
    public int getStatusCode(String path) {
        return webClient.get()
            .uri(path)
            .headers(this::addAuthHeaders)
            .exchangeToMono(response -> Mono.just(response.statusCode().value()))
            .block();
    }
    
    private void addAuthHeaders(HttpHeaders headers) {
        if (authToken != null && !authToken.isEmpty()) {
            headers.setBearerAuth(authToken);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
    }
    
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
    
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }
}
