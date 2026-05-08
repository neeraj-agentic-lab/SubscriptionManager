# Implementation Plan — Headless Subscription Engine (V1, Postgres-only)

This plan is designed to be **actionable in Cursor IDE**: copy sections into tasks, implement module-by-module, and use the checklists to track progress.

## 🎯 Current Status: M6 Testing & Production Readiness 100% Complete ✅
**Last Updated**: February 3, 2026 (Complete Test Suite: 88 Tests with 100% API & Scenario Coverage)

✅ **COMPLETED M1 - Foundation**:
- Multi-module Gradle project structure with proper Spring Boot configuration
- Complete database schema (11 migration files including delivery cancellation)
- Docker-compose setup for local development (PostgreSQL on port 5440)
- Custom attributes strategy implemented (`custom_attrs` JSONB on all tables)
- All tables designed with proper constraints and indexes
- Flyway migrations successfully executed
- jOOQ code generation complete (POJOs, DAOs, Records generated)
- Gradle build system with no deprecation warnings (Gradle 8.5 + Java 17)

✅ **COMPLETED M2 - Core APIs & Authentication**:
- **Tenant Context & JWT Authentication**: Implemented with fallback JWT extraction in TenantSecurityService
- **Idempotency Middleware**: Complete IdempotencyFilter with tenant-aware caching
- **Plans API**: Full CRUD operations with proper validation and tenant isolation
- **Subscription Creation API**: Multi-product ecommerce subscriptions with plan-based billing
- **Scheduled Task System**: Automatic renewal task creation with proper status handling
- **Database Constraint Fixes**: All validation rules properly enforced
- **Billing Interval Logic**: Dynamic interval detection (single product shows actual interval, mixed products show "MIXED")

✅ **COMPLETED M3 - Worker Runtime & Billing**:
- **Worker Runtime**: Full task processing system with distributed locking and lease management
- **Task Claiming**: Database-driven scheduler with `SELECT FOR UPDATE SKIP LOCKED` pattern
- **Tenant Context for Background Tasks**: Fixed TenantContext to support both web requests and background tasks using ThreadLocal
- **Invoice Generation**: Complete invoice creation with proper database constraints (OPEN status)
- **Payment Processing**: End-to-end payment processing with mock payment adapter (SUCCEEDED status)
- **Database Constraint Fixes**: Fixed invoice status (`OPEN`) and payment status (`SUCCEEDED`) constraints
- **Comprehensive Debugging System**: Production-ready logging with structured tags, timing metrics, and error tracking
- **Task Reaper**: Automatic cleanup of expired task locks
- **Billing Flow**: Complete end-to-end flow from task creation → invoice generation → payment processing
- **Subscription Renewal Date Updates**: `next_renewal_at` field properly updated after successful renewals

✅ **COMPLETED M3+ - Configurable Renewal Scheduling System (100% FUNCTIONAL)**:
- **Database Schema**: Added `job_configuration` and `job_execution_history` tables (V009, V010 migrations)
- **Dynamic Scheduling Service**: Runtime job schedule management with database-driven configuration
- **Job Configuration Service**: Schedule presets (every minute, 5min, 15min, 30min, hourly, daily, weekly) with custom cron support
- **Job Execution History Service**: Complete execution tracking with metrics, statistics, and status breakdown
- **Subscription Renewal Scheduler**: Enhanced with execution history tracking and dynamic scheduling
- **REST API Endpoints**: Full admin API for job management and monitoring - ALL WORKING ✅
- **Module Dependencies**: Fixed all TenantContext import issues across modules
- **jOOQ Generation**: Successfully generated classes for new job tables
- **Authentication**: JWT token generation and API authentication working
- **Web Server**: Worker application configured with web server on port 8081 for admin APIs

✅ **COMPLETED M4 - Deliveries + Orders + Entitlements (100% FUNCTIONAL)**:
- **CREATE_DELIVERY Task Handler**: Complete delivery instance creation with cycle snapshots
- **CREATE_ORDER Task Handler**: Commerce adapter integration with mock order creation
- **ENTITLEMENT_GRANT Task Handler**: Digital product access management with mock entitlement adapter
- **Payment Integration**: PaymentProcessingService schedules delivery and entitlement tasks after successful payments
- **Task Scheduling Enhancement**: Added `scheduleDeliveryCreation()`, `scheduleOrderCreation()`, and `scheduleEntitlementGrant()` methods
- **End-to-End Flow Validation**: Complete payment → delivery → order → entitlement flow working with database evidence
- **Delivery Management Service**: Customer delivery viewing and cancellation functionality
- **Delivery Cancellation API**: REST endpoints for customers to cancel upcoming deliveries
- **Cancellation Validation**: Prevents cancellation of deliveries already in order processing
- **Database Migration**: Added cancellation_reason and cancelled_at fields to delivery_instances table
- **Domain-Delivery Module**: Complete module with service layer and API controller
- **Customer Account Views**: Upcoming deliveries endpoint with cancellation eligibility

🎉 **DELIVERY & ENTITLEMENT SYSTEM 100% FUNCTIONAL**:
- Complete end-to-end billing flow: subscription → invoice → payment → delivery → order → entitlement
- Database evidence of working system: delivery instances and entitlements created successfully
- Mock adapters functional for commerce and entitlement platforms
- Customer delivery management with cancellation capabilities
- Proper status validation and task scheduling integration
- Production-ready logging and error handling throughout

✅ **DELIVERY CANCELLATION SYSTEM COMPLETE & TESTED**:
- ✅ Customer can view upcoming deliveries with cancellation eligibility
- ✅ API endpoint to cancel specific deliveries before order processing starts
- ✅ Validation prevents cancellation of deliveries already in processing
- ✅ Automatic cancellation of pending CREATE_ORDER tasks when delivery is cancelled
- ✅ Database fields for tracking cancellation reason and timestamp (migration ready)
- ✅ Comprehensive error handling and logging for cancellation flow
- ✅ **TESTED**: Both subscription-api (port 8080) and subscription-worker (port 8081) running
- ✅ **TESTED**: Test data created with PENDING (cancellable) and PROCESSING (non-cancellable) deliveries
- ✅ **TESTED**: All delivery management endpoints functional

✅ **COMPLETED M5 - Subscription Management (100% COMPLETE)**:
- **Subscription Pause/Resume**: Complete implementation with proper status management
- **Subscription Cancellation**: Immediate cancellation and end-of-period flagging supported
- **Subscription Modification**: Plan change, quantity change, payment method, shipping address updates
- **Customer Dashboard Endpoints**: App-friendly subscription list and dashboard views
- **Unified Subscription Management API**: Clean REST conventions using single endpoint
- **SubscriptionManagementService**: Complete service layer with pause/resume/cancel/modify logic
- **SubscriptionManagementController**: Unified API controller with GET/PUT operations
- **CustomerSubscriptionsController**: Customer-facing dashboard endpoints with deliveries integration
- **End-to-End Testing**: Comprehensive testing with JWT authentication and curl commands
- **Database Integration**: Proper status transitions, task handling, and audit trail
- **Multi-tenant Security**: Customer authorization and tenant isolation
- **Production Logging**: Structured logging with request IDs and comprehensive error handling

🎉 **UNIFIED SUBSCRIPTION MANAGEMENT API COMPLETE**:
- ✅ **GET /v1/subscription-mgmt/{id}** - Get subscription details and management capabilities
- ✅ **PUT /v1/subscription-mgmt/{id}** - Unified update endpoint for all operations
- ✅ **PAUSE Operation**: Stops billing, cancels renewal tasks, records reason and timestamp
- ✅ **RESUME Operation**: Restarts billing, schedules/updates renewal task without duplicates
- ✅ **Dashboard includes**: subscription details, management capabilities, upcoming deliveries
- ✅ **Tested**: Both read-only endpoints working with proper tenant isolation and JWT authentication

✅ **CUSTOMER SELF-SERVICE ENDPOINTS - 100% COMPLETE**:
- ✅ **GET /v1/customers/me/plans** - View available plans for self-signup
- ✅ **POST /v1/customers/me/subscriptions** - Create subscription (self-signup)
- ✅ **GET /v1/customers/me/subscriptions** - List my subscriptions
- ✅ **GET /v1/customers/me/subscriptions/{id}/dashboard** - Comprehensive subscription dashboard
- ✅ **PATCH /v1/customers/me/subscriptions/{id}** - Pause/resume/cancel subscription
- ✅ **GET /v1/customers/me/deliveries** - View all upcoming deliveries
- ✅ **PATCH /v1/customers/me/deliveries/{id}** - Skip/reschedule delivery

🎉 **M5 COMPLETE**: All customer self-service endpoints implemented and tested!
- See Phase 8.2 for implementation details
- See Phase 11 for comprehensive test coverage (11/11 tests passing)

✅ **COMPLETED M6 - Testing & Production Readiness (100% COMPLETE)**:
- ✅ **Apply delivery cancellation database migration (V011)**: Added `cancelled_at` and `cancellation_reason` fields to `delivery_instances` table
- ✅ **Delivery cancellation persistence**: Updated `DeliveryManagementService` to persist and return cancellation data
- ✅ **End-to-end testing**: Verified delivery cancellation with persisted fields working correctly
- ✅ **Outbox relay + webhooks system**: Complete implementation for reliable event delivery
- ✅ **Comprehensive integration test suite**: 74 API tests covering all 30 endpoints (100% coverage)
- ✅ **End-to-end scenario tests**: 14 business flow scenarios (100% coverage)
- ✅ **Test infrastructure**: Testcontainers, REST Assured, WireMock, Allure reporting
- ✅ **CI/CD ready**: Jenkins integration with parameterized builds
- ✅ **Complete documentation**: TESTING.md with full test coverage details

🎯 **REMAINING M6 TASKS**: None - M6 100% Complete!

🎉 **OUTBOX RELAY + WEBHOOKS COMPLETE**:
- ✅ **OutboxService**: Transactional event emission with idempotency keys
- ✅ **WebhookService**: Webhook endpoint registration and management
- ✅ **WebhookRelayWorker**: Background worker for polling and delivering events
- ✅ **Webhook signature verification**: HMAC-SHA256 signatures for security
- ✅ **Retry logic**: Exponential backoff for failed deliveries (5 attempts max)
- ✅ **Event emission**: Delivery cancellation events now emit to outbox
- ✅ **Webhook API endpoints**: POST/GET/PATCH/DELETE for webhook management
- ✅ **Scheduled workers**: Processes events every 5s, delivers webhooks every 10s

🎉 **COMPREHENSIVE TEST SUITE COMPLETE - 88 TESTS - 100% COVERAGE**:

### **API Integration Tests (74 tests - 100% API Coverage)**
- ✅ **Testcontainers setup**: Real PostgreSQL in Docker for tests
- ✅ **Base test infrastructure**: BaseIntegrationTest with REST Assured
- ✅ **Test utilities**: JWT helper, test data factory, assertions
- ✅ **SubscriptionLifecycleTest** (5 tests): Complete create → pause → resume → cancel flow
- ✅ **SubscriptionModificationTest** (8 tests): Plan changes, quantity, address, payment updates
- ✅ **DeliveryManagementTest** (4 tests): Delivery cancellation with outbox event verification
- ✅ **WebhookDeliveryTest** (5 tests): End-to-end webhook delivery with WireMock
- ✅ **CustomerDashboardTest** (6 tests): Dashboard APIs, subscription listing, authorization
- ✅ **PlanManagementTest** (11 tests): Plan CRUD, filtering, validation, status updates
- ✅ **SecurityAndErrorHandlingTest** (13 tests): Auth, authorization, validation, error handling
- ✅ **TenantManagementTest** (8 tests): Tenant CRUD, data integrity, deletion constraints
- ✅ **CustomerManagementTest** (6 tests): Customer creation, validation, multi-tenancy
- ✅ **EcommerceSubscriptionTest** (4 tests): Direct product subscriptions without plans
- ✅ **AdditionalEndpointTest** (4 tests): Subscription listing, management details, delivery checks
- ✅ **100% API Coverage**: All 30 REST endpoints tested

### **End-to-End Scenario Tests (14 scenarios - 100% Business Flow Coverage)**

**Priority 1 - Critical Business Flows (5 scenarios)**
- ✅ **NewCustomerOnboardingScenarioTest**: Complete customer acquisition flow (tenant → plan → customer → subscription → delivery → webhook → renewal task)
- ✅ **CustomerCancellationScenarioTest**: Complete churn flow (cancel → deliveries cancelled → webhook → refund → no future charges)
- ✅ **WebhookRetryScenarioTest**: Webhook reliability (failure → retry with backoff → fix endpoint → success → HMAC verification)
- ✅ **TenantIsolationScenarioTest**: Multi-tenancy security (2 tenants → cross-tenant access blocked → no data leakage)
- ✅ **SubscriptionRenewalScenarioTest**: Recurring billing (scheduled task → payment → new cycle → next renewal → webhook)

**Priority 2 - Important Features (8 scenarios)**
- ✅ **PauseResumeJourneyScenarioTest**: Vacation/pause feature (pause → deliveries cancelled → tasks cancelled → resume → rescheduled)
- ✅ **PlanUpgradeScenarioTest**: Upsell flow (upgrade plan → prorated charge → delivery updated → billing adjusted)
- ✅ **AddressChangeScenarioTest**: Logistics update (change address → upcoming delivery updated → past unchanged)
- ✅ **DeliveryCancellationAfterOrderScenarioTest**: Late cancellation handling (order placed → cancellation rejected → proper error)
- ✅ **BulkDeliveryCancellationScenarioTest**: Cascade operations (cancel subscription → 5 deliveries cancelled → webhooks sent)
- ✅ **MultipleWebhooksScenarioTest**: Fan-out (3 webhooks → same event → all receive → unique signatures)
- ✅ **WebhookFilteringScenarioTest**: Selective notifications (subscription.* webhook → delivery.* webhook → filtering works)
- ✅ **FailedRenewalRetryScenarioTest**: Payment failure handling (renewal fails → retry → 3 failures → subscription paused)
- ✅ **IdempotencyKeyScenarioTest**: Duplicate prevention (same key → same result → different key → new resource)

**Priority 3 - Edge Cases (1 scenario)**
- ✅ **ConcurrentModificationScenarioTest**: Race conditions (3 concurrent operations → only one succeeds → consistent state → no corruption)

### **Test Infrastructure & Reporting**
- ✅ **Allure reporting**: Beautiful web UI with detailed test reports, screenshots, and attachments
- ✅ **Jenkins integration**: Ready for CI/CD with parameterized builds and test triggers
- ✅ **WireMock integration**: Mock external webhook endpoints for testing
- ✅ **Awaitility**: Async operation testing with proper timeouts
- ✅ **Documentation**: Comprehensive TESTING.md guide with full coverage details and scenario tracking

### **Test Coverage Statistics** (Updated: Feb 11, 2026)
- ✅ **Total Tests**: ~135 tests (118 integration + 17 scenarios)
- ⚠️ **API Endpoints Covered**: ~85% (30+ of 35+ endpoints)
- ✅ **Business Scenarios Covered**: 15 end-to-end flows
- ⚠️ **Critical Gaps**: 4 admin controllers with 0 or partial coverage
- ⚠️ **Service Layer**: 60% (integration only, no unit tests)
- ❌ **Worker Module**: 0% (no tests)

**🔴 CRITICAL: Test Gaps Must Be Addressed Before Production**
- AdminApiClientsController (0 tests)
- AdminUserTenantsController (0 tests)
- AdminUsersController (partial coverage)
- Worker module (0 tests)

---

## 📊 COMPREHENSIVE TEST COVERAGE & EXECUTION GUIDE

### **✅ EXISTING TEST COVERAGE** (34 Test Files, ~135 Tests)

#### **Integration Tests (19 Test Classes)**

