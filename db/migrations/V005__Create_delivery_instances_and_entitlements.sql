-- V005: Create delivery_instances and entitlements tables
-- Physical/digital fulfillment and access rights

CREATE TABLE delivery_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    invoice_id UUID REFERENCES invoices(id),
    
    -- Cycle identification (prevents duplicates)
    cycle_key VARCHAR(100) NOT NULL,
    
    -- Delivery details
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    delivery_type VARCHAR(20) NOT NULL DEFAULT 'PHYSICAL',
    
    -- Snapshot of subscription at delivery time (immutable)
    snapshot JSONB NOT NULL,
    
    -- External order system references
    external_order_ref VARCHAR(255),
    external_tracking_ref VARCHAR(255),
    
    -- Timing
    scheduled_for TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    
    -- Custom attributes
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT delivery_instances_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'FAILED', 'CANCELED')),
    CONSTRAINT delivery_instances_type_check CHECK (delivery_type IN ('PHYSICAL', 'DIGITAL', 'HYBRID')),
    -- CRITICAL: Prevent duplicate deliveries for same cycle
    CONSTRAINT delivery_instances_unique_cycle UNIQUE (tenant_id, subscription_id, cycle_key)
);

CREATE TABLE entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    
    -- Entitlement identification
    entitlement_type VARCHAR(100) NOT NULL,
    entitlement_key VARCHAR(255) NOT NULL,
    
    -- Status and lifecycle
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Validity period
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE,
    
    -- Entitlement payload (flexible structure)
    entitlement_payload JSONB NOT NULL DEFAULT '{}',
    
    -- External system references
    external_entitlement_ref VARCHAR(255),
    
    -- Custom attributes
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT entitlements_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT entitlements_validity_check CHECK (valid_until IS NULL OR valid_until > valid_from),
    -- Prevent duplicate entitlements
    CONSTRAINT entitlements_unique_key UNIQUE (tenant_id, customer_id, entitlement_type, entitlement_key, valid_from)
);

-- Indexes
CREATE INDEX idx_delivery_instances_tenant_id ON delivery_instances(tenant_id);
CREATE INDEX idx_delivery_instances_subscription_id ON delivery_instances(subscription_id);
CREATE INDEX idx_delivery_instances_invoice_id ON delivery_instances(invoice_id);
CREATE INDEX idx_delivery_instances_status ON delivery_instances(tenant_id, status);
CREATE INDEX idx_delivery_instances_scheduled ON delivery_instances(scheduled_for) WHERE scheduled_for IS NOT NULL;
CREATE INDEX idx_delivery_instances_external_order ON delivery_instances(external_order_ref);

CREATE INDEX idx_entitlements_tenant_id ON entitlements(tenant_id);
CREATE INDEX idx_entitlements_subscription_id ON entitlements(subscription_id);
CREATE INDEX idx_entitlements_customer_id ON entitlements(customer_id);
CREATE INDEX idx_entitlements_status ON entitlements(tenant_id, status);
CREATE INDEX idx_entitlements_type ON entitlements(tenant_id, entitlement_type);
CREATE INDEX idx_entitlements_validity ON entitlements(valid_from, valid_until);
CREATE INDEX idx_entitlements_external_ref ON entitlements(external_entitlement_ref);

-- Updated at triggers
CREATE TRIGGER update_delivery_instances_updated_at BEFORE UPDATE ON delivery_instances
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_entitlements_updated_at BEFORE UPDATE ON entitlements
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
