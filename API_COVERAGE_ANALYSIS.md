# API Coverage Analysis - FitNesse vs Integration Tests

**Generated:** March 8, 2026  
**Purpose:** Comprehensive analysis of API endpoint coverage across FitNesse tests and Integration tests

---

## Executive Summary

### API Endpoints by Category

#### **ADMIN APIs** (11 controllers, ~60+ endpoints)
1. **Auth** - `/v1/auth` (4 endpoints)
2. **Tenants** - `/v1/admin/tenants` (5 endpoints)
3. **Users** - `/v1/admin/users` (7 endpoints)
4. **User-Tenants** - `/v1/admin/user-tenants` (5 endpoints)
5. **Plans** - `/v1/admin/plans` (6 endpoints)
6. **Subscriptions** - `/v1/admin/subscriptions` (3 endpoints)
7. **Subscription Management** - `/v1/admin/subscriptions/manage` (2 endpoints)
8. **Subscription History** - `/v1/admin/subscriptions/{id}/history` (2 endpoints)
9. **Customers** - `/v1/admin/customers` (1 endpoint)
10. **Deliveries** - `/v1/admin/deliveries` (4 endpoints)
11. **Webhooks** - `/v1/admin/webhooks` (5 endpoints)
12. **API Clients** - `/v1/admin/api-clients` (5 endpoints)
13. **Audit** - `/v1/admin/audit` (2 endpoints)

#### **CUSTOMER APIs** (1 controller, ~7 endpoints)
1. **Customer Self-Service** - `/v1/customers/me` (7 endpoints)

---

## Detailed Endpoint Inventory

### 1. Authentication & Authorization (`/v1/auth`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/auth/login` | POST | User login with email/password | ✅ AuthControllerTest | ✅ All scenarios | **COVERED** |
| `/v1/auth/api-key` | POST | API client authentication | ✅ ApiClientAuthenticationTest | ❌ | **PARTIAL** |
| `/v1/auth/switch-tenant` | POST | Switch user tenant context | ✅ AuthControllerTest | ❌ | **PARTIAL** |
| `/v1/auth/me` | GET | Get current user context | ✅ AuthControllerTest | ❌ | **PARTIAL** |

**Gap Analysis:**
- ❌ FitNesse missing: API key authentication flow
- ❌ FitNesse missing: Tenant switching scenarios
- ❌ FitNesse missing: User context validation

---

### 2. Tenant Management (`/v1/admin/tenants`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/tenants` | POST | Create tenant | ✅ Multiple tests | ✅ NewTenantOnboarding | **COVERED** |
| `/v1/admin/tenants` | GET | List all tenants | ✅ Multiple tests | ❌ | **PARTIAL** |
| `/v1/admin/tenants/{id}` | GET | Get tenant by ID | ✅ Multiple tests | ❌ | **PARTIAL** |
| `/v1/admin/tenants/{id}` | PUT | Update tenant | ❌ | ❌ | **MISSING** |
| `/v1/admin/tenants/{id}` | DELETE | Delete tenant | ❌ | ❌ | **MISSING** |

**Gap Analysis:**
- ❌ FitNesse missing: Tenant listing/pagination
- ❌ FitNesse missing: Tenant retrieval by ID
- ❌ **CRITICAL**: No tests for tenant update
- ❌ **CRITICAL**: No tests for tenant deletion

---

### 3. User Management (`/v1/admin/users`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/users` | POST | Create user | ✅ AdminUsersTest | ✅ BulkUserOperations | **COVERED** |
| `/v1/admin/users` | GET | List users (paginated) | ✅ AdminUsersTest | ❌ | **PARTIAL** |
| `/v1/admin/users/{id}` | GET | Get user by ID | ✅ AdminUsersTest | ❌ | **PARTIAL** |
| `/v1/admin/users/{id}` | PATCH | Update user | ✅ AdminUsersTest | ❌ | **PARTIAL** |
| `/v1/admin/users/{id}/suspend` | POST | Suspend user | ✅ AdminUsersTest | ❌ | **PARTIAL** |
| `/v1/admin/users/{id}/activate` | POST | Activate user | ✅ AdminUsersTest | ❌ | **PARTIAL** |
| `/v1/admin/users/{id}` | DELETE | Delete user (soft) | ✅ AdminUsersTest | ❌ | **PARTIAL** |

