package com.subscriptionengine.api.controller;

import com.subscriptionengine.auth.TenantSecured;
// import com.subscriptionengine.common.idempotency.Idempotent;
import com.subscriptionengine.plans.dto.CreatePlanRequest;
import com.subscriptionengine.plans.dto.PlanResponse;
import com.subscriptionengine.plans.service.PlansService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for subscription plans management.
 * All endpoints are tenant-secured and support idempotency for write operations.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/plans")
@TenantSecured
@Tag(name = "Plans", description = "Subscription plan management APIs. Create and manage subscription plans with pricing, billing intervals, and trial periods.")
public class PlansController {
    
    private static final Logger logger = LoggerFactory.getLogger(PlansController.class);
    
    private final PlansService plansService;
    
    public PlansController(PlansService plansService) {
        this.plansService = plansService;
    }
    
    /**
     * Create a new subscription plan.
     * Requires Idempotency-Key header to prevent duplicate creation.
     * 
     * @param request the plan creation request
     * @return the created plan
     */
    @PostMapping
    @Operation(
        summary = "Create a new subscription plan",
        description = "Creates a new subscription plan with specified pricing, billing interval, and features. "
            + "Plans define the pricing structure and billing frequency for subscriptions. "
            + "Supports trial periods, custom billing intervals (monthly, yearly, etc.), and multiple currencies."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Plan created successfully",
            content = @Content(schema = @Schema(implementation = PlanResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors in plan data"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - plan with same name already exists for this tenant"
        )
    })
    // @Idempotent("Plan creation is idempotent")
    public ResponseEntity<PlanResponse> createPlan(
        @Parameter(description = "Plan creation details including name, pricing, and billing interval", required = true)
        @Valid @RequestBody CreatePlanRequest request) {
        logger.info("Creating new plan: {}", request.getName());
        
        PlanResponse plan = plansService.createPlan(request);
        
        logger.info("Successfully created plan with ID: {}", plan.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }
    
    /**
     * Get a specific plan by ID.
     * 
     * @param planId the plan ID
     * @return the plan if found
     */
    @GetMapping("/{planId}")
    @Operation(
        summary = "Get plan by ID",
        description = "Retrieves detailed information about a specific subscription plan including pricing, billing interval, trial period, and current status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plan found and returned successfully",
            content = @Content(schema = @Schema(implementation = PlanResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Plan not found or does not belong to this tenant"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<PlanResponse> getPlan(
        @Parameter(description = "Unique identifier of the plan", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID planId) {
        logger.debug("Retrieving plan: {}", planId);
        
        Optional<PlanResponse> plan = plansService.getPlan(planId);
        
        if (plan.isPresent()) {
            logger.debug("Found plan: {}", planId);
            return ResponseEntity.ok(plan.get());
        } else {
            logger.debug("Plan not found: {}", planId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all plans with pagination.
     * 
     * @param pageable pagination parameters
     * @return paginated list of plans
     */
    @GetMapping
    @Operation(
        summary = "List all plans with pagination",
        description = "Retrieves a paginated list of all subscription plans for the current tenant. "
            + "Supports sorting and filtering. Default sort is by creation date (newest first)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plans retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Page<PlanResponse>> getPlans(
            @Parameter(description = "Pagination parameters (page, size, sort)", example = "page=0&size=20&sort=createdAt,desc")
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Retrieving plans - page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        Page<PlanResponse> plans = plansService.getPlans(pageable);
        
        logger.debug("Retrieved {} plans", plans.getNumberOfElements());
        return ResponseEntity.ok(plans);
    }
    
    /**
     * Get all active plans.
     * 
     * @return list of active plans
     */
    @GetMapping("/active")
    @Operation(
        summary = "Get all active plans",
        description = "Retrieves all currently active subscription plans. "
            + "Active plans are available for new subscriptions. "
            + "Inactive plans are hidden from customers but existing subscriptions continue."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active plans retrieved successfully",
            content = @Content(schema = @Schema(implementation = List.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<List<PlanResponse>> getActivePlans() {
        logger.debug("Retrieving active plans");
        
        List<PlanResponse> plans = plansService.getActivePlans();
        
        logger.debug("Retrieved {} active plans", plans.size());
        return ResponseEntity.ok(plans);
    }
    
    /**
     * Update a plan's active status.
     * Requires Idempotency-Key header to prevent duplicate updates.
     * 
     * @param planId the plan ID
     * @param active the new active status
     * @return the updated plan if found
     */
    @PatchMapping("/{planId}/status")
    @Operation(
        summary = "Update plan active status",
        description = "Activates or deactivates a subscription plan. "
            + "Deactivating a plan prevents new subscriptions but does not affect existing ones. "
            + "This is useful for retiring old plans while maintaining existing customer subscriptions."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plan status updated successfully",
            content = @Content(schema = @Schema(implementation = PlanResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Plan not found or does not belong to this tenant"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    // @Idempotent("Plan status update is idempotent")
    public ResponseEntity<PlanResponse> updatePlanStatus(
            @Parameter(description = "Unique identifier of the plan", required = true)
            @PathVariable UUID planId,
            @Parameter(description = "New active status (true = active, false = inactive)", required = true, example = "true")
            @RequestParam boolean active) {
        
        logger.info("Updating plan {} status to: {}", planId, active);
        
        Optional<PlanResponse> plan = plansService.updatePlanStatus(planId, active);
        
        if (plan.isPresent()) {
            logger.info("Successfully updated plan {} status to: {}", planId, active);
            return ResponseEntity.ok(plan.get());
        } else {
            logger.warn("Plan not found for status update: {}", planId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Check if a plan exists.
     * 
     * @param planId the plan ID
     * @return 200 if exists, 404 if not found
     */
    @GetMapping("/{planId}/exists")
    @Operation(
        summary = "Check if plan exists",
        description = "Verifies whether a plan with the given ID exists for the current tenant. "
            + "Returns 200 OK if the plan exists, 404 Not Found otherwise. "
            + "Useful for validation before creating subscriptions."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plan exists"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Plan not found or does not belong to this tenant"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Void> checkPlanExists(
        @Parameter(description = "Unique identifier of the plan to check", required = true)
        @PathVariable UUID planId) {
        logger.debug("Checking if plan exists: {}", planId);
        
        boolean exists = plansService.planExists(planId);
        
        if (exists) {
            logger.debug("Plan exists: {}", planId);
            return ResponseEntity.ok().build();
        } else {
            logger.debug("Plan does not exist: {}", planId);
            return ResponseEntity.notFound().build();
        }
    }
}
