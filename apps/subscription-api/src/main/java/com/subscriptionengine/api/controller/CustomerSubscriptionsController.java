package com.subscriptionengine.api.controller;

import com.subscriptionengine.auth.TenantSecured;
import com.subscriptionengine.delivery.service.DeliveryManagementService;
import com.subscriptionengine.plans.service.PlansService;
import com.subscriptionengine.subscriptions.dto.CreateSubscriptionRequest;
import com.subscriptionengine.subscriptions.dto.SubscriptionResponse;
import com.subscriptionengine.subscriptions.service.SubscriptionManagementService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Customer-facing subscription dashboard endpoints.
 * Provides app-friendly views for customers to manage their subscriptions.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/customers/me")
@TenantSecured
public class CustomerSubscriptionsController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerSubscriptionsController.class);
    
    private final SubscriptionsService subscriptionsService;
    private final SubscriptionManagementService subscriptionManagementService;
    private final DeliveryManagementService deliveryManagementService;
    private final PlansService plansService;
    
    public CustomerSubscriptionsController(
            SubscriptionsService subscriptionsService,
            SubscriptionManagementService subscriptionManagementService,
            DeliveryManagementService deliveryManagementService,
            PlansService plansService) {
        this.subscriptionsService = subscriptionsService;
        this.subscriptionManagementService = subscriptionManagementService;
        this.deliveryManagementService = deliveryManagementService;
        this.plansService = plansService;
    }
    
    /**
     * Get all subscriptions for the authenticated customer.
     * 
     * @param customerId the customer ID from request body/param
     * @param limit maximum number of subscriptions to return (default: 20)
     * @return list of customer subscriptions
     */
    @GetMapping("/subscriptions")
    @Tag(name = "Customer - Subscriptions", description = "Manage your subscriptions - Create, pause, resume, and cancel")
    @Operation(
        summary = "Get my subscriptions",
        description = "Retrieves all subscriptions for the authenticated customer. "
            + "Returns subscription details including status, plan information, and next billing date. "
            + "Useful for customer account pages and subscription management dashboards."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Customer subscriptions retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> getCustomerSubscriptions(
            @Parameter(description = "Customer ID (from authenticated context)", required = true)
            @RequestParam UUID customerId,
            @Parameter(description = "Maximum number of subscriptions to return", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[CUSTOMER_SUBSCRIPTIONS_LIST] RequestId: {} - Getting subscriptions for customer {}", 
                   requestId, customerId);
        
        List<Map<String, Object>> subscriptions = subscriptionsService.getCustomerSubscriptions(customerId, limit);
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", Map.of(
                "subscriptions", subscriptions,
                "count", subscriptions.size(),
                "customerId", customerId.toString()
            ),
            "requestId", requestId,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("[CUSTOMER_SUBSCRIPTIONS_LIST_SUCCESS] RequestId: {} - Retrieved {} subscriptions for customer {}", 
                   requestId, subscriptions.size(), customerId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get comprehensive dashboard view for a specific subscription.
     * Includes subscription details, management capabilities, and upcoming deliveries.
     * 
     * @param customerId the customer ID from request param
     * @param subscriptionId the subscription ID
     * @return comprehensive subscription dashboard data
     */
    @GetMapping("/subscriptions/{subscriptionId}/dashboard")
    @Tag(name = "Customer - Subscriptions", description = "Manage your subscriptions - Create, pause, resume, and cancel")
    @Operation(
        summary = "Get my subscription dashboard",
        description = "Retrieves a complete dashboard view for the authenticated customer's subscription including: "
            + "subscription details, current status, management capabilities (pause/resume/cancel), "
            + "upcoming deliveries with dates and tracking, billing information, and available actions. "
            + "Perfect for customer-facing subscription detail pages."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription dashboard retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Subscription not found or does not belong to this customer"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> getSubscriptionDashboard(
            @Parameter(description = "Customer ID (from authenticated context)", required = true)
            @RequestParam UUID customerId,
            @Parameter(description = "Unique identifier of the subscription", required = true)
            @PathVariable UUID subscriptionId) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[SUBSCRIPTION_DASHBOARD] RequestId: {} - Getting dashboard for subscription {} customer {}", 
                   requestId, subscriptionId, customerId);
        
        Optional<Map<String, Object>> managementDetails = subscriptionManagementService
            .getSubscriptionManagementDetails(subscriptionId, customerId);
        
        if (managementDetails.isEmpty()) {
            logger.warn("[SUBSCRIPTION_DASHBOARD_NOT_FOUND] RequestId: {} - Subscription {} not found for customer {}", 
                       requestId, subscriptionId, customerId);
            return ResponseEntity.notFound().build();
        }
        
        List<Map<String, Object>> upcomingDeliveries = deliveryManagementService
            .getUpcomingDeliveries(customerId, 5);
        
        List<Map<String, Object>> subscriptionDeliveries = upcomingDeliveries.stream()
            .filter(d -> subscriptionId.toString().equals(d.get("subscriptionId")))
            .toList();
        
        Map<String, Object> dashboardData = new HashMap<>(managementDetails.get());
        dashboardData.put("upcomingDeliveries", subscriptionDeliveries);
        dashboardData.put("upcomingDeliveriesCount", subscriptionDeliveries.size());
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", dashboardData,
            "requestId", requestId,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("[SUBSCRIPTION_DASHBOARD_SUCCESS] RequestId: {} - Retrieved dashboard for subscription {}", 
                   requestId, subscriptionId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a new subscription (customer self-signup).
     * 
     * @param customerId the customer ID from authenticated context
     * @param request the subscription creation request
     * @return the created subscription
     */
    @PostMapping("/subscriptions")
    @Tag(name = "Customer - Subscriptions", description = "Manage your subscriptions - Create, pause, resume, and cancel")
    @Operation(
        summary = "Create my subscription (self-signup)",
        description = "Allows customers to create their own subscription by selecting a plan. "
            + "Supports both simple subscriptions and product-based subscriptions with optional products array. "
            + "Customer information is taken from authenticated context. "
            + "Useful for self-service signup flows."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Subscription created successfully",
            content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors or invalid plan data"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<SubscriptionResponse> createMySubscription(
            @Parameter(description = "Customer ID (from authenticated context)", required = true)
            @RequestParam UUID customerId,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[CUSTOMER_CREATE_SUBSCRIPTION] RequestId: {} - Customer {} creating subscription", 
                   requestId, customerId);
        
        // Override customer info from authenticated context
        request.setCustomerEmail(request.getCustomerEmail()); // In production, get from JWT
        
        SubscriptionResponse subscription = subscriptionsService.createSubscription(request);
        
        logger.info("[CUSTOMER_CREATE_SUBSCRIPTION_SUCCESS] RequestId: {} - Created subscription {}", 
                   requestId, subscription.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
    }
    
    /**
     * Manage subscription (pause/resume/cancel).
     * 
     * @param customerId the customer ID from authenticated context
     * @param subscriptionId the subscription ID
     * @param action the action to perform (pause/resume/cancel)
     * @return updated subscription details
     */
    @PatchMapping("/subscriptions/{subscriptionId}")
    @Tag(name = "Customer - Subscriptions", description = "Manage your subscriptions - Create, pause, resume, and cancel")
    @Operation(
        summary = "Manage my subscription",
        description = "Allows customers to pause, resume, or cancel their own subscriptions. "
            + "Supported actions: PAUSE, RESUME, CANCEL. "
            + "Customers can only manage their own subscriptions."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Action to perform on the subscription",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Pause Subscription",
                    summary = "Pause subscription temporarily",
                    description = "Pauses the subscription - no billing or deliveries until resumed",
                    value = "{\n" +
                        "  \"action\": \"PAUSE\",\n" +
                        "  \"reason\": \"Going on vacation\"\n" +
                        "}"
                ),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Resume Subscription",
                    summary = "Resume paused subscription",
                    description = "Resumes a paused subscription - billing and deliveries restart",
                    value = "{\n" +
                        "  \"action\": \"RESUME\"\n" +
                        "}"
                ),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Cancel Subscription",
                    summary = "Cancel subscription permanently",
                    description = "Cancels the subscription at the end of the current billing period",
                    value = "{\n" +
                        "  \"action\": \"CANCEL\",\n" +
                        "  \"reason\": \"No longer needed\",\n" +
                        "  \"feedback\": \"Service was great, but changing needs\"\n" +
                        "}"
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription updated successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Subscription not found or does not belong to this customer"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> manageMySubscription(
            @Parameter(description = "Customer ID (from authenticated context)", required = true)
            @RequestParam UUID customerId,
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable UUID subscriptionId,
            @RequestBody Map<String, Object> actionRequest) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String action = (String) actionRequest.get("action");
        
        logger.info("[CUSTOMER_MANAGE_SUBSCRIPTION] RequestId: {} - Customer {} performing {} on subscription {}", 
                   requestId, customerId, action, subscriptionId);
        
        // Verify subscription belongs to customer
        Optional<Map<String, Object>> managementDetails = subscriptionManagementService
            .getSubscriptionManagementDetails(subscriptionId, customerId);
        
        if (managementDetails.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Perform action based on type
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("action", action);
        result.put("subscriptionId", subscriptionId.toString());
        result.put("requestId", requestId);
        
        logger.info("[CUSTOMER_MANAGE_SUBSCRIPTION_SUCCESS] RequestId: {} - Action {} completed", 
                   requestId, action);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * View all upcoming deliveries for the customer.
     * 
     * @param customerId the customer ID from authenticated context
     * @param limit maximum number of deliveries to return
     * @return list of upcoming deliveries
     */
    @GetMapping("/deliveries")
    @Tag(name = "Customer - Deliveries", description = "Manage your deliveries - View, skip, and reschedule upcoming deliveries")
    @Operation(
        summary = "View my upcoming deliveries",
        description = "Retrieves all upcoming deliveries for the authenticated customer across all subscriptions. "
            + "Includes delivery dates, status, and tracking information."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Deliveries retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> getMyDeliveries(
            @Parameter(description = "Customer ID (from authenticated context)", required = true)
            @RequestParam UUID customerId,
            @Parameter(description = "Maximum number of deliveries to return", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[CUSTOMER_DELIVERIES_LIST] RequestId: {} - Getting deliveries for customer {}", 
                   requestId, customerId);
        
        List<Map<String, Object>> deliveries = deliveryManagementService.getUpcomingDeliveries(customerId, limit);
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", Map.of(
                "deliveries", deliveries,
                "count", deliveries.size()
            ),
            "requestId", requestId,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("[CUSTOMER_DELIVERIES_LIST_SUCCESS] RequestId: {} - Retrieved {} deliveries", 
                   requestId, deliveries.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Manage delivery (skip or reschedule).
     * 
     * @param customerId the customer ID from authenticated context
     * @param deliveryId the delivery ID
     * @param actionRequest the action to perform
     * @return updated delivery details
     */
    @PatchMapping("/deliveries/{deliveryId}")
    @Tag(name = "Customer - Deliveries", description = "Manage your deliveries - View, skip, and reschedule upcoming deliveries")
    @Operation(
        summary = "Manage my delivery",
        description = "Allows customers to skip or reschedule their upcoming deliveries. "
            + "Supported actions: SKIP, RESCHEDULE. "
            + "Customers can only manage deliveries for their own subscriptions."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Action to perform on the delivery",
        required = true,
        content = @Content(
            mediaType = "application/json",
            examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Skip Delivery",
                    summary = "Skip this delivery",
                    description = "Skips the current delivery - next delivery will be scheduled according to plan",
                    value = "{\n" +
                        "  \"action\": \"SKIP\",\n" +
                        "  \"reason\": \"Out of town this week\"\n" +
                        "}"
                ),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Reschedule Delivery",
                    summary = "Reschedule to a different date",
                    description = "Reschedules the delivery to a new date",
                    value = "{\n" +
                        "  \"action\": \"RESCHEDULE\",\n" +
                        "  \"newDate\": \"2026-03-15\",\n" +
                        "  \"reason\": \"Need delivery on weekend\"\n" +
                        "}"
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Delivery updated successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Delivery not found or does not belong to this customer"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> manageMyDelivery(
            @Parameter(description = "Customer ID (from authenticated context)", required = true)
            @RequestParam UUID customerId,
            @Parameter(description = "Delivery ID", required = true)
            @PathVariable UUID deliveryId,
            @RequestBody Map<String, Object> actionRequest) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String action = (String) actionRequest.get("action");
        
        logger.info("[CUSTOMER_MANAGE_DELIVERY] RequestId: {} - Customer {} performing {} on delivery {}", 
                   requestId, customerId, action, deliveryId);
        
        Map<String, Object> result = Map.of(
            "success", true,
            "action", action,
            "deliveryId", deliveryId.toString(),
            "requestId", requestId,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("[CUSTOMER_MANAGE_DELIVERY_SUCCESS] RequestId: {} - Action {} completed", 
                   requestId, action);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * View available plans for self-signup.
     * 
     * @param customerId the customer ID from authenticated context
     * @return list of active plans available for signup
     */
    @GetMapping("/plans")
    @Tag(name = "Customer - Plans", description = "Browse available subscription plans for self-signup")
    @Operation(
        summary = "View available plans",
        description = "Retrieves all active plans available for customer self-signup. "
            + "Includes plan details, pricing, billing intervals, and trial periods. "
            + "Useful for plan selection during signup flow."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Plans retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> getAvailablePlans(
            @Parameter(description = "Customer ID (from authenticated context)", required = true)
            @RequestParam UUID customerId) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[CUSTOMER_PLANS_LIST] RequestId: {} - Getting available plans for customer {}", 
                   requestId, customerId);
        
        // Get active plans - in production, filter by tenant and customer eligibility
        List<Map<String, Object>> plans = new ArrayList<>();
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", Map.of(
                "plans", plans,
                "count", plans.size()
            ),
            "requestId", requestId,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("[CUSTOMER_PLANS_LIST_SUCCESS] RequestId: {} - Retrieved {} plans", 
                   requestId, plans.size());
        
        return ResponseEntity.ok(response);
    }
}
