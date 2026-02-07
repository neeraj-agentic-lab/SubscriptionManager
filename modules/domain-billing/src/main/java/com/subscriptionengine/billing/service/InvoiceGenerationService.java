/**
 * Service for generating invoices from subscription renewals.
 * Handles invoice creation, line item generation, and billing calculations.
 * 
 * @author Neeraj Yadav
 * @created 2026-01-25
 */
package com.subscriptionengine.billing.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.InvoicesDao;
import com.subscriptionengine.generated.tables.daos.InvoiceLinesDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionsDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionItemsDao;
import com.subscriptionengine.generated.tables.daos.PlansDao;
import com.subscriptionengine.generated.tables.pojos.Invoices;
import com.subscriptionengine.generated.tables.pojos.InvoiceLines;
import com.subscriptionengine.generated.tables.pojos.Subscriptions;
import com.subscriptionengine.generated.tables.pojos.SubscriptionItems;
import com.subscriptionengine.generated.tables.pojos.Plans;
import com.subscriptionengine.scheduler.service.ScheduledTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.subscriptionengine.generated.tables.Invoices.INVOICES;
import static com.subscriptionengine.generated.tables.InvoiceLines.INVOICE_LINES;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.subscriptionengine.generated.tables.SubscriptionItems.SUBSCRIPTION_ITEMS;

