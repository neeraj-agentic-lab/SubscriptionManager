package com.subscriptionengine.api.controller;

import com.subscriptionengine.generated.tables.daos.ApiClientsDao;
import com.subscriptionengine.generated.tables.daos.UserTenantsDao;
import com.subscriptionengine.generated.tables.daos.UsersDao;
import com.subscriptionengine.generated.tables.pojos.ApiClients;
import com.subscriptionengine.generated.tables.pojos.UserTenants;
import com.subscriptionengine.generated.tables.pojos.Users;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
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
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

import static com.subscriptionengine.generated.tables.Users.USERS;
import static com.subscriptionengine.generated.tables.ApiClients.API_CLIENTS;
import static com.subscriptionengine.generated.tables.UserTenants.USER_TENANTS;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final UsersDao usersDao;
    private final ApiClientsDao apiClientsDao;
    private final UserTenantsDao userTenantsDao;
    private final DSLContext dsl;
    private final BCryptPasswordEncoder passwordEncoder;
    
    @Value("${jwt.secret:dev-secret-key-not-for-production}")
    private String jwtSecret;
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;
    
    public AuthController(UsersDao usersDao, ApiClientsDao apiClientsDao, 
                         UserTenantsDao userTenantsDao, DSLContext dsl) {
        this.usersDao = usersDao;
        this.apiClientsDao = apiClientsDao;
        this.userTenantsDao = userTenantsDao;
        this.dsl = dsl;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }
    
    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user with email and password. Returns JWT token with user and tenant context."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials or account suspended"
        )
    })
    public ResponseEntity<Object> login(
        @Parameter(description = "Login credentials", required = true)
        @Valid @RequestBody LoginRequest request) {
        
        logger.info("Login attempt for user: {}", request.getEmail());
        
        Users user = dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq(request.getEmail()))
            .fetchOneInto(Users.class);
        
        if (user == null) {
            logger.warn("Login failed: User not found - {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "INVALID_CREDENTIALS",
                "message", "Invalid email or password"
            ));
        }
        
        if (!"ACTIVE".equals(user.getStatus())) {
            logger.warn("Login failed: Account not active - {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "ACCOUNT_SUSPENDED",
                "message", "Account is " + user.getStatus().toLowerCase()
            ));
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Login failed: Invalid password - {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "INVALID_CREDENTIALS",
                "message", "Invalid email or password"
            ));
        }
        
        // Check if user is SUPER_ADMIN (global access, no tenant required)
        if ("SUPER_ADMIN".equals(user.getRole())) {
            String token = generateJwtToken(
                user.getId(),
                user.getEmail(),
                null, // SUPER_ADMIN has no specific tenant
                user.getRole()
            );
            
            logger.info("Login successful for SUPER_ADMIN: {}", request.getEmail());
            
            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserId(user.getId());
            response.setEmail(user.getEmail());
            response.setTenantId(null); // SUPER_ADMIN has global access
            response.setRole(user.getRole());
            response.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtExpiration / 1000));
            
            return ResponseEntity.ok(response);
        }
        
        // For non-SUPER_ADMIN users, require tenant access
        List<UserTenants> userTenants = dsl.selectFrom(USER_TENANTS)
            .where(USER_TENANTS.USER_ID.eq(user.getId()))
            .fetchInto(UserTenants.class);
        
        if (userTenants.isEmpty()) {
            logger.warn("Login failed: No tenant access - {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "NO_TENANT_ACCESS",
                "message", "User has no tenant access"
            ));
        }
        
        UserTenants primaryTenant = userTenants.get(0);
        
        String token = generateJwtToken(
            user.getId(),
            user.getEmail(),
            primaryTenant.getTenantId(),
            primaryTenant.getRole()
        );
        
        logger.info("Login successful for user: {} (tenant: {})", request.getEmail(), primaryTenant.getTenantId());
        
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setTenantId(primaryTenant.getTenantId());
        response.setRole(primaryTenant.getRole());
        response.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtExpiration / 1000));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/api-key")
    @Operation(
        summary = "API key authentication",
        description = "Authenticate using API key and secret. Returns JWT token with client and tenant context."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = ApiKeyAuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid API key or secret"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "API client revoked or suspended"
        )
    })
    public ResponseEntity<Object> authenticateWithApiKey(
        @Parameter(description = "API key credentials", required = true)
        @Valid @RequestBody ApiKeyAuthRequest request) {
        
        logger.info("API key authentication attempt: {}", request.getApiKey());
        
        ApiClients client = dsl.selectFrom(API_CLIENTS)
            .where(API_CLIENTS.CLIENT_ID.eq(request.getApiKey()))
            .fetchOneInto(ApiClients.class);
        
        if (client == null) {
            logger.warn("API key authentication failed: Client not found - {}", request.getApiKey());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "INVALID_CREDENTIALS",
                "message", "Invalid API key or secret"
            ));
        }
        
        if ("REVOKED".equals(client.getStatus())) {
            logger.warn("API key authentication failed: Client revoked - {}", request.getApiKey());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "CLIENT_REVOKED",
                "message", "API client has been revoked"
            ));
        }
        
        if ("SUSPENDED".equals(client.getStatus())) {
            logger.warn("API key authentication failed: Client suspended - {}", request.getApiKey());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "CLIENT_SUSPENDED",
                "message", "API client is suspended"
            ));
        }
        
        if (!passwordEncoder.matches(request.getApiSecret(), client.getClientSecretHash())) {
            logger.warn("API key authentication failed: Invalid secret - {}", request.getApiKey());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "INVALID_CREDENTIALS",
                "message", "Invalid API key or secret"
            ));
        }
        
        String token = generateApiClientJwtToken(
            client.getId(),
            client.getClientId(),
            client.getTenantId(),
            Arrays.asList(client.getAllowedScopes())
        );
        
        client.setLastUsedAt(OffsetDateTime.now().toLocalDateTime());
        client.setTotalRequests(client.getTotalRequests() != null ? client.getTotalRequests() + 1 : 1L);
        apiClientsDao.update(client);
        
        logger.info("API key authentication successful: {} (tenant: {})", request.getApiKey(), client.getTenantId());
        
        ApiKeyAuthResponse response = new ApiKeyAuthResponse();
        response.setToken(token);
        response.setClientId(client.getClientId());
        response.setTenantId(client.getTenantId());
        response.setScopes(Arrays.asList(client.getAllowedScopes()));
        response.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtExpiration / 1000));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/switch-tenant")
    @Operation(
        summary = "Switch tenant context",
        description = "Switch the current user's tenant context. Returns new JWT token with updated tenant."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tenant switched successfully",
            content = @Content(schema = @Schema(implementation = SwitchTenantResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "User does not have access to the requested tenant"
        )
    })
    public ResponseEntity<Object> switchTenant(
        @Parameter(description = "Target tenant ID", required = true)
        @Valid @RequestBody SwitchTenantRequest request,
        @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        
        if (userIdHeader == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "UNAUTHORIZED",
                "message", "User ID not found in request headers"
            ));
        }
        
        UUID userId = UUID.fromString(userIdHeader);
        
        logger.info("Tenant switch request: user {} to tenant {}", userId, request.getTenantId());
        
        UserTenants userTenant = dsl.selectFrom(USER_TENANTS)
            .where(USER_TENANTS.USER_ID.eq(userId))
            .and(USER_TENANTS.TENANT_ID.eq(request.getTenantId()))
            .fetchOneInto(UserTenants.class);
        
        if (userTenant == null) {
            logger.warn("Tenant switch failed: User {} has no access to tenant {}", userId, request.getTenantId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "NO_TENANT_ACCESS",
                "message", "User does not have access to the requested tenant"
            ));
        }
        
        Users user = usersDao.fetchOneById(userId);
        
        String token = generateJwtToken(
            user.getId(),
            user.getEmail(),
            request.getTenantId(),
            userTenant.getRole()
        );
        
        logger.info("Tenant switch successful: user {} to tenant {}", userId, request.getTenantId());
        
        SwitchTenantResponse response = new SwitchTenantResponse();
        response.setToken(token);
        response.setTenantId(request.getTenantId());
        response.setRole(userTenant.getRole());
        response.setExpiresAt(OffsetDateTime.now().plusSeconds(jwtExpiration / 1000));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    @Operation(
        summary = "Get current user context",
        description = "Returns the current authenticated user's context including tenant and role."
    )
    public ResponseEntity<Object> getCurrentUser() {
        UUID userId = com.subscriptionengine.auth.UserContext.getUserId();
        UUID tenantId = com.subscriptionengine.auth.TenantContext.getTenantId();
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "UNAUTHORIZED",
                "message", "User context not found"
            ));
        }
        
        Users user = usersDao.fetchOneById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "UNAUTHORIZED",
                "message", "User not found"
            ));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("email", user.getEmail());
        response.put("role", com.subscriptionengine.auth.UserContext.getUserRole());
        
        // Include tenant info if available (not for SUPER_ADMIN)
        if (tenantId != null) {
            UserTenants userTenant = dsl.selectFrom(USER_TENANTS)
                .where(USER_TENANTS.USER_ID.eq(userId))
                .and(USER_TENANTS.TENANT_ID.eq(tenantId))
                .fetchOneInto(UserTenants.class);
            
            response.put("tenantId", tenantId);
            response.put("tenantRole", userTenant != null ? userTenant.getRole() : null);
        }
        
        return ResponseEntity.ok(response);
    }
    
    private String generateJwtToken(UUID userId, String email, UUID tenantId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        var builder = Jwts.builder()
            .setSubject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("type", "USER")
            .setIssuedAt(now)
            .setExpiration(expiryDate);
        
        if (tenantId != null) {
            builder.claim("tenantId", tenantId.toString());
        }
        
        // For CUSTOMER role, include customer_id claim (user ID is the customer ID)
        if ("CUSTOMER".equals(role)) {
            builder.claim("customer_id", userId.toString());
        }
        
        return builder.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }
    
    private String generateApiClientJwtToken(UUID clientId, String clientKey, UUID tenantId, List<String> scopes) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
            .setSubject(clientId.toString())
            .claim("clientKey", clientKey)
            .claim("tenantId", tenantId.toString())
            .claim("scopes", scopes)
            .claim("type", "API_CLIENT")
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        private String email;
        
        @NotBlank(message = "Password is required")
        private String password;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class LoginResponse {
        private String token;
        private UUID userId;
        private String email;
        private UUID tenantId;
        private String role;
        private OffsetDateTime expiresAt;
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    }
    
    public static class ApiKeyAuthRequest {
        @NotBlank(message = "API key is required")
        private String apiKey;
        
        @NotBlank(message = "API secret is required")
        private String apiSecret;
        
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    }
    
    public static class ApiKeyAuthResponse {
        private String token;
        private String clientId;
        private UUID tenantId;
        private List<String> scopes;
        private OffsetDateTime expiresAt;
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        
        public List<String> getScopes() { return scopes; }
        public void setScopes(List<String> scopes) { this.scopes = scopes; }
        
        public OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    }
    
    public static class SwitchTenantRequest {
        @NotNull(message = "Tenant ID is required")
        private UUID tenantId;
        
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    }
    
    public static class SwitchTenantResponse {
        private String token;
        private UUID tenantId;
        private String role;
        private OffsetDateTime expiresAt;
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public OffsetDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    }
}
