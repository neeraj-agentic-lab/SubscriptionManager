package com.subscriptionengine.subscriptions.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.CustomersDao;
import com.subscriptionengine.generated.tables.daos.PlansDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionsDao;
import com.subscriptionengine.generated.tables.pojos.Customers;
import com.subscriptionengine.generated.tables.pojos.Plans;
import com.subscriptionengine.generated.tables.pojos.Subscriptions;
import com.subscriptionengine.subscriptions.dto.CreateSubscriptionRequest;
import com.subscriptionengine.subscriptions.dto.SubscriptionResponse;
import com.subscriptionengine.scheduler.service.ScheduledTaskService;
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
    private final CustomersDao customersDao;
    private final PlansDao plansDao;
    private final ScheduledTaskService scheduledTaskService;
    private final ObjectMapper objectMapper;
    
    public SubscriptionsService(DSLContext dsl, SubscriptionsDao subscriptionsDao, 
                               CustomersDao customersDao, PlansDao plansDao,
                               ScheduledTaskService scheduledTaskService,
                               ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.subscriptionsDao = subscriptionsDao;
        this.customersDao = customersDao;
        this.plansDao = plansDao;
        this.scheduledTaskService = scheduledTaskService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create a new subscription for the current tenant.
     * Validates plan exists, upserts customer, and creates subscription with proper scheduling.
     * 
     * @param request the subscription creation request
     * @return the created subscription
     */
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("Creating subscription for plan {} and customer {} (tenant: {})", 
                   request.getPlanId(), request.getCustomerEmail(), tenantId);
        
        // 1. Validate plan exists and is active
        Plans plan = validateAndGetPlan(request.getPlanId(), tenantId);
        
        // 2. Upsert customer
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
            Map<String, Object> planSnapshot = Map.of(
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
            subscription.setPlanSnapshot(JSONB.valueOf(objectMapper.writeValueAsString(planSnapshot)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create plan snapshot", e);
        }
        
        // Insert subscription
        subscriptionsDao.insert(subscription);
        
        // 5. Schedule renewal and trial end tasks
        if (trialEnd != null) {
            scheduledTaskService.scheduleTrialEnd(subscription.getId(), trialEnd);
        }
        scheduledTaskService.scheduleSubscriptionRenewal(subscription.getId(), nextRenewalAt);
        
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
