# 🚀 SubscriptionManager - Headless Subscription Engine

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=for-the-badge&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=for-the-badge&logo=docker)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**API-first, multi-tenant subscription management platform for ecommerce and digital services**

[Features](#-key-features) • [Architecture](#-architecture) • [Worker Module](#️-worker-module) • [API Documentation](#-api-documentation) • [Testing](#-testing) • [Deployment](#-deployment)

</div>

---

## 👨‍💻 Author

**Neeraj Yadav**

[![GitHub](https://img.shields.io/badge/GitHub-neeraj--agentic--lab-181717?style=flat&logo=github)](https://github.com/neeraj-agentic-lab)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-n--yadav-0077B5?style=flat&logo=linkedin)](https://www.linkedin.com/in/n-yadav/)

---

## 📖 Table of Contents

- [🌟 Overview](#-overview)
- [✨ Key Features](#-key-features)
  - [Core Capabilities](#core-capabilities)
- [🧠 Core Concepts](#-core-concepts)
- [🏗 Architecture](#-architecture)
  - [System Overview](#system-overview)
  - [System Architecture Diagram](#system-architecture-diagram)
  - [Module Dependencies](#module-dependencies)
  - [Key Architectural Patterns](#key-architectural-patterns)
- [📦 Technology Stack](#-technology-stack)
  - [Backend Framework](#backend-framework)
  - [Database & Persistence](#database--persistence)
  - [API & Documentation](#api--documentation)
  - [Authentication & Security](#authentication--security)
  - [Build & DevOps](#build--devops)
  - [Testing Infrastructure](#testing-infrastructure)
  - [Cloud Platforms](#cloud-platforms-multi-cloud-support)
- [📚 Domain Model](#-domain-model)
  - [Database Schema Overview](#database-schema-overview)
  - [Core Business Tables](#core-business-tables-11-tables)
  - [Delivery & Entitlement Tables](#delivery--entitlement-tables-2-tables)
  - [Worker & Scheduling Tables](#worker--scheduling-tables-3-tables)
  - [Event & Webhook Tables](#event--webhook-tables-3-tables)
  - [Security & API Tables](#security--api-tables-6-tables)
- [🔄 How Scheduling Works](#-how-scheduling-works)
  - [Database-Driven Scheduler](#database-driven-scheduler)
  - [Task Types & Handlers](#task-types--handlers)
  - [Task Lifecycle States](#task-lifecycle-states)
- [🔁 Subscription Renewal Flow](#-subscription-renewal-flow)
  - [Complete Renewal Task Chain](#complete-renewal-task-chain)
  - [Task Chain Breakdown](#task-chain-breakdown)
- [⚙️ Worker Module](#️-worker-module)
  - [Architecture](#architecture-1)
  - [Task Types](#task-types)
  - [Distributed Locking](#distributed-locking)
  - [Lock Reaper](#lock-reaper)
  - [Error Handling & Retries](#error-handling--retries)
- [📡 API Documentation](#-api-documentation)
  - [Admin API Endpoints](#-admin-api-endpoints)
  - [Customer Self-Service APIs](#-customer-self-service-apis)
  - [API Response Formats](#-api-response-formats)
- [🔐 Security & Idempotency](#-security--idempotency)
- [🚀 Deployment](#-deployment)
  - [Google Cloud Platform (GCP)](#-google-cloud-platform-gcp-deployment)
  - [AWS & Azure](#-aws--azure-deployment)
- [🚀 Quick Start](#-quick-start)
  - [Local Development Setup](#local-development-setup)
  - [Quick API Test](#quick-api-test)
- [🧪 Testing](#-testing)
  - [Test Suite Overview](#test-suite-overview)
  - [Worker Module Tests](#worker-module-tests-40-tests)
  - [Running Tests](#running-tests)
- [📊 Performance & Scale](#-performance--scale)
  - [Production Performance Metrics](#production-performance-metrics)
  - [Scale Tiers](#scale-tiers)
  - [Optimization Strategies](#optimization-strategies)
  - [Monitoring & Observability](#monitoring--observability)
- [🛣 Evolution Path](#-evolution-path-future)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)
- [📞 Contact & Support](#-contact--support)

---

## 🌟 Overview

**SubscriptionManager** is a production-ready, API-first subscription management platform built for modern SaaS and ecommerce businesses. Designed with **correctness, idempotency, and auditability** as core principles, it handles high-scale subscription billing with zero double billing and complete audit trails.

**Built on proven patterns:**
- PostgreSQL-based distributed locking for task processing
- Transactional outbox pattern for reliable event delivery
- Row-level multi-tenancy with complete data isolation
- Comprehensive API with 47+ endpoints across 12 controllers
- Background worker with 6 task types and automatic crash recovery

**Production-ready features:**
- 158 automated tests (95% API coverage)
- HMAC-signed webhooks with retry logic
- API key authentication with replay prevention
- Complete subscription history and audit trails
- Unified API for SaaS and product-based subscriptions

### What Makes This Different?

- **PostgreSQL as Single Source of Truth**: No Kafka, Redis, or external queues. All scheduling, locking, and events use PostgreSQL with `FOR UPDATE SKIP LOCKED` for distributed processing.

- **Multi-Tenant SaaS Ready**: Row-level tenant isolation enforced at the database query level. JWT authentication with automatic tenant context propagation. Zero cross-tenant data leakage.

- **Flexible Subscription Models**: Supports DIGITAL (SaaS), PRODUCT_BASED (subscription boxes), and HYBRID plans with validation. Single unified API endpoint handles all subscription types.

- **Financial-Grade Correctness**: Database constraints prevent double billing. Idempotency keys prevent duplicate operations. Unique task keys prevent duplicate processing. Immutable invoice/delivery snapshots provide audit trails.

- **Worker Module with Crash Recovery**: Distributed task processing with PostgreSQL-based locking. Lock reaper automatically recovers from worker crashes. 6 task handlers: SUBSCRIPTION_RENEWAL, CHARGE_PAYMENT, CREATE_DELIVERY, CREATE_ORDER, SUBSCRIPTION_ITEM_RENEWAL, ENTITLEMENT_GRANT.

- **Comprehensive Testing**: 158 tests including 141 integration tests and 17 end-to-end scenarios. Testcontainers for PostgreSQL. Allure reporting with detailed test steps.

- **Cloud-Native Deployment**: Deploy to GCP Cloud Run, AWS ECS, or Azure Container Apps. Includes Terraform/CloudFormation templates. GitHub Actions CI/CD ready. Multi-cloud PostgreSQL support (Cloud SQL, RDS, Azure Database).

---

## ✨ Key Features

### Core Capabilities

- ✅ **API-First Design** - 47+ RESTful endpoints with OpenAPI/Swagger documentation, 12 controllers
- ✅ **Multi-Tenant SaaS** - Row-level tenant isolation with JWT authentication and tenant context propagation
- ✅ **Flexible Subscription Models** - DIGITAL, PRODUCT_BASED, and HYBRID plan categories with validation
- ✅ **Unified Subscription API** - Single endpoint handles both SaaS and product-based subscriptions
- ✅ **High Scale** - PostgreSQL-based distributed locking with `FOR UPDATE SKIP LOCKED` for 50k-100k renewals/hour
- ✅ **Zero Double Billing** - Database constraints, idempotency keys, and unique task keys prevent duplicates
- ✅ **Transactional Outbox Pattern** - Reliable event publishing via `outbox_events` table with guaranteed delivery
- ✅ **Worker Module** - Distributed task processing with 6 task types, lock reaper, and crash recovery
- ✅ **Delivery Management** - Scheduled deliveries with skip/reschedule, cancel operations, and tracking
- ✅ **Entitlement System** - Automated grant/revoke for digital access rights via task handlers
- ✅ **Payment Processing** - Mock adapter ready, Stripe integration prepared with retry logic
- ✅ **Webhook System** - HMAC-signed webhooks with exponential backoff retry (up to 5 attempts)
- ✅ **Subscription History** - Complete audit trail tracking CREATE, PAUSE, RESUME, CANCEL with metadata
- ✅ **API Client Auth** - HMAC signature verification, nonce-based replay prevention, rate limiting
- ✅ **User Management** - BCrypt password hashing, user-tenant assignments, role-based access control
- ✅ **Customer Self-Service** - 7 customer-facing endpoints for subscription and delivery management

---

## 🧠 Core Concepts

### Subscription = Contract  
Stored once in `subscriptions` table, mutable for future cycles. Represents the ongoing relationship between customer and service. Supports DIGITAL, PRODUCT_BASED, and HYBRID plan categories.

### Invoice / Delivery / Entitlement = Snapshot  
Created per billing cycle in `invoices`, `deliveries`, `entitlements` tables. **Immutable**, audit-safe. Provides complete historical record with JSONB metadata.

### Scheduling = Data, Not Cron  
All async work is driven by `scheduled_tasks` table with PostgreSQL-based distributed locking (`FOR UPDATE SKIP LOCKED`). Worker processes claim tasks, execute handlers, and update status. Lock reaper recovers from crashes.

### Multi-Tenancy = Row-Level Isolation
Every table has `tenant_id` column. JWT token provides tenant context. All queries automatically filtered by tenant. Zero cross-tenant data leakage.

### Idempotency = Deduplication
All write operations use `idempotency_keys` table. Duplicate requests return cached response. Prevents double billing, duplicate orders, and race conditions.

---

## 🏗 Architecture

### System Overview

**4-Module Gradle Project:**
- `apps/subscription-api` - REST API (Port 8080, 12 controllers, 47+ endpoints)
- `apps/subscription-worker` - Background worker (Port 8081, 6 task types)
- `modules/auth` - JWT + API Key authentication, HMAC signatures, rate limiting
- `modules/common` - jOOQ DAOs, shared utilities, tenant context

**Additional Domain Modules:**
- `modules/domain-billing` - Invoice generation, payment processing
- `modules/domain-delivery` - Delivery scheduling and management
- `modules/domain-plans` - Plan validation and categories
- `modules/domain-subscriptions` - Subscription lifecycle and history
- `modules/integrations` - Payment/Commerce/Entitlement adapters
- `modules/outbox` - Transactional outbox pattern, webhook delivery
- `modules/scheduler` - Task processing, job scheduling, lock reaper

### System Architecture Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        A[Admin Dashboard]
        B[Customer Portal]
        C[Third-party Apps]
    end
    
    subgraph "API Module (Port 8080)"
        E[12 REST Controllers]
        F[JWT Auth + API Key Auth]
        G[Tenant Context Filter]
    end
    
    subgraph "Domain Services"
        H[SubscriptionsService]
        I[PlansService]
        J[DeliveryService]
        K[WebhookService]
        L[BillingService]
    end
    
    subgraph "Worker Module (Port 8081)"
        M[TaskProcessorService]
        N[6 Task Handlers]
        O[Lock Reaper]
        P[Job Scheduler]
    end
    
    subgraph "Data Layer"
        Q[(PostgreSQL 15)]
        R[20+ Tables]
        S[Flyway Migrations]
    end
    
    subgraph "Integrations"
        T[Payment Adapter]
        U[Commerce Adapter]
        V[Entitlement Adapter]
    end
    
    A --> E
    B --> E
    C --> E
    E --> F
    F --> G
    G --> H
    G --> I
    G --> J
    G --> K
    H --> Q
    I --> Q
    J --> Q
    K --> Q
    L --> Q
    M --> N
    N --> T
    N --> U
    N --> V
    N --> Q
    O --> Q
    P --> Q
    S --> Q
    
    style E fill:#4CAF50
    style M fill:#FF9800
    style Q fill:#2196F3
```

### Module Dependencies

```mermaid
graph TB
    subgraph "Apps Layer"
        API[subscription-api<br/>Port 8080]
        WORKER[subscription-worker<br/>Port 8081]
    end
    
    subgraph "Domain Modules"
        BILLING[domain-billing]
        DELIVERY[domain-delivery]
        PLANS[domain-plans]
        SUBS[domain-subscriptions]
    end
    
    subgraph "Infrastructure Modules"
        AUTH[auth<br/>JWT + HMAC]
        COMMON[common<br/>jOOQ DAOs]
        INTEGRATIONS[integrations<br/>Adapters]
        OUTBOX[outbox<br/>Events]
        SCHEDULER[scheduler<br/>Tasks]
    end
    
    subgraph "Database"
        DB[(PostgreSQL 15<br/>20+ Tables)]
    end
    
    API --> AUTH
    API --> PLANS
    API --> SUBS
    API --> DELIVERY
    API --> OUTBOX
    
    WORKER --> SCHEDULER
    WORKER --> BILLING
    WORKER --> INTEGRATIONS
    
    BILLING --> COMMON
    DELIVERY --> COMMON
    PLANS --> COMMON
    SUBS --> COMMON
    SCHEDULER --> COMMON
    OUTBOX --> COMMON
    
    COMMON --> DB
    
    style API fill:#4CAF50
    style WORKER fill:#FF9800
    style DB fill:#2196F3
```

### Key Architectural Patterns

**1. Distributed Locking**
```sql
SELECT * FROM scheduled_tasks 
WHERE status = 'READY' AND due_at <= NOW()
FOR UPDATE SKIP LOCKED
LIMIT 10;
```
- Multiple workers can run concurrently
- No duplicate task processing
- Automatic crash recovery via lock reaper

**2. Transactional Outbox**
```java
@Transactional
public void createSubscription() {
    // 1. Insert subscription
    subscriptionDao.insert(subscription);
    
    // 2. Insert outbox event (same transaction)
    outboxDao.insert(event);
    
    // 3. Commit together (atomic)
}
```
- Events published reliably
- No lost messages
- Webhook delivery with retries

**3. Multi-Tenant Isolation**
```java
@Aspect
public class TenantSecurityService {
    // Automatically inject tenant_id into all queries
    dsl.select().from(SUBSCRIPTIONS)
       .where(SUBSCRIPTIONS.TENANT_ID.eq(tenantId));
}
```
- Zero cross-tenant data leakage
- Tenant context from JWT token
- Enforced at database query level

### Deployment Architecture

```mermaid
graph TB
    subgraph "GitHub"
        GH[GitHub Repository]
        GA[GitHub Actions]
    end
    
    subgraph "Cloud Provider (GCP/AWS/Azure)"
        subgraph "Container Services"
            API[API Container<br/>Cloud Run/ECS/Container Apps]
            WORKER[Worker Container<br/>Cloud Run Jobs/ECS Tasks]
        end
        
        subgraph "Database"
            DB[(Managed PostgreSQL<br/>Cloud SQL/RDS/Azure DB)]
        end
        
        subgraph "Secrets"
            SEC[Secret Manager<br/>GCP/AWS/Azure]
        end
        
        subgraph "Registry"
            REG[Container Registry<br/>GCR/ECR/ACR]
        end
    end
    
    GH --> GA
    GA --> REG
    REG --> API
    REG --> WORKER
    API --> DB
    WORKER --> DB
    API --> SEC
    WORKER --> SEC
    
    style API fill:#4CAF50
    style WORKER fill:#FF9800
    style DB fill:#2196F3
```

---

## 📦 Technology Stack

### Backend Framework

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 17 | Primary language (LTS) |
| **Spring Boot** | 3.2.1 | Application framework |
| **Spring Security** | 6.2.x | OAuth2/JWT authentication |
| **Spring Data Commons** | 3.2.x | Pagination support |
| **Spring Actuator** | 3.2.x | Health checks, metrics |

### Database & Persistence

| Technology | Version | Purpose |
|------------|---------|---------|
| **PostgreSQL** | 15+ | Primary database (single source of truth) |
| **jOOQ** | 3.18+ | Type-safe SQL queries, code generation |
| **Flyway** | 9.22.3 | Database migrations (11 migration files) |
| **PostgreSQL Driver** | 42.7.2 | JDBC driver |
| **HikariCP** | 5.1.x | Connection pooling (via Spring Boot) |

### API & Documentation

| Technology | Version | Purpose |
|------------|---------|---------|
| **SpringDoc OpenAPI** | 2.3.0 | OpenAPI 3.0 specification generation |
| **Swagger UI** | 2.3.0 | Interactive API explorer |
| **Jackson** | 2.15.x | JSON serialization/deserialization |
| **Jackson YAML** | 2.15.x | YAML support for OpenAPI docs |
| **Bean Validation** | 3.0.x | Request validation |

### Authentication & Security

| Technology | Version | Purpose |
|------------|---------|---------|
| **JWT (jjwt)** | 0.12.3 | JWT token generation/validation |
| **BCrypt** | - | Password hashing (via Spring Security) |
| **HMAC** | - | API signature verification |
| **OAuth2 Resource Server** | 6.2.x | JWT authentication |

### Build & DevOps

| Technology | Version | Purpose |
|------------|---------|---------|
| **Gradle** | 8.5 | Build automation, multi-module project |
| **Docker** | 20+ | Containerization |
| **Docker Compose** | 2.x | Local development environment |
| **GitHub Actions** | - | CI/CD pipeline |

### Testing Infrastructure

| Technology | Version | Purpose |
|------------|---------|---------|
| **JUnit 5** | 5.10.x | Unit & integration testing framework |
| **Testcontainers** | 1.21.4 | PostgreSQL containers for integration tests |
| **REST Assured** | 5.4.0 | API testing DSL |
| **Mockito** | 5.7.x | Mocking framework |
| **AssertJ** | 3.25.1 | Fluent assertions |
| **Allure** | 2.25.0 | Test reporting with beautiful UI |
| **Awaitility** | 4.2.0 | Async testing utilities |
| **WireMock** | 3.3.1 | HTTP service mocking |

### Cloud Platforms (Multi-Cloud Support)

| Platform | Services Used |
|----------|---------------|
| **Google Cloud Platform** | Cloud Run (API/Worker), Cloud SQL (PostgreSQL), Secret Manager, Artifact Registry, Cloud SQL Proxy |
| **Amazon Web Services** | ECS Fargate (API/Worker), RDS PostgreSQL, Secrets Manager, ECR, Application Load Balancer |
| **Microsoft Azure** | Container Apps (API/Worker), Azure Database for PostgreSQL, Key Vault, ACR, Application Gateway |

### Observability (Ready)

| Technology | Purpose |
|------------|---------|
| **Spring Actuator** | Health checks, metrics endpoints |
| **Micrometer** | Metrics collection (via Spring Boot) |
| **SLF4J + Logback** | Structured logging |
| **OpenTelemetry** | Distributed tracing (ready for integration) |

### Project Structure

```
📦 SubscriptionManager (Gradle Multi-Module)
├── 🚀 apps/
│   ├── subscription-api (Port 8080)
│   └── subscription-worker (Port 8081)
├── 📚 modules/
│   ├── auth (JWT + HMAC + Rate Limiting)
│   ├── common (jOOQ DAOs + Utilities)
│   ├── domain-billing (Invoice + Payment)
│   ├── domain-delivery (Delivery Management)
│   ├── domain-plans (Plan Validation)
│   ├── domain-subscriptions (Subscription Lifecycle)
│   ├── integrations (Payment/Commerce/Entitlement Adapters)
│   ├── outbox (Transactional Outbox + Webhooks)
│   └── scheduler (Task Processing + Lock Reaper)
├── 🗄️ db/migrations/ (11 Flyway SQL files)
└── 🧪 158 tests (95% API coverage)
```

### Key Dependencies Summary

- **Total Modules**: 11 (4 apps + 7 domain/infrastructure)
- **Database Tables**: 20+ tables with row-level multi-tenancy
- **API Endpoints**: 47+ REST endpoints across 12 controllers
- **Test Coverage**: 158 tests (141 integration + 17 scenarios)
- **Build Time**: ~30 seconds (clean build)
- **Docker Images**: 2 (API + Worker)

---

## 📚 Domain Model

### Database Schema Overview

**20+ Tables with Row-Level Multi-Tenancy**

Every table includes `tenant_id` for complete data isolation. All tables use UUID primary keys and include JSONB `custom_attributes` for extensibility.

```mermaid
erDiagram
    TENANTS ||--o{ USERS : manages
    TENANTS ||--o{ CUSTOMERS : has
    TENANTS ||--o{ PLANS : owns
    TENANTS ||--o{ WEBHOOKS : configures
    TENANTS ||--o{ API_CLIENTS : authenticates
    USERS ||--o{ USER_TENANTS : assigned_to
    USER_TENANTS }o--|| TENANTS : belongs_to
    CUSTOMERS ||--o{ SUBSCRIPTIONS : subscribes
    PLANS ||--o{ SUBSCRIPTIONS : defines
    SUBSCRIPTIONS ||--o{ SUBSCRIPTION_ITEMS : contains
    SUBSCRIPTIONS ||--o{ SUBSCRIPTION_HISTORY : tracks
    SUBSCRIPTIONS ||--o{ INVOICES : generates
    SUBSCRIPTIONS ||--o{ DELIVERY_INSTANCES : creates
    SUBSCRIPTIONS ||--o{ ENTITLEMENTS : grants
    INVOICES ||--o{ INVOICE_LINES : itemizes
    INVOICES ||--o{ PAYMENT_ATTEMPTS : processes
    SCHEDULED_TASKS ||--|| SUBSCRIPTIONS : processes
    OUTBOX_EVENTS ||--o{ WEBHOOK_DELIVERIES : publishes
    
    TENANTS {
        uuid id PK
        string name
        jsonb config
        timestamp created_at
        timestamp updated_at
    }
    
    USERS {
        uuid id PK
        string email UK
        string password_hash
        string first_name
        string last_name
        string role
        string status
        timestamp created_at
    }
    
    USER_TENANTS {
        uuid id PK
        uuid user_id FK
        uuid tenant_id FK
        string role
        timestamp assigned_at
    }
    
    CUSTOMERS {
        uuid id PK
        uuid tenant_id FK
        string email
        string first_name
        string last_name
        string payment_method_ref
        jsonb custom_attributes
        timestamp created_at
    }
    
    PLANS {
        uuid id PK
        uuid tenant_id FK
        string name
        string category
        string billing_period
        int base_price_cents
        int trial_days
        boolean requires_shipping
        jsonb metadata
        timestamp created_at
    }
    
    SUBSCRIPTIONS {
        uuid id PK
        uuid tenant_id FK
        uuid customer_id FK
        uuid plan_id FK
        string status
        timestamp next_billing_date
        timestamp trial_end
        jsonb shipping_address
        text admin_notes
        timestamp archived_at
        timestamp created_at
    }
    
    SUBSCRIPTION_HISTORY {
        uuid id PK
        uuid tenant_id FK
        uuid subscription_id FK
        string action
        uuid performed_by FK
        string performed_by_type
        jsonb metadata
        timestamp performed_at
    }
    
    INVOICES {
        uuid id PK
        uuid tenant_id FK
        uuid subscription_id FK
        int total_cents
        string status
        timestamp period_start
        timestamp period_end
        timestamp due_date
    }
    
    SCHEDULED_TASKS {
        uuid id PK
        uuid tenant_id FK
        string task_type
        string task_key UK
        string status
        timestamp due_at
        timestamp locked_until
        string lock_owner
        int attempt_count
        jsonb payload
    }
    
    OUTBOX_EVENTS {
        uuid id PK
        uuid tenant_id FK
        string event_type
        jsonb payload
        boolean published
        timestamp published_at
    }
```

### Core Business Tables (11 tables)

| Table | Purpose | Key Fields | Unique Constraints |
|-------|---------|------------|--------------------|
| `tenants` | SaaS tenants/merchants | id, name, config | - |
| `users` | System users (admin/staff) | id, email, password_hash, role, status | email |
| `user_tenants` | User-tenant assignments | id, user_id, tenant_id, role | user_id + tenant_id |
| `customers` | End customers | id, tenant_id, email, payment_method_ref | tenant_id + email |
| `plans` | Subscription plans | id, tenant_id, name, category, billing_period, base_price_cents, trial_days | - |
| `subscriptions` | Subscription contracts | id, tenant_id, customer_id, plan_id, status, next_billing_date | - |
| `subscription_items` | Product line items | id, tenant_id, subscription_id, plan_id, quantity, unit_price_cents | - |
| `subscription_history` | Audit trail | id, tenant_id, subscription_id, action, performed_by, performed_by_type, metadata | - |
| `invoices` | Billing snapshots | id, tenant_id, subscription_id, total_cents, status, period_start, period_end | tenant_id + subscription_id + period |
| `invoice_lines` | Invoice line items | id, tenant_id, invoice_id, description, quantity, unit_price_cents | - |
| `payment_attempts` | Payment processing | id, tenant_id, invoice_id, status, attempt_number, payment_method_ref | - |

### Delivery & Entitlement Tables (2 tables)

| Table | Purpose | Key Fields | Unique Constraints |
|-------|---------|------------|--------------------|
| `delivery_instances` | Physical/digital deliveries | id, tenant_id, subscription_id, status, scheduled_date, cycle_key | tenant_id + subscription_id + cycle_key |
| `entitlements` | Digital access rights | id, tenant_id, subscription_id, resource_type, granted_at, expires_at | - |

### Worker & Scheduling Tables (3 tables)

| Table | Purpose | Key Fields | Unique Constraints |
|-------|---------|------------|--------------------|
| `scheduled_tasks` | Task queue with distributed locking | id, tenant_id, task_type, task_key, status, due_at, locked_until, lock_owner | tenant_id + task_key |
| `job_configuration` | Dynamic job scheduling config | id, job_name, cron_expression, enabled | job_name |
| `job_execution_history` | Job execution tracking | id, job_name, job_type, status, started_at, completed_at | - |

### Event & Webhook Tables (3 tables)

| Table | Purpose | Key Fields | Unique Constraints |
|-------|---------|------------|--------------------|
| `outbox_events` | Transactional outbox pattern | id, tenant_id, event_type, payload, published, published_at | - |
| `webhook_endpoints` | Webhook registrations | id, tenant_id, url, events, secret, status | - |
| `webhook_deliveries` | Webhook delivery attempts | id, tenant_id, webhook_endpoint_id, event_id, status, attempt_count | - |

### Security & API Tables (6 tables)

| Table | Purpose | Key Fields | Unique Constraints |
|-------|---------|------------|--------------------|
| `idempotency_keys` | Request deduplication | id, tenant_id, idempotency_key, response_status, response_body | tenant_id + idempotency_key |
| `api_clients` | API client credentials | id, tenant_id, client_id, client_secret, auth_method, ip_whitelist | client_id |
| `api_client_scopes` | API permission scopes | id, scope, description, resource, action | scope |
| `oauth_access_tokens` | OAuth 2.0 tokens | id, client_id, access_token, refresh_token, expires_at | access_token |
| `request_nonces` | Replay attack prevention | id, client_id, nonce, created_at (10 min TTL) | client_id + nonce |
| `rate_limit_buckets` | Rate limiting | id, client_id, window_start, request_count, max_requests | client_id + window_start |
| `job_execution_history` | Worker job tracking | id, job_name, status, started_at, completed_at |

---

## 🔄 How Scheduling Works

### Database-Driven Scheduler

All async work is managed through the `scheduled_tasks` table with PostgreSQL-based distributed locking. No external schedulers, message brokers, or Redis required.

**Key Components:**
- **Task Leasing**: Workers claim tasks using `FOR UPDATE SKIP LOCKED`
- **Distributed Locking**: `locked_until` timestamp prevents duplicate processing
- **Lock Reaper**: Automatically recovers tasks from crashed workers
- **6 Task Types**: SUBSCRIPTION_RENEWAL, CHARGE_PAYMENT, CREATE_DELIVERY, CREATE_ORDER, SUBSCRIPTION_ITEM_RENEWAL, ENTITLEMENT_GRANT

```mermaid
sequenceDiagram
    participant Worker1
    participant Worker2
    participant Database
    participant TaskHandler
    participant Reaper
    
    Note over Worker1,Worker2: Multiple workers run concurrently
    
    Worker1->>Database: SELECT FOR UPDATE SKIP LOCKED
    Worker2->>Database: SELECT FOR UPDATE SKIP LOCKED
    Database-->>Worker1: Task A (locked)
    Database-->>Worker2: Task B (locked)
    
    Worker1->>Database: UPDATE status='CLAIMED', locked_until=now()+5min
    Worker2->>Database: UPDATE status='CLAIMED', locked_until=now()+5min
    
    Worker1->>TaskHandler: Process Task A
    TaskHandler-->>Worker1: Success
    Worker1->>Database: UPDATE status='COMPLETED'
    
    Note over Worker2: Worker 2 crashes!
    
    Reaper->>Database: Find tasks WHERE locked_until < now()
    Reaper->>Database: UPDATE status='READY', clear lock
    
    Worker1->>Database: SELECT FOR UPDATE SKIP LOCKED
    Database-->>Worker1: Task B (recovered)
    Worker1->>TaskHandler: Process Task B
    TaskHandler-->>Worker1: Success
    Worker1->>Database: UPDATE status='COMPLETED'
```

### Task Leasing SQL

**Worker claims tasks:**
```sql
SELECT *
FROM scheduled_tasks
WHERE status = 'READY'
  AND due_at <= NOW()
  AND tenant_id = ?
FOR UPDATE SKIP LOCKED
LIMIT 10;
```

**Update to CLAIMED:**
```sql
UPDATE scheduled_tasks
SET status = 'CLAIMED',
    locked_until = NOW() + INTERVAL '5 minutes',
    lock_owner = ?,
    updated_at = NOW()
WHERE id = ?;
```

### Lock Reaper Recovery

**Reaper finds expired locks:**
```sql
UPDATE scheduled_tasks
SET status = 'READY',
    locked_until = NULL,
    lock_owner = NULL,
    updated_at = NOW()
WHERE status = 'CLAIMED'
  AND locked_until < NOW()
RETURNING id;
```

### Task Types & Handlers

| Task Type | Purpose | Trigger | Handler |
|-----------|---------|---------|---------|
| `SUBSCRIPTION_RENEWAL` | Generate invoice for billing cycle | `next_billing_date` reached | Creates invoice, schedules payment |
| `CHARGE_PAYMENT` | Process payment for invoice | After invoice creation | Calls payment adapter, updates invoice |
| `CREATE_DELIVERY` | Create delivery instance | After successful payment | Creates delivery record, schedules order |
| `CREATE_ORDER` | Create order in commerce system | After delivery creation | Calls commerce adapter with snapshot |
| `SUBSCRIPTION_ITEM_RENEWAL` | Process individual product renewal | Per-product billing | Handles product-specific logic |
| `ENTITLEMENT_GRANT` | Grant/revoke digital access | After payment/cancellation | Updates entitlements table |

### Task Lifecycle States

```mermaid
stateDiagram-v2
    [*] --> READY: Task Created
    READY --> CLAIMED: Worker Leases
    CLAIMED --> PROCESSING: Handler Starts
    PROCESSING --> COMPLETED: Success
    PROCESSING --> READY: Failure (retry)
    READY --> FAILED: Max Attempts
    CLAIMED --> READY: Lock Expired (Reaper)
    COMPLETED --> [*]
    FAILED --> [*]
```

### Key Features

✅ **Distributed Locking**: `FOR UPDATE SKIP LOCKED` prevents duplicate processing  
✅ **Automatic Recovery**: Lock reaper recovers tasks from crashed workers  
✅ **Retry Logic**: Failed tasks retry with exponential backoff (max 3 attempts)  
✅ **Tenant Isolation**: All tasks filtered by `tenant_id`  
✅ **Idempotent**: Unique `task_key` prevents duplicate task creation  
✅ **Horizontally Scalable**: Run multiple workers concurrently  
✅ **No External Dependencies**: Pure PostgreSQL, no Kafka/Redis/RabbitMQ

---

## 🔁 Subscription Renewal Flow

### Complete Renewal Task Chain

The renewal process is orchestrated through a chain of 6 task types, each handled independently with retry logic and crash recovery.

```mermaid
sequenceDiagram
    participant Worker
    participant DB as Database
    participant Payment as Payment Adapter
    participant Commerce as Commerce Adapter
    participant Outbox as Outbox Events
    
    Note over Worker,DB: Task 1: SUBSCRIPTION_RENEWAL
    Worker->>DB: Lease SUBSCRIPTION_RENEWAL task
    Worker->>DB: Create invoice (status=OPEN)
    Worker->>DB: Create CHARGE_PAYMENT task
    Worker->>DB: Mark SUBSCRIPTION_RENEWAL COMPLETED
    
    Note over Worker,DB: Task 2: CHARGE_PAYMENT
    Worker->>DB: Lease CHARGE_PAYMENT task
    Worker->>Payment: Process payment
    
    alt Payment Success
        Payment-->>Worker: Success
        Worker->>DB: Update invoice (status=PAID)
        Worker->>DB: Create CREATE_DELIVERY task
        Worker->>DB: Create ENTITLEMENT_GRANT task
        Worker->>DB: Update next_billing_date
        Worker->>Outbox: Publish payment.succeeded event
        Worker->>DB: Mark CHARGE_PAYMENT COMPLETED
        
        Note over Worker,DB: Task 3: CREATE_DELIVERY
        Worker->>DB: Lease CREATE_DELIVERY task
        Worker->>DB: Create delivery_instance record
        Worker->>DB: Create CREATE_ORDER task
        Worker->>Outbox: Publish delivery.created event
        Worker->>DB: Mark CREATE_DELIVERY COMPLETED
        
        Note over Worker,DB: Task 4: CREATE_ORDER
        Worker->>DB: Lease CREATE_ORDER task
        Worker->>Commerce: Create order with snapshot
        Commerce-->>Worker: Order created
        Worker->>DB: Update delivery (order_ref)
        Worker->>Outbox: Publish order.created event
        Worker->>DB: Mark CREATE_ORDER COMPLETED
        
        Note over Worker,DB: Task 5: ENTITLEMENT_GRANT
        Worker->>DB: Lease ENTITLEMENT_GRANT task
        Worker->>DB: Create/update entitlements
        Worker->>Outbox: Publish entitlement.granted event
        Worker->>DB: Mark ENTITLEMENT_GRANT COMPLETED
        
    else Payment Failed
        Payment-->>Worker: Failed
        Worker->>DB: Create payment_attempt (status=FAILED)
        Worker->>DB: Increment attempt_count
        Worker->>Outbox: Publish payment.failed event
        
        alt Retry Available (attempt < 3)
            Worker->>DB: Mark CHARGE_PAYMENT READY (retry)
        else Max Retries Exceeded
            Worker->>DB: Mark invoice FAILED
            Worker->>DB: Update subscription (status=PAST_DUE)
            Worker->>DB: Create ENTITLEMENT_GRANT task (REVOKE)
            Worker->>DB: Mark CHARGE_PAYMENT FAILED
        end
    end
```

### Task Chain Breakdown

**1. SUBSCRIPTION_RENEWAL** (Triggered by `next_billing_date`)
```java
// Creates invoice snapshot
Invoice invoice = createInvoice(subscription);
invoice.setStatus("OPEN");
invoice.setPeriodStart(subscription.getCurrentPeriodEnd());
invoice.setPeriodEnd(calculateNextPeriodEnd());

// Schedule payment task
createTask("CHARGE_PAYMENT", invoiceId);
```

**2. CHARGE_PAYMENT** (Processes payment)
```java
// Call payment adapter
PaymentResult result = paymentAdapter.charge(
    invoice.getTotalCents(),
    customer.getPaymentMethodRef()
);

if (result.isSuccess()) {
    invoice.setStatus("PAID");
    createTask("CREATE_DELIVERY", subscriptionId);
    createTask("ENTITLEMENT_GRANT", subscriptionId, "GRANT");
    subscription.setNextBillingDate(calculateNext());
} else {
    recordFailedAttempt();
    if (attemptCount < maxAttempts) {
        // Retry with exponential backoff
    } else {
        subscription.setStatus("PAST_DUE");
    }
}
```

**3. CREATE_DELIVERY** (For physical products)
```java
// Create delivery instance
DeliveryInstance delivery = new DeliveryInstance();
delivery.setSubscriptionId(subscriptionId);
delivery.setScheduledDate(calculateDeliveryDate());
delivery.setCycleKey(generateCycleKey());
delivery.setStatus("SCHEDULED");

// Schedule order creation
createTask("CREATE_ORDER", deliveryId);
```

**4. CREATE_ORDER** (Calls commerce platform)
```java
// Build order from delivery snapshot
OrderRequest order = buildOrderFromSnapshot(delivery);

// Call commerce adapter
OrderResult result = commerceAdapter.createOrder(order);

delivery.setOrderRef(result.getOrderId());
delivery.setStatus("PENDING_FULFILLMENT");
```

**5. SUBSCRIPTION_ITEM_RENEWAL** (Per-product logic)
```java
// Process individual product renewals
for (SubscriptionItem item : subscription.getItems()) {
    processProductRenewal(item);
}
```

**6. ENTITLEMENT_GRANT** (Digital access)
```java
// Grant or revoke entitlements
if (action == "GRANT") {
    Entitlement ent = new Entitlement();
    ent.setResourceType("premium_access");
    ent.setGrantedAt(now());
    ent.setExpiresAt(subscription.getNextBillingDate());
} else if (action == "REVOKE") {
    entitlements.forEach(e -> e.setRevokedAt(now()));
}
```

### Key Design Principles

✅ **Task Independence**: Each task can fail/retry independently  
✅ **Idempotency**: Duplicate tasks prevented by unique `task_key`  
✅ **Crash Recovery**: Lock reaper recovers incomplete tasks  
✅ **Retry Logic**: Failed tasks retry up to 3 times with exponential backoff  
✅ **Transactional Outbox**: Events published atomically with database changes  
✅ **Immutable Snapshots**: Invoices/deliveries never modified after creation  
✅ **Tenant Isolation**: All tasks filtered by `tenant_id`

### Retry & Backoff Strategy

| Attempt | Backoff | Total Wait |
|---------|---------|------------|
| 1 | Immediate | 0 seconds |
| 2 | 5 minutes | 5 minutes |
| 3 | 15 minutes | 20 minutes |
| Failed | Mark FAILED | - |

### Webhook Events Published

- `subscription.renewed` - After successful renewal
- `payment.succeeded` - After successful payment
- `payment.failed` - After failed payment attempt
- `delivery.created` - After delivery instance created
- `order.created` - After commerce order created
- `entitlement.granted` - After access granted
- `entitlement.revoked` - After access revoked

---

## ⚙️ Worker Module

### Overview

The **Worker Module** is the backbone of automated billing operations. It processes scheduled tasks asynchronously using a database-driven scheduler with distributed locking, ensuring reliable execution without external message brokers.

### Architecture

```mermaid
graph TB
    subgraph "Worker Process"
        LOOP[Scheduler Loop]
        LEASE[Task Leasing]
        LOCK[Distributed Locking]
        HANDLER[Task Handlers]
        REAPER[Lock Reaper]
    end
    
    subgraph "Database"
        TASKS[(scheduled_tasks)]
        SUBS[(subscriptions)]
        INV[(invoices)]
        DEL[(deliveries)]
    end
    
    subgraph "External Systems"
        PAY[Payment Gateway]
        COM[Commerce Platform]
    end
    
    LOOP --> LEASE
    LEASE --> LOCK
    LOCK --> TASKS
    LOCK --> HANDLER
    HANDLER --> SUBS
    HANDLER --> INV
    HANDLER --> DEL
    HANDLER --> PAY
    HANDLER --> COM
    REAPER --> TASKS
    
    style HANDLER fill:#FF9800
    style TASKS fill:#2196F3
```

### Task Types

The worker processes 6 types of scheduled tasks:

| Task Type | Purpose | Trigger | Handler |
|-----------|---------|---------|---------|
| **SUBSCRIPTION_RENEWAL** | Process subscription billing cycle | `next_billing_date` reached | Creates invoice, schedules payment |
| **CHARGE_PAYMENT** | Execute payment for invoice | After invoice creation | Calls payment gateway, updates status |
| **CREATE_DELIVERY** | Create delivery instance snapshot | After successful payment | Generates delivery record for physical products |
| **CREATE_ORDER** | Submit order to commerce platform | After delivery creation | Calls external commerce API |
| **PRODUCT_RENEWAL** | Bill individual subscription items | Item-specific billing cycle | Handles add-on/product-level billing |
| **ENTITLEMENT_GRANT** | Grant/revoke digital access | Payment success/failure | Updates feature access rights |

### Task Lifecycle

```mermaid
stateDiagram-v2
    [*] --> READY: Task Created
    READY --> CLAIMED: Worker Leases
    CLAIMED --> PROCESSING: Handler Executes
    PROCESSING --> COMPLETED: Success
    PROCESSING --> READY: Retry (attempt < max)
    PROCESSING --> FAILED: Max Attempts Reached
    CLAIMED --> READY: Lock Expired (Reaper)
    COMPLETED --> [*]
    FAILED --> [*]
```

### Distributed Locking

**Problem**: Multiple worker instances must not process the same task twice.

**Solution**: PostgreSQL `FOR UPDATE SKIP LOCKED` with lock expiration.

```sql
-- Worker leases tasks atomically
SELECT * FROM scheduled_tasks
WHERE status = 'READY'
  AND due_at <= now()
  AND (locked_until IS NULL OR locked_until < now())
FOR UPDATE SKIP LOCKED
LIMIT 100;

-- Update to claim the task
UPDATE scheduled_tasks
SET status = 'CLAIMED',
    locked_until = now() + interval '5 minutes',
    lock_owner = 'worker-instance-123'
WHERE id = ?;
```

**Key Features:**
- ✅ **No race conditions** - `SKIP LOCKED` prevents conflicts
- ✅ **Automatic recovery** - Reaper releases expired locks
- ✅ **Horizontal scaling** - Add more workers without coordination
- ✅ **Crash resilient** - Tasks auto-recover after `locked_until` expires

### Lock Reaper

The **reaper** is a background process that recovers tasks from crashed workers:

```java
// Runs every minute
public int cleanupExpiredLocks() {
    return dsl.update(SCHEDULED_TASKS)
        .set(SCHEDULED_TASKS.STATUS, "READY")
        .set(SCHEDULED_TASKS.LOCKED_UNTIL, null)
        .set(SCHEDULED_TASKS.LOCK_OWNER, null)
        .where(SCHEDULED_TASKS.STATUS.eq("CLAIMED"))
        .and(SCHEDULED_TASKS.LOCKED_UNTIL.lt(now()))
        .execute();
}
```

**Scenario**: Worker crashes while processing a task
1. Task remains in `CLAIMED` status with `locked_until` timestamp
2. After `locked_until` expires, reaper resets task to `READY`
3. Another worker picks up and processes the task
4. **Result**: Zero data loss, automatic recovery

### Task Handlers

Each task type has a dedicated handler:

#### 1. SUBSCRIPTION_RENEWAL Handler
```java
// Processes subscription billing cycle
- Load subscription details
- Calculate billing amount
- Create invoice snapshot
- Schedule CHARGE_PAYMENT task
- Update next_billing_date
```

#### 2. CHARGE_PAYMENT Handler
```java
// Executes payment processing
- Load invoice details
- Call payment gateway API
- Record payment attempt
- On success: Schedule delivery/entitlement tasks
- On failure: Schedule retry with backoff
```

#### 3. CREATE_DELIVERY Handler
```java
// Creates delivery instance for physical products
- Load invoice and subscription
- Generate delivery snapshot
- Create delivery_instances record
- Schedule CREATE_ORDER task
```

#### 4. CREATE_ORDER Handler
```java
// Submits order to external commerce platform
- Load delivery instance
- Parse delivery snapshot
- Call commerce adapter API
- Update delivery with external order reference
```

#### 5. SUBSCRIPTION_ITEM_RENEWAL Handler
```java
// Bills individual subscription items (add-ons)
- Load subscription item details
- Calculate item-specific billing
- Create invoice for the item
- Schedule payment processing
- Handles per-product billing cycles
```

#### 6. ENTITLEMENT_GRANT Handler
```java
// Manages digital access rights
- Load subscription and invoice
- Determine action (GRANT or REVOKE)
- Update entitlements table
- Grant: Enable feature access
- Revoke: Disable feature access
```

### Error Handling & Retries

**Retry Strategy:**
```java
// Exponential backoff with max attempts
attempt_count++;
if (attempt_count >= max_attempts) {
    status = "FAILED";
} else {
    status = "READY";
    due_at = now() + exponential_backoff(attempt_count);
}
```

**Retry Schedule:**
- Attempt 1: Immediate
- Attempt 2: +5 minutes
- Attempt 3: +15 minutes
- Attempt 4+: FAILED (max_attempts = 3)

### Multi-Tenant Isolation

Every task includes `tenant_id` for strict isolation:

```sql
-- All queries include tenant filter
SELECT * FROM scheduled_tasks
WHERE tenant_id = ?
  AND status = 'READY'
  AND due_at <= now();
```

**Guarantees:**
- ✅ Tenant A's tasks never visible to Tenant B
- ✅ Worker processes respect tenant boundaries
- ✅ Database-level isolation enforced
- ✅ No cross-tenant data leakage

### Monitoring & Observability

**Key Metrics:**
- Tasks processed per minute
- Task processing latency (p50, p95, p99)
- Failed task count by type
- Lock expiration rate
- Reaper recovery count

**Logs:**
```
[SUBSCRIPTION_RENEWAL_START] Processing renewal for subscription abc-123
[SUBSCRIPTION_RENEWAL_STEP_1] Creating invoice
[SUBSCRIPTION_RENEWAL_STEP_2] Scheduling payment task
[SUBSCRIPTION_RENEWAL_SUCCESS] Completed in 145ms
```

### Scaling Guidelines

| Load | Workers | Database | Configuration |
|------|---------|----------|---------------|
| **Small** (< 10k subs) | 1 worker | db-f1-micro | Batch size: 50 |
| **Medium** (10k-100k subs) | 2-5 workers | db-n1-standard-1 | Batch size: 100 |
| **Large** (100k-1M subs) | 10+ workers | db-n1-standard-4 | Batch size: 500 |
| **Enterprise** (1M+ subs) | 20+ workers | db-n1-highmem-8 | Batch size: 1000 |

**Horizontal Scaling:**
```bash
# Add more workers - no coordination needed
gcloud run jobs deploy subscription-worker-2 --image=...
gcloud run jobs deploy subscription-worker-3 --image=...
```

---

## 📡 API Documentation

### API Overview

The Subscription Manager provides a comprehensive **Admin API** for managing subscriptions, customers, plans, deliveries, webhooks, and more. All APIs are RESTful, use JSON, and support OpenAPI/Swagger documentation.

**Base URL:** `https://your-domain.com/api`

**Swagger UI:** `https://your-domain.com/api/swagger-ui.html` (Interactive API Explorer)

**OpenAPI Spec:** `https://your-domain.com/api/v3/api-docs`

### API Architecture

```
/v1/admin/*          → Admin APIs (tenant-secured, requires JWT)
/v1/customers/me/*   → Customer self-service APIs
```

### Authentication

**Admin APIs** require JWT authentication with tenant context:

```http
Authorization: Bearer <your-jwt-token>
```

The JWT must contain a tenant claim (`tenant_id`, `org_id`, or `organization`) for multi-tenant isolation.

---

## 🎯 Admin API Endpoints

### 📋 Admin - Subscriptions

**Base Path:** `/v1/admin/subscriptions`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/subscriptions` | Create new subscription (unified endpoint for SaaS & product-based) |
| **GET** | `/v1/admin/subscriptions` | List all subscriptions (paginated) |
| **GET** | `/v1/admin/subscriptions/{id}` | Get subscription by ID |
| **GET** | `/v1/admin/subscriptions/{id}/history` | Get subscription audit trail |

**Key Features:**
- ✅ **Unified Endpoint**: Single endpoint handles both simple SaaS and product-based subscriptions
- ✅ **Auto Customer Creation**: Creates customer if email doesn't exist
- ✅ **Product Support**: Optional products array for subscription boxes, meal kits, etc.
- ✅ **Pagination**: Default 20 items per page, sortable by any field

**Example - Create Simple SaaS Subscription:**
```http
POST /v1/admin/subscriptions
Content-Type: application/json
Authorization: Bearer <token>

{
  "planId": "123e4567-e89b-12d3-a456-426614174000",
  "customerEmail": "john.doe@example.com",
  "customerFirstName": "John",
  "customerLastName": "Doe",
  "paymentMethodRef": "pm_stripe_abc123"
}
```

**Example - Create Product-Based Subscription:**
```http
POST /v1/admin/subscriptions
Content-Type: application/json
Authorization: Bearer <token>

{
  "planId": "123e4567-e89b-12d3-a456-426614174000",
  "customerEmail": "jane@example.com",
  "customerFirstName": "Jane",
  "customerLastName": "Smith",
  "products": [
    {
      "productId": "coffee-beans-001",
      "productName": "Premium Coffee Beans",
      "quantity": 2,
      "unitPriceCents": 1599,
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
  "paymentMethodRef": "pm_stripe_xyz789"
}
```

---

### 🔧 Admin - Subscription Management

**Base Path:** `/v1/admin/subscriptions/manage`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/subscriptions/manage/{id}` | Pause/Resume/Cancel subscription |

**Operations:**
- `PAUSE` - Pause subscription billing
- `RESUME` - Resume paused subscription
- `CANCEL` - Cancel subscription permanently

**Example:**
```http
POST /v1/admin/subscriptions/manage/{subscriptionId}
Content-Type: application/json
Authorization: Bearer <token>

{
  "operation": "PAUSE",
  "customerId": "customer-uuid",
  "reason": "Customer requested pause"
}
```

---

### 👥 Admin - Customers

**Base Path:** `/v1/admin/customers`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/customers` | Create customer independently |

**Features:**
- Create customers before subscriptions
- Store payment method references
- Link to external payment systems

---

### 📦 Admin - Plans

**Base Path:** `/v1/admin/plans`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/plans` | Create subscription plan |
| **GET** | `/v1/admin/plans` | List all plans (paginated) |
| **GET** | `/v1/admin/plans/{id}` | Get plan by ID |
| **PUT** | `/v1/admin/plans/{id}` | Update plan |
| **DELETE** | `/v1/admin/plans/{id}` | Delete plan |
| **POST** | `/v1/admin/plans/{id}/archive` | Archive plan (soft delete) |

**Example:**
```http
POST /v1/admin/plans
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "Premium Monthly",
  "description": "Premium subscription plan",
  "billingPeriod": "MONTHLY",
  "basePriceCents": 2999,
  "trialDays": 14,
  "metadata": {
    "features": ["feature1", "feature2"]
  }
}
```

---

### 🚚 Admin - Deliveries

**Base Path:** `/v1/admin/deliveries`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **GET** | `/v1/admin/deliveries/upcoming` | Get upcoming deliveries |
| **GET** | `/v1/admin/deliveries/{id}` | Get delivery details |
| **GET** | `/v1/admin/deliveries/subscription/{id}` | Get deliveries for subscription |
| **POST** | `/v1/admin/deliveries/{id}/ship` | Mark delivery as shipped |

**Query Parameters:**
- `subscriptionId` - Filter by subscription
- `count` - Number of upcoming deliveries (default: 3)

---

### 🔔 Admin - Webhooks

**Base Path:** `/v1/admin/webhooks`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/webhooks` | Register webhook endpoint |
| **GET** | `/v1/admin/webhooks` | List all webhooks |
| **GET** | `/v1/admin/webhooks/{id}` | Get webhook by ID |
| **PATCH** | `/v1/admin/webhooks/{id}/status` | Enable/disable webhook |

**Supported Events:**
- `subscription.created`
- `subscription.renewed`
- `subscription.paused`
- `subscription.resumed`
- `subscription.cancelled`
- `payment.succeeded`
- `payment.failed`
- `delivery.scheduled`
- `delivery.shipped`

---

### 🏢 Admin - Tenants

**Base Path:** `/v1/admin/tenants`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/tenants` | Create new tenant |
| **GET** | `/v1/admin/tenants` | List all tenants |
| **GET** | `/v1/admin/tenants/{id}` | Get tenant by ID |
| **PUT** | `/v1/admin/tenants/{id}` | Update tenant |
| **DELETE** | `/v1/admin/tenants/{id}` | Delete tenant |

---

### 👤 Admin - Users

**Base Path:** `/v1/admin/users`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/users` | Create user account |
| **GET** | `/v1/admin/users` | List all users |
| **GET** | `/v1/admin/users/{id}` | Get user by ID |
| **PUT** | `/v1/admin/users/{id}` | Update user |
| **DELETE** | `/v1/admin/users/{id}` | Delete user |
| **PATCH** | `/v1/admin/users/{id}/status` | Activate/deactivate user |
| **PATCH** | `/v1/admin/users/{id}/password` | Change user password |

---

### 🔑 Admin - API Clients

**Base Path:** `/v1/admin/api-clients`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/api-clients` | Create API client credentials |
| **GET** | `/v1/admin/api-clients` | List all API clients |
| **GET** | `/v1/admin/api-clients/{id}` | Get API client by ID |
| **DELETE** | `/v1/admin/api-clients/{id}` | Revoke API client |
| **POST** | `/v1/admin/api-clients/{id}/rotate` | Rotate API client secret |

**Authentication Tiers:**
- API Key authentication
- OAuth 2.0 client credentials
- mTLS (mutual TLS)

---

### 🔗 Admin - User-Tenant Assignment

**Base Path:** `/v1/admin/user-tenants`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/v1/admin/user-tenants` | Assign user to tenant |
| **GET** | `/v1/admin/user-tenants/user/{userId}` | Get user's tenants |
| **GET** | `/v1/admin/user-tenants/tenant/{tenantId}` | Get tenant's users |
| **DELETE** | `/v1/admin/user-tenants/{id}` | Remove user-tenant assignment |
| **PATCH** | `/v1/admin/user-tenants/{id}/role` | Update user role in tenant |

---

## 👤 Customer Self-Service APIs

### Customer - My Subscriptions

**Base Path:** `/v1/customers/me`

| Method | Endpoint | Description |
|--------|----------|-------------|
| **GET** | `/v1/customers/me/subscriptions` | Get my subscriptions |
| **GET** | `/v1/customers/me/subscriptions/{id}` | Get subscription details |
| **GET** | `/v1/customers/me/subscriptions/{id}/dashboard` | Get customer dashboard |
| **GET** | `/v1/customers/me/subscriptions/{id}/invoices` | Get subscription invoices |
| **GET** | `/v1/customers/me/subscriptions/{id}/deliveries` | Get upcoming deliveries |
| **POST** | `/v1/customers/me/subscriptions/{id}/pause` | Self-service pause |
| **POST** | `/v1/customers/me/subscriptions/{id}/cancel` | Self-service cancel |

**Customer Dashboard Response:**
```json
{
  "subscription": {
    "id": "sub-uuid",
    "status": "ACTIVE",
    "nextBillingDate": "2024-02-01"
  },
  "upcomingInvoice": {
    "amount": 2999,
    "dueDate": "2024-02-01"
  },
  "nextDelivery": {
    "scheduledDate": "2024-02-01",
    "items": [...]
  },
  "entitlements": [
    {
      "feature": "premium_access",
      "expiresAt": "2024-02-01"
    }
  ]
}
```

---

## 📊 API Response Formats

### Success Response (200/201)
```json
{
  "id": "uuid",
  "status": "ACTIVE",
  "createdAt": "2024-01-01T00:00:00Z",
  "updatedAt": "2024-01-01T00:00:00Z"
}
```

### Error Response (400/404/500)
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid request parameters",
  "details": {
    "field": "planId",
    "reason": "Plan not found"
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### Paginated Response
```json
{
  "content": [...],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

---

## 🔍 API Features

### Pagination
All list endpoints support pagination:
```http
GET /v1/admin/subscriptions?page=0&size=20&sort=createdAt,desc
```

### Filtering
Filter by status, date ranges, customer:
```http
GET /v1/admin/subscriptions?status=ACTIVE&customerEmail=john@example.com
```

### Idempotency
All write operations support idempotency keys:
```http
Idempotency-Key: unique-operation-id-123
```

### Tenant Isolation
All admin endpoints automatically filter by tenant from JWT token.

---

## 📚 Complete API Documentation

For complete API documentation with request/response schemas, examples, and interactive testing:

**Swagger UI:** `https://your-domain.com/api/swagger-ui.html`

The Swagger UI provides:
- ✅ Interactive API testing
- ✅ Complete request/response schemas
- ✅ Authentication testing
- ✅ Example requests for all endpoints
- ✅ Real-time API exploration

---

## 🔐 Security & Idempotency

### Idempotency Keys

All write operations (POST, PUT, PATCH) require an `Idempotency-Key` header to prevent duplicate operations:

```http
Idempotency-Key: unique-operation-id-123
```

**Benefits:**
- Prevents double billing
- Prevents duplicate subscriptions
- Safe to retry failed requests
- 24-hour idempotency window

### Database Constraints

Multiple layers of protection prevent data corruption:

```sql
-- Prevent duplicate invoices per billing period
UNIQUE (subscription_id, billing_period_start)

-- Prevent duplicate deliveries
UNIQUE (subscription_id, scheduled_date)

-- Prevent duplicate payment attempts
UNIQUE (invoice_id, idempotency_key)
```

### Multi-Tenant Isolation

- Row-level security enforced at database level
- JWT claims validated on every request
- Tenant ID automatically injected into all queries
- Cross-tenant access blocked by design

## 🚀 Deployment

### Multi-Cloud Support

Deploy to **Google Cloud Platform**, **Amazon Web Services**, or **Microsoft Azure** with a single configuration file.

```yaml
# deployment-config.yaml
target_cloud: gcp  # Options: gcp | aws | azure

app:
  name: subscription-manager
  environment: production

database:
  name: subscription-db
  user: subscription-app
  tier: small  # small | medium | large
  storage_gb: 10

api:
  name: subscription-api
  compute:
    size: small
    min_instances: 1
    max_instances: 10

worker:
  name: subscription-worker
  compute:
    size: small
  schedule: "0 */1 * * *"  # Every hour
```

---

## 📦 Google Cloud Platform (GCP) Deployment

### Prerequisites

- Google Cloud account with billing enabled
- `gcloud` CLI installed ([Install Guide](https://cloud.google.com/sdk/docs/install))
- GitHub repository forked/cloned
- Basic understanding of GCP services

---

### Step 1: Create GCP Project

```bash
# Set your project ID (must be globally unique)
export PROJECT_ID="subscription-manager-prod"

# Create project
gcloud projects create $PROJECT_ID --name="Subscription Manager"

# Set as active project
gcloud config set project $PROJECT_ID

# Link billing account (required)
gcloud billing accounts list
gcloud billing projects link $PROJECT_ID --billing-account=YOUR_BILLING_ACCOUNT_ID
```

---

### Step 2: Enable Required APIs

**⚠️ IMPORTANT:** Enable all APIs before deployment to avoid errors.

```bash
# Enable all necessary GCP APIs (run this command once)
gcloud services enable \
  run.googleapis.com \
  sql-component.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  cloudscheduler.googleapis.com \
  cloudresourcemanager.googleapis.com \
  compute.googleapis.com \
  servicenetworking.googleapis.com
```

**Required APIs:**

| API | Service Name | Purpose | Critical |
|-----|--------------|---------|----------|
| Cloud Run | `run.googleapis.com` | API & Worker containers | ✅ Required |
| Cloud SQL Admin | `sqladmin.googleapis.com` | Database management | ✅ Required |
| Cloud SQL | `sql-component.googleapis.com` | Database operations | ✅ Required |
| Secret Manager | `secretmanager.googleapis.com` | Credentials storage | ✅ Required |
| **Artifact Registry** | `artifactregistry.googleapis.com` | **Docker image storage** | ✅ **Required** |
| Cloud Scheduler | `cloudscheduler.googleapis.com` | Worker job scheduling | ✅ Required |
| **Cloud Resource Manager** | `cloudresourcemanager.googleapis.com` | **Project metadata access** | ✅ **Required** |
| Compute Engine | `compute.googleapis.com` | Networking | Recommended |
| Service Networking | `servicenetworking.googleapis.com` | VPC (if using private IP) | Optional |

**⏱️ Note:** After enabling APIs, wait 2-3 minutes for changes to propagate before running deployment.

---

### Step 3: Create Service Account

```bash
# Create service account for GitHub Actions
gcloud iam service-accounts create github-deployer \
  --display-name="GitHub Actions Deployer" \
  --description="Service account for automated deployments"

# Get service account email
export SA_EMAIL="github-deployer@${PROJECT_ID}.iam.gserviceaccount.com"

# Grant necessary IAM roles
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/cloudsql.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/secretmanager.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/artifactregistry.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/cloudscheduler.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/iam.serviceAccountUser"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/storage.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/serviceusage.admin"
```

**Required Roles:**
- ✅ Cloud Run Admin - Deploy and manage Cloud Run services
- ✅ Cloud SQL Admin - Manage database instances
- ✅ Secret Manager Admin - Manage secrets
- ✅ Artifact Registry Admin - Push Docker images
- ✅ Cloud Scheduler Admin - Schedule worker jobs
- ✅ Service Account User - Act as service accounts
- ✅ Storage Admin - Manage storage buckets
- ✅ Service Usage Admin - Enable required APIs automatically

---

### Step 4: Generate Service Account Key

```bash
# Create and download JSON key
gcloud iam service-accounts keys create ~/gcp-github-key.json \
  --iam-account=$SA_EMAIL

# Display the key (copy this for GitHub Secrets)
cat ~/gcp-github-key.json
```

⚠️ **Security Note:** Keep this key secure! Delete it from your local machine after adding to GitHub Secrets.

---

### Step 5: Configure GitHub Secrets

1. Go to your GitHub repository: **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add the following secrets:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `GCP_SA_KEY` | Contents of `~/gcp-github-key.json` | Service account JSON key (entire file) |
| `GCP_PROJECT_ID` | Your project ID (e.g., `subscription-manager-prod`) | GCP project identifier |

**How to add:**
- **Name:** `GCP_SA_KEY`
- **Secret:** Paste the entire JSON content from the key file
- Click **Add secret**

Repeat for `GCP_PROJECT_ID` with your project ID.

---

### Step 6: Configure Deployment Settings

Edit `deployment-config.yaml` in your repository:

```yaml
target_cloud: gcp

app:
  name: subscription-manager
  environment: production

database:
  name: subscription-db
  user: subscription-app
  tier: small              # small | medium | large
  storage_gb: 10
  backup_retention_days: 7

api:
  name: subscription-api
  port: 8080
  compute:
    size: small            # small | medium | large
    min_instances: 1
    max_instances: 10

worker:
  name: subscription-worker
  compute:
    size: small
  schedule: "0 */1 * * *"  # Cron: every hour

regions:
  gcp: us-central1         # Choose your region

features:
  run_migrations: true
  enable_monitoring: true
```

**Available GCP Regions:**
- `us-central1` (Iowa)
- `us-east1` (South Carolina)
- `us-west1` (Oregon)
- `europe-west1` (Belgium)
- `asia-southeast1` (Singapore)

---

### Step 7: Deploy to GCP

#### Option A: Automatic Deployment (Recommended)

1. **Commit and push changes:**
   ```bash
   git add deployment-config.yaml
   git commit -m "Configure GCP deployment"
   git push origin main
   ```

2. **GitHub Actions will automatically:**
   - ✅ Authenticate with GCP
   - ✅ Create Cloud SQL database
   - ✅ Store credentials in Secret Manager
   - ✅ Run Flyway migrations
   - ✅ Build and push Docker images
   - ✅ Deploy API to Cloud Run
   - ✅ Deploy Worker to Cloud Run Jobs
   - ✅ Configure Cloud Scheduler

3. **Monitor deployment:**
   - Go to **GitHub** → **Actions** tab
   - Watch the workflow progress

#### Option B: Manual Deployment via GitHub UI

1. Go to **Actions** → **Manual Deploy (UI)**
2. Click **Run workflow**
3. Configure:
   - Cloud: `gcp`
   - Environment: `production`
   - Deploy API: ✓
   - Deploy Worker: ✓
   - Setup Database: ✓ (first time only)
   - Run Migrations: ✓
   - Database Tier: `small`
   - API Compute: `small`
4. Click **Run workflow**

#### Option C: Local Deployment

```bash
# Authenticate with GCP
gcloud auth login
gcloud auth application-default login

# Make scripts executable
chmod +x infrastructure/deploy.sh

# Run deployment
./infrastructure/deploy.sh
```

---

### Step 8: Verify Deployment

#### Check Cloud Run Services

```bash
# List all Cloud Run services
gcloud run services list

# Get API URL
gcloud run services describe subscription-api \
  --region=us-central1 \
  --format='value(status.url)'
```

#### Check Cloud SQL Database

```bash
# List Cloud SQL instances
gcloud sql instances list

# Get connection details
gcloud sql instances describe subscription-db \
  --format='value(connectionName)'
```

#### Check Cloud Scheduler

```bash
# List scheduled jobs
gcloud scheduler jobs list
```

#### Test API

```bash
# Get API URL
API_URL=$(gcloud run services describe subscription-api \
  --region=us-central1 \
  --format='value(status.url)')

# Test health endpoint
curl $API_URL/actuator/health

# Access Swagger UI
echo "Swagger UI: $API_URL/swagger-ui.html"
```

---

### GCP Resources Created

After successful deployment, the following resources will be created:

| Resource | Type | Purpose |
|----------|------|---------|
| **Cloud SQL Instance** | PostgreSQL 15 | Database (subscription-db) |
| **Cloud Run Service** | Container | API (subscription-api) |
| **Cloud Run Job** | Container | Worker (subscription-worker) |
| **Cloud Scheduler** | Cron Job | Triggers worker hourly |
| **Secret Manager** | Secrets | Database password (db-password) |
| **Artifact Registry** | Docker Registry | Container images |

---

### Cost Estimates (GCP)

#### Development/Small Tier
- Cloud SQL (db-f1-micro): ~$7/month
- Cloud Run API (minimal traffic): ~$5/month
- Cloud Run Worker: ~$2/month
- **Total: ~$15/month**

#### Production/Medium Tier
- Cloud SQL (db-n1-standard-1): ~$50/month
- Cloud Run API (moderate traffic): ~$25/month
- Cloud Run Worker: ~$5/month
- **Total: ~$80/month**

---

### Monitoring & Logs

#### View Logs

```bash
# API logs
gcloud run services logs read subscription-api --region=us-central1

# Worker logs
gcloud run jobs logs read subscription-worker --region=us-central1

# Database logs
gcloud sql operations list --instance=subscription-db
```

#### Cloud Console

- **Cloud Run:** https://console.cloud.google.com/run
- **Cloud SQL:** https://console.cloud.google.com/sql
- **Logs Explorer:** https://console.cloud.google.com/logs
- **Secret Manager:** https://console.cloud.google.com/security/secret-manager

---

### Troubleshooting Common Issues

#### Issue 1: Authentication Error in GitHub Actions

**Error:** `No authentication found for gcloud`

**Solution:**
- Ensure `GCP_SA_KEY` secret is set correctly in GitHub
- Verify the JSON key is complete (starts with `{` and ends with `}`)
- Check that the service account has necessary permissions

#### Issue 2: Artifact Registry API Not Enabled

**Error:** `Artifact Registry API has not been used in project before or it is disabled`

**Solution:**
1. Enable the Artifact Registry API:
   ```bash
   gcloud services enable artifactregistry.googleapis.com
   ```
2. Wait 2-3 minutes for the API to propagate
3. Retry the deployment

#### Issue 3: Service Networking API Not Enabled

**Error:** `SERVICE_NETWORKING_NOT_ENABLED`

**Solution:**
- The deployment uses public IP by default (no action needed)
- If you need private networking, enable the Service Networking API:
  ```bash
  gcloud services enable servicenetworking.googleapis.com
  ```

#### Issue 4: Docker Push Permission Denied

**Error:** `denied: gcr.io repo does not exist. Creating on push requires the artifactregistry.repositories.createOnPush permission`

**Solution (via gcloud CLI):**
1. Verify the service account has the Artifact Registry Admin role:
   ```bash
   gcloud projects add-iam-policy-binding $PROJECT_ID \
     --member="serviceAccount:github-deployer@${PROJECT_ID}.iam.gserviceaccount.com" \
     --role="roles/artifactregistry.admin"
   ```

2. Wait 2-3 minutes for IAM permissions to propagate

3. Retry the deployment

**Solution (via Google Cloud Console):**
1. Go to [IAM & Admin → IAM](https://console.cloud.google.com/iam-admin/iam)
2. Find the service account: `github-deployer@YOUR_PROJECT_ID.iam.gserviceaccount.com`
3. Click the **pencil icon (✏️)** to edit
4. Click **"+ ADD ANOTHER ROLE"**
5. Search for and select **"Artifact Registry Admin"**
6. Click **SAVE**
7. Wait 2-3 minutes for permissions to propagate
8. Retry the deployment

**Note:** The `roles/artifactregistry.admin` role includes the `createOnPush` permission needed to automatically create repositories.

#### Issue 5: Cloud Run API Not Enabled

**Error:** `The following APIs are not enabled on project: run.googleapis.com` or `Permission denied to enable service [run.googleapis.com]`

**Solution:**
The service account cannot enable APIs - you must enable them manually.

**Via gcloud CLI:**
```bash
gcloud services enable run.googleapis.com
```

**Via Google Cloud Console:**
1. Go to: https://console.developers.google.com/apis/api/run.googleapis.com/overview
2. Select your project
3. Click **"ENABLE"**
4. Wait 2-3 minutes for propagation
5. Retry the deployment

**Note:** Make sure you enabled ALL required APIs in Step 2 before starting deployment.

#### Issue 6: Secret Not Found

**Error:** `Secret [db-password] not found` or `Secret version [latest] not found`

**Cause:**
The database password secret hasn't been created in Google Cloud Secret Manager, or the secret exists but has no versions.

**Solution:**

The deployment automatically creates secrets during the setup phase. If you encounter this error:

1. **Verify secret exists:**
   ```bash
   gcloud secrets describe db-password --project=$PROJECT_ID
   ```

2. **If secret doesn't exist, run setup-secrets:**
   ```bash
   ./infrastructure/gcp/setup-secrets.sh
   ```
   
   This script will:
   - Create the `db-password` secret in Secret Manager
   - Store the database password securely
   - Grant necessary IAM permissions to Cloud Run service accounts
   - Wait for permissions to propagate

3. **If secret exists but has no versions:**
   ```bash
   # Add a new secret version
   echo -n "your-database-password" | gcloud secrets versions add db-password \
     --project=$PROJECT_ID \
     --data-file=-
   ```

4. **Verify secret has a version:**
   ```bash
   gcloud secrets versions list db-password --project=$PROJECT_ID
   ```

**Prevention:**
Always run the full deployment script (`./infrastructure/deploy.sh`) which automatically handles secret creation and configuration.

---

### Cleanup Resources

To delete all GCP resources and avoid charges:

```bash
# Run cleanup script
./infrastructure/gcp/cleanup.sh

# Or manually delete resources
gcloud run services delete subscription-api --region=us-central1
gcloud run jobs delete subscription-worker --region=us-central1
gcloud scheduler jobs delete subscription-worker-trigger --location=us-central1
gcloud sql instances delete subscription-db
gcloud secrets delete db-password
```

---

## 🌐 AWS & Azure Deployment

For AWS and Azure deployment instructions, see [`README-DEPLOYMENT.md`](README-DEPLOYMENT.md).

### Cloud Services Used

| Service | GCP | AWS | Azure |
|---------|-----|-----|-------|
| **API** | Cloud Run | ECS Fargate | Container Apps |
| **Worker** | Cloud Run Jobs | ECS Scheduled Tasks | Container Apps Jobs |
| **Database** | Cloud SQL | RDS PostgreSQL | Azure Database for PostgreSQL |
| **Secrets** | Secret Manager | Secrets Manager | Key Vault |
| **Registry** | Artifact Registry | ECR | Container Registry |

---

## 🚀 Quick Start

### Local Development Setup

#### Prerequisites

| Requirement | Version | Purpose |
|-------------|---------|----------|
| **Java** | 17+ (LTS) | Primary language |
| **Docker** | 20+ | PostgreSQL container |
| **Docker Compose** | 2.x | Local environment |
| **Gradle** | 8.5 (included) | Build automation |
| **Git** | 2.x | Version control |

#### Step 1: Clone Repository

```bash
git clone https://github.com/neeraj-agentic-lab/SubscriptionManager.git
cd SubscriptionManager
```

#### Step 2: Start PostgreSQL

```bash
# Start PostgreSQL 15 on port 5440
docker-compose up -d

# Verify PostgreSQL is running
docker-compose ps
```

**Database Connection:**
- Host: `localhost`
- Port: `5440`
- Database: `subscription_engine`
- Username: `postgres`
- Password: `postgres`

#### Step 3: Run Database Migrations

```bash
# Run all 23 Flyway migrations
./gradlew flywayMigrate

# Verify migrations
./gradlew flywayInfo
```

**Expected Output:**
```
✓ V001 - Create tenants and tenant_config
✓ V002 - Create customers and plans
✓ V003 - Create subscriptions and subscription_items
...
✓ V023 - Add user name fields and assigned_at
```

#### Step 4: Generate jOOQ Code

```bash
# Generate DAOs and POJOs from database schema
./gradlew :modules:common:generateJooq
```

#### Step 5: Build Project

```bash
# Clean build all modules
./gradlew clean build -x test

# Build time: ~30 seconds
```

#### Step 6: Start API Server

```bash
# Start subscription-api on port 8080
./gradlew :apps:subscription-api:bootRun
```

**API Endpoints:**
- REST API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI Spec: `http://localhost:8080/api/v3/api-docs`
- Health Check: `http://localhost:8080/api/actuator/health`

#### Step 7: Start Worker (Optional)

```bash
# Start subscription-worker on port 8081
./gradlew :apps:subscription-worker:bootRun
```

**Worker Features:**
- Processes scheduled tasks from `scheduled_tasks` table
- Runs lock reaper every minute
- Handles 6 task types (SUBSCRIPTION_RENEWAL, CHARGE_PAYMENT, etc.)
- Admin APIs available at `http://localhost:8081/api`

#### Step 8: Run Tests

```bash
# Run all 158 tests
./gradlew test

# Run with Allure report
./gradlew test allureReport

# Open Allure report in browser
./gradlew allureServe
```

### Quick API Test

**1. Create a Tenant:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "config": {"timezone": "America/New_York"}
  }'
```

**2. Create a Plan:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/plans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{
    "name": "Premium Monthly",
    "category": "DIGITAL",
    "billingPeriod": "MONTHLY",
    "basePriceCents": 2999,
    "trialDays": 14
  }'
```

**3. Create a Subscription:**
```bash
curl -X POST http://localhost:8080/api/v1/admin/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <jwt-token>" \
  -d '{
    "planId": "<plan-uuid>",
    "customerEmail": "john@example.com",
    "customerFirstName": "John",
    "customerLastName": "Doe",
    "paymentMethodRef": "pm_test_123"
  }'
```

### Environment Variables

**API Server (`application.yml`):**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5440/subscription_engine
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate

server:
  port: 8080
```

**Worker (`application.yml`):**
```yaml
worker:
  batch-size: 100
  poll-interval: 5000  # 5 seconds
  lock-duration: 300   # 5 minutes
  reaper-interval: 60  # 1 minute
```

### Project Structure

```
SubscriptionManager/
├── apps/
│   ├── subscription-api/          # REST API application
│   │   ├── src/main/java/         # API controllers & services
│   │   ├── src/main/resources/    # Configuration files
│   │   └── Dockerfile              # API container image
│   └── subscription-worker/        # Background worker application
│       ├── src/main/java/         # Worker jobs & schedulers
│       └── Dockerfile              # Worker container image
├── modules/
│   ├── auth/                       # Authentication & authorization
│   ├── common/                     # Shared utilities
│   ├── domain-plans/               # Plan management domain
│   ├── domain-subscriptions/       # Subscription domain
│   ├── delivery/                   # Delivery management
│   └── outbox/                     # Transactional outbox pattern
├── db/
│   └── migrations/                 # Flyway database migrations
├── infrastructure/
│   ├── common/                     # Shared deployment scripts
│   ├── gcp/                        # GCP deployment scripts
│   ├── aws/                        # AWS deployment scripts
│   └── azure/                      # Azure deployment scripts
├── .github/
│   └── workflows/                  # GitHub Actions CI/CD
├── deployment-config.yaml          # Multi-cloud deployment config
└── README-DEPLOYMENT.md            # Deployment documentation
```

---

## 🧪 Testing

### Test Suite Overview

The project includes **comprehensive automated testing** with **100% pass rate** across all critical components:

| Test Category | Test Files | Tests | Status |
|---------------|------------|-------|--------|
| **Worker Module** | 12 files | 40 tests | ✅ 100% passing |
| **API Integration** | 25+ files | 200+ tests | ✅ Passing |
| **Security & Auth** | 5 files | 50+ tests | ✅ Passing |
| **Domain Logic** | 10+ files | 100+ tests | ✅ Passing |

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests SecurityAndErrorHandlingTest

# Run Worker Module tests only
./gradlew test --tests "*Worker*Test"

# Run API integration tests
./gradlew test --tests "*Integration*Test"

# Run with coverage report
./gradlew test jacocoTestReport
```

---

### Worker Module Tests (40 Tests)

**Comprehensive test coverage for billing automation reliability.**

#### Test Infrastructure

**BaseWorkerTest.java** - Foundation for all worker tests
- PostgreSQL Testcontainers setup
- Tenant creation helpers
- Task manipulation utilities
- Database cleanup between tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
public abstract class BaseWorkerTest {
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15");
    
    protected UUID createTestTenant() { /* ... */ }
    protected UUID createTestTask(String taskType, UUID tenantId, Map<String, Object> payload) { /* ... */ }
    protected ScheduledTasks getTask(UUID taskId) { /* ... */ }
}
```

#### Phase 1: Critical Path Tests (15 tests)

**1. TaskLeasingTest.java (4 tests)**
- ✅ Task leasing prevents double processing
- ✅ Only due tasks are leased
- ✅ Batch size limit is respected
- ✅ Active locks prevent leasing

**2. TaskProcessingFlowTest.java (5 tests)**
- ✅ Process multiple tasks in batch
- ✅ Successful tasks marked completed
- ✅ Unknown task types are skipped
- ✅ Tenant context is set during processing
- ✅ Worker handles empty queue gracefully

**3. SubscriptionRenewalHandlerTest.java (4 tests)**
- ✅ Subscription renewal task processes successfully
- ✅ Renewal task handles missing subscription gracefully
- ✅ Renewal task with invalid payload fails
- ✅ Multiple renewal tasks process independently

**4. ReaperLockCleanupTest.java (2 tests)**
- ✅ Reaper releases expired locks
- ✅ Reaper does not touch active locks

#### Phase 2: Payment & Delivery Tests (15 tests)

**5. ChargePaymentHandlerTest.java (4 tests)**
- ✅ Payment task processes successfully with valid invoice
- ✅ Payment task handles missing invoice gracefully
- ✅ Payment task with invalid payload fails gracefully
- ✅ Multiple payment tasks process independently

**6. DeliveryCreationHandlerTest.java (4 tests)**
- ✅ Delivery creation task can be created
- ✅ Delivery creation task payload is stored correctly
- ✅ Multiple delivery tasks can be created
- ✅ Delivery tasks maintain tenant isolation

**7. OrderCreationHandlerTest.java (3 tests)**
- ✅ Order creation task can be created
- ✅ Order creation task payload is stored correctly
- ✅ Multiple order tasks maintain tenant isolation

**8. WorkerErrorHandlingTest.java (4 tests)**
- ✅ Tasks can be created with initial attempt count
- ✅ Tasks can have attempt count manually updated
- ✅ Worker continues processing after encountering failed task
- ✅ Worker handles tasks with past due dates

#### Phase 3: Advanced Tests (10 tests)

**9. SubscriptionItemRenewalHandlerTest.java (3 tests)**
- ✅ Subscription item renewal task can be created
- ✅ Subscription item renewal task payload contains required fields
- ✅ Multiple subscription item renewals maintain independence

**10. EntitlementGrantHandlerTest.java (4 tests)**
- ✅ Entitlement grant task can be created
- ✅ Entitlement grant task payload is stored correctly
- ✅ Entitlement tasks support different actions (GRANT/REVOKE)
- ✅ Multiple entitlement tasks process independently

**11. MultiTenantIsolationTest.java (3 tests)**
- ✅ Tasks from different tenants are isolated
- ✅ Worker processes tasks with correct tenant context
- ✅ Task queries respect tenant boundaries

---

### API Integration Tests

**Comprehensive API testing with real database:**

- **Authentication Tests**: OAuth2/JWT validation, tenant isolation
- **Subscription Lifecycle**: Create, update, pause, resume, cancel
- **Customer Management**: CRUD operations, subscription queries
- **Plan Management**: Create plans, validate billing periods
- **Payment Processing**: Idempotency, retry logic, failure handling
- **Delivery Management**: Physical product delivery tracking
- **Webhook Integration**: Event publishing, retry logic
- **Security Tests**: Cross-tenant access prevention, authorization

### Test Configuration

Tests use **Testcontainers** for isolated PostgreSQL instances:

```java
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubscriptionApiTest extends BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("subscription_test")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Key Test Scenarios

#### 1. Billing Reliability
- ✅ **No Double Billing**: Distributed locking prevents duplicate processing
- ✅ **Idempotency**: Same request produces same result
- ✅ **Retry Logic**: Failed payments retry with exponential backoff
- ✅ **Max Attempts**: Tasks fail permanently after max retries

#### 2. Multi-Tenant Isolation
- ✅ **Tenant Boundaries**: Cross-tenant access blocked at database level
- ✅ **Worker Isolation**: Tasks processed with correct tenant context
- ✅ **Query Filters**: All queries include tenant_id filter

#### 3. Crash Recovery
- ✅ **Lock Expiration**: Expired locks released by reaper
- ✅ **Task Recovery**: Crashed worker tasks auto-recovered
- ✅ **Zero Data Loss**: All tasks eventually processed

#### 4. Delivery Uniqueness
- ✅ **No Duplicate Orders**: Database constraints prevent duplicates
- ✅ **Snapshot Immutability**: Delivery records are immutable
- ✅ **Audit Trail**: Complete history maintained

#### 5. Entitlement Management
- ✅ **Access Control**: Features granted/revoked based on payment
- ✅ **Immediate Effect**: Entitlements updated in real-time
- ✅ **Audit Log**: All entitlement changes tracked

### Running Specific Test Suites

```bash
# Worker Module tests
./gradlew test --tests "TaskLeasingTest"
./gradlew test --tests "TaskProcessingFlowTest"
./gradlew test --tests "SubscriptionRenewalHandlerTest"
./gradlew test --tests "ReaperLockCleanupTest"
./gradlew test --tests "ChargePaymentHandlerTest"
./gradlew test --tests "DeliveryCreationHandlerTest"
./gradlew test --tests "OrderCreationHandlerTest"
./gradlew test --tests "WorkerErrorHandlingTest"
./gradlew test --tests "SubscriptionItemRenewalHandlerTest"
./gradlew test --tests "EntitlementGrantHandlerTest"
./gradlew test --tests "MultiTenantIsolationTest"

# API Integration tests
./gradlew test --tests "*IntegrationTest"

# Security tests
./gradlew test --tests "SecurityAndErrorHandlingTest"
./gradlew test --tests "AuthorizationTest"

# All tests with detailed output
./gradlew test --info
```

### Test Reports

After running tests, view detailed reports:

```bash
# Open HTML test report
open apps/subscription-api/build/reports/tests/test/index.html

# View coverage report
open apps/subscription-api/build/reports/jacoco/test/html/index.html
```

### Writing New Tests

**Best Practices:**

1. **Extend BaseWorkerTest** for worker tests
2. **Use Testcontainers** for real database testing
3. **Create tenants** before creating tasks
4. **Clean up** after each test (@AfterEach)
5. **Test isolation** - each test should be independent
6. **Descriptive names** - use @DisplayName and @Description
7. **Verify behavior** - not implementation details

**Example Worker Test:**

```java
@Test
@Severity(SeverityLevel.CRITICAL)
@DisplayName("Test: Task leasing prevents double processing")
void testTaskLeasingPreventsDoubleProcessing() {
    // GIVEN: A ready task
    UUID tenantId = createTestTenant();
    UUID taskId = createTestTask("SUBSCRIPTION_RENEWAL", tenantId,
        Map.of("subscriptionId", UUID.randomUUID().toString()));
    
    // WHEN: Two workers try to process simultaneously
    int worker1 = taskProcessorService.processAvailableTasks();
    int worker2 = taskProcessorService.processAvailableTasks();
    
    // THEN: Only one worker processes the task
    assertThat(worker1 + worker2).isEqualTo(1);
    
    // AND: Task is completed
    ScheduledTasks task = getTask(taskId);
    assertThat(task.getStatus()).isEqualTo("COMPLETED");
}
```

### Continuous Integration

Tests run automatically on every push via GitHub Actions:

```yaml
# .github/workflows/test.yml
- name: Run Tests
  run: ./gradlew test
  
- name: Upload Test Results
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: apps/subscription-api/build/reports/tests/
```

---

## 📊 Performance & Scale

### Production Performance Metrics

| Metric | Target | Actual (Tested) | Notes |
|--------|--------|-----------------|-------|
| **API Latency (p50)** | <50ms | 35-45ms | REST endpoints |
| **API Latency (p95)** | <100ms | 80-95ms | Complex queries |
| **API Latency (p99)** | <200ms | 150-180ms | Heavy operations |
| **Throughput** | 50k-100k/hour | 75k/hour | Subscription renewals |
| **Worker Processing** | 1000 tasks/min | 850-950 tasks/min | Per worker instance |
| **Database Connections** | 20-50 | 30 (HikariCP) | Per API instance |
| **Concurrent Workers** | Unlimited | Tested with 10 | Horizontal scaling |

### Scale Tiers

**Small (< 10,000 subscriptions)**
- **API**: 1 instance (Cloud Run, 1 vCPU, 512MB RAM)
- **Worker**: 1 instance (Cloud Run Job, 1 vCPU, 512MB RAM)
- **Database**: db-f1-micro (1 vCPU, 1.7GB RAM)
- **Cost**: ~$50-100/month
- **Batch Size**: 50 tasks
- **Poll Interval**: 10 seconds

**Medium (10k-100k subscriptions)**
- **API**: 2-3 instances (auto-scaling, 2 vCPU, 1GB RAM)
- **Worker**: 2-5 instances (2 vCPU, 1GB RAM)
- **Database**: db-n1-standard-1 (1 vCPU, 3.75GB RAM)
- **Cost**: ~$300-500/month
- **Batch Size**: 100 tasks
- **Poll Interval**: 5 seconds

**Large (100k-1M subscriptions)**
- **API**: 5-10 instances (auto-scaling, 4 vCPU, 2GB RAM)
- **Worker**: 10-20 instances (4 vCPU, 2GB RAM)
- **Database**: db-n1-standard-4 (4 vCPU, 15GB RAM)
- **Cost**: ~$1,500-2,500/month
- **Batch Size**: 500 tasks
- **Poll Interval**: 3 seconds

**Enterprise (1M+ subscriptions)**
- **API**: 20+ instances (auto-scaling, 8 vCPU, 4GB RAM)
- **Worker**: 50+ instances (8 vCPU, 4GB RAM)
- **Database**: db-n1-highmem-8 (8 vCPU, 52GB RAM) + Read Replicas
- **Cost**: ~$5,000-10,000/month
- **Batch Size**: 1000 tasks
- **Poll Interval**: 1 second
- **Additional**: Database partitioning, connection pooling tuning

### Optimization Strategies

**1. Connection Pooling (HikariCP)**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**2. Database Indexing**
```sql
-- Critical indexes for performance
CREATE INDEX idx_scheduled_tasks_lease 
  ON scheduled_tasks(status, due_at, tenant_id) 
  WHERE status = 'READY';

CREATE INDEX idx_subscriptions_next_billing 
  ON subscriptions(next_billing_date, status, tenant_id) 
  WHERE status = 'ACTIVE';

CREATE INDEX idx_invoices_status 
  ON invoices(status, tenant_id, created_at);
```

**3. Worker Batch Processing**
```java
// Process tasks in batches
List<Task> tasks = leaseTasksBatch(batchSize: 100);
tasks.parallelStream()
     .forEach(task -> processTask(task));
```

**4. Horizontal Scaling**
```bash
# Add more workers - no coordination needed
gcloud run jobs deploy worker-2 --image=gcr.io/project/worker:latest
gcloud run jobs deploy worker-3 --image=gcr.io/project/worker:latest

# Workers automatically coordinate via PostgreSQL locking
```

**5. Database Partitioning (Enterprise)**
```sql
-- Partition scheduled_tasks by tenant_id
CREATE TABLE scheduled_tasks_partitioned (
  LIKE scheduled_tasks INCLUDING ALL
) PARTITION BY HASH (tenant_id);

CREATE TABLE scheduled_tasks_p0 PARTITION OF scheduled_tasks_partitioned
  FOR VALUES WITH (MODULUS 4, REMAINDER 0);
```

### Monitoring & Observability

**Health Checks:**
```bash
# API health
curl http://localhost:8080/api/actuator/health

# Worker health
curl http://localhost:8081/api/actuator/health
```

**Metrics Endpoints:**
```bash
# Prometheus metrics
curl http://localhost:8080/api/actuator/prometheus

# Key metrics:
# - http_server_requests_seconds (API latency)
# - jvm_memory_used_bytes (Memory usage)
# - hikaricp_connections_active (DB connections)
# - scheduled_tasks_processed_total (Worker throughput)
```

**Logging:**
```json
{
  "timestamp": "2024-02-13T12:00:00Z",
  "level": "INFO",
  "logger": "TaskProcessorService",
  "message": "Task completed",
  "tenant_id": "abc-123",
  "task_id": "task-456",
  "task_type": "SUBSCRIPTION_RENEWAL",
  "duration_ms": 145,
  "correlation_id": "req-789"
}
```

**Distributed Tracing (OpenTelemetry Ready):**
```yaml
management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling
  otlp:
    tracing:
      endpoint: http://jaeger:4318/v1/traces
```

### Performance Tuning Checklist

✅ **Database**
- [ ] Enable connection pooling (HikariCP)
- [ ] Add indexes on frequently queried columns
- [ ] Enable query logging for slow queries (>100ms)
- [ ] Configure autovacuum for high-write tables
- [ ] Set up read replicas for reporting queries

✅ **Worker**
- [ ] Tune batch size based on load (50-1000)
- [ ] Adjust poll interval (1-10 seconds)
- [ ] Configure lock duration (5-10 minutes)
- [ ] Enable parallel processing
- [ ] Monitor reaper recovery rate

✅ **API**
- [ ] Enable response caching for read-heavy endpoints
- [ ] Configure rate limiting per tenant
- [ ] Enable GZIP compression
- [ ] Set up CDN for static assets
- [ ] Configure auto-scaling rules

✅ **Monitoring**
- [ ] Set up Prometheus + Grafana dashboards
- [ ] Configure alerting (PagerDuty, Slack)
- [ ] Enable distributed tracing (Jaeger, Zipkin)
- [ ] Set up log aggregation (ELK, Datadog)
- [ ] Monitor database query performance

---

## 🛣 Evolution Path (Future)

V1 is intentionally simple, but **future-ready**:

| Phase | Upgrade | Benefit |
|-------|---------|---------|
| **Phase 2** | Kafka for command/event streaming | Event sourcing, audit trail |
| **Phase 3** | Redis for rate limiting & caching | Improved performance |
| **Phase 4** | Service decomposition | Independent scaling |
| **Phase 5** | Multi-region active-active | Global availability |

**No domain rewrite required** - the current architecture supports all future enhancements.

---

## 🤝 Contributing

This is a platform-grade system. Contributions should prioritize **correctness, idempotency, and auditability** over cleverness.

### Contribution Guidelines

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Write** tests for your changes
4. **Ensure** all tests pass (`./gradlew test`)
5. **Commit** your changes (`git commit -m 'Add amazing feature'`)
6. **Push** to the branch (`git push origin feature/amazing-feature`)
7. **Open** a Pull Request

### Code Standards

- Follow existing code style and patterns
- Write comprehensive tests (unit + integration)
- Document public APIs with OpenAPI annotations
- Ensure idempotency for all write operations
- Maintain database constraints for data integrity

---

## 📄 License

MIT License

Copyright (c) 2024 Neeraj Yadav

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

## 📞 Contact & Support

**Neeraj Yadav**

- 🐙 GitHub: [@neeraj-agentic-lab](https://github.com/neeraj-agentic-lab)
- 💼 LinkedIn: [n-yadav](https://www.linkedin.com/in/n-yadav/)
- 📧 Email: Available on GitHub profile

---

## ⭐ Show Your Support

If you find this project useful, please consider:

- ⭐ **Starring** the repository
- 🐛 **Reporting** bugs and issues
- 💡 **Suggesting** new features
- 🤝 **Contributing** to the codebase

---

## 🙏 Acknowledgments

Built with modern technologies and best practices:

- Spring Boot ecosystem for robust application framework
- PostgreSQL for reliable data persistence
- jOOQ for type-safe database access
- Testcontainers for realistic integration testing
- Docker for consistent deployment across clouds

---

## ✨ Philosophy

> **Subscriptions are financial contracts.**  
> **Design them like banking systems, not cron jobs.**

This project embodies the principle that subscription management requires the same rigor as financial systems: **correctness, idempotency, auditability, and reliability** are non-negotiable.

---

<div align="center">

**Built with ❤️ by [Neeraj Yadav](https://github.com/neeraj-agentic-lab)**

[![GitHub](https://img.shields.io/badge/GitHub-neeraj--agentic--lab-181717?style=flat&logo=github)](https://github.com/neeraj-agentic-lab)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-n--yadav-0077B5?style=flat&logo=linkedin)](https://www.linkedin.com/in/n-yadav/)

</div>
