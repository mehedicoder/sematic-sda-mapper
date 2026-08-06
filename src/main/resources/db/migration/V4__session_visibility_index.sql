CREATE INDEX IF NOT EXISTS idx_mapping_hub_session_owner_updated
    ON mapping_hub_session (user_id, updated_at DESC);
