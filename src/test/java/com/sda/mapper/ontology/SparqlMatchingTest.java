package com.sda.mapper.ontology;

import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SPARQL Matching Tests — All Three Use Cases")
class SparqlMatchingTest {

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";

    private static OntologyModelLoader loader;
    private static SemanticReasonerService reasoner;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        var queryLoader = new SparqlQueryLoader();
        var deployService = new RDeployService(loader);
        reasoner = new SemanticReasonerService(loader, queryLoader, deployService);
    }

    // ════════════════════════════════════════════════════════════════════
    // USE CASE A: Flowmeter NE 107 (OPC-UA, read-only)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match SVC-FlowmeterDiagnostics via OPC-UA read-only with SIL-0")
    void shouldMatchFlowmeterDiagnostics() {
        Map<String, String> params = new HashMap<>();
        params.put("protocol", "OPC-UA");
        params.put("accessMode", "read-only");
        params.put("safetyIntegrityLevel", "SIL-0");
        ServiceMatchResult result = reasoner.matchInline(params);

        assertTrue(result.matched(), "Should find a matching service for OPC-UA read-only");
        assertTrue(result.serviceName().contains("Flowmeter Diagnostics"),
            "Matched service must be Flowmeter Diagnostics, got: " + result.serviceName());
        assertEquals("OPC-UA", result.protocol());
        assertEquals("read-only", result.accessMode());
        assertTrue(result.recall() >= 0.90, "Recall must be >= 0.90, got: " + result.recall());
        assertTrue(result.latencyMs() <= 60, "Latency must be <= 60, got: " + result.latencyMs());
    }

    @Test
    @DisplayName("Should not match any service with HTTP protocol")
    void shouldExcludeWrongProtocol() {
        Map<String, String> params = Map.of("protocol", "HTTP", "accessMode", "read-only");
        ServiceMatchResult result = reasoner.matchInline(params);

        assertFalse(result.matched(),
            "No service should match HTTP protocol filter");
    }

    @Test
    @DisplayName("Should exclude SVC-NE107Failure (no isDeployedBy context)")
    void shouldExcludeSVCNE107Failure() {
        Map<String, String> params = Map.of("protocol", "OPC-UA", "accessMode", "read-only");
        ServiceMatchResult result = reasoner.matchInline(params);

        assertTrue(result.matched(), "Should match some service");
        assertNotEquals("NE 107 Failure Service", result.serviceName(),
            "SVC-NE107Failure must not match (no isDeployedBy)");
    }

    @Test
    @DisplayName("FILTER NOT EXISTS should exclude services when any device has NE 107 Failure")
    void shouldExcludeWhenDeviceHasNe107Failure() {
        Model model = loader.getModel();
        var pumpAsset = model.createResource(INST_NS + "pump1Asset");
        var failureStatus = model.createResource(INST_NS + "NE107-Failure");
        var goodStatus = model.createResource(INST_NS + "NE107-Good");
        var prop = model.createProperty(SDA_NS + "hasNE107Status");
        model.remove(model.createStatement(pumpAsset, prop, goodStatus));
        model.add(pumpAsset, prop, failureStatus);

        try {
            Map<String, String> params = Map.of("protocol", "OPC-UA", "accessMode", "read-only");
            ServiceMatchResult result = reasoner.matchInline(params);
            assertFalse(result.matched(),
                "FILTER NOT EXISTS must exclude all results when a device has NE 107 Failure status");
        } finally {
            model.remove(pumpAsset, prop, failureStatus);
            model.add(pumpAsset, prop, goodStatus);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // USE CASE B: Fermentation Skid (PROFINET, read-write, SIL-2, HardRealTime)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match SVC-FermentationSkid via PROFINET read-write with SIL-2 HardRealTime")
    void shouldMatchFermentationSkidViaProfinet() {
        Map<String, String> params = new HashMap<>();
        params.put("protocol", "PROFINET");
        params.put("accessMode", "read-write");
        params.put("safetyIntegrityLevel", "SIL-2");
        params.put("determinismClass", "HardRealTime");
        ServiceMatchResult result = reasoner.matchInline(params);

        assertTrue(result.matched(), "Should find SVC-FermentationSkid for PROFINET read-write SIL-2");
        assertTrue(result.serviceName().contains("Fermentation"),
            "Matched service must be Fermentation Skid, got: " + result.serviceName());
        assertEquals("PROFINET", result.protocol());
        assertEquals("SIL-2", result.safetyIntegrityLevel());
        assertEquals("HardRealTime", result.determinismClass());
    }

    @Test
    @DisplayName("Should exclude SVC-FlowmeterDiagnostics on SIL mismatch (SIL-0 vs SIL-2)")
    void shouldExcludeOnSilMismatch() {
        Map<String, String> params = new HashMap<>();
        params.put("protocol", "OPC-UA");
        params.put("accessMode", "read-only");
        params.put("safetyIntegrityLevel", "SIL-2");
        params.put("determinismClass", "HardRealTime");
        ServiceMatchResult result = reasoner.matchInline(params);

        if (result.matched()) {
            assertNotEquals("Flowmeter Diagnostics Service", result.serviceName(),
                "SVC-FlowmeterDiagnostics (SIL-0) must not match SIL-2 requirement");
        }
    }

    @Test
    @DisplayName("Should return service with HardRealTime when determinism class is required")
    void shouldExcludeOnDeterminismMismatch() {
        Map<String, String> params = new HashMap<>();
        params.put("protocol", "PROFINET");
        params.put("accessMode", "read-write");
        params.put("determinismClass", "HardRealTime");
        ServiceMatchResult result = reasoner.matchInline(params);

        assertTrue(result.matched(), "Should match a service with HardRealTime determinism");
        assertEquals("HardRealTime", result.determinismClass(),
            "All matched services must have HardRealTime determinism class");
    }

    // ════════════════════════════════════════════════════════════════════
    // USE CASE C: HVAC Plant (OT-Zone-3)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match SVC-HVACTempAnalysis via OPC-UA read-only in OT-Zone-3")
    void shouldMatchHvacTempAnalysis() {
        Map<String, String> params = Map.of(
            "protocol", "OPC-UA",
            "accessMode", "read-only",
            "networkZone", "OT-Zone-3"
        );
        ServiceMatchResult result = reasoner.matchInline(params);

        assertTrue(result.matched(), "Should find SVC-HVACTempAnalysis for OT-Zone-3");
        assertTrue(result.serviceName().contains("HVAC"),
            "Matched service must be HVAC Temp Analysis, got: " + result.serviceName());
        assertEquals("OT-Zone-3", result.networkZone());
    }

    @Test
    @DisplayName("All returned services must be in OT-Zone-3 when zone filter is applied")
    void shouldExcludeOnNetworkZoneMismatch() {
        Map<String, String> params = Map.of(
            "protocol", "OPC-UA",
            "accessMode", "read-only",
            "networkZone", "OT-Zone-3"
        );
        ServiceMatchResult result = reasoner.matchInline(params);

        assertTrue(result.matched(), "Should match a service in OT-Zone-3");
        assertEquals("OT-Zone-3", result.networkZone(),
            "All matched services must be in OT-Zone-3");
    }

    // ════════════════════════════════════════════════════════════════════
    // Cross-Plant Query (raw SPARQL)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Cross-plant query should find temperature services across plants")
    void shouldFindCrossPlantTemperatureServices() throws IOException {
        String queryStr = loadQuery("queries/cross-plant-temperature.rq");
        Model model = loader.getModel();
        Query query = QueryFactory.create(queryStr);

        List<QuerySolution> results;
        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            results = new ArrayList<>();
            while (rs.hasNext()) {
                results.add(rs.next());
            }
        }

        assertFalse(results.isEmpty(),
            "Cross-plant query must return at least one temperature service");

        List<String> serviceLabels = new ArrayList<>();
        for (QuerySolution sol : results) {
            serviceLabels.add(sol.getLiteral("label").getString());
        }

        boolean hasHvac = serviceLabels.stream()
            .anyMatch(l -> l.contains("HVAC"));
        boolean hasFermentation = serviceLabels.stream()
            .anyMatch(l -> l.contains("Fermentation") || l.contains("Temperature"));

        assertTrue(hasHvac || hasFermentation,
            "Cross-plant query must find HVAC or fermentation temperature services");
    }

    // ════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════

    private String loadQuery(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertTrue(resource.exists(), "SPARQL query file must exist: " + path);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }
}
