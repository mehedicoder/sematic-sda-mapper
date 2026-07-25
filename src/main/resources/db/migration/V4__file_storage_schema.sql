-- V4__file_storage_schema.sql
-- File storage support: persist uploaded file contents on filesystem with metadata in DB

ALTER TABLE uploaded_file ADD COLUMN storage_path VARCHAR(1024);
ALTER TABLE uploaded_file ADD COLUMN mime_type VARCHAR(255);
ALTER TABLE uploaded_file ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE uploaded_file ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_uploaded_file_type ON uploaded_file(file_type);
CREATE INDEX idx_uploaded_file_filename ON uploaded_file(filename);
