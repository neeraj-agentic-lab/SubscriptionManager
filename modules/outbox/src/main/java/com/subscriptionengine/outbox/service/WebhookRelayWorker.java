package com.subscriptionengine.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.WebhookDeliveriesDao;
import com.subscriptionengine.generated.tables.pojos.OutboxEvents;
import com.subscriptionengine.generated.tables.pojos.WebhookDeliveries;
import com.subscriptionengine.generated.tables.pojos.WebhookEndpoints;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

import static com.subscriptionengine.generated.tables.OutboxEvents.OUTBOX_EVENTS;
import static com.subscriptionengine.generated.tables.WebhookDeliveries.WEBHOOK_DELIVERIES;
import static com.subscriptionengine.generated.tables.WebhookEndpoints.WEBHOOK_ENDPOINTS;

/**
 * Background worker that polls outbox events and delivers them to registered webhooks.
 * Runs every 5 seconds to process pending events and retry failed deliveries.
 * 
 * @author Neeraj Yadav
 */
@Service
public class WebhookRelayWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookRelayWorker.class);
    
    private final DSLContext dsl;
    private final WebhookDeliveriesDao webhookDeliveriesDao;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final int retryBackoffBaseSeconds;
    
    public WebhookRelayWorker(
            DSLContext dsl,
            WebhookDeliveriesDao webhookDeliveriesDao,
            OutboxService outboxService,
            ObjectMapper objectMapper,
            @Value("${subscription-engine.webhook.retry-backoff-base-seconds:60}") int retryBackoffBaseSeconds) {
        this.dsl = dsl;
        this.webhookDeliveriesDao = webhookDeliveriesDao;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.retryBackoffBaseSeconds = retryBackoffBaseSeconds;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    /**
     * Process unpublished outbox events and create webhook deliveries.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    @Transactional
    public void processOutboxEvents() {
        try {
            // Fetch unpublished events (limit to 100 per batch)
            var unpublishedEvents = dsl.selectFrom(OUTBOX_EVENTS)
                .where(OUTBOX_EVENTS.PUBLISHED_AT.isNull())
                .orderBy(OUTBOX_EVENTS.CREATED_AT.asc())
                .limit(100)
                .fetchInto(OutboxEvents.class);
            
            if (unpublishedEvents.isEmpty()) {
                return;
            }
            
            logger.info("[WEBHOOK_RELAY_PROCESS] Processing {} unpublished outbox events", 
                       unpublishedEvents.size());
            
            for (var event : unpublishedEvents) {
                try {
                    // Set tenant context for this event
                    TenantContext.setTenantId(event.getTenantId());
                    
                    processEvent(event);
                    
                } catch (Exception e) {
                    logger.error("[WEBHOOK_RELAY_PROCESS_ERROR] Failed to process event {}", 
                                event.getId(), e);
                } finally {
                    TenantContext.clear();
                }
            }
            
        } catch (Exception e) {
            logger.error("[WEBHOOK_RELAY_PROCESS_ERROR] Failed to process outbox events", e);
        }
    }
    
    /**
     * Process a single outbox event by creating webhook deliveries for all matching endpoints.
     */
    private void processEvent(OutboxEvents event) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        logger.debug("[WEBHOOK_RELAY_EVENT] RequestId: {} - Processing event {} (type: {})", 
                    requestId, event.getId(), event.getEventType());
        
        // Find all active webhook endpoints for this tenant that subscribe to this event type
        var webhookEndpoints = dsl.selectFrom(WEBHOOK_ENDPOINTS)
            .where(WEBHOOK_ENDPOINTS.TENANT_ID.eq(event.getTenantId()))
            .and(WEBHOOK_ENDPOINTS.STATUS.eq("ACTIVE"))
            .fetchInto(WebhookEndpoints.class);
        
        int deliveriesCreated = 0;
        
        for (var endpoint : webhookEndpoints) {
            
            // Check if this endpoint subscribes to this event type
            String[] subscribedEvents = endpoint.getEvents();
            if (subscribedEvents == null || subscribedEvents.length == 0 || 
                Arrays.asList(subscribedEvents).contains(event.getEventType())) {
                
                // Create webhook delivery
                createWebhookDelivery(event, endpoint);
                deliveriesCreated++;
            }
        }
        
        logger.info("[WEBHOOK_RELAY_EVENT_SUCCESS] RequestId: {} - Created {} deliveries for event {}", 
                   requestId, deliveriesCreated, event.getId());
        
        // Mark event as published if deliveries were created (or no webhooks exist)
        outboxService.markAsPublished(event.getId());
    }
    
    /**
     * Create a webhook delivery record for an event and endpoint.
     */
    private void createWebhookDelivery(OutboxEvents event, WebhookEndpoints endpoint) {
        try {
            WebhookDeliveries delivery = new WebhookDeliveries();
            delivery.setId(UUID.randomUUID());
            delivery.setTenantId(event.getTenantId());
            delivery.setWebhookEndpointId(endpoint.getId());
            delivery.setOutboxEventId(event.getId());
            delivery.setEventType(event.getEventType());
            delivery.setPayload(event.getEventPayload());
            delivery.setStatus("PENDING");
            delivery.setAttemptCount(0);
            delivery.setMaxAttempts(5);
            delivery.setNextAttemptAt(OffsetDateTime.now());
            delivery.setCustomAttrs(JSONB.valueOf("{}"));
            delivery.setCreatedAt(OffsetDateTime.now());
            delivery.setUpdatedAt(OffsetDateTime.now());
            
            webhookDeliveriesDao.insert(delivery);
            
            logger.debug("[WEBHOOK_RELAY_DELIVERY_CREATED] Created delivery {} for event {} to endpoint {}", 
                        delivery.getId(), event.getId(), endpoint.getId());
            
        } catch (Exception e) {
            logger.error("[WEBHOOK_RELAY_DELIVERY_CREATE_ERROR] Failed to create delivery for event {} to endpoint {}", 
                        event.getId(), endpoint.getId(), e);
        }
    }
    
    /**
     * Deliver pending webhook deliveries.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 15000)
    @Transactional
    public void deliverWebhooks() {
        try {
            // Fetch pending deliveries that are ready for (re)attempt
            var pendingDeliveries = dsl.selectFrom(WEBHOOK_DELIVERIES)
                .where(WEBHOOK_DELIVERIES.STATUS.eq("PENDING"))
                .and(WEBHOOK_DELIVERIES.NEXT_ATTEMPT_AT.le(OffsetDateTime.now()))
                .and(WEBHOOK_DELIVERIES.ATTEMPT_COUNT.lt(WEBHOOK_DELIVERIES.MAX_ATTEMPTS))
                .orderBy(WEBHOOK_DELIVERIES.NEXT_ATTEMPT_AT.asc())
                .limit(50)
                .fetchInto(WebhookDeliveries.class);
            
            if (pendingDeliveries.isEmpty()) {
                return;
            }
            
            logger.info("[WEBHOOK_DELIVER] Delivering {} pending webhooks", pendingDeliveries.size());
            
            for (var delivery : pendingDeliveries) {
                try {
                    // Set tenant context
                    TenantContext.setTenantId(delivery.getTenantId());
                    
                    deliverWebhook(delivery);
                    
                } catch (Exception e) {
                    logger.error("[WEBHOOK_DELIVER_ERROR] Failed to deliver webhook {}", 
                                delivery.getId(), e);
                } finally {
                    TenantContext.clear();
                }
            }
            
        } catch (Exception e) {
            logger.error("[WEBHOOK_DELIVER_ERROR] Failed to deliver webhooks", e);
        }
    }
    
    /**
     * Deliver a single webhook to its endpoint.
     */
    private void deliverWebhook(WebhookDeliveries delivery) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            // Fetch webhook endpoint
            var endpointRecord = dsl.selectFrom(WEBHOOK_ENDPOINTS)
                .where(WEBHOOK_ENDPOINTS.ID.eq(delivery.getWebhookEndpointId()))
                .fetchOne();
            
            if (endpointRecord == null) {
                logger.warn("[WEBHOOK_DELIVER_SKIP] RequestId: {} - Endpoint not found for delivery {}", 
                           requestId, delivery.getId());
                markDeliveryAsFailed(delivery, "Webhook endpoint not found");
                return;
            }
            
            WebhookEndpoints endpoint = endpointRecord.into(WebhookEndpoints.class);
            
            if (!"ACTIVE".equals(endpoint.getStatus())) {
                logger.warn("[WEBHOOK_DELIVER_SKIP] RequestId: {} - Endpoint {} is not active", 
                           requestId, endpoint.getId());
                markDeliveryAsFailed(delivery, "Webhook endpoint is not active");
                return;
            }
            
            // Prepare webhook payload
            Map<String, Object> webhookPayload = new HashMap<>();
            webhookPayload.put("eventId", delivery.getId().toString());
            webhookPayload.put("eventType", delivery.getEventType());
            webhookPayload.put("timestamp", delivery.getCreatedAt().toString());
            webhookPayload.put("data", objectMapper.readValue(delivery.getPayload().data(), Map.class));
            
            String payloadJson = objectMapper.writeValueAsString(webhookPayload);
            
            // Generate signature
            String signature = generateSignature(payloadJson, endpoint.getSecret());
            
            // Send HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint.getUrl()))
                .header("Content-Type", "application/json")
                .header("X-Webhook-Signature", signature)
                .header("X-Event-Type", delivery.getEventType())
                .header("X-Event-Id", delivery.getId().toString())
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                .build();
            
            logger.debug("[WEBHOOK_DELIVER_SEND] RequestId: {} - Sending webhook {} to {}", 
                        requestId, delivery.getId(), endpoint.getUrl());
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // Update delivery based on response
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            delivery.setLastResponseStatus(response.statusCode());
            delivery.setLastResponseBody(response.body().substring(0, Math.min(response.body().length(), 1000)));
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Success
                delivery.setStatus("DELIVERED");
                delivery.setDeliveredAt(OffsetDateTime.now());
                
                logger.info("[WEBHOOK_DELIVER_SUCCESS] RequestId: {} - Delivered webhook {} to {} (status: {})", 
                           requestId, delivery.getId(), endpoint.getUrl(), response.statusCode());
            } else {
                // Failed but will retry
                delivery.setLastError("HTTP " + response.statusCode() + ": " + response.body().substring(0, Math.min(response.body().length(), 200)));
                
                if (delivery.getAttemptCount() >= delivery.getMaxAttempts()) {
                    delivery.setStatus("FAILED");
                    logger.warn("[WEBHOOK_DELIVER_FAILED] RequestId: {} - Webhook {} failed after {} attempts", 
                               requestId, delivery.getId(), delivery.getAttemptCount());
                } else {
                    // Schedule retry with exponential backoff
                    int backoffSeconds = (int) Math.pow(2, delivery.getAttemptCount()) * retryBackoffBaseSeconds;
                    delivery.setNextAttemptAt(OffsetDateTime.now().plusSeconds(backoffSeconds));
                    
                    logger.warn("[WEBHOOK_DELIVER_RETRY] RequestId: {} - Webhook {} failed (status: {}), will retry in {} seconds", 
                               requestId, delivery.getId(), response.statusCode(), backoffSeconds);
                }
            }
            
            delivery.setUpdatedAt(OffsetDateTime.now());
            webhookDeliveriesDao.update(delivery);
            
        } catch (Exception e) {
            logger.error("[WEBHOOK_DELIVER_ERROR] RequestId: {} - Failed to deliver webhook {}", 
                        requestId, delivery.getId(), e);
            
            delivery.setAttemptCount(delivery.getAttemptCount() + 1);
            delivery.setLastError(e.getMessage());
            
            if (delivery.getAttemptCount() >= delivery.getMaxAttempts()) {
                delivery.setStatus("FAILED");
            } else {
                // Schedule retry with exponential backoff
                int backoffSeconds = (int) Math.pow(2, delivery.getAttemptCount()) * retryBackoffBaseSeconds;
                delivery.setNextAttemptAt(OffsetDateTime.now().plusSeconds(backoffSeconds));
            }
            
            delivery.setUpdatedAt(OffsetDateTime.now());
            webhookDeliveriesDao.update(delivery);
        }
    }
    
    /**
     * Mark a delivery as failed.
     */
    private void markDeliveryAsFailed(WebhookDeliveries delivery, String reason) {
        delivery.setStatus("FAILED");
        delivery.setLastError(reason);
        delivery.setUpdatedAt(OffsetDateTime.now());
        webhookDeliveriesDao.update(delivery);
    }
    
    /**
     * Generate HMAC-SHA256 signature for webhook payload.
     */
    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate webhook signature", e);
        }
    }
    
    /**
     * Convert byte array to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
