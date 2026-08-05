package com.sda.mapper.hub.model;

import java.util.List;

/**
 * Aggregated coverage for a single CSS-SDA layer of the mapping hub.
 *
 * @param layerId         the layer identifier (e.g. {@code "L2b"})
 * @param total           number of ontology classes in the layer
 * @param present         classes covered by parsed entities
 * @param inferred        classes covered only by inferred entities
 * @param partial         classes with incomplete required fields
 * @param missing         classes with no coverage at all
 * @param coveragePercent weighted coverage percentage of the layer (0.0 to 100.0)
 */
public record LayerCoverage(
    String layerId,
    int total,
    int present,
    int inferred,
    int partial,
    int missing,
    double coveragePercent
) {

    /**
     * Aggregates the coverage entries of a layer using the same weighting as
     * the overall report (present = 1.0, partial/inferred = 0.5, missing = 0.0).
     *
     * @param layerId the layer identifier
     * @param entries the full coverage report entries
     * @return the aggregated layer coverage
     */
    public static LayerCoverage aggregate(String layerId, List<CoverageEntry> entries) {
        List<CoverageEntry> layerEntries = entries.stream()
            .filter(e -> e.layerId().equals(layerId))
            .toList();
        int total = layerEntries.size();
        int present = count(layerEntries, CoverageStatus.PRESENT);
        int inferred = count(layerEntries, CoverageStatus.INFERRED);
        int partial = count(layerEntries, CoverageStatus.PARTIAL);
        int missing = count(layerEntries, CoverageStatus.MISSING);
        double weighted = present + 0.5 * (inferred + partial);
        double percent = total == 0 ? 0.0 : Math.round(weighted / total * 1000.0) / 10.0;
        return new LayerCoverage(layerId, total, present, inferred, partial, missing, percent);
    }

    private static int count(List<CoverageEntry> entries, CoverageStatus status) {
        return (int) entries.stream().filter(e -> e.status() == status).count();
    }
}
