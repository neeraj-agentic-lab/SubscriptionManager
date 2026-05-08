package com.subscriptionengine.api.controller;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.auth.TenantSecured;
import com.subscriptionengine.generated.tables.daos.CustomersDao;
import com.subscriptionengine.generated.tables.pojos.Customers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for standalone customer management.
 * Allows creating customers independently of subscriptions.
 */
@RestController
@RequestMapping("/v1/admin/customers")
@TenantSecured
@Tag(name = "Admin - Customers", description = "Admin endpoints for customer management. Create and manage customer records independently of subscriptions.")
public class CustomersController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomersController.class);
    
    private final CustomersDao customersDao;
    
    public CustomersController(CustomersDao customersDao) {
        this.customersDao = customersDao;
    }
    
    /**
     * Get all customers for the current tenant.
     */
    @GetMapping
    @Operation(
        summary = "Get all customers",
        description = "Retrieves all customers for the current tenant."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Customers retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("Fetching all customers for tenant: {}", tenantId);
        
        List<Customers> customers = customersDao.fetchByTenantId(tenantId);
        
        logger.info("Found {} customers for tenant: {}", customers.size(), tenantId);
        
        List<CustomerResponse> response = customers.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get customer by ID.
     */
    @GetMapping("/{customerId}")
    @Operation(
        summary = "Get customer by ID",
        description = "Retrieves a specific customer by their ID for the current tenant."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Customer retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Customer not found"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable UUID customerId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("Fetching customer {} for tenant: {}", customerId, tenantId);
        
        Customers customer = customersDao.fetchOneById(customerId);
        
        if (customer == null || !customer.getTenantId().equals(tenantId)) {
            logger.warn("Customer {} not found for tenant: {}", customerId, tenantId);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(mapToResponse(customer));
    }
    
    private CustomerResponse mapToResponse(Customers customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setTenantId(customer.getTenantId());
        response.setEmail(customer.getEmail());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setExternalCustomerId(customer.getExternalCustomerId());
        response.setStatus(customer.getStatus());
        response.setCustomerType(customer.getCustomerType());
        response.setCustomAttrs(customer.getCustomAttrs() != null ? customer.getCustomAttrs().data() : "{}");
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());
        return response;
    }
    
    /**
     * Create a new customer.
     */
    @PostMapping
    @Operation(
        summary = "Create a new customer",
        description = "Creates a new customer record with email and optional name. "
            + "Customers can be created independently before subscriptions or automatically during subscription creation. "
            + "Email must be unique within the tenant. Supports external customer ID for integration with other systems."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Customer created successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors in customer data"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - customer with this email already exists for this tenant"
        )
    })
    public ResponseEntity<Map<String, Object>> createCustomer(
        @Parameter(description = "Customer creation details including email, name, and external reference", required = true)
        @Valid @RequestBody CreateCustomerRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("Creating customer with email: {} for tenant: {}", request.getEmail(), tenantId);
        
        Customers customer = new Customers();
        customer.setId(UUID.randomUUID());
        customer.setTenantId(tenantId);
        customer.setEmail(request.getEmail());
        customer.setFirstName(request.getName());
        customer.setExternalCustomerId(request.getExternalCustomerRef());
        customer.setCustomAttrs(org.jooq.JSONB.valueOf("{}"));
        customer.setStatus("ACTIVE");
        customer.setCustomerType("REGISTERED");
        customer.setCreatedAt(OffsetDateTime.now());
        customer.setUpdatedAt(OffsetDateTime.now());
        
        logger.debug("Customer object before insert - email: {}, firstName: {}, externalCustomerId: {}", 
            customer.getEmail(), customer.getFirstName(), customer.getExternalCustomerId());
        
        try {
            customersDao.insert(customer);
        } catch (DuplicateKeyException e) {
            logger.warn("Customer with email {} already exists for tenant: {}", request.getEmail(), tenantId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        logger.info("Successfully created customer: {}", customer.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customer.getId().toString());
        data.put("email", customer.getEmail());
        data.put("name", customer.getFirstName());
        data.put("externalCustomerRef", customer.getExternalCustomerId());
        
        response.put("data", data);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Request DTO for creating a customer.
     */
    public static class CreateCustomerRequest {
        
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        
        private String name;
        private String externalCustomerRef;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getExternalCustomerRef() {
            return externalCustomerRef;
        }
        
        public void setExternalCustomerRef(String externalCustomerRef) {
            this.externalCustomerRef = externalCustomerRef;
        }
    }
    
    /**
     * Response DTO for customer data.
     */
    public static class CustomerResponse {
        private UUID id;
        private UUID tenantId;
        private String email;
        private String firstName;
        private String lastName;
        private String externalCustomerId;
        private String status;
        private String customerType;
        private String customAttrs;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        
        public UUID getTenantId() { return tenantId; }
        public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getExternalCustomerId() { return externalCustomerId; }
        public void setExternalCustomerId(String externalCustomerId) { this.externalCustomerId = externalCustomerId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getCustomerType() { return customerType; }
        public void setCustomerType(String customerType) { this.customerType = customerType; }
        
        public String getCustomAttrs() { return customAttrs; }
        public void setCustomAttrs(String customAttrs) { this.customAttrs = customAttrs; }
        
        public OffsetDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
        
        public OffsetDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
