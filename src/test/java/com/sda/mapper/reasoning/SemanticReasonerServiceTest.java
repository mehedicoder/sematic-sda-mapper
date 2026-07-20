package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SemanticReasonerService SPARQL matching")
class SemanticReasonerServiceTest {

    private static SemanticReasonerService reasoner;
    private static OntologyModelLoader sharedLoader;

    @BeforeAll
    static void setUp() {
        sharedLoader = new OntologyModelLoader();
        sharedLoader.setFilePath("ontology/css-sda-small.ttl");
        sharedLoader.loadOntology();
        var queryLoader = new SparqlQueryLoader();
        var rDeployService = new RDeployService(sharedLoader);
        reasoner = new SemanticReasonerService(sharedLoader, queryLoader, rDeployService);
    }

    // ════════════════════════════════════════════════════════════════════
    // Mode A: matchByRequirementId
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match REQ-001 and return matched=true with correct service name")
    void shouldMatchReq001WithCorrectService() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched(), "REQ-001 should match a service");
        assertEquals("Flowmeter Diagnostics Service", result.serviceName(),
            "REQ-001 should match SVC-FlowmeterDiagnostics");
    }

    @Test
    @DisplayName("Should match REQ-001 and return correct protocol OPC-UA")
    void shouldMatchReq001WithOpcUaProtocol() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched());
        assertEquals("OPC-UA", result.protocol(),
            "REQ-001 match must use OPC-UA protocol");
        assertEquals("read-only", result.accessMode(),
            "REQ-001 match must use read-only access mode");
    }

    @Test
    @DisplayName("Should match REQ-001 and return recall >= 0.90")
    void shouldMatchReq001WithHighRecall() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched());
        assertTrue(result.recall() >= 0.90,
            "REQ-001 match must have recall >= 0.90, was: " + result.recall());
    }

    @Test
    @DisplayName("Should match REQ-002 and return matched=true with SVC-FermentationSkid")
    void shouldMatchReq002WithFermentationSkid() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-002");
        assertTrue(result.matched(), "REQ-002 should match a service");
        assertEquals("Fermentation Skid Service", result.serviceName(),
            "REQ-002 should match SVC-FermentationSkid");
    }

    @Test
    @DisplayName("Should match REQ-003 and return matched=true with SVC-HVACTempAnalysis")
    void shouldMatchReq003WithHvacTempAnalysis() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-003");
        assertTrue(result.matched(), "REQ-003 should match a service");
        assertEquals("HVAC Temperature Analysis Service", result.serviceName(),
            "REQ-003 should match SVC-HVACTempAnalysis");
    }

    @Test
    @DisplayName("Should return matched=false for nonexistent requirement ID")
    void shouldReturnNoMatchForNonexistentId() {
        ServiceMatchResult result = reasoner.matchByRequirementId("NONEXISTENT");
        assertFalse(result.matched(), "NONEXISTENT should not match any service");
        assertNotNull(result.explanation(), "Explanation should be provided for no-match");
    }

    // ════════════════════════════════════════════════════════════════════
    // Mode B: matchInline
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match inline with correct OPC-UA read-only parameters")
    void shouldMatchInlineWithCorrectParameters() {
        Map<String, String> params = Map.of(
            "protocol", "OPC-UA",
            "accessMode", "read-only",
            "minRecall", "0.90",
            "maxLatency", "60",
            "onPlantNetwork", "true"
        );
        ServiceMatchResult result = reasoner.matchInline(params);
        assertTrue(result.matched(), "Inline match with OPC-UA read-only should succeed");
        assertNotNull(result.serviceName(), "Matched service name should not be null");
        assertEquals("OPC-UA", result.protocol());
    }

    @Test
    @DisplayName("Should return no match when inline parameters use wrong protocol")
    void shouldNotMatchInlineWithWrongProtocol() {
        Map<String, String> params = Map.of(
            "protocol", "MQTT",
            "accessMode", "read-only",
            "minRecall", "0.90",
            "maxLatency", "60",
            "onPlantNetwork", "true"
        );
        ServiceMatchResult result = reasoner.matchInline(params);
        assertFalse(result.matched(), "Inline match with MQTT should not match");
    }

    @Test
    @DisplayName("Should return no match for empty parameters")
    void shouldReturnNoMatchForEmptyParameters() {
        ServiceMatchResult result = reasoner.matchInline(Map.of());
        assertFalse(result.matched(), "Empty parameters should not match");
    }

    @Test
    @DisplayName("Should return no match for null parameters")
    void shouldReturnNoMatchForNullParameters() {
        ServiceMatchResult result = reasoner.matchInline(null);
        assertFalse(result.matched(), "Null parameters should not match");
    }

    // ════════════════════════════════════════════════════════════════════
    // Mode B fallback: matchByRequirementId for dynamic requirements
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match dynamic requirement with OPC-UA read-only params via Mode B fallback")
    void shouldMatchDynamicRequirementWithOpcUaParams() {
        Model model = sharedLoader.getModel();
        String reqId = "REQ-TEST-DYN-01";
        String uri = "http://www.example.org/instance#" + reqId;

        Resource req = model.createResource(uri);
        req.addProperty(
            model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
            model.createResource("http://www.example.org/sda#Requirement"));
        req.addProperty(model.createProperty("http://www.example.org/sda#requirementID"), reqId);
        req.addProperty(model.createProperty("http://www.example.org/sda#protocol"), "OPC-UA");
        req.addProperty(model.createProperty("http://www.example.org/sda#accessMode"), "read-only");

        ServiceMatchResult result = reasoner.matchByRequirementId(reqId);
        assertTrue(result.matched(),
            "Dynamic requirement with OPC-UA read-only should match via Mode B fallback");
        assertEquals("OPC-UA", result.protocol());
        assertEquals(reqId, result.requirementId());
    }

    @Test
    @DisplayName("Should not match dynamic requirement with MQTT protocol")
    void shouldNotMatchDynamicRequirementWithMqtt() {
        Model model = sharedLoader.getModel();
        String reqId = "REQ-TEST-DYN-02";
        String uri = "http://www.example.org/instance#" + reqId;

        Resource req = model.createResource(uri);
        req.addProperty(
            model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
            model.createResource("http://www.example.org/sda#Requirement"));
        req.addProperty(model.createProperty("http://www.example.org/sda#requirementID"), reqId);
        req.addProperty(model.createProperty("http://www.example.org/sda#protocol"), "MQTT");
        req.addProperty(model.createProperty("http://www.example.org/sda#accessMode"), "read-only");

        ServiceMatchResult result = reasoner.matchByRequirementId(reqId);
        assertFalse(result.matched(),
            "Dynamic requirement with MQTT should not match any service");
    }

    @Test
    @DisplayName("Should return no match for dynamic requirement with no parameters")
    void shouldNotMatchDynamicRequirementWithNoParams() {
        Model model = sharedLoader.getModel();
        String reqId = "REQ-TEST-DYN-03";
        String uri = "http://www.example.org/instance#" + reqId;

        Resource req = model.createResource(uri);
        req.addProperty(
            model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
            model.createResource("http://www.example.org/sda#Requirement"));
        req.addProperty(model.createProperty("http://www.example.org/sda#requirementID"), reqId);

        ServiceMatchResult result = reasoner.matchByRequirementId(reqId);
        assertFalse(result.matched(),
            "Dynamic requirement with no parameters should not match");
    }
}
