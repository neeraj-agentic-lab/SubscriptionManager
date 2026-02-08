-- Add TRIALING status to subscriptions_status_check constraint
-- This allows subscriptions to have a TRIALING status during trial periods

ALTER TABLE subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check;

ALTER TABLE subscriptions 
ADD CONSTRAINT subscriptions_status_check 
CHECK (status IN ('ACTIVE', 'TRIALING', 'PAUSED', 'CANCELED', 'EXPIRED', 'PAST_DUE'));
