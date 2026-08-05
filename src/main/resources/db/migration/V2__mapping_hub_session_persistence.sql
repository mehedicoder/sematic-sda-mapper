-- V2__mapping_hub_session_persistence.sql
-- Persistent Semantic Mapping Hub session, pipeline, canonical model, match, and manifest state.

CREATE TABLE mapping_hub_session (
    id UUID PRIMARY KEY,
    user_id UUID NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    current_coverage_percent NUMERIC(5,2),
    current_blocking_gap_count INTEGER NOT NULL DEFAULT 0,
    selected_manifest_id UUID,
    last_pipeline_run_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mapping_hub_session_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED', 'FAILED'))
);

CREATE TABLE mapping_hub_uploaded_file (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    original_file_name VARCHAR(500) NOT NULL,
    detected_format VARCHAR(60) NOT NULL,
    content_type VARCHAR(120),
    file_size_bytes BIGINT NOT NULL,
    sha256_hash CHAR(64) NOT NULL,
    content BYTEA NOT NULL,
    parse_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    parse_error TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mapping_hub_uploaded_file_format
        CHECK (detected_format IN ('AAS_XML', 'AAS_JSON', 'MTP_AML', 'OPCUA_JSON')),
    CONSTRAINT chk_mapping_hub_uploaded_file_parse_status
        CHECK (parse_status IN ('PENDING', 'PARSED', 'FAILED')),
    CONSTRAINT chk_mapping_hub_uploaded_file_size
        CHECK (file_size_bytes >= 0),
    CONSTRAINT uq_mapping_hub_uploaded_file_session_hash
        UNIQUE (session_id, sha256_hash)
);

CREATE TABLE mapping_hub_pipeline_run (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    status VARCHAR(40) NOT NULL,
    coverage_percent NUMERIC(5,2),
    present_count INTEGER NOT NULL DEFAULT 0,
    inferred_count INTEGER NOT NULL DEFAULT 0,
    partial_count INTEGER NOT NULL DEFAULT 0,
    missing_count INTEGER NOT NULL DEFAULT 0,
    blocking_gap_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(120),
    error_message TEXT,
    CONSTRAINT chk_mapping_hub_pipeline_run_status
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_mapping_hub_pipeline_run_coverage
        CHECK (coverage_percent IS NULL OR (coverage_percent >= 0 AND coverage_percent <= 100))
);

CREATE TABLE mapping_hub_pipeline_stage (
    id UUID PRIMARY KEY,
    pipeline_run_id UUID NOT NULL REFERENCES mapping_hub_pipeline_run(id) ON DELETE CASCADE,
    stage_name VARCHAR(40) NOT NULL,
    stage_order INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,
    console_lines JSONB NOT NULL DEFAULT '[]'::jsonb,
    error_code VARCHAR(120),
    error_message TEXT,
    CONSTRAINT chk_mapping_hub_pipeline_stage_name
        CHECK (stage_name IN ('PARSE', 'LIFT', 'INFER', 'MATCH', 'MANIFEST')),
    CONSTRAINT chk_mapping_hub_pipeline_stage_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED')),
    CONSTRAINT chk_mapping_hub_pipeline_stage_order
        CHECK (stage_order > 0),
    CONSTRAINT chk_mapping_hub_pipeline_stage_duration
        CHECK (duration_ms IS NULL OR duration_ms >= 0),
    CONSTRAINT uq_mapping_hub_pipeline_stage_run_name
        UNIQUE (pipeline_run_id, stage_name)
);

