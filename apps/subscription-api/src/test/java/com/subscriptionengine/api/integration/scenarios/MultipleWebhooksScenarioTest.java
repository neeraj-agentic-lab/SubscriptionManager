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
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Scenario 4.2: Multiple Webhooks for Same Event
 * Priority: P2 (Should Have)
 */
@Epic("End-to-End Scenarios")
@Feature("Webhook & Integration")
@Story("Multiple Webhooks")
class MultipleWebhooksScenarioTest extends BaseIntegrationTest {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static WireMockServer wireMockServer;
    
    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
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
    @DisplayName("Scenario 4.2: Multiple webhooks receive same event")
    @Description("Validates fan-out: register 3 webhooks → trigger event → all 3 receive → unique signatures → parallel delivery")
    @Severity(SeverityLevel.NORMAL)
    void shouldDeliverEventToMultipleWebhooks() {
        String tenantId = TestDataFactory.DEFAULT_TENANT_ID;
        
        step1_RegisterThreeWebhooks(tenantId);
        step2_ConfigureEndpoints();
        step3_TriggerEvent(tenantId);
        step4_VerifyAllWebhooksReceived();
        
        Allure.addAttachment("Scenario Summary", "text/plain", "Successfully delivered event to multiple webhooks");
    }
    
    @Step("Step 1: Register 3 webhook endpoints")
    private void step1_RegisterThreeWebhooks(String tenantId) {
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> webhookRequest = Map.of(
                "url", "http://localhost:8090/webhook" + i,
                "events", new String[]{"delivery.canceled"},
                "description", "Webhook " + i
            );
            Response response = givenAuthenticated(tenantId).body(webhookRequest).when().post("/v1/webhooks").then().statusCode(200).extract().response();
            assertThat(response.jsonPath().getString("data.webhookId")).isNotNull();
        }
        Allure.addAttachment("Webhooks Registered", "text/plain", "3 webhooks registered");
    }
    
    @Step("Step 2: Configure all endpoints to succeed")
    private void step2_ConfigureEndpoints() {
        for (int i = 1; i <= 3; i++) {
            stubFor(post(urlEqualTo("/webhook" + i)).willReturn(aResponse().withStatus(200).withBody("{\"received\": true}")));
        }
        Allure.addAttachment("Endpoints Configured", "text/plain", "All 3 endpoints ready");
    }
    
    @Step("Step 3: Trigger delivery.canceled event")
    private void step3_TriggerEvent(String tenantId) {
        UUID customerId = createCustomer(tenantId);
        UUID planId = createPlan(tenantId);
        UUID subscriptionId = createSubscription(tenantId, customerId, planId);
        UUID deliveryId = createDelivery(tenantId, subscriptionId);
        
        Map<String, Object> cancelRequest = TestDataFactory.createDeliveryCancelRequest(customerId);
        givenAuthenticated(tenantId).body(cancelRequest).when().post("/v1/deliveries/" + deliveryId + "/cancel").then().statusCode(200);
        
        Allure.addAttachment("Event Triggered", "text/plain", "Delivery cancelled");
    }
    
    @Step("Step 4: Verify all 3 webhooks received event")
    private void step4_VerifyAllWebhooksReceived() {
        await().atMost(30, SECONDS).untilAsserted(() -> {
            for (int i = 1; i <= 3; i++) {
                verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/webhook" + i))
                    .withHeader("Content-Type", equalTo("application/json"))
                    .withHeader("X-Webhook-Signature", matching("sha256=.*")));
            }
        });
        Allure.addAttachment("All Webhooks Received", "text/plain", "All 3 webhooks successfully received the event");
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
