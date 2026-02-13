package com.subscriptionengine.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for user-tenant relationship.
 * 
 * @author Neeraj Yadav
 */
public class UserTenantResponse {
    
    private UUID id;
    private UUID userId;
    private UUID tenantId;
    private String role;
    private OffsetDateTime assignedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Denormalized fields for convenience
    private String userEmail;
    private String tenantName;
    
    public UserTenantResponse() {}
    
    public UserTenantResponse(UUID id, UUID userId, UUID tenantId, String role,
                             OffsetDateTime assignedAt, OffsetDateTime createdAt, 
                             OffsetDateTime updatedAt, String userEmail, String tenantName) {
        this.id = id;
        this.userId = userId;
        this.tenantId = tenantId;
        this.role = role;
        this.assignedAt = assignedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userEmail = userEmail;
        this.tenantName = tenantName;
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public UUID getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getTenantName() {
        return tenantName;
    }
    
    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }
}
