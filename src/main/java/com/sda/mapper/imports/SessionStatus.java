package com.sda.mapper.imports;

/**
 * Lifecycle states for an import session.
 */
public enum SessionStatus {
    CREATED,
    UPLOADING,
    UPLOADED,
    VALIDATING,
    VALIDATED,
    ANALYZING,
    ANALYZED,
    COMPLETING,
    COMPLETED,
    RESOLVING,
    RESOLVED,
    PREVIEWING,
    PREVIEWED,
    MERGING,
    MERGED,
    COMMITTED,
    FAILED
}