**Gap Analysis:**
- ❌ FitNesse missing: User listing/pagination/search
- ❌ FitNesse missing: User retrieval by ID
- ❌ FitNesse missing: User update scenarios
- ❌ FitNesse missing: User suspend/activate workflows
- ❌ FitNesse missing: User deletion scenarios

---

### 4. User-Tenant Assignment (`/v1/admin/user-tenants`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/user-tenants` | POST | Assign user to tenant | ✅ AdminUserTenantsCrudTest | ✅ BulkUserOperations | **COVERED** |
| `/v1/admin/user-tenants/user/{id}` | GET | Get user's tenants | ✅ AdminUserTenantsCrudTest | ❌ | **PARTIAL** |
| `/v1/admin/user-tenants/tenant/{id}` | GET | Get tenant's users | ✅ AdminUserTenantsCrudTest | ✅ BulkUserOperations | **COVERED** |
| `/v1/admin/user-tenants/{id}` | PATCH | Update user role in tenant | ✅ AdminUserTenantsCrudTest | ✅ BulkUserOperations | **COVERED** |
| `/v1/admin/user-tenants/{id}` | DELETE | Remove user from tenant | ✅ AdminUserTenantsCrudTest | ✅ BulkUserOperations | **COVERED** |

**Gap Analysis:**
- ❌ FitNesse missing: Get user's tenant list (multi-tenant user scenario)

---

### 5. Plan Management (`/v1/admin/plans`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/plans` | POST | Create plan | ✅ PlanManagementTest | ✅ RegisteredCustomerSubscriptionFlow | **COVERED** |
| `/v1/admin/plans` | GET | List plans (paginated) | ✅ PlanManagementTest | ❌ | **PARTIAL** |
| `/v1/admin/plans/active` | GET | Get active plans | ✅ PlanManagementTest | ❌ | **PARTIAL** |
| `/v1/admin/plans/{id}` | GET | Get plan by ID | ✅ PlanManagementTest | ❌ | **PARTIAL** |
| `/v1/admin/plans/{id}/status` | PATCH | Update plan status | ✅ PlanManagementTest | ❌ | **PARTIAL** |
| `/v1/admin/plans/{id}/exists` | GET | Check if plan exists | ✅ PlanManagementTest | ❌ | **PARTIAL** |

**Gap Analysis:**
- ❌ FitNesse missing: Plan listing/pagination
- ❌ FitNesse missing: Active plans retrieval
- ❌ FitNesse missing: Plan status updates (activate/deactivate)
- ❌ FitNesse missing: Plan existence validation

---

### 6. Subscription Management (`/v1/admin/subscriptions`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/subscriptions` | POST | Create subscription | ✅ Multiple tests | ✅ RegisteredCustomer/GuestCustomer | **COVERED** |
| `/v1/admin/subscriptions` | GET | List subscriptions (paginated) | ✅ Multiple tests | ❌ | **PARTIAL** |
| `/v1/admin/subscriptions/{id}` | GET | Get subscription by ID | ✅ Multiple tests | ✅ RegisteredCustomerSubscriptionFlow | **COVERED** |

**Gap Analysis:**
- ❌ FitNesse missing: Subscription listing/pagination/filtering

---

### 7. Subscription Lifecycle (`/v1/admin/subscriptions/manage`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/subscriptions/manage/{id}` | GET | Get subscription management details | ✅ SubscriptionLifecycleTest | ❌ | **PARTIAL** |
| `/v1/admin/subscriptions/manage/{id}` | PUT | Update subscription (PAUSE/RESUME/CANCEL/MODIFY) | ✅ Multiple scenario tests | ✅ RegisteredCustomerSubscriptionFlow | **COVERED** |

