-- V006: Create scheduled_tasks and outbox_events tables
-- DB-driven scheduler and transactional outbox pattern

CREATE TABLE scheduled_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    
    -- Task identification
    task_type VARCHAR(50) NOT NULL,
    task_key VARCHAR(255), -- Optional unique key for idempotent task creation
    
    -- Scheduling
    status VARCHAR(20) NOT NULL DEFAULT 'READY',
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Execution tracking
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    
    -- Locking mechanism for distributed workers
    locked_until TIMESTAMP WITH TIME ZONE,
    lock_owner VARCHAR(100),
    
    -- Task payload
    payload JSONB NOT NULL DEFAULT '{}',
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    -- Results
    last_error TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    CONSTRAINT scheduled_tasks_status_check CHECK (status IN ('READY', 'CLAIMED', 'COMPLETED', 'FAILED')),
    CONSTRAINT scheduled_tasks_attempts_check CHECK (attempt_count >= 0 AND max_attempts > 0),
    -- Optional uniqueness constraint for idempotent task creation
    CONSTRAINT scheduled_tasks_unique_key UNIQUE (tenant_id, task_key) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    
    -- Event identification
    event_type VARCHAR(100) NOT NULL,
    event_key VARCHAR(255), -- Optional idempotency key
    
    -- Event payload
    event_payload JSONB NOT NULL,
    
    -- Publishing status
    published_at TIMESTAMP WITH TIME ZONE,
    
    -- Custom attributes
    custom_attrs JSONB NOT NULL DEFAULT '{}',
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    
    -- Optional uniqueness for idempotent event creation
    CONSTRAINT outbox_events_unique_key UNIQUE (tenant_id, event_key) DEFERRABLE INITIALLY DEFERRED
);

-- Indexes for scheduled_tasks (critical for performance)
CREATE INDEX idx_scheduled_tasks_tenant_id ON scheduled_tasks(tenant_id);
CREATE INDEX idx_scheduled_tasks_status ON scheduled_tasks(status);
-- Critical index for task claiming
CREATE INDEX idx_scheduled_tasks_ready ON scheduled_tasks(due_at, status) 
    WHERE status = 'READY';
-- Index for reaper (stuck task recovery)
CREATE INDEX idx_scheduled_tasks_locked ON scheduled_tasks(locked_until, status) 
    WHERE status = 'CLAIMED' AND locked_until IS NOT NULL;
CREATE INDEX idx_scheduled_tasks_type ON scheduled_tasks(tenant_id, task_type);
CREATE INDEX idx_scheduled_tasks_key ON scheduled_tasks(tenant_id, task_key) 
    WHERE task_key IS NOT NULL;

-- Indexes for outbox_events
CREATE INDEX idx_outbox_events_tenant_id ON outbox_events(tenant_id);
CREATE INDEX idx_outbox_events_type ON outbox_events(tenant_id, event_type);
CREATE INDEX idx_outbox_events_unpublished ON outbox_events(created_at) 
    WHERE published_at IS NULL;
CREATE INDEX idx_outbox_events_key ON outbox_events(tenant_id, event_key) 
    WHERE event_key IS NOT NULL;

-- Updated at trigger for scheduled_tasks
CREATE TRIGGER update_scheduled_tasks_updated_at BEFORE UPDATE ON scheduled_tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
