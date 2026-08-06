-- V5__mapping_hub_session_user_id_text.sql
-- Session Visibility attributes sessions to the authenticated user name, but V2
-- created mapping_hub_session.user_id as UUID. Align the column type with the
-- string principal so INSERT/ON CONFLICT in MappingHubPersistenceService works.

ALTER TABLE mapping_hub_session
    ALTER COLUMN user_id TYPE VARCHAR(255);
