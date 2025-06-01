-- Medical sharing sessions
CREATE TABLE IF NOT EXISTS medical_sharing_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    professional_id UUID NOT NULL, -- References professionals table (future)
    photo_ids UUID[] NOT NULL, -- Array of photo IDs
    notes TEXT,
    max_total_views INTEGER NOT NULL DEFAULT 3,
    max_view_duration_minutes INTEGER NOT NULL DEFAULT 5,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    allow_screenshots BOOLEAN DEFAULT FALSE,
    allow_download BOOLEAN DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_DOCTOR_ACCESS',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_reason TEXT
);

-- Doctor access sessions
CREATE TABLE IF NOT EXISTS doctor_access_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    medical_session_id UUID NOT NULL REFERENCES medical_sharing_sessions(id) ON DELETE CASCADE,
    professional_id UUID NOT NULL,
    device_fingerprint VARCHAR(500) NOT NULL,
    ip_address INET NOT NULL,
    user_agent TEXT NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE
);

-- Individual photo viewing events
CREATE TABLE IF NOT EXISTS viewing_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    access_session_id UUID NOT NULL REFERENCES doctor_access_sessions(id) ON DELETE CASCADE,
    photo_id UUID NOT NULL REFERENCES photo_metadata(id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    duration_seconds INTEGER,
    device_fingerprint VARCHAR(500) NOT NULL,
    ip_address INET NOT NULL,
    screenshot_attempts INTEGER DEFAULT 0,
    download_attempts INTEGER DEFAULT 0,
    suspicious_activity BOOLEAN DEFAULT FALSE
);

-- Medical notifications
CREATE TABLE IF NOT EXISTS medical_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_session_id UUID REFERENCES medical_sharing_sessions(id) ON DELETE SET NULL,
    related_professional_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE
);

-- Ephemeral decryption keys
CREATE TABLE IF NOT EXISTS ephemeral_decryption_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES medical_sharing_sessions(id) ON DELETE CASCADE,
    photo_id UUID NOT NULL REFERENCES photo_metadata(id) ON DELETE CASCADE,
    professional_id UUID NOT NULL,
    encrypted_key TEXT NOT NULL,
    key_derivation_params TEXT NOT NULL,
    max_uses INTEGER NOT NULL DEFAULT 1,
    current_uses INTEGER DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP WITH TIME ZONE,
    is_revoked BOOLEAN DEFAULT FALSE
);

-- Indexes for performance
CREATE INDEX idx_medical_sharing_patient ON medical_sharing_sessions(patient_id, status);
CREATE INDEX idx_medical_sharing_professional ON medical_sharing_sessions(professional_id, status);
CREATE INDEX idx_medical_sharing_expires ON medical_sharing_sessions(expires_at, status);

CREATE INDEX idx_doctor_access_medical_session ON doctor_access_sessions(medical_session_id, is_active);
CREATE INDEX idx_doctor_access_professional ON doctor_access_sessions(professional_id, started_at);

CREATE INDEX idx_viewing_events_access_session ON viewing_events(access_session_id, started_at);
CREATE INDEX idx_viewing_events_photo ON viewing_events(photo_id, started_at);

CREATE INDEX idx_medical_notifications_recipient ON medical_notifications(recipient_id, is_read, created_at);
CREATE INDEX idx_medical_notifications_session ON medical_notifications(related_session_id);

CREATE INDEX idx_ephemeral_keys_session ON ephemeral_decryption_keys(session_id, photo_id);
CREATE INDEX idx_ephemeral_keys_expires ON ephemeral_decryption_keys(expires_at, is_revoked);