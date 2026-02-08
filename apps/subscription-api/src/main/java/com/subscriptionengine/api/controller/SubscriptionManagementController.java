package com.subscriptionengine.api.controller;

import com.subscriptionengine.auth.TenantSecured;
import com.subscriptionengine.subscriptions.service.SubscriptionManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified subscription management controller using clean REST conventions.
 * GET /v1/subscription-mgmt/{id} - Get subscription details
 * PUT /v1/subscription-mgmt/{id} - Update subscription (pause/resume/cancel/modify)
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/subscription-mgmt")
@TenantSecured
@Tag(name = "Subscription Management", description = "Subscription lifecycle operations APIs. Pause, resume, cancel, and modify active subscriptions with customer authorization.")
public class SubscriptionManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionManagementController.class);
    
    private final SubscriptionManagementService subscriptionManagementService;
    
    public SubscriptionManagementController(SubscriptionManagementService subscriptionManagementService) {
        this.subscriptionManagementService = subscriptionManagementService;
    }
    
    /**
     * Get subscription details and management capabilities.
     * 
     * @param subscriptionId the subscription ID
     * @param customerId the customer ID for authorization
     * @return subscription details with management capabilities
     */
    @GetMapping("/{subscriptionId}")
    @Operation(
        summary = "Get subscription management details",
        description = "Retrieves subscription details with available management operations. "
            + "Requires customer ID for authorization to ensure customers can only manage their own subscriptions. "
            + "Returns current status, next billing date, and available actions (pause, resume, cancel)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription details retrieved successfully",
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
    public ResponseEntity<Map<String, Object>> getSubscription(
            @Parameter(description = "Unique identifier of the subscription", required = true)
            @PathVariable UUID subscriptionId,
            @Parameter(description = "Customer ID for authorization", required = true)
            @RequestParam UUID customerId) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[SUBSCRIPTION_MGMT_API_START] RequestId: {} - Getting management details for subscription {} customer {}", 
                   requestId, subscriptionId, customerId);
        
        Optional<Map<String, Object>> details = subscriptionManagementService
            .getSubscriptionManagementDetails(subscriptionId, customerId);
        
        if (details.isEmpty()) {
            logger.warn("[SUBSCRIPTION_MGMT_API_NOT_FOUND] RequestId: {} - Subscription {} not found for customer {}", 
                       requestId, subscriptionId, customerId);
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = Map.of(
            "success", true,
            "data", details.get(),
            "requestId", requestId,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("[SUBSCRIPTION_MGMT_API_SUCCESS] RequestId: {} - Retrieved management details for subscription {}", 
                   requestId, subscriptionId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update subscription - handles pause, resume, cancel, and modify operations.
     * 
     * @param subscriptionId the subscription ID
     * @param request the update request specifying the operation and parameters
     * @return success response
     */
    @PutMapping("/{subscriptionId}")
    @Operation(
        summary = "Update subscription (pause, resume, cancel)",
        description = "Performs subscription lifecycle operations: PAUSE (temporarily stop billing), "
            + "RESUME (reactivate paused subscription), CANCEL (permanently end subscription), "
            + "or MODIFY (change plan or billing details). "
            + "Requires customer ID for authorization. Triggers appropriate webhook events."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Subscription updated successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors or invalid operation for current subscription state"
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
    public ResponseEntity<Map<String, Object>> updateSubscription(
            @Parameter(description = "Unique identifier of the subscription", required = true)
            @PathVariable UUID subscriptionId,
            @Parameter(description = "Update operation details (PAUSE, RESUME, CANCEL, MODIFY) with customer ID", required = true)
            @Valid @RequestBody SubscriptionUpdateRequest request) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        logger.info("[SUBSCRIPTION_UPDATE_START] RequestId: {} - Operation {} for subscription {}", 
                   requestId, request.getOperation(), subscriptionId);
        
        boolean success = false;
        String message = "";
        Map<String, Object> data = Map.of("subscriptionId", subscriptionId.toString());
        
        switch (request.getOperation().toUpperCase()) {
                case "PAUSE":
                    success = subscriptionManagementService.pauseSubscription(
                        subscriptionId, request.getCustomerId(), request.getReason());
                    message = success ? "Subscription paused successfully" : "Failed to pause subscription";
                    if (success) {
                        data = Map.of(
                            "subscriptionId", subscriptionId.toString(),
                            "status", "PAUSED",
                            "operation", "PAUSE",
                            "timestamp", System.currentTimeMillis()
                        );
                    }
                    break;
                    
                case "RESUME":
                    success = subscriptionManagementService.resumeSubscription(
                        subscriptionId, request.getCustomerId());
                    message = success ? "Subscription resumed successfully" : "Failed to resume subscription";
                    if (success) {
                        data = Map.of(
                            "subscriptionId", subscriptionId.toString(),
                            "status", "ACTIVE",
                            "operation", "RESUME",
                            "timestamp", System.currentTimeMillis()
                        );
                    }
                    break;
                    
                case "CANCEL":
                    boolean immediate = request.getCancellationType() != null
                        && request.getCancellationType().equalsIgnoreCase("immediate");

                    success = subscriptionManagementService.cancelSubscription(
                        subscriptionId,
                        request.getCustomerId(),
                        immediate,
                        request.getReason()
                    );
                    message = success ? "Subscription cancelled successfully" : "Failed to cancel subscription";
                    if (success) {
                        data = Map.of(
                            "subscriptionId", subscriptionId.toString(),
                            "status", immediate ? "CANCELED" : "ACTIVE",
                            "operation", "CANCEL",
                            "cancellationType", immediate ? "IMMEDIATE" : "END_OF_PERIOD",
                            "timestamp", System.currentTimeMillis()
                        );
                    }
                    break;
                    
                case "MODIFY":
                    success = subscriptionManagementService.modifySubscription(
                        subscriptionId,
                        request.getCustomerId(),
                        request.getNewPlanId(),
                        request.getNewQuantity(),
                        request.getShippingAddress(),
                        request.getPaymentMethodRef()
                    );
                    message = success ? "Subscription modified successfully" : "Failed to modify subscription";
                    if (success) {
                        data = Map.of(
                            "subscriptionId", subscriptionId.toString(),
                            "operation", "MODIFY",
                            "timestamp", System.currentTimeMillis()
                        );
                    }
                    break;
                    
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid operation: " + request.getOperation() + 
                                  ". Supported: PAUSE, RESUME, CANCEL, MODIFY",
                        "requestId", requestId,
                        "timestamp", System.currentTimeMillis()
                    ));
            }
            
            if (!success) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", message,
                    "requestId", requestId,
                    "timestamp", System.currentTimeMillis()
                ));
            }
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", message,
                "data", data,
                "requestId", requestId,
                "timestamp", System.currentTimeMillis()
            );
            
            logger.info("[SUBSCRIPTION_UPDATE_SUCCESS] RequestId: {} - {} completed for subscription {}", 
                       requestId, request.getOperation(), subscriptionId);
            
            return ResponseEntity.ok(response);
    }
    
    /**
     * Unified request DTO for subscription updates.
     */
    public static class SubscriptionUpdateRequest {
        @NotNull(message = "Customer ID is required")
        private UUID customerId;
        
        @NotBlank(message = "Operation is required")
        private String operation; // PAUSE, RESUME, CANCEL, MODIFY
        
        private String reason;
        private String cancellationType; // immediate, end_of_period
        
        // For MODIFY operations (future use)
        private UUID newPlanId;
        private Integer newQuantity;
        private Map<String, Object> shippingAddress;
        private String paymentMethodRef;
        
        // Getters and setters
        public UUID getCustomerId() { return customerId; }
        public void setCustomerId(UUID customerId) { this.customerId = customerId; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getCancellationType() { return cancellationType; }
        public void setCancellationType(String cancellationType) { this.cancellationType = cancellationType; }
        
        public UUID getNewPlanId() { return newPlanId; }
        public void setNewPlanId(UUID newPlanId) { this.newPlanId = newPlanId; }
        
        public Integer getNewQuantity() { return newQuantity; }
        public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
        
        public Map<String, Object> getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(Map<String, Object> shippingAddress) { this.shippingAddress = shippingAddress; }
        
        public String getPaymentMethodRef() { return paymentMethodRef; }
        public void setPaymentMethodRef(String paymentMethodRef) { this.paymentMethodRef = paymentMethodRef; }
    }
}
