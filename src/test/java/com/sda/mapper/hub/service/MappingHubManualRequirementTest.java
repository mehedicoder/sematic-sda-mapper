package com.sda.mapper.hub.service;

import com.sda.mapper.hub.parser.AasModelParser;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.MtpModelParser;
import com.sda.mapper.hub.parser.OpcUaModelParser;
import com.sda.mapper.observability.PipelineAuditPublisher;
import com.sda.mapper.observability.PipelineMetrics;
import com.sda.mapper.observability.PipelineStageObserver;
import com.sda.mapper.observability.PipelineTracingContext;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.service.OntologyDataService;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.PipelineResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Mapping Hub manual requirement matching")
class MappingHubManualRequirementTest {

    private MappingHubService buildHub() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.setFilePaths(List.of(
            "ontology/sda_ontology_v2_structured.ttl",
            "ontology/seed-services.ttl",
            "ontology/seed-requirements.ttl"));
        loader.loadOntology();
        RDeployService rDeploy = new RDeployService(loader);
        SemanticReasonerService reasoner =
            new SemanticReasonerService(loader, new SparqlQueryLoader(), rDeploy);

        var metrics = new PipelineMetrics(new SimpleMeterRegistry());
        var stageObserver = new PipelineStageObserver(
            ObservationRegistry.NOOP, new PipelineAuditPublisher(null), metrics);
        var tracingContext = new PipelineTracingContext(ObservationRegistry.NOOP);

        return new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            new MatchingService(new OntologyDataService()),
            new SemanticMatchService(new SparqlQueryLoader(),
                new com.sda.mapper.observability.TracedSparqlExecutor(metrics)),
            new RecommendationService(),
            new ManifestExportService(
                new MatchingService(new OntologyDataService()), new RecommendationService()),
            new MappingHubSession(),
            null,
            null,
            null,
            stageObserver,
            tracingContext,
            new PipelineAuditPublisher(null),
            metrics,
            null,
            reasoner);
    }

    @Test
    @DisplayName("Should match a manually entered requirement against the catalogue by capability type")
    void shouldMatchManualRequirementByCapabilityType() {
        MappingHubService hub = buildHub();
        PipelineResult result = hub.addManualRequirement(
            "REQ-005", "Manual flowmeter diagnostics", "FlowDiagnostics");

        boolean matched = result.matches().stream().anyMatch(m ->
            "REQ-005".equals(m.requiredCapabilityId())
                && m.serviceName() != null
                && m.serviceName().contains("Flowmeter"));
        assertTrue(matched,
            "Manual requirement REQ-005 (FlowDiagnostics) must match the Flowmeter Diagnostics Service");
    }

    @Test
    @DisplayName("Should not match a manual requirement whose capability type is not offered")
    void shouldNotMatchUnknownCapabilityType() {
        MappingHubService hub = buildHub();
        PipelineResult result = hub.addManualRequirement(
            "REQ-006", "Unknown capability", "NoSuchCapabilityType");

        boolean matched = result.matches().stream()
            .anyMatch(m -> "REQ-006".equals(m.requiredCapabilityId()));
        assertFalse(matched,
            "Manual requirement with an unoffered capability type must not match");
    }
}