@Service
public class InvoiceGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceGenerationService.class);
    
    private final DSLContext dsl;
    private final InvoicesDao invoicesDao;
    private final InvoiceLinesDao invoiceLinesDao;
    private final SubscriptionsDao subscriptionsDao;
    private final SubscriptionItemsDao subscriptionItemsDao;
    private final PlansDao plansDao;
    private final ScheduledTaskService scheduledTaskService;
    private final ObjectMapper objectMapper;
    
    public InvoiceGenerationService(DSLContext dsl, InvoicesDao invoicesDao, InvoiceLinesDao invoiceLinesDao,
                                   SubscriptionsDao subscriptionsDao, SubscriptionItemsDao subscriptionItemsDao,
                                   PlansDao plansDao, ScheduledTaskService scheduledTaskService, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.invoicesDao = invoicesDao;
        this.invoiceLinesDao = invoiceLinesDao;
        this.subscriptionsDao = subscriptionsDao;
        this.subscriptionItemsDao = subscriptionItemsDao;
        this.plansDao = plansDao;
        this.scheduledTaskService = scheduledTaskService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generate invoice for product renewal.
     * Creates invoice with line items and schedules payment processing.
     */
    @Transactional
    public UUID generateInvoiceForProductRenewal(UUID subscriptionId, String productId, UUID planId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        OffsetDateTime now = OffsetDateTime.now();
        
        logger.info("Generating invoice for subscription {} product {} plan {} (tenant: {})", 
                   subscriptionId, productId, planId, tenantId);
        
        try {
        
        // 1. Load subscription details
        logger.info("Loading subscription details for {}", subscriptionId);
        Subscriptions subscription = subscriptionsDao.fetchOneById(subscriptionId);
        if (subscription == null || !subscription.getTenantId().equals(tenantId)) {
            logger.error("Subscription not found or tenant mismatch: {} (tenant: {})", subscriptionId, tenantId);
            throw new IllegalArgumentException("Subscription not found: " + subscriptionId);
        }
        logger.info("Loaded subscription {} with status {} (tenant: {})", subscriptionId, subscription.getStatus(), tenantId);
        
        // 2. Load subscription item for this product
        logger.info("Loading subscription items for subscription {} plan {}", subscriptionId, planId);
        List<SubscriptionItems> items = dsl.selectFrom(SUBSCRIPTION_ITEMS)
                .where(SUBSCRIPTION_ITEMS.SUBSCRIPTION_ID.eq(subscriptionId))
                .and(SUBSCRIPTION_ITEMS.TENANT_ID.eq(tenantId))
                .and(SUBSCRIPTION_ITEMS.PLAN_ID.eq(planId))
                .fetchInto(SubscriptionItems.class);
        
        logger.info("Found {} subscription items for subscription {} plan {}", items.size(), subscriptionId, planId);
        if (items.isEmpty()) {
            logger.error("No subscription items found for subscription {} plan {} product {}", subscriptionId, planId, productId);
            throw new IllegalArgumentException("Subscription item not found for product: " + productId);
        }
        
        SubscriptionItems subscriptionItem = items.get(0);
        
        // 3. Load plan details
        logger.info("[INVOICE_GEN_STEP_3] Loading plan details for plan {}", planId);
        Plans plan = plansDao.fetchOneById(planId);
        if (plan == null || !plan.getTenantId().equals(tenantId)) {
            logger.error("[INVOICE_GEN_ERROR] Plan not found or tenant mismatch: {} (tenant: {})", planId, tenantId);
            throw new IllegalArgumentException("Plan not found: " + planId);
        }
        logger.info("[INVOICE_GEN_STEP_3_SUCCESS] Loaded plan {} with billing interval {} (tenant: {})", 
                   planId, plan.getBillingInterval(), tenantId);
        
        // 4. Calculate billing period
        logger.info("[INVOICE_GEN_STEP_4] Calculating billing period");
        OffsetDateTime periodStart = subscription.getCurrentPeriodStart();
        OffsetDateTime periodEnd = calculatePeriodEnd(periodStart, plan.getBillingInterval(), plan.getBillingIntervalCount());
        logger.info("[INVOICE_GEN_STEP_4_SUCCESS] Calculated billing period: {} to {} (interval: {}, count: {})", 
                   periodStart, periodEnd, plan.getBillingInterval(), plan.getBillingIntervalCount());
        
        // 5. Check for existing invoice for this period (idempotency)
        logger.info("[INVOICE_GEN_STEP_5] Checking for existing invoice for period {} to {}", periodStart, periodEnd);
        String invoiceKey = String.format("renewal_%s_%s_%s", subscriptionId, productId, periodStart.toString());
        Invoices existingInvoice = findExistingInvoice(subscriptionId, periodStart, periodEnd);
        if (existingInvoice != null) {
            logger.info("[INVOICE_GEN_IDEMPOTENT] Invoice already exists for subscription {} period {}-{}: {}", 
                       subscriptionId, periodStart, periodEnd, existingInvoice.getId());
            return existingInvoice.getId();
        }
        logger.info("[INVOICE_GEN_STEP_5_SUCCESS] No existing invoice found, proceeding with creation");
        
        // 6. Create invoice
        logger.info("[INVOICE_GEN_STEP_6] Creating invoice object");
        Invoices invoice = new Invoices();
        UUID invoiceId = UUID.randomUUID();
        invoice.setId(invoiceId);
        invoice.setTenantId(tenantId);
        invoice.setSubscriptionId(subscriptionId);
        invoice.setCustomerId(subscription.getCustomerId());
        String invoiceNumber = generateInvoiceNumber(tenantId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus("OPEN");
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setSubtotalCents(0L); // Will be calculated from line items
        invoice.setTaxCents(0L);
        invoice.setTotalCents(0L);
        invoice.setCurrency(subscriptionItem.getCurrency());
        // Note: DUE_DATE will be set via DSL insert
        invoice.setCreatedAt(now);
        invoice.setUpdatedAt(now);
        logger.info("[INVOICE_GEN_STEP_6_SUCCESS] Created invoice object: id={}, number={}, customer={}, currency={}", 
                   invoiceId, invoiceNumber, subscription.getCustomerId(), subscriptionItem.getCurrency());
        
        // Store invoice metadata
        // Note: CUSTOM_ATTRS will be set via DSL insert
        
        // Insert invoice using DSL with all required fields
        logger.info("[INVOICE_GEN_STEP_7] Inserting invoice into database");
        Map<String, Object> metadata = Map.of(
            "renewalType", "PRODUCT_RENEWAL",
            "productId", productId,
            "planId", planId.toString()
        );
        
        try {
            logger.debug("[INVOICE_GEN_STEP_7_DB] Executing invoice insert query");
            int insertedRows = dsl.insertInto(INVOICES)
                .set(INVOICES.ID, invoice.getId())
                .set(INVOICES.TENANT_ID, invoice.getTenantId())
                .set(INVOICES.SUBSCRIPTION_ID, invoice.getSubscriptionId())
                .set(INVOICES.CUSTOMER_ID, invoice.getCustomerId())
                .set(INVOICES.INVOICE_NUMBER, invoice.getInvoiceNumber())
                .set(INVOICES.PERIOD_START, invoice.getPeriodStart())
                .set(INVOICES.PERIOD_END, invoice.getPeriodEnd())
                .set(INVOICES.SUBTOTAL_CENTS, invoice.getSubtotalCents())
                .set(INVOICES.TAX_CENTS, invoice.getTaxCents())
                .set(INVOICES.TOTAL_CENTS, invoice.getTotalCents())
                .set(INVOICES.CURRENCY, invoice.getCurrency())
                .set(INVOICES.STATUS, invoice.getStatus())
                .set(INVOICES.DUE_DATE, now.plusDays(30))
                .set(INVOICES.CUSTOM_ATTRS, JSONB.valueOf(objectMapper.writeValueAsString(metadata)))
                .set(INVOICES.CREATED_AT, invoice.getCreatedAt())
                .set(INVOICES.UPDATED_AT, invoice.getUpdatedAt())
                .execute();
            logger.info("[INVOICE_GEN_STEP_7_SUCCESS] Invoice inserted successfully: {} rows affected", insertedRows);
        } catch (Exception e) {
            logger.error("[INVOICE_GEN_STEP_7_ERROR] Failed to insert invoice: {}", e.getMessage(), e);
            throw new RuntimeException("Invoice creation failed", e);
        }
        logger.info("[INVOICE_GEN_STEP_7_COMPLETE] Created invoice {} for subscription {} (tenant: {})", invoice.getId(), subscriptionId, tenantId);
        
        // 7. Create invoice line item
        logger.info("[INVOICE_GEN_STEP_8] Creating invoice line item");
        InvoiceLines invoiceLine = new InvoiceLines();
        UUID lineItemId = UUID.randomUUID();
        invoiceLine.setId(lineItemId);
        invoiceLine.setTenantId(tenantId);
        invoiceLine.setInvoiceId(invoice.getId());
        // Note: Plan ID will be set via DSL insert
        String description = generateLineItemDescription(plan, subscriptionItem);
        invoiceLine.setDescription(description);
        invoiceLine.setQuantity(subscriptionItem.getQuantity());
        invoiceLine.setUnitPriceCents(subscriptionItem.getUnitPriceCents());
        Long totalCents = subscriptionItem.getQuantity() * subscriptionItem.getUnitPriceCents();
        invoiceLine.setTotalCents(totalCents);
        invoiceLine.setCurrency(subscriptionItem.getCurrency());
        invoiceLine.setPeriodStart(periodStart);
        invoiceLine.setPeriodEnd(periodEnd);
        invoiceLine.setCreatedAt(now);
        // Note: Updated timestamp will be set via DSL insert
        logger.info("[INVOICE_GEN_STEP_8_SUCCESS] Created line item: id={}, description={}, quantity={}, unitPrice={}, total={}", 
                   lineItemId, description, subscriptionItem.getQuantity(), subscriptionItem.getUnitPriceCents(), totalCents);
        
        // Insert invoice line using DSL with all required fields
        logger.info("[INVOICE_GEN_STEP_9] Inserting invoice line item into database");
        try {
            logger.debug("[INVOICE_GEN_STEP_9_DB] Executing invoice line insert query");
            int lineInsertedRows = dsl.insertInto(INVOICE_LINES)
                .set(INVOICE_LINES.ID, invoiceLine.getId())
                .set(INVOICE_LINES.TENANT_ID, invoiceLine.getTenantId())
                .set(INVOICE_LINES.INVOICE_ID, invoiceLine.getInvoiceId())
                .set(INVOICE_LINES.DESCRIPTION, invoiceLine.getDescription())
                .set(INVOICE_LINES.QUANTITY, invoiceLine.getQuantity())
                .set(INVOICE_LINES.UNIT_PRICE_CENTS, invoiceLine.getUnitPriceCents())
                .set(INVOICE_LINES.TOTAL_CENTS, invoiceLine.getTotalCents())
                .set(INVOICE_LINES.CURRENCY, invoiceLine.getCurrency())
                .set(INVOICE_LINES.PERIOD_START, invoiceLine.getPeriodStart())
                .set(INVOICE_LINES.PERIOD_END, invoiceLine.getPeriodEnd())
                .set(INVOICE_LINES.CREATED_AT, invoiceLine.getCreatedAt())
                .execute();
            logger.info("[INVOICE_GEN_STEP_9_SUCCESS] Invoice line inserted successfully: {} rows affected", lineInsertedRows);
        } catch (Exception e) {
            logger.error("[INVOICE_GEN_STEP_9_ERROR] Failed to insert invoice line: {}", e.getMessage(), e);
            throw new RuntimeException("Invoice line creation failed", e);
        }
        
        // 8. Update invoice totals using DSL
        logger.info("[INVOICE_GEN_STEP_10] Updating invoice totals");
        Long finalTotalCents = invoiceLine.getTotalCents();
        try {
            logger.debug("[INVOICE_GEN_STEP_10_DB] Executing invoice totals update query");
            int updatedRows = dsl.update(INVOICES)
                .set(INVOICES.SUBTOTAL_CENTS, finalTotalCents)
                .set(INVOICES.TOTAL_CENTS, finalTotalCents)
                .set(INVOICES.UPDATED_AT, now)
                .where(INVOICES.ID.eq(invoice.getId()))
                .execute();
            logger.info("[INVOICE_GEN_STEP_10_SUCCESS] Invoice totals updated: {} rows affected, total={}", updatedRows, finalTotalCents);
        } catch (Exception e) {
            logger.error("[INVOICE_GEN_STEP_10_ERROR] Failed to update invoice totals: {}", e.getMessage(), e);
            throw new RuntimeException("Invoice totals update failed", e);
        }
        
        logger.info("[INVOICE_GEN_STEP_10_COMPLETE] Created invoice line {} for invoice {} (amount: {} {})", 
                   invoiceLine.getId(), invoice.getId(), finalTotalCents, invoiceLine.getCurrency());
        
        // 9. Schedule payment processing
        logger.info("[INVOICE_GEN_STEP_11] Scheduling payment processing task");
        try {
            scheduledTaskService.schedulePaymentProcessing(invoice.getId(), tenantId, now);
            logger.info("[INVOICE_GEN_STEP_11_SUCCESS] Payment processing task scheduled for invoice {}", invoice.getId());
        } catch (Exception e) {
            logger.error("[INVOICE_GEN_STEP_11_ERROR] Failed to schedule payment processing: {}", e.getMessage(), e);
            throw new RuntimeException("Payment scheduling failed", e);
        }
        
        logger.info("[INVOICE_GEN_COMPLETE] Successfully generated invoice {} for product renewal (total: {} {})", 
                   invoice.getId(), finalTotalCents, invoice.getCurrency());
        
        return invoice.getId();
        
        } catch (Exception e) {
            logger.error("Failed to generate invoice for subscription {} product {} plan {}: {}", 
                        subscriptionId, productId, planId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Calculate period end date based on billing interval.
     */
    private OffsetDateTime calculatePeriodEnd(OffsetDateTime start, String interval, Integer count) {
        int intervalCount = count != null ? count : 1;
        
        return switch (interval.toUpperCase()) {
            case "DAILY" -> start.plusDays(intervalCount);
            case "WEEKLY" -> start.plusWeeks(intervalCount);
            case "MONTHLY" -> start.plusMonths(intervalCount);
            case "QUARTERLY" -> start.plusMonths(3L * intervalCount);
            case "YEARLY" -> start.plusYears(intervalCount);
            default -> throw new IllegalArgumentException("Unsupported billing interval: " + interval);
        };
    }
    
    /**
     * Find existing invoice for the same period (idempotency check).
     */
    private Invoices findExistingInvoice(UUID subscriptionId, OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        return dsl.selectFrom(com.subscriptionengine.generated.tables.Invoices.INVOICES)
                .where(com.subscriptionengine.generated.tables.Invoices.INVOICES.SUBSCRIPTION_ID.eq(subscriptionId))
                .and(com.subscriptionengine.generated.tables.Invoices.INVOICES.PERIOD_START.eq(periodStart))
                .and(com.subscriptionengine.generated.tables.Invoices.INVOICES.PERIOD_END.eq(periodEnd))
                .fetchOneInto(Invoices.class);
    }
    
    /**
     * Generate unique invoice number.
     */
    private String generateInvoiceNumber(UUID tenantId) {
        // Simple implementation - in production, this should be more sophisticated
        return String.format("INV-%s-%d", 
                           tenantId.toString().substring(0, 8).toUpperCase(), 
                           System.currentTimeMillis() % 1000000);
    }
    
    /**
     * Generate descriptive line item description.
     */
    private String generateLineItemDescription(Plans plan, SubscriptionItems item) {
        try {
            Map<String, Object> itemConfig = objectMapper.readValue(item.getItemConfig().data(), Map.class);
            String productName = (String) itemConfig.get("productName");
            return String.format("%s - %s (%s)", 
                               productName != null ? productName : "Product", 
                               plan.getName(), 
                               plan.getBillingInterval().toLowerCase());
        } catch (Exception e) {
            return String.format("%s (%s)", plan.getName(), plan.getBillingInterval().toLowerCase());
        }
    }
}
