package com.subscriptionengine.subscriptions.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for subscription history entries.
 * 
 * @author Neeraj Yadav
 */
public class SubscriptionHistoryResponse {
    
    private UUID id;
    private UUID tenantId;
    private UUID subscriptionId;
    private String action;
    private UUID performedBy;
    private String performedByType;
    private OffsetDateTime performedAt;
    private Map<String, Object> metadata;
    private String notes;
    private OffsetDateTime createdAt;
    
    // Constructors
    public SubscriptionHistoryResponse() {}
    
    public SubscriptionHistoryResponse(UUID id, UUID tenantId, UUID subscriptionId, String action,
                                      UUID performedBy, String performedByType, OffsetDateTime performedAt,
                                      Map<String, Object> metadata, String notes, OffsetDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.subscriptionId = subscriptionId;
        this.action = action;
        this.performedBy = performedBy;
        this.performedByType = performedByType;
        this.performedAt = performedAt;
        this.metadata = metadata;
        this.notes = notes;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public UUID getSubscriptionId() {
        return subscriptionId;
    }
    
    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public UUID getPerformedBy() {
        return performedBy;
    }
    
    public void setPerformedBy(UUID performedBy) {
        this.performedBy = performedBy;
    }
    
    public String getPerformedByType() {
        return performedByType;
    }
    
    public void setPerformedByType(String performedByType) {
        this.performedByType = performedByType;
    }
    
    public OffsetDateTime getPerformedAt() {
        return performedAt;
    }
    
    public void setPerformedAt(OffsetDateTime performedAt) {
        this.performedAt = performedAt;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "SubscriptionHistoryResponse{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", subscriptionId=" + subscriptionId +
                ", action='" + action + '\'' +
                ", performedBy=" + performedBy +
                ", performedByType='" + performedByType + '\'' +
                ", performedAt=" + performedAt +
                ", metadata=" + metadata +
                ", notes='" + notes + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
