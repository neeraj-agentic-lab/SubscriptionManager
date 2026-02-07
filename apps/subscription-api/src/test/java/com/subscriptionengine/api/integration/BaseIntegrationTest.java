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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
@Testcontainers
public abstract class BaseIntegrationTest {
    
    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("subscription_engine_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);
    
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
}