| Test Class | Tests | Coverage Area | Status |
|------------|-------|---------------|--------|
| `SubscriptionLifecycleTest` | 5 | Create → Pause → Resume → Cancel flows | ✅ Complete |
| `SubscriptionModificationTest` | 7 | Plan changes, quantity, address, payment updates | ✅ Complete |
| `DeliveryManagementTest` | 4 | Delivery cancellation, outbox events | ✅ Complete |
| `WebhookDeliveryTest` | 6 | Webhook registration, delivery, retries | ✅ Complete |
| `CustomerDashboardTest` | 6 | Dashboard APIs, subscription listing | ✅ Complete |
| `PlanManagementTest` | 11 | Plan CRUD, filtering, validation, status updates | ✅ Complete |
| `SecurityAndErrorHandlingTest` | 13 | Auth, authorization, validation, errors | ✅ Complete |
| `TenantManagementTest` | 8 | Tenant CRUD, data integrity | ✅ Complete |
| `CustomerManagementTest` | 6 | Customer creation, validation, multi-tenancy | ✅ Complete |
| `UnifiedSubscriptionTest` | 5 | Unified subscription creation API | ✅ Complete |
| `AdditionalEndpointTest` | 5 | Subscription listing, management details | ✅ Complete |
| `AuthorizationTest` | 11 | Role-based access control (RBAC) | ✅ Complete |
| `CustomerSelfServiceTest` | 11 | Customer self-service endpoints | ✅ Complete |
| `ApiClientAuthenticationTest` | 4 | HMAC auth, nonce replay, rate limiting | ✅ Complete |
| `PlanCategoryValidationTest` | 4 | DIGITAL, PRODUCT_BASED, HYBRID validation | ✅ Complete |
| `SubscriptionHistoryTest` | 4 | Lifecycle tracking, metadata, pagination | ✅ Complete |
| `UserManagementTest` | 6 | User CRUD, BCrypt hashing, suspend/activate | ✅ Complete |
| `CrossFeatureIntegrationTest` | 2 | End-to-end integration across features | ✅ Complete |

**Subtotal: ~118 integration tests**

#### **Scenario Tests (15 Test Classes)**

| Scenario Test | Tests | Coverage | Status |
|--------------|-------|----------|--------|
| `NewCustomerOnboardingScenarioTest` | 1 | Complete onboarding flow | ✅ Complete |
| `SubscriptionRenewalScenarioTest` | 1 | Renewal with payment | ✅ Complete |
| `PauseResumeJourneyScenarioTest` | 1 | Pause → Resume journey | ✅ Complete |
| `CustomerCancellationScenarioTest` | 1 | Customer-initiated cancellation | ✅ Complete |
| `AddressChangeScenarioTest` | 1 | Address update flow | ✅ Complete |
| `PlanUpgradeScenarioTest` | 1 | Plan upgrade scenario | ✅ Complete |
| `FailedRenewalRetryScenarioTest` | 1 | Payment failure handling | ✅ Complete |
| `BulkDeliveryCancellationScenarioTest` | 1 | Bulk delivery operations | ✅ Complete |
| `DeliveryCancellationAfterOrderScenarioTest` | 1 | Cancellation validation | ✅ Complete |
| `IdempotencyKeyScenarioTest` | 1 | Idempotency enforcement | ✅ Complete |
| `TenantIsolationScenarioTest` | 1 | Multi-tenant isolation | ✅ Complete |
| `ConcurrentModificationScenarioTest` | 3 | Concurrent updates | ✅ Complete |
| `WebhookRetryScenarioTest` | 1 | Webhook retry logic | ✅ Complete |
| `MultipleWebhooksScenarioTest` | 1 | Fan-out delivery | ✅ Complete |
| `WebhookFilteringScenarioTest` | 1 | Event filtering | ✅ Complete |

**Subtotal: ~17 scenario tests**

**TOTAL EXISTING TESTS: ~135 tests across 34 test files**

---

### **❌ CRITICAL TEST GAPS** (Production-Blocking)

#### **🔴 HIGH PRIORITY - Untested Controllers**

| Controller | Endpoints | Tests | Status | Priority |
|-----------|-----------|-------|--------|----------|
| `AdminApiClientsController` | 5 | ❌ 0 | **MISSING** | 🔴 Critical |
| `AdminUserTenantsController` | 5 | ❌ 0 | **MISSING** | 🔴 Critical |
| `AdminUsersController` | 7 | ⚠️ Partial | **GAPS** | 🔴 Critical |
| `AdminSubscriptionHistoryController` | 2 | ⚠️ Partial | **GAPS** | 🟡 High |

**Missing Coverage Details:**

1. **AdminApiClientsController** (0 tests) ❌
   - POST /v1/admin/api-clients - Create API client
   - GET /v1/admin/api-clients - List clients
   - GET /v1/admin/api-clients/{id} - Get client details
   - PATCH /v1/admin/api-clients/{id} - Update/rotate secret
   - DELETE /v1/admin/api-clients/{id} - Revoke client
   - **Gap**: Only auth filter tested, no CRUD operations

2. **AdminUserTenantsController** (0 tests) ❌
   - POST /api/admin/user-tenants - Assign user to tenant
   - GET /api/admin/user-tenants/user/{userId} - Get user's tenants
   - GET /api/admin/user-tenants/tenant/{tenantId} - Get tenant's users
   - PATCH /api/admin/user-tenants/{id} - Update role
   - DELETE /api/admin/user-tenants/{id} - Remove assignment
   - **Gap**: Complete controller untested

3. **AdminUsersController** (partial coverage) ⚠️
   - Missing: Suspend/activate flows
   - Missing: Soft delete verification
   - Missing: Pagination testing
   - Missing: BCrypt password validation

4. **AdminSubscriptionHistoryController** (partial coverage) ⚠️
   - Missing: Pagination testing
   - Missing: Filtering by action type
   - Missing: Metadata verification

---

### **🟡 MEDIUM PRIORITY - Service Layer Tests**

| Service | Unit Tests | Integration Tests | Status |
|---------|-----------|-------------------|--------|
| `PlanValidationService` | ❌ 0 | ✅ Via integration | **GAPS** |
| `SubscriptionHistoryService` | ❌ 0 | ✅ Via integration | **GAPS** |
| `SignatureService` | ❌ 0 | ✅ Via integration | **GAPS** |
| `NonceCache` | ❌ 0 | ✅ Via integration | **GAPS** |
| `RateLimiter` | ❌ 0 | ✅ Via integration | **GAPS** |

**Recommendation**: Add unit tests for business logic isolation and faster test execution.

---

### **🟢 LOW PRIORITY - Worker Module & Performance**

| Area | Tests | Status |
|------|-------|--------|
| **Worker Module** | ❌ 0 | **MISSING** |
| **Load/Performance Tests** | ❌ 0 | **MISSING** |
| **Security Attack Tests** | ⚠️ Partial | **GAPS** |

**Worker Module Gaps:**
- Task handlers (CREATE_DELIVERY, CREATE_ORDER, ENTITLEMENT_GRANT)
- Job scheduling system
- Renewal processing
- Payment processing
- Invoice generation

---

### **📊 Coverage by Controller (12 Controllers)**

| Controller | Test Coverage | Status |
|-----------|---------------|--------|
| PlansController | ✅ PlanManagementTest (11 tests) | **GOOD** |
| SubscriptionsController | ✅ Multiple test classes | **GOOD** |
| CustomerSubscriptionsController | ✅ CustomerSelfServiceTest (11 tests) | **GOOD** |
| CustomersController | ✅ CustomerManagementTest (6 tests) | **GOOD** |
| TenantsController | ✅ TenantManagementTest (8 tests) | **GOOD** |
| DeliveryController | ✅ DeliveryManagementTest (4 tests) | **GOOD** |
| WebhooksController | ✅ WebhookDeliveryTest (6 tests) | **GOOD** |
| SubscriptionManagementController | ✅ Multiple tests | **GOOD** |
| AdminApiClientsController | ❌ No CRUD tests | **CRITICAL GAP** |
| AdminSubscriptionHistoryController | ⚠️ Partial | **GAPS** |
| AdminUsersController | ⚠️ Partial | **GAPS** |
| AdminUserTenantsController | ❌ No tests | **CRITICAL GAP** |

---

### **🎯 Overall Coverage Estimate**

- **API Endpoints**: ~85% covered (tested 30+ of 35+ endpoints)
- **Service Layer**: ~60% covered (integration only, no unit tests)
- **Worker Module**: ~0% covered (no tests)
- **Security**: ~70% covered (auth yes, attack vectors partial)
- **Performance**: ~0% covered (no load tests)

---

## 🎯 Test Priority Roadmap

### **Phase 1: Critical Admin Controller Tests** (1-2 weeks) 🔴

**Estimated Tests**: ~20 new integration tests

| Test Class | Tests | Endpoints Covered | Priority |
|-----------|-------|-------------------|----------|
| `AdminApiClientsCrudTest` | 5 | API client CRUD operations | 🔴 Critical |
| `AdminUserTenantsCrudTest` | 5 | User-tenant management | 🔴 Critical |
| `AdminUsersEnhancedTest` | 7 | Complete user management | 🔴 Critical |
| `AdminSubscriptionHistoryEnhancedTest` | 3 | History pagination & filtering | 🟡 High |

**Why Critical**: These are production admin features with zero test coverage. Required before production deployment.

---

### **Phase 2: Service Layer Unit Tests** (1 week) 🟡

**Estimated Tests**: ~15-20 unit tests

| Service | Tests | Coverage Focus |
|---------|-------|----------------|
| `PlanValidationServiceTest` | 4 | Plan category rules, validation logic |
| `SubscriptionHistoryServiceTest` | 3 | Action recording, metadata handling |
| `SignatureServiceTest` | 3 | HMAC generation, verification |
| `NonceCacheTest` | 3 | Replay prevention, expiration |
| `RateLimiterTest` | 3 | Sliding window, rate enforcement |

**Why Important**: Faster test execution, better isolation, easier debugging.

---

### **Phase 3: Worker Module Tests** (1 week) 🟡

**Estimated Tests**: ~10-15 integration tests

| Test Class | Tests | Coverage Focus |
|-----------|-------|----------------|
| `TaskHandlerTest` | 5 | CREATE_DELIVERY, CREATE_ORDER, ENTITLEMENT_GRANT |
| `JobSchedulingTest` | 3 | Job configuration, execution history |
| `RenewalProcessingTest` | 3 | Invoice generation, payment processing |
| `WorkerIntegrationTest` | 4 | End-to-end worker flows |

**Why Important**: Worker module has zero test coverage. Critical for billing reliability.

---

### **Phase 4: Security & Performance Tests** (1 week) 🟢

**Estimated Tests**: ~15 tests

| Test Class | Tests | Coverage Focus |
|-----------|-------|----------------|
| `SecurityAttackTest` | 5 | Replay attacks, signature tampering, SQL injection |
| `LoadTestScenarios` | 5 | High-volume subscriptions, concurrent operations |
| `PerformanceBenchmarkTest` | 5 | Response times, throughput, database performance |

**Why Important**: Production readiness, security hardening, scalability validation.

---

### **Recommended Execution Order**

1. **Week 1-2**: Phase 1 (Critical admin tests) - **MUST HAVE for production**
2. **Week 3**: Phase 2 (Service unit tests) - Improves test suite quality
3. **Week 4**: Phase 3 (Worker tests) - Critical for billing operations
4. **Week 5**: Phase 4 (Security & performance) - Production hardening

**Total Estimated Effort**: 5 weeks for complete test coverage

---

## 🚀 Running Tests

### **Prerequisites**

- **Docker** installed (for Testcontainers)
- **Java 17+**
- **Gradle** (included via wrapper)

### **Run All Tests**

```bash
# Run all integration tests
./gradlew :apps:subscription-api:test

# Run with Allure report generation
./gradlew :apps:subscription-api:test allureReport

# Open Allure report in browser
./gradlew :apps:subscription-api:allureServe
```

### **Run Specific Test Classes**

```bash
# Run subscription lifecycle tests only
./gradlew :apps:subscription-api:test --tests SubscriptionLifecycleTest

# Run delivery management tests only
./gradlew :apps:subscription-api:test --tests DeliveryManagementTest

# Run webhook tests only
./gradlew :apps:subscription-api:test --tests WebhookDeliveryTest
```

### **Run Tests by Tag/Feature**

```bash
# Run critical tests only
./gradlew :apps:subscription-api:test -Dgroups="critical"

# Run specific feature tests
./gradlew :apps:subscription-api:test --tests "*Lifecycle*"
```

---

## 📊 Allure Reports

### **Local Report Generation**

```bash
# Generate report (HTML files in build/reports/allure-report/)
./gradlew :apps:subscription-api:allureReport

# Serve report with live server (opens browser automatically)
./gradlew :apps:subscription-api:allureServe
```

### **Report Features**

The Allure report includes:
- ✅ **Test execution overview** with pass/fail rates
- 📊 **Graphs and trends** over multiple runs
- 📝 **Detailed test steps** with timing information
- 📎 **HTTP request/response attachments**
- 🔍 **Filtering by feature, severity, status**
- ⚠️ **Flaky test detection**
- 📈 **Historical trends** (when run multiple times)

---

## 🏗️ Test Architecture

### **Test Structure**

```
apps/subscription-api/src/test/java/
├── integration/
│   ├── BaseIntegrationTest.java          # Base class with Testcontainers
│   ├── JwtTestHelper.java                # JWT token generation
│   ├── TestDataFactory.java              # Test data builders
│   ├── SubscriptionLifecycleTest.java    # Subscription flow tests
│   ├── DeliveryManagementTest.java       # Delivery operation tests
│   └── WebhookDeliveryTest.java          # Webhook system tests
```

### **Key Technologies**

- **JUnit 5**: Test framework
- **Testcontainers**: Real PostgreSQL in Docker
- **REST Assured**: API testing DSL
- **Allure**: Beautiful test reporting
- **Awaitility**: Async testing utilities
- **WireMock**: HTTP service mocking
- **AssertJ**: Fluent assertions

### **Test Data**

Tests use `TestDataFactory` to create:
- Plans, customers, subscriptions
- Delivery instances
- Webhook registrations
- Request payloads

Default test tenant: `5aa82d8e-ebec-432b-b568-ac4ba61bb578`

---

## 🐳 Testcontainers

Tests automatically spin up a PostgreSQL container:
- **Image**: `postgres:15-alpine`
- **Database**: `subscription_engine_test`
- **Credentials**: `test/test`
- **Container reuse**: Enabled for faster runs

The container is automatically:
- Started before tests
- Stopped after tests
- Cleaned up on exit

---

## 🎯 Best Practices for Writing New Tests

### **1. Extend BaseIntegrationTest**
```java
class MyNewTest extends BaseIntegrationTest {
    // Your tests here
}
```

### **2. Use Allure annotations**
```java
@Epic("Feature Area")
@Feature("Specific Feature")
@Story("User Story")
@Severity(SeverityLevel.CRITICAL)
@DisplayName("Clear test description")
@Test
void shouldDoSomething() {
    // Test code
}
```

### **3. Add steps for clarity**
```java
@Step("Create subscription")
private UUID createSubscription() {
    // Implementation
}
```

### **4. Attach evidence**
```java
Allure.addAttachment("Response", "application/json", response.asString());
```

### **Test Organization**
- **One test class per feature area**
- **Clear, descriptive test names**
- **Use helper methods for common operations**
- **Keep tests independent** (no shared state)

---

**M7 - Real Integrations & Production Deployment (PENDING)**:

**MEDIUM PRIORITY - Real Adapter Integrations**:
- [ ] **Stripe Payment Adapter**: Replace mock payment adapter with real Stripe integration
  - [ ] Stripe API client setup
  - [ ] Payment intent creation
  - [ ] Payment method management
  - [ ] Webhook handling from Stripe
  - [ ] Refund processing
  - [ ] Payment failure handling
  - [ ] Test with Stripe test mode

- [ ] **Commerce Platform Adapter**: Replace mock commerce adapter with real integration
  - [ ] Choose platform (Shopify, WooCommerce, BigCommerce, etc.)
  - [ ] API client setup
  - [ ] Order creation in external system
  - [ ] Order status synchronization
  - [ ] Inventory management integration
  - [ ] Shipping integration
  - [ ] Test with platform sandbox

**LOW PRIORITY - Performance & Monitoring**:
- [ ] **Load Testing Framework**:
  - [ ] Generate N subscriptions with staggered renewals
  - [ ] Run workers with batch size tuning
  - [ ] Monitor DB CPU and query latency
  - [ ] Test horizontal scaling
  - [ ] Optimize task claiming performance

