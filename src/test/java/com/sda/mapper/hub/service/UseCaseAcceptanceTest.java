package com.sda.mapper.hub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.parser.AasModelParser;
import com.sda.mapper.hub.parser.AboxTtlParser;
import com.sda.mapper.hub.parser.AutomationMlParser;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.MtpModelParser;
import com.sda.mapper.hub.parser.OpcUaModelParser;
import com.sda.mapper.hub.parser.OpcUaNodeSetXmlParser;
import com.sda.mapper.hub.parser.RequirementTtlParser;
import com.sda.mapper.observability.PipelineAuditPublisher;
import com.sda.mapper.observability.PipelineMetrics;
import com.sda.mapper.observability.PipelineStageObserver;
import com.sda.mapper.observability.PipelineTracingContext;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.service.OntologyDataService;
import com.sda.mapper.support.DemoAboxBuilder;
import com.sda.mapper.hub.model.CapabilityMatch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance gate: every documented use case under {@code docs/usecases} must be
 * runnable end-to-end through the Mapping Hub pipeline (instantiate &rarr; lift
 * &rarr; infer &rarr; match &rarr; catalog) and must export a
 * {@code complete-manifest.json}. This is the authoritative "all four use cases
 * can be tested" criterion referenced from AGENTS.md.
 */
@DisplayName("Use-case acceptance: all four documented use cases run end-to-end and export a manifest")
class UseCaseAcceptanceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> USE_CASE_FOLDERS = List.of(
        "Pump Asset Health Monitoring",
        "REQ-001-flowmeter-diagnostics",
        "REQ-002-fermentation-skid",
        "REQ-003-hvac-plant");

    @Test
    @DisplayName("Each documented use case uploads, runs the pipeline, and exports complete-manifest.json")
    void allUseCasesExportManifest() {
        List<org.junit.jupiter.api.function.Executable> checks = new ArrayList<>();
        for (String folder : USE_CASE_FOLDERS) {
            checks.add(() -> runUseCase(folder));
        }
        assertAll(checks);
    }

    private void runUseCase(String folder) throws Exception {
        Path dir = Paths.get("docs", "usecases", folder);
        assertTrue(Files.isDirectory(dir), "missing use-case directory: " + dir);

        MappingHubService service = buildService();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase();
                    return n.endsWith(".json") || n.endsWith(".xml")
                        || n.endsWith(".aml") || n.endsWith(".ttl");
                })
                .forEach(files::add);
        }
        assertFalse(files.isEmpty(), "no input files found under " + dir);

        for (Path file : files) {
            byte[] content = Files.readAllBytes(file);
            service.processUpload(file.getFileName().toString(), content);
        }

        PipelineResult result = service.reRunPipeline();
        assertNotNull(result, folder + ": pipeline returned null result");

        String manifest = service.buildManifest();
        assertNotNull(manifest, folder + ": buildManifest() returned null");
        JsonNode root = MAPPER.readTree(manifest);
        assertTrue(root.has("coveragePercent"),
            folder + ": manifest missing coveragePercent");
        double coverage = root.get("coveragePercent").asDouble();
        assertTrue(coverage >= 0.0 && coverage <= 100.0,
            folder + ": coveragePercent out of range: " + coverage);

        if (folder.equals("Pump Asset Health Monitoring")) {
            assertPumpHealthExpectations(result, root);
        }
    }

    private void assertPumpHealthExpectations(PipelineResult result, JsonNode root) {
        assertFalse(result.matches().isEmpty(),
            "Pump Health requirement must match a service once the ABox is loaded");
        boolean matchedNe107 = result.matches().stream()
            .map(CapabilityMatch::skillId)
            .anyMatch(s -> s != null && s.contains("NE107"));
        assertTrue(matchedNe107,
            "the Pump Health requirement must match the NE107 Asset Health Service");
        assertEquals("Pump Asset Condition Monitoring",
            root.get("entities").get("UseCaseRequirement").get(0).get("name").asText());
        double coverage = root.get("coveragePercent").asDouble();
        assertTrue(coverage >= 75.0, "Pump coverage must reflect ABox supply, was " + coverage + "%");
    }

    private MappingHubService buildService() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        SemanticReasonerService reasoner = new SemanticReasonerService(
            loader, new SparqlQueryLoader(), new RDeployService(loader));

        MatchingService matchingService = new MatchingService(new OntologyDataService());
        RecommendationService recommendationService = new RecommendationService();
        ManifestExportService manifestExportService =
            new ManifestExportService(matchingService, recommendationService);

        return new MappingHubService(
            new CanonicalParserRegistry(java.util.List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser(),
                new OpcUaNodeSetXmlParser(), new AutomationMlParser(),
                new AboxTtlParser(new OntologyDataService()), new RequirementTtlParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            matchingService,
            new SemanticMatchService(new SparqlQueryLoader()),
            recommendationService,
            manifestExportService,
            new MappingHubSession(),
            null,
            null,
            null,
            new PipelineStageObserver(ObservationRegistry.NOOP,
                new PipelineAuditPublisher(null), new PipelineMetrics(new SimpleMeterRegistry())),
            new PipelineTracingContext(ObservationRegistry.NOOP),
            new PipelineAuditPublisher(null),
            new PipelineMetrics(new SimpleMeterRegistry()),
            null,
            reasoner, null);
    }
}
