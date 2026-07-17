package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RDeployService — SWRL R-Deploy rule equivalent")
class RDeployServiceTest {

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";

    private static OntologyModelLoader loader;
    private static RDeployService service;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        service = new RDeployService(loader);
    }

    @Test
    @DisplayName("Should assert boundService triple on requirement after deploy")
    void shouldAssertBoundService() {
        service.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        Model model = loader.getModel();
        Resource req = model.createResource(INST_NS + "REQ-001");
        Property boundService = model.createProperty(SDA_NS + "boundService");

        assertTrue(req.hasProperty(boundService),
            "REQ-001 should have boundService after R-Deploy");
        Resource serviceRes = req.getPropertyResourceValue(boundService);
        assertNotNull(serviceRes, "boundService should point to a resource");
    }

    @Test
    @DisplayName("Should assert boundBinding triple on requirement after deploy")
    void shouldAssertBoundBinding() {
        service.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        Model model = loader.getModel();
        Resource req = model.createResource(INST_NS + "REQ-001");
        Property boundBinding = model.createProperty(SDA_NS + "boundBinding");

        assertTrue(req.hasProperty(boundBinding),
            "REQ-001 should have boundBinding after R-Deploy");
    }

    @Test
    @DisplayName("Should assert boundConnector triple on requirement after deploy")
    void shouldAssertBoundConnector() {
        service.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        Model model = loader.getModel();
        Resource req = model.createResource(INST_NS + "REQ-001");
        Property boundConnector = model.createProperty(SDA_NS + "boundConnector");

        assertTrue(req.hasProperty(boundConnector),
            "REQ-001 should have boundConnector after R-Deploy");
    }

    @Test
    @DisplayName("Should assert boundContext triple on requirement after deploy")
    void shouldAssertBoundContext() {
        service.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        Model model = loader.getModel();
        Resource req = model.createResource(INST_NS + "REQ-001");
        Property boundContext = model.createProperty(SDA_NS + "boundContext");

        assertTrue(req.hasProperty(boundContext),
            "REQ-001 should have boundContext after R-Deploy");
    }

    @Test
    @DisplayName("Should assert matchedCandidate triple on requirement after deploy")
    void shouldAssertMatchedCandidate() {
        service.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        Model model = loader.getModel();
        Resource req = model.createResource(INST_NS + "REQ-001");
        Property matchedCandidate = model.createProperty(SDA_NS + "matchedCandidate");

        assertTrue(req.hasProperty(matchedCandidate),
            "REQ-001 should have matchedCandidate after R-Deploy");
    }

    @Test
    @DisplayName("Should transition lifecycleStatus from Defined to Matched")
    void shouldTransitionLifecycleToMatched() {
        service.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        String status = service.getLifecycleStatus("REQ-001");
        assertEquals("Matched", status,
            "lifecycleStatus should be 'Matched' after R-Deploy");
    }

    @Test
    @DisplayName("Should report isMatched=true after deploy")
    void shouldReportIsMatched() {
        service.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        assertTrue(service.isMatched("REQ-001"),
            "isMatched should return true after R-Deploy");
    }

    @Test
    @DisplayName("Should report lifecycleStatus=Defined before deploy")
    void shouldReportDefinedBeforeDeploy() {
        String status = service.getLifecycleStatus("REQ-002");
        assertEquals("Defined", status,
            "lifecycleStatus should be 'Defined' before R-Deploy");
    }

    @Test
    @DisplayName("Should report isMatched=false for unmatched requirement")
    void shouldReportNotMatchedForUnmatched() {
        assertFalse(service.isMatched("REQ-002"),
            "isMatched should return false for unmatched requirement");
    }

    @Test
    @DisplayName("Should handle null service name gracefully")
    void shouldHandleNullServiceName() {
        assertDoesNotThrow(() ->
            service.execute("REQ-001", null, null, null, null),
            "Should not throw with null arguments");
    }

    @Test
    @DisplayName("Should handle nonexistent requirement gracefully")
    void shouldHandleNonexistentRequirement() {
        assertDoesNotThrow(() ->
            service.execute("NONEXISTENT", "Some Service",
                "Some Binding", "Some Connector", "Some Deploy"),
            "Should not throw for nonexistent requirement");
    }
}
