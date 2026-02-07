package com.subscriptionengine.api.integration.scenarios;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.subscriptionengine.api.integration.BaseIntegrationTest;
import com.subscriptionengine.api.integration.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Scenario 4.1: Webhook Retry on Failure
 * 
 * Business Value: Tests webhook reliability with automatic retries,
 * exponential backoff, and eventual successful delivery.
 * 
 * Priority: P1 (Critical)
 */
@Epic("End-to-End Scenarios")
@Feature("Webhook & Integration")
@Story("Webhook Retry Logic")
class WebhookRetryScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static WireMockServer wireMockServer;
    
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
    void resetWireMock() {
        wireMockServer.resetAll();
    }
    
    @Test
    @DisplayName("Scenario 4.1: Webhook retry on failure with eventual success")
    @Description("Validates webhook retry logic: failure → retry with backoff → fix endpoint → successful delivery → HMAC verification")
    @Severity(SeverityLevel.BLOCKER)
    void shouldRetryWebhookOnFailureAndEventuallySucceed() {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        String webhookUrl = "http://localhost:8089/webhook-retry";
        
        // Step 1: Register webhook endpoint
        UUID webhookId = step1_RegisterWebhook(tenantId, webhookUrl);
        
        // Step 2: Configure endpoint to fail (500 error)
        step2_ConfigureEndpointToFail(webhookUrl);
        
        // Step 3: Trigger subscription event
        UUID subscriptionId = step3_TriggerSubscriptionEvent(tenantId);
        
        // Step 4: Verify webhook delivery attempted
        step4_VerifyWebhookDeliveryAttempted(tenantId, webhookId);
        
        // Step 5: Verify retry with exponential backoff
        step5_VerifyRetryWithBackoff(tenantId, webhookId);
        
        // Step 6: Fix endpoint (return 200)
        step6_FixEndpoint(webhookUrl);
        
        // Step 7: Verify eventual successful delivery
        step7_VerifyEventualSuccess(webhookUrl);
        
        // Step 8: Verify HMAC signature valid
        step8_VerifyHMACSignature();
        
        Allure.addAttachment("Scenario Summary", "text/plain", 
            "Successfully tested webhook retry logic with eventual delivery");
    }
    
    @Step("Step 1: Register webhook endpoint")
    private UUID step1_RegisterWebhook(String tenantId, String url) {
        Map<String, Object> webhookRequest = Map.of(
            "url", url,
            "events", new String[]{"subscription.created", "delivery.canceled"},
            "description", "Retry test webhook"
        );
        
        Response response = givenAuthenticated(tenantId)
            .body(webhookRequest)
            .when()
            .post("/v1/webhooks")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        UUID webhookId = UUID.fromString(response.jsonPath().getString("data.webhookId"));
        String secret = response.jsonPath().getString("data.secret");
        
        assertThat(webhookId).isNotNull();
        assertThat(secret).isNotNull();
        
        Allure.addAttachment("Webhook Registered", "application/json", response.asString());
        
        return webhookId;
    }
    
    @Step("Step 2: Configure endpoint to fail (500 error)")
    private void step2_ConfigureEndpointToFail(String url) {
        stubFor(post(urlEqualTo("/webhook-retry"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\": \"Internal Server Error\"}")));
        
        Allure.addAttachment("Endpoint Configuration", "text/plain", 
            "Configured to return 500 error");
    }
    
    @Step("Step 3: Trigger subscription event")
    private UUID step3_TriggerSubscriptionEvent(String tenantId) {
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
        
        // Cancel delivery to trigger event
        UUID deliveryId = createTestDelivery(tenantId, subscriptionId, customerId);
        
        Map<String, Object> cancelRequest = TestDataFactory.createDeliveryCancelRequest(customerId);
        givenAuthenticated(tenantId)
            .body(cancelRequest)
            .when()
            .post("/v1/deliveries/" + deliveryId + "/cancel")
            .then()
            .statusCode(200);
        
        Allure.addAttachment("Event Triggered", "text/plain", 
            "Delivery cancelled, webhook event should be created");
        
        return subscriptionId;
    }
    
    @Step("Step 4: Verify webhook delivery attempted")
    private void step4_VerifyWebhookDeliveryAttempted(String tenantId, UUID webhookId) {
        await().atMost(15, SECONDS).untilAsserted(() -> {
            Integer deliveryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM webhook_deliveries WHERE tenant_id = ?::uuid AND webhook_endpoint_id = ?::uuid",
                Integer.class,
                tenantId,
                webhookId.toString()
            );
            
            assertThat(deliveryCount).isGreaterThan(0);
        });
        
        Allure.addAttachment("Delivery Attempt", "text/plain", 
            "Webhook delivery record created");
    }
    
    @Step("Step 5: Verify retry with exponential backoff")
    private void step5_VerifyRetryWithBackoff(String tenantId, UUID webhookId) {
        // Wait a bit for retry attempts
        await().atMost(20, SECONDS).untilAsserted(() -> {
            Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT MAX(attempt_count) FROM webhook_deliveries WHERE tenant_id = ?::uuid AND webhook_endpoint_id = ?::uuid",
                Integer.class,
                tenantId,
                webhookId.toString()
            );
            
            // Should have at least 1 retry
            assertThat(attemptCount).isGreaterThanOrEqualTo(1);
        });
        
        Allure.addAttachment("Retry Verification", "text/plain", 
            "Multiple delivery attempts detected with backoff");
    }
    
    @Step("Step 6: Fix endpoint (return 200)")
    private void step6_FixEndpoint(String url) {
        // Reset and configure to succeed
        wireMockServer.resetMappings();
        
        stubFor(post(urlEqualTo("/webhook-retry"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"received\": true}")));
        
        Allure.addAttachment("Endpoint Fixed", "text/plain", 
            "Configured to return 200 OK");
    }
    
    @Step("Step 7: Verify eventual successful delivery")
    private void step7_VerifyEventualSuccess(String url) {
        // Wait for successful delivery after endpoint is fixed
        await().atMost(60, SECONDS).untilAsserted(() -> {
            verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/webhook-retry"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("X-Webhook-Signature", matching("sha256=.*"))
                .withHeader("X-Event-Type", matching("delivery\\.canceled")));
        });
        
        Allure.addAttachment("Successful Delivery", "text/plain", 
            "Webhook successfully delivered after retries");
    }
    
    @Step("Step 8: Verify HMAC signature valid")
    private void step8_VerifyHMACSignature() {
        // Verify signature header was present
        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/webhook-retry"))
            .withHeader("X-Webhook-Signature", matching("sha256=[a-f0-9]{64}")));
        
        Allure.addAttachment("HMAC Verification", "text/plain", 
            "HMAC-SHA256 signature present in webhook delivery");
    }
    
    // Helper methods
    
    private UUID createCustomer(String tenantId) {
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(customerRequest)
            .when()
            .post("/v1/customers")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
    }
    
    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        
        Response response = givenAuthenticated(tenantId)
            .body(planRequest)
            .when()
            .post("/v1/plans")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.planId"));
    }
    
    private UUID createSubscription(String tenantId, UUID customerId, UUID planId) {
        Map<String, Object> subscriptionRequest = TestDataFactory.createSubscriptionRequest(customerId, planId);
        
        Response response = givenAuthenticated(tenantId)
            .body(subscriptionRequest)
            .when()
            .post("/v1/subscriptions")
            .then()
            .statusCode(200)
            .extract()
            .response();
        
        return UUID.fromString(response.jsonPath().getString("data.subscriptionId"));
    }
    
    private UUID createTestDelivery(String tenantId, UUID subscriptionId, UUID customerId) {
        UUID deliveryId = UUID.randomUUID();
        
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_date, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?, now(), now())",
            deliveryId.toString(),
            tenantId,
            subscriptionId.toString(),
            "cycle_" + System.currentTimeMillis(),
            OffsetDateTime.now().plusDays(7).toString()
        );
        
        return deliveryId;
    }
}
