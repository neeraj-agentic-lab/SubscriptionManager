package com.subscriptionengine.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
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
    
    @Value("${api.base-url:}")
    private String apiBaseUrl;
    
    @Bean
    public OpenAPI customOpenAPI() {
        // Use API_BASE_URL environment variable if set (for Cloud Run), otherwise localhost
        String baseUrl;
        if (apiBaseUrl != null && !apiBaseUrl.isEmpty()) {
            baseUrl = apiBaseUrl + (contextPath != null && !contextPath.isEmpty() ? contextPath : "");
        } else {
            baseUrl = "http://localhost:" + serverPort + (contextPath != null && !contextPath.isEmpty() ? contextPath : "");
        }
        
        // Define JWT security scheme
        SecurityScheme jwtSecurityScheme = new SecurityScheme()
            .name("Bearer Authentication")
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("Enter your JWT token (without 'Bearer' prefix)");
        
        // Add security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
            .addList("Bearer Authentication");
        
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", jwtSecurityScheme))
            .addSecurityItem(securityRequirement)
            .info(new Info()
                .title("SubscriptionManager: Headless Subscription Engine")
                .version("1.0.0")
                .description("""
                    ## Overview
                    
                    A comprehensive, multi-tenant headless subscription management platform designed for modern SaaS applications.
                    Build subscription-based products with flexible billing, automated delivery, and powerful management APIs.
                    
                    ## Key Features
                    
                    ### 🏢 Multi-Tenancy
                    - Complete tenant isolation with row-level security
                    - Tenant-specific JWT authentication
                    - Automatic tenant context injection
                    
                    ### 📦 Subscription Management
                    - Flexible subscription plans with custom pricing
                    - Multiple billing cycles (daily, weekly, monthly, yearly)
                    - Subscription lifecycle management (create, pause, resume, cancel)
                    - Trial periods and grace periods
                    - Proration support for plan changes
                    
                    ### 👥 Customer Management
                    - Customer profiles and metadata
                    - Multiple subscriptions per customer
                    - Customer dashboard access
                    - Subscription history tracking
                    
                    ### 🚚 Delivery Management
                    - Automated delivery scheduling
                    - Flexible delivery frequencies
                    - Delivery address management
                    - Delivery status tracking
                    - Skip and reschedule deliveries
                    
                    ### 💳 Billing & Entitlements
                    - Automated billing cycles
                    - Invoice generation
                    - Payment tracking
                    - Usage-based entitlements
                    - Feature access control
                    
                    ### 🔔 Webhooks & Integration
                    - Real-time webhook notifications
                    - Configurable webhook endpoints
                    - Event-driven architecture
                    - Retry mechanisms with exponential backoff
                    
                    ### 🛒 Ecommerce Integration
                    - Product catalog management
                    - Subscription boxes with product lists
                    - Inventory tracking
                    - Order management
                    
                    ## Authentication
                    
                    All API endpoints require JWT authentication with tenant information embedded in the token.
                    
                    **To test in Swagger UI:**
                    1. Click the **"Authorize" 🔓** button at the top right
                    2. Enter your JWT token (without "Bearer" prefix)
                    3. Click **"Authorize"** then **"Close"**
                    4. All requests will now include the Authorization header automatically
                    
                    The JWT token should contain tenant information and be formatted as:
                    ```
                    Authorization: Bearer <your-jwt-token>
                    ```
                    
                    ## Rate Limiting
                    
                    API requests are rate-limited per tenant to ensure fair usage and system stability.
                    
                    ## Support
                    
                    For technical support, bug reports, feature requests, or integration assistance, please create an issue on GitHub:
                    https://github.com/neeraj-agentic-lab/SubscriptionManager/issues
                    """)
                .contact(new Contact()
                    .name("GitHub Issues")
                    .url("https://github.com/neeraj-agentic-lab/SubscriptionManager/issues"))
                .license(new License()
                    .name("MIT License")
                    .url("https://github.com/neeraj-agentic-lab/SubscriptionManager/blob/main/LICENSE")))
            .servers(List.of(
                new Server()
                    .url(baseUrl)
                    .description(apiBaseUrl != null && !apiBaseUrl.isEmpty() ? "Production server" : "Development server")
            ));
    }
}
