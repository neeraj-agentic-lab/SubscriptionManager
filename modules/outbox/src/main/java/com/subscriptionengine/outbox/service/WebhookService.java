package com.subscriptionengine.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.WebhookEndpointsDao;
import com.subscriptionengine.generated.tables.pojos.WebhookEndpoints;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing webhook endpoint registrations.
 * 
 * @author Neeraj Yadav
 */
@Service
public class WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    private final WebhookEndpointsDao webhookEndpointsDao;
    private final ObjectMapper objectMapper;
    
    public WebhookService(WebhookEndpointsDao webhookEndpointsDao, ObjectMapper objectMapper) {
        this.webhookEndpointsDao = webhookEndpointsDao;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Register a new webhook endpoint for the current tenant.
     * 
     * @param url the webhook URL
     * @param events array of event types to subscribe to
     * @param description optional description
     * @return the created webhook endpoint
     */
    @Transactional
    public WebhookEndpoints registerWebhook(String url, String[] events, String description) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            logger.info("[WEBHOOK_REGISTER] RequestId: {} - Registering webhook for tenant {} at URL: {}", 
                       requestId, tenantId, url);
            
            // Generate a secure secret for signature verification
            String secret = UUID.randomUUID().toString();
            
            WebhookEndpoints webhook = new WebhookEndpoints();
            webhook.setId(UUID.randomUUID());
            webhook.setTenantId(tenantId);
            webhook.setUrl(url);
            webhook.setSecret(secret);
            webhook.setEvents(events);
            webhook.setStatus("ACTIVE");
            webhook.setDescription(description);
            webhook.setCustomAttrs(JSONB.valueOf("{}"));
            webhook.setCreatedAt(OffsetDateTime.now());
            webhook.setUpdatedAt(OffsetDateTime.now());
            
            webhookEndpointsDao.insert(webhook);
            
            logger.info("[WEBHOOK_REGISTER_SUCCESS] RequestId: {} - Registered webhook {} for tenant {}", 
                       requestId, webhook.getId(), tenantId);
            
            return webhook;
            
        } catch (Exception e) {
            logger.error("[WEBHOOK_REGISTER_ERROR] RequestId: {} - Failed to register webhook", 
                        requestId, e);
            throw new RuntimeException("Failed to register webhook", e);
        }
    }
    
    /**
     * Get all active webhook endpoints for the current tenant.
     * 
     * @return list of active webhook endpoints
     */
    @Transactional(readOnly = true)
    public List<WebhookEndpoints> getActiveWebhooks() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        return webhookEndpointsDao.fetchByTenantId(tenantId).stream()
            .filter(webhook -> "ACTIVE".equals(webhook.getStatus()))
            .toList();
    }
    
    /**
     * Get all webhook endpoints for the current tenant.
     * 
     * @return list of webhook endpoints
     */
    @Transactional(readOnly = true)
    public List<WebhookEndpoints> getAllWebhooks() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        return webhookEndpointsDao.fetchByTenantId(tenantId);
    }
    
    /**
     * Update webhook endpoint status.
     * 
     * @param webhookId the webhook ID
     * @param status the new status (ACTIVE, INACTIVE, DISABLED)
     */
    @Transactional
    public void updateWebhookStatus(UUID webhookId, String status) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            WebhookEndpoints webhook = webhookEndpointsDao.fetchOneById(webhookId);
            if (webhook == null || !webhook.getTenantId().equals(tenantId)) {
                logger.warn("[WEBHOOK_UPDATE_STATUS_ERROR] RequestId: {} - Webhook not found: {}", 
                           requestId, webhookId);
                throw new IllegalArgumentException("Webhook not found");
            }
            
            webhook.setStatus(status);
            webhook.setUpdatedAt(OffsetDateTime.now());
            webhookEndpointsDao.update(webhook);
            
            logger.info("[WEBHOOK_UPDATE_STATUS_SUCCESS] RequestId: {} - Updated webhook {} status to {}", 
                       requestId, webhookId, status);
            
        } catch (Exception e) {
            logger.error("[WEBHOOK_UPDATE_STATUS_ERROR] RequestId: {} - Failed to update webhook status", 
                        requestId, e);
            throw new RuntimeException("Failed to update webhook status", e);
        }
    }
    
    /**
     * Delete a webhook endpoint.
     * 
     * @param webhookId the webhook ID
     */
    @Transactional
    public void deleteWebhook(UUID webhookId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            WebhookEndpoints webhook = webhookEndpointsDao.fetchOneById(webhookId);
            if (webhook == null || !webhook.getTenantId().equals(tenantId)) {
                logger.warn("[WEBHOOK_DELETE_ERROR] RequestId: {} - Webhook not found: {}", 
                           requestId, webhookId);
                throw new IllegalArgumentException("Webhook not found");
            }
            
            webhookEndpointsDao.deleteById(webhookId);
            
            logger.info("[WEBHOOK_DELETE_SUCCESS] RequestId: {} - Deleted webhook {}", 
                       requestId, webhookId);
            
        } catch (Exception e) {
            logger.error("[WEBHOOK_DELETE_ERROR] RequestId: {} - Failed to delete webhook", 
                        requestId, e);
            throw new RuntimeException("Failed to delete webhook", e);
        }
    }
}
