package com.sda.mapper.hub.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Full coverage report over all 24 ontology classes of the mapping hub.
 *
 * @param entries         one coverage entry per ontology class, in ontology order
 * @param blockingGaps    gaps that block core relations with covered classes
 * @param coveragePercent weighted coverage percentage (0.0 to 100.0)
 */
public record GapReport(
    List<CoverageEntry> entries,
    List<BlockingGap> blockingGaps,
    double coveragePercent
) {

    /**
     * Finds the coverage entry for the given ontology class.
     *
     * @param className the ontology class name
     * @return the matching entry, or empty when unknown
     */
    public Optional<CoverageEntry> forClass(String className) {
        return entries.stream().filter(e -> e.className().equals(className)).findFirst();
    }

    /**
     * Counts the entries with the given status.
     *
     * @param status the status to count
     * @return the number of matching entries
     */
    public int count(CoverageStatus status) {
        return (int) entries.stream().filter(e -> e.status() == status).count();
    }

    /**
     * Aggregates the coverage entries per CSS-SDA layer in first-seen layer
     * order (L1, L2, L2b, L3, L4).
     *
     * @return one aggregated coverage per layer
     */
    public List<LayerCoverage> layerCoverage() {
        List<LayerCoverage> result = new ArrayList<>();
        for (CoverageEntry entry : entries) {
            String layerId = entry.layerId();
            if (result.stream().noneMatch(l -> l.layerId().equals(layerId))) {
                result.add(LayerCoverage.aggregate(layerId, entries));
            }
        }
        return List.copyOf(result);
    }
}
