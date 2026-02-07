package com.subscriptionengine.subscriptions.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for subscription data.
 * 
 * @author Neeraj Yadav
 */
public class SubscriptionResponse {
    
    private UUID id;
    private UUID tenantId;
    private UUID customerId;
    private UUID planId;
    private String status;
    private OffsetDateTime currentPeriodStart;
    private OffsetDateTime currentPeriodEnd;
    private OffsetDateTime nextRenewalAt;
    private String paymentMethodRef;
    private Boolean cancelAtPeriodEnd;
    private OffsetDateTime canceledAt;
    private String cancellationReason;
    private OffsetDateTime trialStart;
    private OffsetDateTime trialEnd;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // Customer details (denormalized for convenience)
    private String customerEmail;
    private String customerFirstName;
    private String customerLastName;
    
    // Plan details (denormalized for convenience)
    private String planName;
    private Long planBasePriceCents;
    private String planCurrency;
    private String planBillingInterval;
    
    // Constructors
    public SubscriptionResponse() {}
    
    public SubscriptionResponse(UUID id, UUID tenantId, UUID customerId, UUID planId, String status,
                               OffsetDateTime currentPeriodStart, OffsetDateTime currentPeriodEnd,
                               OffsetDateTime nextRenewalAt, String paymentMethodRef,
                               Boolean cancelAtPeriodEnd, OffsetDateTime trialStart, OffsetDateTime trialEnd,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.planId = planId;
        this.status = status;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.nextRenewalAt = nextRenewalAt;
        this.paymentMethodRef = paymentMethodRef;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.trialStart = trialStart;
        this.trialEnd = trialEnd;
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
    
    public UUID getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }
    
    public UUID getPlanId() {
        return planId;
    }
    
    public void setPlanId(UUID planId) {
        this.planId = planId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public OffsetDateTime getCurrentPeriodStart() {
        return currentPeriodStart;
    }
    
    public void setCurrentPeriodStart(OffsetDateTime currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }
    
    public OffsetDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }
    
    public void setCurrentPeriodEnd(OffsetDateTime currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }
    
    public OffsetDateTime getNextRenewalAt() {
        return nextRenewalAt;
    }
    
    public void setNextRenewalAt(OffsetDateTime nextRenewalAt) {
        this.nextRenewalAt = nextRenewalAt;
    }
    
    public String getPaymentMethodRef() {
        return paymentMethodRef;
    }
    
    public void setPaymentMethodRef(String paymentMethodRef) {
        this.paymentMethodRef = paymentMethodRef;
    }
    
    public Boolean getCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }
    
    public void setCancelAtPeriodEnd(Boolean cancelAtPeriodEnd) {
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }
    
    public OffsetDateTime getCanceledAt() {
        return canceledAt;
    }
    
    public void setCanceledAt(OffsetDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    
    public OffsetDateTime getTrialStart() {
        return trialStart;
    }
    
    public void setTrialStart(OffsetDateTime trialStart) {
        this.trialStart = trialStart;
    }
    
    public OffsetDateTime getTrialEnd() {
        return trialEnd;
    }
    
    public void setTrialEnd(OffsetDateTime trialEnd) {
        this.trialEnd = trialEnd;
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
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public String getCustomerFirstName() {
        return customerFirstName;
    }
    
    public void setCustomerFirstName(String customerFirstName) {
        this.customerFirstName = customerFirstName;
    }
    
    public String getCustomerLastName() {
        return customerLastName;
    }
    
    public void setCustomerLastName(String customerLastName) {
        this.customerLastName = customerLastName;
    }
    
    public String getPlanName() {
        return planName;
    }
    
    public void setPlanName(String planName) {
        this.planName = planName;
    }
    
    public Long getPlanBasePriceCents() {
        return planBasePriceCents;
    }
    
    public void setPlanBasePriceCents(Long planBasePriceCents) {
        this.planBasePriceCents = planBasePriceCents;
    }
    
    public String getPlanCurrency() {
        return planCurrency;
    }
    
    public void setPlanCurrency(String planCurrency) {
        this.planCurrency = planCurrency;
    }
    
    public String getPlanBillingInterval() {
        return planBillingInterval;
    }
    
    public void setPlanBillingInterval(String planBillingInterval) {
        this.planBillingInterval = planBillingInterval;
    }
    
    @Override
    public String toString() {
        return "SubscriptionResponse{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", customerId=" + customerId +
                ", planId=" + planId +
                ", status='" + status + '\'' +
                ", currentPeriodStart=" + currentPeriodStart +
                ", currentPeriodEnd=" + currentPeriodEnd +
                ", nextRenewalAt=" + nextRenewalAt +
                ", paymentMethodRef='" + paymentMethodRef + '\'' +
                ", cancelAtPeriodEnd=" + cancelAtPeriodEnd +
                ", trialStart=" + trialStart +
                ", trialEnd=" + trialEnd +
                ", customerEmail='" + customerEmail + '\'' +
                ", planName='" + planName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
