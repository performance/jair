CREATE TABLE IF NOT EXISTS recommendations (
    id UUID PRIMARY KEY,
    professional_id UUID NOT NULL,
    user_id UUID NOT NULL,
    consultation_id VARCHAR(255),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL, -- TREATMENT_ADJUSTMENT, NEW_INTERVENTION, LIFESTYLE_CHANGE
    details TEXT,              -- JSON stored as text
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, SUPERSEDED, DELETED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recommendations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    -- Constraint for professional_id can be added once professional entity/table is finalized
    -- For now, we assume professional_id refers to an ID in the existing users table,
    -- and the user has a 'PROFESSIONAL' role.
    -- CONSTRAINT fk_recommendations_professional FOREIGN KEY (professional_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_recommendations_professional_id ON recommendations(professional_id);
CREATE INDEX idx_recommendations_user_id ON recommendations(user_id);
CREATE INDEX idx_recommendations_status ON recommendations(status);
CREATE INDEX idx_recommendations_type ON recommendations(type);
