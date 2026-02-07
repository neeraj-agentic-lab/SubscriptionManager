package com.subscriptionengine.plans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new subscription plan.
 * 
 * @author Neeraj Yadav
 */
public class CreatePlanRequest {
    
    @NotBlank(message = "Plan name is required")
    @Size(max = 255, message = "Plan name must not exceed 255 characters")
    private String name;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    @NotNull(message = "Base price in cents is required")
    @Positive(message = "Base price must be positive")
    private Long basePriceCents;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-character ISO code")
    private String currency;
    
    @NotBlank(message = "Billing interval is required")
    @Size(max = 20, message = "Billing interval must not exceed 20 characters")
    private String billingInterval;
    
    @Positive(message = "Billing interval count must be positive")
    private Integer billingIntervalCount = 1;
    
    @Size(max = 20, message = "Plan type must not exceed 20 characters")
    private String planType;
    
    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status = "ACTIVE";
    
    private Integer trialPeriodDays;
    
    // Constructors
    public CreatePlanRequest() {}
    
    public CreatePlanRequest(String name, String description, Long basePriceCents, String currency, 
                           String billingInterval, String planType, String status) {
        this.name = name;
        this.description = description;
        this.basePriceCents = basePriceCents;
        this.currency = currency;
        this.billingInterval = billingInterval;
        this.planType = planType;
        this.status = status;
    }
    
    // Getters and Setters
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
    
    public Integer getTrialPeriodDays() {
        return trialPeriodDays;
    }
    
    public void setTrialPeriodDays(Integer trialPeriodDays) {
        this.trialPeriodDays = trialPeriodDays;
    }
    
    @Override
    public String toString() {
        return "CreatePlanRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", basePriceCents=" + basePriceCents +
                ", currency='" + currency + '\'' +
                ", billingInterval='" + billingInterval + '\'' +
                ", planType='" + planType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
