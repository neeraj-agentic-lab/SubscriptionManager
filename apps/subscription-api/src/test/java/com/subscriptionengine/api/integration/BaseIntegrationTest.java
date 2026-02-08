package com.subscriptionengine.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

/**
 * Base class for integration tests.
 * Provides Testcontainers setup with PostgreSQL and REST Assured configuration.
 * 
 * @author Neeraj Yadav
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.profiles.active=test"
)
public abstract class BaseIntegrationTest {
    
    // Use singleton container that survives across all test classes
    protected static final PostgreSQLContainer<?> postgres = PostgresTestContainer.getInstance();
    
    @LocalServerPort
    protected int port;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    protected RequestSpecification requestSpec;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }
    
    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api";  // Match production context path
        
        // Enable detailed logging for debugging
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        requestSpec = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .log(LogDetail.ALL)
            .build();
    }
    
    /**
     * Get base request specification with JWT authentication.
     */
    protected RequestSpecification givenAuthenticated(String tenantId) {
        String jwt = JwtTestHelper.generateToken(tenantId);
        return RestAssured.given(requestSpec)
            .header("Authorization", "Bearer " + jwt);
    }
    
    /**
     * Get base request specification without authentication.
     */
    protected RequestSpecification given() {
        return RestAssured.given(requestSpec);
    }
    
    /**
     * Generate a unique tenant ID for test isolation.
     * Each test class should use this to avoid database state pollution.
     */
    protected String generateUniqueTenantId() {
        return UUID.randomUUID().toString();
    }
}
