package com.subscriptionengine.api.controller;

import com.subscriptionengine.auth.TenantSecured;
import com.subscriptionengine.delivery.service.DeliveryManagementService;
import com.subscriptionengine.subscriptions.service.SubscriptionManagementService;
import com.subscriptionengine.subscriptions.service.SubscriptionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/v1/customers")
@TenantSecured
@Tag(name = "Customer Dashboard", description = "Customer-facing subscription dashboard APIs. Provides comprehensive views for customers to view and manage their subscriptions and deliveries.")
public class CustomerSubscriptionsController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerSubscriptionsController.class);
    
    private final SubscriptionsService subscriptionsService;
    private final SubscriptionManagementService subscriptionManagementService;
    private final DeliveryManagementService deliveryManagementService;
    
    public CustomerSubscriptionsController(
            SubscriptionsService subscriptionsService,
            SubscriptionManagementService subscriptionManagementService,
            DeliveryManagementService deliveryManagementService) {
        this.subscriptionsService = subscriptionsService;
        this.subscriptionManagementService = subscriptionManagementService;
        this.deliveryManagementService = deliveryManagementService;
    }
    
    /**
     * Get all subscriptions for a customer.
     * 
     * @param customerId the customer ID
     * @param limit maximum number of subscriptions to return (default: 20)
     * @return list of customer subscriptions
     */
    @GetMapping("/{customerId}/subscriptions")
    @Operation(
        summary = "Get all subscriptions for a customer",
        description = "Retrieves all subscriptions belonging to a specific customer. "
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
            @Parameter(description = "Unique identifier of the customer", required = true)
            @PathVariable UUID customerId,
            @Parameter(description = "Maximum number of subscriptions to return", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[CUSTOMER_SUBSCRIPTIONS_LIST] RequestId: {} - Getting subscriptions for customer {}", 
                   requestId, customerId);
        
        try {
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
            
        } catch (Exception e) {
            logger.error("[CUSTOMER_SUBSCRIPTIONS_LIST_ERROR] RequestId: {} - Error: {}", 
                        requestId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving customer subscriptions: " + e.getMessage(),
                "requestId", requestId,
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * Get comprehensive dashboard view for a specific subscription.
     * Includes subscription details, management capabilities, and upcoming deliveries.
     * 
     * @param customerId the customer ID for authorization
     * @param subscriptionId the subscription ID
     * @return comprehensive subscription dashboard data
     */
    @GetMapping("/{customerId}/subscriptions/{subscriptionId}/dashboard")
    @Operation(
        summary = "Get comprehensive subscription dashboard",
        description = "Retrieves a complete dashboard view for a specific subscription including: "
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
            @Parameter(description = "Unique identifier of the customer for authorization", required = true)
            @PathVariable UUID customerId,
            @Parameter(description = "Unique identifier of the subscription", required = true)
            @PathVariable UUID subscriptionId) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[SUBSCRIPTION_DASHBOARD] RequestId: {} - Getting dashboard for subscription {} customer {}", 
                   requestId, subscriptionId, customerId);
        
        try {
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
            
        } catch (Exception e) {
            logger.error("[SUBSCRIPTION_DASHBOARD_ERROR] RequestId: {} - Error: {}", 
                        requestId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving subscription dashboard: " + e.getMessage(),
                "requestId", requestId,
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