**Operations Tested:**
- ✅ PAUSE - Integration + FitNesse
- ✅ RESUME - Integration + FitNesse  
- ✅ CANCEL (immediate) - Integration + FitNesse
- ✅ CANCEL (end of period) - Integration only
- ✅ MODIFY (plan change) - Integration + FitNesse

**Gap Analysis:**
- ❌ FitNesse missing: Get management details endpoint
- ❌ FitNesse missing: End-of-period cancellation scenario

---

### 8. Subscription History (`/v1/admin/subscriptions/{id}/history`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/subscriptions/{id}/history` | GET | Get subscription history (paginated) | ✅ AdminSubscriptionHistoryTest | ❌ | **PARTIAL** |
| `/v1/admin/subscriptions/{id}/history/all` | GET | Get all subscription history | ✅ AdminSubscriptionHistoryTest | ❌ | **PARTIAL** |

**Gap Analysis:**
- ❌ **CRITICAL**: No FitNesse tests for subscription audit trail
- ❌ FitNesse missing: History pagination
- ❌ FitNesse missing: Complete history retrieval

---

### 9. Customer Management (`/v1/admin/customers`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/customers` | POST | Create customer | ✅ CustomerManagementTest | ✅ RegisteredCustomerSubscriptionFlow | **COVERED** |

**Gap Analysis:**
- ❌ **CRITICAL**: No GET/UPDATE/DELETE endpoints for customers
- ❌ FitNesse missing: Customer listing
- ❌ FitNesse missing: Customer search
- ❌ FitNesse missing: Customer update

---

### 10. Delivery Management (`/v1/admin/deliveries`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/deliveries` | GET | List deliveries | ✅ DeliveryManagementTest | ❌ | **PARTIAL** |
| `/v1/admin/deliveries/{id}` | GET | Get delivery by ID | ✅ DeliveryManagementTest | ❌ | **PARTIAL** |
| `/v1/admin/deliveries/{id}/can-cancel` | GET | Check if delivery can be canceled | ✅ DeliveryManagementTest | ❌ | **PARTIAL** |
| `/v1/admin/deliveries/{id}/cancel` | POST | Cancel delivery | ✅ Multiple scenario tests | ❌ | **PARTIAL** |

**Gap Analysis:**
- ❌ **CRITICAL**: No FitNesse tests for delivery management
- ❌ FitNesse missing: Delivery listing/filtering
- ❌ FitNesse missing: Delivery cancellation workflows

---

### 11. Webhook Management (`/v1/admin/webhooks`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/webhooks` | POST | Create webhook | ✅ Multiple tests | ❌ | **PARTIAL** |
| `/v1/admin/webhooks` | GET | List webhooks | ✅ Multiple tests | ❌ | **PARTIAL** |
| `/v1/admin/webhooks/{id}` | GET | Get webhook by ID | ✅ Multiple tests | ❌ | **PARTIAL** |
| `/v1/admin/webhooks/{id}` | PATCH | Update webhook | ✅ Multiple tests | ❌ | **PARTIAL** |
| `/v1/admin/webhooks/{id}` | DELETE | Delete webhook | ✅ Multiple tests | ❌ | **PARTIAL** |

**Gap Analysis:**
- ❌ **CRITICAL**: No FitNesse tests for webhook management
- ❌ FitNesse missing: Webhook CRUD operations
- ❌ FitNesse missing: Webhook event filtering

---

### 12. API Client Management (`/v1/admin/api-clients`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/api-clients` | POST | Create API client | ✅ AdminApiClientsCrudTest | ✅ ApiClientLifecycle | **COVERED** |
| `/v1/admin/api-clients` | GET | List API clients | ✅ AdminApiClientsCrudTest | ❌ | **PARTIAL** |
| `/v1/admin/api-clients/{id}` | GET | Get API client by ID | ✅ AdminApiClientsCrudTest | ❌ | **PARTIAL** |
| `/v1/admin/api-clients/{id}` | PATCH | Update API client | ✅ AdminApiClientsCrudTest | ❌ | **PARTIAL** |
| `/v1/admin/api-clients/{id}` | DELETE | Revoke API client | ✅ AdminApiClientsCrudTest | ✅ ApiClientLifecycle | **COVERED** |

