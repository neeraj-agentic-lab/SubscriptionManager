package com.subscriptionengine.subscriptions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Simplified request for ecommerce product subscriptions.
 * Uses direct products instead of confusing "product plans".
 * 
 * @author Neeraj Yadav
 */
public class CreateEcommerceSubscriptionRequest {
    
    // Base plan defines billing frequency (monthly, weekly, etc.)
    @NotNull(message = "Base plan ID is required")
    private UUID basePlanId;
    
    // Direct products - no confusing "product plans" needed
    @NotEmpty(message = "At least one product is required")
    @Valid
    private List<ProductItem> products;
    
    // Customer information
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
    
    // Payment and subscription config
    @Size(max = 255, message = "Payment method reference must not exceed 255 characters")
    private String paymentMethodRef;
    
    private OffsetDateTime trialStart;
    private OffsetDateTime trialEnd;
    private OffsetDateTime startDate;
    
    // Shipping for physical products
    private ShippingAddress shippingAddress;
    
    // Constructors
    public CreateEcommerceSubscriptionRequest() {}
    
    public CreateEcommerceSubscriptionRequest(UUID basePlanId, List<ProductItem> products, 
                                            String customerEmail, String customerFirstName, 
                                            String customerLastName, String paymentMethodRef) {
        this.basePlanId = basePlanId;
        this.products = products;
        this.customerEmail = customerEmail;
        this.customerFirstName = customerFirstName;
        this.customerLastName = customerLastName;
        this.paymentMethodRef = paymentMethodRef;
    }
    
    // Getters and setters
    public UUID getBasePlanId() {
        return basePlanId;
    }
    
    public void setBasePlanId(UUID basePlanId) {
        this.basePlanId = basePlanId;
    }
    
    public List<ProductItem> getProducts() {
        return products;
    }
    
    public void setProducts(List<ProductItem> products) {
        this.products = products;
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
    
    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }
    
    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    /**
     * Calculate total subscription amount from all products.
     */
    public Long getTotalAmountCents() {
        return products.stream()
                .mapToLong(ProductItem::getTotalPriceCents)
                .sum();
    }
    
    @Override
    public String toString() {
        return "CreateEcommerceSubscriptionRequest{" +
                "basePlanId=" + basePlanId +
                ", products=" + products +
                ", customerEmail='" + customerEmail + '\'' +
                ", totalAmountCents=" + getTotalAmountCents() +
                '}';
    }
    
    /**
     * Shipping address for physical product deliveries.
     */
    public static class ShippingAddress {
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        
        public ShippingAddress() {}
        
        public ShippingAddress(String addressLine1, String city, String state, 
                             String postalCode, String country) {
            this.addressLine1 = addressLine1;
            this.city = city;
            this.state = state;
            this.postalCode = postalCode;
            this.country = country;
        }
        
        // Getters and setters
        public String getAddressLine1() {
            return addressLine1;
        }
        
        public void setAddressLine1(String addressLine1) {
            this.addressLine1 = addressLine1;
        }
        
        public String getAddressLine2() {
            return addressLine2;
        }
        
        public void setAddressLine2(String addressLine2) {
            this.addressLine2 = addressLine2;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getState() {
            return state;
        }
        
        public void setState(String state) {
            this.state = state;
        }
        
        public String getPostalCode() {
            return postalCode;
        }
        
        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
        
        public String getCountry() {
            return country;
        }
        
        public void setCountry(String country) {
            this.country = country;
        }
        
        @Override
        public String toString() {
            return "ShippingAddress{" +
                    "addressLine1='" + addressLine1 + '\'' +
                    ", city='" + city + '\'' +
                    ", state='" + state + '\'' +
                    ", postalCode='" + postalCode + '\'' +
                    ", country='" + country + '\'' +
                    '}';
        }
    }
}
