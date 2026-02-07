package com.subscriptionengine.subscriptions.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.CustomersDao;
import com.subscriptionengine.generated.tables.daos.PlansDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionsDao;
import com.subscriptionengine.generated.tables.daos.SubscriptionItemsDao;
import com.subscriptionengine.generated.tables.pojos.Customers;
import com.subscriptionengine.generated.tables.pojos.Plans;
import com.subscriptionengine.generated.tables.pojos.Subscriptions;
import com.subscriptionengine.generated.tables.pojos.SubscriptionItems;
import com.subscriptionengine.subscriptions.dto.CreateEcommerceSubscriptionRequest;
import com.subscriptionengine.subscriptions.dto.ProductItem;
import com.subscriptionengine.subscriptions.dto.SubscriptionResponse;
import com.subscriptionengine.scheduler.service.ScheduledTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.stream.Collectors;

import static com.subscriptionengine.generated.tables.Customers.CUSTOMERS;

/**
 * Service for managing ecommerce subscriptions with direct products.
 * Simplified approach without confusing "product plans".
 * 
 * @author Neeraj Yadav
 */
@Service
public class EcommerceSubscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(EcommerceSubscriptionService.class);
    
    private final DSLContext dsl;
    private final SubscriptionsDao subscriptionsDao;
    private final SubscriptionItemsDao subscriptionItemsDao;
    private final CustomersDao customersDao;
    private final PlansDao plansDao;
    private final ScheduledTaskService scheduledTaskService;
    private final ObjectMapper objectMapper;
    
    public EcommerceSubscriptionService(DSLContext dsl, SubscriptionsDao subscriptionsDao,
                                       SubscriptionItemsDao subscriptionItemsDao,
                                       CustomersDao customersDao, PlansDao plansDao,
                                       ScheduledTaskService scheduledTaskService,
                                       ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.subscriptionsDao = subscriptionsDao;
        this.subscriptionItemsDao = subscriptionItemsDao;
        this.customersDao = customersDao;
        this.plansDao = plansDao;
        this.scheduledTaskService = scheduledTaskService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Create ecommerce subscription with direct products.
     */
    @Transactional
    public SubscriptionResponse createEcommerceSubscription(CreateEcommerceSubscriptionRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        OffsetDateTime now = OffsetDateTime.now();
        
        logger.info("Creating ecommerce subscription for {} products (tenant: {})", 
                   request.getProducts().size(), tenantId);
        
        // 1. Validate plans for each product
        Map<UUID, Plans> plans = validatePlans(request.getProducts(), tenantId);
        
        // 2. Upsert customer
        Customers customer = upsertCustomer(request, tenantId, now);
        
        // 3. Create main subscription (container for multiple product plans)
        OffsetDateTime startDate = request.getStartDate() != null ? request.getStartDate() : now;
        OffsetDateTime trialStart = request.getTrialStart();
        OffsetDateTime trialEnd = request.getTrialEnd();
        
        OffsetDateTime currentPeriodStart = trialStart != null ? trialStart : startDate;
        
        // 4. Create main subscription
        Subscriptions subscription = new Subscriptions();
        subscription.setId(UUID.randomUUID());
        subscription.setTenantId(tenantId);
        subscription.setCustomerId(customer.getId());
        subscription.setPlanId(request.getBasePlanId()); // Use base plan for subscription-level billing
        subscription.setStatus("ACTIVE");
        subscription.setCurrentPeriodStart(currentPeriodStart);
        subscription.setCurrentPeriodEnd(currentPeriodStart.plusMonths(1)); // Default period
        subscription.setNextRenewalAt(null); // Will be set per product
        subscription.setPaymentMethodRef(request.getPaymentMethodRef());
        subscription.setCancelAtPeriodEnd(false);
        subscription.setTrialStart(trialStart);
        subscription.setTrialEnd(trialEnd);
        
        // Store subscription metadata (no single plan snapshot since each product has its own plan)
        try {
            Map<String, Object> subscriptionMeta = Map.of(
                "subscriptionType", "MULTI_PRODUCT",
                "productCount", request.getProducts().size(),
                "createdAt", now.toString()
            );
            subscription.setPlanSnapshot(JSONB.valueOf(objectMapper.writeValueAsString(subscriptionMeta)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create subscription metadata", e);
        }
        
        // Store shipping address if provided
        if (request.getShippingAddress() != null) {
            try {
                subscription.setShippingAddress(JSONB.valueOf(objectMapper.writeValueAsString(request.getShippingAddress())));
            } catch (Exception e) {
                throw new RuntimeException("Failed to store shipping address", e);
            }
        }
        
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        
        subscriptionsDao.insert(subscription);
        
        // 5. Create subscription items for each product and schedule individual renewal tasks
        for (ProductItem product : request.getProducts()) {
            Plans productPlan = plans.get(product.getPlanId());
            createSubscriptionItem(subscription.getId(), product, productPlan, tenantId, now);
            
            // Schedule individual renewal task for this product based on its plan
            OffsetDateTime productRenewalAt = calculatePeriodEnd(currentPeriodStart, 
                productPlan.getBillingInterval(), productPlan.getBillingIntervalCount());
            scheduledTaskService.scheduleProductRenewal(subscription.getId(), product.getProductId(), 
                product.getPlanId(), productRenewalAt);
        }
        
        // 6. Schedule trial end task if applicable
        if (trialEnd != null) {
            scheduledTaskService.scheduleTrialEnd(subscription.getId(), trialEnd);
        }
        
        logger.info("Successfully created ecommerce subscription {} with {} products (tenant: {})", 
                   subscription.getId(), request.getProducts().size(), tenantId);
        
        return mapToEcommerceResponse(subscription, customer, plans, request.getProducts());
    }
    
    private Map<UUID, Plans> validatePlans(List<ProductItem> products, UUID tenantId) {
        // Get unique plan IDs
        List<UUID> planIds = products.stream()
                .map(ProductItem::getPlanId)
                .distinct()
                .collect(Collectors.toList());
        
        // Fetch all plans in one query
        List<Plans> plans = plansDao.fetchById(planIds.toArray(new UUID[0]));
        Map<UUID, Plans> planMap = new HashMap<>();
        
        for (Plans plan : plans) {
            if (!plan.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Plan not found: " + plan.getId());
            }
            if (!"ACTIVE".equals(plan.getStatus())) {
                throw new IllegalArgumentException("Plan is not active: " + plan.getId());
            }
            planMap.put(plan.getId(), plan);
        }
        
        // Verify all requested plans were found
        for (UUID planId : planIds) {
            if (!planMap.containsKey(planId)) {
                throw new IllegalArgumentException("Plan not found: " + planId);
            }
        }
        
        return planMap;
    }
    
    private Customers upsertCustomer(CreateEcommerceSubscriptionRequest request, UUID tenantId, OffsetDateTime now) {
        // Try to find existing customer by email
        Customers existing = dsl.selectFrom(CUSTOMERS)
                .where(CUSTOMERS.TENANT_ID.eq(tenantId))
                .and(CUSTOMERS.EMAIL.eq(request.getCustomerEmail()))
                .fetchOneInto(Customers.class);
        
        if (existing != null) {
            // Update existing customer
            existing.setFirstName(request.getCustomerFirstName());
            existing.setLastName(request.getCustomerLastName());
            existing.setPhone(request.getCustomerPhone());
            existing.setUpdatedAt(now);
            customersDao.update(existing);
            return existing;
        } else {
            // Create new customer
            Customers customer = new Customers();
            customer.setId(UUID.randomUUID());
            customer.setTenantId(tenantId);
            customer.setEmail(request.getCustomerEmail());
            customer.setFirstName(request.getCustomerFirstName());
            customer.setLastName(request.getCustomerLastName());
            customer.setPhone(request.getCustomerPhone());
            customer.setExternalCustomerId(request.getExternalCustomerId());
            customer.setStatus("ACTIVE");
            customer.setCreatedAt(now);
            customer.setUpdatedAt(now);
            customersDao.insert(customer);
            return customer;
        }
    }
    
    private void createSubscriptionItem(UUID subscriptionId, ProductItem product, Plans productPlan, UUID tenantId, OffsetDateTime now) {
        SubscriptionItems item = new SubscriptionItems();
        item.setId(UUID.randomUUID());
        item.setTenantId(tenantId);
        item.setSubscriptionId(subscriptionId);
        
        // Store product plan reference
        item.setPlanId(product.getPlanId()); // Reference to the product's chosen plan
        item.setQuantity(product.getQuantity());
        item.setUnitPriceCents(product.getUnitPriceCents());
        item.setCurrency(product.getCurrency());
        
        // Store product details in item_config
        try {
            Map<String, Object> itemConfig = Map.of(
                "productId", product.getProductId(),
                "productName", product.getProductName(),
                "description", product.getDescription() != null ? product.getDescription() : "",
                "imageUrl", product.getImageUrl() != null ? product.getImageUrl() : "",
                "category", product.getCategory() != null ? product.getCategory() : ""
            );
            item.setItemConfig(JSONB.valueOf(objectMapper.writeValueAsString(itemConfig)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to store product details", e);
        }
        
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        
        subscriptionItemsDao.insert(item);
        
        logger.debug("Created subscription item for product {} (quantity: {}, price: {})", 
                    product.getProductId(), product.getQuantity(), product.getUnitPriceCents());
    }
    
    private OffsetDateTime calculatePeriodEnd(OffsetDateTime start, String interval, Integer count) {
        int intervalCount = count != null ? count : 1;
        
        return switch (interval.toUpperCase()) {
            case "DAILY" -> start.plusDays(intervalCount);
            case "WEEKLY" -> start.plusWeeks(intervalCount);
            case "MONTHLY" -> start.plusMonths(intervalCount);
            case "QUARTERLY" -> start.plusMonths(3L * intervalCount);
            case "YEARLY" -> start.plusYears(intervalCount);
            // Support legacy lowercase formats as well
            case "DAY" -> start.plusDays(intervalCount);
            case "WEEK" -> start.plusWeeks(intervalCount);
            case "MONTH" -> start.plusMonths(intervalCount);
            case "QUARTER" -> start.plusMonths(3L * intervalCount);
            case "YEAR" -> start.plusYears(intervalCount);
            default -> throw new IllegalArgumentException("Unsupported billing interval: " + interval);
        };
    }
    
    private SubscriptionResponse mapToEcommerceResponse(Subscriptions subscription, Customers customer, 
                                                       Map<UUID, Plans> plans, List<ProductItem> products) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(subscription.getId());
        response.setStatus(subscription.getStatus());
        response.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        response.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        response.setNextRenewalAt(subscription.getNextRenewalAt());
        response.setTrialStart(subscription.getTrialStart());
        response.setTrialEnd(subscription.getTrialEnd());
        response.setCreatedAt(subscription.getCreatedAt());
        response.setUpdatedAt(subscription.getUpdatedAt());
        
        // Customer info
        response.setCustomerId(customer.getId());
        response.setCustomerEmail(customer.getEmail());
        response.setCustomerFirstName(customer.getFirstName());
        response.setCustomerLastName(customer.getLastName());
        
        // Determine billing interval based on products
        String billingInterval = determineBillingInterval(plans, products);
        response.setPlanBillingInterval(billingInterval);
        
        // Set plan info based on whether single or multi-product
        if (products.size() == 1) {
            Plans singlePlan = plans.get(products.get(0).getPlanId());
            response.setPlanId(singlePlan.getId());
            response.setPlanName(singlePlan.getName());
        } else {
            response.setPlanId(null); // No single plan - multiple product plans
            response.setPlanName("Multi-Product Subscription");
        }
        
        // Calculate total amount from products
        Long totalAmountCents = products.stream()
                .mapToLong(ProductItem::getTotalPriceCents)
                .sum();
        response.setPlanBasePriceCents(totalAmountCents);
        response.setPlanCurrency(products.isEmpty() ? "USD" : products.get(0).getCurrency());
        
        return response;
    }
    
    /**
     * Determine the billing interval for the subscription based on product plans.
     * Returns the actual interval if all products have the same interval, otherwise "MIXED".
     */
    private String determineBillingInterval(Map<UUID, Plans> plans, List<ProductItem> products) {
        if (products.isEmpty()) {
            return "UNKNOWN";
        }
        
        // Get the billing interval of the first product
        String firstInterval = plans.get(products.get(0).getPlanId()).getBillingInterval();
        
        // Check if all products have the same billing interval
        boolean allSameInterval = products.stream()
                .map(ProductItem::getPlanId)
                .map(plans::get)
                .map(Plans::getBillingInterval)
                .allMatch(interval -> interval.equals(firstInterval));
        
        return allSameInterval ? firstInterval : "MIXED";
    }
}
