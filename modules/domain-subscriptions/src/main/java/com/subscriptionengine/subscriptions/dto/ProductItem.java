package com.subscriptionengine.subscriptions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Product item with customer-chosen delivery plan.
 * Customer selects both the product and their preferred delivery schedule (plan).
 * 
 * @author Neeraj Yadav
 */
public class ProductItem {
    
    @NotBlank(message = "Product ID is required")
    @Size(max = 255, message = "Product ID must not exceed 255 characters")
    private String productId;  // Your ecommerce product SKU/ID
    
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String productName;
    
    @Positive(message = "Quantity must be positive")
    private Integer quantity = 1;
    
    @NotNull(message = "Unit price is required")
    @Min(value = 0, message = "Unit price must not be negative")
    private Long unitPriceCents;  // Price per unit in cents
    
    @NotBlank(message = "Currency is required")
    @Size(max = 3, message = "Currency must be 3 characters")
    private String currency = "USD";
    
    // Customer-chosen plan for this product (delivery schedule, billing frequency, etc.)
    @NotNull(message = "Plan ID is required")
    private UUID planId;  // References plan with schedule (monthly, weekly, etc.) - works for physical & digital
    
    // Optional product metadata
    private String description;
    private String imageUrl;
    private String category;
    
    // Constructors
    public ProductItem() {}
    
    public ProductItem(String productId, String productName, Integer quantity, 
                      Long unitPriceCents, UUID planId) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.planId = planId;
    }
    
    public ProductItem(String productId, String productName, Integer quantity, 
                      Long unitPriceCents, String currency, UUID planId) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.currency = currency;
        this.planId = planId;
    }
    
    // Getters and setters
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Long getUnitPriceCents() {
        return unitPriceCents;
    }
    
    public void setUnitPriceCents(Long unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public UUID getPlanId() {
        return planId;
    }
    
    public void setPlanId(UUID planId) {
        this.planId = planId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    /**
     * Calculate total price for this item (quantity × unit price).
     */
    public Long getTotalPriceCents() {
        return quantity * unitPriceCents;
    }
    
    @Override
    public String toString() {
        return "ProductItem{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", unitPriceCents=" + unitPriceCents +
                ", currency='" + currency + '\'' +
                ", totalPriceCents=" + getTotalPriceCents() +
                '}';
    }
}
