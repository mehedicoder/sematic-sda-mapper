package com.sda.mapper.api.dto;

import jakarta.validation.constraints.Positive;

/**
 * Request payload for semantic requirement matching.
 * Supports two modes: Mode A (requirementId) or Mode B (inline fields).
 */
public record MatchRequirementRequest(
    String requirementId,
    String requirementName,
    String requirementDescription,
    String capabilityType,
    String functionClass,
    String protocol,
    String accessMode,
    String engineeringUnit,
    String safetyIntegrityLevel,
    String networkZone,
    String determinismClass,
    @Positive(message = "minRecall must be positive") Double minRecall,
    @Positive(message = "maxLatencyMs must be positive") Integer maxLatencyMs,
    Double nominalRangeMin,
    Double nominalRangeMax
) {
}