**Gap Analysis:**
- ❌ FitNesse missing: API client listing
- ❌ FitNesse missing: API client retrieval
- ❌ FitNesse missing: API client updates

---

### 13. Audit Trail (`/v1/admin/audit`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/admin/audit` | GET | Get audit logs | ✅ AuditControllerTest | ❌ | **PARTIAL** |
| `/v1/admin/audit/search` | GET | Search audit logs | ✅ AuditControllerTest | ❌ | **PARTIAL** |

**Gap Analysis:**
- ❌ **CRITICAL**: No FitNesse tests for audit trail
- ❌ FitNesse missing: Audit log retrieval
- ❌ FitNesse missing: Audit log search/filtering

---

### 14. Customer Self-Service (`/v1/customers/me`)

| Endpoint | Method | Purpose | Integration Test | FitNesse Test | Status |
|----------|--------|---------|------------------|---------------|--------|
| `/v1/customers/me/subscriptions` | GET | Get customer's subscriptions | ✅ CustomerDashboardTest | ❌ | **PARTIAL** |
| `/v1/customers/me/subscriptions/{id}` | GET | Get subscription details | ✅ CustomerDashboardTest | ❌ | **PARTIAL** |
| `/v1/customers/me/subscriptions/{id}/dashboard` | GET | Get subscription dashboard | ✅ CustomerDashboardTest | ❌ | **PARTIAL** |
| `/v1/customers/me/subscriptions` | POST | Create subscription (self-signup) | ✅ CustomerSelfServiceTest | ❌ | **PARTIAL** |
| `/v1/customers/me/subscriptions/{id}` | PATCH | Manage subscription | ✅ CustomerDashboardTest | ❌ | **PARTIAL** |
| `/v1/customers/me/deliveries` | GET | Get customer's deliveries | ✅ CustomerDashboardTest | ❌ | **PARTIAL** |
| `/v1/customers/me/deliveries/{id}` | PATCH | Manage delivery | ✅ CustomerDashboardTest | ❌ | **PARTIAL** |
| `/v1/plans` | GET | View available plans (public) | ✅ CustomerSelfServiceTest | ❌ | **PARTIAL** |

**Gap Analysis:**
- ❌ **CRITICAL**: No FitNesse tests for customer self-service flows
- ❌ FitNesse missing: Customer dashboard
- ❌ FitNesse missing: Customer subscription management
- ❌ FitNesse missing: Customer delivery management
- ❌ FitNesse missing: Plan browsing for self-signup

---

## Coverage Summary

### FitNesse Test Coverage by Scenario

#### **AdminScenarios** (9 scenarios)
1. ✅ **NewTenantOnboarding** - Tenant creation, user creation, plan creation
2. ✅ **CrossTenantDataIsolation** - Tenant isolation validation
3. ✅ **MultiTenantUserManagement** - Multi-tenant user workflows
4. ✅ **ApiClientLifecycle** - API client CRUD
5. ✅ **UserOffboarding** - User removal workflows
6. ✅ **RegisteredCustomerSubscriptionFlow** - Subscription lifecycle (registered customer)
7. ✅ **GuestCustomerSubscriptionFlow** - Subscription creation (guest customer)
8. ✅ **BulkUserOperations** - Bulk user management
9. ⚠️ **SimpleTest** - Basic fixture test (not comprehensive)

#### **CustomerScenarios** (1 scenario)
1. ⚠️ **CreateSubscription** - Uses legacy SubscriptionFixture (needs migration)

### Integration Test Coverage (225 tests)

