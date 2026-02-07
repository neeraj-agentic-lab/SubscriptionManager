package com.subscriptionengine.subscriptions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request DTO for creating a new subscription.
 * 
 * @author Neeraj Yadav
 */
public class CreateSubscriptionRequest {
    
    @NotNull(message = "Plan ID is required")
    private UUID planId;
    
    // Customer information (for upsert)
    @Email(message = "Valid email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String customerEmail;
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String customerFirstName;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String customerLastName;
    
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String customerPhone;
    
    @Size(max = 255, message = "External customer ID must not exceed 255 characters")
    private String externalCustomerId;
    
    // Subscription configuration
    @Size(max = 255, message = "Payment method reference must not exceed 255 characters")
    private String paymentMethodRef;
    
    private OffsetDateTime trialStart;
    private OffsetDateTime trialEnd;
    
    // Optional scheduling override
    private OffsetDateTime startDate;
    
    // Constructors
    public CreateSubscriptionRequest() {}
    
    public CreateSubscriptionRequest(UUID planId, String customerEmail, String customerFirstName, 
                                   String customerLastName, String paymentMethodRef) {
        this.planId = planId;
        this.customerEmail = customerEmail;
        this.customerFirstName = customerFirstName;
        this.customerLastName = customerLastName;
        this.paymentMethodRef = paymentMethodRef;
    }
    
    // Getters and Setters
    public UUID getPlanId() {
        return planId;
    }
    
    public void setPlanId(UUID planId) {
        this.planId = planId;
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
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
    
    public String getExternalCustomerId() {
        return externalCustomerId;
    }
    
    public void setExternalCustomerId(String externalCustomerId) {
        this.externalCustomerId = externalCustomerId;
    }
    
    public String getPaymentMethodRef() {
        return paymentMethodRef;
    }
    
    public void setPaymentMethodRef(String paymentMethodRef) {
        this.paymentMethodRef = paymentMethodRef;
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
    
    public OffsetDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }
    
    @Override
    public String toString() {
        return "CreateSubscriptionRequest{" +
                "planId=" + planId +
                ", customerEmail='" + customerEmail + '\'' +
                ", customerFirstName='" + customerFirstName + '\'' +
                ", customerLastName='" + customerLastName + '\'' +
                ", paymentMethodRef='" + paymentMethodRef + '\'' +
                ", trialStart=" + trialStart +
                ", trialEnd=" + trialEnd +
                ", startDate=" + startDate +
                '}';
    }
}
