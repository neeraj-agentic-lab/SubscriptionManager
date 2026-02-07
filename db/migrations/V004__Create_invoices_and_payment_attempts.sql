-- V004: Create invoices, invoice_lines, and payment_attempts tables
-- Billing snapshots and payment processing

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    
    -- Invoice identification
    invoice_number VARCHAR(100) NOT NULL,
    
    -- Billing period (immutable snapshot)
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Amounts
    subtotal_cents BIGINT NOT NULL DEFAULT 0,
    tax_cents BIGINT NOT NULL DEFAULT 0,
    total_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    
    -- Status and dates
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    due_date TIMESTAMP WITH TIME ZONE,
    paid_at TIMESTAMP WITH TIME ZONE,
    
    -- External references
    external_invoice_ref VARCHAR(255),
    
    -- Custom attributes
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT invoices_status_check CHECK (status IN ('DRAFT', 'OPEN', 'PAID', 'VOID', 'UNCOLLECTIBLE')),
    CONSTRAINT invoices_period_check CHECK (period_end > period_start),
    CONSTRAINT invoices_amounts_check CHECK (subtotal_cents >= 0 AND tax_cents >= 0 AND total_cents >= 0),
    -- CRITICAL: Prevent duplicate invoices for same period
    CONSTRAINT invoices_unique_period UNIQUE (tenant_id, subscription_id, period_start, period_end)
);

CREATE TABLE invoice_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    subscription_item_id UUID REFERENCES subscription_items(id),
    
    -- Line item details
    description VARCHAR(500) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price_cents BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    
    -- Period this line covers
    period_start TIMESTAMP WITH TIME ZONE,
    period_end TIMESTAMP WITH TIME ZONE,
    
    -- Custom attributes
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT invoice_lines_quantity_check CHECK (quantity > 0),
    CONSTRAINT invoice_lines_amounts_check CHECK (unit_price_cents >= 0 AND total_cents >= 0)
);

CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    
    -- Payment details
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method_ref VARCHAR(255),
    
    -- Attempt tracking
    attempt_number INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- External payment system references
    external_payment_id VARCHAR(255),
    external_charge_id VARCHAR(255),
    
    -- Results
    failure_reason VARCHAR(500),
    failure_code VARCHAR(100),
    
    -- Timing
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at TIMESTAMP WITH TIME ZONE,
    
    -- Custom attributes
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT payment_attempts_status_check CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELED')),
    CONSTRAINT payment_attempts_amount_check CHECK (amount_cents > 0),
    CONSTRAINT payment_attempts_attempt_check CHECK (attempt_number > 0)
);

-- Indexes
CREATE INDEX idx_invoices_tenant_id ON invoices(tenant_id);
CREATE INDEX idx_invoices_subscription_id ON invoices(subscription_id);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(tenant_id, status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date) WHERE due_date IS NOT NULL;
CREATE INDEX idx_invoices_period ON invoices(period_start, period_end);
CREATE UNIQUE INDEX idx_invoices_tenant_number ON invoices(tenant_id, invoice_number);

CREATE INDEX idx_invoice_lines_tenant_id ON invoice_lines(tenant_id);
CREATE INDEX idx_invoice_lines_invoice_id ON invoice_lines(invoice_id);
CREATE INDEX idx_invoice_lines_subscription_item_id ON invoice_lines(subscription_item_id);

CREATE INDEX idx_payment_attempts_tenant_id ON payment_attempts(tenant_id);
CREATE INDEX idx_payment_attempts_invoice_id ON payment_attempts(invoice_id);
CREATE INDEX idx_payment_attempts_status ON payment_attempts(tenant_id, status);
CREATE INDEX idx_payment_attempts_external_payment ON payment_attempts(external_payment_id);

-- Updated at triggers
CREATE TRIGGER update_invoices_updated_at BEFORE UPDATE ON invoices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payment_attempts_updated_at BEFORE UPDATE ON payment_attempts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
