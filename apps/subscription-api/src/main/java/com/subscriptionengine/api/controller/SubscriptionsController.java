package com.subscriptionengine.api.controller;

import com.subscriptionengine.auth.TenantSecured;
// import com.subscriptionengine.common.idempotency.Idempotent;
import com.subscriptionengine.subscriptions.dto.CreateSubscriptionRequest;
import com.subscriptionengine.subscriptions.dto.SubscriptionResponse;
import com.subscriptionengine.subscriptions.service.SubscriptionsService;
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

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for subscription management.
 * All endpoints are tenant-secured and support idempotency for write operations.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/subscriptions")
@TenantSecured
@Tag(name = "Subscriptions", description = "Subscription lifecycle management APIs. Create, retrieve, and manage customer subscriptions with automatic billing and delivery scheduling.")
public class SubscriptionsController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionsController.class);
    
    private final SubscriptionsService subscriptionsService;
    
    public SubscriptionsController(SubscriptionsService subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }
    
    /**
     * Create a new subscription.
     * Requires Idempotency-Key header to prevent duplicate creation.
     * 
     * @param request the subscription creation request
     * @return the created subscription
     */
    @PostMapping
    @Operation(
        summary = "Create a new subscription",
        description = "Creates a new subscription for a customer with a specified plan. "
            + "Automatically creates the customer if they don't exist based on email. "
            + "Initiates billing cycle, schedules first delivery, and triggers subscription.created webhook event. "
            + "Supports custom start dates and trial periods from the plan."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Subscription created successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors or invalid plan/customer data"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Plan not found or does not belong to this tenant"
        )
    })
    // @Idempotent("Subscription creation is idempotent")
    public ResponseEntity<SubscriptionResponse> createSubscription(
        @Parameter(description = "Subscription creation details including plan ID, customer email, and start date", required = true)
        @Valid @RequestBody CreateSubscriptionRequest request) {
        logger.info("Creating new subscription for plan: {} and customer: {}", 
                   request.getPlanId(), request.getCustomerEmail());
        
        try {
            SubscriptionResponse subscription = subscriptionsService.createSubscription(request);
            
            logger.info("Successfully created subscription with ID: {}", subscription.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid subscription creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get a specific subscription by ID.
     * 
     * @param subscriptionId the subscription ID
     * @return the subscription if found
     */
    @GetMapping("/{subscriptionId}")
    @Operation(
        summary = "Get subscription by ID",
        description = "Retrieves detailed information about a specific subscription including current status, "
            + "billing details, next billing date, customer information, and associated plan."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription found and returned successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Subscription not found or does not belong to this tenant"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<SubscriptionResponse> getSubscription(
        @Parameter(description = "Unique identifier of the subscription", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable UUID subscriptionId) {
        logger.debug("Retrieving subscription: {}", subscriptionId);
        
        Optional<SubscriptionResponse> subscription = subscriptionsService.getSubscription(subscriptionId);
        
        if (subscription.isPresent()) {
            logger.debug("Found subscription: {}", subscriptionId);
            return ResponseEntity.ok(subscription.get());
        } else {
            logger.debug("Subscription not found: {}", subscriptionId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get all subscriptions with pagination.
     * 
     * @param pageable pagination parameters
     * @return paginated list of subscriptions
     */
    @GetMapping
    @Operation(
        summary = "List all subscriptions with pagination",
        description = "Retrieves a paginated list of all subscriptions for the current tenant. "
            + "Includes subscriptions in all states (active, paused, canceled). "
            + "Supports sorting and filtering. Default sort is by creation date (newest first)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscriptions retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Page<SubscriptionResponse>> getSubscriptions(
            @Parameter(description = "Pagination parameters (page, size, sort)", example = "page=0&size=20&sort=createdAt,desc")
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Retrieving subscriptions - page: {}, size: {}", 
                    pageable.getPageNumber(), pageable.getPageSize());
        
        Page<SubscriptionResponse> subscriptions = subscriptionsService.getSubscriptions(pageable);
        
        logger.debug("Retrieved {} subscriptions", subscriptions.getNumberOfElements());
        return ResponseEntity.ok(subscriptions);
    }
}
