package com.subscriptionengine.plans.service;

import com.subscriptionengine.generated.tables.pojos.Plans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating plan configurations and subscription requests against plan rules.
 * Enforces plan category constraints (DIGITAL, PRODUCT_BASED, HYBRID).
 * 
 * @author Neeraj Yadav
 */
@Service
public class PlanValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlanValidationService.class);
    
    /**
     * Validate plan configuration is consistent with its category.
     */
    public ValidationResult validatePlanConfiguration(Plans plan) {
        List<String> errors = new ArrayList<>();
        
        if (plan.getPlanCategory() == null) {
            errors.add("Plan category is required");
            return ValidationResult.failure(errors);
        }
        
        switch (plan.getPlanCategory()) {
            case "DIGITAL":
                validateDigitalPlan(plan, errors);
                break;
            case "PRODUCT_BASED":
                validateProductBasedPlan(plan, errors);
                break;
            case "HYBRID":
                validateHybridPlan(plan, errors);
                break;
            default:
                errors.add("Invalid plan category: " + plan.getPlanCategory());
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
    
    /**
     * Validate DIGITAL plan: no products, base price required.
     */
    private void validateDigitalPlan(Plans plan, List<String> errors) {
        if (plan.getRequiresProducts() != null && plan.getRequiresProducts()) {
            errors.add("DIGITAL plans cannot require products");
        }
        if (plan.getAllowsProducts() != null && plan.getAllowsProducts()) {
            errors.add("DIGITAL plans cannot allow products");
        }
        if (plan.getBasePriceRequired() != null && !plan.getBasePriceRequired()) {
            errors.add("DIGITAL plans must require base price");
        }
        if (plan.getBasePriceCents() == null || plan.getBasePriceCents() <= 0) {
            errors.add("DIGITAL plans must have base price > 0");
        }
    }
    
    /**
     * Validate PRODUCT_BASED plan: products required, base price optional.
     */
    private void validateProductBasedPlan(Plans plan, List<String> errors) {
        if (plan.getRequiresProducts() == null || !plan.getRequiresProducts()) {
            errors.add("PRODUCT_BASED plans must require products");
        }
        if (plan.getAllowsProducts() == null || !plan.getAllowsProducts()) {
            errors.add("PRODUCT_BASED plans must allow products");
        }
        if (plan.getBasePriceRequired() != null && plan.getBasePriceRequired()) {
            errors.add("PRODUCT_BASED plans should not require base price (products determine price)");
        }
    }
    
    /**
     * Validate HYBRID plan: products optional, base price required.
     */
    private void validateHybridPlan(Plans plan, List<String> errors) {
        if (plan.getRequiresProducts() != null && plan.getRequiresProducts()) {
            errors.add("HYBRID plans cannot require products (they are optional)");
        }
        if (plan.getAllowsProducts() == null || !plan.getAllowsProducts()) {
            errors.add("HYBRID plans must allow products");
        }
        if (plan.getBasePriceRequired() == null || !plan.getBasePriceRequired()) {
            errors.add("HYBRID plans must require base price");
        }
        if (plan.getBasePriceCents() == null || plan.getBasePriceCents() <= 0) {
            errors.add("HYBRID plans must have base price > 0");
        }
    }
    
    /**
     * Validate subscription request against plan rules.
     */
    public ValidationResult validateSubscriptionRequest(Plans plan, boolean hasProducts, int productCount) {
        List<String> errors = new ArrayList<>();
        
        if (plan.getPlanCategory() == null) {
            errors.add("Plan category is not set");
            return ValidationResult.failure(errors);
        }
        
        switch (plan.getPlanCategory()) {
            case "DIGITAL":
                if (hasProducts) {
                    errors.add("DIGITAL plans do not allow products in subscription");
                }
                break;
            case "PRODUCT_BASED":
                if (!hasProducts || productCount == 0) {
                    errors.add("PRODUCT_BASED plans require at least one product in subscription");
                }
                break;
            case "HYBRID":
                // Products are optional for HYBRID, no validation needed
                break;
            default:
                errors.add("Invalid plan category: " + plan.getPlanCategory());
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
    
    /**
     * Calculate total subscription price based on plan type.
     */
    public long calculateSubscriptionPrice(Plans plan, long productsTotalCents) {
        if (plan.getPlanCategory() == null) {
            throw new IllegalArgumentException("Plan category is required");
        }
        
        long basePriceCents = plan.getBasePriceCents() != null ? plan.getBasePriceCents() : 0L;
        
        switch (plan.getPlanCategory()) {
            case "DIGITAL":
                // Only base price
                return basePriceCents;
            case "PRODUCT_BASED":
                // Only products price (base price is optional/zero)
                return productsTotalCents;
            case "HYBRID":
                // Base price + products price
                return basePriceCents + productsTotalCents;
            default:
                throw new IllegalArgumentException("Invalid plan category: " + plan.getPlanCategory());
        }
    }
    
    /**
     * Set default validation flags based on plan category.
     */
    public void setDefaultValidationFlags(Plans plan) {
        if (plan.getPlanCategory() == null) {
            plan.setPlanCategory("DIGITAL"); // Default to DIGITAL
        }
        
        switch (plan.getPlanCategory()) {
            case "DIGITAL":
                plan.setRequiresProducts(false);
                plan.setAllowsProducts(false);
                plan.setBasePriceRequired(true);
                break;
            case "PRODUCT_BASED":
                plan.setRequiresProducts(true);
                plan.setAllowsProducts(true);
                plan.setBasePriceRequired(false);
                break;
            case "HYBRID":
                plan.setRequiresProducts(false);
                plan.setAllowsProducts(true);
                plan.setBasePriceRequired(true);
                break;
        }
    }
    
    /**
     * Validation result wrapper.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
}