- [ ] **Comprehensive Observability**:
  - [ ] OpenTelemetry integration
  - [ ] Prometheus metrics export
  - [ ] Grafana dashboards
  - [ ] Distributed tracing
  - [ ] Alert configuration
  - [ ] Performance monitoring

- [ ] **API Rate Limiting & Throttling**:
  - [ ] Rate limiting per tenant
  - [ ] API throttling middleware
  - [ ] DDoS protection
  - [ ] Request quota management

- [ ] **Performance Optimization**:
  - [ ] Batch task processing optimization
  - [ ] Database query optimization
  - [ ] Connection pool tuning
  - [ ] Caching strategy implementation
  - [ ] High-volume scenario handling

---

## M7.1) Stripe Integration - Two Architectural Approaches

### 🎯 **Decision Point: How to Integrate Stripe**

You have **two architectural options** for integrating Stripe with your subscription engine. Choose based on your business requirements and complexity needs.

---

### **Approach 1: Stripe as Payment Processor Only (RECOMMENDED)**

**Architecture**: Your engine controls subscriptions, Stripe only processes payments

**What You Control:**
- ✅ Subscription lifecycle (create, pause, resume, cancel, modify)
- ✅ Billing schedules and cycles
- ✅ Invoice generation (your database)
- ✅ Delivery management
- ✅ Entitlement grants
- ✅ Business rules and workflows
- ✅ Multi-product subscriptions
- ✅ Custom billing intervals

**What Stripe Does:**
- 💳 Process payments (charge cards)
- 💳 Store payment methods securely
- 💳 Handle PCI compliance
- 💳 3D Secure authentication
- 💳 Send payment webhooks
- 💳 Process refunds

**Integration Flow:**
```
Your Subscription Engine              Stripe API
-----------------------              ----------
1. Customer signs up
2. Create subscription (your DB)
3. Store Stripe customer ID
4. Attach payment method     →    Store payment method
5. Schedule renewal task
   
[Renewal Time]
6. Generate invoice (your DB)
7. Create PaymentIntent      →    8. Charge customer
9. Receive webhook           ←    10. Payment result
11. Update invoice status
12. Create delivery
13. Grant entitlement
```

**Implementation Steps:**
```java
// 1. Add Stripe dependency
dependencies {
    implementation 'com.stripe:stripe-java:24.x.x'
}

// 2. Create StripePaymentAdapter
public class StripePaymentAdapter implements PaymentAdapter {
    
    @Override
    public PaymentResult charge(ChargeRequest request) {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(request.getAmountCents())
            .setCurrency(request.getCurrency())
            .setCustomer(request.getStripeCustomerId())
            .setPaymentMethod(request.getPaymentMethodId())
            .setConfirm(true)
            .setOffSession(true) // For recurring charges
            .putMetadata("invoice_id", request.getInvoiceId())
            .putMetadata("tenant_id", request.getTenantId())
            .build();
        
        try {
            PaymentIntent intent = PaymentIntent.create(params);
            return mapToPaymentResult(intent);
        } catch (StripeException e) {
            return handleStripeError(e);
        }
    }
    
    @Override
    public RefundResult refund(RefundRequest request) {
        Refund refund = Refund.create(
            RefundCreateParams.builder()
                .setPaymentIntent(request.getPaymentIntentId())
                .setAmount(request.getAmountCents())
                .build()
        );
        return mapToRefundResult(refund);
    }
}

// 3. Handle Stripe Webhooks
@PostMapping("/webhooks/stripe")
public ResponseEntity<String> handleStripeWebhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String signature) {
    
    Event event = Webhook.constructEvent(payload, signature, webhookSecret);
    
    switch (event.getType()) {
        case "payment_intent.succeeded":
            handlePaymentSuccess(event);
            break;
        case "payment_intent.payment_failed":
            handlePaymentFailure(event);
            break;
        case "charge.refunded":
            handleRefund(event);
            break;
    }
    return ResponseEntity.ok("OK");
}
```

**Pros:**
- ✅ **Full control** over subscription logic and business rules
- ✅ **Complex workflows** (pause, modify, deliveries, entitlements)
- ✅ **Multi-product subscriptions** with different billing cycles
- ✅ **Custom billing intervals** (weekly, bi-weekly, custom)
- ✅ **Your database is source of truth** for all subscription data
- ✅ **Easy to switch payment providers** (adapter pattern)
- ✅ **No vendor lock-in** for subscription logic

**Cons:**
- ⚠️ More code to write and maintain
- ⚠️ You handle all subscription edge cases
- ⚠️ Manual proration calculations
- ⚠️ More testing required

**When to Use:**
- ✅ You need complex subscription workflows (like your current system)
- ✅ Multi-product subscriptions with deliveries
- ✅ Custom business rules (pause/resume with delivery management)
- ✅ Integration with multiple systems (commerce, entitlements)
- ✅ You want full control and flexibility

---

### **Approach 2: Stripe Billing as Subscription Engine**

**Architecture**: Stripe controls subscriptions, you sync data and handle fulfillment

