package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.parser.AboxTtlParser;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that a complete SDA use-case package (requirement + service
 * catalogue + asset description + edge-node topology) can be ingested through
 * the mapping hub and produces a semantic capability match.
 */
@DisplayName("Mapping Hub - Pump Asset Health Monitoring use-case package")
class MappingHubAboxUploadTest {

    private byte[] fixture(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Fixture not found: " + path);
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
            reasoner);
    }

    @Test
    @DisplayName("Should detect ABox TTL files as ABOX_TTL")
    void shouldDetectAboxTtl() {
        HubFileTypeDetector detector = new HubFileTypeDetector();
        assertEquals(AssetFileFormat.ABOX_TTL,
            detector.detect("catalogue_services.ttl",
                new ByteArrayInputStream(fixture("/abox/pump-health/catalogue_services.ttl")))
                .orElseThrow());
        assertEquals(AssetFileFormat.ABOX_TTL,
            detector.detect("edge_nodes.ttl",
                new ByteArrayInputStream(fixture("/abox/pump-health/edge_nodes.ttl")))
                .orElseThrow());
    }

    @Test
    @DisplayName("Should merge the use-case ABox into the shared catalogue and match the "
        + "uploaded requirement against the NE107 Asset Health Service")
    void shouldImplementPumpHealthUseCase() {
        MappingHubService service = buildService();

        service.processUpload("catalogue_services.ttl",
            fixture("/abox/pump-health/catalogue_services.ttl"));
        service.processUpload("catalogue_assets.ttl",
            fixture("/abox/pump-health/catalogue_assets.ttl"));
        service.processUpload("edge_nodes.ttl",
            fixture("/abox/pump-health/edge_nodes.ttl"));

        PipelineResult result = service.processUpload(
            "req_pump_health.ttl", fixture("/abox/pump-health/req_pump_health.ttl"));

        assertFalse(result.matches().isEmpty(),
            "uploaded Pump Health requirement must match a service once the ABox is loaded");

        boolean matchedNe107 = result.matches().stream()
            .map(CapabilityMatch::skillId)
            .anyMatch(skill -> skill != null && skill.contains("NE107"));
        assertTrue(matchedNe107,
            "the Pump Health requirement must match the NE107 Asset Health Service");

        CapabilityMatch match = result.matches().stream()
            .filter(m -> m.skillId() != null && m.skillId().contains("NE107"))
            .findFirst()
            .orElseThrow();
        assertTrue(match.offeredCapabilityId() != null
                && match.offeredCapabilityId().contains("AssetConditionMonitoring"),
            "the matched offered capability must be AssetConditionMonitoring");
        assertNotNull(match.skillId());
        assertNotNull(match.endpointUrl());

        // The manifest must carry the real binding endpoint URI, not the
        // binding label, and the deployment zone name (from zoneName), not blank.
        assertEquals("opc.tcp://edge-node-01.plant.local:4840/sda/diagnostics",
            match.endpointUrl());
        assertEquals("OT Production Zone Alpha", match.networkZone());
    }

    @Test
    @DisplayName("Should reflect the uploaded ABox individuals in coverage (Service, Asset, "
        + "Capability, Skill, etc. present) instead of reporting them as MISSING")
    void shouldReflectAboxEntitiesInCoverage() throws Exception {
        MappingHubService service = buildService();

        service.processUpload("catalogue_services.ttl",
            fixture("/abox/pump-health/catalogue_services.ttl"));
        service.processUpload("catalogue_assets.ttl",
            fixture("/abox/pump-health/catalogue_assets.ttl"));
        service.processUpload("edge_nodes.ttl",
            fixture("/abox/pump-health/edge_nodes.ttl"));
        service.processUpload("req_pump_health.ttl",
            fixture("/abox/pump-health/req_pump_health.ttl"));

        String manifest = service.buildManifest();
        com.fasterxml.jackson.databind.JsonNode root =
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(manifest);

        double coveragePercent = root.get("coveragePercent").asDouble();
        assertTrue(coveragePercent >= 75.0,
            "coverage must reflect the ABox supply/asset/edge individuals, was "
                + coveragePercent + "%");

        assertEquals("PRESENT",
            root.get("coverage").get("Service").get("status").asText());
        assertEquals("PRESENT",
            root.get("coverage").get("Asset").get("status").asText());
        assertEquals("PRESENT",
            root.get("coverage").get("Capability").get("status").asText());
        assertEquals("PRESENT",
            root.get("coverage").get("Skill").get("status").asText());

        // The requirement file is a full use-case bundle: the surrounding L1
        // individuals must be lifted too, not reported as MISSING.
        for (String l1 : new String[] {
            "UseCase", "Stakeholder", "UseCaseRequirementParameter",
            "UseCaseRequirementConstraint", "VerificationCriterion"}) {
            assertEquals("PRESENT",
                root.get("coverage").get(l1).get("status").asText(),
                l1 + " must be lifted from the requirement bundle");
        }

        // Requirement parser must read the ontology-canonical :title (not only
        // the legacy :requirementName spelling).
        assertEquals("Pump Asset Condition Monitoring",
            root.get("entities").get("UseCaseRequirement").get(0).get("name").asText());
    }
}
