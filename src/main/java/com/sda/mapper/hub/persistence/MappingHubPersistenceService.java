package com.sda.mapper.hub.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.EntityOrigin;
import com.sda.mapper.hub.model.SessionOverview;
import com.sda.mapper.hub.model.UploadedFileInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists Mapping Hub session state, uploaded file content and instance RDF
 * triples in PostgreSQL. The base ontology remains read-only on the classpath;
 * only session ABox data is stored here.
 */
@Service
public class MappingHubPersistenceService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CanonicalEntityRestorer entityRestorer;

    /**
     * Creates the persistence service.
     *
     * @param jdbcTemplate   database access
     * @param entityRestorer typed canonical entity reconstruction
     */
    public MappingHubPersistenceService(JdbcTemplate jdbcTemplate, CanonicalEntityRestorer entityRestorer) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityRestorer = entityRestorer;
    }

    /**
     * Ensures that the durable session row exists without a user.
     *
     * @param sessionId mapping hub session id
     */
    public void ensureSession(UUID sessionId) {
        ensureSession(sessionId, null);
    }

    /**
     * Ensures that the durable session row exists, attributing it to the given
     * user when known.
     *
     * @param sessionId mapping hub session id
     * @param userId    owning user name, or {@code null}
     */
    public void ensureSession(UUID sessionId, String userId) {
        jdbcTemplate.update("""
            INSERT INTO mapping_hub_session (id, name, user_id, status)
            VALUES (?, ?, ?, 'ACTIVE')
            ON CONFLICT (id) DO UPDATE SET user_id = COALESCE(EXCLUDED.user_id, mapping_hub_session.user_id)
            """, sessionId, "Mapping Hub Session " + sessionId, userId);
    }

    /**
     * Starts a durable pipeline run.
     *
     * @param sessionId owning session id
     * @return pipeline run id
     */
    public UUID startPipelineRun(UUID sessionId) {
        ensureSession(sessionId);
        UUID runId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO mapping_hub_pipeline_run (id, session_id, status, started_at)
            VALUES (?, ?, 'RUNNING', ?)
            """, runId, sessionId, Timestamp.from(Instant.now()));
        jdbcTemplate.update("""
            UPDATE mapping_hub_session
            SET last_pipeline_run_id = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, runId, sessionId);
        return runId;
    }

    /**
     * Completes a durable pipeline run.
     *
     * @param sessionId owning session id
     * @param runId pipeline run id
     * @param coveragePercent latest coverage percentage
     * @param blockingGapCount latest blocking gap count
     */
    public void completePipelineRun(UUID sessionId, UUID runId, double coveragePercent, int blockingGapCount) {
        jdbcTemplate.update("""
            UPDATE mapping_hub_pipeline_run
            SET status = 'COMPLETED',
                coverage_percent = ?,
                blocking_gap_count = ?,
                completed_at = ?
            WHERE id = ?
            """, coveragePercent, blockingGapCount, Timestamp.from(Instant.now()), runId);
        jdbcTemplate.update("""
            UPDATE mapping_hub_session
            SET current_coverage_percent = ?,
                current_blocking_gap_count = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, coveragePercent, blockingGapCount, sessionId);
    }

    /**
     * Marks a pipeline run as failed.
     *
     * @param runId pipeline run id
     * @param errorMessage failure message
     */
    public void failPipelineRun(UUID runId, String errorMessage) {
        jdbcTemplate.update("""
            UPDATE mapping_hub_pipeline_run
            SET status = 'FAILED',
                error_message = ?,
                completed_at = ?
            WHERE id = ?
            """, errorMessage, Timestamp.from(Instant.now()), runId);
    }

    /**
     * Persists uploaded file content for future re-processing.
     *
     * @param sessionId owning session id
     * @param info uploaded file metadata
     * @param content uploaded bytes
     * @return uploaded file row id
     */
    public UUID saveUploadedFile(UUID sessionId, UploadedFileInfo info, byte[] content) {
        ensureSession(sessionId);
        UUID fileId = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO mapping_hub_uploaded_file (
                id, session_id, original_file_name, detected_format, content_type,
                file_size_bytes, sha256_hash, content, parse_status, uploaded_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
            """, fileId, sessionId, info.fileName(), info.format().name(), contentType(info.fileName()),
            content.length, sha256(content), content, Timestamp.from(Instant.now()));
        return fileId;
    }

    /**
     * Marks an uploaded file as successfully parsed.
     *
     * @param fileId uploaded file id
     */
    public void markUploadedFileParsed(UUID fileId) {
        jdbcTemplate.update("""
            UPDATE mapping_hub_uploaded_file
            SET parse_status = 'PARSED', parse_error = NULL
            WHERE id = ?
            """, fileId);
    }

    /**
     * Marks an uploaded file as failed while preserving its original content.
     *
     * @param fileId uploaded file id
     * @param message parse failure message
     */
    public void markUploadedFileFailed(UUID fileId, String message) {
        jdbcTemplate.update("""
            UPDATE mapping_hub_uploaded_file
            SET parse_status = 'FAILED', parse_error = ?
            WHERE id = ?
            """, message, fileId);
    }

    /**
     * Persists canonical entities as an audit/UI snapshot.
     *
     * @param sessionId owning session id
     * @param runId pipeline run id
     * @param sourceFileId source file id, when file-derived
     * @param stageName stage creating the entities
     * @param origin semantic origin
     * @param entities entities to persist
     */
    public void saveCanonicalEntities(UUID sessionId, UUID runId, UUID sourceFileId, String stageName,
                                      EntityOrigin origin, List<? extends CanonicalEntity> entities) {
        for (CanonicalEntity entity : entities) {
            jdbcTemplate.update("""
                INSERT INTO canonical_entity (
                    id, session_id, canonical_id, class_name, name, source_file_id,
                    source_file_name, origin, inferred, attributes, created_by_stage,
                    created_by_run_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?)
                ON CONFLICT (session_id, class_name, canonical_id)
                DO UPDATE SET
                    name = EXCLUDED.name,
                    source_file_id = EXCLUDED.source_file_id,
                    source_file_name = EXCLUDED.source_file_name,
                    origin = EXCLUDED.origin,
                    inferred = EXCLUDED.inferred,
                    attributes = EXCLUDED.attributes,
                    created_by_stage = EXCLUDED.created_by_stage,
                    created_by_run_id = EXCLUDED.created_by_run_id,
                    updated_at = CURRENT_TIMESTAMP
                """,
                UUID.randomUUID(), sessionId, entity.id(), entity.className(), entity.name(),
                sourceFileId, entity.sourceFileName().orElse(null), origin.name(), entity.inferred(),
                toJson(entity.attributes()), stageName, runId);
        }
    }

    /**
     * Persists RDF instance triples.
     *
     * @param triples triples to store
     */
    public void saveTriples(List<InstanceTriple> triples) {
        for (InstanceTriple triple : triples) {
            jdbcTemplate.update("""
                INSERT INTO mapping_hub_instance_triple (
                    id, session_id, source_file_id, pipeline_run_id, subject_iri,
                    predicate_iri, object_value, object_is_literal, object_datatype_iri,
                    object_language, origin, stage_name, inferred, manual_entry
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                triple.id(), triple.sessionId(), triple.sourceFileId(), triple.pipelineRunId(),
                triple.subjectIri(), triple.predicateIri(), triple.objectValue(),
                triple.objectIsLiteral(), triple.objectDatatypeIri(), triple.objectLanguage(),
                triple.origin().name(), triple.stageName(), triple.inferred(), triple.manualEntry());
        }
    }

    /**
     * Loads all instance triples for a Mapping Hub session.
     *
     * @param sessionId owning session id
     * @return persisted instance triples
     */
    public List<InstanceTriple> loadTriples(UUID sessionId) {
        return jdbcTemplate.query("""
            SELECT id, session_id, source_file_id, pipeline_run_id, subject_iri,
                   predicate_iri, object_value, object_is_literal, object_datatype_iri,
                   object_language, origin, stage_name, inferred, manual_entry
            FROM mapping_hub_instance_triple
            WHERE session_id = ?
            ORDER BY created_at, id
            """, this::mapTriple, sessionId);
    }

    /**
     * Deletes all persisted data for a Mapping Hub session.
     *
     * @param sessionId owning session id
     */
    public void deleteSessionData(UUID sessionId) {
        jdbcTemplate.update("DELETE FROM mapping_hub_session WHERE id = ?", sessionId);
    }

    /**
     * Lists all persisted Mapping Hub sessions with their aggregate counts,
     * most recently updated first.
     *
     * @return session overviews
     */
    public List<SessionOverview> listSessions() {
        return jdbcTemplate.query("""
            SELECT s.id, s.name, s.user_id AS owner, s.status,
                   s.current_coverage_percent AS coverage_percent,
                   s.current_blocking_gap_count AS blocking_gap_count,
                   s.created_at, s.updated_at,
                   (SELECT COUNT(*) FROM mapping_hub_uploaded_file f WHERE f.session_id = s.id) AS file_count,
                   (SELECT COUNT(*) FROM canonical_entity e WHERE e.session_id = s.id) AS entity_count,
                   (SELECT COUNT(*) FROM semantic_match_result m WHERE m.session_id = s.id) AS match_count,
                   (SELECT COUNT(*) FROM deployment_manifest d WHERE d.session_id = s.id) AS manifest_count
            FROM mapping_hub_session s
            ORDER BY s.updated_at DESC
            """, this::mapSessionOverview);
    }

    /**
     * Marks a Mapping Hub session as archived.
     *
     * @param sessionId owning session id
     */
    public void archiveSession(UUID sessionId) {
        jdbcTemplate.update("""
            UPDATE mapping_hub_session
            SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, sessionId);
    }

    /**
     * Loads the canonical entities persisted for a Mapping Hub session,
     * reconstructed as their typed records.
     *
     * @param sessionId owning session id
     * @return the reconstructed canonical entities
     */
    public List<CanonicalEntity> loadCanonicalEntities(UUID sessionId) {
        if (entityRestorer == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
            SELECT class_name, canonical_id, name, source_file_name, inferred, attributes
            FROM canonical_entity
            WHERE session_id = ?
            ORDER BY updated_at, canonical_id
            """, (rs, rowNum) -> mapCanonicalEntity(rs), sessionId);
    }

    private SessionOverview mapSessionOverview(ResultSet rs, int rowNum) throws SQLException {
        return new SessionOverview(
            rs.getObject("id", UUID.class),
            rs.getString("name"),
            rs.getString("owner"),
            rs.getString("status"),
            rs.getDouble("coverage_percent"),
            rs.getInt("blocking_gap_count"),
            rs.getInt("file_count"),
            rs.getInt("entity_count"),
            rs.getInt("match_count"),
            rs.getInt("manifest_count"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant());
    }

    private CanonicalEntity mapCanonicalEntity(ResultSet rs) throws SQLException {
        return entityRestorer.restore(
            rs.getString("class_name"),
            rs.getString("canonical_id"),
            rs.getString("name"),
            rs.getString("source_file_name"),
            rs.getBoolean("inferred"),
            readAttributes(rs.getString("attributes")));
    }

    private Map<String, Object> readAttributes(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse persisted canonical entity attributes", e);
        }
    }

    private InstanceTriple mapTriple(ResultSet rs, int rowNum) throws SQLException {
        return new InstanceTriple(
            rs.getObject("id", UUID.class),
            rs.getObject("session_id", UUID.class),
            rs.getObject("source_file_id", UUID.class),
            rs.getObject("pipeline_run_id", UUID.class),
            rs.getString("subject_iri"),
            rs.getString("predicate_iri"),
            rs.getString("object_value"),
            rs.getBoolean("object_is_literal"),
            rs.getString("object_datatype_iri"),
            rs.getString("object_language"),
            EntityOrigin.valueOf(rs.getString("origin")),
            rs.getString("stage_name"),
            rs.getBoolean("inferred"),
            rs.getBoolean("manual_entry"));
    }

    private String toJson(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize canonical entity attributes", e);
        }
    }

    private static String contentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".xml") || lower.endsWith(".aml")) {
            return "application/xml";
        }
        return "application/octet-stream";
    }

    private static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            return HexFormat.of().formatHex(String.valueOf(content.length).getBytes(StandardCharsets.UTF_8));
        }
    }
}
