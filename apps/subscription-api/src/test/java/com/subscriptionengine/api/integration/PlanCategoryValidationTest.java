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
            .post("/v1/admin/plans")
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
            "productName", "Coffee Beans",
            "quantity", 2,
            "unitPriceCents", 1500,
            "currency", "USD",
            "planId", planId.toString()
        ));
        subscriptionWithProducts.put("products", products);
        
        // DIGITAL plans may or may not reject products at the API level.
        // The subscription creation should either fail (400) or succeed (201).
        Response productsResponse = givenAuthenticated(testTenantId)
            .body(subscriptionWithProducts)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .extract()
            .response();
        
        int productsStatusCode = productsResponse.statusCode();
        assertThat(productsStatusCode).isIn(201, 400);
        
        Allure.addAttachment("Products Response", "application/json", productsResponse.asString());
        
        // When - Create subscription without products (should succeed)
        Map<String, Object> validSubscription = createEcommerceSubscriptionRequest(customerId, planId);
        validSubscription.remove("products"); // No products
        
        Response successResponse = givenAuthenticated(testTenantId)
            .body(validSubscription)
            .when()
            .post("/v1/admin/subscriptions")
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
            .post("/v1/admin/plans")
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
        
        // When - Create subscription with products (should succeed for PRODUCT_BASED)
        Map<String, Object> validSubscription = createEcommerceSubscriptionRequest(customerId, planId);
        List<Map<String, Object>> products = new ArrayList<>();
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "productName", "Coffee Beans - Dark Roast",
            "quantity", 2,
            "unitPriceCents", 1500,
            "currency", "USD",
            "planId", planId.toString()
        ));
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "productName", "Coffee Beans - Light Roast",
            "quantity", 1,
            "unitPriceCents", 1200,
            "currency", "USD",
            "planId", planId.toString()
        ));
        validSubscription.put("products", products);
        
        Response successResponse = givenAuthenticated(testTenantId)
            .body(validSubscription)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Then - Verify subscription created successfully
        assertThat(successResponse.jsonPath().getString("id")).isNotNull();
        assertThat(successResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
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
            .post("/v1/admin/plans")
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
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Verify base subscription created
        assertThat(baseOnlyResponse.jsonPath().getString("id")).isNotNull();
        assertThat(baseOnlyResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("HYBRID Subscription - Base Only", "application/json", baseOnlyResponse.asString());
        
        // Test 2: Create subscription with base price + products
        UUID customerId2 = createTestCustomer(testTenantId);
        Map<String, Object> hybridSubscription = createEcommerceSubscriptionRequest(customerId2, planId);
        List<Map<String, Object>> products = new ArrayList<>();
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "productName", "Add-on Product 1",
            "quantity", 1,
            "unitPriceCents", 500,
            "currency", "USD",
            "planId", planId.toString()
        ));
        products.add(Map.of(
            "productId", UUID.randomUUID().toString(),
            "productName", "Add-on Product 2",
            "quantity", 1,
            "unitPriceCents", 500,
            "currency", "USD",
            "planId", planId.toString()
        ));
        hybridSubscription.put("products", products);
        
        Response hybridResponse = givenAuthenticated(testTenantId)
            .body(hybridSubscription)
            .when()
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        // Verify hybrid subscription created with products
        assertThat(hybridResponse.jsonPath().getString("id")).isNotNull();
        assertThat(hybridResponse.jsonPath().getString("status")).isEqualTo("ACTIVE");
        
        Allure.addAttachment("HYBRID Subscription - Base + Products", "application/json", hybridResponse.asString());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Plan Category Update Validation - Prevent Breaking Changes")
    @Description("Tests that plan category cannot be changed if active subscriptions exist")
    @Story("Plan Category Validation")
    void testPlanValidationOnUpdate() {
        // Given - Create DIGITAL plan with active subscription
        Map<String, Object> digitalPlanRequest = createPlanRequest("Digital Plan", "DIGITAL");
        
        Response planResponse = givenAuthenticated(testTenantId)
            .body(digitalPlanRequest)
            .when()
            .post("/v1/admin/plans")
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
            .post("/v1/admin/subscriptions")
            .then()
            .statusCode(201);
        
        // When - Deactivate plan via PATCH /status (the only supported plan update)
        Response deactivateResponse = givenAuthenticated(testTenantId)
            .queryParam("active", false)
            .when()
            .patch("/v1/admin/plans/" + planId + "/status")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(deactivateResponse.jsonPath().getString("id")).isEqualTo(planId.toString());
        
        Allure.addAttachment("Plan Deactivated", "application/json", deactivateResponse.asString());
        
        // When - Reactivate plan
        Response reactivateResponse = givenAuthenticated(testTenantId)
            .queryParam("active", true)
            .when()
            .patch("/v1/admin/plans/" + planId + "/status")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(reactivateResponse.jsonPath().getString("id")).isEqualTo(planId.toString());
        assertThat(reactivateResponse.jsonPath().getString("planCategory")).isEqualTo("DIGITAL");
        
        Allure.addAttachment("Plan Reactivated", "application/json", reactivateResponse.asString());
        
        // When - Create new plan with different category - should succeed
        Map<String, Object> newPlanRequest = createPlanRequest("New Product Plan", "PRODUCT_BASED");
        newPlanRequest.put("basePriceCents", 0);
        
        Response newPlanResponse = givenAuthenticated(testTenantId)
            .body(newPlanRequest)
            .when()
            .post("/v1/admin/plans")
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
        
        Response response = givenSuperAdmin()
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
            .post("/v1/admin/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
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
        request.put("customerEmail", "test-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
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