**Comprehensive coverage across:**
- ✅ All CRUD operations for core entities
- ✅ Subscription lifecycle (15 scenario tests)
- ✅ Delivery management
- ✅ Webhook management
- ✅ Multi-tenant isolation
- ✅ Authorization and JWT validation
- ✅ Customer self-service
- ✅ Audit trail
- ✅ Error handling

---

## Critical Gaps - High Priority

### **ADMIN Persona - Missing in FitNesse**

1. **Subscription History & Audit**
   - ❌ GET `/v1/admin/subscriptions/{id}/history` - Audit trail retrieval
   - ❌ GET `/v1/admin/subscriptions/{id}/history/all` - Complete history
   - **Business Impact:** Compliance, dispute resolution, customer support

2. **Delivery Management**
   - ❌ GET `/v1/admin/deliveries` - List deliveries
   - ❌ GET `/v1/admin/deliveries/{id}` - Get delivery details
   - ❌ POST `/v1/admin/deliveries/{id}/cancel` - Cancel delivery
   - **Business Impact:** Order fulfillment, customer service

3. **Webhook Management**
   - ❌ All webhook CRUD operations
   - **Business Impact:** Integration with external systems, event-driven workflows

4. **Audit Trail**
   - ❌ GET `/v1/admin/audit` - Audit log retrieval
   - ❌ GET `/v1/admin/audit/search` - Audit search
   - **Business Impact:** Compliance, security monitoring

5. **Tenant Management**
   - ❌ PUT `/v1/admin/tenants/{id}` - Update tenant
   - ❌ DELETE `/v1/admin/tenants/{id}` - Delete tenant
   - **Business Impact:** Tenant lifecycle management

6. **User Management**
   - ❌ GET `/v1/admin/users` - List/search users
   - ❌ PATCH `/v1/admin/users/{id}` - Update user
   - ❌ POST `/v1/admin/users/{id}/suspend` - Suspend user
   - ❌ POST `/v1/admin/users/{id}/activate` - Activate user
   - **Business Impact:** User administration, security

7. **Plan Management**
   - ❌ GET `/v1/admin/plans` - List plans
   - ❌ PATCH `/v1/admin/plans/{id}/status` - Activate/deactivate plans
   - **Business Impact:** Product catalog management

### **CUSTOMER Persona - Missing in FitNesse**

1. **Customer Self-Service Dashboard**
   - ❌ GET `/v1/customers/me/subscriptions` - View my subscriptions
   - ❌ GET `/v1/customers/me/subscriptions/{id}/dashboard` - Subscription dashboard
   - ❌ PATCH `/v1/customers/me/subscriptions/{id}` - Manage subscription
   - **Business Impact:** Customer self-service, reduced support load

2. **Customer Delivery Management**
   - ❌ GET `/v1/customers/me/deliveries` - View my deliveries
   - ❌ PATCH `/v1/customers/me/deliveries/{id}` - Manage delivery
   - **Business Impact:** Customer control over deliveries

3. **Plan Browsing & Self-Signup**
   - ❌ GET `/v1/plans` - Browse available plans
   - ❌ POST `/v1/customers/me/subscriptions` - Self-signup
   - **Business Impact:** Customer acquisition, self-service onboarding

---

## Recommendations

### Phase 1: Critical Admin Scenarios (High Priority)

1. **SubscriptionHistoryAudit** - Comprehensive audit trail testing
   - Verify history tracking for all subscription changes
   - Test pagination and filtering
   - Validate audit data completeness

2. **DeliveryManagement** - Delivery lifecycle testing
   - List deliveries with filtering
   - Cancel deliveries (with business rules)
   - Verify delivery status transitions

3. **WebhookManagement** - Webhook CRUD and event testing
   - Create/update/delete webhooks
   - Test event filtering
   - Verify webhook delivery (DB-level, as HTTP not implemented)

### Phase 2: Customer Self-Service Scenarios (High Priority)

4. **CustomerDashboard** - Customer self-service testing
   - View subscriptions
   - View deliveries
   - Manage subscription (pause/resume/cancel)

