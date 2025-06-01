ALTER TABLE recommendations
ADD COLUMN user_action VARCHAR(50), -- PENDING_ACTION, ACCEPTED, ACCEPTED_WITH_MODIFICATIONS, DECLINED
ADD COLUMN user_action_notes TEXT,
ADD COLUMN user_action_at TIMESTAMP WITH TIME ZONE;

-- Optional: Add an index if user_action will be frequently queried
CREATE INDEX idx_recommendations_user_action ON recommendations(user_action);

-- Note: A CHECK constraint for user_action values can be added if desired and supported, e.g.:
-- ALTER TABLE recommendations ADD CONSTRAINT check_user_action_values
-- CHECK (user_action IS NULL OR user_action IN ('PENDING_ACTION', 'ACCEPTED', 'ACCEPTED_WITH_MODIFICATIONS', 'DECLINED'));
-- For simplicity and broader database compatibility, it's omitted here but good for data integrity.
