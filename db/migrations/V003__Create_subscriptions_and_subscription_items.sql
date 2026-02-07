-- V003: Create subscriptions and subscription_items tables
-- Core subscription contracts and their line items

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES plans(id),
    
    -- Subscription lifecycle
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Billing schedule
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    next_renewal_at TIMESTAMP WITH TIME ZONE,
    
    -- Schedule configuration (JSONB for flexibility)
    schedule_config JSONB NOT NULL DEFAULT '{}',
    
    -- Payment method reference (external system)
    payment_method_ref VARCHAR(255),
    
    -- Shipping information
    shipping_address JSONB,
    shipping_preferences JSONB DEFAULT '{}',
    
    -- Plan snapshot at subscription creation (immutable reference)
    plan_snapshot JSONB NOT NULL,
    
    -- Cancellation
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(500),
    
    -- Trial
    trial_start TIMESTAMP WITH TIME ZONE,
    trial_end TIMESTAMP WITH TIME ZONE,
    
    -- Custom attributes
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT subscriptions_status_check CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELED', 'EXPIRED', 'PAST_DUE')),
    CONSTRAINT subscriptions_period_check CHECK (current_period_end > current_period_start),
    CONSTRAINT subscriptions_trial_check CHECK (trial_end IS NULL OR trial_end > trial_start)
);

CREATE TABLE subscription_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    
    -- Item details
    plan_id UUID NOT NULL REFERENCES plans(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    
    -- Pricing override (if different from plan)
    unit_price_cents BIGINT,
    currency VARCHAR(3),
    
    -- Item configuration
    item_config JSONB NOT NULL DEFAULT '{}',
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT subscription_items_quantity_check CHECK (quantity > 0),
    CONSTRAINT subscription_items_price_check CHECK (unit_price_cents IS NULL OR unit_price_cents >= 0)
);

-- Indexes
CREATE INDEX idx_subscriptions_tenant_id ON subscriptions(tenant_id);
CREATE INDEX idx_subscriptions_customer_id ON subscriptions(customer_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(tenant_id, status);
CREATE INDEX idx_subscriptions_next_renewal ON subscriptions(next_renewal_at) WHERE next_renewal_at IS NOT NULL;
CREATE INDEX idx_subscriptions_current_period ON subscriptions(current_period_start, current_period_end);

CREATE INDEX idx_subscription_items_tenant_id ON subscription_items(tenant_id);
CREATE INDEX idx_subscription_items_subscription_id ON subscription_items(subscription_id);
CREATE INDEX idx_subscription_items_plan_id ON subscription_items(plan_id);

-- Updated at triggers
CREATE TRIGGER update_subscriptions_updated_at BEFORE UPDATE ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_subscription_items_updated_at BEFORE UPDATE ON subscription_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
