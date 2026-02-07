package com.subscriptionengine.api.controller;

import com.subscriptionengine.generated.tables.daos.TenantsDao;
import com.subscriptionengine.generated.tables.pojos.Tenants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.subscriptionengine.generated.tables.Tenants.TENANTS;
import static com.subscriptionengine.generated.tables.Subscriptions.SUBSCRIPTIONS;

/**
 * REST controller for tenant management.
 * This is a simple API for creating tenants - typically would be more sophisticated in production.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/tenants")
@Tag(name = "Tenants", description = "Multi-tenant management APIs. Create and manage tenant organizations for complete data isolation.")
public class TenantsController {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantsController.class);
    
    private final TenantsDao tenantsDao;
    private final DSLContext dsl;
    
    public TenantsController(TenantsDao tenantsDao, DSLContext dsl) {
        this.tenantsDao = tenantsDao;
        this.dsl = dsl;
    }
    
    /**
     * Create a new tenant.
     * This is a simple endpoint for testing - no authentication required.
     */
    @PostMapping
    @Operation(
        summary = "Create a new tenant",
        description = "Creates a new tenant organization with complete data isolation. "
            + "Each tenant has separate customers, subscriptions, and billing. "
            + "No authentication required for tenant creation (typically would be restricted in production)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Tenant created successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors in tenant data"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - tenant with this slug already exists"
        )
    })
    public ResponseEntity<Object> createTenant(
        @Parameter(description = "Tenant creation details including name and unique slug", required = true)
        @Valid @RequestBody CreateTenantRequest request) {
        logger.info("Creating new tenant: {}", request.getName());
        
        try {
            Tenants tenant = new Tenants();
            tenant.setId(request.getId() != null ? request.getId() : UUID.randomUUID());
            tenant.setName(request.getName());
            tenant.setSlug(request.getSlug());
            tenant.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
            tenant.setCreatedAt(OffsetDateTime.now());
            tenant.setUpdatedAt(OffsetDateTime.now());
            
            tenantsDao.insert(tenant);
            
            logger.info("Successfully created tenant: {}", tenant.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", tenant.getId().toString(),
                "name", tenant.getName(),
                "slug", tenant.getSlug(),
                "status", tenant.getStatus(),
                "createdAt", tenant.getCreatedAt().toString()
            ));
            
        } catch (Exception e) {
            logger.error("Error creating tenant: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Failed to create tenant"
            ));
        }
    }
    
    /**
     * Get all tenants with pagination.
     */
    @GetMapping
    @Operation(
        summary = "List all tenants with pagination",
        description = "Retrieves a paginated list of all tenant organizations. Includes tenant statistics like subscription count."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tenants retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        )
    })
    public ResponseEntity<Page<TenantResponse>> getAllTenants(
        @Parameter(description = "Pagination parameters (page, size)", example = "page=0&size=20")
        @PageableDefault(size = 20) Pageable pageable) {
        logger.info("Fetching tenants with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            List<Tenants> tenants = dsl.selectFrom(TENANTS)
                    .orderBy(TENANTS.CREATED_AT.desc())
                    .limit(pageable.getPageSize())
                    .offset((int) pageable.getOffset())
                    .fetchInto(Tenants.class);
            
            long totalCount = dsl.selectCount()
                    .from(TENANTS)
                    .fetchOne(0, Long.class);
            
            List<TenantResponse> tenantResponses = tenants.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            
            Page<TenantResponse> page = new PageImpl<>(tenantResponses, pageable, totalCount);
            
            return ResponseEntity.ok(page);
            
        } catch (Exception e) {
            logger.error("Error fetching tenants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get tenant by ID.
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<Object> getTenantById(@PathVariable UUID tenantId) {
        logger.info("Fetching tenant: {}", tenantId);
        
        try {
            Tenants tenant = tenantsDao.fetchOneById(tenantId);
            
            if (tenant == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "TENANT_NOT_FOUND",
                    "message", "Tenant not found: " + tenantId
                ));
            }
            
            return ResponseEntity.ok(mapToResponse(tenant));
            
        } catch (Exception e) {
            logger.error("Error fetching tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Failed to fetch tenant"
            ));
        }
    }
    
    /**
     * Update tenant.
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<Object> updateTenant(@PathVariable UUID tenantId, 
                                             @Valid @RequestBody UpdateTenantRequest request) {
        logger.info("Updating tenant: {}", tenantId);
        
        try {
            Tenants existingTenant = tenantsDao.fetchOneById(tenantId);
            
            if (existingTenant == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "TENANT_NOT_FOUND",
                    "message", "Tenant not found: " + tenantId
                ));
            }
            
            // Update fields
            existingTenant.setName(request.getName());
            existingTenant.setSlug(request.getSlug());
            if (request.getStatus() != null) {
                existingTenant.setStatus(request.getStatus());
            }
            existingTenant.setUpdatedAt(OffsetDateTime.now());
            
            tenantsDao.update(existingTenant);
            
            logger.info("Successfully updated tenant: {}", tenantId);
            return ResponseEntity.ok(mapToResponse(existingTenant));
            
        } catch (Exception e) {
            logger.error("Error updating tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Failed to update tenant"
            ));
        }
    }
    
    /**
     * Delete tenant.
     * Prevents deletion if tenant has active subscriptions.
     */
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Object> deleteTenant(@PathVariable UUID tenantId) {
        logger.info("Deleting tenant: {}", tenantId);
        
        try {
            Tenants existingTenant = tenantsDao.fetchOneById(tenantId);
            
            if (existingTenant == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "TENANT_NOT_FOUND",
                    "message", "Tenant not found: " + tenantId
                ));
            }
            
            // Check if tenant has any subscriptions
            long subscriptionCount = dsl.selectCount()
                    .from(SUBSCRIPTIONS)
                    .where(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                    .fetchOne(0, Long.class);
            
            if (subscriptionCount > 0) {
                logger.warn("Cannot delete tenant {} - has {} active subscriptions", tenantId, subscriptionCount);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "TENANT_HAS_SUBSCRIPTIONS",
                    "message", "Cannot delete tenant with active subscriptions. Found " + subscriptionCount + " subscription(s).",
                    "subscriptionCount", subscriptionCount
                ));
            }
            
            // Check for other related data (plans, customers, etc.)
            long planCount = dsl.selectCount()
                    .from(com.subscriptionengine.generated.tables.Plans.PLANS)
                    .where(com.subscriptionengine.generated.tables.Plans.PLANS.TENANT_ID.eq(tenantId))
                    .fetchOne(0, Long.class);
            
            long customerCount = dsl.selectCount()
                    .from(com.subscriptionengine.generated.tables.Customers.CUSTOMERS)
                    .where(com.subscriptionengine.generated.tables.Customers.CUSTOMERS.TENANT_ID.eq(tenantId))
                    .fetchOne(0, Long.class);
            
            if (planCount > 0 || customerCount > 0) {
                logger.warn("Cannot delete tenant {} - has related data: {} plans, {} customers", 
                           tenantId, planCount, customerCount);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "TENANT_HAS_RELATED_DATA",
                    "message", "Cannot delete tenant with related data. Found " + planCount + " plan(s) and " + customerCount + " customer(s).",
                    "planCount", planCount,
                    "customerCount", customerCount
                ));
            }
            
            tenantsDao.deleteById(tenantId);
            
            logger.info("Successfully deleted tenant: {}", tenantId);
            return ResponseEntity.ok(Map.of(
                "message", "Tenant deleted successfully",
                "tenantId", tenantId.toString()
            ));
            
        } catch (Exception e) {
            logger.error("Error deleting tenant {}: {}", tenantId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Failed to delete tenant"
            ));
        }
    }
    
    /**
     * Map tenant entity to response DTO.
     */
    private TenantResponse mapToResponse(Tenants tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setSlug(tenant.getSlug());
        response.setStatus(tenant.getStatus());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        return response;
    }
    
    /**
     * DTO for tenant creation.
     */
    public static class CreateTenantRequest {
        private UUID id;
        private String name;
        private String slug;
        private String status;
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    /**
     * DTO for tenant updates.
     */
    public static class UpdateTenantRequest {
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        private String name;
        
        @NotBlank(message = "Slug is required")
        @Size(max = 100, message = "Slug must not exceed 100 characters")
        private String slug;
        
        @Size(max = 20, message = "Status must not exceed 20 characters")
        private String status;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    /**
     * DTO for tenant responses.
     */
    public static class TenantResponse {
        private UUID id;
        private String name;
        private String slug;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
