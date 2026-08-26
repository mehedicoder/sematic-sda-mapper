-- Phase 21: End-to-End Pipeline Observability
-- Audit trail of every pipeline stage transition (STARTED / SUCCEEDED / FAILED).

CREATE TABLE pipeline_audit_log (
    id             BIGSERIAL PRIMARY KEY,
    run_id         VARCHAR(64)  NOT NULL,
    session_id     VARCHAR(64),
    requirement_id VARCHAR(255),
    stage          VARCHAR(32)  NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    started_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_ms    BIGINT,
    detail         TEXT
);

CREATE INDEX idx_pipeline_audit_session ON pipeline_audit_log (session_id);
CREATE INDEX idx_pipeline_audit_requirement ON pipeline_audit_log (requirement_id);
CREATE INDEX idx_pipeline_audit_run ON pipeline_audit_log (run_id);
CREATE INDEX idx_pipeline_audit_started_at ON pipeline_audit_log (started_at);
