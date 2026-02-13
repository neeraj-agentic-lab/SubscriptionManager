package com.subscriptionengine.plans.service;

import com.subscriptionengine.auth.TenantContext;
import com.subscriptionengine.generated.tables.daos.PlansDao;
import com.subscriptionengine.generated.tables.pojos.Plans;
import com.subscriptionengine.plans.dto.CreatePlanRequest;
import com.subscriptionengine.plans.dto.PlanResponse;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.subscriptionengine.generated.tables.Plans.PLANS;

/**
 * Service for managing subscription plans with tenant isolation.
 * Uses jOOQ generated classes for type-safe database operations.
 * 
 * @author Neeraj Yadav
 */
@Service
public class PlansService {
    
    private static final Logger logger = LoggerFactory.getLogger(PlansService.class);
    
    private final DSLContext dsl;
    private final PlansDao plansDao;
    private final PlanValidationService planValidationService;
    
    public PlansService(DSLContext dsl, PlansDao plansDao, PlanValidationService planValidationService) {
        this.dsl = dsl;
        this.plansDao = plansDao;
        this.planValidationService = planValidationService;
    }
    
    /**
     * Create a new subscription plan for the current tenant.
     * 
     * @param request the plan creation request
     * @return the created plan
     */
    @Transactional
    public PlanResponse createPlan(CreatePlanRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        logger.info("Creating plan '{}' for tenant {}", request.getName(), tenantId);
        
        // Create new plan entity
        Plans plan = new Plans();
        plan.setId(UUID.randomUUID());
        plan.setTenantId(tenantId);
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setPlanType(request.getPlanType());
        plan.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        plan.setBasePriceCents(request.getBasePriceCents());
        plan.setCurrency(request.getCurrency());
        plan.setBillingInterval(request.getBillingInterval());
        plan.setBillingIntervalCount(request.getBillingIntervalCount());
        plan.setTrialPeriodDays(request.getTrialPeriodDays());
        
        // Set plan category and validation flags
        plan.setPlanCategory(request.getPlanCategory() != null ? request.getPlanCategory() : "DIGITAL");
        planValidationService.setDefaultValidationFlags(plan);
        
        // Validate plan configuration
        PlanValidationService.ValidationResult validationResult = planValidationService.validatePlanConfiguration(plan);
        if (!validationResult.isValid()) {
            logger.error("Plan validation failed: {}", validationResult.getErrorMessage());
            throw new IllegalArgumentException("Plan validation failed: " + validationResult.getErrorMessage());
        }
        
        plan.setCreatedAt(OffsetDateTime.now());
        plan.setUpdatedAt(OffsetDateTime.now());
        
        // Insert into database
        plansDao.insert(plan);
        
        logger.info("Successfully created plan {} for tenant {}", plan.getId(), tenantId);
        
        return mapToResponse(plan);
    }
    
    /**
     * Get a plan by ID for the current tenant.
     * 
     * @param planId the plan ID
     * @return the plan if found and belongs to current tenant
     */
    @Transactional(readOnly = true)
    public Optional<PlanResponse> getPlan(UUID planId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        Plans plan = dsl.selectFrom(PLANS)
                .where(PLANS.ID.eq(planId))
                .and(PLANS.TENANT_ID.eq(tenantId))
                .fetchOneInto(Plans.class);
        
        if (plan != null) {
            logger.debug("Found plan {} for tenant {}", planId, tenantId);
            return Optional.of(mapToResponse(plan));
        } else {
            logger.debug("Plan {} not found for tenant {}", planId, tenantId);
            return Optional.empty();
        }
    }
    
    /**
     * Get all plans for the current tenant with pagination.
     * 
     * @param pageable pagination parameters
     * @return paginated list of plans
     */
    @Transactional(readOnly = true)
    public Page<PlanResponse> getPlans(Pageable pageable) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        // Get total count
        int totalCount = dsl.selectCount()
                .from(PLANS)
                .where(PLANS.TENANT_ID.eq(tenantId))
                .fetchOne(0, int.class);
        
        // Get paginated results
        List<Plans> plans = dsl.selectFrom(PLANS)
                .where(PLANS.TENANT_ID.eq(tenantId))
                .orderBy(PLANS.CREATED_AT.desc())
                .limit(pageable.getPageSize())
                .offset((int) pageable.getOffset())
                .fetchInto(Plans.class);
        
        List<PlanResponse> planResponses = plans.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        logger.debug("Retrieved {} plans for tenant {} (page {}, size {})", 
                    plans.size(), tenantId, pageable.getPageNumber(), pageable.getPageSize());
        
        return new PageImpl<>(planResponses, pageable, totalCount);
    }
    
    /**
     * Get all active plans for the current tenant.
     * 
     * @return list of active plans
     */
    @Transactional(readOnly = true)
    public List<PlanResponse> getActivePlans() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        List<Plans> plans = dsl.selectFrom(PLANS)
                .where(PLANS.TENANT_ID.eq(tenantId))
                .and(PLANS.STATUS.eq("ACTIVE"))
                .orderBy(PLANS.CREATED_AT.desc())
                .fetchInto(Plans.class);
        
        logger.debug("Retrieved {} active plans for tenant {}", plans.size(), tenantId);
        
        return plans.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Update a plan's active status.
     * 
     * @param planId the plan ID
     * @param active the new active status
     * @return the updated plan if found and belongs to current tenant
     */
    @Transactional
    public Optional<PlanResponse> updatePlanStatus(UUID planId, boolean active) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        int updatedRows = dsl.update(PLANS)
                .set(PLANS.STATUS, active ? "ACTIVE" : "INACTIVE")
                .set(PLANS.UPDATED_AT, OffsetDateTime.now())
                .where(PLANS.ID.eq(planId))
                .and(PLANS.TENANT_ID.eq(tenantId))
                .execute();
        
        if (updatedRows > 0) {
            logger.info("Updated plan {} status to {} for tenant {}", planId, active, tenantId);
            return getPlan(planId);
        } else {
            logger.warn("Plan {} not found or not owned by tenant {}", planId, tenantId);
            return Optional.empty();
        }
    }
    
    /**
     * Check if a plan exists and belongs to the current tenant.
     * 
     * @param planId the plan ID
     * @return true if plan exists and belongs to current tenant
     */
    @Transactional(readOnly = true)
    public boolean planExists(UUID planId) {
        UUID tenantId = TenantContext.getRequiredTenantId();
        
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(PLANS)
                        .where(PLANS.ID.eq(planId))
                        .and(PLANS.TENANT_ID.eq(tenantId))
        );
    }
    
    /**
     * Map Plans entity to PlanResponse DTO.
     */
    private PlanResponse mapToResponse(Plans plan) {
        PlanResponse response = new PlanResponse(
                plan.getId(),
                plan.getTenantId(),
                plan.getName(),
                plan.getDescription(),
                plan.getPlanType(),
                plan.getStatus(),
                plan.getBasePriceCents(),
                plan.getCurrency(),
                plan.getBillingInterval(),
                plan.getBillingIntervalCount(),
                plan.getTrialPeriodDays(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
        
        // Add validation fields
        response.setPlanCategory(plan.getPlanCategory());
        response.setRequiresProducts(plan.getRequiresProducts());
        response.setAllowsProducts(plan.getAllowsProducts());
        response.setBasePriceRequired(plan.getBasePriceRequired());
        
        // Add audit fields
        response.setCreatedBy(plan.getCreatedBy());
        response.setUpdatedBy(plan.getUpdatedBy());
        
        return response;
    }
}
