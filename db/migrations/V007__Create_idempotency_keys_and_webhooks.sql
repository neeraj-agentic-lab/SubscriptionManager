-- V007: Create idempotency_keys and webhook tables
-- API idempotency and webhook delivery system

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    
    -- Idempotency key from client
    idempotency_key VARCHAR(255) NOT NULL,
    
    -- Request fingerprint
    request_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(500) NOT NULL,
    request_hash VARCHAR(64), -- SHA-256 of request body
    
    -- Response data
    response_status_code INTEGER,
    response_body TEXT,
    response_headers JSONB,
    
    -- Timing
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now() + INTERVAL '24 hours'),
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    -- CRITICAL: Ensure idempotency per tenant
    CONSTRAINT idempotency_keys_unique UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE webhook_endpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    
    -- Endpoint configuration
    url VARCHAR(2048) NOT NULL,
    secret VARCHAR(255) NOT NULL,
    
    -- Event filtering
    events TEXT[] NOT NULL DEFAULT '{}', -- Array of event types to subscribe to
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    
    -- Metadata
    description VARCHAR(500),
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT webhook_endpoints_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISABLED'))
);

CREATE TABLE webhook_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    webhook_endpoint_id UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE CASCADE,
    outbox_event_id UUID REFERENCES outbox_events(id) ON DELETE CASCADE,
    
    -- Delivery details
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    
    -- Delivery tracking
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    
    -- Scheduling for retries
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    -- Response tracking
    last_response_status INTEGER,
    last_response_body TEXT,
    last_error TEXT,
    
    -- Timing
    delivered_at TIMESTAMP WITH TIME ZONE,
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT webhook_deliveries_status_check CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED', 'ABANDONED')),
    CONSTRAINT webhook_deliveries_attempts_check CHECK (attempt_count >= 0 AND max_attempts > 0)
);

-- Indexes for idempotency_keys
CREATE INDEX idx_idempotency_keys_tenant_id ON idempotency_keys(tenant_id);
CREATE INDEX idx_idempotency_keys_expires ON idempotency_keys(expires_at);

-- Indexes for webhook_endpoints
CREATE INDEX idx_webhook_endpoints_tenant_id ON webhook_endpoints(tenant_id);
CREATE INDEX idx_webhook_endpoints_status ON webhook_endpoints(tenant_id, status);

-- Indexes for webhook_deliveries
CREATE INDEX idx_webhook_deliveries_tenant_id ON webhook_deliveries(tenant_id);
CREATE INDEX idx_webhook_deliveries_endpoint_id ON webhook_deliveries(webhook_endpoint_id);
CREATE INDEX idx_webhook_deliveries_outbox_event_id ON webhook_deliveries(outbox_event_id);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(tenant_id, status);
-- Critical index for retry processing
CREATE INDEX idx_webhook_deliveries_retry ON webhook_deliveries(next_attempt_at, status) 
    WHERE status = 'PENDING';

-- Updated at triggers
CREATE TRIGGER update_webhook_endpoints_updated_at BEFORE UPDATE ON webhook_endpoints
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_webhook_deliveries_updated_at BEFORE UPDATE ON webhook_deliveries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Cleanup job for expired idempotency keys (optional)
COMMENT ON TABLE idempotency_keys IS 'Idempotency keys expire after 24 hours and should be cleaned up periodically';
