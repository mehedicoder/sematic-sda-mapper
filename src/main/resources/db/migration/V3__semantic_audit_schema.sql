-- V3__semantic_audit_schema.sql
-- Semantic audit trail for Phase 16 Interactive Import Completion

CREATE TABLE IF NOT EXISTS semantic_audit (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES import_session(id) ON DELETE CASCADE,
    entity_uri VARCHAR(512) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    source VARCHAR(20) NOT NULL DEFAULT 'UPLOAD',
    value TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_semantic_audit_session ON semantic_audit(session_id);
CREATE INDEX IF NOT EXISTS idx_semantic_audit_source ON semantic_audit(session_id, source);
