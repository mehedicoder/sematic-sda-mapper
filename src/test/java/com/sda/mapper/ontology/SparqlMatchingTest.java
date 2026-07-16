package com.sda.mapper.ontology;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("SPARQL Matching Tests — All Three Use Cases")
class SparqlMatchingTest {

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";
    private static Model model;

    @BeforeAll
    static void loadOntology() throws IOException {
        ClassPathResource resource = new ClassPathResource("ontology/css-sda-small.ttl");
        assertTrue(resource.exists(), "Ontology file must exist");
        model = ModelFactory.createDefaultModel();
        try (InputStream in = resource.getInputStream()) {
            model.read(in, null, "TTL");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // USE CASE A (MVP): Flowmeter NE 107
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match REQ-001 to SVC-FlowmeterDiagnostics (R1-R3 satisfied)")
    void shouldMatchReq001ToFlowmeterDiagnostics() throws IOException {
        String queryStr = loadQuery("queries/match-service.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        assertFalse(results.isEmpty(),
            "Query must return at least one result for REQ-001");

        QuerySolution sol = results.getFirst();
        assertEquals(INST_NS + "SVC-FlowmeterDiagnostics",
            sol.getResource("service").getURI(),
            "Must return SVC-FlowmeterDiagnostics for REQ-001");
        assertEquals("OPC-UA", sol.getLiteral("protocol").getString());
        assertEquals("read-only", sol.getLiteral("accessMode").getString());
        assertTrue(sol.getLiteral("recall").getDouble() >= 0.90,
            "Recall must be >= 0.90");
        assertTrue(sol.getLiteral("latency").getInt() <= 60,
            "Latency must be <= 60");
        assertTrue(sol.getLiteral("onPlantNetwork").getBoolean(),
            "onPlantNetwork must be true");
    }

    @Test
    @DisplayName("Should exclude SVC-WrongProtocol (protocol mismatch)")
    void shouldExcludeWrongProtocol() throws IOException {
        String queryStr = loadQuery("queries/match-service.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        for (QuerySolution sol : results) {
            assertNotEquals(INST_NS + "SVC-WrongProtocol",
                sol.getResource("service").getURI(),
                "SVC-WrongProtocol must not appear in results");
        }
    }

    @Test
    @DisplayName("Should exclude SVC-WrongCapability (capability mismatch)")
    void shouldExcludeWrongCapability() throws IOException {
        String queryStr = loadQuery("queries/match-service.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        for (QuerySolution sol : results) {
            assertNotEquals(INST_NS + "SVC-WrongCapability",
                sol.getResource("service").getURI(),
                "SVC-WrongCapability must not appear in results");
        }
    }

    @Test
    @DisplayName("Should exclude SVC-NE107Failure (NE 107 status = Failure)")
    void shouldExcludeNe107Failure() throws IOException {
        String queryStr = loadQuery("queries/match-service.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        for (QuerySolution sol : results) {
            assertNotEquals(INST_NS + "SVC-NE107Failure",
                sol.getResource("service").getURI(),
                "SVC-NE107Failure must not appear in results");
        }
    }

    @Test
    @DisplayName("FILTER NOT EXISTS should exclude services when any device has NE 107 Failure")
    void shouldExcludeWhenDeviceHasNe107Failure() throws IOException {
        var modifiedModel = ModelFactory.createDefaultModel();
        try (var in = new ByteArrayInputStream(loadOntologyBytes())) {
            modifiedModel.read(in, null, "TTL");
        }

        var pumpAsset = modifiedModel.createResource(INST_NS + "pump1Asset");
        var failureStatus = modifiedModel.createResource(INST_NS + "NE107-Failure");
        var prop = modifiedModel.createProperty(SDA_NS + "hasNE107Status");
        // Remove existing statement (pump1Asset hasNE107Status NE107-Good)
        var goodStatus = modifiedModel.createResource(INST_NS + "NE107-Good");
        modifiedModel.remove(modifiedModel.createStatement(pumpAsset, prop, goodStatus));
        // Add failure status
        modifiedModel.add(pumpAsset, prop, failureStatus);

        String queryStr = loadQuery("queries/match-service.rq");
        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qe = QueryExecutionFactory.create(query, modifiedModel)) {
            ResultSet rs = qe.execSelect();
            List<QuerySolution> solutions = new ArrayList<>();
            while (rs.hasNext()) {
                solutions.add(rs.next());
            }
            assertTrue(solutions.isEmpty(),
                "FILTER NOT EXISTS must exclude all results when a device has NE 107 Failure status");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // USE CASE B: Fermentation Skid
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match REQ-002 to SVC-FermentationSkid via PROFINET")
    void shouldMatchReq002ToFermentationSkid() throws IOException {
        String queryStr = loadQuery("queries/match-service-req002.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        assertFalse(results.isEmpty(),
            "Query must return at least one result for REQ-002");

        QuerySolution sol = results.getFirst();
        assertEquals(INST_NS + "SVC-FermentationSkid",
            sol.getResource("service").getURI(),
            "Must return SVC-FermentationSkid for REQ-002");
        assertEquals("PROFINET", sol.getLiteral("protocol").getString());
        assertEquals("read-write", sol.getLiteral("accessMode").getString());
        assertEquals("SIL-2", sol.getLiteral("safetyIntegrityLevel").getString());
        assertEquals("HardRealTime", sol.getLiteral("determinismClass").getString());
    }

    @Test
    @DisplayName("Should exclude service when SIL mismatch (SIL-0 vs SIL-2)")
    void shouldExcludeOnSilMismatch() throws IOException {
        // REQ-002 requires SIL-2, but DEPLOY-OnPrem has SIL-0
        // The query filters by SIL-2, so SVC-FlowmeterDiagnostics should not appear
        String queryStr = loadQuery("queries/match-service-req002.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        for (QuerySolution sol : results) {
            assertNotEquals(INST_NS + "SVC-FlowmeterDiagnostics",
                sol.getResource("service").getURI(),
                "SVC-FlowmeterDiagnostics must not appear (SIL-0 vs SIL-2 mismatch)");
        }
    }

    @Test
    @DisplayName("Should exclude service when determinism class mismatch")
    void shouldExcludeOnDeterminismMismatch() throws IOException {
        // REQ-002 requires HardRealTime, but DEPLOY-OnPrem has SoftRealTime
        String queryStr = loadQuery("queries/match-service-req002.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        for (QuerySolution sol : results) {
            String detClass = sol.getLiteral("determinismClass").getString();
            assertEquals("HardRealTime", detClass,
                "All matched services must have HardRealTime determinism class");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // USE CASE C: HVAC Plant
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match REQ-003 to SVC-HVACTempAnalysis")
    void shouldMatchReq003ToHvacTempAnalysis() throws IOException {
        String queryStr = loadQuery("queries/match-service-req003.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        assertFalse(results.isEmpty(),
            "Query must return at least one result for REQ-003");

        QuerySolution sol = results.getFirst();
        assertEquals(INST_NS + "SVC-HVACTempAnalysis",
            sol.getResource("service").getURI(),
            "Must return SVC-HVACTempAnalysis for REQ-003");
        assertEquals("OPC-UA", sol.getLiteral("protocol").getString());
        assertEquals("read-only", sol.getLiteral("accessMode").getString());
        assertEquals("OT-Zone-3", sol.getLiteral("networkZone").getString());
    }

    @Test
    @DisplayName("Should exclude service when networkZone mismatch")
    void shouldExcludeOnNetworkZoneMismatch() throws IOException {
        // REQ-003 requires OT-Zone-3, but DEPLOY-OnPrem and DEPLOY-Critical have OT-Zone-1
        String queryStr = loadQuery("queries/match-service-req003.rq");
        List<QuerySolution> results = executeQuery(queryStr);

        for (QuerySolution sol : results) {
            assertEquals("OT-Zone-3", sol.getLiteral("networkZone").getString(),
                "All matched services must be in OT-Zone-3");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Cross-Plant Query
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Cross-plant query should find temperature services across plants")
    void shouldFindCrossPlantTemperatureServices() throws IOException {
        String queryStr = loadQuery("queries/cross-plant-temperature.rq");
        List<QuerySolution> results = executeQuery(queryStr);

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

    private List<QuerySolution> executeQuery(String queryStr) {
        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            List<QuerySolution> solutions = new ArrayList<>();
            while (rs.hasNext()) {
                solutions.add(rs.next());
            }
            return solutions;
        }
    }

    private String loadQuery(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertTrue(resource.exists(), "SPARQL query file must exist: " + path);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }

    private byte[] loadOntologyBytes() throws IOException {
        ClassPathResource resource = new ClassPathResource("ontology/css-sda-small.ttl");
        assertTrue(resource.exists(), "Ontology file must exist");
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
