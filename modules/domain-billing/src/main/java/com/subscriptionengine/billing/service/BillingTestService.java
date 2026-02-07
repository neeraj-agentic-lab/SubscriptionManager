/**
 * Test service to debug billing flow issues.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.billing.service;

import com.subscriptionengine.auth.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BillingTestService {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingTestService.class);
    
    private final InvoiceGenerationService invoiceGenerationService;
    
    public BillingTestService(InvoiceGenerationService invoiceGenerationService) {
        this.invoiceGenerationService = invoiceGenerationService;
    }
    
    public void testInvoiceGeneration() {
        // Set tenant context for testing
        TenantContext.setTenantId(UUID.fromString("d541f18d-59e0-4a76-a0fc-1c19da0285ed"));
        
        try {
            logger.info("Starting invoice generation test...");
            
            UUID subscriptionId = UUID.fromString("a44d9564-ed4f-45fe-95e6-4cf0db58bbdd");
            String productId = "coffee-beans-premium";
            UUID planId = UUID.fromString("2b2183b5-4e3b-4379-9a82-553e4e307354");
            
            logger.info("Test parameters: subscription={}, product={}, plan={}", 
                       subscriptionId, productId, planId);
            
            UUID invoiceId = invoiceGenerationService.generateInvoiceForProductRenewal(
                subscriptionId, productId, planId);
            
            logger.info("Invoice generation test SUCCESSFUL: created invoice {}", invoiceId);
            
        } catch (Exception e) {
            logger.error("Invoice generation test FAILED: {}", e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
    }
}
