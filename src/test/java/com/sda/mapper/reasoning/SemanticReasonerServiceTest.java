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

    @Test
    @DisplayName("Should match dynamic requirement with OPC-UA read-only params")
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
            "Dynamic requirement with OPC-UA read-only should match");
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

    @Test
    @DisplayName("Should return no match for null requirement ID")
    void shouldReturnNoMatchForNullId() {
        ServiceMatchResult result = reasoner.matchByRequirementId(null);
        assertFalse(result.matched());
    }

    @Test
    @DisplayName("Should return no match for blank requirement ID")
    void shouldReturnNoMatchForBlankId() {
        ServiceMatchResult result = reasoner.matchByRequirementId("  ");
        assertFalse(result.matched());
    }

    @Test
    @DisplayName("Should return no match for nonexistent requirement ID")
    void shouldReturnNoMatchForNonexistentId() {
        ServiceMatchResult result = reasoner.matchByRequirementId("NONEXISTENT");
        assertFalse(result.matched(), "NONEXISTENT should not match any service");
        assertNotNull(result.explanation(), "Explanation should be provided for no-match");
    }
}
