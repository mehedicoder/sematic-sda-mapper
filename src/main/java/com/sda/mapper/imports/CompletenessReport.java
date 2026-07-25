package com.sda.mapper.imports;

import java.util.List;
import java.util.Map;

/**
 * Completeness analysis report for an import session.
 * Contains per-category scores, missing items, and recommended next actions.
 */
public record CompletenessReport(
    double overallScore,
    Map<MissingCategory, Double> categoryScores,
    Map<MissingCategory, Integer> missingCounts,
    List<MissingItem> missingItems,
    List<String> recommendedActions,
    int totalEntitiesAnalyzed
) {
}