5. **CustomerSelfSignup** - Self-service onboarding
   - Browse plans
   - Create subscription
   - Payment method setup

### Phase 3: Admin Management Scenarios (Medium Priority)

6. **TenantLifecycle** - Complete tenant management
   - Update tenant details
   - Tenant deletion (with cascading rules)

7. **UserAdministration** - Complete user management
   - List/search users
   - Update user details
   - Suspend/activate users

8. **PlanCatalogManagement** - Plan administration
   - List/filter plans
   - Activate/deactivate plans
   - Plan versioning

### Phase 4: Advanced Scenarios (Lower Priority)

9. **AuditTrailValidation** - Audit log testing
   - Retrieve audit logs
   - Search/filter audit logs
   - Verify audit completeness

10. **MultiTenantAdvanced** - Advanced multi-tenant scenarios
    - Tenant switching
    - Cross-tenant access denial
    - Tenant-specific configurations

---

## Test Organization Recommendations

### Proposed FitNesse Structure

```
FitNesseRoot/
├── AdminScenarios/
│   ├── TenantManagement/
│   │   ├── TenantLifecycle (NEW)
│   │   └── CrossTenantDataIsolation (EXISTS)
│   ├── UserManagement/
│   │   ├── UserAdministration (NEW)
│   │   ├── BulkUserOperations (EXISTS)
│   │   └── UserOffboarding (EXISTS)
│   ├── SubscriptionManagement/
│   │   ├── RegisteredCustomerSubscriptionFlow (EXISTS)
│   │   ├── GuestCustomerSubscriptionFlow (EXISTS)
│   │   ├── SubscriptionHistoryAudit (NEW - HIGH PRIORITY)
│   │   └── PlanCatalogManagement (NEW)
│   ├── DeliveryManagement/
│   │   └── DeliveryLifecycle (NEW - HIGH PRIORITY)
│   ├── IntegrationManagement/
│   │   ├── WebhookManagement (NEW - HIGH PRIORITY)
│   │   └── ApiClientLifecycle (EXISTS)
│   └── ComplianceAndAudit/
│       └── AuditTrailValidation (NEW)
│
└── CustomerScenarios/
    ├── SelfService/
    │   ├── CustomerDashboard (NEW - HIGH PRIORITY)
    │   ├── CustomerSelfSignup (NEW - HIGH PRIORITY)
    │   └── SubscriptionManagement (NEW)
    └── DeliveryManagement/
        └── CustomerDeliveryControl (NEW)
```

---

## Next Steps

1. **Review this analysis** with stakeholders
2. **Prioritize scenarios** based on business value
3. **Create FitNesse tests** for Phase 1 (Critical Admin Scenarios)
4. **Create FitNesse tests** for Phase 2 (Customer Self-Service)
5. **Migrate legacy tests** (CreateSubscription) to RestApiFixture
6. **Validate coverage** against production use cases

---

## Appendix: Integration Test Files

**Total: 26 test files, 225 tests**

- AdminApiClientsCrudTest.java
- AdminSubscriptionHistoryTest.java
- AdminUserTenantsCrudTest.java
- AdminUsersTest.java
- ApiClientAuthenticationTest.java
- AuditControllerTest.java
- AuthControllerTest.java
- AuthorizationTest.java
- CrossFeatureIntegrationTest.java
- CustomerDashboardTest.java
- CustomerManagementTest.java
- CustomerSelfServiceTest.java
- DeliveryManagementTest.java
- JwtClaimsValidationTest.java
- MultiTenantIsolationTest.java
- PlanCategoryValidationTest.java
- PlanManagementTest.java
- SecurityAndErrorHandlingTest.java
- SubscriptionHistoryTest.java
- SubscriptionLifecycleTest.java
- SubscriptionModificationTest.java
- WebhookDeliveryTest.java
- Plus 15 scenario tests (e.g., AddressChangeScenarioTest, BulkDeliveryCancellationScenarioTest, etc.)

---

**End of Analysis**
