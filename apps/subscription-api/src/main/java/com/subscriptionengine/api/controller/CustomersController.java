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
import java.util.Map;
import java.util.UUID;

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
        
        customersDao.insert(customer);
        
        logger.info("Successfully created customer: {}", customer.getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customer.getId().toString());
        data.put("email", customer.getEmail());
        data.put("name", customer.getFirstName());
        data.put("externalCustomerRef", customer.getExternalCustomerId());
        
        response.put("data", data);
        
        return ResponseEntity.ok(response);
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
}
