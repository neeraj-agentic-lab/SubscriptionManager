# Integration Testing Guide

This guide covers running integration tests with Allure reporting and Jenkins setup.

## 📊 Test Coverage Summary

**Current Status: 38 Test Files, 158 Integration Tests - 95% API Coverage**

**Last Updated**: February 13, 2026

### **✅ EXISTING TEST COVERAGE**

#### **Integration Tests (23 Test Classes)**

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
| `AdminApiClientsCrudTest` | 8 | API client CRUD operations | ✅ Complete |
| `AdminUserTenantsCrudTest` | 5 | User-tenant assignments | ✅ Complete |
| `AdminUsersTest` | 7 | User management (full CRUD) | ✅ Complete |
| `AdminSubscriptionHistoryTest` | 3 | Subscription audit trail | ✅ Complete |

**Subtotal: 141 integration tests**

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

**Subtotal: 17 scenario tests**

**TOTAL EXISTING TESTS: 158 tests across 38 test files**

---

### **✅ PHASE 1 COMPLETE - Admin Controller Tests**

#### **🎉 ALL CRITICAL ADMIN TESTS COMPLETED (February 13, 2026)**

| Controller | Endpoints | Tests | Status | Completion |
|-----------|-----------|-------|--------|------------|
| `AdminApiClientsController` | 5 | ✅ 8 | **COMPLETE** | 100% |
| `AdminUserTenantsController` | 5 | ✅ 5 | **COMPLETE** | 100% |
| `AdminUsersController` | 7 | ✅ 7 | **COMPLETE** | 100% |
| `AdminSubscriptionHistoryController` | 2 | ✅ 3 | **COMPLETE** | 100% |

**Test Coverage Details:**

1. **AdminApiClientsCrudTest** (8 tests) ✅
   - ✅ Create API Client with API_KEY auth method
   - ✅ Create API Client with OAUTH auth method
   - ✅ Create API Client with MTLS auth method
   - ✅ List API Clients with pagination
   - ✅ Get API Client by ID
   - ✅ Update API Client and Rotate Secret
   - ✅ Delete (Revoke) API Client
   - ✅ Get Non-Existent API Client Returns 404

2. **AdminUserTenantsCrudTest** (5 tests) ✅
   - ✅ Assign User to Tenant
   - ✅ Prevent Duplicate User-Tenant Assignment (409 conflict)
   - ✅ Get User's Tenants (list all tenants for a user)
   - ✅ Get Tenant's Users (list all users for a tenant)
   - ✅ Update User Role and Remove Assignment

3. **AdminUsersTest** (7 tests) ✅
   - ✅ Create User
   - ✅ Prevent Duplicate Email (409 conflict)
   - ✅ Get User by ID
   - ✅ List Users with Pagination and Filters
   - ✅ Update User
   - ✅ Suspend and Activate User
   - ✅ Delete User (Soft Delete)

4. **AdminSubscriptionHistoryTest** (3 tests) ✅
   - ✅ Get Subscription History with Pagination
   - ✅ Get All Subscription History
   - ✅ Verify History Endpoint Pagination Parameters

---

### **🟡 REMAINING TEST GAPS** (Non-Blocking)

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
| PlansController | ✅ PlanManagementTest (11 tests) | **EXCELLENT** |
| SubscriptionsController | ✅ Multiple test classes | **EXCELLENT** |
| CustomerSubscriptionsController | ✅ CustomerSelfServiceTest (11 tests) | **EXCELLENT** |
| CustomersController | ✅ CustomerManagementTest (6 tests) | **EXCELLENT** |
| TenantsController | ✅ TenantManagementTest (8 tests) | **EXCELLENT** |
| DeliveryController | ✅ DeliveryManagementTest (4 tests) | **EXCELLENT** |
| WebhooksController | ✅ WebhookDeliveryTest (6 tests) | **EXCELLENT** |
| SubscriptionManagementController | ✅ Multiple tests | **EXCELLENT** |
| AdminApiClientsController | ✅ AdminApiClientsCrudTest (8 tests) | **EXCELLENT** |
| AdminSubscriptionHistoryController | ✅ AdminSubscriptionHistoryTest (3 tests) | **EXCELLENT** |
| AdminUsersController | ✅ AdminUsersTest (7 tests) | **EXCELLENT** |
| AdminUserTenantsController | ✅ AdminUserTenantsCrudTest (5 tests) | **EXCELLENT** |

---

### **🎯 Overall Coverage Estimate**

- **API Endpoints**: ~95% covered (tested 35+ of 35+ endpoints) ✅
- **Service Layer**: ~60% covered (integration only, no unit tests)
- **Worker Module**: ~0% covered (no tests)
- **Security**: ~70% covered (auth yes, attack vectors partial)
- **Performance**: ~0% covered (no load tests)
- **Functional Tests**: FitNesse module available for acceptance testing

---

## 🎯 Test Priority Roadmap

### **Phase 1: Critical Admin Controller Tests** ✅ COMPLETE

**Completion Date**: February 13, 2026  
**Tests Added**: 23 new integration tests

| Test Class | Tests | Endpoints Covered | Status |
|-----------|-------|-------------------|--------|
| `AdminApiClientsCrudTest` | 8 | API client CRUD operations | ✅ Complete |
| `AdminUserTenantsCrudTest` | 5 | User-tenant management | ✅ Complete |
| `AdminUsersTest` | 7 | Complete user management | ✅ Complete |
| `AdminSubscriptionHistoryTest` | 3 | History pagination & filtering | ✅ Complete |

**Result**: All critical admin endpoints now have comprehensive test coverage. Production-ready.

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

1. ✅ **Week 1-2**: Phase 1 (Critical admin tests) - **COMPLETE** (February 13, 2026)
2. **Week 3**: Phase 2 (Service unit tests) - Improves test suite quality
3. **Week 4**: Phase 3 (Worker tests) - Critical for billing operations
4. **Week 5**: Phase 4 (Security & performance) - Production hardening

**Total Estimated Effort**: 3 weeks remaining for complete test coverage

---

## 🎭 FitNesse Functional Testing

### **Overview**

**Status**: ✅ Module Created (February 13, 2026)  
**Location**: `apps/fitnesse-tests/`  
**Purpose**: Acceptance testing and functional flow validation

FitNesse provides wiki-based acceptance testing for business stakeholders to write and execute tests in plain language.

### **Module Structure**

```
apps/fitnesse-tests/
├── build.gradle                           # FitNesse dependencies
├── README.md                              # Complete documentation
├── src/main/java/
│   ├── FitNesseTestApplication.java       # Spring Boot application
│   ├── config/
│   │   ├── FitNesseConfiguration.java     # Auto-configuration
│   │   └── FitNesseProperties.java        # Configuration properties
│   ├── server/
│   │   └── FitNesseServer.java            # Server wrapper
│   ├── fixtures/
│   │   ├── SubscriptionFixture.java       # Subscription test fixture
│   │   └── PlanFixture.java               # Plan test fixture
│   └── util/
│       └── ApiClient.java                 # REST API client
└── FitNesseRoot/                          # Wiki test pages
    ├── FrontPage/                         # Home page
    └── SubscriptionTests/                 # Example tests
```

### **Quick Start**

**Start FitNesse Server:**
```bash
./gradlew :apps:fitnesse-tests:bootRun
```

**Access FitNesse Wiki:**
```
http://localhost:9090
```

### **Configuration**

```yaml
fitnesse:
  enabled: true                    # Enable/disable FitNesse
  port: 9090                       # FitNesse server port
  api:
    base-url: http://localhost:8080/api
```

### **Available Test Fixtures**

#### **SubscriptionFixture**
Methods for testing subscription flows:
- `setTenantId(String)` - Set tenant context
- `setCustomerId(String)` - Set customer ID
- `setPlanId(String)` - Set plan ID
- `createSubscription()` - Create new subscription
- `getSubscription(String)` - Retrieve subscription
- `cancelSubscription()` - Cancel subscription
- `statusIs(String)` - Verify status
- `hasNextBillingDate()` - Check billing date

#### **PlanFixture**
Methods for testing plan management:
- `createPlan(String, String, double)` - Create plan
- `getPlan(String)` - Retrieve plan
- `planName()` - Get plan name
- `planPrice()` - Get plan price

### **Example Test**

```
!define TEST_SYSTEM {slim}

!|Subscription Fixture|
|set tenant id|test-tenant-001|
|set customer id|test-customer-001|
|set plan id|plan-basic-monthly|
|create subscription|true|
|status is|ACTIVE|
|has next billing date|true|
```

### **Use Cases**

1. **Business Acceptance Testing**
   - Non-technical stakeholders can write tests
   - Plain language test scenarios
   - Immediate feedback on business rules

2. **Regression Testing**
   - Automated functional flow validation
   - End-to-end scenario coverage
   - Integration with CI/CD pipelines

3. **Documentation**
   - Living documentation of features
   - Test pages serve as specifications
   - Always up-to-date with implementation

### **Enable/Disable**

**Disable for Production:**
```yaml
fitnesse:
  enabled: false
```

Or simply don't deploy the `fitnesse-tests` module.

### **Benefits**

- ✅ Business-readable test scenarios
- ✅ Wiki-based test management
- ✅ Standalone module (no production impact)
- ✅ REST API integration
- ✅ Spring Boot dependency injection
- ✅ Configurable enable/disable

### **Documentation**