CREATE TABLE canonical_entity (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    canonical_id VARCHAR(255) NOT NULL,
    class_name VARCHAR(120) NOT NULL,
    name VARCHAR(500),
    source_file_id UUID REFERENCES mapping_hub_uploaded_file(id),
    source_file_name VARCHAR(500),
    origin VARCHAR(60) NOT NULL,
    inferred BOOLEAN NOT NULL DEFAULT FALSE,
    rdf_subject_iri TEXT,
    semantic_id TEXT,
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by_stage VARCHAR(40),
    created_by_run_id UUID REFERENCES mapping_hub_pipeline_run(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_canonical_entity_origin
        CHECK (origin IN ('PARSED', 'LIFTED', 'INFERRED', 'MANUAL', 'IMPORTED', 'AAS', 'MTP', 'OPC_UA')),
    CONSTRAINT chk_canonical_entity_created_by_stage
        CHECK (created_by_stage IS NULL OR created_by_stage IN ('PARSE', 'LIFT', 'INFER', 'MATCH', 'MANIFEST')),
    CONSTRAINT uq_canonical_entity_session_class_id
        UNIQUE (session_id, class_name, canonical_id)
);

CREATE TABLE canonical_entity_relation (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    source_entity_id UUID NOT NULL REFERENCES canonical_entity(id) ON DELETE CASCADE,
    relation_type VARCHAR(160) NOT NULL,
    target_entity_id UUID NOT NULL REFERENCES canonical_entity(id) ON DELETE CASCADE,
    inferred BOOLEAN NOT NULL DEFAULT FALSE,
    origin VARCHAR(60) NOT NULL,
    source_file_id UUID REFERENCES mapping_hub_uploaded_file(id),
    created_by_run_id UUID REFERENCES mapping_hub_pipeline_run(id),
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_canonical_entity_relation_origin
        CHECK (origin IN ('PARSED', 'LIFTED', 'INFERRED', 'MANUAL', 'IMPORTED', 'AAS', 'MTP', 'OPC_UA')),
    CONSTRAINT uq_canonical_relation
        UNIQUE (session_id, source_entity_id, relation_type, target_entity_id)
);

CREATE TABLE mapping_hub_instance_triple (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    source_file_id UUID REFERENCES mapping_hub_uploaded_file(id),
    pipeline_run_id UUID REFERENCES mapping_hub_pipeline_run(id),
    subject_iri TEXT NOT NULL,
    predicate_iri TEXT NOT NULL,
    object_value TEXT NOT NULL,
    object_is_literal BOOLEAN NOT NULL,
    object_datatype_iri TEXT,
    object_language VARCHAR(32),
    origin VARCHAR(60) NOT NULL,
    stage_name VARCHAR(40) NOT NULL,
    inferred BOOLEAN NOT NULL DEFAULT FALSE,
    manual_entry BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mapping_hub_instance_triple_origin
        CHECK (origin IN ('PARSED', 'LIFTED', 'INFERRED', 'MANUAL', 'IMPORTED', 'AAS', 'MTP', 'OPC_UA')),
    CONSTRAINT chk_mapping_hub_instance_triple_stage
        CHECK (stage_name IN ('PARSE', 'INSTANTIATE', 'LIFT', 'INFER', 'MATCH', 'MANIFEST'))
);

CREATE TABLE mapping_hub_coverage_entry (
    id UUID PRIMARY KEY,
    pipeline_run_id UUID NOT NULL REFERENCES mapping_hub_pipeline_run(id) ON DELETE CASCADE,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    class_name VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    entity_count INTEGER NOT NULL DEFAULT 0,
    missing_fields JSONB NOT NULL DEFAULT '[]'::jsonb,
    source_file_names JSONB NOT NULL DEFAULT '[]'::jsonb,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mapping_hub_coverage_entry_status
        CHECK (status IN ('PRESENT', 'PARTIAL', 'INFERRED', 'MISSING')),
    CONSTRAINT chk_mapping_hub_coverage_entry_count
        CHECK (entity_count >= 0),
    CONSTRAINT uq_mapping_hub_coverage_entry_run_class
        UNIQUE (pipeline_run_id, class_name)
);

CREATE TABLE mapping_hub_blocking_gap (
    id UUID PRIMARY KEY,
    pipeline_run_id UUID NOT NULL REFERENCES mapping_hub_pipeline_run(id) ON DELETE CASCADE,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    class_name VARCHAR(120) NOT NULL,
    gap_type VARCHAR(120) NOT NULL,
    message TEXT NOT NULL,
    required_fields JSONB NOT NULL DEFAULT '[]'::jsonb,
    recommended_formats JSONB NOT NULL DEFAULT '[]'::jsonb,
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE semantic_match_result (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    pipeline_run_id UUID NOT NULL REFERENCES mapping_hub_pipeline_run(id) ON DELETE CASCADE,
    required_capability_entity_id UUID REFERENCES canonical_entity(id),
    offered_capability_entity_id UUID REFERENCES canonical_entity(id),
    service_entity_id UUID REFERENCES canonical_entity(id),
    skill_entity_id UUID REFERENCES canonical_entity(id),
    communication_binding_entity_id UUID REFERENCES canonical_entity(id),
    data_connector_entity_id UUID REFERENCES canonical_entity(id),
    deployment_context_entity_id UUID REFERENCES canonical_entity(id),
    matched BOOLEAN NOT NULL,
    score NUMERIC(6,3),
    function_class VARCHAR(160),
    protocol VARCHAR(80),
    access_mode VARCHAR(80),
    network_zone VARCHAR(160),
    safety_integrity_level VARCHAR(80),
    endpoint_url TEXT,
    explanation TEXT,
    sparql_query_name VARCHAR(255),
    sparql_projection JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_semantic_match_result_score
        CHECK (score IS NULL OR score >= 0)
);

CREATE TABLE selected_service (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    match_result_id UUID REFERENCES semantic_match_result(id),
    service_entity_id UUID NOT NULL REFERENCES canonical_entity(id),
    deployment_context_entity_id UUID REFERENCES canonical_entity(id),
    communication_binding_entity_id UUID REFERENCES canonical_entity(id),
    data_connector_entity_id UUID REFERENCES canonical_entity(id),
    service_name VARCHAR(500) NOT NULL,
    image VARCHAR(500),
    endpoint_url TEXT,
    selected_by_user_id UUID,
    selected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    selection_reason TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE deployment_manifest (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    pipeline_run_id UUID REFERENCES mapping_hub_pipeline_run(id),
    selected_service_id UUID REFERENCES selected_service(id),
    manifest_type VARCHAR(60) NOT NULL,
    manifest_name VARCHAR(255) NOT NULL,
    content_yaml TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    validation_status VARCHAR(40) NOT NULL,
    validation_errors JSONB NOT NULL DEFAULT '[]'::jsonb,
    generated_from JSONB NOT NULL DEFAULT '{}'::jsonb,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_deployment_manifest_type
        CHECK (manifest_type IN ('DOCKER_COMPOSE', 'KUBERNETES')),
    CONSTRAINT chk_deployment_manifest_validation_status
        CHECK (validation_status IN ('VALID', 'INVALID', 'NOT_VALIDATED'))
);

CREATE TABLE mapping_hub_export_snapshot (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    pipeline_run_id UUID REFERENCES mapping_hub_pipeline_run(id),
    export_type VARCHAR(60) NOT NULL,
    content_json JSONB NOT NULL,
    content_hash CHAR(64) NOT NULL,
    exported_by_user_id UUID,
    exported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mapping_hub_export_snapshot_type
        CHECK (export_type IN ('COMPLETE_JSON_MANIFEST'))
);

ALTER TABLE mapping_hub_session
    ADD CONSTRAINT fk_mapping_hub_session_selected_manifest
        FOREIGN KEY (selected_manifest_id) REFERENCES deployment_manifest(id);

ALTER TABLE mapping_hub_session
    ADD CONSTRAINT fk_mapping_hub_session_last_pipeline_run
        FOREIGN KEY (last_pipeline_run_id) REFERENCES mapping_hub_pipeline_run(id);

CREATE INDEX idx_mapping_hub_session_user_id
    ON mapping_hub_session(user_id);

CREATE INDEX idx_mapping_hub_session_status
    ON mapping_hub_session(status);

CREATE INDEX idx_mapping_hub_uploaded_file_session_id
    ON mapping_hub_uploaded_file(session_id);

CREATE INDEX idx_mapping_hub_pipeline_run_session_id
    ON mapping_hub_pipeline_run(session_id);

CREATE INDEX idx_mapping_hub_pipeline_stage_run_id
    ON mapping_hub_pipeline_stage(pipeline_run_id);

CREATE INDEX idx_canonical_entity_session_id
    ON canonical_entity(session_id);

CREATE INDEX idx_canonical_entity_class_name
    ON canonical_entity(class_name);

CREATE INDEX idx_canonical_entity_origin
    ON canonical_entity(origin);

CREATE INDEX idx_canonical_entity_attributes_gin
    ON canonical_entity USING GIN (attributes);

CREATE INDEX idx_canonical_entity_relation_session_id
    ON canonical_entity_relation(session_id);

CREATE INDEX idx_canonical_entity_relation_source
    ON canonical_entity_relation(source_entity_id);

CREATE INDEX idx_canonical_entity_relation_target
    ON canonical_entity_relation(target_entity_id);

CREATE INDEX idx_canonical_entity_relation_type
    ON canonical_entity_relation(relation_type);

CREATE INDEX idx_mapping_hub_instance_triple_session_id
    ON mapping_hub_instance_triple(session_id);

CREATE INDEX idx_mapping_hub_instance_triple_source_file_id
    ON mapping_hub_instance_triple(source_file_id);

CREATE INDEX idx_mapping_hub_instance_triple_pipeline_run_id
    ON mapping_hub_instance_triple(pipeline_run_id);

CREATE INDEX idx_mapping_hub_instance_triple_subject
    ON mapping_hub_instance_triple(subject_iri);

CREATE INDEX idx_mapping_hub_instance_triple_predicate
    ON mapping_hub_instance_triple(predicate_iri);

CREATE INDEX idx_mapping_hub_coverage_entry_session_id
    ON mapping_hub_coverage_entry(session_id);

CREATE INDEX idx_mapping_hub_coverage_entry_run_id
    ON mapping_hub_coverage_entry(pipeline_run_id);

CREATE INDEX idx_mapping_hub_blocking_gap_session_id
    ON mapping_hub_blocking_gap(session_id);

CREATE INDEX idx_mapping_hub_blocking_gap_run_id
    ON mapping_hub_blocking_gap(pipeline_run_id);

CREATE INDEX idx_semantic_match_result_session_id
    ON semantic_match_result(session_id);

CREATE INDEX idx_semantic_match_result_pipeline_run_id
    ON semantic_match_result(pipeline_run_id);

CREATE INDEX idx_semantic_match_result_service_entity_id
    ON semantic_match_result(service_entity_id);

CREATE INDEX idx_semantic_match_result_matched
    ON semantic_match_result(matched);

CREATE UNIQUE INDEX uq_selected_service_active_session
    ON selected_service(session_id)
    WHERE active = TRUE;

CREATE INDEX idx_deployment_manifest_session_id
    ON deployment_manifest(session_id);

CREATE INDEX idx_deployment_manifest_pipeline_run_id
    ON deployment_manifest(pipeline_run_id);

CREATE INDEX idx_deployment_manifest_type
    ON deployment_manifest(manifest_type);

CREATE INDEX idx_mapping_hub_export_snapshot_session_id
    ON mapping_hub_export_snapshot(session_id);

CREATE INDEX idx_mapping_hub_export_snapshot_run_id
    ON mapping_hub_export_snapshot(pipeline_run_id);
