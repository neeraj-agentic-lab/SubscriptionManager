package com.subscriptionengine.plans.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for subscription plan data.
 * 
 * @author Neeraj Yadav
 */
public class PlanResponse {
    
    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private String planType;
    private String status;
    private Long basePriceCents;
    private String currency;
    private String billingInterval;
    private Integer billingIntervalCount;
    private Integer trialPeriodDays;
    
    // Validation fields (Phase 1)
    private String planCategory;
    private Boolean requiresProducts;
    private Boolean allowsProducts;
    private Boolean basePriceRequired;
    
    // Audit fields
    private UUID createdBy;
    private UUID updatedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Constructors
    public PlanResponse() {}
    
    public PlanResponse(UUID id, UUID tenantId, String name, String description, String planType,
                       String status, Long basePriceCents, String currency, String billingInterval,
                       Integer billingIntervalCount, Integer trialPeriodDays,
                       OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.planType = planType;
        this.status = status;
        this.basePriceCents = basePriceCents;
        this.currency = currency;
        this.billingInterval = billingInterval;
        this.billingIntervalCount = billingIntervalCount;
        this.trialPeriodDays = trialPeriodDays;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPlanType() {
        return planType;
    }
    
    public void setPlanType(String planType) {
        this.planType = planType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getBasePriceCents() {
        return basePriceCents;
    }
    
    public void setBasePriceCents(Long basePriceCents) {
        this.basePriceCents = basePriceCents;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getBillingInterval() {
        return billingInterval;
    }
    
    public void setBillingInterval(String billingInterval) {
        this.billingInterval = billingInterval;
    }
    
    public Integer getBillingIntervalCount() {
        return billingIntervalCount;
    }
    
    public void setBillingIntervalCount(Integer billingIntervalCount) {
        this.billingIntervalCount = billingIntervalCount;
    }
    
    public Integer getTrialPeriodDays() {
        return trialPeriodDays;
    }
    
    public void setTrialPeriodDays(Integer trialPeriodDays) {
        this.trialPeriodDays = trialPeriodDays;
    }
    
    public String getPlanCategory() {
        return planCategory;
    }
    
    public void setPlanCategory(String planCategory) {
        this.planCategory = planCategory;
    }
    
    public Boolean getRequiresProducts() {
        return requiresProducts;
    }
    
    public void setRequiresProducts(Boolean requiresProducts) {
        this.requiresProducts = requiresProducts;
    }
    
    public Boolean getAllowsProducts() {
        return allowsProducts;
    }
    
    public void setAllowsProducts(Boolean allowsProducts) {
        this.allowsProducts = allowsProducts;
    }
    
    public Boolean getBasePriceRequired() {
        return basePriceRequired;
    }
    
    public void setBasePriceRequired(Boolean basePriceRequired) {
        this.basePriceRequired = basePriceRequired;
    }
    
    public UUID getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
    
    public UUID getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
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
    
    @Override
    public String toString() {
        return "PlanResponse{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", planType='" + planType + '\'' +
                ", status='" + status + '\'' +
                ", basePriceCents=" + basePriceCents +
                ", currency='" + currency + '\'' +
                ", billingInterval='" + billingInterval + '\'' +
                ", billingIntervalCount=" + billingIntervalCount +
                ", trialPeriodDays=" + trialPeriodDays +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