See `apps/fitnesse-tests/README.md` for:
- Complete setup instructions
- Writing custom fixtures
- Creating test pages
- Best practices
- Troubleshooting guide

---

### Features Covered

✅ **Subscription Management**
- Complete lifecycle (create, pause, resume, cancel)
- Plan modifications and upgrades
- Quantity and address updates
- Payment method changes
- Scheduled task management

✅ **Delivery Operations**
- Delivery cancellation with reasons
- Outbox event emission
- Database persistence verification
- Status validation

✅ **Webhook System**
- Endpoint registration and management
- Event delivery with HMAC signatures
- Retry logic with exponential backoff
- WireMock integration for testing

✅ **Customer Dashboard**
- Subscription listing by customer
- Dashboard view with capabilities
- Authorization enforcement
- Multi-subscription support

✅ **Plan Management**
- Plan creation with various intervals
- Trial period support
- Active/inactive filtering
- Tenant isolation

✅ **Security & Validation**
- JWT authentication
- Multi-tenant isolation
- Input validation
- Error handling
- Customer ownership enforcement
- Concurrent modification handling

## 🎯 Overview

The subscription engine includes **50 comprehensive integration tests** covering all major features and edge cases.

## 🚀 Quick Start

### Prerequisites

- **Docker** installed (for Testcontainers)
- **Java 17+**
- **Gradle** (included via wrapper)

### Run All Tests

```bash
# Run all integration tests
./gradlew :apps:subscription-api:test

# Run with Allure report generation
./gradlew :apps:subscription-api:test allureReport

# Open Allure report in browser
./gradlew :apps:subscription-api:allureServe
```

### Run Specific Test Classes

```bash
# Run subscription lifecycle tests only
./gradlew :apps:subscription-api:test --tests SubscriptionLifecycleTest

# Run delivery management tests only
./gradlew :apps:subscription-api:test --tests DeliveryManagementTest

# Run webhook tests only
./gradlew :apps:subscription-api:test --tests WebhookDeliveryTest
```

### Run Tests by Tag/Feature

```bash
# Run critical tests only
./gradlew :apps:subscription-api:test -Dgroups="critical"

# Run specific feature tests
./gradlew :apps:subscription-api:test --tests "*Lifecycle*"
```

## 📊 Allure Reports

### Local Report Generation

```bash
# Generate report (HTML files in build/reports/allure-report/)
./gradlew :apps:subscription-api:allureReport

# Serve report with live server (opens browser automatically)
./gradlew :apps:subscription-api:allureServe
```

### Report Features

The Allure report includes:
- ✅ **Test execution overview** with pass/fail rates
- 📊 **Graphs and trends** over multiple runs
- 📝 **Detailed test steps** with timing information
- 📎 **HTTP request/response attachments**
- 🔍 **Filtering by feature, severity, status**
- ⚠️ **Flaky test detection**
- 📈 **Historical trends** (when run multiple times)

## 🏗️ Test Architecture

### Test Structure

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

### Key Technologies

- **JUnit 5**: Test framework
- **Testcontainers**: Real PostgreSQL in Docker
- **REST Assured**: API testing DSL
- **Allure**: Beautiful test reporting
- **Awaitility**: Async testing utilities
- **WireMock**: HTTP service mocking
- **AssertJ**: Fluent assertions

### Test Data

Tests use `TestDataFactory` to create:
- Plans, customers, subscriptions
- Delivery instances
- Webhook registrations
- Request payloads

Default test tenant: `5aa82d8e-ebec-432b-b568-ac4ba61bb578`

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

## 🔧 Configuration

### Test Properties

Tests use `application-test.properties` (auto-configured by Testcontainers):
- Database URL: Dynamically set to container
- Flyway migrations: Enabled
- Scheduled tasks: Enabled for webhook testing

### Parallel Execution

Tests run in parallel by default:
```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.config.strategy=dynamic
```

Disable parallel execution:
```bash
./gradlew test -Djunit.jupiter.execution.parallel.enabled=false
```

## 🎨 Jenkins + Allure Setup

### Option 1: Docker Compose (Recommended)

Create `docker-compose.jenkins.yml`:

```yaml
version: '3.8'

services:
  jenkins:
    image: jenkins/jenkins:lts
    ports:
      - "8081:8080"
      - "50000:50000"
    volumes:
      - jenkins_home:/var/jenkins_home
      - /var/run/docker.sock:/var/run/docker.sock
    environment:
      - JAVA_OPTS=-Djenkins.install.runSetupWizard=false

volumes:
  jenkins_home:
```

Start Jenkins:
```bash
docker-compose -f docker-compose.jenkins.yml up -d
```

### Option 2: Standalone Docker

```bash
docker run -d \
  -p 8081:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --name jenkins \
  jenkins/jenkins:lts
```

### Jenkins Configuration

1. **Access Jenkins**: `http://localhost:8081`

2. **Get initial password**:
   ```bash
   docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```

3. **Install plugins**:
   - Allure Plugin
   - Gradle Plugin
   - Git Plugin
   - Pipeline Plugin

4. **Create Pipeline Job**:
   - New Item → Pipeline
   - Configure → Pipeline script from SCM
   - Add repository URL

5. **Jenkinsfile** (create in repo root):

```groovy
pipeline {
    agent any
    
    tools {
        gradle 'Gradle 8.5'
    }
    
    parameters {
        choice(
            name: 'TEST_SUITE',
            choices: ['all', 'subscription', 'delivery', 'webhook'],
            description: 'Which test suite to run'
        )
        choice(
            name: 'ENVIRONMENT',
            choices: ['local', 'staging', 'production'],
            description: 'Target environment'
        )
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                sh './gradlew clean build -x test'
            }
        }
        
        stage('Run Tests') {
            steps {
                script {
                    def testCommand = './gradlew :apps:subscription-api:test allureReport'
                    
                    if (params.TEST_SUITE != 'all') {
                        testCommand += " --tests *${params.TEST_SUITE.capitalize()}*"
                    }
                    
                    sh testCommand
                }
            }
        }
    }
    
    post {
        always {
            allure([
                includeProperties: false,
                jdk: '',
                properties: [],
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'apps/subscription-api/build/allure-results']]
            ])
            
            junit 'apps/subscription-api/build/test-results/test/*.xml'
        }
        
        failure {
            echo 'Tests failed! Check Allure report for details.'
        }
        
        success {
            echo 'All tests passed!'
        }
    }
}
```

### Triggering Tests from Jenkins UI

1. Go to your job
2. Click **"Build with Parameters"**
3. Select test suite and environment
4. Click **"Build"**
5. View progress in console output
6. Click **"Allure Report"** when complete

## 📈 Viewing Reports

### In Jenkins

