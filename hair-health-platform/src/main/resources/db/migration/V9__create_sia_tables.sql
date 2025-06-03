-- Implementation Plans Table
CREATE TABLE implementation_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_recommendation_ids TEXT, -- Comma-separated list of recommendation IDs or JSON array
    status VARCHAR(50) NOT NULL, -- e.g., DRAFT, ACTIVE, COMPLETED, CANCELLED, ADJUSTMENT_REQUESTED
    user_context_snapshot TEXT,  -- JSON blob of user data at time of plan creation
    risk_assessment_results TEXT, -- JSON blob
    success_probability DOUBLE PRECISION, -- Nullable
    current_phase_number INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_sia_plan_status CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'ADJUSTMENT_REQUESTED'))
);
CREATE INDEX idx_implementation_plans_user_id ON implementation_plans(user_id);
CREATE INDEX idx_implementation_plans_status ON implementation_plans(status);

-- Implementation Phases Table
CREATE TABLE implementation_phases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES implementation_plans(id) ON DELETE CASCADE,
    phase_number INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL, -- e.g., "Foundation Phase", "Intervention Adjustment"
    description TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) NOT NULL, -- e.g., PENDING, ACTIVE, COMPLETED, SKIPPED
    goals TEXT, -- JSON array of goal strings
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (plan_id, phase_number),
    CONSTRAINT check_sia_phase_status CHECK (status IN ('PENDING', 'ACTIVE', 'COMPLETED', 'SKIPPED'))
);
CREATE INDEX idx_implementation_phases_plan_id ON implementation_phases(plan_id);
CREATE INDEX idx_implementation_phases_status ON implementation_phases(status);

-- Phase Actions Table
CREATE TABLE phase_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phase_id UUID NOT NULL REFERENCES implementation_phases(id) ON DELETE CASCADE,
    action_description TEXT NOT NULL,
    action_type VARCHAR(100) NOT NULL, -- e.g., START_INTERVENTION, ADJUST_INTERVENTION, MONITOR_SYMPTOM, LIFESTYLE_CHANGE, CHECK_IN
    action_details TEXT, -- JSON blob for type-specific details (e.g., interventionId, dosage for START_INTERVENTION)
    is_key_action BOOLEAN DEFAULT FALSE, -- Corrected column name
    status VARCHAR(50) NOT NULL, -- e.g., PENDING, COMPLETED, MISSED, NOT_APPLICABLE
    due_date DATE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_sia_action_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'MISSED', 'NOT_APPLICABLE'))
);
CREATE INDEX idx_phase_actions_phase_id ON phase_actions(phase_id);
CREATE INDEX idx_phase_actions_status ON phase_actions(status);
CREATE INDEX idx_phase_actions_action_type ON phase_actions(action_type);
