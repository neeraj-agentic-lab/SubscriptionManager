-- Job execution history table for tracking scheduled job runs
-- This is essential for production monitoring and debugging

CREATE TABLE job_execution_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name VARCHAR(100) NOT NULL,
    job_type VARCHAR(50) NOT NULL, -- 'SCHEDULED' or 'MANUAL'
    status VARCHAR(20) NOT NULL, -- 'RUNNING', 'COMPLETED', 'FAILED'
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    execution_time_ms BIGINT,
    
    -- Metrics for renewal job
    subscriptions_found INTEGER DEFAULT 0,
    subscriptions_processed INTEGER DEFAULT 0,
    tasks_created INTEGER DEFAULT 0,
    errors_count INTEGER DEFAULT 0,
    
    -- Additional context
    trigger_source VARCHAR(100), -- 'CRON', 'API', 'ADMIN'
    triggered_by VARCHAR(100), -- user ID or system
    error_message TEXT,
    execution_details JSONB,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for efficient querying
CREATE INDEX idx_job_execution_history_job_name ON job_execution_history(job_name);
CREATE INDEX idx_job_execution_history_status ON job_execution_history(status);
CREATE INDEX idx_job_execution_history_started_at ON job_execution_history(started_at DESC);
CREATE INDEX idx_job_execution_history_job_type ON job_execution_history(job_type);

-- Comments for documentation
COMMENT ON TABLE job_execution_history IS 'Tracks execution history of scheduled jobs for monitoring and debugging';
COMMENT ON COLUMN job_execution_history.job_name IS 'Name of the job (e.g., subscription_renewal)';
COMMENT ON COLUMN job_execution_history.job_type IS 'Whether job was triggered by schedule or manually';
COMMENT ON COLUMN job_execution_history.execution_details IS 'Additional JSON details about the execution';