After build completes:
1. Click build number (e.g., #42)
2. Click **"Allure Report"** in left sidebar
3. Browse test results, graphs, timelines

### Locally

```bash
# Generate and serve report
./gradlew allureServe

# Or generate static report
./gradlew allureReport
open apps/subscription-api/build/reports/allure-report/index.html
```

## 🔍 Debugging Failed Tests

### View Test Logs

```bash
# Gradle output
./gradlew test --info

# Test reports
open apps/subscription-api/build/reports/tests/test/index.html
```

### Common Issues

**Testcontainers fails to start**:
- Ensure Docker is running
- Check Docker has sufficient resources (4GB+ RAM)

**Tests timeout**:
- Webhook tests may take longer (up to 60s for retries)
- Increase timeout in test code if needed

**Database conflicts**:
- Tests use isolated Testcontainers
- Each test class gets fresh database

## 🎯 Best Practices

### Writing New Tests

1. **Extend BaseIntegrationTest**:
   ```java
   class MyNewTest extends BaseIntegrationTest {
       // Your tests here
   }
   ```

2. **Use Allure annotations**:
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

3. **Add steps for clarity**:
   ```java
   @Step("Create subscription")
   private UUID createSubscription() {
       // Implementation
   }
   ```

4. **Attach evidence**:
   ```java
   Allure.addAttachment("Response", "application/json", response.asString());
   ```

### Test Organization

- **One test class per feature area**
- **Clear, descriptive test names**
- **Use helper methods for common operations**
- **Keep tests independent** (no shared state)

---

## 🎬 End-to-End Test Scenarios

Beyond individual API tests, these scenarios validate complete business workflows spanning multiple APIs and services.

### **Category 1: Customer Journey Scenarios**

#### ✅ **Scenario 1.1: New Customer Onboarding Flow**
**Status**: � Implemented  
**Test Class**: `NewCustomerOnboardingScenarioTest`  
**Priority**: P1 (Critical)  
**Business Value**: Validates complete customer acquisition

**Steps**:
1. Create new tenant (company signs up)
2. Create subscription plan (monthly coffee delivery)
3. Customer registers with email
4. Customer creates subscription with payment method
5. Verify subscription is ACTIVE
6. Verify first delivery is scheduled
7. Verify webhook event sent (subscription.created)
8. Verify scheduled renewal task created

**Expected Outcomes**:
- ✓ Customer can subscribe end-to-end
- ✓ Payment captured
- ✓ Delivery scheduled
- ✓ External systems notified via webhook

---

#### ⚪ **Scenario 1.2: Customer Subscription Pause & Resume Journey**
**Status**: � Implemented  
**Test Class**: `PauseResumeJourneyScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests vacation/pause feature

**Steps**:
1. Customer has active subscription
2. Customer views dashboard (sees "can pause")
3. Customer pauses subscription with reason
4. Verify upcoming deliveries cancelled
5. Verify renewal tasks cancelled
6. Wait 30 days (simulated)
7. Customer resumes subscription
8. Verify new deliveries scheduled
9. Verify renewal tasks recreated

**Expected Outcomes**:
- ✓ No deliveries during pause
- ✓ Clean resume with proper scheduling
- ✓ Billing cycle adjusted correctly

---

#### ✅ **Scenario 1.3: Customer Cancellation with Refund**
**Status**: � Implemented  
**Test Class**: `CustomerCancellationScenarioTest`  
**Priority**: P1 (Critical)  
**Business Value**: Tests churn flow

**Steps**:
1. Customer has active subscription with upcoming delivery
2. Customer cancels immediately
3. Verify subscription status = CANCELED
4. Verify pending deliveries cancelled
5. Verify refund initiated
6. Verify webhook sent (subscription.canceled)
7. Verify no future charges scheduled

**Expected Outcomes**:
- ✓ Clean cancellation
- ✓ No orphaned data
- ✓ External systems notified

---

### **Category 2: Subscription Modification Scenarios**

#### ⚪ **Scenario 2.1: Plan Upgrade Mid-Cycle**
**Status**: � Implemented  
**Test Class**: `PlanUpgradeScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests upsell flow

**Steps**:
1. Customer on "Basic Plan" ($29/month)
2. Customer upgrades to "Premium Plan" ($49/month)
3. Verify prorated charge calculated
4. Verify next delivery updated with premium items
5. Verify billing cycle adjusted
6. Verify webhook sent (subscription.modified)

**Expected Outcomes**:
- ✓ Correct proration
- ✓ Seamless upgrade
- ✓ No service interruption

---

#### ⚪ **Scenario 2.2: Address Change Before Delivery**
**Status**: � Implemented  
**Test Class**: `AddressChangeScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests logistics update

**Steps**:
1. Customer has delivery scheduled in 3 days
2. Customer updates shipping address
3. Verify address updated for upcoming delivery
4. Verify past deliveries unchanged
5. Verify external commerce system notified

**Expected Outcomes**:
- ✓ Address change applied correctly
- ✓ No impact on historical data

---

### **Category 3: Delivery Management Scenarios**

#### ⚪ **Scenario 3.1: Delivery Cancellation After Order Placed**
**Status**: � Implemented  
**Test Class**: `DeliveryCancellationAfterOrderScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests late cancellation handling

**Steps**:
1. Delivery scheduled for tomorrow
2. External order already created (external_order_ref exists)
3. Customer attempts to cancel
4. Verify cancellation rejected (too late)
5. Verify proper error message

**Expected Outcomes**:
- ✓ Business rules enforced
- ✓ Clear customer communication

---

#### ⚪ **Scenario 3.2: Bulk Delivery Cancellation on Subscription Cancel**
**Status**: � Implemented  
**Test Class**: `BulkDeliveryCancellationScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests cascade operations

**Steps**:
1. Customer has 5 upcoming deliveries
2. Customer cancels subscription immediately
3. Verify all 5 deliveries cancelled
4. Verify webhook events sent for each
5. Verify external orders cancelled

**Expected Outcomes**:
- ✓ Clean cascade
- ✓ All systems synchronized

---

### **Category 4: Webhook & Integration Scenarios**

#### ✅ **Scenario 4.1: Webhook Retry on Failure**
**Status**: � Implemented  
**Test Class**: `WebhookRetryScenarioTest`  
**Priority**: P1 (Critical)  
**Business Value**: Tests reliability

**Steps**:
1. Register webhook endpoint
2. Configure endpoint to fail (500 error)
3. Trigger subscription event
4. Verify webhook delivery attempted
5. Verify retry with exponential backoff
6. Fix endpoint (return 200)
7. Verify eventual successful delivery
8. Verify HMAC signature valid

**Expected Outcomes**:
- ✓ Automatic retries work
- ✓ Events eventually delivered
- ✓ Signatures valid

---

#### ⚪ **Scenario 4.2: Multiple Webhooks for Same Event**
**Status**: � Implemented  
**Test Class**: `MultipleWebhooksScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests fan-out

**Steps**:
1. Register 3 webhook endpoints
2. All subscribe to "delivery.canceled"
3. Cancel a delivery
4. Verify all 3 webhooks receive event
5. Verify each has unique signature
6. Verify parallel delivery

**Expected Outcomes**:
- ✓ All webhooks notified
- ✓ No duplicate events
- ✓ Performance acceptable

---

#### ⚪ **Scenario 4.3: Webhook Event Filtering**
**Status**: � Implemented  
**Test Class**: `WebhookFilteringScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests selective notifications

**Steps**:
1. Webhook A subscribes to "subscription.*"
2. Webhook B subscribes to "delivery.*"
3. Trigger subscription.created
4. Trigger delivery.canceled
5. Verify Webhook A gets only subscription events
6. Verify Webhook B gets only delivery events

**Expected Outcomes**:
- ✓ Filtering works correctly
- ✓ No unwanted events delivered

---

### **Category 5: Multi-Tenancy Scenarios**

#### ✅ **Scenario 5.1: Tenant Isolation Verification**
**Status**: � Implemented  
**Test Class**: `TenantIsolationScenarioTest`  
**Priority**: P1 (Critical)  
**Business Value**: Critical security test

**Steps**:
1. Create Tenant A with customer and subscription
2. Create Tenant B with customer and subscription
3. Tenant A tries to access Tenant B's subscription
4. Verify 404/403 error
5. Tenant A tries to cancel Tenant B's delivery
6. Verify rejection
7. Verify no data leakage

**Expected Outcomes**:
- ✓ Complete isolation
- ✓ No cross-tenant access
- ✓ Security enforced

---

### **Category 6: Scheduled Task Scenarios**

#### ✅ **Scenario 6.1: Subscription Renewal Processing**
**Status**: � Implemented  
**Test Class**: `SubscriptionRenewalScenarioTest`  
**Priority**: P1 (Critical)  
**Business Value**: Tests recurring billing

**Steps**:
1. Create subscription with renewal in 1 minute
2. Wait for scheduled task to fire
3. Verify renewal processed
4. Verify payment charged
5. Verify new billing cycle started
6. Verify next renewal scheduled
7. Verify webhook sent

**Expected Outcomes**:
- ✓ Automatic renewal works
- ✓ Billing accurate
- ✓ Next cycle scheduled

---

#### ⚪ **Scenario 6.2: Failed Renewal Retry Logic**
**Status**: � Implemented  
**Test Class**: `FailedRenewalRetryScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests payment failure handling

**Steps**:
1. Subscription due for renewal
2. Payment method fails
3. Verify retry scheduled
4. Verify customer notified
5. After 3 failures, verify subscription paused
6. Verify webhook sent (payment.failed)

**Expected Outcomes**:
- ✓ Graceful failure handling
- ✓ Customer communication
- ✓ Subscription protected

---

### **Category 7: Error Recovery Scenarios**

#### ⚪ **Scenario 7.1: Idempotency Key Handling**
**Status**: � Implemented  
**Test Class**: `IdempotencyKeyScenarioTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests duplicate prevention

**Steps**:
1. Create subscription with idempotency key
2. Retry same request with same key
3. Verify same subscription returned
4. Verify no duplicate created
5. Use different key
6. Verify new subscription created

**Expected Outcomes**:
- ✓ Idempotency works
- ✓ No duplicates

---

#### ⚪ **Scenario 7.2: Concurrent Subscription Modifications**
**Status**: � Implemented  
**Test Class**: `ConcurrentModificationScenarioTest`  
**Priority**: P3 (Nice to Have)  
**Business Value**: Tests race conditions

**Steps**:
1. Create subscription
2. Simultaneously:
   - Pause from one session
   - Modify from another session
   - Cancel from third session
3. Verify only one succeeds
4. Verify consistent state
5. Verify no data corruption

**Expected Outcomes**:
- ✓ Optimistic locking works
- ✓ Data consistency maintained

---

### **Category 8: User Management & Multi-Tenant Access** (M8 Features)

#### ✅ **Scenario 8.1: User Onboarding and Role Assignment**
**Status**: ✅ Implemented  
**Test Class**: `AdminUsersTest`, `AdminUserTenantsCrudTest`  
**Priority**: P1 (Critical)  
**Business Value**: Tests user management and access control

**Steps**:
1. Admin creates new user account
2. Verify user created with BCrypt password hash
3. Assign user to Tenant A with ADMIN role
4. Assign same user to Tenant B with VIEWER role
5. Verify user can access both tenants
6. Verify role-based permissions enforced
7. Update user role in Tenant A to VIEWER
8. Verify permission changes applied

**Expected Outcomes**:
- ✓ User created successfully
- ✓ Multi-tenant access works
- ✓ Role-based permissions enforced
- ✓ Role updates applied correctly

**API Endpoints Covered**:
- `POST /api/admin/users` - Create user
- `POST /api/admin/user-tenants` - Assign to tenant
- `GET /api/admin/user-tenants/user/{userId}` - Get user's tenants
- `PATCH /api/admin/user-tenants/{id}` - Update role

---

#### ✅ **Scenario 8.2: User Suspension and Reactivation**
**Status**: ✅ Implemented  
**Test Class**: `AdminUsersTest`  
**Priority**: P1 (Critical)  
**Business Value**: Tests account lifecycle management

**Steps**:
1. Create active user account
2. User successfully authenticates
3. Admin suspends user account
4. Verify user cannot authenticate
5. Verify user cannot access any APIs
6. Admin reactivates user account
7. Verify user can authenticate again
8. Verify user can access APIs

**Expected Outcomes**:
- ✓ Suspended users blocked from access
- ✓ Reactivation restores access
- ✓ Security enforced

**API Endpoints Covered**:
- `POST /api/admin/users/{id}/suspend` - Suspend user
- `POST /api/admin/users/{id}/activate` - Activate user

---

#### ✅ **Scenario 8.3: API Client Authentication Flow**
**Status**: ✅ Implemented  
**Test Class**: `ApiClientAuthenticationTest`, `AdminApiClientsCrudTest`  
**Priority**: P1 (Critical)  
**Business Value**: Tests secure API client authentication

**Steps**:
1. Admin creates API client with API_KEY auth method
2. Generate HMAC-SHA256 signature for request
3. Make authenticated API request
4. Verify request succeeds with valid signature
5. Attempt request with invalid signature
6. Verify request rejected (401)
7. Attempt replay attack with old nonce
8. Verify replay blocked
9. Test rate limiting enforcement

**Expected Outcomes**:
- ✓ HMAC authentication works
- ✓ Invalid signatures rejected
- ✓ Replay attacks prevented
- ✓ Rate limiting enforced

**API Endpoints Covered**:
- `POST /v1/admin/api-clients` - Create API client
- `PATCH /v1/admin/api-clients/{id}` - Rotate secret
- All API endpoints (with HMAC auth)

---

#### ✅ **Scenario 8.4: API Client Secret Rotation**
**Status**: ✅ Implemented  
**Test Class**: `AdminApiClientsCrudTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests security best practices

**Steps**:
1. Create API client with secret
2. Make successful authenticated request
3. Rotate API client secret
4. Verify old secret no longer works
5. Verify new secret works
6. Verify all existing requests with old secret fail

**Expected Outcomes**:
- ✓ Secret rotation works
- ✓ Old secrets invalidated immediately
- ✓ New secrets work immediately
- ✓ No downtime during rotation

**API Endpoints Covered**:
- `PATCH /v1/admin/api-clients/{id}` - Rotate secret

---

#### ✅ **Scenario 8.5: Subscription History Audit Trail**
**Status**: ✅ Implemented  
**Test Class**: `SubscriptionHistoryTest`, `AdminSubscriptionHistoryTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests compliance and audit requirements

**Steps**:
1. Create new subscription (action: CREATE)
2. Pause subscription (action: PAUSE)
3. Resume subscription (action: RESUME)
4. Modify subscription (action: MODIFY)
5. Cancel subscription (action: CANCEL)
6. Retrieve subscription history
7. Verify all actions recorded with metadata
8. Verify actor tracking (CUSTOMER, ADMIN, SYSTEM)
9. Verify timestamps accurate
10. Test pagination of history

**Expected Outcomes**:
- ✓ Complete lifecycle tracked
- ✓ All actions recorded with metadata
- ✓ Actor attribution correct
- ✓ Pagination works

**API Endpoints Covered**:
- `GET /api/admin/subscriptions/{id}/history` - Get history
- All subscription modification endpoints

---

#### ✅ **Scenario 8.6: Plan Category Validation**
**Status**: ✅ Implemented  
**Test Class**: `PlanCategoryValidationTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests business rule enforcement

**Steps**:
1. Create DIGITAL plan (no shipping required)
2. Attempt to create subscription with shipping address
3. Verify validation error
4. Create PRODUCT_BASED plan (shipping required)
5. Attempt to create subscription without shipping
6. Verify validation error
7. Create HYBRID plan
8. Verify both digital and physical items allowed

**Expected Outcomes**:
- ✓ DIGITAL plans validated correctly
- ✓ PRODUCT_BASED plans validated correctly
- ✓ HYBRID plans support both types
- ✓ Business rules enforced

**API Endpoints Covered**:
- `POST /v1/admin/plans` - Create plan with category
- `POST /v1/admin/subscriptions` - Create subscription (validated)

---

#### ✅ **Scenario 8.7: User Tenant Removal and Access Revocation**
**Status**: ✅ Implemented  
**Test Class**: `AdminUserTenantsCrudTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests access control cleanup

**Steps**:
1. User assigned to Tenant A and Tenant B
2. User can access resources in both tenants
3. Admin removes user from Tenant A
4. Verify user can no longer access Tenant A resources
5. Verify user can still access Tenant B resources
6. Verify no orphaned data

**Expected Outcomes**:
- ✓ Access revocation immediate
- ✓ Other tenant access unaffected
- ✓ Clean removal

**API Endpoints Covered**:
- `DELETE /api/admin/user-tenants/{id}` - Remove assignment

---

#### ✅ **Scenario 8.8: Duplicate Prevention Across User Management**
**Status**: ✅ Implemented  
**Test Class**: `AdminUsersTest`, `AdminUserTenantsCrudTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests data integrity

**Steps**:
1. Create user with email "test@example.com"
2. Attempt to create another user with same email
3. Verify 409 conflict error
4. Assign user to Tenant A
5. Attempt to assign same user to Tenant A again
6. Verify 409 conflict error
7. Verify duplicate prevention works

**Expected Outcomes**:
- ✓ Duplicate emails prevented
- ✓ Duplicate tenant assignments prevented
- ✓ Clear error messages

**API Endpoints Covered**:
- `POST /api/admin/users` - Create user (duplicate check)
- `POST /api/admin/user-tenants` - Assign to tenant (duplicate check)

---

### **Scenario Implementation Priority**

#### **Priority 1 (Must Have) - 8 Scenarios**
- ✅ New Customer Onboarding Flow
- ✅ Customer Cancellation with Refund
- ✅ Tenant Isolation Verification
- ✅ Webhook Retry on Failure
- ✅ Subscription Renewal Processing
- ✅ User Onboarding and Role Assignment (M8)
- ✅ User Suspension and Reactivation (M8)
- ✅ API Client Authentication Flow (M8)

#### **Priority 2 (Should Have) - 13 Scenarios**
- ✅ Customer Pause & Resume Journey
- ✅ Plan Upgrade Mid-Cycle
- ✅ Address Change Before Delivery
- ✅ Delivery Cancellation After Order Placed
- ✅ Bulk Delivery Cancellation
- ✅ Multiple Webhooks for Same Event
- ✅ Webhook Event Filtering
- ✅ Failed Renewal Retry Logic
- ✅ Idempotency Key Handling
- ✅ API Client Secret Rotation (M8)
- ✅ Subscription History Audit Trail (M8)
- ✅ Plan Category Validation (M8)
- ✅ User Tenant Removal and Access Revocation (M8)
- ✅ Duplicate Prevention Across User Management (M8)

#### **Priority 3 (Nice to Have) - 1 Scenario**
- ✅ Concurrent Subscription Modifications

**Total Scenarios**: 22  
**Implemented**: 22 (100% Complete! 🎉🎉🎉)  
**Remaining**: 0

### 🏆 Achievement Unlocked: Complete Scenario Coverage Including M8 Features!

---

## 📚 Additional Resources

- [Allure Documentation](https://docs.qameta.io/allure/)
- [Testcontainers Guide](https://www.testcontainers.org/)
- [REST Assured](https://rest-assured.io/)
- [JUnit 5](https://junit.org/junit5/)

## 🚦 CI/CD Integration

### GitHub Actions Example

```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Tests
        run: ./gradlew :apps:subscription-api:test allureReport
      
      - name: Upload Allure Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: allure-results
          path: apps/subscription-api/build/allure-results/
      
      - name: Upload Allure Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: allure-report
          path: apps/subscription-api/build/reports/allure-report/
```

---

## 🆕 New Feature Test Scenarios (Phases 0-5)

The following test scenarios cover newly implemented features from development Phases 0 through 5.

---

## 📋 Test Implementation Checklist

### **Quick Summary**

**Total New Tests Required**: 20 scenarios across 5 test classes  
**Priority 1 (Critical)**: 9 scenarios - **Start Here!**  
**Priority 2 (Important)**: 11 scenarios  
**Estimated Implementation Time**: 2-3 weeks

---

### **✅ Implementation Checklist - Priority 1 (Critical)**

**All 9 critical scenarios completed! ✅**

- [x] **1. API Client HMAC Authentication** (`ApiClientAuthenticationTest.testApiClientHmacAuthentication()`)
  - Create API client, generate HMAC signature, test valid/invalid signatures, test timestamp expiry
  
- [x] **2. Nonce Replay Prevention** (`ApiClientAuthenticationTest.testNonceReplayPrevention()`)
  - Test nonce tracking, verify duplicate nonces rejected, verify cache expiry
  
- [x] **3. Plan Category DIGITAL Validation** (`PlanCategoryValidationTest.testDigitalPlanValidation()`)
  - Test DIGITAL plan rules: no products allowed, base price required
  
- [x] **4. Plan Category PRODUCT_BASED Validation** (`PlanCategoryValidationTest.testProductBasedPlanValidation()`)
  - Test PRODUCT_BASED plan rules: products required, pricing from products only
  
- [x] **5. Plan Category HYBRID Validation** (`PlanCategoryValidationTest.testHybridPlanValidation()`)
  - Test HYBRID plan rules: base price + optional products, combined pricing
  
- [x] **6. Subscription History Complete Lifecycle** (`SubscriptionHistoryTest.testSubscriptionHistoryCompleteLifecycle()`)
  - Test full lifecycle tracking: CREATE → PAUSE → RESUME → PLAN_CHANGE → CANCEL
  
- [x] **7. User Creation & Authentication** (`UserManagementTest.testUserCreationAndAuthentication()`)
  - Test user creation, BCrypt password hashing, email uniqueness, user listing
  
- [x] **8. User-Tenant Assignment** (`UserManagementTest.testUserTenantAssignment()`)
  - Test multi-tenant assignment, role tracking, assigned_at timestamps
  
- [x] **9. End-to-End Plan Validation & History** (`CrossFeatureIntegrationTest.testEndToEndPlanValidationWithHistory()`)
  - Test integration: plan validation + subscription creation + history tracking

---

### **✅ Implementation Checklist - Priority 2 (Important)**

**All 11 important scenarios completed! ✅**

- [x] **10. API Client Secret Rotation** (`ApiClientAuthenticationTest.testApiClientSecretRotation()`)
- [x] **11. API Client Rate Limiting** (`ApiClientAuthenticationTest.testApiClientRateLimiting()`)
- [x] **12. Plan Validation on Update** (`PlanCategoryValidationTest.testPlanValidationOnUpdate()`)
- [x] **13. Subscription History Metadata** (`SubscriptionHistoryTest.testSubscriptionHistoryMetadata()`)
- [x] **14. Subscription History Pagination** (`SubscriptionHistoryTest.testSubscriptionHistoryPagination()`)
- [x] **15. Subscription History Actor Tracking** (`SubscriptionHistoryTest.testSubscriptionHistoryActorTracking()`)
- [x] **16. User Suspend & Activate** (`UserManagementTest.testUserSuspendAndActivate()`)
- [x] **17. User-Tenant Role Updates** (`UserManagementTest.testUserTenantRoleUpdate()`)
- [x] **18. User-Tenant Removal** (`UserManagementTest.testUserTenantRemoval()`)
- [x] **19. User Listing & Filtering** (`UserManagementTest.testUserListingAndFiltering()`)
- [x] **20. API Client with User Management** (`CrossFeatureIntegrationTest.testApiClientUserManagement()`)

---

### **📦 Test Classes to Create**

Create these 5 new test classes in `apps/subscription-api/src/test/java/integration/`:

#### **1. ApiClientAuthenticationTest.java** (4 test methods)
```java
@Epic("API Client Security")
@Feature("HMAC Authentication")
class ApiClientAuthenticationTest extends BaseIntegrationTest {
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("API Client HMAC Authentication - Valid Signature")
    void testApiClientHmacAuthentication() {
        // Test scenarios 1 & 2
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Nonce Replay Attack Prevention")
    void testNonceReplayPrevention() {
        // Test scenario 2
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("API Client Secret Rotation")
    void testApiClientSecretRotation() {
        // Test scenario 10
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("API Client Rate Limiting")
    void testApiClientRateLimiting() {
        // Test scenario 11
    }
}
```

#### **2. PlanCategoryValidationTest.java** (4 test methods)
```java
@Epic("Plan Management")
@Feature("Plan Category Validation")
class PlanCategoryValidationTest extends BaseIntegrationTest {
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("DIGITAL Plan Validation Rules")
    void testDigitalPlanValidation() {
        // Test scenario 3
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("PRODUCT_BASED Plan Validation Rules")
    void testProductBasedPlanValidation() {
        // Test scenario 4
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("HYBRID Plan Validation Rules")
    void testHybridPlanValidation() {
        // Test scenario 5
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Plan Category Update Validation")
    void testPlanValidationOnUpdate() {
        // Test scenario 12
    }
}
```

#### **3. SubscriptionHistoryTest.java** (4 test methods)
```java
@Epic("Subscription Management")
@Feature("Subscription History & Audit Trail")
class SubscriptionHistoryTest extends BaseIntegrationTest {
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Subscription History - Complete Lifecycle Tracking")
    void testSubscriptionHistoryCompleteLifecycle() {
        // Test scenario 6
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Subscription History - Metadata Tracking")
    void testSubscriptionHistoryMetadata() {
        // Test scenario 13
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Subscription History - Pagination")
    void testSubscriptionHistoryPagination() {
        // Test scenario 14
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Subscription History - Actor Tracking")
    void testSubscriptionHistoryActorTracking() {
        // Test scenario 15
    }
}
```

#### **4. UserManagementTest.java** (6 test methods)
```java
@Epic("User Management")
@Feature("User & Tenant Management")
class UserManagementTest extends BaseIntegrationTest {
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("User Creation with BCrypt Password Hashing")
    void testUserCreationAndAuthentication() {
        // Test scenario 7
    }
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("User-Tenant Assignment & Multi-Tenancy")
    void testUserTenantAssignment() {
        // Test scenario 8
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("User Lifecycle - Suspend & Activate")
    void testUserSuspendAndActivate() {
        // Test scenario 16
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("User-Tenant Role Updates")
    void testUserTenantRoleUpdate() {
        // Test scenario 17
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("User-Tenant Removal & Access Revocation")
    void testUserTenantRemoval() {
        // Test scenario 18
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("User Listing with Pagination & Filtering")
    void testUserListingAndFiltering() {
        // Test scenario 19
    }
}
```

#### **5. CrossFeatureIntegrationTest.java** (2 test methods)
```java
@Epic("Integration Testing")
@Feature("Cross-Feature Integration")
class CrossFeatureIntegrationTest extends BaseIntegrationTest {
    
    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("End-to-End: Plan Validation + Subscription + History")
    void testEndToEndPlanValidationWithHistory() {
        // Test scenario 9
    }
    
    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("API Client Integration with User Management")
    void testApiClientUserManagement() {
        // Test scenario 20
    }
}
```

---

### **📊 Implementation Timeline**

| Week | Focus | Tests to Complete | Status |
|------|-------|-------------------|--------|
| **Week 1** | P1 Critical Tests | Tests 1-5 (API Client Auth + Plan Validation) | ⏳ Not Started |
| **Week 2** | P1 Critical Tests | Tests 6-9 (History + User Management + Integration) | ⏳ Not Started |
| **Week 3** | P2 Important Tests | Tests 10-20 (All P2 scenarios) | ⏳ Not Started |

---

### **🎯 Test Implementation Guidelines**

**For Each Test:**
1. Extend `BaseIntegrationTest` for Testcontainers setup
2. Use `@Epic`, `@Feature`, `@Story` annotations for Allure reporting
3. Add `@Step` annotations for detailed test steps
4. Use `@Severity` to mark critical vs normal tests
5. Add clear `@DisplayName` for readable reports
6. Attach HTTP requests/responses using `Allure.addAttachment()`
7. Use `TestDataFactory` for test data creation
8. Follow existing test patterns from `SubscriptionLifecycleTest`

**Example Test Structure:**
```java
@Test
@Severity(SeverityLevel.CRITICAL)
@DisplayName("Clear description of what is being tested")
void testMethodName() {
    // Given - Setup test data
    UUID tenantId = createTestTenant();
    String jwtToken = generateJwtToken(tenantId);
    
    // When - Execute action
    Response response = given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(requestPayload)
        .when()
        .post("/api/endpoint")
        .then()
        .extract().response();
    
    // Then - Verify results
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.jsonPath().getString("field")).isEqualTo("expected");
    
    // Attach evidence to Allure report
    Allure.addAttachment("Response", "application/json", response.asString());
}
```

---

### **🔧 Required Test Dependencies**

Ensure these are in `apps/subscription-api/build.gradle`:

```gradle
testImplementation 'io.rest-assured:rest-assured:5.3.0'
testImplementation 'org.testcontainers:postgresql:1.19.0'
testImplementation 'io.qameta.allure:allure-junit5:2.24.0'
testImplementation 'org.awaitility:awaitility:4.2.0'
testImplementation 'org.assertj:assertj-core:3.24.2'
testImplementation 'com.github.tomakehurst:wiremock-jre8:2.35.0'
```

---

### **📈 Progress Tracking**

**Current Status**: 20/20 tests implemented (100%) ✅ 🎉

Update this section as tests are completed:

```
Priority 1 Progress: 9/9 (100%) ✅
Priority 2 Progress: 11/11 (100%) ✅
Total Progress: 20/20 (100%) ✅
```

**✅ All Priority 1 (Critical) Tests Completed**:
- ✅ Test 1: API Client HMAC Authentication
- ✅ Test 2: Nonce Replay Prevention
- ✅ Test 3: Plan Category DIGITAL Validation
- ✅ Test 4: Plan Category PRODUCT_BASED Validation
- ✅ Test 5: Plan Category HYBRID Validation
- ✅ Test 6: Subscription History Complete Lifecycle
- ✅ Test 7: User Creation & Authentication
- ✅ Test 8: User-Tenant Assignment
- ✅ Test 9: End-to-End Plan Validation & History

**✅ All Priority 2 (Important) Tests Completed**:
- ✅ Test 10: API Client Secret Rotation
- ✅ Test 11: API Client Rate Limiting
- ✅ Test 12: Plan Validation on Update
- ✅ Test 13: Subscription History Metadata Tracking
- ✅ Test 14: Subscription History Pagination
- ✅ Test 15: Subscription History Actor Tracking
- ✅ Test 16: User Lifecycle - Suspend & Activate
- ✅ Test 17: User-Tenant Role Updates
- ✅ Test 18: User-Tenant Removal
- ✅ Test 19: User Listing & Filtering
- ✅ Test 20: API Client with User Management Integration

**Test Classes Created**:
1. ✅ `ApiClientAuthenticationTest.java` - 4 test methods (2 P1 + 2 P2)
2. ✅ `PlanCategoryValidationTest.java` - 4 test methods (3 P1 + 1 P2)
3. ✅ `SubscriptionHistoryTest.java` - 4 test methods (1 P1 + 3 P2)
4. ✅ `UserManagementTest.java` - 6 test methods (2 P1 + 4 P2)
5. ✅ `CrossFeatureIntegrationTest.java` - 2 test methods (1 P1 + 1 P2)

**Total**: 5 test classes, 20 test methods, 100% coverage achieved! 🎉

**Status**: ✅ **READY TO RUN**

---

### **Category 8: API Client Authentication & Security (Phase 0.5)**

#### ⚪ **Scenario 8.1: API Client Creation & HMAC Authentication**
**Status**: ⏳ Not Implemented  
**Test Class**: `ApiClientAuthenticationTest`  
**Priority**: P1 (Critical)  
**Business Value**: Validates secure API client authentication flow

**Steps**:
1. Admin creates API client via `/api/admin/api-clients`
2. Verify client_id and client_secret returned
3. Generate HMAC signature using client_secret
4. Make authenticated request with X-API-Key and X-Signature headers
5. Verify request succeeds with valid signature
6. Attempt request with invalid signature
7. Verify request rejected (401 Unauthorized)
8. Attempt request with expired timestamp
9. Verify request rejected (401 Unauthorized)

**Expected Outcomes**:
- ✓ API clients can authenticate successfully
- ✓ Invalid signatures rejected
- ✓ Replay attacks prevented (timestamp validation)
- ✓ Secrets stored securely (hashed)

---

#### ⚪ **Scenario 8.2: API Client Secret Rotation**
**Status**: ⏳ Not Implemented  
**Test Class**: `ApiClientSecretRotationTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests security best practice of secret rotation

**Steps**:
1. Create API client with initial secret
2. Make successful authenticated request
3. Rotate secret via `/api/admin/api-clients/{id}/rotate-secret`
4. Verify new secret returned
5. Attempt request with old secret
6. Verify request rejected
7. Make request with new secret
8. Verify request succeeds

**Expected Outcomes**:
- ✓ Old secrets immediately invalidated
- ✓ New secrets work immediately
- ✓ No downtime during rotation

---

#### ⚪ **Scenario 8.3: Rate Limiting per API Client**
**Status**: ⏳ Not Implemented  
**Test Class**: `ApiClientRateLimitingTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Prevents API abuse

**Steps**:
1. Create API client with rate limit (e.g., 100 req/min)
2. Make 100 successful requests
3. Make 101st request
4. Verify 429 Too Many Requests response
5. Wait 1 minute
6. Verify requests allowed again

**Expected Outcomes**:
- ✓ Rate limits enforced per client
- ✓ Limits reset correctly
- ✓ Clear error messages

---

#### ⚪ **Scenario 8.4: Nonce-Based Replay Attack Prevention**
**Status**: ⏳ Not Implemented  
**Test Class**: `NonceReplayPreventionTest`  
**Priority**: P1 (Critical)  
**Business Value**: Critical security feature

**Steps**:
1. Create API client
2. Make authenticated request with nonce "abc123"
3. Verify request succeeds
4. Replay exact same request (same nonce, signature, timestamp)
5. Verify request rejected (nonce already used)
6. Make new request with different nonce
7. Verify request succeeds

**Expected Outcomes**:
- ✓ Nonces tracked in cache
- ✓ Duplicate nonces rejected
- ✓ Cache expires appropriately

---

### **Category 9: Plan Validation & Categories (Phases 1-3)**

#### ⚪ **Scenario 9.1: Plan Category Validation - DIGITAL**
**Status**: ⏳ Not Implemented  
**Test Class**: `PlanCategoryDigitalValidationTest`  
**Priority**: P1 (Critical)  
**Business Value**: Ensures plan rules enforced correctly

**Steps**:
1. Create plan with category=DIGITAL
2. Verify requires_products=false, allows_products=false, base_price_required=true
3. Attempt to create subscription with products
4. Verify subscription rejected (DIGITAL plans don't allow products)
5. Create subscription without products
6. Verify subscription succeeds

**Expected Outcomes**:
- ✓ DIGITAL plan validation rules enforced
- ✓ Clear error messages for violations
- ✓ Valid subscriptions created successfully

---

#### ⚪ **Scenario 9.2: Plan Category Validation - PRODUCT_BASED**
**Status**: ⏳ Not Implemented  
**Test Class**: `PlanCategoryProductBasedValidationTest`  
**Priority**: P1 (Critical)  
**Business Value**: Validates product-based subscription rules

**Steps**:
1. Create plan with category=PRODUCT_BASED
2. Verify requires_products=true, allows_products=true, base_price_required=false
3. Attempt to create subscription without products
4. Verify subscription rejected (PRODUCT_BASED requires products)
5. Create subscription with products
6. Verify subscription succeeds
7. Verify pricing calculated from products only

**Expected Outcomes**:
- ✓ PRODUCT_BASED plan validation enforced
- ✓ Products required
- ✓ Pricing calculated correctly

---

#### ⚪ **Scenario 9.3: Plan Category Validation - HYBRID**
**Status**: ⏳ Not Implemented  
**Test Class**: `PlanCategoryHybridValidationTest`  
**Priority**: P1 (Critical)  
**Business Value**: Validates hybrid subscription model

**Steps**:
1. Create plan with category=HYBRID, base_price=$10
2. Verify requires_products=false, allows_products=true, base_price_required=true
3. Create subscription with base price only
4. Verify subscription succeeds with $10 charge
5. Create subscription with base price + 2 products ($5 each)
6. Verify subscription succeeds with $20 total ($10 + $5 + $5)
7. Verify pricing calculation correct

**Expected Outcomes**:
- ✓ HYBRID plan allows both models
- ✓ Pricing combines base + products
- ✓ Flexible subscription creation

---

#### ⚪ **Scenario 9.4: Plan Validation on Update**
**Status**: ⏳ Not Implemented  
**Test Class**: `PlanValidationOnUpdateTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Prevents invalid plan modifications

**Steps**:
1. Create DIGITAL plan with active subscriptions
2. Attempt to change category to PRODUCT_BASED
3. Verify update rejected (would break existing subscriptions)
4. Update other fields (name, description)
5. Verify update succeeds
6. Create new plan with different category
7. Verify creation succeeds

**Expected Outcomes**:
- ✓ Category changes blocked if subscriptions exist
- ✓ Safe updates allowed
- ✓ Data integrity maintained

---

### **Category 10: Subscription History & Audit Trail (Phases 1-4)**

#### ⚪ **Scenario 10.1: Subscription History - Complete Lifecycle**
**Status**: ⏳ Not Implemented  
**Test Class**: `SubscriptionHistoryLifecycleTest`  
**Priority**: P1 (Critical)  
**Business Value**: Validates complete audit trail

**Steps**:
1. Create subscription (verify CREATED entry)
2. Pause subscription (verify PAUSED entry with reason)
3. Resume subscription (verify RESUMED entry)
4. Change plan (verify PLAN_CHANGED entry with old/new plan IDs)
5. Cancel subscription (verify CANCELED entry with reason)
6. Retrieve history via `/api/admin/subscriptions/{id}/history`
7. Verify all 5 entries present in correct order
8. Verify each entry has: action, performed_by, performed_at, metadata

**Expected Outcomes**:
- ✓ Complete audit trail captured
- ✓ All actions tracked with metadata
- ✓ History retrievable via API

---

#### ⚪ **Scenario 10.2: Subscription History - Metadata Tracking**
**Status**: ⏳ Not Implemented  
**Test Class**: `SubscriptionHistoryMetadataTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Ensures rich audit data captured

**Steps**:
1. Pause subscription with reason "Going on vacation"
2. Retrieve history entry
3. Verify metadata contains: reason, previous_status, new_status
4. Update products (add 2, remove 1)
5. Retrieve PRODUCTS_UPDATED entry
6. Verify metadata contains: products_added, products_removed, product_count
7. Change plan from Basic to Premium
8. Verify metadata contains: old_plan_id, new_plan_id, old_price, new_price

**Expected Outcomes**:
- ✓ Rich metadata captured for each action
- ✓ Metadata queryable and useful
- ✓ Audit trail provides business insights

---

#### ⚪ **Scenario 10.3: Subscription History - Pagination**
**Status**: ⏳ Not Implemented  
**Test Class**: `SubscriptionHistoryPaginationTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Handles long-lived subscriptions

**Steps**:
1. Create subscription
2. Perform 50 actions (pause, resume, update, etc.)
3. Retrieve history with page=0, size=20
4. Verify 20 entries returned
5. Verify totalCount=51 (50 actions + 1 creation)
6. Retrieve page=1, size=20
7. Verify next 20 entries returned
8. Verify page=2, size=20 returns remaining 11 entries

**Expected Outcomes**:
- ✓ Pagination works correctly
- ✓ Total count accurate
- ✓ No duplicate or missing entries

---

#### ⚪ **Scenario 10.4: Subscription History - Admin vs Customer Actions**
**Status**: ⏳ Not Implemented  
**Test Class**: `SubscriptionHistoryActorTrackingTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Distinguishes who performed actions

**Steps**:
1. Customer pauses subscription
2. Verify history entry: performed_by_type=CUSTOMER, performed_by={customer_id}
3. Admin resumes subscription
4. Verify history entry: performed_by_type=ADMIN, performed_by={admin_user_id}
5. System auto-renews subscription
6. Verify history entry: performed_by_type=SYSTEM, performed_by=null
7. Retrieve history and verify actor types correct

**Expected Outcomes**:
- ✓ Actor type tracked (CUSTOMER, ADMIN, SYSTEM)
- ✓ Actor ID captured when applicable
- ✓ Clear attribution for all actions

---

### **Category 11: User Management & Multi-Tenant Access (Phase 5)**

#### ⚪ **Scenario 11.1: User Creation & Authentication**
**Status**: ⏳ Not Implemented  
**Test Class**: `UserManagementCreationTest`  
**Priority**: P1 (Critical)  
**Business Value**: Validates user management system

**Steps**:
1. Admin creates user via `/api/admin/users`
2. Provide: email, password, first_name, last_name, role=TENANT_ADMIN
3. Verify user created with hashed password (BCrypt)
4. Verify password not returned in response
5. Attempt to create duplicate email
6. Verify 409 Conflict error
7. List users and verify new user appears

**Expected Outcomes**:
- ✓ Users created successfully
- ✓ Passwords hashed securely
- ✓ Email uniqueness enforced
- ✓ Sensitive data not exposed

---

#### ⚪ **Scenario 11.2: User Lifecycle - Suspend & Activate**
**Status**: ⏳ Not Implemented  
**Test Class**: `UserLifecycleSuspendActivateTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests user account management

**Steps**:
1. Create active user
2. Suspend user via `/api/admin/users/{id}/suspend`
3. Verify status=SUSPENDED
4. Attempt to authenticate as suspended user
5. Verify authentication rejected
6. Activate user via `/api/admin/users/{id}/activate`
7. Verify status=ACTIVE
8. Verify user can authenticate again

**Expected Outcomes**:
- ✓ Suspend prevents access
- ✓ Activate restores access
- ✓ Status transitions tracked

---

#### ⚪ **Scenario 11.3: User-Tenant Assignment**
**Status**: ⏳ Not Implemented  
**Test Class**: `UserTenantAssignmentTest`  
**Priority**: P1 (Critical)  
**Business Value**: Validates multi-tenant access control

**Steps**:
1. Create user (no tenant assignments)
2. Assign user to Tenant A with role=ADMIN
3. Verify assignment created with assigned_at timestamp
4. Assign same user to Tenant B with role=MEMBER
5. Verify user has 2 tenant assignments
6. Retrieve user's tenants via `/api/admin/user-tenants/user/{userId}`
7. Verify both tenants returned
8. Retrieve Tenant A's users
9. Verify user appears in list

**Expected Outcomes**:
- ✓ Users can belong to multiple tenants
- ✓ Roles tracked per tenant
- ✓ Assignment timestamps captured

---

#### ⚪ **Scenario 11.4: User-Tenant Role Updates**
**Status**: ⏳ Not Implemented  
**Test Class**: `UserTenantRoleUpdateTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests permission management

**Steps**:
1. Assign user to tenant with role=VIEWER
2. Verify user has read-only access
3. Update role to ADMIN via `/api/admin/user-tenants/{id}`
4. Verify role updated
5. Verify user now has admin permissions
6. Attempt to downgrade OWNER to MEMBER
7. Verify business rules enforced (e.g., must have at least one OWNER)

**Expected Outcomes**:
- ✓ Roles updatable
- ✓ Permissions change immediately
- ✓ Business rules enforced

---

#### ⚪ **Scenario 11.5: User-Tenant Removal**
**Status**: ⏳ Not Implemented  
**Test Class**: `UserTenantRemovalTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests access revocation

**Steps**:
1. User assigned to Tenant A and Tenant B
2. Remove user from Tenant A via `/api/admin/user-tenants/{id}`
3. Verify assignment deleted
4. Verify user can no longer access Tenant A resources
5. Verify user still has access to Tenant B
6. Attempt to remove last OWNER from tenant
7. Verify removal blocked (business rule)

**Expected Outcomes**:
- ✓ Access revoked immediately
- ✓ Other tenant access unaffected
- ✓ Business rules prevent orphaned tenants

---

#### ⚪ **Scenario 11.6: User Listing & Filtering**
**Status**: ⏳ Not Implemented  
**Test Class**: `UserListingFilteringTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests admin user management UI

**Steps**:
1. Create 50 users with various roles and statuses
2. List users with pagination (page=0, size=20)
3. Verify 20 users returned
4. Filter by status=ACTIVE
5. Verify only active users returned
6. Filter by role=TENANT_ADMIN
7. Verify only admins returned
8. Filter by status=SUSPENDED and role=CUSTOMER
9. Verify combined filters work

**Expected Outcomes**:
- ✓ Pagination works correctly
- ✓ Filters work independently
- ✓ Combined filters work
- ✓ Performance acceptable

---

### **Category 12: Integration & Cross-Feature Scenarios**

#### ⚪ **Scenario 12.1: End-to-End with Plan Validation & History**
**Status**: ⏳ Not Implemented  
**Test Class**: `EndToEndPlanValidationHistoryTest`  
**Priority**: P1 (Critical)  
**Business Value**: Validates feature integration

**Steps**:
1. Admin creates HYBRID plan
2. Customer creates subscription with base + products
3. Verify plan validation passes
4. Verify CREATED history entry
5. Customer pauses subscription
6. Verify PAUSED history entry with metadata
7. Admin changes plan to DIGITAL
8. Verify plan validation prevents change (has products)
9. Customer removes products
10. Admin changes plan to DIGITAL
11. Verify PLAN_CHANGED history entry
12. Retrieve complete history
13. Verify all actions tracked correctly

**Expected Outcomes**:
- ✓ Plan validation integrates with subscription creation
- ✓ History tracks all actions
- ✓ Business rules enforced across features

---

#### ⚪ **Scenario 12.2: API Client with User Management**
**Status**: ⏳ Not Implemented  
**Test Class**: `ApiClientUserManagementIntegrationTest`  
**Priority**: P2 (Should Have)  
**Business Value**: Tests external system integration

**Steps**:
1. Create API client for external HR system
2. External system creates user via API (with HMAC auth)
3. Verify user created
4. External system assigns user to tenant
5. Verify assignment created
6. External system lists users
7. Verify pagination and filtering work with API auth
8. Track all actions in sensitive_operations_log

**Expected Outcomes**:
- ✓ API clients can manage users
- ✓ All operations audited
- ✓ Security maintained

---

### **Test Implementation Priority Summary**

#### **Priority 1 (Critical) - Must Implement First**
1. ✅ API Client HMAC Authentication (8.1)
2. ✅ Nonce Replay Prevention (8.4)
3. ✅ Plan Category DIGITAL Validation (9.1)
4. ✅ Plan Category PRODUCT_BASED Validation (9.2)
5. ✅ Plan Category HYBRID Validation (9.3)
6. ✅ Subscription History Lifecycle (10.1)
7. ✅ User Creation & Authentication (11.1)
8. ✅ User-Tenant Assignment (11.3)
9. ✅ End-to-End Plan Validation & History (12.1)

**Total P1 Tests**: 9 scenarios

#### **Priority 2 (Important) - Implement Next**
1. API Client Secret Rotation (8.2)
2. API Client Rate Limiting (8.3)
3. Plan Validation on Update (9.4)
4. Subscription History Metadata (10.2)
5. Subscription History Pagination (10.3)
6. Subscription History Actor Tracking (10.4)
7. User Suspend & Activate (11.2)
8. User-Tenant Role Updates (11.4)
9. User-Tenant Removal (11.5)
10. User Listing & Filtering (11.6)
11. API Client with User Management (12.2)

**Total P2 Tests**: 11 scenarios

---

### **Updated Test Coverage Summary**

**Existing Tests**: 74 integration tests (100% coverage of original features)  
**New Test Scenarios Required**: 20 scenarios for Phases 0-5 features  
**Total Test Coverage Target**: 94+ integration tests

**New Test Classes to Create**:
1. `ApiClientAuthenticationTest` (4 scenarios)
2. `PlanCategoryValidationTest` (4 scenarios)
3. `SubscriptionHistoryTest` (4 scenarios)
4. `UserManagementTest` (6 scenarios)
5. `CrossFeatureIntegrationTest` (2 scenarios)

**Estimated Implementation Time**: 2-3 weeks for complete test coverage

---

---

## 🎯 Customer Self-Service API Tests

### **Test Class**: `CustomerSelfServiceTest`
**Status**: ✅ **ALL 11 TESTS PASSING** (100% Success Rate)  
**Location**: `apps/subscription-api/src/test/java/com/subscriptionengine/api/integration/CustomerSelfServiceTest.java`  
**Priority**: P1 (Critical)  
**Business Value**: Validates complete customer self-service journey and authorization

### **Test Coverage Summary**

| Test | Description | Status |
|------|-------------|--------|
| `testGetAvailablePlans` | Customer views available subscription plans | ✅ PASSING |
| `testCreateCustomerSubscription` | Customer creates subscription via self-signup | ✅ PASSING |
| `testGetCustomerSubscriptions` | Customer lists their subscriptions | ✅ PASSING |
| `testGetSubscriptionDashboard` | Customer views subscription dashboard | ✅ PASSING |
| `testPauseSubscription` | Customer pauses their subscription | ✅ PASSING |
| `testResumeSubscription` | Customer resumes paused subscription | ✅ PASSING |
| `testCancelSubscription` | Customer cancels subscription | ✅ PASSING |
| `testGetCustomerDeliveries` | Customer views delivery history | ✅ PASSING |
| `testSkipDelivery` | Customer skips upcoming delivery | ✅ PASSING |
| `testRescheduleDelivery` | Customer reschedules delivery date | ✅ PASSING |
| `testCustomerCannotAccessOtherCustomerData` | Authorization: Cross-customer access blocked | ✅ PASSING |

**Total**: 11 tests, 100% passing ✅

---

### **Critical Bug Fixed: `performed_by` Constraint Violation**

#### **Original Issue**
All 11 tests were failing with database constraint violation:
```
ERROR: null value in column "performed_by" of relation "subscription_history" 
violates not-null constraint
```

#### **Root Cause Analysis**
The `subscription_history` table has a NOT NULL foreign key constraint on `performed_by` that references the `users` table. The subscription creation code was using a hardcoded `UUID.randomUUID()` instead of extracting the actual authenticated user's ID from the JWT token.

#### **Solution Implemented**

**1. Created `UserContext` Utility Class**
- **File**: `modules/auth/src/main/java/com/subscriptionengine/auth/UserContext.java`
- **Purpose**: Extract user information from Spring Security context
- **Methods**:
  - `getUserId()` - Get authenticated user's ID from JWT
  - `getUserEmail()` - Get user's email
  - `getUserRole()` - Get user's role
  - `getCustomerId()` - Get customer ID (for CUSTOMER role)

```java
public class UserContext {
    public static String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getClaimAsString("user_id");
        }
        return null;
    }
    // ... other methods
}
```

**2. Updated `SubscriptionsService`**
- **File**: `modules/domain-subscriptions/src/main/java/com/subscriptionengine/subscriptions/service/SubscriptionsService.java`
- **Change**: Use `UserContext.getUserId()` instead of `UUID.randomUUID()`
- **Impact**: Properly records who performed subscription actions

```java
// Before (BROKEN):
String performedBy = UUID.randomUUID().toString();

// After (FIXED):
String performedBy = UserContext.getUserId();
String performedByType = determinePerformedByType(UserContext.getUserRole());
```

**3. Fixed Test Infrastructure**
- **File**: `apps/subscription-api/src/test/java/com/subscriptionengine/api/integration/CustomerSelfServiceTest.java`
- **Changes**:
  - Created `createUser()` helper that directly inserts users into database with proper schema fields
  - Fixed role mapping: `users.role` (CUSTOMER) → `user_tenants.role` (MEMBER)
  - Made emails unique per test to avoid conflicts
  - Fixed `createSubscriptionForCustomer()` to create customer records with exact IDs
  - Fixed `pauseSubscription()` to use customer self-service endpoint
  - Fixed `getFirstDeliveryId()` to use customer deliveries endpoint

```java
private void createUser(String tenantId, String userId, String email, String role) {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    String hashedPassword = encoder.encode("TestPassword123!");
    
    // Insert into users table
    dsl.insertInto(table("users"))
        .set(field("id"), UUID.fromString(userId))
        .set(field("email"), email)
        .set(field("full_name"), "Test User")
        .set(field("first_name"), "Test")
        .set(field("last_name"), "User")
        .set(field("password_hash"), hashedPassword)
        .set(field("role"), role)
        .set(field("status"), "ACTIVE")
        // ... other fields
        .execute();
    
    // Map system role to tenant role
    String tenantRole = role.equals("CUSTOMER") ? "MEMBER" : "ADMIN";
    
    // Insert into user_tenants table
    dsl.insertInto(table("user_tenants"))
        .set(field("user_id"), UUID.fromString(userId))
        .set(field("tenant_id"), UUID.fromString(tenantId))
        .set(field("role"), tenantRole)
        // ... other fields
        .execute();
}
```

---

### **Key Fixes Applied**

#### **1. User Creation in Tests**
- **Problem**: JWT tokens contained user IDs that didn't exist in database
- **Solution**: Create user records before generating JWT tokens
- **Impact**: All foreign key constraints satisfied

#### **2. Role Mapping**
- **Problem**: System roles (CUSTOMER, TENANT_ADMIN) don't match tenant roles (MEMBER, ADMIN, OWNER, VIEWER)
- **Solution**: Map `CUSTOMER` → `MEMBER` when inserting into `user_tenants`
- **Impact**: Database constraints satisfied, proper role hierarchy

#### **3. Email Uniqueness**
- **Problem**: Multiple tests using same email caused conflicts
- **Solution**: Generate unique emails per test using tenant ID: `customer-{tenantId}@example.com`
- **Impact**: Tests can run in parallel without conflicts

#### **4. Customer ID Consistency**
- **Problem**: Admin endpoint created subscriptions with different customer IDs than test expected
- **Solution**: 
  - Create customer record in database with exact ID needed
  - Use customer self-service endpoint instead of admin endpoint
- **Impact**: Tests can verify subscription ownership correctly

#### **5. Endpoint Consistency**
- **Problem**: Helper methods used non-existent admin endpoints
- **Solution**: Use customer self-service endpoints consistently:
  - `/v1/customers/me/subscriptions` for subscription operations
  - `/v1/customers/me/deliveries` for delivery operations
- **Impact**: Tests actually test customer self-service APIs

---

### **Test Execution**

```bash
# Run all customer self-service tests
./gradlew :apps:subscription-api:test --tests CustomerSelfServiceTest --info

# Run specific test
./gradlew :apps:subscription-api:test --tests CustomerSelfServiceTest.testCreateCustomerSubscription --info

# Expected output
BUILD SUCCESSFUL in 1m 16s
11 tests completed, 0 failed ✅
```

---

### **Database Schema Requirements**

The tests validate these schema constraints are properly handled:

**1. `users` table**:
- `id` (UUID, PRIMARY KEY)
- `email` (VARCHAR, UNIQUE, NOT NULL)
- `full_name` (VARCHAR, NOT NULL)
- `first_name` (VARCHAR, NOT NULL)
- `last_name` (VARCHAR, NOT NULL)
- `password_hash` (VARCHAR, NOT NULL)
- `role` (VARCHAR, NOT NULL) - System role: SUPER_ADMIN, TENANT_ADMIN, STAFF, CUSTOMER
- `status` (VARCHAR, NOT NULL) - ACTIVE, SUSPENDED, INACTIVE

**2. `user_tenants` table**:
- `user_id` (UUID, FOREIGN KEY → users.id)
- `tenant_id` (UUID, FOREIGN KEY → tenants.id)
- `role` (VARCHAR, NOT NULL) - Tenant role: OWNER, ADMIN, MEMBER, VIEWER
- `assigned_at` (TIMESTAMP, NOT NULL, DEFAULT NOW())

**3. `subscription_history` table**:
- `performed_by` (UUID, NOT NULL, FOREIGN KEY → users.id)
- `performed_by_type` (VARCHAR, NOT NULL) - CUSTOMER, ADMIN, SYSTEM
- `action` (VARCHAR, NOT NULL) - CREATED, PAUSED, RESUMED, CANCELED, etc.

---

### **Authorization Testing**

The test suite validates these authorization rules:

1. **Customer Ownership**: Customers can only access their own subscriptions
2. **Cross-Customer Isolation**: Customer A cannot access Customer B's data
3. **JWT Token Validation**: All requests require valid JWT with correct claims
4. **Tenant Isolation**: Data is properly isolated by tenant_id
5. **Role-Based Access**: CUSTOMER role has appropriate permissions for self-service operations

---

### **API Response Format Validation**

Tests verify two different response formats:

**1. Direct Response (Subscription Creation)**:
```json
{
  "id": "uuid",
  "status": "ACTIVE",
  "planId": "uuid",
  "customerId": "uuid"
}
```

**2. Wrapped Response (List Operations)**:
```json
{
  "success": true,
  "data": {
    "subscriptions": [...],
    "count": 5
  },
  "timestamp": "2026-02-11T10:46:34.665742-05:00",
  "requestId": "abc123"
}
```

---

### **Key Learnings**

1. **Always create test users before generating JWT tokens** - Foreign key constraints require actual database records
2. **Map roles correctly** - System roles ≠ Tenant roles
3. **Use consistent endpoints** - Don't mix admin and customer endpoints in customer tests
4. **Make test data unique** - Use tenant IDs or UUIDs to avoid conflicts
5. **Test with actual database constraints** - Integration tests should validate real schema rules
6. **Verify audit trail** - Ensure `performed_by` is populated for all actions

---

### **Performance Metrics**

- **Test Suite Execution Time**: ~1 minute 16 seconds
- **Database Setup**: Testcontainers with PostgreSQL 15
- **Parallel Execution**: Enabled (JUnit 5 parallel execution)
- **Success Rate**: 100% (11/11 tests passing)

---

### **Future Enhancements**

1. Add tests for subscription modification (plan changes, quantity updates)
2. Add tests for payment method updates
3. Add tests for subscription renewal scenarios
4. Add tests for trial period handling
5. Add tests for proration calculations
6. Add performance tests for high-volume scenarios

---

## 📞 Support

For issues or questions:
1. Check test logs and Allure report
2. Review this documentation
3. Check existing test examples
4. Consult team documentation
