CREATE TABLE IF NOT EXISTS hair_fall_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    date DATE NOT NULL,
    count INTEGER,
    category VARCHAR(50) NOT NULL, -- SHOWER, PILLOW, COMBING, BRUSHING, OTHER
    description TEXT,
    photo_metadata_id UUID, -- Nullable, FK to be added later if photo_metadata table is created
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hair_fall_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    -- CONSTRAINT fk_hair_fall_logs_photo FOREIGN KEY (photo_metadata_id) REFERENCES photo_metadata(id) ON DELETE SET NULL -- Add when photo_metadata table exists
);

CREATE INDEX idx_hair_fall_logs_user_id ON hair_fall_logs(user_id);
CREATE INDEX idx_hair_fall_logs_date ON hair_fall_logs(date);
CREATE INDEX idx_hair_fall_logs_category ON hair_fall_logs(category);

-- Optional: Add CHECK constraint for category if desired and supported by DB
-- ALTER TABLE hair_fall_logs ADD CONSTRAINT check_hair_fall_category
-- CHECK (category IN ('SHOWER', 'PILLOW', 'COMBING', 'BRUSHING', 'OTHER'));
