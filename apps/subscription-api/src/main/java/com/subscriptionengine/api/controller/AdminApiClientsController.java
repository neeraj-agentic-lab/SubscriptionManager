package com.subscriptionengine.api.controller;

import com.subscriptionengine.generated.tables.daos.ApiClientsDao;
import com.subscriptionengine.generated.tables.pojos.ApiClients;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.subscriptionengine.generated.tables.ApiClients.API_CLIENTS;

/**
 * REST controller for API client management.
 * Handles creation, rotation, and management of API client credentials.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/admin/api-clients")
@Tag(name = "Admin - API Clients", description = "Manage API client credentials for multi-tier authentication (API Key, OAuth, mTLS)")
public class AdminApiClientsController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminApiClientsController.class);
    
    private final ApiClientsDao apiClientsDao;
    private final DSLContext dsl;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public AdminApiClientsController(ApiClientsDao apiClientsDao, DSLContext dsl) {
        this.apiClientsDao = apiClientsDao;
        this.dsl = dsl;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }
    
    /**
     * Create a new API client.
     * Returns client_id and client_secret (shown ONLY ONCE).
     */
    @PostMapping
    @Operation(
        summary = "Create API client",
        description = "Creates a new API client with credentials. The client_secret is shown ONLY ONCE and cannot be retrieved later. "
            + "Supports three authentication methods: API_KEY (HMAC signing), OAUTH (OAuth 2.0), and MTLS (certificate-based)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "API client created successfully",
            content = @Content(schema = @Schema(implementation = CreateApiClientResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors"
        )
    })
    public ResponseEntity<Object> createApiClient(
        @Parameter(description = "API client creation details", required = true)
        @Valid @RequestBody CreateApiClientRequest request) {
        
        logger.info("Creating API client: {} for tenant: {}", request.getName(), request.getTenantId());
        
        // Generate client credentials
        String clientId = generateClientId(request.getName());
        String clientSecret = null;
        String clientSecretHash = null;
        
        // Generate secret for API_KEY and OAUTH methods
        if ("API_KEY".equals(request.getAuthMethod()) || "OAUTH".equals(request.getAuthMethod())) {
            clientSecret = generateClientSecret();
            clientSecretHash = passwordEncoder.encode(clientSecret);
        }
        
        // Create API client
        ApiClients apiClient = new ApiClients();
        apiClient.setId(UUID.randomUUID());
        apiClient.setTenantId(request.getTenantId());
        apiClient.setClientId(clientId);
        apiClient.setClientName(request.getName());
        apiClient.setClientType(request.getClientType());
        apiClient.setAuthMethod(request.getAuthMethod());
        apiClient.setClientSecretHash(clientSecretHash);
        apiClient.setAllowedScopes(request.getScopes() != null ? request.getScopes().toArray(new String[0]) : new String[]{});
        apiClient.setAllowedIps(request.getAllowedIps() != null ? request.getAllowedIps().toArray(new Object[0]) : null);
        apiClient.setRateLimitPerHour(request.getRateLimitPerHour() != null ? request.getRateLimitPerHour() : 1000);
        apiClient.setRedirectUris(request.getRedirectUris() != null ? request.getRedirectUris().toArray(new String[0]) : null);
        apiClient.setDescription(request.getDescription());
        
        // Set status based on auth method
        if ("MTLS".equals(request.getAuthMethod())) {
            apiClient.setStatus("PENDING_CERTIFICATE");
        } else {
            apiClient.setStatus("ACTIVE");
        }
        
        apiClient.setCreatedAt(LocalDateTime.now());
        apiClient.setUpdatedAt(LocalDateTime.now());
        
        apiClientsDao.insert(apiClient);
        
        logger.info("Successfully created API client: {} ({})", clientId, apiClient.getId());
        
        // Return response with secret (ONLY ONCE)
        CreateApiClientResponse response = new CreateApiClientResponse();
        response.setId(apiClient.getId());
        response.setClientId(clientId);
        response.setClientSecret(clientSecret); // Only returned on creation
        response.setName(request.getName());
        response.setAuthMethod(request.getAuthMethod());
        response.setScopes(List.of(apiClient.getAllowedScopes()));
        response.setStatus(apiClient.getStatus());
        response.setCreatedAt(OffsetDateTime.now());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * List all API clients for a tenant.
     */
    @GetMapping
    @Operation(
        summary = "List API clients",
        description = "Retrieves a paginated list of API clients with usage statistics. Does NOT include client secrets."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "API clients retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    public ResponseEntity<Page<ApiClientResponse>> listApiClients(
        @Parameter(description = "Tenant ID to filter clients")
        @RequestParam(required = false) UUID tenantId,
        @Parameter(description = "Pagination parameters")
        @PageableDefault(size = 20) Pageable pageable) {
        
        logger.info("Listing API clients for tenant: {}", tenantId);
        
        var query = dsl.selectFrom(API_CLIENTS);
        
        if (tenantId != null) {
            query.where(API_CLIENTS.TENANT_ID.eq(tenantId));
        }
        
        List<ApiClients> clients = query
                .orderBy(API_CLIENTS.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetchInto(ApiClients.class);
        
        long totalCount = dsl.selectCount()
                .from(API_CLIENTS)
                .where(tenantId != null ? API_CLIENTS.TENANT_ID.eq(tenantId) : null)
                .fetchOne(0, Long.class);
        
        List<ApiClientResponse> responses = clients.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        Page<ApiClientResponse> page = new PageImpl<>(responses, pageable, totalCount);
        
        return ResponseEntity.ok(page);
    }
    
    /**
     * Get API client by ID.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get API client details",
        description = "Retrieves detailed information about an API client including usage statistics. Does NOT include client secret."
    )
    public ResponseEntity<Object> getApiClient(@PathVariable UUID id) {
        logger.info("Fetching API client: {}", id);
        
        ApiClients client = apiClientsDao.fetchOneById(id);
        
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "API_CLIENT_NOT_FOUND",
                "message", "API client not found: " + id
            ));
        }
        
        return ResponseEntity.ok(mapToResponse(client));
    }
    
    /**
     * Update API client.
     * Handles status changes, scope updates, secret rotation, etc.
     */
    @PatchMapping("/{id}")
    @Operation(
        summary = "Update API client",
        description = "Updates API client settings including status (suspend/resume), scopes, rate limits, and secret rotation. "
            + "Use rotateSecret=true to generate a new client secret."
    )
    public ResponseEntity<Object> updateApiClient(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateApiClientRequest request) {
        
        logger.info("Updating API client: {}", id);
        
        ApiClients client = apiClientsDao.fetchOneById(id);
        
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "API_CLIENT_NOT_FOUND",
                "message", "API client not found: " + id
            ));
        }
        
        String newSecret = null;
        
        // Handle secret rotation
        if (Boolean.TRUE.equals(request.getRotateSecret())) {
            if ("API_KEY".equals(client.getAuthMethod()) || "OAUTH".equals(client.getAuthMethod())) {
                newSecret = generateClientSecret();
                client.setClientSecretHash(passwordEncoder.encode(newSecret));
                logger.info("Rotated secret for API client: {}", id);
            }
        }
        
        // Update status
        if (request.getStatus() != null) {
            client.setStatus(request.getStatus());
        }
        
        // Update scopes
        if (request.getScopes() != null) {
            client.setAllowedScopes(request.getScopes().toArray(new String[0]));
        }
        
        // Update rate limit
        if (request.getRateLimitPerHour() != null) {
            client.setRateLimitPerHour(request.getRateLimitPerHour());
        }
        
        // Update allowed IPs
        if (request.getAllowedIps() != null) {
            client.setAllowedIps(request.getAllowedIps().toArray(new Object[0]));
        }
        
        client.setUpdatedAt(LocalDateTime.now());
        
        apiClientsDao.update(client);
        
        logger.info("Successfully updated API client: {}", id);
        
        // If secret was rotated, return it (ONLY ONCE)
        if (newSecret != null) {
            return ResponseEntity.ok(Map.of(
                "message", "API client updated and secret rotated",
                "client", mapToResponse(client),
                "newClientSecret", newSecret
            ));
        }
        
        return ResponseEntity.ok(mapToResponse(client));
    }
    
    /**
     * Delete (revoke) API client.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Revoke API client",
        description = "Permanently revokes an API client. This action cannot be undone."
    )
    public ResponseEntity<Object> deleteApiClient(@PathVariable UUID id) {
        logger.info("Revoking API client: {}", id);
        
        ApiClients client = apiClientsDao.fetchOneById(id);
        
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "API_CLIENT_NOT_FOUND",
                "message", "API client not found: " + id
            ));
        }
        
        // Soft delete by setting status to REVOKED
        client.setStatus("REVOKED");
        client.setUpdatedAt(LocalDateTime.now());
        apiClientsDao.update(client);
        
        logger.info("Successfully revoked API client: {}", id);
        
        return ResponseEntity.ok(Map.of(
            "message", "API client revoked successfully",
            "clientId", client.getClientId()
        ));
    }
    
    /**
     * Map API client to response DTO (without secret).
     */
    private ApiClientResponse mapToResponse(ApiClients client) {
        ApiClientResponse response = new ApiClientResponse();
        response.setId(client.getId());
        response.setTenantId(client.getTenantId());
        response.setClientId(client.getClientId());
        response.setName(client.getClientName());
        response.setClientType(client.getClientType());
        response.setAuthMethod(client.getAuthMethod());
        
        if (client.getAllowedScopes() != null) {
            response.setScopes(Arrays.asList(client.getAllowedScopes()));
        } else {
            response.setScopes(List.of());
        }
        
        if (client.getAllowedIps() != null) {
            response.setAllowedIps(Arrays.stream(client.getAllowedIps()).map(Object::toString).collect(Collectors.toList()));
        } else {
            response.setAllowedIps(List.of());
        }
        
        response.setRateLimitPerHour(client.getRateLimitPerHour());
        response.setStatus(client.getStatus());
        response.setLastUsedAt(client.getLastUsedAt() != null ? OffsetDateTime.of(client.getLastUsedAt(), java.time.ZoneOffset.UTC) : null);
        response.setTotalRequests(client.getTotalRequests());
        response.setDescription(client.getDescription());
        response.setCreatedAt(client.getCreatedAt() != null ? OffsetDateTime.of(client.getCreatedAt(), java.time.ZoneOffset.UTC) : null);
        response.setUpdatedAt(client.getUpdatedAt() != null ? OffsetDateTime.of(client.getUpdatedAt(), java.time.ZoneOffset.UTC) : null);
        return response;
    }
    
    /**
     * Generate client ID from name.
     */
    private String generateClientId(String name) {
        String sanitized = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String random = UUID.randomUUID().toString().substring(0, 8);
        return sanitized + "_" + random;
    }
    
    /**
     * Generate secure client secret.
     */
    private String generateClientSecret() {
        return "sk_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * DTO for API client creation.
     */
    public static class CreateApiClientRequest {
        @NotNull(message = "Tenant ID is required")
        private UUID tenantId;
        
        @NotBlank(message = "Client name is required")
        private String name;
        
        @NotBlank(message = "Client type is required")
        private String clientType; // SERVER, SPA, MOBILE, NATIVE
        
        @NotBlank(message = "Auth method is required")
        private String authMethod; // API_KEY, OAUTH, MTLS
        
        private List<String> scopes;
        private List<String> allowedIps;
        private List<String> redirectUris; // For OAuth
        private Integer rateLimitPerHour;
        private String description;
        
        // Getters and setters
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getClientType() { return clientType; }
        public void setClientType(String clientType) { this.clientType = clientType; }
        
        public String getAuthMethod() { return authMethod; }
        public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
        
        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }
        
        public List<String> getAllowedIps() { return allowedIps; }
        public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }
        
        public List<String> getRedirectUris() { return redirectUris; }
        public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }
        
        public Integer getRateLimitPerHour() { return rateLimitPerHour; }
        public void setRateLimitPerHour(Integer rateLimitPerHour) { this.rateLimitPerHour = rateLimitPerHour; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * DTO for API client update.
     */
    public static class UpdateApiClientRequest {
        private String status; // ACTIVE, SUSPENDED, REVOKED
        private List<String> scopes;
        private List<String> allowedIps;
        private Integer rateLimitPerHour;
        private Boolean rotateSecret;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }
        
        public List<String> getAllowedIps() { return allowedIps; }
        public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }
        
        public Integer getRateLimitPerHour() { return rateLimitPerHour; }
        public void setRateLimitPerHour(Integer rateLimitPerHour) { this.rateLimitPerHour = rateLimitPerHour; }
        
        public Boolean getRotateSecret() { return rotateSecret; }
        public void setRotateSecret(Boolean rotateSecret) { this.rotateSecret = rotateSecret; }
    }
    
    /**
     * DTO for API client response (without secret).
     */
    public static class ApiClientResponse {
        private UUID id;
        private UUID tenantId;
        private String clientId;
        private String name;
        private String clientType;
        private String authMethod;
        private List<String> scopes;
        private List<String> allowedIps;
        private Integer rateLimitPerHour;
        private String status;
        private OffsetDateTime lastUsedAt;
        private Long totalRequests;
        private String description;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        
        // Getters and setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getClientType() { return clientType; }
        public void setClientType(String clientType) { this.clientType = clientType; }
        
        public String getAuthMethod() { return authMethod; }
        public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
        
        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }
        
        public List<String> getAllowedIps() { return allowedIps; }
        public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }
        
        public Integer getRateLimitPerHour() { return rateLimitPerHour; }
        public void setRateLimitPerHour(Integer rateLimitPerHour) { this.rateLimitPerHour = rateLimitPerHour; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
        public void setLastUsedAt(OffsetDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
        
        public Long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(Long totalRequests) { this.totalRequests = totalRequests; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
    
    /**
     * DTO for API client creation response (includes secret ONLY ONCE).
     */
    public static class CreateApiClientResponse {
        private UUID id;
        private String clientId;
        private String clientSecret; // Only returned on creation
        private String name;
        private String authMethod;
        private List<String> scopes;
        private String status;
        private OffsetDateTime createdAt;
        
        // Getters and setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getAuthMethod() { return authMethod; }
        public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
        
        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    }
}
