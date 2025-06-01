CREATE TABLE IF NOT EXISTS interventions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL, -- TOPICAL, ORAL, OTHER_TREATMENT
    product_name VARCHAR(255) NOT NULL,
    dosage_amount VARCHAR(100),
    frequency VARCHAR(100) NOT NULL,
    application_time VARCHAR(100),
    start_date DATE NOT NULL,
    end_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    provider VARCHAR(255),
    notes TEXT,
    source_recommendation_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_interventions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_interventions_recommendation FOREIGN KEY (source_recommendation_id) REFERENCES recommendations(id) ON DELETE SET NULL
);

CREATE INDEX idx_interventions_user_id ON interventions(user_id);
CREATE INDEX idx_interventions_is_active ON interventions(is_active);
CREATE INDEX idx_interventions_type ON interventions(type);
