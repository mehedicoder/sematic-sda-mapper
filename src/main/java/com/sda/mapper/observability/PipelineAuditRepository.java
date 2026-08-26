package com.sda.mapper.observability;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC persistence for {@link PipelineEvent} audit rows backed by the
 * {@code pipeline_audit_log} table (Flyway V6). Follows the same
 * record-plus-{@link JdbcTemplate} convention as the mapping hub persistence
 * layer; no Hibernate entity is involved.
 */
@Component
public class PipelineAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates the repository.
     *
     * @param jdbcTemplate database access
     */
    public PipelineAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<PipelineEvent> ROW_MAPPER = (rs, rowNum) -> new PipelineEvent(
        rs.getLong("id"),
        rs.getString("run_id"),
        rs.getString("session_id"),
        rs.getString("requirement_id"),
        rs.getString("stage"),
        rs.getString("status"),
        rs.getTimestamp("started_at").toInstant(),
        rs.getObject("duration_ms") == null ? null : rs.getLong("duration_ms"),
        rs.getString("detail"));

    /**
     * Inserts one audit event.
     *
     * @param event the event to persist
     */
    public void insert(PipelineEvent event) {
        jdbcTemplate.update("""
            INSERT INTO pipeline_audit_log
              (run_id, session_id, requirement_id, stage, status, started_at, duration_ms, detail)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            event.runId(), event.sessionId(), event.requirementId(), event.stage(),
            event.status(), Timestamp.from(event.startedAt()),
            event.durationMs(), event.detail());
    }

    /**
     * Queries the audit timeline with optional filters.
     *
     * @param sessionId     exact session id match, or {@code null} to ignore
     * @param requirementId exact requirement id match, or {@code null} to ignore
     * @param runId         exact run id match, or {@code null} to ignore
     * @param limit         maximum number of rows returned
     * @return chronological timeline ordered by {@code started_at}, then id
     */
    public List<PipelineEvent> find(String sessionId, String requirementId, String runId, int limit) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, run_id, session_id, requirement_id, stage, status, started_at, duration_ms, detail "
                + "FROM pipeline_audit_log WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (sessionId != null && !sessionId.isBlank()) {
            sql.append(" AND session_id = ?");
            args.add(sessionId);
        }
        if (requirementId != null && !requirementId.isBlank()) {
            sql.append(" AND requirement_id = ?");
            args.add(requirementId);
        }
        if (runId != null && !runId.isBlank()) {
            sql.append(" AND run_id = ?");
            args.add(runId);
        }
        sql.append(" ORDER BY started_at ASC, id ASC LIMIT ?");
        args.add(limit);
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray());
    }
}
