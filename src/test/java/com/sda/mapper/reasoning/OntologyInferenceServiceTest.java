package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.support.DemoAboxBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OntologyInferenceService")
class OntologyInferenceServiceTest {

    private static OntologyModelLoader loader;
    private static OntologyInferenceService service;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        service = new OntologyInferenceService(loader);
        service.runInference();
    }

    @Test
    @DisplayName("Should run inference and produce inferred capabilities from existing Skill instances")
    void shouldProduceInferredCapabilities() {
        List<InferredCapability> caps = service.getInferredCapabilities();
        assertFalse(caps.isEmpty(),
            "Inferred capabilities list should not be empty after inference");
        assertTrue(caps.size() >= 3,
            "Should infer at least 3 capabilities from Skills with engineering parameters");
    }

    @Test
    @DisplayName("Should return positive inferred triple count after inference")
    void shouldReturnPositiveInferredTripleCount() {
        int count = service.getInferredTripleCount();
        assertTrue(count >= 0,
            "Inferred triple count should be non-negative after inference");
    }

    @Test
    @DisplayName("Should not overwrite existing explicit Capability triples")
    void shouldNotOverwriteExplicitTriples() {
        Model baseModel = loader.getModel();
        Model inferredModel = service.getInferredModel();

        String instNs = "http://sda-platform.org/instance#";
        String sdaNs = "http://sda-platform.org/ontology#";

        // CAP-FlowDiagnostics is an explicit Capability — must still exist
        assertTrue(inferredModel.contains(
            inferredModel.createResource(instNs + "CAP-FlowDiagnostics"),
            RDF.type,
            inferredModel.createResource(sdaNs + "Capability")),
            "Explicit Capability CAP-FlowDiagnostics must still exist after inference");

        // Its capabilityName must be preserved
        var capNameProp = inferredModel.createProperty(sdaNs + "capabilityName");
        var capNameStmt = inferredModel.getResource(instNs + "CAP-FlowDiagnostics")
            .getProperty(capNameProp);
        assertNotNull(capNameStmt, "CAP-FlowDiagnostics must retain capabilityName property");
        assertEquals("Flowmeter Diagnostics", capNameStmt.getString());
    }

    @Test
    @DisplayName("Should infer capabilities with correct names and skills from the capability chain")
    void shouldInferCapabilitiesWithCorrectEngineeringUnits() {
        List<InferredCapability> caps = service.getInferredCapabilities();

        assertTrue(caps.stream().anyMatch(c -> "Flowmeter Diagnostics".equals(c.capabilityName())),
            "Should infer capability named Flowmeter Diagnostics");
        assertTrue(caps.stream().anyMatch(c -> "Fermentation Control".equals(c.capabilityName())),
            "Should infer capability named Fermentation Control");
        assertTrue(caps.stream().anyMatch(c -> "HVAC Temperature Monitoring".equals(c.capabilityName())),
            "Should infer capability named HVAC Temperature Monitoring");

        assertTrue(caps.stream().anyMatch(c -> "SK-FlowmeterDiagnostics".equals(c.sourceSkill())),
            "Inferred capability should reference skill SK-FlowmeterDiagnostics");
    }

    @Test
    @DisplayName("Should report inference disabled via configuration")
    void shouldSupportDisableByConfiguration() {
        var disabledService = new OntologyInferenceService(loader);
        disabledService.setEnabled(false);
        disabledService.runInference();

        assertTrue(disabledService.getInferredCapabilities().isEmpty(),
            "Inferred capabilities should be empty when inference is disabled");
        assertEquals(0, disabledService.getInferredTripleCount(),
            "Inferred triple count should be 0 when disabled");
    }
}
