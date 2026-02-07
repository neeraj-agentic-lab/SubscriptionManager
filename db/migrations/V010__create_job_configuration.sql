-- Job configuration table for managing scheduled job settings
-- Allows admins to configure job schedules dynamically

CREATE TABLE job_configuration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_name VARCHAR(100) NOT NULL UNIQUE,
    job_description TEXT,
    
    -- Scheduling configuration
    cron_expression VARCHAR(100) NOT NULL,
    schedule_type VARCHAR(50) NOT NULL, -- 'CRON', 'FIXED_RATE', 'FIXED_DELAY'
    time_zone VARCHAR(50) DEFAULT 'UTC',
    
    -- Job control
    enabled BOOLEAN DEFAULT true,
    max_concurrent_executions INTEGER DEFAULT 1,
    timeout_minutes INTEGER DEFAULT 60,
    
    -- Predefined schedule options for easy selection
    schedule_preset VARCHAR(50), -- 'DAILY_6AM', 'HOURLY', 'EVERY_MINUTE', 'EVERY_5_MINUTES', 'CUSTOM'
    
    -- Metadata
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    -- Configuration details in JSON format
    job_config JSONB DEFAULT '{}'::jsonb
);

-- Insert default configuration for subscription renewal job
INSERT INTO job_configuration (
    job_name, 
    job_description, 
    cron_expression, 
    schedule_type, 
    schedule_preset,
    enabled,
    created_by,
    job_config
) VALUES (
    'subscription_renewal',
    'Daily subscription renewal processing - finds subscriptions due for renewal and creates tasks',
    '0 0 6 * * *',
    'CRON',
    'DAILY_6AM',
    true,
    'system',
    '{"description": "Processes subscription renewals daily at 6 AM", "maxRetries": 3}'::jsonb
);

-- Indexes for efficient querying
CREATE INDEX idx_job_configuration_job_name ON job_configuration(job_name);
CREATE INDEX idx_job_configuration_enabled ON job_configuration(enabled);
CREATE INDEX idx_job_configuration_schedule_preset ON job_configuration(schedule_preset);

-- Comments for documentation
COMMENT ON TABLE job_configuration IS 'Stores configuration settings for scheduled jobs';
COMMENT ON COLUMN job_configuration.cron_expression IS 'Cron expression defining when the job should run';
COMMENT ON COLUMN job_configuration.schedule_preset IS 'Predefined schedule options for easy admin selection';
COMMENT ON COLUMN job_configuration.job_config IS 'Additional job-specific configuration in JSON format';
