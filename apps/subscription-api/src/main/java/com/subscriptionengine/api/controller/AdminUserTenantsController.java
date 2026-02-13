package com.subscriptionengine.api.controller;

import com.subscriptionengine.api.dto.UserTenantResponse;
import com.subscriptionengine.generated.tables.pojos.UserTenants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.subscriptionengine.generated.tables.UserTenants.USER_TENANTS;
import static com.subscriptionengine.generated.tables.Users.USERS;
import static com.subscriptionengine.generated.tables.Tenants.TENANTS;

/**
 * Admin controller for user-tenant relationship management.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/admin/user-tenants")
@Tag(name = "Admin - User Tenants", description = "Admin endpoints for user-tenant assignment")
public class AdminUserTenantsController {
    
    private final DSLContext dsl;
    
    public AdminUserTenantsController(DSLContext dsl) {
        this.dsl = dsl;
    }
    
    /**
     * Assign user to tenant.
     */
    @PostMapping
    @Operation(summary = "Assign user to tenant", description = "Assign a user to a tenant with a specific role")
    public ResponseEntity<UserTenantResponse> assignUserToTenant(@Valid @RequestBody AssignUserRequest request) {
        
        // Check if assignment already exists
        boolean exists = dsl.fetchExists(
            dsl.selectOne()
                .from(USER_TENANTS)
                .where(USER_TENANTS.USER_ID.eq(request.getUserId()))
                .and(USER_TENANTS.TENANT_ID.eq(request.getTenantId()))
        );
        
        if (exists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        // Create assignment
        LocalDateTime now = LocalDateTime.now();
        UUID assignmentId = UUID.randomUUID();
        
        dsl.insertInto(USER_TENANTS)
            .set(USER_TENANTS.ID, assignmentId)
            .set(USER_TENANTS.USER_ID, request.getUserId())
            .set(USER_TENANTS.TENANT_ID, request.getTenantId())
            .set(USER_TENANTS.ROLE, request.getRole())
            .set(USER_TENANTS.ASSIGNED_AT, now)
            .set(USER_TENANTS.CREATED_AT, now)
            .set(USER_TENANTS.UPDATED_AT, now)
            .execute();
        
        var result = dsl.select(
                USER_TENANTS.ID,
                USER_TENANTS.USER_ID,
                USER_TENANTS.TENANT_ID,
                USER_TENANTS.ROLE,
                USER_TENANTS.ASSIGNED_AT,
                USER_TENANTS.CREATED_AT,
                USER_TENANTS.UPDATED_AT,
                USERS.EMAIL,
                TENANTS.NAME
            )
            .from(USER_TENANTS)
            .join(USERS).on(USER_TENANTS.USER_ID.eq(USERS.ID))
            .join(TENANTS).on(USER_TENANTS.TENANT_ID.eq(TENANTS.ID))
            .where(USER_TENANTS.ID.eq(assignmentId))
            .fetchOne();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(result));
    }
    
    /**
     * Get user's tenants.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's tenants", description = "List all tenants assigned to a user")
    public ResponseEntity<List<UserTenantResponse>> getUserTenants(@PathVariable UUID userId) {
        
        var results = dsl.select(
                USER_TENANTS.ID,
                USER_TENANTS.USER_ID,
                USER_TENANTS.TENANT_ID,
                USER_TENANTS.ROLE,
                USER_TENANTS.ASSIGNED_AT,
                USER_TENANTS.CREATED_AT,
                USER_TENANTS.UPDATED_AT,
                USERS.EMAIL,
                TENANTS.NAME
            )
            .from(USER_TENANTS)
            .join(USERS).on(USER_TENANTS.USER_ID.eq(USERS.ID))
            .join(TENANTS).on(USER_TENANTS.TENANT_ID.eq(TENANTS.ID))
            .where(USER_TENANTS.USER_ID.eq(userId))
            .orderBy(USER_TENANTS.ASSIGNED_AT.desc())
            .fetch();
        
        List<UserTenantResponse> responses = results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Get tenant's users.
     */
    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get tenant's users", description = "List all users assigned to a tenant")
    public ResponseEntity<List<UserTenantResponse>> getTenantUsers(@PathVariable UUID tenantId) {
        
        var results = dsl.select(
                USER_TENANTS.ID,
                USER_TENANTS.USER_ID,
                USER_TENANTS.TENANT_ID,
                USER_TENANTS.ROLE,
                USER_TENANTS.ASSIGNED_AT,
                USER_TENANTS.CREATED_AT,
                USER_TENANTS.UPDATED_AT,
                USERS.EMAIL,
                TENANTS.NAME
            )
            .from(USER_TENANTS)
            .join(USERS).on(USER_TENANTS.USER_ID.eq(USERS.ID))
            .join(TENANTS).on(USER_TENANTS.TENANT_ID.eq(TENANTS.ID))
            .where(USER_TENANTS.TENANT_ID.eq(tenantId))
            .orderBy(USER_TENANTS.ASSIGNED_AT.desc())
            .fetch();
        
        List<UserTenantResponse> responses = results.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Update user's role in tenant.
     */
    @PatchMapping("/{assignmentId}")
    @Operation(summary = "Update user role", description = "Update a user's role in a tenant")
    public ResponseEntity<UserTenantResponse> updateUserRole(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody UpdateRoleRequest request) {
        
        int updated = dsl.update(USER_TENANTS)
            .set(USER_TENANTS.ROLE, request.getRole())
            .set(USER_TENANTS.UPDATED_AT, LocalDateTime.now())
            .where(USER_TENANTS.ID.eq(assignmentId))
            .execute();
        
        if (updated == 0) {
            return ResponseEntity.notFound().build();
        }
        
        var result = dsl.select(
                USER_TENANTS.ID,
                USER_TENANTS.USER_ID,
                USER_TENANTS.TENANT_ID,
                USER_TENANTS.ROLE,
                USER_TENANTS.ASSIGNED_AT,
                USER_TENANTS.CREATED_AT,
                USER_TENANTS.UPDATED_AT,
                USERS.EMAIL,
                TENANTS.NAME
            )
            .from(USER_TENANTS)
            .join(USERS).on(USER_TENANTS.USER_ID.eq(USERS.ID))
            .join(TENANTS).on(USER_TENANTS.TENANT_ID.eq(TENANTS.ID))
            .where(USER_TENANTS.ID.eq(assignmentId))
            .fetchOne();
        
        return ResponseEntity.ok(mapToResponse(result));
    }
    
    /**
     * Remove user from tenant.
     */
    @DeleteMapping("/{assignmentId}")
    @Operation(summary = "Remove user from tenant", description = "Remove a user's assignment to a tenant")
    public ResponseEntity<Void> removeUserFromTenant(@PathVariable UUID assignmentId) {
        
        int deleted = dsl.deleteFrom(USER_TENANTS)
            .where(USER_TENANTS.ID.eq(assignmentId))
            .execute();
        
        if (deleted == 0) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Map query result to UserTenantResponse DTO.
     */
    private UserTenantResponse mapToResponse(org.jooq.Record result) {
        return new UserTenantResponse(
            result.get(USER_TENANTS.ID),
            result.get(USER_TENANTS.USER_ID),
            result.get(USER_TENANTS.TENANT_ID),
            result.get(USER_TENANTS.ROLE),
            result.get(USER_TENANTS.ASSIGNED_AT) != null ? 
                OffsetDateTime.of(result.get(USER_TENANTS.ASSIGNED_AT), ZoneOffset.UTC) : null,
            result.get(USER_TENANTS.CREATED_AT) != null ? 
                OffsetDateTime.of(result.get(USER_TENANTS.CREATED_AT), ZoneOffset.UTC) : null,
            result.get(USER_TENANTS.UPDATED_AT) != null ? 
                OffsetDateTime.of(result.get(USER_TENANTS.UPDATED_AT), ZoneOffset.UTC) : null,
            result.get(USERS.EMAIL),
            result.get(TENANTS.NAME)
        );
    }
    
    /**
     * Request DTO for assigning user to tenant.
     */
    public static class AssignUserRequest {
        @NotNull(message = "User ID is required")
        private UUID userId;
        
        @NotNull(message = "Tenant ID is required")
        private UUID tenantId;
        
        @NotBlank(message = "Role is required")
        private String role;
        
        public UUID getUserId() {
            return userId;
        }
        
        public void setUserId(UUID userId) {
            this.userId = userId;
        }
        
        public UUID getTenantId() {
            return tenantId;
        }
        
        public void setTenantId(UUID tenantId) {
            this.tenantId = tenantId;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
    
    /**
     * Request DTO for updating user role.
     */
    public static class UpdateRoleRequest {
        @NotBlank(message = "Role is required")
        private String role;
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
}
