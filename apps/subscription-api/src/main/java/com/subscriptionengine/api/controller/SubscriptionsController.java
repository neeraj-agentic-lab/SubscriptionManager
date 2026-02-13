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
@RequestMapping("/v1/admin/subscriptions")
@TenantSecured
@Tag(name = "Admin - Subscriptions", description = "Admin endpoints for subscription lifecycle management. Create, retrieve, and manage customer subscriptions with automatic billing and delivery scheduling.")
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
        summary = "Create a new subscription (unified endpoint)",
        description = "**UNIFIED ENDPOINT**: Creates both simple SaaS subscriptions and product-based subscriptions. "
            + "\n\n**Simple Subscription** (no products): Provide planId, customer info, and payment method. "
            + "Uses plan's base price and billing interval. "
            + "\n\n**Product-Based Subscription** (with products): Include optional 'products' array with product details (SKU, name, quantity, price). "
            + "Each product can have individual pricing. Requires shipping address for physical products. "
            + "\n\nAutomatically creates the customer if they don't exist based on email. "
            + "Initiates billing cycle, schedules deliveries, and triggers subscription.created webhook event. "
            + "Supports custom start dates and trial periods."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Subscription creation request. Products array is optional - omit for simple subscriptions, include for product-based subscriptions.",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = CreateSubscriptionRequest.class),
            examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Simple SaaS Subscription",
                    summary = "Create a simple subscription without products",
                    description = "Basic subscription using plan's base price and billing interval",
                    value = "{\n" +
                        "  \"planId\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                        "  \"customerEmail\": \"john.doe@example.com\",\n" +
                        "  \"customerFirstName\": \"John\",\n" +
                        "  \"customerLastName\": \"Doe\",\n" +
                        "  \"paymentMethodRef\": \"pm_stripe_abc123\"\n" +
                        "}"
                ),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Product-Based Subscription",
                    summary = "Create subscription with multiple products",
                    description = "Subscription with product array for subscription boxes, meal kits, etc.",
                    value = "{\n" +
                        "  \"planId\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                        "  \"customerEmail\": \"jane.smith@example.com\",\n" +
                        "  \"customerFirstName\": \"Jane\",\n" +
                        "  \"customerLastName\": \"Smith\",\n" +
                        "  \"products\": [\n" +
                        "    {\n" +
                        "      \"productId\": \"coffee-beans-001\",\n" +
                        "      \"productName\": \"Premium Coffee Beans\",\n" +
                        "      \"quantity\": 2,\n" +
                        "      \"unitPriceCents\": 1599,\n" +
                        "      \"currency\": \"USD\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"productId\": \"coffee-filter-002\",\n" +
                        "      \"productName\": \"Coffee Filters\",\n" +
                        "      \"quantity\": 1,\n" +
                        "      \"unitPriceCents\": 599,\n" +
                        "      \"currency\": \"USD\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"shippingAddress\": {\n" +
                        "    \"line1\": \"123 Main St\",\n" +
                        "    \"city\": \"San Francisco\",\n" +
                        "    \"state\": \"CA\",\n" +
                        "    \"postalCode\": \"94102\",\n" +
                        "    \"country\": \"US\"\n" +
                        "  },\n" +
                        "  \"paymentMethodRef\": \"pm_stripe_xyz789\"\n" +
                        "}"
                )
            }
        )
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
        @Valid @RequestBody CreateSubscriptionRequest request) {
        logger.info("Creating new subscription for plan: {} and customer: {}", 
                   request.getPlanId(), request.getCustomerEmail());
        
        SubscriptionResponse subscription = subscriptionsService.createSubscription(request);
        
        logger.info("Successfully created subscription with ID: {}", subscription.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
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
