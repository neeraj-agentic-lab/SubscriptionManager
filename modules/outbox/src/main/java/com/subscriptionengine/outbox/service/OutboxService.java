package com.subscriptionengine.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.OutboxEventsDao;
import com.subscriptionengine.generated.tables.pojos.OutboxEvents;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service for emitting events to the outbox for reliable delivery.
 * Events are persisted transactionally with business logic and delivered asynchronously.
 * 
 * @author Neeraj Yadav
 */
@Service
public class OutboxService {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);
    
    private final OutboxEventsDao outboxEventsDao;
    private final ObjectMapper objectMapper;
    
    public OutboxService(OutboxEventsDao outboxEventsDao, ObjectMapper objectMapper) {
        this.outboxEventsDao = outboxEventsDao;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Emit an event to the outbox for async delivery.
     * This method should be called within an existing transaction to ensure atomicity.
     * 
     * @param eventType the type of event (e.g., "subscription.canceled")
     * @param payload the event payload
     * @return the created outbox event
     */
    @Transactional
    public OutboxEvents emitEvent(String eventType, Map<String, Object> payload) {
        return emitEvent(eventType, payload, null);
    }
    
    /**
     * Emit an event to the outbox with an optional idempotency key.
     * 
     * @param eventType the type of event
     * @param payload the event payload
     * @param eventKey optional idempotency key to prevent duplicate events
     * @return the created outbox event
     */
    @Transactional
    public OutboxEvents emitEvent(String eventType, Map<String, Object> payload, String eventKey) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            logger.debug("[OUTBOX_EMIT] RequestId: {} - Emitting event type: {} with key: {}", 
                        requestId, eventType, eventKey);
            
            OutboxEvents event = new OutboxEvents();
            event.setId(UUID.randomUUID());
            event.setTenantId(tenantId);
            event.setEventType(eventType);
            event.setEventKey(eventKey);
            event.setEventPayload(JSONB.valueOf(objectMapper.writeValueAsString(payload)));
            event.setCreatedAt(OffsetDateTime.now());
            event.setCustomAttrs(JSONB.valueOf("{}"));
            
            outboxEventsDao.insert(event);
            
            logger.info("[OUTBOX_EMIT_SUCCESS] RequestId: {} - Emitted event {} (type: {}, key: {})", 
                       requestId, event.getId(), eventType, eventKey);
            
            return event;
            
        } catch (Exception e) {
            logger.error("[OUTBOX_EMIT_ERROR] RequestId: {} - Failed to emit event type: {}", 
                        requestId, eventType, e);
            throw new RuntimeException("Failed to emit outbox event", e);
        }
    }
    
    /**
     * Mark an event as published (delivered to all webhooks).
     * 
     * @param eventId the event ID
     */
    @Transactional
    public void markAsPublished(UUID eventId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        try {
            OutboxEvents event = outboxEventsDao.fetchOneById(eventId);
            if (event == null || !event.getTenantId().equals(tenantId)) {
                logger.warn("[OUTBOX_MARK_PUBLISHED_ERROR] RequestId: {} - Event not found: {}", 
                           requestId, eventId);
                return;
            }
            
            event.setPublishedAt(OffsetDateTime.now());
            outboxEventsDao.update(event);
            
            logger.info("[OUTBOX_MARK_PUBLISHED_SUCCESS] RequestId: {} - Marked event {} as published", 
                       requestId, eventId);
            
        } catch (Exception e) {
            logger.error("[OUTBOX_MARK_PUBLISHED_ERROR] RequestId: {} - Failed to mark event {} as published", 
                        requestId, eventId, e);
            throw new RuntimeException("Failed to mark event as published", e);
        }
    }
}