**What Stripe Controls:**
- 📅 Subscription lifecycle
- 📅 Billing schedules
- 📅 Invoice generation (Stripe's invoices)
- 📅 Payment collection
- 📅 Proration calculations
- 📅 Trial periods
- 📅 Metered billing

**What You Do:**
- 🔄 Listen to Stripe webhooks
- 🔄 Sync subscription data to your database
- 🔄 Handle deliveries based on Stripe events
- 🔄 Grant entitlements on payment success
- 🔄 Add custom business logic on top

**Integration Flow:**
```
Stripe Billing                      Your System
--------------                      -----------
1. Create Stripe subscription
2. Stripe generates invoice
3. Stripe charges customer
4. Send webhook              →   5. Receive subscription.created
                             →   6. Sync to your database
                             →   7. Create delivery
                             →   8. Grant entitlement

[Renewal Time]
9. Stripe auto-renews
10. Send webhook             →   11. Receive invoice.paid
                             →   12. Create next delivery
```

**Implementation Steps:**
```java
// 1. Create subscription in Stripe
public StripeSubscription createSubscription(CreateRequest request) {
    SubscriptionCreateParams params = SubscriptionCreateParams.builder()
        .setCustomer(request.getStripeCustomerId())
        .addItem(
            SubscriptionCreateParams.Item.builder()
                .setPrice(request.getStripePriceId())
                .setQuantity(request.getQuantity())
                .build()
        )
        .build();
    
    return Subscription.create(params);
}

// 2. Listen to Stripe webhooks
@PostMapping("/webhooks/stripe")
public ResponseEntity<String> handleStripeWebhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String signature) {
    
    Event event = Webhook.constructEvent(payload, signature, webhookSecret);
    
    switch (event.getType()) {
        case "customer.subscription.created":
            syncSubscriptionToDatabase(event);
            break;
        case "invoice.paid":
            createDeliveryAndEntitlement(event);
            break;
        case "customer.subscription.updated":
            updateSubscriptionInDatabase(event);
            break;
        case "customer.subscription.deleted":
            handleSubscriptionCancellation(event);
            break;
    }
    return ResponseEntity.ok("OK");
}

// 3. Your system becomes event-driven
private void createDeliveryAndEntitlement(Event event) {
    Invoice invoice = (Invoice) event.getDataObjectDeserializer()
        .getObject().orElseThrow();
    
    String subscriptionId = invoice.getSubscription();
    
    // Create delivery in your system
    deliveryService.createDelivery(subscriptionId);
    
    // Grant entitlement
    entitlementService.grantAccess(subscriptionId);
}
```

**Pros:**
- ✅ **Less code to write** - Stripe handles subscription complexity
- ✅ **Built-in proration** - Automatic when changing plans
- ✅ **Stripe dashboard** - Manage subscriptions via UI
- ✅ **Battle-tested** - Stripe's subscription engine is mature
- ✅ **Automatic retries** - Stripe handles failed payments
- ✅ **Dunning management** - Built-in retry logic

**Cons:**
- ⚠️ **Limited flexibility** - Constrained by Stripe's data model
- ⚠️ **Vendor lock-in** - Hard to switch payment providers
- ⚠️ **Complex workflows harder** - Pause/resume with deliveries is tricky
- ⚠️ **Stripe is source of truth** - Your DB is secondary
- ⚠️ **Multi-product complexity** - Each product needs separate subscription or items
- ⚠️ **Webhook dependency** - System breaks if webhooks fail

**When to Use:**
- ✅ Simple subscription model (single product, standard intervals)
- ✅ You want to minimize code
- ✅ Standard SaaS billing (no physical deliveries)
- ✅ You're okay with Stripe's limitations
- ✅ You want Stripe's dashboard for management

---

### **Comparison Table**

| Feature | Approach 1 (Processor Only) | Approach 2 (Stripe Billing) |
|---------|----------------------------|----------------------------|
| **Control** | Full control | Limited by Stripe |
| **Complexity** | Higher code complexity | Lower code complexity |
| **Flexibility** | Very flexible | Constrained |
| **Multi-product** | Easy | Complex |
| **Custom intervals** | Yes | Limited |
| **Deliveries** | Native support | Webhook-based |
| **Pause/Resume** | Full control | Limited |
| **Proration** | Manual | Automatic |
| **Vendor lock-in** | Low | High |
| **Dashboard** | Build your own | Stripe dashboard |
| **Testing** | More tests needed | Less tests needed |

---

### **Recommendation for Your System**

**Use Approach 1: Stripe as Payment Processor Only**

**Reasons:**
1. ✅ You already have a sophisticated subscription engine (M1-M6 complete)
2. ✅ You need complex features:
   - Multi-product subscriptions with different billing cycles
   - Delivery management with cancellation rules
   - Entitlement grants tied to deliveries
   - Pause/resume with delivery rescheduling
   - Custom business workflows
3. ✅ Your database is already the source of truth
4. ✅ You have scheduled tasks system for renewals
5. ✅ You have webhook system for external notifications
6. ✅ Adapter pattern allows switching payment providers later

**Implementation Checklist for Approach 1:**
- [ ] Add Stripe Java SDK dependency
- [ ] Create `StripePaymentAdapter` implementing `PaymentAdapter` interface
- [ ] Implement customer creation in Stripe
- [ ] Implement payment method attachment
- [ ] Update `CHARGE_PAYMENT` handler to use Stripe
- [ ] Create Stripe webhook endpoint
- [ ] Handle payment success/failure webhooks
- [ ] Implement refund processing
- [ ] Add Stripe error handling and retry logic
- [ ] Test with Stripe test mode (test cards)
- [ ] Handle 3D Secure authentication
- [ ] Add integration tests with Stripe mock

---

### **Stripe Key Concepts**

**1. Customer**
- Represents your customer in Stripe
- Stores payment methods
- Links to subscriptions (if using Approach 2)

**2. PaymentMethod**
- Credit card, bank account, digital wallet
- Attached to customer
- Reusable for recurring charges

**3. PaymentIntent**
- Single payment attempt
- Handles 3D Secure, authentication
- Tracks payment lifecycle (requires_action, succeeded, failed)

**4. SetupIntent**
- Used to save payment method without charging
- For future off-session payments

**5. Webhook Events**
- `payment_intent.succeeded` - Payment successful
- `payment_intent.payment_failed` - Payment failed
- `charge.refunded` - Refund processed
- `customer.subscription.*` - Subscription events (Approach 2)

---

### **Idempotency with Stripe**

Both your system and Stripe use idempotency keys:

```java
// Your system generates idempotency key
String idempotencyKey = invoice.getId().toString();

// Pass to Stripe
RequestOptions requestOptions = RequestOptions.builder()
    .setIdempotencyKey(idempotencyKey)
    .build();

PaymentIntent intent = PaymentIntent.create(params, requestOptions);
```

This ensures:
- ✅ No duplicate charges if request retries
- ✅ Same invoice always uses same Stripe payment
- ✅ Safe to retry failed requests

---

### **Next Steps**

When ready to implement:
1. Choose your approach (Recommendation: Approach 1)
2. Set up Stripe test account
3. Get API keys (test mode)
4. Implement adapter
5. Test with test cards
6. Handle edge cases
7. Move to production

---

## 0) V1 Non-Negotiables (Guardrails)

**Infrastructure**
- ✅ Postgres only
- ❌ No Kafka / Pulsar
- ❌ No Redis
- ❌ No external queues

**Correctness**
- Idempotent write APIs (`Idempotency-Key`)
- DB uniqueness constraints prevent duplicates (invoice per period, delivery per cycle)
- DB-driven scheduler queue (`scheduled_tasks`) with leasing + reaper
- Transactional outbox (`outbox_events`) for reliable webhook/event delivery

---

## 1) Milestones (Suggested 3–5 week V1)

### M1 — Skeleton + Core Persistence (Week 1) ✅ COMPLETED
- ✅ Repo + build + local dev setup (Gradle multi-module structure)
- ✅ DB schema + migrations (8 migration files with complete schema + guest shopper support)
- ✅ Docker-compose setup for Postgres
- ✅ Custom attributes strategy (`custom_attrs` JSONB on all tables)
- ✅ Domain models + repositories (jOOQ) - Generated POJOs, DAOs, Records
- ✅ Tenant & auth scaffolding - COMPLETED

### M2 — Subscription APIs + Scheduling Bootstrap (Week 2) ✅ COMPLETED
- ✅ Plans API - Full CRUD with validation and tenant isolation
- ✅ Create subscription API - Multi-product ecommerce subscriptions working
- ✅ Idempotency keys table + middleware - Complete with tenant-aware caching
- ✅ First scheduled task creation (`PRODUCT_RENEWAL`) - Automatic task scheduling implemented
- ✅ Basic read endpoints - Subscription and plan endpoints functional
- ✅ Authentication & Security - JWT tenant extraction with fallback mechanism
- ✅ Database constraint fixes - All validation rules properly enforced

### M3 — Worker Runtime + Renewal/Invoice/Payment Attempts (Week 3) ✅ COMPLETED
- ✅ Worker app (or profile) running scheduler + handlers
- ✅ Renewal -> invoice -> payment attempt (complete end-to-end flow)
- ✅ Retry policy and reaper (automatic task cleanup and retry logic)
- ✅ Comprehensive debugging system with production-ready logging
- ⏳ Outbox relay skeleton (deferred to M5)

### M4 — Deliveries + Orders + Entitlements (Week 4) ✅ COMPLETED
- ✅ Delivery instance creation (cycle snapshots)
- ✅ Commerce adapter (mock + one real adapter optional)
- ✅ Entitlement adapter (mock)
- ✅ Customer account views: `next`, `upcoming-deliveries`
- ✅ Delivery cancellation API for customers
- ✅ Validation to prevent cancellation of deliveries in processing
- ✅ Database migration for cancellation tracking fields

### M5 — Webhooks + Hardening + Load Tests (Week 5)
- Webhook endpoints + deliveries tables
- Webhook dispatcher + retries
- Observability (OpenTelemetry), metrics, dashboards
- Soak/load testing, tuning, documentation polish

> If you want faster: ship M1–M3 as a “billing-only” beta, then add deliveries/entitlements.

---

## 2) Repo Structure (Recommended)

```
subscription-engine/
  README.md
  IMPLEMENTATION_PLAN.md
  build.gradle (or pom.xml)
  docker-compose.yml (Postgres)
  /apps
    /subscription-api
    /subscription-worker
  /modules
    /common
    /auth
    /domain-plans
    /domain-subscriptions
    /domain-billing
    /domain-delivery
    /domain-entitlements
    /scheduler
    /outbox
    /integrations
      /commerce
      /payments
      /entitlements
  /db
    /migrations (Flyway)
  /docs
    architecture.md
    api.md
```

**Key principle**: API and Worker are separate runnable apps, but share domain modules.

---

## 3) Tech Stack Decisions (Lock These Early)

- Java 21
- Spring Boot 3.x
- jOOQ + Flyway
- Postgres
- OpenAPI (springdoc)
- OpenTelemetry (traces + metrics)
- Testcontainers (Postgres)

---

## 4) Core Data Model (V1) — Build Order ✅ COMPLETED

### 4.1 Migrations (Flyway) ✅ COMPLETED
All migrations created and ready for deployment:

**Migration sequence** ✅
1. ✅ `V001` - `tenants`, `tenant_config`
2. ✅ `V002` - `customers`, `plans`
3. ✅ `V003` - `subscriptions`, `subscription_items`
4. ✅ `V004` - `invoices`, `invoice_lines`, `payment_attempts`
5. ✅ `V005` - `delivery_instances`, `entitlements`
6. ✅ `V006` - `scheduled_tasks`, `outbox_events`
7. ✅ `V007` - `idempotency_keys`, `webhook_endpoints`, `webhook_deliveries`

### 4.2 Critical Constraints (Must-have)
- invoices: `UNIQUE(tenant_id, subscription_id, period_start, period_end)`
- deliveries: `UNIQUE(tenant_id, subscription_id, cycle_key)`
- idempotency: `UNIQUE(tenant_id, idempotency_key)`

### 4.3 JSONB Columns (V1 Flexibility) ✅ COMPLETED
**Specialized JSONB columns** (serve specific technical purposes):
- `tenant_config.config_data` - System configuration
- `plans.plan_config` - Plan feature definitions
- `subscriptions.schedule_config` - Billing schedule logic
- `subscriptions.shipping_address` - Structured address data
- `subscriptions.shipping_preferences` - Delivery preferences
- `subscriptions.plan_snapshot` - Immutable plan state
- `subscription_items.item_config` - Item-level configuration
- `delivery_instances.snapshot` - Delivery state snapshot
- `entitlements.entitlement_payload` - Entitlement data structure
- `scheduled_tasks.payload` - Task execution parameters
- `outbox_events.event_payload` - Event publishing data
- `idempotency_keys.response_headers` - HTTP headers
- `webhook_deliveries.payload` - Webhook data

**Plus `custom_attrs JSONB` on ALL tables** for user-defined custom attributes.

---

## 5) API Service (subscription-api) — Implementation Steps

### 5.1 Cross-cutting (Do first) ✅ COMPLETED
- ✅ JWT auth with tenant_id extraction (e.g., `tenant_id` claim) - Implemented with fallback extraction in TenantSecurityService
- ✅ TenantContext (ThreadLocal / request scoped bean) - Using Spring RequestContextHolder for request-scoped tenant context
- ✅ Request validation (Bean Validation) - Implemented with proper error handling
- ✅ Idempotency middleware:
  - ✅ On write endpoints, require `Idempotency-Key`
  - ✅ Look up existing response; if exists, return it
  - ✅ Else proceed and store key with response summary
  - ✅ Tenant-aware caching with proper cleanup

**Cursor prompt snippet**
> “Implement Spring `OncePerRequestFilter` enforcing Idempotency-Key for POST/PATCH/commands; store in `idempotency_keys`; return cached response for duplicate keys.”

### 5.2 Plans API ✅ COMPLETED
- ✅ `POST /v1/plans` - Full implementation with validation and tenant isolation
- ✅ `GET /v1/plans/{planId}` - Individual plan retrieval
- ✅ `GET /v1/plans` - List plans with filtering
- ✅ Database constraints properly enforced (billing intervals, plan types, etc.)
- ✅ Proper error handling and validation messages

### 5.3 Subscriptions API ✅ PARTIALLY COMPLETED
- ✅ `POST /v1/subscriptions/ecommerce` - Multi-product subscription creation
  - ✅ Upsert customer
  - ✅ Create subscription + items
  - ✅ Compute period and nextRenewalAt
  - ✅ Insert `PRODUCT_RENEWAL` tasks (one per product)
  - ⏳ Insert outbox `subscription.created` - TODO
- ✅ Dynamic billing interval detection (single product shows actual interval, mixed shows "MIXED")
- ✅ Proper plan ID mapping and validation
- ⏳ `PATCH /v1/subscriptions/{id}` - TODO
  - Update address/payment/schedule/items (future only)
  - Recompute nextRenewalAt if schedule changed
  - Update (or cancel+recreate) next RENEWAL_DUE task deterministically
  - outbox `subscription.updated`
- ⏳ Actions: - TODO
  - [ ] `:pause` (set status PAUSED, outbox)
  - [ ] `:resume` (set ACTIVE, ensure future renewal task exists, outbox)
  - [ ] `:cancel` (cancel_now / cancel_at_period_end, schedule revoke if needed, outbox)
- ⏳ Reads: - TODO
  - [ ] `GET /v1/subscriptions/{id}` (include items)

### 5.4 Customer Account Views (Read Models)
- [ ] `GET /v1/subscriptions/{id}/next`
- [ ] `GET /v1/subscriptions/{id}/upcoming-deliveries?count=3`

Implementation pattern:
1. Read subscription + items
2. Compute upcoming cycle keys/dates from schedule_config
3. Query `delivery_instances` for those cycle_keys
4. Merge: delivery row overrides projection

### 5.5 Billing/Delivery/Entitlements Read APIs
- [ ] `GET /v1/invoices/{id}`
- [ ] `GET /v1/invoices?...`
- [ ] `GET /v1/payment-attempts?invoiceId=...`
- [ ] `GET /v1/deliveries/{id}`
- [ ] `GET /v1/entitlements?...`

---

## 6) Worker Service (subscription-worker) — Implementation Steps

### 6.1 Worker Runtime
- [ ] Spring Boot app with:
  - Scheduler loop (claims tasks)
  - Task dispatcher (switch by task_type)
  - Handler implementations per task

### 6.2 Task Claiming (DB Queue)
Implement:
- `claimReadyTasks(batchSize, leaseSeconds)`
- `markDone(taskId)`
- `markFailed(taskId, reason)`
- `reschedule(taskId, dueAt, attempt++)`

SQL pattern:
- `SELECT ... FOR UPDATE SKIP LOCKED LIMIT N`
- Update rows to `CLAIMED`, set `locked_until`, `lock_owner`

### 6.3 Reaper (Stuck Task Recovery)
- [ ] Periodic job (e.g., every 30–60s):
  - find `CLAIMED` where `locked_until < now()`
  - if attempts < max -> set READY, clear locks
  - else -> FAILED

### 6.4 Task Types (Implement in this order)

#### A) RENEWAL_DUE (Subscription → Invoice)
- [ ] Load subscription; if not ACTIVE, suppress
- [ ] Compute next period boundaries
- [ ] Create invoice header + lines
- [ ] Insert outbox `invoice.created`
- [ ] Schedule `CHARGE_PAYMENT` for this invoice immediately
- [ ] Update subscription current_period and next_renewal_at
- [ ] Create next `RENEWAL_DUE` task (for next cycle)

#### B) CHARGE_PAYMENT / PAYMENT_RETRY
- [ ] Create payment_attempt row
- [ ] Call PaymentAdapter.charge(invoice, payment_method_ref, idempotencyKey=invoiceId)
- [ ] On success:
  - mark invoice PAID
  - outbox `payment.succeeded`
  - schedule `CREATE_DELIVERY` (if physical/hybrid)
  - schedule `ENTITLEMENT_GRANT` (if digital/hybrid)
- [ ] On failure:
  - mark attempt FAILED
  - outbox `payment.failed`
  - schedule `PAYMENT_RETRY` with backoff
  - if max retries exceeded: mark invoice FAILED, schedule entitlement suspend/revoke

#### C) CREATE_DELIVERY (Create delivery_instance snapshot)
- [ ] Compute cycle_key for period
- [ ] Insert delivery_instances row idempotently (on conflict do nothing)
- [ ] Snapshot items + shipping address from subscription at creation time
- [ ] Schedule `CREATE_ORDER`

#### D) CREATE_ORDER (Commerce adapter)
- [ ] Load delivery instance
- [ ] Call CommerceAdapter.createOrder(delivery.snapshot, idempotencyKey=deliveryId)
- [ ] On success: update `external_order_ref`, status ORDER_CREATED, outbox `delivery.order_created`
- [ ] On failure: retry with backoff; after max -> delivery FAILED

#### E) ENTITLEMENT_GRANT / SUSPEND / REVOKE
- [ ] Upsert entitlements record for period
- [ ] Call EntitlementAdapter.grant/suspend/revoke
- [ ] Emit outbox events

---

## 7) Integrations (Adapters) — V1 Strategy

### 7.1 PaymentAdapter (Start with mock)
Interface:
- `charge(invoiceId, amount, currency, paymentMethodRef, idempotencyKey) -> result`

Implementation:
- Mock provider first (always succeed/fail by config)
- Real provider next (Stripe/Adyen), but keep it optional for V1

### 7.2 CommerceAdapter (Start with mock)
Interface:
- `createOrder(deliverySnapshot, idempotencyKey) -> externalOrderRef`

Implementation:
- Mock adapter writes to log and returns deterministic order id
- Add one real platform connector later

### 7.3 EntitlementAdapter (Mock)
Interface:
- `grant(entitlementPayload, idempotencyKey)`
- `suspend(...)`
- `revoke(...)`

---

## 8) Outbox + Webhooks (Reliable Delivery)

### 8.1 Outbox Relay
- [ ] Poll `outbox_events` where `published_at IS NULL`
- [ ] For each event, create webhook deliveries (one per endpoint)
- [ ] Mark outbox published when deliveries are created (or when delivered; choose one)

### 8.2 Webhooks Tables
- `webhook_endpoints`: tenant_id, url, events, secret_ref, status
- `webhook_deliveries`: endpoint_id, outbox_id, attempt, status, next_attempt_at, last_error

### 8.3 Webhook Dispatcher
- [ ] Claims due webhook deliveries similarly to scheduled_tasks OR reuse scheduled_tasks for webhook attempts
- [ ] Sends signed webhook payload
- [ ] Retries with backoff
- [ ] Marks delivered/failed

**Signing**
- HMAC-SHA256 over request body with endpoint secret
- headers: `X-Webhook-Signature`, `X-Webhook-Event`, `X-Webhook-Id`

---

## 9) Observability (Do not skip)

### 9.1 Logging
- Structured JSON logs
- Include: tenant_id, subscription_id, invoice_id, delivery_id, task_id

### 9.2 Metrics (minimum)
- tasks_claimed_total by task_type
- task_duration_ms histogram by task_type
- tasks_failed_total by reason
- renewal_throughput_per_min
- payment_success_rate
- webhook_delivery_success_rate

### 9.3 Tracing
- Trace id from API -> worker where possible (store in scheduled_tasks.payload or outbox payload)

---

## 10) Local Dev Setup

### 10.1 docker-compose
- Postgres only
- Optional admin UI (pgadmin)

### 10.2 Run modes
- `./gradlew :apps:subscription-api:bootRun`
- `./gradlew :apps:subscription-worker:bootRun`

### 10.3 Seed Data
- one tenant
- one plan
- one customer
- one subscription

---

## 11) Testing Plan (Cursor-friendly)

### 11.1 Unit Tests (light)
- schedule computation
- cycle_key computation
- payload mapping

### 11.2 Integration Tests (heavy; must-have)
Use Testcontainers Postgres:
- Create subscription -> ensures RENEWAL_DUE task exists
- Run worker once -> invoice created -> payment attempt created
- Ensure invoice uniqueness for same period (idempotent renewal)
- Ensure delivery uniqueness for cycle_key
- Retry behavior for failed payment
- Reaper recovers stuck tasks

### 11.3 Load/Soak
- Generate N subscriptions with staggered renewals
- Run workers with batchSize tuning
- Watch DB CPU + query latency

---

## 12) Performance & Tuning Checklist

- [ ] Claim tasks in batches (200–2000) with short transactions
- [ ] External calls outside DB transaction
- [ ] Proper partial indexes on scheduled_tasks
- [ ] Limit per-tenant concurrency in worker (simple in-memory semaphore per tenant per worker instance)
- [ ] Stagger renewals (jitter nextRenewalAt) to avoid midnight storms

---

## 13) Cursor “Build Prompts” (Use these in IDE)

### Prompt A — Generate Flyway migrations + jOOQ codegen
> “Create Flyway migrations for the V1 schema tables: tenants, tenant_config, customers, plans, subscriptions, subscription_items, invoices, invoice_lines, payment_attempts, delivery_instances, entitlements, scheduled_tasks, outbox_events, idempotency_keys. Include uniqueness constraints and indexes described in the doc.”

### Prompt B — Implement scheduled task claiming
> “Implement ScheduledTaskRepository with claimReadyTasks(batchSize, leaseSeconds) using SELECT FOR UPDATE SKIP LOCKED and setting locked_until/lock_owner, plus markDone/markFailed/reschedule methods.”

### Prompt C — Implement renewal handler
> “Implement RENEWAL_DUE handler: load subscription, guard status, create invoice with unique constraint, schedule CHARGE_PAYMENT, advance subscription period and schedule next renewal.”

### Prompt D — Implement customer account read models
> “Implement GET /v1/subscriptions/{id}/next and /upcoming-deliveries: compute cycle keys from schedule_config, query delivery_instances, merge projection vs materialized deliveries.”

---

## 14) Definition of Done (V1)

**Functional**
- Create plan
- Create subscription
- Worker renews subscription -> invoice -> payment attempt
- Payment success triggers delivery/order + entitlement grant (mock adapters ok)
- Customer account endpoints show next + upcoming deliveries
- Pause/resume/cancel behave correctly
- Webhooks deliver core events (optional but recommended in V1)

**Correctness**
- No double invoices per period
- No duplicate deliveries per cycle_key
- Idempotency-Key works for POST/PATCH

**Operational**
- Metrics and logs present
- Worker is horizontally scalable
- Reaper prevents stuck work

---

## 15) Implementation Tracker (Copy into GitHub Issues)

### Week 1 ✅ COMPLETED
- [x] Repo + build + docker-compose Postgres
- [x] Flyway base migrations
- [x] jOOQ setup + codegen
- [x] TenantContext + auth scaffold

### Week 2 ✅ COMPLETED
- [x] Idempotency middleware
- [x] Plans APIs
- [x] Create subscription API + mapping
- [x] scheduled_tasks bootstrap
- [x] Authentication & tenant security fixes
- [x] Database constraint validation fixes
- [x] Dynamic billing interval logic

### Week 3 ✅ COMPLETED
- [x] Worker runtime + scheduler claim loop
- [x] Reaper (automatic cleanup of expired task locks)
- [x] PRODUCT_RENEWAL handler (invoice generation)
- [x] CHARGE_PAYMENT handler + payment processing
- [x] Comprehensive debugging system with production-ready logging
- [x] Database constraint fixes (invoice and payment status)
- [x] Tenant context fixes for background task processing

### Week 4 ✅ COMPLETED
- [x] Delivery snapshot + create order (mock adapter)
- [x] Entitlements (mock adapter)
- [x] Customer account endpoints: next/upcoming
- [x] Delivery cancellation API with validation
- [x] End-to-end testing of delivery cancellation system
- [ ] Pause/resume/cancel full behavior (moved to M5)

### Week 5 - M5 Subscription Management & Production Readiness ✅ 100% COMPLETED
- [x] Subscription pause/resume operations (unified API)
- [x] Unified subscription management API with clean REST conventions
- [x] End-to-end testing with JWT authentication
- [x] Fix duplicate scheduled task issue in resume operation
- [x] Subscription cancellation operations (immediate vs end-of-period)
- [x] Subscription modification endpoints
- [x] Customer subscription dashboard endpoints
- [ ] Apply delivery cancellation database migration (moved to M6)
- [ ] Outbox relay + webhooks (moved to M6)
- [ ] Integration tests (moved to M6)
- [ ] Real payment adapter integration (moved to M6)
- [ ] Load test harness + tuning (moved to M6)
- [ ] Release docs and examples (moved to M6)

---

---

## 16) Key Technical Decisions & Fixes Implemented

### Authentication & Security
- **JWT Tenant Extraction**: Implemented fallback mechanism in `TenantSecurityService` when `JwtTenantAuthenticationFilter` doesn't execute properly
- **Tenant Context**: Enhanced to support both web requests (RequestContextHolder) and background tasks (ThreadLocal)
- **Security Annotations**: `@TenantSecured` annotation working with method-level security

### Database Constraints & Validation
- **Plan Constraints**: Fixed billing interval validation (`MONTHLY`, `WEEKLY`, `YEARLY` vs lowercase)
- **Scheduled Tasks**: Fixed status constraint (`READY` vs `PENDING`)
- **Plan Types**: Enforced `RECURRING` vs `ONE_TIME` validation
- **Invoice Status**: Fixed constraint violation by using `OPEN` instead of `PENDING`
- **Payment Status**: Fixed constraint violation by using `SUCCEEDED` instead of `COMPLETED`

### API Design Decisions
- **Multi-Product Subscriptions**: Single subscription can contain multiple products with different plans
- **Dynamic Billing Intervals**: Shows actual interval for single products, "MIXED" only when intervals differ
- **Idempotency**: All write operations require `Idempotency-Key` header with tenant-aware caching
- **Plan-Based Architecture**: Products reference plans for billing schedules rather than embedded billing logic

### Task Scheduling & Worker Runtime
- **Product-Level Tasks**: Each product gets its own `PRODUCT_RENEWAL` task for independent billing cycles
- **Task Status**: Using `READY` status for new tasks to match database constraints
- **Tenant Isolation**: All scheduled tasks are tenant-scoped
- **Distributed Locking**: Implemented `SELECT FOR UPDATE SKIP LOCKED` pattern for task claiming
- **Task Reaper**: Automatic cleanup of expired task locks with configurable retry limits
- **Lease Management**: 5-minute task leases with proper lock ownership tracking

### Billing System Implementation
- **Invoice Generation**: Complete invoice creation with line items, totals calculation, and payment scheduling
- **Payment Processing**: End-to-end payment flow with mock payment adapter integration
- **Database Consistency**: Proper transaction handling and constraint compliance
- **Idempotency**: Invoice creation is idempotent per subscription period
- **Status Management**: Proper invoice status transitions (OPEN → PAID)

### Error Handling & Debugging
- **Comprehensive Logging**: Production-ready structured logging with timing metrics throughout entire billing flow
- **Structured Log Tags**: `[BILLING_FLOW_START]`, `[INVOICE_GEN_STEP_X]`, `[PAYMENT_PROC_SUCCESS]` for easy filtering
- **Performance Monitoring**: Execution time tracking for all major operations
- **Error Context**: Detailed error messages with full stack traces and tenant context
- **Database Operation Tracking**: Row counts and query execution confirmation
- **Constraint Validation**: Proper error messages for database constraint violations
- **Fallback Mechanisms**: JWT extraction fallback ensures system works even when filters don't execute

### Production Readiness Features
- **Comprehensive Debugging**: Detailed logging at every step for production troubleshooting
- **Performance Metrics**: Timing data for optimization and monitoring
- **Error Recovery**: Proper exception handling and transaction rollback
- **Database Health**: Query logging and performance tracking
- **Tenant Context Tracking**: Clear tenant isolation logging throughout request lifecycle

### M3+ Configurable Scheduling - Critical Bug Fixes (January 26, 2026)
- **Jackson JSONB Serialization Issue**: Fixed `No serializer found for class org.jooq.JSONB` error in job status endpoint
  - **Root Cause**: jOOQ JSONB objects cannot be serialized directly by Jackson
  - **Solution**: Added JSONB to String conversion in `JobConfigurationService.getJobConfiguration()`
  - **Code**: `((org.jooq.JSONB) jobConfigValue).data()` conversion before JSON response
- **Spring CronExpression Temporal Type Issue**: Fixed `UnsupportedTemporalTypeException: Unsupported field: DayOfWeek`
  - **Root Cause**: `CronExpression.next()` doesn't support `Instant` for day-of-week calculations
  - **Solution**: Convert `Instant` to `LocalDateTime` for cron calculations, then back to `Instant`
  - **Code**: `LocalDateTime.ofInstant(instant, ZoneId.systemDefault())` → `cron.next(localDateTime)` → `toInstant()`
- **Comprehensive Debug Logging**: Added production-ready structured logging with request IDs throughout scheduling system
  - **Pattern**: `[LOG_TAG] RequestId: {requestId} - {message}` for traceable debugging
  - **Coverage**: JobConfigurationService, DynamicSchedulingService, RenewalJobController
- **End-to-End API Testing**: Validated all admin API endpoints with dynamic schedule changes
  - **Tested**: EVERY_MINUTE → DAILY_6AM → HOURLY schedule transitions
  - **Verified**: Database updates, job rescheduling, configuration persistence

### Notes
- Keep DB transactions **small**.
- Keep external calls **idempotent** and **outside** DB transactions.
- Treat subscription billing like a bank ledger: snapshots + auditability.
- **Tenant isolation is critical**: All operations must be tenant-scoped for security.
- **Jackson JSONB Handling**: Always convert jOOQ JSONB objects to strings before JSON serialization
- **Spring CronExpression**: Use LocalDateTime for cron calculations, not Instant, especially with day-of-week patterns

---

## M8 - API Redesign: Admin vs Customer Separation (PLANNING PHASE)

**Status:** Planning & Design  
**Created:** February 8, 2026  
**Target:** Complete redesign before M9

### 🎯 Objectives

1. **Clear API Separation:** Distinct Admin and Customer APIs for better security and UX
2. **Unified Subscription Model:** Single endpoint for digital and product-based subscriptions
3. **Plan Validation:** Enforce rules at plan level (requires products, allows products, etc.)
4. **Audit Trail:** Track who made changes (customer vs admin)
5. **RESTful Design:** Use PATCH for updates instead of multiple POST endpoints
6. **Data Integrity:** Soft delete only, preserve historical data

---

### 📊 Current State Issues

**Existing Controllers (To Be Refactored/Removed):**
- ❌ `SubscriptionsController.java` - Generic subscription endpoints
- ❌ `EcommerceSubscriptionsController.java` - Separate ecommerce endpoint (to merge)
- ❌ `SubscriptionManagementController.java` - Mixed admin/customer operations
- ❌ `CustomerSubscriptionsController.java` - Customer-specific endpoints (to redesign)
- ⚠️ `DeliveryController.java` - Keep but refactor for admin/customer split
- ⚠️ `CustomersController.java` - Keep but refactor for admin/customer split

**Problems:**
1. No clear separation between admin and customer operations
2. ~~Separate endpoints for ecommerce vs digital subscriptions~~ ✅ **RESOLVED** - Unified into single endpoint with optional products
3. Multiple POST endpoints for each operation (pause, resume, cancel)
4. No plan-level validation for product requirements
5. No audit trail for who made changes
6. Hard delete capability (data loss risk)

---

### 🏗️ New API Structure (53 Total Endpoints)

#### **Admin APIs (36 endpoints)**

**Admin - Tenants** (`/v1/admin/tenants`)
```
POST   /v1/admin/tenants                          - Create tenant
GET    /v1/admin/tenants                          - List all tenants
GET    /v1/admin/tenants/{id}                     - Get tenant details
PATCH  /v1/admin/tenants/{id}                     - Update tenant
```

**Admin - Plans** (`/v1/admin/plans`)
```
POST   /v1/admin/plans                            - Create plan
GET    /v1/admin/plans                            - List all plans
GET    /v1/admin/plans/{id}                       - Get plan details
PATCH  /v1/admin/plans/{id}                       - Update plan
```

**Admin - Users** (`/v1/admin/users`)
```
POST   /v1/admin/users                            - Create user
GET    /v1/admin/users                            - List all users
GET    /v1/admin/users/{id}                       - Get user details
PATCH  /v1/admin/users/{id}                       - Update user (profile, role, tenant assignments)
DELETE /v1/admin/users/{id}                       - Delete user
```

**PATCH Operations:**
```json
// Update profile
{ "fullName": "John Smith", "email": "john@example.com" }

// Change role
{ "role": "TENANT_ADMIN" }

// Assign to tenants
{ "tenants": [{"tenantId": "uuid-123", "role": "OWNER"}] }

// Remove from tenant
{ "tenants": [{"tenantId": "uuid-123", "action": "REMOVE"}] }
```

**Admin - API Clients** (`/v1/admin/api-clients`)
```
POST   /v1/admin/api-clients                      - Create API client
GET    /v1/admin/api-clients                      - List all API clients (includes usage stats)
GET    /v1/admin/api-clients/{id}                 - Get client details (includes usage stats)
PATCH  /v1/admin/api-clients/{id}                 - Update client (status, scopes, rotate secret, IPs, rate limits)
DELETE /v1/admin/api-clients/{id}                 - Revoke client
```

**PATCH Operations:**
```json
// Suspend client
{ "status": "SUSPENDED" }

// Resume client
{ "status": "ACTIVE" }

// Rotate secret (returns new secret in response)
{ "rotateSecret": true }

// Update scopes
{ "scopes": ["subscriptions:*"] }

// Update rate limits
{ "rateLimitPerHour": 2000 }

// Multiple operations
{ "status": "ACTIVE", "scopes": ["subscriptions:*"], "rateLimitPerHour": 5000 }
```

**Admin - Subscriptions** (`/v1/admin/subscriptions`)
```
POST   /v1/admin/subscriptions                    - Create subscription (unified: supports both simple SaaS and ecommerce with products)
GET    /v1/admin/subscriptions                    - List all subscriptions (paginated, filterable)
GET    /v1/admin/subscriptions/{id}               - Get subscription details
PATCH  /v1/admin/subscriptions/{id}               - Unified update (all actions)
GET    /v1/admin/subscriptions/{id}/deliveries    - List deliveries for subscription
```

**Admin - Deliveries** (`/v1/admin/deliveries`)
```
GET    /v1/admin/deliveries                       - List all deliveries (filterable)
GET    /v1/admin/deliveries/{id}                  - Get delivery details
PATCH  /v1/admin/deliveries/{id}                  - Unified update (all actions)
```

**Admin - Customers** (`/v1/admin/customers`)
```
POST   /v1/admin/customers                        - Create customer
GET    /v1/admin/customers                        - List all customers (paginated)
GET    /v1/admin/customers/{id}                   - Get customer details
PATCH  /v1/admin/customers/{id}                   - Update customer
GET    /v1/admin/customers/{id}/subscriptions     - Get customer's subscriptions
GET    /v1/admin/customers/{id}/deliveries        - Get all deliveries for customer
```

**Admin - Webhooks** (`/v1/admin/webhooks`)
```
POST   /v1/admin/webhooks                         - Create webhook endpoint
GET    /v1/admin/webhooks                         - List all webhooks
GET    /v1/admin/webhooks/{id}                    - Get webhook details
PATCH  /v1/admin/webhooks/{id}                    - Update webhook
DELETE /v1/admin/webhooks/{id}                    - Delete webhook
```

#### **Customer APIs (11 endpoints)**

**Customer - Subscriptions** (`/v1/customer/subscriptions`)
```
POST   /v1/customer/subscriptions                 - Create own subscription
GET    /v1/customer/subscriptions                 - List my subscriptions
GET    /v1/customer/subscriptions/{id}            - Get my subscription details
PATCH  /v1/customer/subscriptions/{id}            - Unified update (all actions)
GET    /v1/customer/subscriptions/{id}/deliveries - List deliveries for my subscription
```

**Customer - Deliveries** (`/v1/customer/deliveries`)
```
GET    /v1/customer/deliveries                    - List ALL my deliveries (all subscriptions)
GET    /v1/customer/deliveries/{id}               - Get delivery details
PATCH  /v1/customer/deliveries/{id}               - Unified update (all actions)
```

**Customer - Profile** (`/v1/customer/profile`)
```
GET    /v1/customer/profile                       - Get my profile
PATCH  /v1/customer/profile                       - Update my profile
```

---

### 🗄️ Database Migrations

#### **Phase 0: User Management System (NEW - Foundation)**

**Migration:** `V016__Create_users_and_user_tenants.sql`

```sql
-- Users table for admin and staff users
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) NOT NULL UNIQUE,
  full_name VARCHAR(255),
  role VARCHAR(50) NOT NULL, -- 'SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF', 'CUSTOMER'
  status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'SUSPENDED', 'DELETED'
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  CONSTRAINT users_role_check CHECK (role IN ('SUPER_ADMIN', 'TENANT_ADMIN', 'STAFF', 'CUSTOMER')),
  CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

COMMENT ON TABLE users IS 'System users (admins, staff) who manage tenants and subscriptions';
COMMENT ON COLUMN users.role IS 'SUPER_ADMIN: platform admin, TENANT_ADMIN: tenant owner, STAFF: tenant employee, CUSTOMER: end customer';

-- User-Tenant relationships (multi-tenant access)
CREATE TABLE user_tenants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
  role VARCHAR(50) NOT NULL, -- 'OWNER', 'ADMIN', 'MEMBER'
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  CONSTRAINT user_tenants_role_check CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
  UNIQUE(user_id, tenant_id)
);

CREATE INDEX idx_user_tenants_user ON user_tenants(user_id);
CREATE INDEX idx_user_tenants_tenant ON user_tenants(tenant_id);

COMMENT ON TABLE user_tenants IS 'Maps users to tenants with specific roles';
COMMENT ON COLUMN user_tenants.role IS 'OWNER: full control, ADMIN: manage resources, MEMBER: read-only';
```

**Migration:** `V017__Add_audit_fields_to_existing_tables.sql`

```sql
-- Add created_by and updated_by to all existing tables

-- Tenants
ALTER TABLE tenants ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE tenants ADD COLUMN updated_by UUID REFERENCES users(id);
COMMENT ON COLUMN tenants.created_by IS 'User who created this tenant';
COMMENT ON COLUMN tenants.updated_by IS 'User who last updated this tenant';

-- Customers
ALTER TABLE customers ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE customers ADD COLUMN updated_by UUID REFERENCES users(id);
COMMENT ON COLUMN customers.created_by IS 'User who created this customer (admin or system)';
COMMENT ON COLUMN customers.updated_by IS 'User who last updated this customer';

-- Plans
ALTER TABLE plans ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE plans ADD COLUMN updated_by UUID REFERENCES users(id);
COMMENT ON COLUMN plans.created_by IS 'User who created this plan';
COMMENT ON COLUMN plans.updated_by IS 'User who last updated this plan';

-- Subscriptions
ALTER TABLE subscriptions ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE subscriptions ADD COLUMN updated_by UUID REFERENCES users(id);
ALTER TABLE subscriptions ADD COLUMN created_by_type VARCHAR(50); -- 'ADMIN', 'CUSTOMER', 'SYSTEM'
COMMENT ON COLUMN subscriptions.created_by IS 'User who created subscription (admin user_id or customer user_id)';
COMMENT ON COLUMN subscriptions.updated_by IS 'User who last updated subscription';
COMMENT ON COLUMN subscriptions.created_by_type IS 'Whether created by admin, customer, or system';

-- Subscription Items
ALTER TABLE subscription_items ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE subscription_items ADD COLUMN updated_by UUID REFERENCES users(id);

-- Invoices
ALTER TABLE invoices ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE invoices ADD COLUMN updated_by UUID REFERENCES users(id);

-- Delivery Instances
ALTER TABLE delivery_instances ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE delivery_instances ADD COLUMN updated_by UUID REFERENCES users(id);

-- Webhook Endpoints
ALTER TABLE webhook_endpoints ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE webhook_endpoints ADD COLUMN updated_by UUID REFERENCES users(id);

-- Scheduled Tasks
ALTER TABLE scheduled_tasks ADD COLUMN created_by UUID REFERENCES users(id);
ALTER TABLE scheduled_tasks ADD COLUMN updated_by UUID REFERENCES users(id);
```

---

#### **2. Plans Table - Add Validation Fields**

**Migration:** `V018__Add_plan_validation_fields.sql`

```sql
ALTER TABLE plans ADD COLUMN plan_category VARCHAR(50) NOT NULL DEFAULT 'DIGITAL';
COMMENT ON COLUMN plans.plan_category IS 'DIGITAL, PRODUCT_BASED, or HYBRID';

ALTER TABLE plans ADD COLUMN requires_products BOOLEAN NOT NULL DEFAULT false;
COMMENT ON COLUMN plans.requires_products IS 'Whether subscription must include products';

ALTER TABLE plans ADD COLUMN allows_products BOOLEAN NOT NULL DEFAULT false;
COMMENT ON COLUMN plans.allows_products IS 'Whether subscription can include products';

ALTER TABLE plans ADD COLUMN base_price_required BOOLEAN NOT NULL DEFAULT true;
COMMENT ON COLUMN plans.base_price_required IS 'Whether plan must have base price > 0';
```

**Plan Types:**
- **DIGITAL**: `requiresProducts=false`, `allowsProducts=false`, `basePriceRequired=true` (SaaS)
- **PRODUCT_BASED**: `requiresProducts=true`, `allowsProducts=true`, `basePriceRequired=false` (Subscription Box)
- **HYBRID**: `requiresProducts=false`, `allowsProducts=true`, `basePriceRequired=true` (Base + Add-ons)

#### **3. Subscriptions Table - Add Additional Audit Fields**

**Migration:** `V019__Add_subscription_additional_audit_fields.sql`

```sql
-- Additional audit fields for subscriptions (beyond created_by/updated_by from V017)
ALTER TABLE subscriptions ADD COLUMN admin_notes TEXT;
COMMENT ON COLUMN subscriptions.admin_notes IS 'Admin notes for internal tracking';

ALTER TABLE subscriptions ADD COLUMN archived_at TIMESTAMP NULL;
COMMENT ON COLUMN subscriptions.archived_at IS 'When soft deleted/archived';

ALTER TABLE subscriptions ADD COLUMN archived_by UUID REFERENCES users(id);
COMMENT ON COLUMN subscriptions.archived_by IS 'User who archived the subscription';
```

#### **4. Subscription History Table - New**

**Migration:** `V020__Create_subscription_history.sql`

```sql
CREATE TABLE subscription_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL REFERENCES tenants(id),
  subscription_id UUID NOT NULL REFERENCES subscriptions(id),
  action VARCHAR(50) NOT NULL,
  performed_by UUID NOT NULL REFERENCES users(id),
  performed_by_type VARCHAR(50) NOT NULL, -- 'ADMIN', 'CUSTOMER', 'SYSTEM'
  performed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  metadata JSONB,
  notes TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_history_subscription ON subscription_history(subscription_id);
CREATE INDEX idx_subscription_history_tenant ON subscription_history(tenant_id);
CREATE INDEX idx_subscription_history_performed_by ON subscription_history(performed_by);
CREATE INDEX idx_subscription_history_performed_at ON subscription_history(performed_at DESC);

COMMENT ON TABLE subscription_history IS 'Audit trail for all subscription changes';
COMMENT ON COLUMN subscription_history.action IS 'CREATED, PAUSED, RESUMED, CANCELED, PLAN_CHANGED, etc.';
COMMENT ON COLUMN subscription_history.performed_by IS 'User ID who performed the action';
COMMENT ON COLUMN subscription_history.performed_by_type IS 'Whether action was by ADMIN, CUSTOMER, or SYSTEM';
```

**Actions:** `CREATED`, `PAUSED`, `RESUMED`, `CANCELED`, `PLAN_CHANGED`, `PAYMENT_UPDATED`, `PRODUCTS_UPDATED`, `SHIPPING_UPDATED`, `METADATA_UPDATED`, `ARCHIVED`

#### **5. Deliveries Table - Add Additional Audit Fields**

**Migration:** `V021__Add_delivery_additional_audit_fields.sql`

```sql
-- Additional audit fields for deliveries (beyond created_by/updated_by from V017)
ALTER TABLE delivery_instances ADD COLUMN rescheduled_by UUID REFERENCES users(id);
ALTER TABLE delivery_instances ADD COLUMN reschedule_reason TEXT;

COMMENT ON COLUMN delivery_instances.rescheduled_by IS 'User who rescheduled the delivery';
COMMENT ON COLUMN delivery_instances.canceled_by IS 'User who canceled the delivery (already exists from V011)';
```

---

### 💻 Implementation Tasks

#### **Phase 0: User Management System (Foundation)** ✅ **COMPLETED**
- [x] Create V016__Create_users_and_user_tenants.sql
- [x] Create V017__Add_audit_fields_to_existing_tables.sql
- [x] Test user management migrations locally
- [x] Regenerate jOOQ classes for users and user_tenants tables
- [x] Create seed data for initial admin users (V019__Bootstrap_super_admin.sql)

#### **Phase 0.5: API Client Management & Authentication (Security Foundation)** ✅ **API KEY AUTH COMPLETE** (OAuth & mTLS pending)

**Status Summary:**
- ✅ **Database Schema**: All tables created (api_clients, oauth_access_tokens, request_nonces, rate_limit_buckets)
- ✅ **Admin CRUD APIs**: `AdminApiClientsController` fully implemented (create, list, get, update, delete, rotate secret)
- ✅ **DTOs**: All request/response models created
- ✅ **Authentication Middleware**: `ApiKeyAuthFilter` fully implemented with Spring Security integration
- ✅ **HMAC Signature Verification**: `SignatureService` fully implemented with HMAC-SHA256
- ✅ **Nonce Replay Prevention**: `NonceCache` fully implemented with PostgreSQL backend
- ✅ **Rate Limiting Enforcement**: `RateLimiter` fully implemented with sliding window algorithm
- ❌ **OAuth 2.0 Flow**: NOT IMPLEMENTED (planned for Tier 2)
- ❌ **mTLS Support**: NOT IMPLEMENTED (planned for Tier 3)

**What Works:** Complete API Key + HMAC authentication system (Tier 1 - 90% of use cases)
**What's Pending:** OAuth 2.0 and mTLS for advanced use cases

---

**Auth Method Selection:**

When creating an API client, the admin chooses the authentication method via the `authMethod` field:

```json
POST /v1/admin/api-clients
{
  "name": "Shopify Integration",
  "authMethod": "API_KEY",  // Options: API_KEY, OAUTH, MTLS
  "clientType": "SERVER",    // SERVER, SPA, MOBILE, NATIVE
  "scopes": ["subscriptions:*", "deliveries:*"],
  "allowedIps": ["203.0.113.10"],  // Optional, for API_KEY
  "redirectUris": [],               // Required for OAUTH
  "rateLimitPerHour": 1000
}
```

**Auth Method Behavior:**

1. **API_KEY** (Default for server-to-server):
   - System generates: `client_id` + `client_secret`
   - Secret shown ONLY ONCE in response
   - Client uses HMAC-SHA256 request signing
   - Headers: X-Client-ID, X-Timestamp, X-Nonce, X-Signature

2. **OAUTH** (For customer-facing apps):
   - System generates: `client_id` + `client_secret`
   - Requires `redirectUris` in request
   - Client uses OAuth 2.0 authorization code flow with PKCE
   - Returns JWT access tokens (1 hour) + refresh tokens (30 days)

3. **MTLS** (For enterprise):
   - System generates: `client_id` only (no secret)
   - Status set to `PENDING_CERTIFICATE`
   - Admin must upload client certificate via separate endpoint
   - Client uses X.509 certificate for authentication

**Multi-Auth Filter Logic:**

The system automatically detects which auth method to use based on:
- mTLS: Presence of X.509 certificate in request
- API Key: Presence of X-Client-ID header
- OAuth: Presence of Authorization: Bearer token

Each client's `auth_method` is stored in database and enforced on every request.

**Database Migrations:** ✅ **COMPLETED**
- [x] Create V018__Create_api_clients.sql (renamed from V022)
  - [x] api_clients table (client_id, secret_hash, auth_method, scopes, allowed_ips)
  - [x] api_client_scopes table (granular permissions)
  - [x] oauth_access_tokens table (for OAuth 2.0)
  - [x] api_usage_logs table (rate limiting & analytics)
  - [x] request_nonces table (replay attack prevention - PostgreSQL)
  - [x] rate_limit_buckets table (rate limiting - PostgreSQL)
- [x] Test all security migrations
- [x] Regenerate jOOQ classes

**Tier 1: API Key + HMAC Signing (Primary - 90% of customers):** ✅ **FULLY IMPLEMENTED**
- [x] Create `AdminApiClientsController` (implements all CRUD operations)
  - [x] `createClient()` - Generate client_id & secret (show secret ONLY ONCE)
  - [x] `rotateSecret()` - Rotate client secret securely via PATCH
  - [x] `revokeClient()` - Revoke client access via DELETE
  - [x] `updateScopes()` - Modify client permissions via PATCH
  - [x] `getClientUsage()` - Get usage statistics via GET
  - [x] `listClients()` - List all clients for tenant via GET
- [x] Create `SignatureService` (modules/auth/src/main/java/com/subscriptionengine/auth/) ✅ **IMPLEMENTED**
  - [x] `generateCanonicalRequest()` - Create canonical string
  - [x] `generateSignature()` - HMAC-SHA256 signing
  - [x] `verifySignature()` - Server-side verification
  - [x] `validateTimestamp()` - 5 minute window check
- [x] Create `ApiKeyAuthFilter` (Spring Security Filter) ✅ **IMPLEMENTED**
  - [x] Extract client_id from X-Client-ID header
  - [x] Verify client exists and status is ACTIVE
  - [x] Validate timestamp (prevent old requests)
  - [x] Check nonce in PostgreSQL (prevent replay attacks)
  - [x] Verify HMAC signature
  - [x] Check IP whitelist (if configured)
  - [x] Validate scopes for requested endpoint
  - [x] Set authentication context
  - [x] Update last_used_at timestamp
- [x] Create `NonceCache` - **Option A (PostgreSQL-backed)** ✅ **IMPLEMENTED**
  - [x] request_nonces table created in V018
  - [x] Implement checkAndStore() method
  - [x] Scheduled cleanup job (delete old nonces every 5 minutes)
- [x] Create `RateLimiter` - **Option A (PostgreSQL-backed)** ✅ **IMPLEMENTED**
  - [x] rate_limit_buckets table created in V018
  - [x] Implement sliding window in SQL
  - [x] Scheduled cleanup job (delete old buckets every hour)
  - [x] Return 429 when exceeded with X-RateLimit headers

**Tier 2: OAuth 2.0 + PKCE (Customer-Facing - 5% of customers):**
- [ ] Create OAuth 2.0 Authorization Server
  - [ ] `/oauth/authorize` - Authorization endpoint
  - [ ] `/oauth/token` - Token endpoint
  - [ ] `/oauth/revoke` - Revocation endpoint
- [ ] Implement PKCE support
  - [ ] code_challenge validation
  - [ ] code_verifier verification
- [ ] Create `OAuthTokenService`
  - [ ] `generateAccessToken()` - 1 hour TTL
  - [ ] `generateRefreshToken()` - 30 days TTL
  - [ ] `refreshAccessToken()` - Token refresh flow
  - [ ] `revokeToken()` - Token revocation
- [ ] Create `OAuthAuthFilter`
  - [ ] JWT token validation
  - [ ] Scope verification
  - [ ] Token expiration check

**Tier 3: mTLS (Enterprise - 5% of customers):**
- [ ] Configure mTLS connector (port 8443)
- [ ] Set up certificate trust store
- [ ] Create `MtlsAuthFilter`
  - [ ] Extract X.509 certificate
  - [ ] Verify certificate chain
  - [ ] Check certificate fingerprint
  - [ ] Validate certificate not revoked (CRL/OCSP)
  - [ ] Extract client identity from CN
- [ ] Create certificate management endpoints
  - [ ] Upload client certificate
  - [ ] Revoke certificate
  - [ ] List certificates

**Security Monitoring & Response:**
- [ ] Create `AnomalyDetectionService`
  - [ ] Detect unusual request patterns
  - [ ] Monitor for geographic anomalies
  - [ ] Track new IP addresses
  - [ ] Identify spike in error rates
  - [ ] Flag unusual endpoint access
- [ ] Create `SecurityAlertService`
  - [ ] Real-time alerts to admins
  - [ ] Email notifications
  - [ ] Slack/webhook integration
  - [ ] Alert severity levels
- [ ] Create `AutoSuspensionService`
  - [ ] Auto-suspend on high-risk behavior
  - [ ] Configurable thresholds
  - [ ] Manual override capability
  - [ ] Suspension notifications
- [ ] Create `AuditLogService`
  - [ ] Log all API requests
  - [ ] Store client_id, endpoint, IP, timestamp
  - [ ] Searchable audit trail
  - [ ] Retention policy (90 days)

**Admin APIs - API Client Management:** ✅ **COMPLETED**
- [x] Create `AdminApiClientsController`
  - [x] POST /v1/admin/api-clients - Create client
  - [x] GET /v1/admin/api-clients - List all clients (includes usage stats)
  - [x] GET /v1/admin/api-clients/{id} - Get client details (includes usage stats)
  - [x] PATCH /v1/admin/api-clients/{id} - Update client (status, scopes, rotate secret, IPs, rate limits)
  - [x] DELETE /v1/admin/api-clients/{id} - Revoke client

**DTOs:** ✅ **COMPLETED**
- [x] Create `CreateApiClientRequest` DTO (inline in controller)
- [x] Create `ApiClientResponse` DTO (inline in controller)
- [x] Create `UpdateApiClientRequest` DTO (inline in controller)
- [x] Create `CreateApiClientResponse` DTO (with secret on creation only)

**Security Configuration:** ✅ **COMPLETED**
- [x] Update `SecurityConfig` to support multi-auth (JWT + API Key)
- [x] Add `ApiKeyAuthFilter` to Spring Security filter chain
- [x] Configure filter order (API Key before JWT authentication)
- [x] Add jOOQ and BCrypt dependencies to auth module

**Testing:**
- [ ] Unit tests for signature generation/verification
- [ ] Unit tests for nonce cache
- [ ] Unit tests for rate limiter
- [ ] Integration tests for API key authentication
- [ ] Integration tests for OAuth flow
- [ ] Integration tests for mTLS
- [ ] Security tests (replay attacks, tampering, etc.)
- [ ] Load tests for rate limiting

#### **Phase 1: Plan Validation & Subscription Audit** ✅ **COMPLETED**
- [x] Create V020__Add_plan_validation_fields.sql
- [x] Create V021__Create_subscription_history.sql
- [x] Create V022__Add_delivery_additional_audit_fields.sql
- [x] Test all migrations locally
- [x] Regenerate jOOQ classes for all updated tables
- [x] Update existing plans with default validation values
- [x] Create PlanValidationService
- [x] Create SubscriptionHistoryService

#### **Phase 2: DTOs and Request/Response Models** ✅ **COMPLETED**
- [x] Create `UserResponse` DTO (user details)
- [x] Create `CreateUserRequest` DTO (admin creates users)
- [x] Create `UserTenantResponse` DTO (user-tenant relationships)
- [x] Create `SubscriptionActionRequest` DTO (for PATCH actions)
- [x] Create `DeliveryActionRequest` DTO (for PATCH actions)
- [x] Update `SubscriptionResponse` to include audit fields (created_by, updated_by, created_by_type)
- [x] Update `PlanResponse` to include validation fields and audit fields
- [x] Update `TenantResponse` to include audit fields
- [x] Update `CustomerResponse` to include audit fields
- [x] Create `SubscriptionHistoryResponse` DTO
- [x] Create `PlanValidationRequest` DTO

#### **Phase 3: Service Layer Integration** ✅ **COMPLETED**
- [x] Update `PlansService` to use `PlanValidationService`
  - [x] Inject PlanValidationService dependency
  - [x] Validate plan configuration on create
  - [x] Set default validation flags based on category
  - [x] Update mapToResponse to include new validation fields
- [x] Update `SubscriptionsService` to use `SubscriptionHistoryService`
  - [x] Inject SubscriptionHistoryService dependency
  - [x] Record creation action with metadata
- [x] Update `EcommerceSubscriptionService` with validation
  - [x] Inject PlanValidationService and SubscriptionHistoryService
  - [x] Validate subscription requests against plan rules
  - [x] Validate base plan if provided
  - [x] Record creation and product update actions
  - [x] Reject invalid product combinations based on plan category

#### **Phase 4: Admin APIs & Subscription Management** ✅ **COMPLETED**
- [x] Update `SubscriptionManagementService` with history tracking
  - [x] Inject SubscriptionHistoryService
  - [x] Record pause action with reason and performer
  - [x] Record resume action with performer
  - [x] Record cancellation with reason (immediate or end-of-period)
- [x] Create `AdminSubscriptionHistoryController`
  - [x] GET /api/admin/subscriptions/{id}/history - Paginated history
  - [x] GET /api/admin/subscriptions/{id}/history/all - Complete history
  - [x] Return SubscriptionHistoryResponse DTOs with metadata

#### **Phase 5: User Management APIs & Tenant Assignment** ✅ **COMPLETED**
- [x] Create V023 migration - Add first_name, last_name, assigned_at fields
  - [x] Add first_name and last_name to users table
  - [x] Migrate existing full_name data
  - [x] Add assigned_at to user_tenants table
  - [x] Create indexes for performance
- [x] Create `AdminUsersController`
  - [x] POST /api/admin/users - Create user with BCrypt password hashing
  - [x] GET /api/admin/users - List users with pagination and filters
  - [x] GET /api/admin/users/{id} - Get user details
  - [x] PATCH /api/admin/users/{id} - Update user information
  - [x] POST /api/admin/users/{id}/suspend - Suspend user account
  - [x] POST /api/admin/users/{id}/activate - Activate user account
  - [x] DELETE /api/admin/users/{id} - Soft delete user
- [x] Create `AdminUserTenantsController`
  - [x] POST /api/admin/user-tenants - Assign user to tenant with role
  - [x] GET /api/admin/user-tenants/user/{userId} - Get user's tenants
  - [x] GET /api/admin/user-tenants/tenant/{tenantId} - Get tenant's users
  - [x] PATCH /api/admin/user-tenants/{id} - Update user's role in tenant
  - [x] DELETE /api/admin/user-tenants/{id} - Remove user from tenant
- [x] Create User Management DTOs
  - [x] UserResponse - User details with first/last name
  - [x] CreateUserRequest - Validated user creation request
  - [x] UpdateUserRequest - Partial update request
  - [x] UserTenantResponse - Assignment details with denormalized data

#### **Phase 6: Testing & Validation** ⚠️ **PARTIALLY COMPLETE** (85% API Coverage)

**Current Status**: 34 test files, ~135 integration tests, 85% API coverage

**✅ Completed Tests:**
- [x] Integration tests for subscription lifecycle (SubscriptionLifecycleTest - 5 tests)
- [x] Integration tests for plan validation rules (PlanCategoryValidationTest - 4 tests)
- [x] Integration tests for subscription history (SubscriptionHistoryTest - 4 tests)
- [x] Integration tests for user management (UserManagementTest - 6 tests)
- [x] Integration tests for customer self-service (CustomerSelfServiceTest - 11 tests)
- [x] Integration tests for API client auth (ApiClientAuthenticationTest - 4 tests)
- [x] 15 scenario tests covering end-to-end flows

**❌ Critical Test Gaps (Production-Blocking):**
- [ ] **AdminApiClientsController** - 0 tests (5 endpoints untested) 🔴 CRITICAL
- [ ] **AdminUserTenantsController** - 0 tests (5 endpoints untested) 🔴 CRITICAL
- [ ] **AdminUsersController** - Partial coverage (missing suspend/activate/delete tests) 🔴 CRITICAL
- [ ] **AdminSubscriptionHistoryController** - Partial coverage (missing pagination tests) 🟡 HIGH

**🟡 Medium Priority Gaps:**
- [ ] Unit tests for PlanValidationService (only integration tests exist)
- [ ] Unit tests for SubscriptionHistoryService (only integration tests exist)
- [ ] Unit tests for SignatureService (HMAC verification)
- [ ] Unit tests for NonceCache (replay prevention)
- [ ] Unit tests for RateLimiter (sliding window algorithm)
- [ ] Worker module tests (0 tests - task handlers, job scheduling, renewals)

**🟢 Low Priority Gaps:**
- [ ] Load tests for admin APIs
- [ ] Performance benchmarks
- [ ] Security attack tests (replay, tampering, SQL injection)

**See TESTING.md for complete test coverage analysis and roadmap**

#### **Phase 7: Additional Features** ⏳
- [ ] Create `AdminSubscriptionsController` for advanced subscription management
  - [ ] PATCH /api/admin/subscriptions/{id}/plan - Change plan
  - [ ] PATCH /api/admin/subscriptions/{id}/payment-method - Update payment
  - [ ] PATCH /api/admin/subscriptions/{id}/shipping - Update shipping address
  - [ ] POST /api/admin/subscriptions/{id}/archive - Archive subscription
- [ ] Create `AdminDeliveriesController`
- [ ] Create `AdminCustomersController`

#### **Phase 8.1: Unified Subscription Creation** ✅
**Status**: 100% Complete - All tasks done, deprecated code removed

**Goal**: Merge ecommerce and standard subscription creation into one unified endpoint.

**Tasks:**
- [x] Update `CreateSubscriptionRequest` DTO to include optional `products` array
  - [x] Add `List<ProductItem> products` field (nullable)
  - [x] Add `ShippingAddress shippingAddress` field (nullable)
  - [x] Add `hasProducts()` helper method to check if ecommerce subscription
  - [x] Keep existing fields: planId, customerEmail, customerFirstName, customerLastName, startDate, paymentMethodRef
  
- [x] Merge `EcommerceSubscriptionService` logic into `SubscriptionsService` ✅ **COMPLETE**
  - [x] Add SubscriptionItemsDao and PlanValidationService dependencies
  - [x] Modify createSubscription() to check request.hasProducts()
  - [x] Add product validation logic from EcommerceSubscriptionService
  - [x] Add createSubscriptionItems() helper method for products
  - [x] Add validateProductsAgainstPlan() helper method
  - [x] Handle both simple (no products) and ecommerce (with products) cases
  - [x] Preserve all existing functionality for simple subscriptions
  - [x] Store shipping address if provided
  - [x] Schedule individual product renewals for ecommerce subscriptions
  - [x] Different plan snapshots for simple vs ecommerce subscriptions
  
- [x] Update `SubscriptionsController.createSubscription()` method ✅ **COMPLETE**
  - [x] Updated Swagger documentation to reflect unified endpoint
  - [x] Documented both simple and ecommerce subscription creation
  - [x] No code changes needed - service handles both cases automatically
  
- [x] Remove deprecated `EcommerceSubscriptionsController` ✅ **COMPLETE**
  - [x] Deleted EcommerceSubscriptionsController.java
  - [x] Deleted EcommerceSubscriptionService.java
  - [x] Deleted CreateEcommerceSubscriptionRequest.java DTO
  - [x] Clean codebase - no deprecated code remaining
  
- [x] Update tests ✅ **COMPLETE**
  - [x] Renamed EcommerceSubscriptionTest to UnifiedSubscriptionTest
  - [x] Updated all test methods to use /v1/admin/subscriptions
  - [x] Removed ecommerce-specific terminology, now uses "product-based" and "simple"
  - [x] Migrated all 4 product-based tests to unified endpoint
  - [x] Removed deprecated basePlanId, now uses planId
  - [x] Removed planId from product objects (not needed)
  - [x] Added new test for simple subscription without products
  - [x] Updated all helper methods to use /v1/admin/* URLs
  - [x] Tests now verify both simple and product-based subscription creation
  
- [x] Update Swagger documentation ✅ **COMPLETE**
  - [x] Updated API description to document unified endpoint
  - [x] Documented both simple and product-based subscription creation
  - [x] Added @ExampleObject annotations with two complete examples:
    - Simple SaaS Subscription (no products)
    - Product-Based Subscription (with products array and shipping address)
  - [x] Explained optional products array in operation description
  - [x] Added detailed request body documentation with examples dropdown in Swagger UI

**Implementation Notes:**
- Products array is optional - if null/empty, create simple subscription
- If products provided, validate against plan category and create subscription_items
- Backward compatible - existing simple subscription requests still work
- All deprecated code removed - clean codebase

---

#### **Phase 8.2: Customer Self-Service APIs** ✅
**Status**: 100% Complete - All 8 customer endpoints implemented

**Implemented Endpoints:**
- [x] `CustomerSubscriptionsController` with complete self-service capabilities:
  - [x] GET /v1/customers/me/subscriptions - List my subscriptions
  - [x] GET /v1/customers/me/subscriptions/{id}/dashboard - View subscription dashboard
  - [x] POST /v1/customers/me/subscriptions - Create subscription (self-signup)
  - [x] PATCH /v1/customers/me/subscriptions/{id} - Manage subscription (pause/resume/cancel)
  - [x] GET /v1/customers/me/deliveries - View all upcoming deliveries
  - [x] PATCH /v1/customers/me/deliveries/{id} - Skip or reschedule delivery
  - [x] GET /v1/customers/me/plans - View available plans for self-signup

**Implementation Details:**
- All endpoints use existing services: `SubscriptionsService`, `SubscriptionManagementService`, `DeliveryManagementService`, `PlansService`
- Customer authorization enforced via customerId from request params (to be replaced with JWT in production)
- All actions logged with request IDs for audit trail
- Comprehensive Swagger documentation for all endpoints
- Supports both simple and product-based subscription creation

#### **Phase 9: Authorization & Security** ✅
**Status**: 100% Complete - Role-based authorization implemented

**Implemented Components:**
- [x] Updated `JwtTenantExtractor` to extract user_id, role, and customer_id
  - [x] Added `extractUserRole()` method supporting 'role' and 'roles' claims
  - [x] Added `extractCustomerId()` method for customer-facing APIs
  - [x] Enhanced JWT claims extraction for authorization
  
- [x] Created `UserRole` enum
  - [x] SUPER_ADMIN - Access to all tenants and system-wide operations
  - [x] TENANT_ADMIN - Full access to their tenant's data
  - [x] STAFF - Limited admin access to their tenant
  - [x] CUSTOMER - Access only to their own subscriptions and data
  - [x] Helper methods: `isAdmin()`, `isCustomer()`, `fromString()`
  
- [x] Created `AdminAuthorizationAspect`
  - [x] Intercepts all admin controller methods
  - [x] Verifies user has admin role (SUPER_ADMIN, TENANT_ADMIN, or STAFF)
  - [x] Verifies tenant access (SUPER_ADMIN can access any tenant)
  - [x] Logs all admin actions with user_id and role
  - [x] Throws AccessDeniedException for unauthorized access
  
- [x] Created `CustomerAuthorizationAspect`
  - [x] Intercepts all CustomerSubscriptionsController methods
  - [x] Verifies customers can only access their own resources
  - [x] Compares JWT customer_id with requested customerId
  - [x] Allows admin override for support purposes
  - [x] Logs all customer access attempts

**JWT Claims Structure:**
```json
{
  "sub": "user-uuid",
  "user_id": "user-uuid",
  "tenant_id": "tenant-uuid",
  "role": "TENANT_ADMIN",
  "customer_id": "customer-uuid",
  "email": "user@example.com"
}
```

**Authorization Flow:**
1. JWT authentication extracts token claims
2. Aspect intercepts controller method calls
3. Role verification (admin vs customer)
4. Resource ownership verification (for customers)
5. Tenant access verification (for admins)
6. Access granted or AccessDeniedException thrown

#### **Phase 10: Swagger Documentation** ✅
**Status**: 100% Complete - Comprehensive API documentation with context-specific content

**Implemented Improvements:**
- [x] Updated `OpenApiConfig` tags order with logical workflow organization:
  1. Admin - Tenants (Multi-tenant management)
  2. Admin - Plans (Subscription plan management)
  3. Admin - Users (User management with roles)
  4. Admin - Subscriptions (Subscription lifecycle)
  5. Admin - Deliveries (Delivery management)
  6. Admin - Customers (Customer management)
  7. Admin - Webhooks (Webhook configuration)
  8. Admin - User Tenants (User-tenant assignment)
  9. Admin - API Clients (API client management)
  10. Admin - Subscription History (Audit trail)
  11. Customer - Plans (Browse plans for self-signup)
  12. Customer - Subscriptions (Manage subscriptions - includes dashboard)
  13. Customer - Deliveries (Manage deliveries)

- [x] Context-specific documentation for each API group:
  - **Admin APIs**: Separate title and comprehensive admin-focused documentation
    - Multi-tenant management capabilities
    - Role permissions (SUPER_ADMIN, TENANT_ADMIN, STAFF)
    - Getting started guide for admins
    - Admin JWT claims structure
  - **Customer APIs**: Separate title and customer-focused documentation
    - Self-service capabilities
    - Common use cases with step-by-step flows
    - Security & privacy notes
    - Customer JWT claims structure
    - Integration tips for customer portals
  
- [x] Simplified Customer API structure:
  - Removed redundant "Customer - My Account" tag
  - Consolidated dashboard endpoint into "Customer - Subscriptions"
  - Clean 3-tag structure for better UX
  
- [x] Enhanced tag descriptions with clear, actionable descriptions
  - Each tag now explains its purpose and use case
  - Organized by logical workflow for better developer experience
  
- [x] Added comprehensive request/response examples for PATCH endpoints:
  - **PATCH /v1/customers/me/subscriptions/{id}** - 3 examples:
    - Pause Subscription (with reason)
    - Resume Subscription
    - Cancel Subscription (with feedback)
  - **PATCH /v1/customers/me/deliveries/{id}** - 2 examples:
    - Skip Delivery (with reason)
    - Reschedule Delivery (with new date)
    
- [x] All examples include:
  - Clear names and summaries
  - Detailed descriptions of behavior
  - Realistic request payloads
  - Optional fields (reason, feedback, newDate)

**Documentation Quality:**
- Context-aware documentation that changes based on selected API group
- Clear separation between Admin and Customer APIs with different titles
- Logical tag ordering matching typical workflow
- Interactive examples in Swagger UI dropdown
- Comprehensive descriptions for all operations
- Download links for OpenAPI JSON/YAML specs per group

#### **Phase 11: Testing** ✅
**Status**: 100% Complete - Customer Self-Service tests fully functional with comprehensive bug fixes

**✅ Completed Tests:**
- [x] `CustomerSelfServiceTest` - **ALL 11 TESTS PASSING** ✅
  - [x] Test get available plans for self-signup
  - [x] Test create customer subscription via self-signup
  - [x] Test get customer subscriptions
  - [x] Test get subscription dashboard
  - [x] Test pause subscription
  - [x] Test resume subscription
  - [x] Test cancel subscription
  - [x] Test get customer deliveries
  - [x] Test skip delivery
  - [x] Test reschedule delivery
  - [x] Test customer cannot access other customer's data (authorization)
  
- [x] `AuthorizationTest` - Integration tests for authorization aspects
  - [x] Test SUPER_ADMIN can access any tenant
  - [x] Test TENANT_ADMIN can access own tenant only
  - [x] Test STAFF has limited admin access
  - [x] Test CUSTOMER cannot access admin endpoints
  - [x] Test customer can access own subscriptions only
  - [x] Test admin can access customer endpoints for support
  - [x] Test customer cannot manage other customer's resources
  
- [x] Updated `JwtTestHelper` with role-based token generation
  - [x] Added `generateTokenWithRole()` method
  - [x] Support for role claim (SUPER_ADMIN, TENANT_ADMIN, STAFF, CUSTOMER)
  - [x] Support for customer_id claim

**🐛 Critical Bug Fixed: `performed_by` Constraint Violation**
- **Issue**: All tests failing with `NULL value in column "performed_by" violates not-null constraint`
- **Root Cause**: `SubscriptionsService` was using `UUID.randomUUID()` instead of extracting actual user ID from JWT
- **Solution Implemented**:
  1. Created `UserContext` utility class to extract user info from Spring Security context
  2. Updated `SubscriptionsService` to use `UserContext.getUserId()` for audit trail
  3. Fixed test infrastructure to create actual user records in database before generating JWT tokens
  4. Fixed role mapping: System roles (CUSTOMER) → Tenant roles (MEMBER)
  5. Fixed helper methods to use customer self-service endpoints consistently
  6. Made test data unique to avoid conflicts

**📊 Test Results:**
- **Execution Time**: ~1 minute 16 seconds
- **Success Rate**: 100% (11/11 tests passing)
- **Database**: Testcontainers with PostgreSQL 15
- **Coverage**: Complete customer self-service API coverage

**📝 Documentation:**
- [x] Comprehensive test documentation added to `TESTING.md`
- [x] Bug fix details documented with code examples
- [x] Database schema requirements documented
- [x] Authorization testing rules documented
- [x] Key learnings and best practices documented

**Pending Tests (Optional Enhancements):**
- [ ] Unit tests for `SubscriptionManagementService`
- [ ] Unit tests for `DeliveryManagementService`
- [ ] Unit tests for `PlansService`
- [ ] Performance tests for high-volume scenarios
- [ ] Tests for subscription modification (plan changes, quantity updates)
- [ ] Tests for payment method updates

#### **Phase 12: Cleanup & Migration** ⏳
- [ ] Mark old controllers as @Deprecated
- [ ] Add migration guide
- [ ] Remove old controllers after migration

---

### 📦 Unified Subscription Creation

**Decision**: Merge `/v1/admin/subscriptions` and `/v1/admin/subscriptions/ecommerce` into ONE unified endpoint.

**Rationale:**
- Simpler API - one endpoint instead of two
- Flexible - products array is optional
- Backward compatible - simple subscriptions still work
- Cleaner codebase - one service handles both cases

**POST /v1/admin/subscriptions - Unified Request Format:**

```json
// Simple SaaS Subscription (no products)
{
  "planId": "uuid-plan-123",
  "customerEmail": "john@example.com",
  "customerFirstName": "John",
  "customerLastName": "Doe",
  "startDate": "2026-03-01T00:00:00Z",
  "paymentMethodRef": "pm_stripe_123"
}

// Ecommerce Subscription (with products)
{
  "planId": "uuid-plan-456",
  "customerEmail": "jane@example.com",
  "customerFirstName": "Jane",
  "customerLastName": "Smith",
  "products": [
    {
      "productId": "coffee-sku-001",
      "productName": "Premium Coffee Beans",
      "quantity": 2,
      "unitPriceCents": 1599,
      "currency": "USD"
    },
    {
      "productId": "tea-sku-002",
      "productName": "Organic Green Tea",
      "quantity": 1,
      "unitPriceCents": 999,
      "currency": "USD"
    }
  ],
  "shippingAddress": {
    "line1": "123 Main St",
    "city": "San Francisco",
    "state": "CA",
    "postalCode": "94102",
    "country": "US"
  },
  "paymentMethodRef": "pm_stripe_456"
}
```

**Implementation:**
- `CreateSubscriptionRequest` DTO includes optional `products` array
- `SubscriptionsService` checks if products exist:
  - If empty/null → Simple subscription (plan price applies)
  - If provided → Ecommerce subscription (product-level pricing)
- Merge `EcommerceSubscriptionService` logic into `SubscriptionsService`
- Remove `EcommerceSubscriptionsController` entirely

**Migration:**
- Mark `POST /v1/admin/subscriptions/ecommerce` as @Deprecated
- Add migration guide for clients
- Remove deprecated endpoint after migration period

---

### 🎬 PATCH Action Examples

**Subscription Actions:**
```json
{"action": "PAUSE", "reason": "...", "performedBy": "admin:john@company.com"}
{"action": "RESUME", "performedBy": "customer"}
{"action": "CANCEL", "reason": "...", "cancelAtPeriodEnd": true}
{"action": "ARCHIVE", "reason": "Cleanup"}
{"action": "CHANGE_PLAN", "newPlanId": "uuid", "prorationBehavior": "CREATE_PRORATIONS"}
{"action": "UPDATE_PAYMENT", "paymentMethodRef": "pm_123"}
{"action": "UPDATE_SHIPPING", "shippingAddress": {...}}
{"action": "UPDATE_PRODUCTS", "products": [...]}
{"action": "UPDATE_METADATA", "metadata": {...}}
```

**Delivery Actions:**
```json
{"action": "CANCEL", "reason": "...", "performedBy": "customer"}
{"action": "RESCHEDULE", "newDeliveryDate": "2026-03-15T00:00:00Z"}
{"action": "UPDATE_TRACKING", "trackingNumber": "...", "carrier": "UPS"}
{"action": "MARK_DELIVERED", "deliveredAt": "2026-03-10T14:30:00Z"}
```

---

### 📈 Success Criteria

- [ ] User management system fully implemented (users, user_tenants tables)
- [ ] All 45 endpoints implemented and tested
- [ ] All database tables have created_by and updated_by audit fields
- [ ] Subscription tracking includes created_by_type (ADMIN/CUSTOMER/SYSTEM)
- [ ] Plan validation working for all 3 plan types
- [ ] Audit trail capturing all changes with user context
- [ ] Admin/Customer authorization working with role-based access
- [ ] No hard deletes (only soft delete/archive)
- [ ] Swagger documentation complete
- [ ] All integration tests passing
- [ ] Old endpoints deprecated and removed

---

### 🚀 Rollout Plan

**Week 1:** User management system + database migrations + jOOQ regeneration  
**Week 2:** Service layer (user management + validation + actions)  
**Week 3:** Update all services to set audit fields (created_by/updated_by)  
**Week 4:** Admin controllers + authorization  
**Week 5:** Customer controllers + authorization  
**Week 6:** Testing + documentation  
**Week 7:** Deprecate old endpoints  
**Week 8:** Remove old code after migration  

---

### 📝 M8 Notes

- Preserve backward compatibility during migration period
- Monitor API usage to ensure smooth transition
- Update client SDKs after API stabilizes
- Consider feature flags for gradual rollout
- **IMPORTANT:** Uncomment .gitignore rules for *.md before committing to GitHub

---

---

## 📊 **Complete Recommendation Summary**

### **Security Architecture Decision:**

**✅ RECOMMENDED: Multi-Tier Authentication Approach**

```
Tier 1 (90% of customers): API Key + HMAC-SHA256 Signing
├─ Best for: Standard e-commerce integrations
├─ Security: Good (with IP whitelisting, rate limiting, anomaly detection)
├─ Complexity: Low
├─ Cost: $0
└─ Time to implement: 2-3 weeks

Tier 2 (5% of customers): OAuth 2.0 + PKCE
├─ Best for: Customer self-service portals, mobile apps
├─ Security: Good (short-lived tokens, refresh tokens)
├─ Complexity: Medium
├─ Cost: $0
└─ Time to implement: 1-2 weeks

Tier 3 (5% of customers): mTLS
├─ Best for: Enterprise, banking, healthcare, compliance
├─ Security: Maximum (private key in HSM, certificate-based)
├─ Complexity: High
├─ Cost: $$ (PKI infrastructure, certificates)
└─ Time to implement: 2-3 weeks
```

### **Why This Approach?**

1. **Pragmatic:** Start with API Key (easy), add OAuth/mTLS later
2. **Flexible:** Different customers can use different auth methods
3. **Secure:** Each tier provides appropriate security for use case
4. **Cost-Effective:** No upfront PKI costs, pay only when needed
5. **Developer-Friendly:** API Key is easiest to integrate
6. **Scalable:** Can support thousands of clients per tenant

### **Key Security Features:**

✅ **Defense in Depth:**
- Layer 1: TLS/HTTPS + API Gateway
- Layer 2: API Client Management
- Layer 3: Multi-tier authentication
- Layer 4: RBAC + tenant isolation
- Layer 5: Anomaly detection + auto-suspension
- Layer 6: Audit logging + forensics

✅ **Replay Attack Prevention:**
- Timestamp validation (5 min window)
- Nonce caching (Redis, 10 min TTL)
- Request signature verification

✅ **Credential Compromise Mitigation:**
- IP whitelisting (optional per client)
- Rate limiting (per client, Redis-backed)
- Anomaly detection (unusual patterns)
- Auto-suspension (high-risk behavior)
- Scope restrictions (granular permissions)

✅ **Monitoring & Response:**
- Real-time security alerts
- Comprehensive audit logs
- Usage analytics per client
- Forensic analysis capabilities

### **Implementation Priority:**

**Week 1-2: Foundation**
1. Phase 0: User Management System
2. Phase 0.5: API Client Management + API Key Auth

**Week 3-4: Core APIs**
3. Phase 1: Plan Validation + Subscription Audit
4. Phase 2-6: Services + Controllers

**Week 5-6: Advanced (Optional)**
5. OAuth 2.0 implementation
6. mTLS support for enterprise

**Week 7-8: Testing & Launch**
7. Comprehensive testing
8. Documentation + client SDKs

### **Total Deliverables:**

- **53 API endpoints** (36 Admin + 11 Customer + 6 Auth/System)
- **3 authentication methods** (API Key, OAuth, mTLS)
- **User management system** (users, roles, permissions)
- **API client management** (client_id, secrets, scopes)
- **Security monitoring** (anomaly detection, alerts, auto-suspension)
- **Audit trail** (created_by, updated_by on all tables)
- **Rate limiting** (per client, Redis-backed)
- **Comprehensive documentation** (Swagger, integration guides)

### **Success Metrics:**

- [ ] 100% of endpoints have authentication
- [ ] 100% of tables have audit fields (created_by, updated_by)
- [ ] 0 hard deletes (only soft delete/archive)
- [ ] < 100ms authentication overhead
- [ ] 99.9% uptime for auth services
- [ ] < 1% false positive rate for anomaly detection
- [ ] < 5 min response time for security incidents

### **Infrastructure Requirements:**

**Required:**
- PostgreSQL (already in use)
- Spring Boot application
- TLS/HTTPS certificates

**Optional:**
- Redis (for better performance on nonce cache & rate limiting)
  - Can use PostgreSQL instead (simpler, slightly slower)
- API Gateway (Kong/AWS API Gateway)
  - Can use Spring Cloud Gateway (free, built-in)

### **Cost Estimate:**

**Infrastructure (with PostgreSQL-only approach):**
- PostgreSQL: Already in use ($0 additional)
- TLS certificates: $0 (Let's Encrypt)
- Monitoring/Alerts: $50/month
- **Total: ~$50/month**

**Infrastructure (with Redis for optimal performance):**
- PostgreSQL: Already in use ($0 additional)
- Redis: $50/month
- API Gateway: $100/month (or Spring Cloud Gateway - $0)
- Monitoring/Alerts: $50/month
- **Total: ~$200/month** (scales with usage)

**Development Time:**
- Phase 0: 1 week
- Phase 0.5: 2-3 weeks
- Phase 1-12: 4-5 weeks
- **Total: 7-9 weeks**

---

## 📖 API Client Usage Guide

### **Quick Start: Creating Your First API Client**

#### **Step 1: Create an API Client**

```bash
POST /v1/admin/api-clients
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Shopify Integration",
  "clientType": "SERVER",
  "authMethod": "API_KEY",
  "scopes": [
    "subscriptions:read",
    "subscriptions:write",
    "deliveries:read"
  ],
  "allowedIps": ["203.0.113.10", "203.0.113.11"],
  "rateLimitPerHour": 1000,
  "description": "Production Shopify integration"
}
```

**Response (⚠️ Secret shown ONLY ONCE):**
```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "clientId": "shopify_integration_a1b2c3d4",
  "clientSecret": "sk_f4e3d2c1b0a9f8e7d6c5b4a3f2e1d0c9b8a7f6e5d4c3b2a1f0e9d8c7b6a5f4e3",
  "name": "Shopify Integration",
  "authMethod": "API_KEY",
  "scopes": ["subscriptions:read", "subscriptions:write", "deliveries:read"],
  "status": "ACTIVE",
  "createdAt": "2026-02-09T15:30:00Z"
}
```

**⚠️ IMPORTANT:** Save the `clientSecret` immediately! It cannot be retrieved later.

---

#### **Step 2: Make Authenticated Requests**

**Required Headers:**
- `X-Client-ID`: Your client ID
- `X-Timestamp`: Current Unix timestamp (seconds)
- `X-Nonce`: Unique request identifier (UUID)
- `X-Signature`: HMAC-SHA256 signature

**Example Request:**

```bash
# Generate signature components
HTTP_METHOD="GET"
REQUEST_PATH="/v1/subscriptions"
TIMESTAMP=$(date +%s)
NONCE=$(uuidgen)
BODY_HASH=""  # Empty for GET requests

# Create canonical request
CANONICAL_REQUEST="${HTTP_METHOD}\n${REQUEST_PATH}\n${TIMESTAMP}\n${NONCE}\n${BODY_HASH}"

# Generate HMAC-SHA256 signature
SIGNATURE=$(echo -n "$CANONICAL_REQUEST" | openssl dgst -sha256 -hmac "$CLIENT_SECRET" -binary | base64)

# Make request
curl -X GET "https://api.example.com/v1/subscriptions" \
  -H "X-Client-ID: shopify_integration_a1b2c3d4" \
  -H "X-Timestamp: $TIMESTAMP" \
  -H "X-Nonce: $NONCE" \
  -H "X-Signature: $SIGNATURE"
```

---

### **Security Best Practices**

✅ **DO:**
- Store client secrets in environment variables or secret managers (AWS Secrets Manager, HashiCorp Vault)
- Rotate secrets regularly (every 90 days)
- Use HTTPS for all requests
- Implement exponential backoff for rate limit errors (429)
- Monitor for unusual activity patterns
- Set IP whitelists for production clients

❌ **DON'T:**
- Hard-code secrets in source code
- Share secrets via email or chat
- Reuse nonces (each request must have unique nonce)
- Use timestamps older than 5 minutes
- Ignore rate limit headers

---

### **Rate Limiting**

**Headers in Response:**
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 847
```

**429 Response (Rate Limit Exceeded):**
```json
{
  "error": "Rate limit exceeded"
}
```

**Handling Rate Limits:**
```python
import time

def make_request_with_retry(url, headers, max_retries=3):
    for attempt in range(max_retries):
        response = requests.get(url, headers=headers)
        
        if response.status_code == 429:
            # Exponential backoff
            wait_time = 2 ** attempt
            time.sleep(wait_time)
            continue
            
        return response
    
    raise Exception("Rate limit exceeded after retries")
```

---

### **Error Handling**

**Common Error Responses:**

| Status | Error | Cause | Solution |
|--------|-------|-------|----------|
| 401 | Invalid client credentials | Client ID not found | Verify client ID is correct |
| 401 | Invalid signature | Signature mismatch | Check canonical request format |
| 401 | Request timestamp expired | Timestamp > 5 min old | Use current timestamp |
| 401 | Request already processed | Nonce reused | Generate new UUID for each request |
| 403 | Client is not active | Client suspended/revoked | Contact admin |
| 403 | IP address not allowed | Request from non-whitelisted IP | Add IP to whitelist |
| 429 | Rate limit exceeded | Too many requests | Implement backoff, reduce request rate |

---

### **Managing API Clients**

#### **List All Clients**
```bash
GET /v1/admin/api-clients?tenantId=<tenant-id>&page=0&size=20
```

#### **Get Client Details**
```bash
GET /v1/admin/api-clients/{id}
```

#### **Update Client (Change Status, Scopes, Rate Limits)**
```bash
PATCH /v1/admin/api-clients/{id}
{
  "status": "SUSPENDED",
  "scopes": ["subscriptions:read"],
  "rateLimitPerHour": 500
}
```

#### **Rotate Secret**
```bash
PATCH /v1/admin/api-clients/{id}
{
  "rotateSecret": true
}
```

**Response:**
```json
{
  "message": "API client updated and secret rotated",
  "client": { ... },
  "newClientSecret": "sk_new_secret_here"
}
```

#### **Revoke Client**
```bash
DELETE /v1/admin/api-clients/{id}
```

---

### **Available Scopes**

| Scope | Description |
|-------|-------------|
| `subscriptions:read` | Read subscription data |
| `subscriptions:write` | Create and update subscriptions |
| `subscriptions:delete` | Cancel subscriptions |
| `deliveries:read` | Read delivery data |
| `deliveries:write` | Update delivery schedules |
| `customers:read` | Read customer data |
| `customers:write` | Create and update customers |
| `plans:read` | Read plan data |
| `plans:write` | Create and update plans |
| `invoices:read` | Read invoice data |
| `webhooks:read` | Read webhook configurations |
| `webhooks:write` | Configure webhooks |
| `tenants:read` | Read tenant data |
| `tenants:write` | Manage tenants |
| `admin:*` | Full admin access (use with caution) |

---

### **Client SDKs (Coming Soon)**

**Python:**
```python
from subscription_engine import Client

client = Client(
    client_id="shopify_integration_a1b2c3d4",
    client_secret="sk_...",
    base_url="https://api.example.com"
)

# SDK handles signature generation automatically
subscriptions = client.subscriptions.list()
```

**Node.js:**
```javascript
const SubscriptionEngine = require('@subscription-engine/sdk');

const client = new SubscriptionEngine({
  clientId: 'shopify_integration_a1b2c3d4',
  clientSecret: 'sk_...',
  baseUrl: 'https://api.example.com'
});

// SDK handles signature generation automatically
const subscriptions = await client.subscriptions.list();
```

---

**End of M8 API Redesign Plan**
