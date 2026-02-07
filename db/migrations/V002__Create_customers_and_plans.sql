-- V002: Create customers and plans tables
-- Core entities for subscription management

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    external_customer_id VARCHAR(255),
    email VARCHAR(320) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT customers_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    CONSTRAINT customers_tenant_email_unique UNIQUE (tenant_id, email),
    CONSTRAINT customers_tenant_external_unique UNIQUE (tenant_id, external_customer_id)
);

CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    plan_type VARCHAR(20) NOT NULL DEFAULT 'RECURRING',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Pricing
    base_price_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    
    -- Billing configuration
    billing_interval VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    billing_interval_count INTEGER NOT NULL DEFAULT 1,
    
    -- Trial configuration
    trial_period_days INTEGER DEFAULT 0,
    
    -- Plan configuration
    plan_config JSONB NOT NULL DEFAULT '{}',
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT plans_type_check CHECK (plan_type IN ('RECURRING', 'ONE_TIME')),
    CONSTRAINT plans_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT plans_interval_check CHECK (billing_interval IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT plans_interval_count_check CHECK (billing_interval_count > 0),
    CONSTRAINT plans_base_price_check CHECK (base_price_cents >= 0),
    CONSTRAINT plans_trial_period_check CHECK (trial_period_days >= 0)
);

-- Indexes
CREATE INDEX idx_customers_tenant_id ON customers(tenant_id);
CREATE INDEX idx_customers_email ON customers(tenant_id, email);
CREATE INDEX idx_customers_external_id ON customers(tenant_id, external_customer_id);
CREATE INDEX idx_customers_status ON customers(tenant_id, status);

CREATE INDEX idx_plans_tenant_id ON plans(tenant_id);
CREATE INDEX idx_plans_status ON plans(tenant_id, status);
CREATE INDEX idx_plans_type ON plans(tenant_id, plan_type);

-- Updated at triggers
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_plans_updated_at BEFORE UPDATE ON plans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
