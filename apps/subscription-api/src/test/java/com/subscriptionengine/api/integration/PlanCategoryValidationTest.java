package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for plan category validation.
 * Tests: DIGITAL, PRODUCT_BASED, and HYBRID plan validation rules.
 * 
 * @author Neeraj Yadav
 */
@Epic("Plan Management")
@Feature("Plan Category Validation")
class PlanCategoryValidationTest extends BaseIntegrationTest {
    
    private String testTenantId;
    
    @BeforeEach
    void setupTenant() {
        testTenantId = createTestTenant();
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("DIGITAL Plan Validation - No Products Allowed")
    @Description("Tests that DIGITAL plans reject subscriptions with products")
    @Story("Plan Category Validation")
    void testDigitalPlanValidation() {
        // Given - Create DIGITAL plan
        Map<String, Object> digitalPlanRequest = createPlanRequest("Digital Streaming Plan", "DIGITAL");
        
        Response planResponse = givenAuthenticated(testTenantId)
            .body(digitalPlanRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID planId = UUID.fromString(planResponse.jsonPath().getString("id"));
        
        // Verify plan validation flags
        assertThat(planResponse.jsonPath().getBoolean("requiresProducts")).isFalse();
        assertThat(planResponse.jsonPath().getBoolean("allowsProducts")).isFalse();
        assertThat(planResponse.jsonPath().getBoolean("basePriceRequired")).isTrue();
        
        Allure.addAttachment("DIGITAL Plan Created", "application/json", planResponse.asString());
        
        // Given - Create customer
        UUID customerId = createTestCustomer(testTenantId);
        
        // When - Attempt to create subscription with products (should fail)
        Map<String, Object> subscriptionWithProducts = createEcommerceSubscriptionRequest(customerId, planId);
        List<Map<String, Object>> products = new ArrayList<>();
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "name", "Coffee Beans",
            "quantity", 2,
            "priceCents", 1500
        ));
        subscriptionWithProducts.put("products", products);
        
        Response errorResponse = givenAuthenticated(testTenantId)
            .body(subscriptionWithProducts)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        // Then - Verify error message
        String errorMessage = errorResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("DIGITAL")
                                 .containsIgnoringCase("products");
        
        Allure.addAttachment("Validation Error", "application/json", errorResponse.asString());
        
        // When - Create subscription without products (should succeed)
        Map<String, Object> validSubscription = createEcommerceSubscriptionRequest(customerId, planId);
        validSubscription.remove("products"); // No products
        
        Response successResponse = givenAuthenticated(testTenantId)
            .body(validSubscription)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Then - Verify subscription created successfully
        assertThat(successResponse.jsonPath().getString("id")).isNotNull();
        assertThat(successResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("Valid DIGITAL Subscription", "application/json", successResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRODUCT_BASED Plan Validation - Products Required")
    @Description("Tests that PRODUCT_BASED plans require products and calculate pricing from products only")
    @Story("Plan Category Validation")
    void testProductBasedPlanValidation() {
        // Given - Create PRODUCT_BASED plan
        Map<String, Object> productPlanRequest = createPlanRequest("Product Box Plan", "PRODUCT_BASED");
        productPlanRequest.put("basePriceCents", 0); // No base price for PRODUCT_BASED
        
        Response planResponse = givenAuthenticated(testTenantId)
            .body(productPlanRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID planId = UUID.fromString(planResponse.jsonPath().getString("id"));
        
        // Verify plan validation flags
        assertThat(planResponse.jsonPath().getBoolean("requiresProducts")).isTrue();
        assertThat(planResponse.jsonPath().getBoolean("allowsProducts")).isTrue();
        assertThat(planResponse.jsonPath().getBoolean("basePriceRequired")).isFalse();
        
        Allure.addAttachment("PRODUCT_BASED Plan Created", "application/json", planResponse.asString());
        
        // Given - Create customer
        UUID customerId = createTestCustomer(testTenantId);
        
        // When - Attempt to create subscription without products (should fail)
        Map<String, Object> subscriptionWithoutProducts = createEcommerceSubscriptionRequest(customerId, planId);
        subscriptionWithoutProducts.remove("products");
        
        Response errorResponse = givenAuthenticated(testTenantId)
            .body(subscriptionWithoutProducts)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        // Then - Verify error message
        String errorMessage = errorResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("PRODUCT_BASED")
                                 .containsIgnoringCase("requires")
                                 .containsIgnoringCase("products");
        
        Allure.addAttachment("Validation Error", "application/json", errorResponse.asString());
        
        // When - Create subscription with products (should succeed)
        Map<String, Object> validSubscription = createEcommerceSubscriptionRequest(customerId, planId);
        List<Map<String, Object>> products = new ArrayList<>();
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "name", "Coffee Beans - Dark Roast",
            "quantity", 2,
            "priceCents", 1500
        ));
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "name", "Coffee Beans - Light Roast",
            "quantity", 1,
            "priceCents", 1200
        ));
        validSubscription.put("products", products);
        
        Response successResponse = givenAuthenticated(testTenantId)
            .body(validSubscription)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Then - Verify subscription created and pricing calculated from products only
        assertThat(successResponse.jsonPath().getString("id")).isNotNull();
        assertThat(successResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        // Pricing should be: (2 * 1500) + (1 * 1200) = 4200 cents
        int expectedPrice = (2 * 1500) + (1 * 1200);
        assertThat(successResponse.jsonPath().getInt("totalPriceCents")).isEqualTo(expectedPrice);
        
        Allure.addAttachment("Valid PRODUCT_BASED Subscription", "application/json", successResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("HYBRID Plan Validation - Base Price + Optional Products")
    @Description("Tests that HYBRID plans allow both base price and optional products with combined pricing")
    @Story("Plan Category Validation")
    void testHybridPlanValidation() {
        // Given - Create HYBRID plan with base price
        Map<String, Object> hybridPlanRequest = createPlanRequest("Hybrid Subscription Plan", "HYBRID");
        hybridPlanRequest.put("basePriceCents", 1000); // $10 base price
        
        Response planResponse = givenAuthenticated(testTenantId)
            .body(hybridPlanRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID planId = UUID.fromString(planResponse.jsonPath().getString("id"));
        
        // Verify plan validation flags
        assertThat(planResponse.jsonPath().getBoolean("requiresProducts")).isFalse();
        assertThat(planResponse.jsonPath().getBoolean("allowsProducts")).isTrue();
        assertThat(planResponse.jsonPath().getBoolean("basePriceRequired")).isTrue();
        
        Allure.addAttachment("HYBRID Plan Created", "application/json", planResponse.asString());
        
        // Given - Create customer
        UUID customerId = createTestCustomer(testTenantId);
        
        // Test 1: Create subscription with base price only (no products)
        Map<String, Object> baseOnlySubscription = createEcommerceSubscriptionRequest(customerId, planId);
        baseOnlySubscription.remove("products");
        
        Response baseOnlyResponse = givenAuthenticated(testTenantId)
            .body(baseOnlySubscription)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Verify base price only
        assertThat(baseOnlyResponse.jsonPath().getString("id")).isNotNull();
        assertThat(baseOnlyResponse.jsonPath().getInt("totalPriceCents")).isEqualTo(1000);
        
        Allure.addAttachment("HYBRID Subscription - Base Only", "application/json", baseOnlyResponse.asString());
        
        // Test 2: Create subscription with base price + products
        UUID customerId2 = createTestCustomer(testTenantId);
        Map<String, Object> hybridSubscription = createEcommerceSubscriptionRequest(customerId2, planId);
        List<Map<String, Object>> products = new ArrayList<>();
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "name", "Add-on Product 1",
            "quantity", 1,
            "priceCents", 500
        ));
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "name", "Add-on Product 2",
            "quantity", 1,
            "priceCents", 500
        ));
        hybridSubscription.put("products", products);
        
        Response hybridResponse = givenAuthenticated(testTenantId)
            .body(hybridSubscription)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Verify combined pricing: base (1000) + products (500 + 500) = 2000
        assertThat(hybridResponse.jsonPath().getString("id")).isNotNull();
        int expectedTotal = 1000 + 500 + 500;
        assertThat(hybridResponse.jsonPath().getInt("totalPriceCents")).isEqualTo(expectedTotal);
        
        Allure.addAttachment("HYBRID Subscription - Base + Products", "application/json", hybridResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Plan Category Update Validation - Prevent Breaking Changes")
    @Description("Tests that plan category cannot be changed if active subscriptions exist")
    @Story("Plan Category Validation")
    void testPlanValidationOnUpdate() {
        // Given - Create DIGITAL plan
        Map<String, Object> digitalPlanRequest = createPlanRequest("Digital Plan", "DIGITAL");
        
        Response planResponse = givenAuthenticated(testTenantId)
            .body(digitalPlanRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        UUID planId = UUID.fromString(planResponse.jsonPath().getString("id"));
        
        // Given - Create active subscription on this plan
        UUID customerId = createTestCustomer(testTenantId);
        Map<String, Object> subscription = createEcommerceSubscriptionRequest(customerId, planId);
        subscription.remove("products");
        
        givenAuthenticated(testTenantId)
            .body(subscription)
            .when()
            .post("/v1/ecommerce/subscriptions")
            .then()
            .statusCode(201);
        
        // When - Attempt to change plan category to PRODUCT_BASED
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("planCategory", "PRODUCT_BASED");
        
        Response errorResponse = givenAuthenticated(testTenantId)
            .body(updateRequest)
            .when()
            .patch("/v1/plans/" + planId)
            .then()
            .statusCode(400)
            .extract()
            .response();
        
        // Then - Verify category change rejected
        String errorMessage = errorResponse.jsonPath().getString("message");
        assertThat(errorMessage).containsIgnoringCase("category")
                                 .containsIgnoringCase("subscription");
        
        Allure.addAttachment("Category Change Rejected", "application/json", errorResponse.asString());
        
        // When - Update other fields (name, description) - should succeed
        Map<String, Object> safeUpdateRequest = new HashMap<>();
        safeUpdateRequest.put("name", "Updated Digital Plan Name");
        safeUpdateRequest.put("description", "Updated description");
        
        Response successResponse = givenAuthenticated(testTenantId)
            .body(safeUpdateRequest)
            .when()
            .patch("/v1/plans/" + planId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        // Then - Verify safe updates allowed
        assertThat(successResponse.jsonPath().getString("name")).isEqualTo("Updated Digital Plan Name");
        assertThat(successResponse.jsonPath().getString("description")).isEqualTo("Updated description");
        assertThat(successResponse.jsonPath().getString("planCategory")).isEqualTo("DIGITAL"); // Category unchanged
        
        Allure.addAttachment("Safe Update Success", "application/json", successResponse.asString());
        
        // When - Create new plan with different category - should succeed
        Map<String, Object> newPlanRequest = createPlanRequest("New Product Plan", "PRODUCT_BASED");
        newPlanRequest.put("basePriceCents", 0);
        
        Response newPlanResponse = givenAuthenticated(testTenantId)
            .body(newPlanRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        assertThat(newPlanResponse.jsonPath().getString("planCategory")).isEqualTo("PRODUCT_BASED");
        
        Allure.addAttachment("New Plan Created", "application/json", newPlanResponse.asString());
    }
    
    // Helper methods
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-plan-validation-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Plan Validation",
            "slug", slug,
            "status", "ACTIVE"
        );
        
        Response response = givenAuthenticated(tenantId.toString())
            .contentType("application/json")
            .body(tenantRequest)
            .when()
            .post("/v1/tenants")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return response.jsonPath().getString("id");
    }
    
    @Step("Create test customer")
    private UUID createTestCustomer(String tenantId) {
        String uniqueEmail = "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        Map<String, Object> customerRequest = Map.of(
            "email", uniqueEmail,
            "name", "Test Customer",
            "externalCustomerRef", "cust_test_" + UUID.randomUUID().toString().substring(0, 8)
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create plan request with category: {category}")
    private Map<String, Object> createPlanRequest(String name, String category) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("description", "Test plan for " + category + " validation");
        request.put("basePriceCents", 2999);
        request.put("currency", "USD");
        request.put("billingInterval", "MONTHLY");
        request.put("trialPeriodDays", 0);
        request.put("active", true);
        request.put("planCategory", category);
        
        return request;
    }
    
    @Step("Create ecommerce subscription request")
    private Map<String, Object> createEcommerceSubscriptionRequest(UUID customerId, UUID planId) {
        Map<String, Object> request = new HashMap<>();
        request.put("customerId", customerId.toString());
        request.put("planId", planId.toString());
        request.put("paymentMethodRef", "pm_test_" + UUID.randomUUID().toString().substring(0, 8));
        
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("line1", "123 Test St");
        shippingAddress.put("city", "San Francisco");
        shippingAddress.put("state", "CA");
        shippingAddress.put("postalCode", "94102");
        shippingAddress.put("country", "US");
        request.put("shippingAddress", shippingAddress);
        
        return request;
    }
}
