package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for unified subscription creation endpoint.
 * Tests: POST /v1/admin/subscriptions supports both simple SaaS subscriptions and 
 * product-based subscriptions with optional products array.
 * Verifies backward compatibility and new product-based subscription capabilities.
 */
@Epic("Unified Subscriptions")
@Feature("Subscription Creation (Simple & Product-Based)")
class UnifiedSubscriptionTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @DisplayName("Should create product-based subscription with multiple products")
    @Description("Tests POST /v1/admin/subscriptions with products array")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Unified subscription creation")
    void shouldCreateProductBasedSubscription() {
        String tenantId = testTenantId;
        
        // Create plan for subscription
        UUID planId = createBasePlan(tenantId);
        
        Map<String, Object> product1 = Map.of(
            "productId", "coffee-beans-001",
            "productName", "Premium Coffee Beans",
            "quantity", 2,
            "unitPriceCents", 1599,
            "currency", "USD"
        );
        
        Map<String, Object> product2 = Map.of(
            "productId", "coffee-filter-002",
            "productName", "Coffee Filters Pack",
            "quantity", 1,
            "unitPriceCents", 599,
            "currency", "USD"
        );
        
        Map<String, Object> unifiedRequest = Map.of(
            "planId", planId,
            "products", List.of(product1, product2),
            "customerEmail", "product-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
            "customerFirstName", "John",
            "customerLastName", "Doe",
            "paymentMethodRef", "pm_stripe_" + UUID.randomUUID().toString().substring(0, 8)
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(unifiedRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("id")).isNotNull();
        assertThat(response.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Product-Based Subscription Created", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should validate required fields for subscription creation")
    @Description("Tests validation for missing required fields")
    @Severity(SeverityLevel.NORMAL)
    @Story("Input validation")
    void shouldValidateRequiredFields() {
        String tenantId = testTenantId;
        
        // Missing products
        Map<String, Object> invalidRequest = Map.of(
            "customerEmail", "test@example.com",
            "customerFirstName", "John",
            "paymentMethodRef", "pm_test_123"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(invalidRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("error")).isNotNull();
        
        Allure.addAttachment("Validation Error", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should create subscription with single product")
    @Description("Tests ecommerce subscription with one product")
    @Severity(SeverityLevel.NORMAL)
    @Story("Unified subscription creation")
    void shouldCreateSubscriptionWithSingleProduct() {
        String tenantId = testTenantId;
        UUID basePlanId = createBasePlan(tenantId);
        
        Map<String, Object> product = Map.of(
            "productId", "single-product-001",
            "productName", "Monthly Box",
            "quantity", 1,
            "unitPriceCents", 2999,
            "currency", "USD"
        );
        
        Map<String, Object> unifiedRequest = Map.of(
            "planId", basePlanId,
            "products", List.of(product),
            "customerEmail", "single-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
            "customerFirstName", "Jane",
            "customerLastName", "Smith",
            "paymentMethodRef", "pm_test_single"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(unifiedRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("id")).isNotNull();
        
        Allure.addAttachment("Single Product Subscription", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should create simple SaaS subscription without products")
    @Description("Tests POST /v1/admin/subscriptions without products array (backward compatibility)")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Unified subscription creation")
    void shouldCreateSimpleSubscriptionWithoutProducts() {
        String tenantId = testTenantId;
        UUID planId = createBasePlan(tenantId);
        
        Map<String, Object> simpleRequest = Map.of(
            "planId", planId,
            "customerEmail", "simple-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
            "customerFirstName", "Alice",
            "customerLastName", "Johnson",
            "paymentMethodRef", "pm_simple_test"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(simpleRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("id")).isNotNull();
        assertThat(response.jsonPath().getString("status")).isIn("ACTIVE", "TRIALING");
        
        Allure.addAttachment("Simple Subscription Created", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should validate product pricing")
    @Description("Tests that product prices must be positive")
    @Severity(SeverityLevel.NORMAL)
    @Story("Input validation")
    void shouldValidateProductPricing() {
        String tenantId = testTenantId;
        UUID basePlanId = createBasePlan(tenantId);
        
        Map<String, Object> invalidProduct = Map.of(
            "productId", "invalid-price-001",
            "productName", "Invalid Product",
            "quantity", 1,
            "unitPriceCents", -100, // Negative price
            "currency", "USD"
        );
        
        Map<String, Object> unifiedRequest = Map.of(
            "planId", basePlanId,
            "products", List.of(invalidProduct),
            "customerEmail", "invalid-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com",
            "customerFirstName", "Test",
            "paymentMethodRef", "pm_test"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(unifiedRequest)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getString("error")).isNotNull();
        
        Allure.addAttachment("Price Validation Error", "application/json", response.asString());
    }
    
    // Helper methods
    
    @Step("Create base plan")
    private UUID createBasePlan(String tenantId) {
        Map<String, Object> planRequest = Map.of(
            "name", "Unified Subscription Plan",
            "description", "Plan supporting both simple and product-based subscriptions",
            "basePriceCents", 1, // Minimum positive price (validation requires positive)
            "currency", "USD",
            "billingInterval", "MONTHLY",
            "trialPeriodDays", 0,
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/admin/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-unified-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Unified Subscriptions",
            "slug", slug,
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/admin/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return response.jsonPath().getString("id");
    }
}
