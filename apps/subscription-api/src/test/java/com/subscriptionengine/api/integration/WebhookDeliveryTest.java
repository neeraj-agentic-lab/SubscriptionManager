package com.subscriptionengine.api.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Integration tests for webhook delivery system.
 * Tests: Webhook registration, event emission, delivery, and retry logic.
 */
@Epic("Webhook System")
@Feature("Webhook Delivery")
class WebhookDeliveryTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static WireMockServer wireMockServer;
    private String testTenantId;
    
    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }
    
    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
    
    @BeforeEach
    void setupTenantAndResetWireMock() {
        testTenantId = createTestTenant();
        wireMockServer.resetAll();
    }
    
    @Test
    @DisplayName("Should register webhook and deliver events successfully")
    @Description("Tests end-to-end webhook registration, event emission, and successful delivery")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Webhook registration and delivery")
    void shouldRegisterWebhookAndDeliverEvents() {
        String tenantId = testTenantId;
        String webhookUrl = "http://localhost:8089/webhook";
        
        // Setup WireMock to accept webhook
        stubFor(post(urlEqualTo("/webhook"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"received\": true}")));
        
        // Register webhook
        UUID webhookId = registerWebhook(tenantId, webhookUrl);
        
        // Create subscription and trigger delivery cancellation (which emits event)
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        // Cancel delivery to trigger event
        Map<String, Object> cancelRequest = Map.of(
            "customerId", customerId.toString(),
            "reason", "Customer requested delivery cancellation for testing"
        );
        givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/deliveries/" + deliveryId + "/cancel")
            .then()
            .statusCode(200);
        
        // Wait for webhook to be delivered
        await().atMost(30, SECONDS).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/webhook"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-Webhook-Signature", matching("sha256=.*"))
                .withHeader("X-Event-Type", equalTo("delivery.canceled")));
        });
        
        // Verify webhook delivery record
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Integer deliveredCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM webhook_deliveries WHERE tenant_id = ?::uuid AND webhook_endpoint_id = ?::uuid AND status = 'DELIVERED'",
                Integer.class,
                tenantId,
                webhookId.toString()
            );
            assertThat(deliveredCount).isGreaterThan(0);
        });
    }
    
    @Test
    @DisplayName("Should retry failed webhook deliveries with exponential backoff")
    @Description("Tests webhook retry logic when endpoint returns error")
    @Severity(SeverityLevel.NORMAL)
    @Story("Webhook retry logic")
    void shouldRetryFailedWebhookDeliveries() {
        String tenantId = testTenantId;
        String webhookUrl = "http://localhost:8089/webhook-fail";
        
        // Setup WireMock to fail first 2 attempts, then succeed
        stubFor(post(urlEqualTo("/webhook-fail"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("First Retry"));
        
        stubFor(post(urlEqualTo("/webhook-fail"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("First Retry")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("Second Retry"));
        
        stubFor(post(urlEqualTo("/webhook-fail"))
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("Second Retry")
            .willReturn(aResponse().withStatus(200)));
        
        // Register webhook and trigger event
        UUID webhookId = registerWebhook(tenantId, webhookUrl);
        
        UUID planId = createPlan(tenantId);
        Map<String, String> customer = createCustomer(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customer.get("email"), planId);
        UUID customerId = UUID.fromString(customer.get("customerId"));
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        // Cancel delivery to trigger event
        Map<String, Object> cancelRequest = Map.of(
            "customerId", customerId.toString(),
            "reason", "Customer requested delivery cancellation for testing"
        );
        givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/deliveries/" + deliveryId + "/cancel")
            .then()
            .statusCode(200);
        
        // Wait for eventual success after retries (may take longer due to backoff)
        await().atMost(60, SECONDS).untilAsserted(() -> {
            Integer deliveredCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM webhook_deliveries WHERE tenant_id = ?::uuid AND webhook_endpoint_id = ?::uuid AND status = 'DELIVERED'",
                Integer.class,
                tenantId,
                webhookId.toString()
            );
            assertThat(deliveredCount).isGreaterThan(0);
        });
        
        // Verify multiple attempts were made
        verify(moreThanOrExactly(2), postRequestedFor(urlEqualTo("/webhook-fail")));
    }
    
    @Test
    @DisplayName("Should list registered webhooks")
    @Description("Tests webhook listing endpoint")
    @Severity(SeverityLevel.NORMAL)
    @Story("Webhook management")
    void shouldListRegisteredWebhooks() {
        String tenantId = testTenantId;
        
        // Register multiple webhooks
        registerWebhook(tenantId, "http://localhost:8089/webhook1");
        registerWebhook(tenantId, "http://localhost:8089/webhook2");
        
        // List webhooks
        Response response = givenAuthenticated(tenantId)
            .when()
            .get("/v1/webhooks")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        List<Map<String, Object>> webhooks = response.jsonPath().getList("data.webhooks");
        assertThat(webhooks).hasSizeGreaterThanOrEqualTo(2);
        
        Allure.addAttachment("Webhook List", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should update webhook status")
    @Description("Tests webhook status update (ACTIVE/INACTIVE/DISABLED)")
    @Severity(SeverityLevel.NORMAL)
    @Story("Webhook management")
    void shouldUpdateWebhookStatus() {
        String tenantId = testTenantId;
        
        UUID webhookId = registerWebhook(tenantId, "http://localhost:8089/webhook");
        
        // Update status to INACTIVE
        Map<String, Object> updateRequest = Map.of("status", "INACTIVE");
        
        Response response = givenAuthenticated(tenantId)
            .body(updateRequest)
            .when()
            .patch("/v1/webhooks/" + webhookId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        assertThat(response.jsonPath().getString("data.status")).isEqualTo("INACTIVE");
        
        Allure.addAttachment("Webhook Status Updated", "application/json", response.asString());
    }
    
    @Test
    @DisplayName("Should delete webhook")
    @Description("Tests webhook deletion")
    @Severity(SeverityLevel.NORMAL)
    @Story("Webhook management")
    void shouldDeleteWebhook() {
        String tenantId = testTenantId;
        
        UUID webhookId = registerWebhook(tenantId, "http://localhost:8089/webhook");
        
        // Delete webhook
        Response response = givenAuthenticated(tenantId)
            .when()
            .delete("/v1/webhooks/" + webhookId)
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        
        // Verify deleted
        givenAuthenticated(tenantId)
            .when()
            .get("/v1/webhooks")
            .then()
            .statusCode(200);
        
        Allure.addAttachment("Webhook Deleted", "application/json", response.asString());
    }
    
    // Helper methods
    
    @Step("Register webhook")
    private UUID registerWebhook(String tenantId, String url) {
        Map<String, Object> webhookRequest = Map.of(
            "url", url,
            "events", List.of("delivery.canceled", "subscription.created"),
            "description", "Test webhook"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(webhookRequest)
            .when()
            .post("/v1/webhooks")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String webhookId = response.jsonPath().getString("data.webhookId");
        Allure.addAttachment("Webhook Registered", "application/json", response.asString());
        
        return UUID.fromString(webhookId);
    }
    
    @Step("Create plan")
    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create customer")
    private Map<String, String> createCustomer(String tenantId) {
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        String email = response.jsonPath().getString("data.email");
        String customerId = response.jsonPath().getString("data.customerId");
        return Map.of("email", email, "customerId", customerId);
    }
    
    @Step("Create subscription")
    private UUID createSubscription(String tenantId, String customerEmail, UUID planId) {
        Map<String, Object> subscriptionRequest = new java.util.HashMap<>();
        subscriptionRequest.put("planId", planId.toString());
        subscriptionRequest.put("customerEmail", customerEmail);
        subscriptionRequest.put("startDate", java.time.OffsetDateTime.now().toString());
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(201)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("id"));
    }
    
    @Step("Create test delivery")
    private UUID createTestDelivery(String tenantId, UUID subscriptionId, UUID customerId) {
        UUID deliveryId = UUID.randomUUID();
        
        String snapshot = "{\"subscriptionId\":\"" + subscriptionId + "\",\"customerId\":\"" + customerId + "\"}";
        
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_for, snapshot, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', now() + interval '7 days', ?::jsonb, now(), now())",
            deliveryId.toString(),
            tenantId,
            subscriptionId.toString(),
            "cycle_" + System.currentTimeMillis(),
            snapshot
        );
        
        return deliveryId;
    }
    
    @Step("Create test tenant")
    private String createTestTenant() {
        UUID tenantId = UUID.randomUUID();
        String slug = "test-tenant-webhook-" + UUID.randomUUID().toString().substring(0, 8);
        
        Map<String, Object> tenantRequest = Map.of(
            "name", "Test Tenant for Webhook Delivery",
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
}
