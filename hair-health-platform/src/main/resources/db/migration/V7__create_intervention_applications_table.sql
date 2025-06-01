CREATE TABLE IF NOT EXISTS intervention_applications (
    id UUID PRIMARY KEY,
    intervention_id UUID NOT NULL,
    user_id UUID NOT NULL, -- Denormalized for query convenience
    "timestamp" TIMESTAMP WITH TIME ZONE NOT NULL, -- Quoted "timestamp" as it's a reserved keyword in some SQL dialects
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_intervention_applications_intervention FOREIGN KEY (intervention_id) REFERENCES interventions(id) ON DELETE CASCADE,
    CONSTRAINT fk_intervention_applications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE -- Assuming user_id in intervention_applications should also link to users table
);

CREATE INDEX idx_intervention_applications_intervention_id ON intervention_applications(intervention_id);
CREATE INDEX idx_intervention_applications_user_id_timestamp ON intervention_applications(user_id, "timestamp" DESC);
