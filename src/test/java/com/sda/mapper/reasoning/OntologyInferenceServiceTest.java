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
        loader.setFilePath("ontology/css-sda-unified.ttl");
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
    @DisplayName("Should not overwrite existing explicit OfferedCapability triples")
    void shouldNotOverwriteExplicitTriples() {
        Model baseModel = loader.getModel();
        Model inferredModel = service.getInferredModel();

        String instNs = "http://www.example.org/instance#";
        String sdaNs = "https://example.org/ontology/sda#";

        // OC-ACM-Flowmeter is an explicit OfferedCapability — must still exist
        assertTrue(inferredModel.contains(
            inferredModel.createResource(instNs + "OC-ACM-Flowmeter"),
            RDF.type,
            inferredModel.createResource(sdaNs + "OfferedCapability")),
            "Explicit OfferedCapability OC-ACM-Flowmeter must still exist after inference");

        // Its capabilityName must be preserved
        var capNameProp = inferredModel.createProperty(sdaNs + "capabilityName");
        var capNameStmt = inferredModel.getResource(instNs + "OC-ACM-Flowmeter")
            .getProperty(capNameProp);
        assertNotNull(capNameStmt, "OC-ACM-Flowmeter must retain capabilityName property");
        assertEquals("AssetConditionMonitoring", capNameStmt.getString());
    }

    @Test
    @DisplayName("Should infer capabilities with correct engineering units from Skill parameters")
    void shouldInferCapabilitiesWithCorrectEngineeringUnits() {
        List<InferredCapability> caps = service.getInferredCapabilities();

        boolean hasLPerMin = caps.stream()
            .anyMatch(c -> "l/min".equals(c.engineeringUnit()));
        boolean hasCelsius = caps.stream()
            .anyMatch(c -> "°C".equals(c.engineeringUnit()));
        boolean hasMlPerMin = caps.stream()
            .anyMatch(c -> "mL/min".equals(c.engineeringUnit()));

        assertTrue(hasLPerMin, "Should infer capability with engineering unit l/min");
        assertTrue(hasCelsius, "Should infer capability with engineering unit °C");
        assertTrue(hasMlPerMin, "Should infer capability with engineering unit mL/min");
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
