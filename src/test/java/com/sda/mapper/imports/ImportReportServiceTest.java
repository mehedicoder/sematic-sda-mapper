package com.sda.mapper.imports;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Import Report Service")
class ImportReportServiceTest {

    private final ImportReportService reportService = new ImportReportService(
        new MissingInformationDetector(new com.sda.mapper.ontology.OntologyModelLoader()),
        new ImportRecommendationService()
    );

    @Test
    @DisplayName("Should generate report with correct entity counts")
    void shouldGenerateReportWithCounts() {
        ImportSession session = new ImportSession("Test Session");
        Model model = ModelFactory.createDefaultModel();

        var mergeResult = new OntologyMergeService.MergeResult(
            model, 5, 3, 2, 4, 2, 1, 0
        );

        ImportResult report = reportService.generateReport(session, mergeResult, Collections.emptyList());

        assertEquals(5, report.getAssetsFound());
        assertEquals(3, report.getServicesFound());
        assertEquals(2, report.getCapabilitiesFound());
        assertEquals(4, report.getSkillsFound());
        assertEquals(2, report.getCommunicationBindingsFound());
        assertEquals(1, report.getDataConnectorsFound());
        assertEquals(0, report.getConflictsDetected());
    }

    @Test
    @DisplayName("Should calculate completeness score based on entity types present")
    void shouldCalculateCompletenessScore() {
        ImportSession session = new ImportSession("Test Session");
        Model model = ModelFactory.createDefaultModel();

        var mergeResult = new OntologyMergeService.MergeResult(
            model, 1, 1, 1, 1, 1, 1, 0
        );

        ImportResult report = reportService.generateReport(session, mergeResult, Collections.emptyList());

        assertEquals(100.0, report.getCompletenessScore(), 0.1);
    }

    @Test
    @DisplayName("Should calculate partial completeness when some types missing")
    void shouldCalculatePartialCompleteness() {
        ImportSession session = new ImportSession("Test Session");
        Model model = ModelFactory.createDefaultModel();

        var mergeResult = new OntologyMergeService.MergeResult(
            model, 1, 1, 0, 0, 0, 0, 0
        );

        ImportResult report = reportService.generateReport(session, mergeResult, Collections.emptyList());

        assertEquals(40.0, report.getCompletenessScore(), 0.1);
    }

    @Test
    @DisplayName("Should count conflicts as errors")
    void shouldCountConflictsAsErrors() {
        ImportSession session = new ImportSession("Test Session");
        Model model = ModelFactory.createDefaultModel();

        var mergeResult = new OntologyMergeService.MergeResult(
            model, 1, 1, 0, 0, 0, 0, 0
        );

        var conflicts = List.of(
            new ConflictDetectionService.Conflict(
                ConflictDetectionService.ConflictType.DUPLICATE_ASSET,
                "Duplicate asset", "merge"
            ),
            new ConflictDetectionService.Conflict(
                ConflictDetectionService.ConflictType.DUPLICATE_ASSET,
                "Another duplicate", "replace"
            )
        );

        ImportResult report = reportService.generateReport(session, mergeResult, conflicts);

        assertEquals(2, report.getConflictsDetected());
        assertEquals(2, report.getErrors());
    }

    @Test
    @DisplayName("Should return zero completeness for empty merge result")
    void shouldReturnZeroCompletenessForEmptyResult() {
        ImportSession session = new ImportSession("Test Session");
        Model model = ModelFactory.createDefaultModel();

        var mergeResult = new OntologyMergeService.MergeResult(
            model, 0, 0, 0, 0, 0, 0, 0
        );

        ImportResult report = reportService.generateReport(session, mergeResult, Collections.emptyList());

        assertEquals(0.0, report.getCompletenessScore(), 0.1);
    }

    @Test
    @DisplayName("Should build summary with entity counts and conflict info")
    void shouldBuildSummary() {
        ImportSession session = new ImportSession("Test Session");
        Model model = ModelFactory.createDefaultModel();

        var mergeResult = new OntologyMergeService.MergeResult(
            model, 2, 1, 1, 1, 1, 1, 0
        );

        var conflicts = List.of(
            new ConflictDetectionService.Conflict(
                ConflictDetectionService.ConflictType.DUPLICATE_ASSET,
                "Duplicate", "merge"
            )
        );

        ImportResult report = reportService.generateReport(session, mergeResult, conflicts);

        assertNotNull(report.getSummary());
        assertTrue(report.getSummary().contains("2 assets"));
        assertTrue(report.getSummary().contains("1 service"));
        assertTrue(report.getSummary().contains("1 conflict(s) detected"));
    }
}
