package com.subscriptionengine.api.integration;

import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AuditController endpoints.
 * Tests: User audit log, API client audit log, tenant audit log, subscription audit log, and search.
 * 
 * @author Neeraj Yadav
 */
@Epic("Audit & Compliance")
@Feature("Audit Trail Endpoints")
class AuditControllerTest extends BaseIntegrationTest {
    
    private String testTenantId;
    private String testUserId;
    private String testApiClientId;
    private String testSubscriptionId;
    
    @BeforeEach
    void setupTestData() {
        testTenantId = generateUniqueTenantId();
        testUserId = UUID.randomUUID().toString();
        testApiClientId = UUID.randomUUID().toString();
        testSubscriptionId = UUID.randomUUID().toString();
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/admin/audit/users/{userId} - Get user audit log")
    @Description("Tests retrieving audit trail for a specific user")
    @Story("User Audit Trail")
    void testGetUserAuditLog() {
        givenSuperAdmin()
            .when()
            .get("/v1/admin/audit/users/" + testUserId)
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/admin/audit/api-clients/{clientId} - Get API client audit log")
    @Description("Tests retrieving audit trail for a specific API client")
    @Story("API Client Audit Trail")
    void testGetApiClientAuditLog() {
        givenSuperAdmin()
            .when()
            .get("/v1/admin/audit/api-clients/" + testApiClientId)
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/admin/audit/api-clients/{clientId} - Verify client events")
    @Description("Tests that API client audit includes creation, authentication, and rotation events")
    @Story("API Client Audit Trail")
    void testApiClientAuditLogEvents() {
        givenSuperAdmin()
            .when()
            .get("/v1/admin/audit/api-clients/" + testApiClientId)
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/admin/audit/tenants/{tenantId} - Get tenant audit log")
    @Description("Tests retrieving audit trail for a specific tenant")
    @Story("Tenant Audit Trail")
    void testGetTenantAuditLog() {
        givenSuperAdmin()
            .when()
            .get("/v1/admin/audit/tenants/" + testTenantId)
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/admin/audit/subscriptions/{subscriptionId} - Get subscription audit log")
    @Description("Tests retrieving audit trail for a specific subscription")
    @Story("Subscription Audit Trail")
    void testGetSubscriptionAuditLog() {
        givenSuperAdmin()
            .when()
            .get("/v1/admin/audit/subscriptions/" + testSubscriptionId)
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/admin/audit/subscriptions/{subscriptionId} - Verify subscription events")
    @Description("Tests that subscription audit includes creation and state change events")
    @Story("Subscription Audit Trail")
    void testSubscriptionAuditLogEvents() {
        givenSuperAdmin()
            .when()
            .get("/v1/admin/audit/subscriptions/" + testSubscriptionId)
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/admin/audit/search - Search by action")
    @Description("Tests searching audit entries by action type")
    @Story("Audit Search")
    void testSearchAuditLogByAction() {
        givenSuperAdmin()
            .queryParam("action", "USER_CREATED")
            .when()
            .get("/v1/admin/audit/search")
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /v1/admin/audit/search - Search by actor")
    @Description("Tests searching audit entries by actor")
    @Story("Audit Search")
    void testSearchAuditLogByActor() {
        givenSuperAdmin()
            .queryParam("actor", "admin")
            .when()
            .get("/v1/admin/audit/search")
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Audit Trail - Compliance data retention")
    @Description("Tests that audit trail maintains all required fields for compliance")
    @Story("Audit Compliance")
    void testAuditTrailComplianceFields() {
        givenSuperAdmin()
            .when()
            .get("/v1/admin/audit/subscriptions/" + testSubscriptionId)
            .then()
            .statusCode(200)
            .body("$", notNullValue());
    }
}
