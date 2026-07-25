-- V2__import_session_schema.sql
-- Import session schema for multi-file semantic import

CREATE TABLE IF NOT EXISTS import_session (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    file_count INTEGER NOT NULL DEFAULT 0,
    total_triples INTEGER NOT NULL DEFAULT 0,
    validation_errors INTEGER NOT NULL DEFAULT 0,
    warnings INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    committed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS uploaded_file (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES import_session(id) ON DELETE CASCADE,
    filename VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    content_hash VARCHAR(128),
    size_bytes BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    triple_count INTEGER NOT NULL DEFAULT 0,
    validation_report TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS import_result (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES import_session(id) ON DELETE CASCADE,
    assets_found INTEGER NOT NULL DEFAULT 0,
    services_found INTEGER NOT NULL DEFAULT 0,
    capabilities_found INTEGER NOT NULL DEFAULT 0,
    skills_found INTEGER NOT NULL DEFAULT 0,
    communication_bindings_found INTEGER NOT NULL DEFAULT 0,
    data_connectors_found INTEGER NOT NULL DEFAULT 0,
    conflicts_detected INTEGER NOT NULL DEFAULT 0,
    warnings INTEGER NOT NULL DEFAULT 0,
    errors INTEGER NOT NULL DEFAULT 0,
    completeness_score DOUBLE PRECISION DEFAULT 0.0,
    summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_uploaded_file_session ON uploaded_file(session_id);
CREATE INDEX IF NOT EXISTS idx_import_result_session ON import_result(session_id);
