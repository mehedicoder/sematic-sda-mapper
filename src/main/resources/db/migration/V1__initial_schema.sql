-- V1__initial_schema.sql
-- Initial schema for Semantic SDA Mapper

-- System status table for health checks
CREATE TABLE IF NOT EXISTS system_status (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(50) NOT NULL DEFAULT 'UP',
    application VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial system status
INSERT INTO system_status (status, application) VALUES ('UP', 'semantic-sda-mapper')
ON CONFLICT DO NOTHING;
