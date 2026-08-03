package com.sda.mapper.hub.model;

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
}
