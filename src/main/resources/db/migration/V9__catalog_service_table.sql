-- Introduces a first-class, durable catalogue of offered services that the
-- Service Catalog view reads as its source of truth. It is populated from the
-- union of the base ontology and every uploaded instance triple for a session,
-- so all uploaded services (AAS / MTP / OPC-UA / ABox TTL) are listed and the
-- catalogue survives application restarts.

CREATE TABLE catalog_service (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mapping_hub_session(id) ON DELETE CASCADE,
    service_iri TEXT NOT NULL,
    service_name TEXT,
    capability_name TEXT,
    skill_name TEXT,
    protocol TEXT,
    access_mode TEXT,
    security_mode TEXT,
    recall DOUBLE PRECISION NOT NULL DEFAULT 0,
    latency_ms INTEGER NOT NULL DEFAULT 0,
    connector_name TEXT,
    deployment_name TEXT,
    safety_integrity_level TEXT,
    determinism_class TEXT,
    network_zone TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_catalog_service_session_service
    ON catalog_service (session_id, service_iri);
