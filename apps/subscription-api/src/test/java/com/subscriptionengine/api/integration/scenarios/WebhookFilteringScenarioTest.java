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
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Scenario 4.3: Webhook Event Filtering
 * Priority: P2 (Should Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Webhook & Integration")
@Story("Event Filtering")
class WebhookFilteringScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static WireMockServer wireMockServer;
    
    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8091);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8091);
    }
    
    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) wireMockServer.stop();
    }
    
    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }
    
    @Test
    @DisplayName("Scenario 4.3: Webhook event filtering")
    @Description("Validates selective notifications: Webhook A gets subscription.* → Webhook B gets delivery.* → filtering works")
    @Severity(SeverityLevel.NORMAL)
    void shouldFilterWebhookEventsByType() {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        step1_RegisterFilteredWebhooks(tenantId);
        step2_ConfigureEndpoints();
        step3_TriggerSubscriptionEvent(tenantId);
        step4_TriggerDeliveryEvent(tenantId);
        step5_VerifyFiltering();
        
        Allure.addAttachment("Scenario Summary", "text/plain", "Successfully validated webhook event filtering");
    }
    
    @Step("Step 1: Register webhooks with event filters")
    private void step1_RegisterFilteredWebhooks(String tenantId) {
        // Webhook A: subscription events only
        Map<String, Object> webhookA = Map.of(
            "url", "http://localhost:8091/webhook-subscription",
            "events", new String[]{"subscription.created", "subscription.canceled"},
            "description", "Subscription events only"
        );
        givenAuthenticated(tenantId).body(webhookA).when().post("/v1/webhooks").then().statusCode(200);
        
        // Webhook B: delivery events only
        Map<String, Object> webhookB = Map.of(
            "url", "http://localhost:8091/webhook-delivery",
            "events", new String[]{"delivery.canceled"},
            "description", "Delivery events only"
        );
        givenAuthenticated(tenantId).body(webhookB).when().post("/v1/webhooks").then().statusCode(200);
        
        Allure.addAttachment("Webhooks Registered", "text/plain", "2 webhooks with different event filters");
    }
    
    @Step("Step 2: Configure endpoints")
    private void step2_ConfigureEndpoints() {
        stubFor(post(urlEqualTo("/webhook-subscription")).willReturn(aResponse().withStatus(200)));
        stubFor(post(urlEqualTo("/webhook-delivery")).willReturn(aResponse().withStatus(200)));
        Allure.addAttachment("Endpoints Ready", "text/plain", "Both endpoints configured");
    }
    
    @Step("Step 3: Trigger subscription.created event")
    private void step3_TriggerSubscriptionEvent(String tenantId) {
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        createSubscription(tenantId, customerId, planId);
        Allure.addAttachment("Subscription Event", "text/plain", "subscription.created triggered");
    }
    
    @Step("Step 4: Trigger delivery.canceled event")
    private void step4_TriggerDeliveryEvent(String tenantId) {
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
        UUID deliveryId = createDelivery(tenantId, subscriptionId);
        
        Map<String, Object> cancelRequest = TestDataFactory.createDeliveryCancelRequest(customerId);
        givenAuthenticated(tenantId).body(cancelRequest).when().post("/v1/deliveries/" + deliveryId + "/cancel").then().statusCode(200);
        
        Allure.addAttachment("Delivery Event", "text/plain", "delivery.canceled triggered");
    }
    
    @Step("Step 5: Verify filtering works correctly")
    private void step5_VerifyFiltering() {
        await().atMost(30, SECONDS).untilAsserted(() -> {
            // Webhook A should receive subscription events
            verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/webhook-subscription"))
                .withHeader("X-Event-Type", matching("subscription\\..*")));
            
            // Webhook B should receive delivery events
            verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/webhook-delivery"))
                .withHeader("X-Event-Type", equalTo("delivery.canceled")));
        });
        
        // Verify webhook-subscription did NOT receive delivery events
        verify(0, postRequestedFor(urlEqualTo("/webhook-subscription"))
            .withHeader("X-Event-Type", equalTo("delivery.canceled")));
        
        Allure.addAttachment("Filtering Verified", "text/plain", "Each webhook received only its subscribed events");
    }
    
    private UUID createCustomer(String tenantId) {
        Map<String, Object> customerRequest = TestDataFactory.createCustomerRequest();
        Response response = givenAuthenticated(tenantId).body(customerRequest).when().post("/v1/customers").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.customerId"));
    }
    
    private UUID createPlan(String tenantId) {
        Map<String, Object> planRequest = TestDataFactory.createPlanRequest();
        Response response = givenAuthenticated(tenantId).body(planRequest).when().post("/v1/plans").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.planId"));
    }
    
    private UUID createSubscription(String tenantId, UUID customerId, UUID planId) {
        Map<String, Object> subscriptionRequest = TestDataFactory.createSubscriptionRequest(customerId, planId);
        Response response = givenAuthenticated(tenantId).body(subscriptionRequest).when().post("/v1/subscriptions").then().statusCode(200).extract().response();
        return UUID.fromString(response.jsonPath().getString("data.subscriptionId"));
    }
    
    private UUID createDelivery(String tenantId, UUID subscriptionId) {
        UUID deliveryId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO delivery_instances (id, tenant_id, subscription_id, cycle_key, status, scheduled_date, created_at, updated_at) " +
            "VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'PENDING', ?, now(), now())",
            deliveryId.toString(), tenantId, subscriptionId.toString(), "cycle_" + System.currentTimeMillis(),
            OffsetDateTime.now().plusDays(7).toString()
        );
        return deliveryId;
    }
}
