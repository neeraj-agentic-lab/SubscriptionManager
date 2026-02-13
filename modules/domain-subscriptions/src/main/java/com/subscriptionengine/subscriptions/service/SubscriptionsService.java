package com.subscriptionengine.subscriptions.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.CustomersDao;
import com.subscriptionengine.generated.tables.daos.PlansDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionsDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionItemsDao;
import com.subscriptionengine.generated.tables.pojos.Customers;
import com.subscriptionengine.generated.tables.pojos.Plans;
import com.subscriptionengine.generated.tables.pojos.Subscriptions;
import com.subscriptionengine.generated.tables.pojos.SubscriptionItems;
import com.subscriptionengine.subscriptions.dto.CreateSubscriptionRequest;
import com.subscriptionengine.subscriptions.dto.ProductItem;
import com.subscriptionengine.subscriptions.dto.SubscriptionResponse;
import com.subscriptionengine.scheduler.service.ScheduledTaskService;
import com.subscriptionengine.plans.service.PlanValidationService;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.subscriptionengine.generated.tables.Customers.CUSTOMERS;
import static com.subscriptionengine.generated.tables.Plans.PLANS;
import static com.subscriptionengine.generated.tables.Subscriptions.SUBSCRIPTIONS;

/**
 * Service for managing subscriptions with tenant isolation.
 * Handles subscription creation, customer upsert, and plan validation.
 * 
 * @author Neeraj Yadav
 */
