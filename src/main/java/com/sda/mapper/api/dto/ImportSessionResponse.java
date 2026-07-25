package com.sda.mapper.api.dto;

import com.sda.mapper.imports.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for import session responses.
 */
public record ImportSessionResponse(
    UUID id,
    String name,
    SessionStatus status,
    int fileCount,
    int totalTriples,
    int validationErrors,
    int warnings,
    Instant createdAt,
    Instant updatedAt,
    Instant committedAt,
    List<ImportFileResponse> files,
    ImportResultResponse importResult
) {
    /**
     * DTO for uploaded file responses.
     */
    public record ImportFileResponse(
        UUID id,
        String filename,
        String fileType,
        String status,
        long sizeBytes,
        int tripleCount,
        String validationReport,
        String errorMessage
    ) {
    }

    /**
     * DTO for import result responses.
     */
    public record ImportResultResponse(
        int assetsFound,
        int servicesFound,
        int capabilitiesFound,
        int skillsFound,
        int communicationBindingsFound,
        int dataConnectorsFound,
        int conflictsDetected,
        int warnings,
        int errors,
        double completenessScore,
        String summary
    ) {
    }
}
