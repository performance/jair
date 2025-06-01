CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    "timestamp" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Quoted "timestamp"
    actor_id VARCHAR(255), -- Can be UUID or system identifier
    actor_type VARCHAR(50), -- USER, PROFESSIONAL, SYSTEM
    action VARCHAR(255) NOT NULL,
    target_entity_type VARCHAR(100),
    target_entity_id VARCHAR(255), -- Can be UUID or other type of ID
    ip_address VARCHAR(100),
    device_info TEXT,
    status VARCHAR(50) NOT NULL, -- SUCCESS, FAILURE
    details TEXT, -- JSON for additional data
    CONSTRAINT check_audit_actor_type CHECK (actor_type IN ('USER', 'PROFESSIONAL', 'SYSTEM', NULL)), -- Allow NULL if actor is unknown/not applicable
    CONSTRAINT check_audit_status CHECK (status IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX idx_audit_logs_timestamp ON audit_logs("timestamp" DESC);
CREATE INDEX idx_audit_logs_actor_id_actor_type ON audit_logs(actor_id, actor_type);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_target_entity ON audit_logs(target_entity_type, target_entity_id);
CREATE INDEX idx_audit_logs_status ON audit_logs(status);
