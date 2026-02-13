package com.subscriptionengine.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
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
            .tags(List.of(
                // Admin API Tags - Organized by logical workflow
                new Tag().name("Admin - Tenants").description("Multi-tenant management - Create and manage tenant organizations"),
                new Tag().name("Admin - Plans").description("Subscription plan management - Define pricing, billing intervals, and features"),
                new Tag().name("Admin - Users").description("User management - Create admin users, assign roles and permissions"),
                new Tag().name("Admin - Subscriptions").description("Subscription lifecycle - Create, manage, pause, resume, and cancel subscriptions"),
                new Tag().name("Admin - Deliveries").description("Delivery management - Schedule, track, and manage subscription deliveries"),
                new Tag().name("Admin - Customers").description("Customer management - View and manage customer profiles and data"),
                new Tag().name("Admin - Webhooks").description("Webhook configuration - Set up real-time event notifications"),
                new Tag().name("Admin - User Tenants").description("User-tenant assignment - Manage user access across multiple tenants"),
                new Tag().name("Admin - API Clients").description("API client management - Manage API keys and authentication"),
                new Tag().name("Admin - Subscription History").description("Audit trail - View subscription change history and events"),
                // Customer API Tags
                new Tag().name("Customer - Plans").description("Browse available subscription plans for self-signup"),
                new Tag().name("Customer - Subscriptions").description("Manage your subscriptions - Create, pause, resume, and cancel"),
                new Tag().name("Customer - Deliveries").description("Manage your deliveries - View, skip, and reschedule upcoming deliveries")
            ))
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
                    
                    ## Download OpenAPI Specification
                    
                    **Complete API Specification:**
                    - [OpenAPI JSON](/api/v3/api-docs) - Complete spec in JSON format
                    - [OpenAPI YAML](/api/v3/api-docs.yaml) - Complete spec in YAML format
                    
                    **Group-Specific Specifications:**
                    - Admin APIs: [JSON](/api/v3/api-docs/1.%20Admin%20APIs) | [YAML](/api/v3/api-docs/1.%20Admin%20APIs.yaml)
                    - Customer APIs: [JSON](/api/v3/api-docs/2.%20Customer%20APIs) | [YAML](/api/v3/api-docs/2.%20Customer%20APIs.yaml)
                    
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
    
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("1. Admin APIs")
            .pathsToMatch("/v1/admin/**")
            .displayName("Admin APIs")
            .addOpenApiCustomizer(openApi -> {
                // Filter to only show tags that start with "Admin -"
                if (openApi.getTags() != null) {
                    openApi.setTags(openApi.getTags().stream()
                        .filter(tag -> tag.getName().startsWith("Admin -"))
                        .toList());
                }
                
                // Customize description for Admin APIs
                if (openApi.getInfo() != null) {
                    openApi.getInfo().title("SubscriptionManager: Admin APIs");
                    openApi.getInfo().description("""
                        ## Admin APIs - Tenant Management & Operations
                        
                        Comprehensive administrative endpoints for managing your subscription platform. These APIs provide full control over tenants, plans, users, subscriptions, deliveries, and system configuration.
                        
                        ### 🎯 Key Admin Capabilities
                        
                        **Multi-Tenant Management**
                        - Create and configure tenant organizations
                        - Manage tenant-specific settings and branding
                        - Monitor tenant usage and billing
                        
                        **Subscription Plan Management**
                        - Define flexible pricing models and billing cycles
                        - Configure trial periods and grace periods
                        - Set up product catalogs for subscription boxes
                        - Manage plan features and entitlements
                        
                        **User & Access Control**
                        - Create admin users with role-based permissions (SUPER_ADMIN, TENANT_ADMIN, STAFF)
                        - Assign users to multiple tenants
                        - Manage API client credentials
                        
                        **Subscription Lifecycle**
                        - Create subscriptions for customers (simple or product-based)
                        - Manage subscription states (pause, resume, cancel)
                        - Handle plan changes and proration
                        - View subscription history and audit trails
                        
                        **Delivery Operations**
                        - Schedule and manage subscription deliveries
                        - Track delivery status and fulfillment
                        - Handle delivery exceptions and rescheduling
                        
                        **Customer Management**
                        - View and manage customer profiles
                        - Access customer subscription history
                        - Handle customer support requests
                        
                        **Webhooks & Integration**
                        - Configure webhook endpoints for real-time notifications
                        - Monitor webhook delivery and retry status
                        - Set up event-driven integrations
                        
                        ### 🔐 Authentication & Authorization
                        
                        All admin endpoints require JWT authentication with an admin role (SUPER_ADMIN, TENANT_ADMIN, or STAFF).
                        
                        **To test in Swagger UI:**
                        1. Click the **"Authorize" 🔓** button at the top right
                        2. Enter your JWT token (without "Bearer" prefix)
                        3. Click **"Authorize"** then **"Close"**
                        
                        **Required JWT Claims:**
                        ```json
                        {
                          "user_id": "admin-user-uuid",
                          "tenant_id": "tenant-uuid",
                          "role": "TENANT_ADMIN",
                          "email": "admin@example.com"
                        }
                        ```
                        
                        ### 📊 Role Permissions
                        
                        - **SUPER_ADMIN**: Full access to all tenants and system-wide operations
                        - **TENANT_ADMIN**: Full access to their tenant's data and configuration
                        - **STAFF**: Limited admin access to their tenant (read-only for sensitive operations)
                        
                        ### 📥 Download OpenAPI Specification
                        
                        - [Admin APIs JSON](/api/v3/api-docs/1.%20Admin%20APIs)
                        - [Admin APIs YAML](/api/v3/api-docs/1.%20Admin%20APIs.yaml)
                        
                        ### 💡 Getting Started
                        
                        1. **Set up tenant**: Use `POST /v1/admin/tenants` to create your organization
                        2. **Create plans**: Define subscription plans with `POST /v1/admin/plans`
                        3. **Add users**: Create admin users with `POST /v1/admin/users`
                        4. **Create subscriptions**: Start managing customer subscriptions with `POST /v1/admin/subscriptions`
                        
                        ### 📞 Support
                        
                        For technical support or feature requests: [GitHub Issues](https://github.com/neeraj-agentic-lab/SubscriptionManager/issues)
                        """);
                }
            })
            .build();
    }
    
    @Bean
    public GroupedOpenApi customerApi() {
        return GroupedOpenApi.builder()
            .group("2. Customer APIs")
            .pathsToMatch("/v1/customers/**")
            .displayName("Customer APIs")
            .addOpenApiCustomizer(openApi -> {
                // Filter to only show tags that start with "Customer -"
                if (openApi.getTags() != null) {
                    openApi.setTags(openApi.getTags().stream()
                        .filter(tag -> tag.getName().startsWith("Customer -"))
                        .toList());
                }
                
                // Customize description for Customer APIs
                if (openApi.getInfo() != null) {
                    openApi.getInfo().title("SubscriptionManager: Customer APIs");
                    openApi.getInfo().description("""
                        ## Customer Self-Service APIs
                        
                        Empower your customers with full control over their subscriptions and deliveries. These APIs enable customers to manage their own accounts, subscriptions, and delivery preferences without admin intervention.
                        
                        ### 🎯 Customer Capabilities
                        
                        **Browse & Subscribe**
                        - View available subscription plans
                        - Compare pricing and features
                        - Sign up for new subscriptions (simple or product-based)
                        - Select billing cycles and trial periods
                        
                        **Manage Subscriptions**
                        - View all active and past subscriptions
                        - Pause subscriptions temporarily (e.g., during vacation)
                        - Resume paused subscriptions
                        - Cancel subscriptions (with optional feedback)
                        - View subscription dashboard with upcoming charges
                        
                        **Delivery Management**
                        - View upcoming delivery schedule
                        - Skip individual deliveries
                        - Reschedule deliveries to different dates
                        - Update delivery addresses
                        - Track delivery status
                        
                        **Account Dashboard**
                        - View subscription overview and status
                        - See next billing date and amount
                        - Access delivery history
                        - Manage payment methods
                        
                        ### 🔐 Authentication
                        
                        All customer endpoints require JWT authentication with customer credentials.
                        
                        **To test in Swagger UI:**
                        1. Click the **"Authorize" 🔓** button at the top right
                        2. Enter your JWT token (without "Bearer" prefix)
                        3. Click **"Authorize"** then **"Close"**
                        
                        **Required JWT Claims:**
                        ```json
                        {
                          "user_id": "customer-user-uuid",
                          "customer_id": "customer-uuid",
                          "tenant_id": "tenant-uuid",
                          "role": "CUSTOMER",
                          "email": "customer@example.com"
                        }
                        ```
                        
                        ### 🛡️ Security & Privacy
                        
                        - Customers can **only access their own data**
                        - All requests are validated against the authenticated customer ID
                        - Admins can access customer data for support purposes
                        - All actions are logged for audit trail
                        
                        ### 📱 Common Use Cases
                        
                        **Self-Signup Flow:**
                        1. Browse plans: `GET /v1/customers/me/plans`
                        2. Create subscription: `POST /v1/customers/me/subscriptions`
                        
                        **Pause for Vacation:**
                        1. View subscriptions: `GET /v1/customers/me/subscriptions`
                        2. Pause subscription: `PATCH /v1/customers/me/subscriptions/{id}` with action "PAUSE"
                        
                        **Skip Delivery:**
                        1. View deliveries: `GET /v1/customers/me/deliveries`
                        2. Skip delivery: `PATCH /v1/customers/me/deliveries/{id}` with action "SKIP"
                        
                        **Cancel Subscription:**
                        1. View subscription dashboard: `GET /v1/customers/me/subscriptions/{id}/dashboard`
                        2. Cancel: `PATCH /v1/customers/me/subscriptions/{id}` with action "CANCEL"
                        
                        ### 📥 Download OpenAPI Specification
                        
                        - [Customer APIs JSON](/api/v3/api-docs/2.%20Customer%20APIs)
                        - [Customer APIs YAML](/api/v3/api-docs/2.%20Customer%20APIs.yaml)
                        
                        ### 💡 Integration Tips
                        
                        - Use these APIs to build customer portals and mobile apps
                        - All endpoints return consistent JSON responses
                        - Interactive examples available for all PATCH operations
                        - Rate limiting applied per customer for fair usage
                        
                        ### 📞 Support
                        
                        For technical support or feature requests: [GitHub Issues](https://github.com/neeraj-agentic-lab/SubscriptionManager/issues)
                        """);
                }
            })
            .build();
    }
}
