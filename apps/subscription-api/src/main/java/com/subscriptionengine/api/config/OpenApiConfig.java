package com.subscriptionengine.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * Provides comprehensive API documentation with Swagger UI.
 * 
 * @author Neeraj Yadav
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private int serverPort;
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    @Bean
    public OpenAPI customOpenAPI() {
        String baseUrl = "http://localhost:" + serverPort + (contextPath != null && !contextPath.isEmpty() ? contextPath : "");
        
        return new OpenAPI()
            .info(new Info()
                .title("Subscription Management API")
                .version("1.0.0")
                .description("""
                    Multi-tenant subscription management platform with comprehensive features:
                    - Tenant isolation and management
                    - Subscription plans and pricing
                    - Customer management
                    - Subscription lifecycle (create, pause, resume, cancel)
                    - Delivery scheduling and management
                    - Webhook notifications
                    - Ecommerce subscriptions with product lists
                    - Customer dashboard
                    """)
                .contact(new Contact()
                    .name("API Support")
                    .email("support@subscriptionengine.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://subscriptionengine.com/license")))
            .servers(List.of(
                new Server()
                    .url(baseUrl)
                    .description("Development server")
            ));
    }
}
