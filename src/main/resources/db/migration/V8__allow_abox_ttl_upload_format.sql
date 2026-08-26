-- Extends the uploaded-file format check constraint to cover ABOX_TTL
-- (raw SDA ABox Turtle: service catalogue, asset descriptions, edge-node
-- topology). Without this, uploading such a file fails the
-- chk_mapping_hub_uploaded_file_format check constraint.

ALTER TABLE mapping_hub_uploaded_file
    DROP CONSTRAINT chk_mapping_hub_uploaded_file_format;

ALTER TABLE mapping_hub_uploaded_file
    ADD CONSTRAINT chk_mapping_hub_uploaded_file_format
    CHECK (detected_format IN (
        'AAS_XML', 'AAS_JSON', 'MTP_AML', 'OPCUA_JSON', 'AUTOMATION_ML',
        'REQUIREMENT_TTL', 'ABOX_TTL'
    ));
