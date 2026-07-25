package com.sda.mapper.imports;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Generates structured import reports with asset counts, warnings, errors, and completeness scores.
 * Extended in Phase 16 with per-category completeness analysis.
 */
@Component
public class ImportReportService {

    private static final Logger log = LoggerFactory.getLogger(ImportReportService.class);

    private final MissingInformationDetector missingInfoDetector;
    private final ImportRecommendationService recommendationService;

    public ImportReportService(MissingInformationDetector missingInfoDetector,
                                ImportRecommendationService recommendationService) {
        this.missingInfoDetector = missingInfoDetector;
        this.recommendationService = recommendationService;
    }

    /**
     * Generates an import report from merge results and conflicts.
     */
    public ImportResult generateReport(ImportSession session,
                                        OntologyMergeService.MergeResult mergeResult,
                                        List<ConflictDetectionService.Conflict> conflicts) {
        ImportResult result = new ImportResult(session);
        result.setAssetsFound(mergeResult.assetsFound());
        result.setServicesFound(mergeResult.servicesFound());
        result.setCapabilitiesFound(mergeResult.capabilitiesFound());
        result.setSkillsFound(mergeResult.skillsFound());
        result.setCommunicationBindingsFound(mergeResult.communicationBindingsFound());
        result.setDataConnectorsFound(mergeResult.dataConnectorsFound());
        result.setConflictsDetected(conflicts.size());
        result.setWarnings(mergeResult.warnings());

        int errors = (int) conflicts.stream()
            .filter(c -> c.type() == ConflictDetectionService.ConflictType.DUPLICATE_ASSET)
            .count();
        result.setErrors(errors);

        result.setCompletenessScore(calculateCompleteness(mergeResult));
        result.setSummary(buildSummary(mergeResult, conflicts));

        log.info("Generated import report: {} assets, {} conflicts, completeness={}%",
            result.getAssetsFound(), result.getConflictsDetected(),
            String.format("%.1f", result.getCompletenessScore()));
        return result;
    }

    /**
     * Generates a detailed completeness report by analyzing the merged model
     * for missing semantic information per category.
     */
    public CompletenessReport generateCompletenessReport(Model mergedModel) {
        List<MissingItem> missingItems = missingInfoDetector.detectMissing(mergedModel);
        Map<MissingCategory, ImportRecommendationService.Recommendation> recommendations = recommendationService.recommend(missingItems);

        Map<MissingCategory, Long> countsByCategory = new HashMap<>();
        for (MissingItem item : missingItems) {
            countsByCategory.merge(item.category(), 1L, Long::sum);
        }

        Map<MissingCategory, Double> categoryScores = new LinkedHashMap<>();
        Map<MissingCategory, Integer> missingCounts = new LinkedHashMap<>();
        List<String> recommendedActions = new ArrayList<>();

        for (MissingCategory category : MissingCategory.values()) {
            int count = countsByCategory.getOrDefault(category, 0L).intValue();
            missingCounts.put(category, count);

            double score = count == 0 ? 100.0 : Math.max(0, 100.0 - (count * 20.0));
            categoryScores.put(category, score);

            if (count > 0 && recommendations.containsKey(category)) {
                recommendedActions.add(recommendations.get(category).description());
            }
        }

        double overallScore = categoryScores.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        if (missingItems.isEmpty()) {
            overallScore = 100.0;
        }

        log.info("Completeness analysis: overall={}%, {} missing items across {} categories",
            String.format("%.1f", overallScore), missingItems.size(), countsByCategory.size());

        return new CompletenessReport(
            overallScore, categoryScores, missingCounts,
            missingItems, recommendedActions, countEntities(mergedModel)
        );
    }

    private double calculateCompleteness(OntologyMergeService.MergeResult mergeResult) {
        int totalEntities = mergeResult.assetsFound() + mergeResult.servicesFound()
            + mergeResult.capabilitiesFound() + mergeResult.skillsFound()
            + mergeResult.communicationBindingsFound() + mergeResult.dataConnectorsFound();

        if (totalEntities == 0) {
            return 0.0;
        }

        double score = 0.0;
        if (mergeResult.assetsFound() > 0) score += 20;
        if (mergeResult.servicesFound() > 0) score += 20;
        if (mergeResult.capabilitiesFound() > 0) score += 20;
        if (mergeResult.skillsFound() > 0) score += 15;
        if (mergeResult.communicationBindingsFound() > 0) score += 15;
        if (mergeResult.dataConnectorsFound() > 0) score += 10;

        return Math.min(score, 100.0);
    }

    private int countEntities(Model model) {
        return (int) model.listResourcesWithProperty(
            org.apache.jena.vocabulary.RDF.type).toList().size();
    }

    private String buildSummary(OntologyMergeService.MergeResult mergeResult,
                                 List<ConflictDetectionService.Conflict> conflicts) {
        StringBuilder sb = new StringBuilder();
        sb.append("Merged ").append(mergeResult.assetsFound()).append(" assets, ");
        sb.append(mergeResult.servicesFound()).append(" services, ");
        sb.append(mergeResult.capabilitiesFound()).append(" capabilities, ");
        sb.append(mergeResult.skillsFound()).append(" skills, ");
        sb.append(mergeResult.communicationBindingsFound()).append(" communication bindings, ");
        sb.append(mergeResult.dataConnectorsFound()).append(" data connectors.");

        if (!conflicts.isEmpty()) {
            sb.append(" ").append(conflicts.size()).append(" conflict(s) detected.");
        }

        return sb.toString();
    }
}
