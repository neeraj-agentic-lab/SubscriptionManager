package com.subscriptionengine.api.controller;

import com.subscriptionengine.delivery.service.DeliveryManagementService;
import com.subscriptionengine.auth.TenantSecured;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API controller for customer delivery management.
 * Allows customers to view and manage their upcoming deliveries.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/admin/deliveries")
@TenantSecured
@Tag(name = "Admin - Deliveries", description = "Admin endpoints for delivery management. View upcoming deliveries, check delivery details, and manage delivery scheduling for subscription orders.")
public class DeliveryController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeliveryController.class);
    
    private final DeliveryManagementService deliveryManagementService;
    
    public DeliveryController(DeliveryManagementService deliveryManagementService) {
        this.deliveryManagementService = deliveryManagementService;
    }
    
    /**
     * Get upcoming deliveries for a customer.
     * 
     * @param customerId Customer ID to get deliveries for
     * @param limit Maximum number of deliveries to return (default: 10)
     * @return List of upcoming delivery instances
     */
    @GetMapping("/upcoming")
    @Operation(
        summary = "Get upcoming deliveries for a customer",
        description = "Retrieves scheduled upcoming deliveries for a customer across all their subscriptions. "
            + "Includes delivery dates, subscription details, and delivery status. "
            + "Useful for showing customers when their next orders will arrive."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Upcoming deliveries retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> getUpcomingDeliveries(
            @Parameter(description = "Customer ID to retrieve deliveries for", required = true)
            @RequestParam UUID customerId,
            @Parameter(description = "Maximum number of deliveries to return", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[DELIVERY_API_UPCOMING] RequestId: {} - Getting upcoming deliveries for customer {}", 
                   requestId, customerId);
        
        List<Map<String, Object>> deliveries = deliveryManagementService.getUpcomingDeliveries(customerId, limit);
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", Map.of(
                "deliveries", deliveries,
                "count", deliveries.size(),
                "limit", limit
            ),
            "timestamp", System.currentTimeMillis(),
            "requestId", requestId
        );
        
        logger.info("[DELIVERY_API_UPCOMING_SUCCESS] RequestId: {} - Retrieved {} upcoming deliveries for customer {}", 
                   requestId, deliveries.size(), customerId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get details of a specific delivery instance.
     * 
     * @param deliveryId Delivery instance ID
     * @param customerId Customer ID for authorization
     * @return Delivery instance details
     */
    @GetMapping("/{deliveryId}")
    @Operation(
        summary = "Get delivery details",
        description = "Retrieves detailed information about a specific delivery including scheduled date, "
            + "subscription details, delivery status, and tracking information if available. "
            + "Requires customer ID for authorization."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Delivery details retrieved successfully",
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
    public ResponseEntity<Map<String, Object>> getDeliveryDetails(
            @Parameter(description = "Unique identifier of the delivery", required = true)
            @PathVariable UUID deliveryId,
            @Parameter(description = "Customer ID for authorization", required = true)
            @RequestParam UUID customerId) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[DELIVERY_API_DETAILS] RequestId: {} - Getting delivery details {} for customer {}", 
                   requestId, deliveryId, customerId);
        
        Optional<Map<String, Object>> deliveryOpt = deliveryManagementService.getDeliveryDetails(deliveryId, customerId);
        
        if (deliveryOpt.isEmpty()) {
            logger.warn("[DELIVERY_API_DETAILS_NOT_FOUND] RequestId: {} - Delivery {} not found for customer {}", 
                       requestId, deliveryId, customerId);
            
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Delivery not found or not accessible",
                "timestamp", System.currentTimeMillis(),
                "requestId", requestId
            ));
        }
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", deliveryOpt.get(),
            "timestamp", System.currentTimeMillis(),
            "requestId", requestId
        );
        
        logger.info("[DELIVERY_API_DETAILS_SUCCESS] RequestId: {} - Retrieved delivery details for {}", 
                   requestId, deliveryId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel a specific upcoming delivery.
     * Only allows cancellation if the delivery hasn't started order processing.
     * 
     * @param deliveryId Delivery instance ID to cancel
     * @param request Cancellation request containing customer ID and reason
     * @return Cancellation result
     */
    @PostMapping("/{deliveryId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelDelivery(
            @PathVariable UUID deliveryId,
            @RequestBody Map<String, Object> request) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        UUID customerId = UUID.fromString((String) request.get("customerId"));
        String reason = (String) request.getOrDefault("reason", "Cancelled by customer");
        
        logger.info("[DELIVERY_API_CANCEL] RequestId: {} - Cancelling delivery {} for customer {} (reason: {})", 
                   requestId, deliveryId, customerId, reason);
        
        boolean cancelled = deliveryManagementService.cancelDelivery(deliveryId, customerId, reason);
        
        if (!cancelled) {
            logger.warn("[DELIVERY_API_CANCEL_FAILED] RequestId: {} - Failed to cancel delivery {} for customer {}", 
                       requestId, deliveryId, customerId);
            
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "message", "Cannot cancel delivery - either not found, not owned by customer, or already in processing",
                "timestamp", System.currentTimeMillis(),
                "requestId", requestId
            ));
        }
        
        Map<String, Object> response = Map.of(
            "success", true,
            "message", "Delivery cancelled successfully",
            "data", Map.of(
                "deliveryId", deliveryId.toString(),
                "customerId", customerId.toString(),
                "reason", reason,
                "cancelledAt", System.currentTimeMillis()
            ),
            "timestamp", System.currentTimeMillis(),
            "requestId", requestId
        );
        
        logger.info("[DELIVERY_API_CANCEL_SUCCESS] RequestId: {} - Successfully cancelled delivery {} for customer {}", 
                   requestId, deliveryId, customerId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get delivery cancellation eligibility.
     * Checks if a delivery can be cancelled without actually cancelling it.
     * 
     * @param deliveryId Delivery instance ID
     * @param customerId Customer ID for authorization
     * @return Cancellation eligibility information
     */
    @GetMapping("/{deliveryId}/can-cancel")
    public ResponseEntity<Map<String, Object>> canCancelDelivery(
            @PathVariable UUID deliveryId,
            @RequestParam UUID customerId) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[DELIVERY_API_CAN_CANCEL] RequestId: {} - Checking cancellation eligibility for delivery {} (customer: {})", 
                   requestId, deliveryId, customerId);
        
        Optional<Map<String, Object>> deliveryOpt = deliveryManagementService.getDeliveryDetails(deliveryId, customerId);
        
        if (deliveryOpt.isEmpty()) {
            logger.warn("[DELIVERY_API_CAN_CANCEL_NOT_FOUND] RequestId: {} - Delivery {} not found for customer {}", 
                       requestId, deliveryId, customerId);
            
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Delivery not found or not accessible",
                "timestamp", System.currentTimeMillis(),
                "requestId", requestId
            ));
        }
        
        Map<String, Object> delivery = deliveryOpt.get();
        boolean canCancel = (Boolean) delivery.get("canCancel");
        String status = (String) delivery.get("status");
        String externalOrderRef = (String) delivery.get("externalOrderRef");
        
        String reason = "";
        if (!canCancel) {
            if (!"PENDING".equals(status)) {
                reason = "Delivery is already in " + status + " status";
            } else if (externalOrderRef != null && !externalOrderRef.trim().isEmpty()) {
                reason = "Order has already been created with external system";
            }
        }
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", Map.of(
                "deliveryId", deliveryId.toString(),
                "canCancel", canCancel,
                "status", status,
                "reason", reason,
                "externalOrderRef", externalOrderRef != null ? externalOrderRef : ""
            ),
            "timestamp", System.currentTimeMillis(),
            "requestId", requestId
        );
        
        logger.info("[DELIVERY_API_CAN_CANCEL_SUCCESS] RequestId: {} - Delivery {} cancellation eligibility: {}", 
                   requestId, deliveryId, canCancel);
        
        return ResponseEntity.ok(response);
    }
}
