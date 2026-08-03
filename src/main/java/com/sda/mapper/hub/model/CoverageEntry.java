package com.sda.mapper.hub.model;

import java.util.List;

/**
 * Coverage result for a single ontology class of the mapping hub.
 *
 * @param className     the ontology class name
 * @param layerId       the layer the class belongs to
 * @param status        the computed coverage status
 * @param sources       distinct source file names that contributed entities
 * @param missingFields required fields that are missing or blank (empty when complete)
 */
public record CoverageEntry(
    String className,
    String layerId,
    CoverageStatus status,
    List<String> sources,
    List<String> missingFields
) {

    /**
     * Factory for an entry with no coverage at all.
     *
     * @param className the ontology class name
     * @param layerId   the layer the class belongs to
     * @return a {@code MISSING} entry
     */
    public static CoverageEntry missing(String className, String layerId) {
        return new CoverageEntry(className, layerId, CoverageStatus.MISSING, List.of(), List.of());
    }
}
