package com.sda.mapper.hub.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of a persisted Mapping Hub session for the session
 * visibility view.
 */
public record SessionOverview(
        UUID id,
        String name,
        String owner,
        String status,
        double coveragePercent,
        int blockingGapCount,
        int fileCount,
        int entityCount,
        int matchCount,
        int manifestCount,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * A session is complete when it was explicitly marked {@code COMPLETED}.
     * Otherwise it is complete only when its latest pipeline run reached 100%
     * coverage with zero blocking gaps; a failed run is always incomplete,
     * regardless of stored coverage.
     *
     * @return {@code true} when the session is complete
     */
    public boolean complete() {
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return true;
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return false;
        }
        return coveragePercent >= 100.0 && blockingGapCount == 0;
    }
}
