package com.subscriptionengine.subscriptions.service;

import com.subscriptionengine.generated.tables.pojos.SubscriptionHistory;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.SubscriptionHistory.SUBSCRIPTION_HISTORY;

/**
 * Service for tracking subscription history and audit trail.
 * Records all changes to subscriptions for compliance and debugging.
 * 
 * @author Neeraj Yadav
 */
@Service
public class SubscriptionHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionHistoryService.class);
    
    private final DSLContext dsl;
    
    public SubscriptionHistoryService(DSLContext dsl) {
        this.dsl = dsl;
    }
    
    /**
     * Record a subscription action in the history.
     */
    public void recordAction(
            UUID tenantId,
            UUID subscriptionId,
            String action,
            UUID performedBy,
            String performedByType,
            Map<String, Object> metadata,
            String notes) {
        
        try {
            dsl.insertInto(SUBSCRIPTION_HISTORY)
                    .set(SUBSCRIPTION_HISTORY.ID, UUID.randomUUID())
                    .set(SUBSCRIPTION_HISTORY.TENANT_ID, tenantId)
                    .set(SUBSCRIPTION_HISTORY.SUBSCRIPTION_ID, subscriptionId)
                    .set(SUBSCRIPTION_HISTORY.ACTION, action)
                    .set(SUBSCRIPTION_HISTORY.PERFORMED_BY, performedBy)
                    .set(SUBSCRIPTION_HISTORY.PERFORMED_BY_TYPE, performedByType)
                    .set(SUBSCRIPTION_HISTORY.PERFORMED_AT, LocalDateTime.now())
                    .set(SUBSCRIPTION_HISTORY.METADATA, metadata != null ? JSONB.valueOf(toJson(metadata)) : null)
                    .set(SUBSCRIPTION_HISTORY.NOTES, notes)
                    .set(SUBSCRIPTION_HISTORY.CREATED_AT, LocalDateTime.now())
                    .execute();
            
            logger.info("Recorded subscription action: {} for subscription: {} by user: {}", 
                    action, subscriptionId, performedBy);
            
        } catch (Exception e) {
            logger.error("Error recording subscription history for subscription: {}", subscriptionId, e);
            // Don't throw - history recording should not fail the main operation
        }
    }
    
    /**
     * Record subscription creation.
     */
    public void recordCreation(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType) {
        recordAction(tenantId, subscriptionId, "CREATED", performedBy, performedByType, null, "Subscription created");
    }
    
    /**
     * Record subscription pause.
     */
    public void recordPause(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType, String reason) {
        Map<String, Object> metadata = Map.of("reason", reason);
        recordAction(tenantId, subscriptionId, "PAUSED", performedBy, performedByType, metadata, "Subscription paused");
    }
    
    /**
     * Record subscription resume.
     */
    public void recordResume(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType) {
        recordAction(tenantId, subscriptionId, "RESUMED", performedBy, performedByType, null, "Subscription resumed");
    }
    
    /**
     * Record subscription cancellation.
     */
    public void recordCancellation(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType, String reason) {
        Map<String, Object> metadata = Map.of("reason", reason);
        recordAction(tenantId, subscriptionId, "CANCELED", performedBy, performedByType, metadata, "Subscription canceled");
    }
    
    /**
     * Record plan change.
     */
    public void recordPlanChange(
            UUID tenantId, 
            UUID subscriptionId, 
            UUID performedBy, 
            String performedByType,
            UUID oldPlanId,
            UUID newPlanId,
            String oldPlanName,
            String newPlanName) {
        
        Map<String, Object> metadata = Map.of(
                "oldPlanId", oldPlanId.toString(),
                "newPlanId", newPlanId.toString(),
                "oldPlanName", oldPlanName,
                "newPlanName", newPlanName
        );
        recordAction(tenantId, subscriptionId, "PLAN_CHANGED", performedBy, performedByType, metadata, 
                "Plan changed from " + oldPlanName + " to " + newPlanName);
    }
    
    /**
     * Record payment method update.
     */
    public void recordPaymentUpdate(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType) {
        recordAction(tenantId, subscriptionId, "PAYMENT_UPDATED", performedBy, performedByType, null, "Payment method updated");
    }
    
    /**
     * Record products update.
     */
    public void recordProductsUpdate(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType, int productCount) {
        Map<String, Object> metadata = Map.of("productCount", productCount);
        recordAction(tenantId, subscriptionId, "PRODUCTS_UPDATED", performedBy, performedByType, metadata, 
                "Subscription products updated (" + productCount + " items)");
    }
    
    /**
     * Record shipping address update.
     */
    public void recordShippingUpdate(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType) {
        recordAction(tenantId, subscriptionId, "SHIPPING_UPDATED", performedBy, performedByType, null, "Shipping address updated");
    }
    
    /**
     * Record metadata update.
     */
    public void recordMetadataUpdate(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType) {
        recordAction(tenantId, subscriptionId, "METADATA_UPDATED", performedBy, performedByType, null, "Metadata updated");
    }
    
    /**
     * Record subscription archival.
     */
    public void recordArchival(UUID tenantId, UUID subscriptionId, UUID performedBy, String performedByType, String reason) {
        Map<String, Object> metadata = Map.of("reason", reason);
        recordAction(tenantId, subscriptionId, "ARCHIVED", performedBy, performedByType, metadata, "Subscription archived");
    }
    
    /**
     * Get subscription history.
     */
    public List<SubscriptionHistory> getSubscriptionHistory(UUID subscriptionId) {
        return dsl.selectFrom(SUBSCRIPTION_HISTORY)
                .where(SUBSCRIPTION_HISTORY.SUBSCRIPTION_ID.eq(subscriptionId))
                .orderBy(SUBSCRIPTION_HISTORY.PERFORMED_AT.desc())
                .fetchInto(SubscriptionHistory.class);
    }
    
    /**
     * Get subscription history with pagination.
     */
    public List<SubscriptionHistory> getSubscriptionHistory(UUID subscriptionId, int page, int size) {
        return dsl.selectFrom(SUBSCRIPTION_HISTORY)
                .where(SUBSCRIPTION_HISTORY.SUBSCRIPTION_ID.eq(subscriptionId))
                .orderBy(SUBSCRIPTION_HISTORY.PERFORMED_AT.desc())
                .limit(size)
                .offset(page * size)
                .fetchInto(SubscriptionHistory.class);
    }
    
    /**
     * Get history count for a subscription.
     */
    public long getHistoryCount(UUID subscriptionId) {
        return dsl.selectCount()
                .from(SUBSCRIPTION_HISTORY)
                .where(SUBSCRIPTION_HISTORY.SUBSCRIPTION_ID.eq(subscriptionId))
                .fetchOne(0, Long.class);
    }
    
    /**
     * Convert map to JSON string.
     */
    private String toJson(Map<String, Object> map) {
        try {
            // Simple JSON serialization - in production use Jackson or Gson
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            logger.error("Error converting map to JSON", e);
            return "{}";
        }
    }
}
