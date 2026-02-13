package com.subscriptionengine.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom OpenAPI customizer to enforce specific tag ordering in Swagger UI.
 * 
 * @author Neeraj Yadav
 */
@Component
public class OpenApiTagSorter implements OpenApiCustomizer {
    
    private static final List<String> TAG_ORDER = List.of(
        "Tenants",
        "Plans",
        "Subscriptions",
        "Subscription Management",
        "Ecommerce Subscriptions",
        "Deliveries",
        "Customer Dashboard",
        "Customers",
        "Webhooks"
    );
    
    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getTags() == null) {
            return;
        }
        
        // Create a new ordered list based on TAG_ORDER
        List<Tag> orderedTags = new ArrayList<>();
        
        // Add tags in the specified order
        for (String tagName : TAG_ORDER) {
            openApi.getTags().stream()
                .filter(tag -> tag.getName().equals(tagName))
                .findFirst()
                .ifPresent(orderedTags::add);
        }
        
        // Add any remaining tags that weren't in TAG_ORDER (just in case)
        openApi.getTags().stream()
            .filter(tag -> !TAG_ORDER.contains(tag.getName()))
            .forEach(orderedTags::add);
        
        // Replace the tags with the ordered list
        openApi.setTags(orderedTags);
    }
}
