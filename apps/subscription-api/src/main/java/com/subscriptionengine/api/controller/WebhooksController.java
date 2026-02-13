package com.subscriptionengine.api.controller;

import com.subscriptionengine.auth.TenantSecured;
import com.subscriptionengine.generated.tables.pojos.WebhookEndpoints;
import com.subscriptionengine.outbox.service.WebhookService;
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

import java.util.*;

/**
 * REST API for managing webhook endpoints.
 * Allows tenants to register webhooks to receive events.
 * 
 * @author Neeraj Yadav
 */
@RestController
@RequestMapping("/v1/admin/webhooks")
@TenantSecured
@Tag(name = "Admin - Webhooks", description = "Admin endpoints for webhook management. Register and manage webhook endpoints to receive real-time event notifications for subscription lifecycle events.")
public class WebhooksController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhooksController.class);
    
    private final WebhookService webhookService;
    
    public WebhooksController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }
    
    /**
     * Register a new webhook endpoint.
     * 
     * POST /v1/webhooks
     */
    @PostMapping
    @Operation(
        summary = "Register a new webhook endpoint",
        description = "Registers a new webhook endpoint to receive event notifications. "
            + "Webhooks are called when subscription events occur (created, updated, canceled, etc.). "
            + "Returns a secret key for verifying webhook signatures. "
            + "Supports filtering by event types."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook registered successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors in webhook data"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> registerWebhook(
        @Parameter(description = "Webhook registration details including URL and event filters", required = true)
        @Valid @RequestBody RegisterWebhookRequest request) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        logger.info("[WEBHOOK_REGISTER_API] RequestId: {} - Registering webhook at URL: {}", 
                   requestId, request.url);
        
        WebhookEndpoints webhook = webhookService.registerWebhook(
            request.url,
            request.events != null ? request.events : new String[0],
            request.description
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("timestamp", System.currentTimeMillis());
        response.put("success", true);
        response.put("message", "Webhook registered successfully");
        
        Map<String, Object> webhookData = new HashMap<>();
        webhookData.put("webhookId", webhook.getId().toString());
        webhookData.put("url", webhook.getUrl());
        webhookData.put("secret", webhook.getSecret());
        webhookData.put("events", webhook.getEvents());
        webhookData.put("status", webhook.getStatus());
        webhookData.put("description", webhook.getDescription());
        webhookData.put("createdAt", webhook.getCreatedAt().toString());
        
        response.put("data", webhookData);
        
        logger.info("[WEBHOOK_REGISTER_API_SUCCESS] RequestId: {} - Registered webhook {}", 
                   requestId, webhook.getId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * List all webhook endpoints for the current tenant.
     * 
     * GET /v1/webhooks
     */
    @GetMapping
    @Operation(
        summary = "List all webhook endpoints",
        description = "Retrieves all registered webhook endpoints for the current tenant including their status, registered events, and creation dates."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhooks retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> listWebhooks() {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        logger.info("[WEBHOOK_LIST_API] RequestId: {} - Listing webhooks", requestId);
        
        List<WebhookEndpoints> webhooks = webhookService.getAllWebhooks();
        
        List<Map<String, Object>> webhookList = webhooks.stream()
            .map(webhook -> {
                Map<String, Object> webhookData = new HashMap<>();
                webhookData.put("webhookId", webhook.getId().toString());
                webhookData.put("url", webhook.getUrl());
                webhookData.put("events", webhook.getEvents());
                webhookData.put("status", webhook.getStatus());
                webhookData.put("description", webhook.getDescription());
                webhookData.put("createdAt", webhook.getCreatedAt().toString());
                webhookData.put("updatedAt", webhook.getUpdatedAt().toString());
                return webhookData;
            })
            .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("timestamp", System.currentTimeMillis());
        response.put("success", true);
        response.put("data", Map.of("webhooks", webhookList, "count", webhookList.size()));
        
        logger.info("[WEBHOOK_LIST_API_SUCCESS] RequestId: {} - Listed {} webhooks", 
                   requestId, webhookList.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Update webhook status.
     * 
     * PATCH /v1/webhooks/{webhookId}
     */
    @PatchMapping("/{webhookId}")
    @Operation(
        summary = "Update webhook status",
        description = "Activates or deactivates a webhook endpoint. Inactive webhooks will not receive event notifications."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Webhook status updated successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Webhook not found"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - missing or invalid authentication token"
        )
    })
    public ResponseEntity<Map<String, Object>> updateWebhookStatus(
            @PathVariable UUID webhookId,
            @Valid @RequestBody UpdateWebhookStatusRequest request) {
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        logger.info("[WEBHOOK_UPDATE_API] RequestId: {} - Updating webhook {} status to {}", 
                   requestId, webhookId, request.status);
        
        webhookService.updateWebhookStatus(webhookId, request.status);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("timestamp", System.currentTimeMillis());
        response.put("success", true);
        response.put("message", "Webhook status updated successfully");
        response.put("data", Map.of("webhookId", webhookId.toString(), "status", request.status));
        
        logger.info("[WEBHOOK_UPDATE_API_SUCCESS] RequestId: {} - Updated webhook {} status", 
                   requestId, webhookId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a webhook endpoint.
     * 
     * DELETE /v1/webhooks/{webhookId}
     */
    @DeleteMapping("/{webhookId}")
    public ResponseEntity<Map<String, Object>> deleteWebhook(@PathVariable UUID webhookId) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        logger.info("[WEBHOOK_DELETE_API] RequestId: {} - Deleting webhook {}", requestId, webhookId);
        
        webhookService.deleteWebhook(webhookId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requestId", requestId);
        response.put("timestamp", System.currentTimeMillis());
        response.put("success", true);
        response.put("message", "Webhook deleted successfully");
        response.put("data", Map.of("webhookId", webhookId.toString()));
        
        logger.info("[WEBHOOK_DELETE_API_SUCCESS] RequestId: {} - Deleted webhook {}", 
                   requestId, webhookId);
        
        return ResponseEntity.ok(response);
    }
    
    // DTOs
    
    public static class RegisterWebhookRequest {
        @NotBlank(message = "URL is required")
        public String url;
        
        public String[] events;
        
        public String description;
    }
    
    public static class UpdateWebhookStatusRequest {
        @NotNull(message = "Status is required")
        public String status;
    }
}
