-- V3__add_instantiate_pipeline_stage.sql
-- The pipeline has five stages: Parse -> Instantiate -> Lift -> Infer -> Match.
-- V2 omitted 'INSTANTIATE' from the canonical_entity and pipeline_stage checks,
-- so persisting parsed/manual entities with created_by_stage = 'INSTANTIATE'
-- violated chk_canonical_entity_created_by_stage at runtime.

ALTER TABLE canonical_entity
    DROP CONSTRAINT chk_canonical_entity_created_by_stage;

ALTER TABLE canonical_entity
    ADD CONSTRAINT chk_canonical_entity_created_by_stage
        CHECK (created_by_stage IS NULL OR created_by_stage IN ('PARSE', 'INSTANTIATE', 'LIFT', 'INFER', 'MATCH', 'MANIFEST'));

ALTER TABLE mapping_hub_pipeline_stage
    DROP CONSTRAINT chk_mapping_hub_pipeline_stage_name;

ALTER TABLE mapping_hub_pipeline_stage
    ADD CONSTRAINT chk_mapping_hub_pipeline_stage_name
        CHECK (stage_name IN ('PARSE', 'INSTANTIATE', 'LIFT', 'INFER', 'MATCH', 'MANIFEST'));
