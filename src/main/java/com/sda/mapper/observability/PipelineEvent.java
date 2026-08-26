package com.sda.mapper.observability;

import java.time.Instant;

/**
 * A persisted audit event describing one pipeline stage transition.
 *
 * <p>One row is written per {@code STARTED} / {@code SUCCEEDED} / {@code FAILED}
 * transition so that every historical pipeline run can be reconstructed from
 * the {@code pipeline_audit_log} table.</p>
 *
 * @param id             database identifier, {@code null} before insert
 * @param runId          identifier grouping one pipeline run
 * @param sessionId      mapping hub session id, {@code null} for pure REST runs
 * @param requirementId  requirement id, {@code null} for non-requirement stages
 * @param stage          stage name (PARSE, INSTANTIATE, LIFT, INFER, MATCH, MANIFEST, ...)
 * @param status         STARTED, SUCCEEDED or FAILED
 * @param startedAt      when the transition was recorded
 * @param durationMs     stage duration in ms, {@code null} for STARTED rows
 * @param detail         human-readable summary or failure reason
 */
public record PipelineEvent(
    Long id,
    String runId,
    String sessionId,
    String requirementId,
    String stage,
    String status,
    Instant startedAt,
    Long durationMs,
    String detail
) {

    /** Audit status written when a stage begins. */
    public static final String STARTED = "STARTED";
    /** Audit status written when a stage completes successfully. */
    public static final String SUCCEEDED = "SUCCEEDED";
    /** Audit status written when a stage throws. */
    public static final String FAILED = "FAILED";
}