@Service
public class SubscriptionsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionsService.class);
    
    private final DSLContext dsl;
    private final SubscriptionsDao subscriptionsDao;
    private final SubscriptionItemsDao subscriptionItemsDao;
    private final CustomersDao customersDao;
    private final PlansDao plansDao;
    private final ScheduledTaskService scheduledTaskService;
    private final ObjectMapper objectMapper;
    private final SubscriptionHistoryService subscriptionHistoryService;
    private final PlanValidationService planValidationService;
    
    public SubscriptionsService(DSLContext dsl, SubscriptionsDao subscriptionsDao,
                               SubscriptionItemsDao subscriptionItemsDao,
                               CustomersDao customersDao, PlansDao plansDao,
                               ScheduledTaskService scheduledTaskService,
                               ObjectMapper objectMapper,
                               SubscriptionHistoryService subscriptionHistoryService,
                               PlanValidationService planValidationService) {
        this.dsl = dsl;
        this.subscriptionsDao = subscriptionsDao;
        this.subscriptionItemsDao = subscriptionItemsDao;
        this.customersDao = customersDao;
        this.plansDao = plansDao;
        this.scheduledTaskService = scheduledTaskService;
        this.objectMapper = objectMapper;
        this.subscriptionHistoryService = subscriptionHistoryService;
        this.planValidationService = planValidationService;
    }
    
    /**
     * Create a new subscription for the current tenant.
     * Supports both simple SaaS subscriptions and ecommerce subscriptions with products.
     * Validates plan exists, upserts customer, and creates subscription with proper scheduling.
     * 
     * @param request the subscription creation request (with optional products array)
     * @return the created subscription
     */
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        boolean isEcommerce = request.hasProducts();
        
        logger.info("Creating {} subscription for plan {} and customer {} (tenant: {})", 
                   isEcommerce ? "ecommerce" : "simple", 
                   request.getPlanId(), request.getCustomerEmail(), tenantId);
        
        // 1. Validate plan exists and is active
        Plans plan = validateAndGetPlan(request.getPlanId(), tenantId);
        
        // 2. Validate products if this is an ecommerce subscription
        if (isEcommerce) {
            validateProductsAgainstPlan(request.getProducts(), plan, tenantId);
        }
        
        // 3. Upsert customer
        Customers customer = upsertCustomer(request, tenantId);
        
        // 3. Calculate subscription periods
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startDate = request.getStartDate() != null ? request.getStartDate() : now;
        OffsetDateTime trialStart = request.getTrialStart();
        OffsetDateTime trialEnd = request.getTrialEnd();
        
        // Calculate current period and next renewal
        OffsetDateTime currentPeriodStart = startDate;
        OffsetDateTime currentPeriodEnd = calculatePeriodEnd(currentPeriodStart, plan.getBillingInterval());
        OffsetDateTime nextRenewalAt = trialEnd != null ? trialEnd : currentPeriodEnd;
        
        // 4. Create subscription
        Subscriptions subscription = new Subscriptions();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(tenantId);
        subscription.setCustomerId(customer.getId());
        subscription.setPlanId(plan.getId());
        subscription.setStatus(trialStart != null ? "TRIALING" : "ACTIVE");
        subscription.setCurrentPeriodStart(currentPeriodStart);
        subscription.setCurrentPeriodEnd(currentPeriodEnd);
        subscription.setNextRenewalAt(nextRenewalAt);
        subscription.setPaymentMethodRef(request.getPaymentMethodRef());
        subscription.setCancelAtPeriodEnd(false);
        subscription.setTrialStart(trialStart);
        subscription.setTrialEnd(trialEnd);
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        
        // Store plan snapshot (immutable reference to plan details at subscription creation)
        try {
            Map<String, Object> planSnapshot;
            if (isEcommerce) {
                planSnapshot = Map.of(
                    "subscriptionType", "ECOMMERCE",
                    "planId", plan.getId().toString(),
                    "planName", plan.getName(),
                    "productCount", request.getProducts().size(),
                    "billingInterval", plan.getBillingInterval(),
                    "billingIntervalCount", plan.getBillingIntervalCount(),
                    "snapshotAt", now.toString()
                );
            } else {
                planSnapshot = Map.of(
                    "subscriptionType", "SIMPLE",
                    "planId", plan.getId().toString(),
                    "planName", plan.getName(),
                    "basePriceCents", plan.getBasePriceCents(),
                    "currency", plan.getCurrency(),
                    "billingInterval", plan.getBillingInterval(),
                    "billingIntervalCount", plan.getBillingIntervalCount(),
                    "planType", plan.getPlanType(),
                    "trialPeriodDays", plan.getTrialPeriodDays() != null ? plan.getTrialPeriodDays() : 0,
                    "snapshotAt", now.toString()
                );
            }
            subscription.setPlanSnapshot(JSONB.valueOf(objectMapper.writeValueAsString(planSnapshot)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create plan snapshot", e);
        }
        
        // Store shipping address if provided (for ecommerce subscriptions)
        if (request.getShippingAddress() != null) {
            try {
                subscription.setShippingAddress(JSONB.valueOf(objectMapper.writeValueAsString(request.getShippingAddress())));
            } catch (Exception e) {
                throw new RuntimeException("Failed to store shipping address", e);
            }
        }
        
        // Insert subscription
        subscriptionsDao.insert(subscription);
        
        // Get current user ID from security context
        UUID performedBy = com.subscriptionengine.auth.UserContext.getUserId();
        String performedByType = determinePerformedByType();
        
        // Record subscription creation in history
        subscriptionHistoryService.recordCreation(tenantId, subscription.getId(), performedBy, performedByType);
        
        // 5. Create subscription items for ecommerce products
        if (isEcommerce) {
            createSubscriptionItems(subscription.getId(), request.getProducts(), tenantId, now);
            subscriptionHistoryService.recordProductsUpdate(tenantId, subscription.getId(), performedBy, performedByType, request.getProducts().size());
        }
        
        // 6. Schedule renewal and trial end tasks
        if (trialEnd != null) {
            scheduledTaskService.scheduleTrialEnd(subscription.getId(), trialEnd);
        }
        
        if (isEcommerce) {
            // For ecommerce, schedule individual product renewals
            for (ProductItem product : request.getProducts()) {
                OffsetDateTime productRenewalAt = calculatePeriodEnd(currentPeriodStart, plan.getBillingInterval());
                scheduledTaskService.scheduleProductRenewal(subscription.getId(), product.getProductId(), 
                    plan.getId(), productRenewalAt);
            }
        } else {
            // For simple subscriptions, schedule single renewal
            scheduledTaskService.scheduleSubscriptionRenewal(subscription.getId(), nextRenewalAt);
        }
        
        logger.info("Successfully created subscription {} for customer {} (tenant: {})", 
                   subscription.getId(), customer.getId(), tenantId);
        
        return mapToResponse(subscription, customer, plan);
    }
    
    /**
     * Get a subscription by ID for the current tenant.
     * 
     * @param subscriptionId the subscription ID
     * @return the subscription if found and belongs to current tenant
     */
    @Transactional(readOnly = true)
    public Optional<SubscriptionResponse> getSubscription(UUID subscriptionId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        var result = dsl.select()
                .from(SUBSCRIPTIONS)
                .join(CUSTOMERS).on(SUBSCRIPTIONS.CUSTOMER_ID.eq(CUSTOMERS.ID))
                .join(PLANS).on(SUBSCRIPTIONS.PLAN_ID.eq(PLANS.ID))
                .where(SUBSCRIPTIONS.ID.eq(subscriptionId))
                .and(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .fetchOne();
        
        if (result != null) {
            Subscriptions subscription = result.into(SUBSCRIPTIONS).into(Subscriptions.class);
            Customers customer = result.into(CUSTOMERS).into(Customers.class);
            Plans plan = result.into(PLANS).into(Plans.class);
            
            logger.debug("Found subscription {} for tenant {}", subscriptionId, tenantId);
            return Optional.of(mapToResponse(subscription, customer, plan));
        } else {
            logger.debug("Subscription {} not found for tenant {}", subscriptionId, tenantId);
            return Optional.empty();
        }
    }
    
    /**
     * Get all subscriptions for the current tenant with pagination.
     * 
     * @param pageable pagination parameters
     * @return paginated list of subscriptions
     */
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getSubscriptions(Pageable pageable) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        // Get total count
        int totalCount = dsl.selectCount()
                .from(SUBSCRIPTIONS)
                .where(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .fetchOne(0, int.class);
        
        // Get paginated results with joins
        var results = dsl.select()
                .from(SUBSCRIPTIONS)
                .join(CUSTOMERS).on(SUBSCRIPTIONS.CUSTOMER_ID.eq(CUSTOMERS.ID))
                .join(PLANS).on(SUBSCRIPTIONS.PLAN_ID.eq(PLANS.ID))
                .where(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
                .orderBy(SUBSCRIPTIONS.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetch();
        
        List<SubscriptionResponse> subscriptionResponses = results.stream()
                .map(result -> {
                    Subscriptions subscription = result.into(SUBSCRIPTIONS).into(Subscriptions.class);
                    Customers customer = result.into(CUSTOMERS).into(Customers.class);
                    Plans plan = result.into(PLANS).into(Plans.class);
                    return mapToResponse(subscription, customer, plan);
                })
                .collect(Collectors.toList());
        
        logger.debug("Retrieved {} subscriptions for tenant {} (page {}, size {})", 
                    subscriptionResponses.size(), tenantId, pageable.getPageNumber(), pageable.getPageSize());
        
        return new PageImpl<>(subscriptionResponses, pageable, totalCount);
    }
    
    /**
     * Get all subscriptions for a specific customer.
     * 
     * @param customerId the customer ID
     * @param limit maximum number of subscriptions to return
     * @return list of subscription summary maps
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCustomerSubscriptions(UUID customerId, int limit) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        var results = dsl.select(
                SUBSCRIPTIONS.ID,
                SUBSCRIPTIONS.STATUS,
                SUBSCRIPTIONS.CURRENT_PERIOD_START,
                SUBSCRIPTIONS.CURRENT_PERIOD_END,
                SUBSCRIPTIONS.NEXT_RENEWAL_AT,
                SUBSCRIPTIONS.CANCEL_AT_PERIOD_END,
                SUBSCRIPTIONS.CANCELED_AT,
                SUBSCRIPTIONS.CREATED_AT,
                PLANS.ID.as("planId"),
                PLANS.NAME.as("planName"),
                PLANS.BASE_PRICE_CENTS.as("planBasePriceCents"),
                PLANS.CURRENCY.as("planCurrency"),
                PLANS.BILLING_INTERVAL.as("planBillingInterval")
            )
            .from(SUBSCRIPTIONS)
            .join(PLANS).on(SUBSCRIPTIONS.PLAN_ID.eq(PLANS.ID))
            .where(SUBSCRIPTIONS.TENANT_ID.eq(tenantId))
            .and(SUBSCRIPTIONS.CUSTOMER_ID.eq(customerId))
            .orderBy(SUBSCRIPTIONS.CREATED_AT.desc())
            .limit(limit)
            .fetch();
        
        List<Map<String, Object>> subscriptions = results.stream()
            .map(record -> {
                Map<String, Object> subscription = new HashMap<>();
                subscription.put("subscriptionId", record.get(SUBSCRIPTIONS.ID).toString());
                subscription.put("status", record.get(SUBSCRIPTIONS.STATUS));
                subscription.put("currentPeriodStart", record.get(SUBSCRIPTIONS.CURRENT_PERIOD_START));
                subscription.put("currentPeriodEnd", record.get(SUBSCRIPTIONS.CURRENT_PERIOD_END));
                subscription.put("nextRenewalAt", record.get(SUBSCRIPTIONS.NEXT_RENEWAL_AT));
                subscription.put("cancelAtPeriodEnd", record.get(SUBSCRIPTIONS.CANCEL_AT_PERIOD_END));
                subscription.put("canceledAt", record.get(SUBSCRIPTIONS.CANCELED_AT));
                subscription.put("createdAt", record.get(SUBSCRIPTIONS.CREATED_AT));
                
                Map<String, Object> plan = new HashMap<>();
                plan.put("planId", record.get("planId").toString());
                plan.put("name", record.get("planName"));
                plan.put("basePriceCents", record.get("planBasePriceCents"));
                plan.put("currency", record.get("planCurrency"));
                plan.put("billingInterval", record.get("planBillingInterval"));
                subscription.put("plan", plan);
                
                return subscription;
            })
            .collect(Collectors.toList());
        
        logger.debug("Retrieved {} subscriptions for customer {} in tenant {}", 
                    subscriptions.size(), customerId, tenantId);
        
        return subscriptions;
    }
    
    /**
     * Validate that a plan exists and is active for the tenant.
     */
    private Plans validateAndGetPlan(UUID planId, UUID tenantId) {
        Plans plan = dsl.selectFrom(PLANS)
                .where(PLANS.ID.eq(planId))
                .and(PLANS.TENANT_ID.eq(tenantId))
                .fetchOneInto(Plans.class);
        
        if (plan == null) {
            throw new IllegalArgumentException("Plan not found: " + planId);
        }
        
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new IllegalArgumentException("Plan is not active: " + planId);
        }
        
        return plan;
    }
    
    /**
     * Upsert customer based on email or external customer ID.
     */
    private Customers upsertCustomer(CreateSubscriptionRequest request, UUID tenantId) {
        Customers existingCustomer = null;
        
        // Try to find existing customer by email or external ID
        if (request.getCustomerEmail() != null) {
            existingCustomer = dsl.selectFrom(CUSTOMERS)
                    .where(CUSTOMERS.EMAIL.eq(request.getCustomerEmail()))
                    .and(CUSTOMERS.TENANT_ID.eq(tenantId))
                    .fetchOneInto(Customers.class);
        } else if (request.getExternalCustomerId() != null) {
            existingCustomer = dsl.selectFrom(CUSTOMERS)
                    .where(CUSTOMERS.EXTERNAL_CUSTOMER_ID.eq(request.getExternalCustomerId()))
                    .and(CUSTOMERS.TENANT_ID.eq(tenantId))
                    .fetchOneInto(Customers.class);
        }
        
        if (existingCustomer != null) {
            // Update existing customer
            existingCustomer.setFirstName(request.getCustomerFirstName());
            existingCustomer.setLastName(request.getCustomerLastName());
            existingCustomer.setPhone(request.getCustomerPhone());
            existingCustomer.setUpdatedAt(OffsetDateTime.now());
            
            customersDao.update(existingCustomer);
            logger.debug("Updated existing customer: {}", existingCustomer.getId());
            return existingCustomer;
        } else {
            // Create new customer
            Customers newCustomer = new Customers();
            newCustomer.setId(UUID.randomUUID());
            newCustomer.setTenantId(tenantId);
            newCustomer.setExternalCustomerId(request.getExternalCustomerId());
            newCustomer.setEmail(request.getCustomerEmail());
            newCustomer.setFirstName(request.getCustomerFirstName());
            newCustomer.setLastName(request.getCustomerLastName());
            newCustomer.setPhone(request.getCustomerPhone());
            newCustomer.setStatus("ACTIVE");
            newCustomer.setCustomerType("REGISTERED");
            newCustomer.setCreatedAt(OffsetDateTime.now());
            newCustomer.setUpdatedAt(OffsetDateTime.now());
            
            customersDao.insert(newCustomer);
            logger.debug("Created new customer: {}", newCustomer.getId());
            return newCustomer;
        }
    }
    
    /**
     * Validate products against plan rules.
     */
    private void validateProductsAgainstPlan(List<ProductItem> products, Plans plan, UUID tenantId) {
        if (products == null || products.isEmpty()) {
            return;
        }
        
        // Validate subscription request against plan rules
        PlanValidationService.ValidationResult validationResult = 
            planValidationService.validateSubscriptionRequest(plan, true, products.size());
        
        if (!validationResult.isValid()) {
            logger.error("Product validation failed: {}", validationResult.getErrorMessage());
            throw new IllegalArgumentException("Product validation failed: " + validationResult.getErrorMessage());
        }
        
        logger.debug("Validated {} products against plan {}", products.size(), plan.getId());
    }
    
    /**
     * Create subscription items for product-based subscriptions.
     */
    private void createSubscriptionItems(UUID subscriptionId, List<ProductItem> products, 
                                        UUID tenantId, OffsetDateTime now) {
        for (ProductItem product : products) {
            SubscriptionItems item = new SubscriptionItems();
            item.setId(UUID.randomUUID());
            item.setSubscriptionId(subscriptionId);
            item.setTenantId(tenantId);
            item.setPlanId(UUID.randomUUID()); // Placeholder - products don't need plans
            item.setQuantity(product.getQuantity());
            item.setUnitPriceCents(product.getUnitPriceCents());
            item.setCurrency(product.getCurrency());
            
            // Store product details in item_config JSONB
            try {
                Map<String, Object> itemConfig = Map.of(
                    "productId", product.getProductId(),
                    "productName", product.getProductName(),
                    "type", "PRODUCT"
                );
                item.setItemConfig(JSONB.valueOf(objectMapper.writeValueAsString(itemConfig)));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create item config", e);
            }
            
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            
            subscriptionItemsDao.insert(item);
            
            logger.debug("Created subscription item for product {} (subscription: {})", 
                        product.getProductId(), subscriptionId);
        }
    }
    
    /**
     * Calculate period end based on billing interval.
     * Expects database format: DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
     */
    private OffsetDateTime calculatePeriodEnd(OffsetDateTime start, String billingInterval) {
        switch (billingInterval.toUpperCase()) {
            case "DAILY":
                return start.plus(1, ChronoUnit.DAYS);
            case "WEEKLY":
                return start.plus(1, ChronoUnit.WEEKS);
            case "MONTHLY":
                return start.plus(1, ChronoUnit.MONTHS);
            case "QUARTERLY":
                return start.plus(3, ChronoUnit.MONTHS);
            case "YEARLY":
                return start.plus(1, ChronoUnit.YEARS);
            default:
                throw new IllegalArgumentException("Unsupported billing interval: " + billingInterval);
        }
    }
    
    /**
     * Map entities to response DTO.
     */
    /**
     * Determine the performed_by_type based on the current user's role.
     */
    private String determinePerformedByType() {
        String role = com.subscriptionengine.auth.UserContext.getUserRole();
        if (role == null) {
            return "SYSTEM";
        }
        
        switch (role) {
            case "CUSTOMER":
                return "CUSTOMER";
            case "SUPER_ADMIN":
            case "TENANT_ADMIN":
            case "STAFF":
                return "ADMIN";
            default:
                return "SYSTEM";
        }
    }
    
    private SubscriptionResponse mapToResponse(Subscriptions subscription, Customers customer, Plans plan) {
        SubscriptionResponse response = new SubscriptionResponse();
        
        // Subscription fields
        response.setId(subscription.getId());
        response.setTenantId(subscription.getTenantId());
        response.setCustomerId(subscription.getCustomerId());
        response.setPlanId(subscription.getPlanId());
        response.setStatus(subscription.getStatus());
        response.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        response.setNextRenewalAt(subscription.getNextRenewalAt());
        response.setPaymentMethodRef(subscription.getPaymentMethodRef());
        response.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());
        response.setCanceledAt(subscription.getCanceledAt());
        response.setCancellationReason(subscription.getCancellationReason());
        response.setTrialStart(subscription.getTrialStart());
        response.setTrialEnd(subscription.getTrialEnd());
        response.setCreatedAt(subscription.getCreatedAt());
        response.setUpdatedAt(subscription.getUpdatedAt());
        
        // Customer fields (denormalized)
        response.setCustomerEmail(customer.getEmail());
        response.setCustomerFirstName(customer.getFirstName());
        response.setCustomerLastName(customer.getLastName());
        
        // Plan fields (denormalized)
        response.setPlanName(plan.getName());
        response.setPlanBasePriceCents(plan.getBasePriceCents());
        response.setPlanCurrency(plan.getCurrency());
        response.setPlanBillingInterval(plan.getBillingInterval());
        
        return response;
    }
}
