package com.subscriptionengine.api.controller;

// import com.subscriptionengine.common.idempotency.IdempotentResponse;
import com.subscriptionengine.subscriptions.dto.CreateEcommerceSubscriptionRequest;
import com.subscriptionengine.subscriptions.dto.SubscriptionResponse;
import com.subscriptionengine.subscriptions.service.EcommerceSubscriptionService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for simplified ecommerce subscriptions with direct products.
 * No confusing "product plans" - just direct product references.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/subscriptions/ecommerce")
@Tag(name = "Ecommerce Subscriptions", description = "Ecommerce subscription APIs. Create subscriptions with product lists, quantities, and pricing for subscription box or recurring product delivery services.")
public class EcommerceSubscriptionsController {
    
    private static final Logger logger = LoggerFactory.getLogger(EcommerceSubscriptionsController.class);
    
    private final EcommerceSubscriptionService ecommerceSubscriptionService;
    
    public EcommerceSubscriptionsController(EcommerceSubscriptionService ecommerceSubscriptionService) {
        this.ecommerceSubscriptionService = ecommerceSubscriptionService;
    }
    
    /**
     * Create ecommerce subscription with direct products.
     * 
     * POST /v1/subscriptions/ecommerce
     * 
     * Example request:
     * {
     *   "basePlanId": "monthly-delivery-plan",
     *   "products": [
     *     {
     *       "productId": "coffee-beans-sku-001",
     *       "productName": "Premium Coffee Beans",
     *       "quantity": 2,
     *       "unitPriceCents": 1599,
     *       "currency": "USD"
     *     }
     *   ],
     *   "customerEmail": "john@example.com",
     *   "customerFirstName": "John",
     *   "customerLastName": "Doe",
     *   "paymentMethodRef": "pm_stripe_123"
     * }
     */
    @PostMapping
    @Operation(
        summary = "Create ecommerce subscription with product list",
        description = "Creates a subscription with a list of products, quantities, and individual pricing. "
            + "Perfect for subscription boxes, meal kits, or recurring product deliveries. "
            + "Each product has its own SKU, name, quantity, and unit price. "
            + "Automatically creates customer if they don't exist. "
            + "Supports shipping address and payment method references."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Ecommerce subscription created successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors in product data or missing required fields"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Base plan not found"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Object> createEcommerceSubscription(
            @Parameter(description = "Ecommerce subscription details with product list, customer info, and shipping address", required = true)
            @Valid @RequestBody CreateEcommerceSubscriptionRequest request) {
        
        logger.info("Creating ecommerce subscription for {} products", request.getProducts().size());
        
        try {
            SubscriptionResponse subscription = ecommerceSubscriptionService.createEcommerceSubscription(request);
            
            logger.info("Successfully created ecommerce subscription: {}", subscription.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid ecommerce subscription request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_REQUEST",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error creating ecommerce subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", "Failed to create subscription"
            ));
        }
    }
    
}
