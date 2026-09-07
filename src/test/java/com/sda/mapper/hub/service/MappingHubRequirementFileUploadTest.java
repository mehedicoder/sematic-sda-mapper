package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.service.PipelineResult;
import com.sda.mapper.hub.parser.AasModelParser;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.MtpModelParser;
import com.sda.mapper.hub.parser.OpcUaModelParser;
import com.sda.mapper.hub.parser.RequirementTtlParser;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.observability.PipelineAuditPublisher;
import com.sda.mapper.observability.PipelineMetrics;
import com.sda.mapper.observability.PipelineStageObserver;
import com.sda.mapper.observability.PipelineTracingContext;
import com.sda.mapper.observability.ReasoningTracePublisher;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.ReasoningTreeBuilder;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.service.OntologyDataService;
import com.sda.mapper.support.DemoAboxBuilder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Mapping Hub requirement file upload")
class MappingHubRequirementFileUploadTest {

    private static final String REQUIREMENT_TTL = """
        @prefix rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix sda:  <http://sda-platform.org/ontology#> .
        @prefix :     <http://sda-platform.org/instance#> .

        :REQ-MY-001 a sda:UseCaseRequirement ;
            rdfs:label "Flowmeter Diagnostics Requirement" ;
            sda:requirementID "REQ-MY-001" ;
            sda:requirementName "Flowmeter Diagnostics Requirement" ;
            sda:requirementDescription "Match a flowmeter diagnostics service." ;
            sda:mapsToCapability :CAP-MY-FlowDiagnostics .

        :CAP-MY-FlowDiagnostics a sda:RequiredCapability ;
            rdfs:label "Flowmeter Diagnostics (Required)" ;
            sda:capabilityID "CAP-MY-FLOW-DIAG-001" ;
            sda:capabilityName "Flowmeter Diagnostics" ;
            sda:capabilityType "FlowDiagnostics" .
        """;

    private MappingHubService buildService() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        SemanticReasonerService reasoner = new SemanticReasonerService(
            loader, new SparqlQueryLoader(), new RDeployService(loader));

        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        ManifestExportService manifestExportService =
            new ManifestExportService(matchingService, recommendationService);

        return new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser(),
                new RequirementTtlParser())),
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
            reasoner, null,
            new ReasoningTreeBuilder(),
            new ReasoningTracePublisher());
    }

    @Test
    @DisplayName("Should ingest an uploaded requirement file and match it against the Service Catalogue")
    void shouldMatchUploadedRequirementAgainstCatalogue() {
        MappingHubService service = buildService();
        byte[] content = REQUIREMENT_TTL.getBytes(StandardCharsets.UTF_8);

        PipelineResult result = service.processUpload("req-flow.ttl", content);

        assertFalse(service.getSession().getModel().requirements().isEmpty(),
            "uploaded requirement must enter the session model");
        assertEquals(1, service.getSession().getModel().requiredCapabilities().size());
        assertEquals("FlowDiagnostics",
            service.getSession().getModel().requiredCapabilities().get(0).functionClass());

        boolean matched = result.matches().stream()
            .anyMatch(m -> m.serviceName() != null
                && m.serviceName().contains("Flowmeter Diagnostics"));
        assertTrue(matched,
            "uploaded FlowDiagnostics requirement must match the Flowmeter Diagnostics Service");
    }

    @Test
    @DisplayName("Should embed the requirement match in the exported deployment manifest")
    void shouldEmbedRequirementMatchInManifest() {
        MappingHubService service = buildService();
        service.processUpload("req-flow.ttl", REQUIREMENT_TTL.getBytes(StandardCharsets.UTF_8));

        String manifest = service.buildManifest();
        assertTrue(manifest.contains("Flowmeter Diagnostics"),
            "deployment manifest must include the matched service for the uploaded requirement");
        assertTrue(manifest.contains("deploymentManifest"),
            "manifest must expose the deploymentManifest tuple");
    }
}
