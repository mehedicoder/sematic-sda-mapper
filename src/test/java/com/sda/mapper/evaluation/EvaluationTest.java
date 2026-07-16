package com.sda.mapper.evaluation;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.parser.MTPAmlParser;
import com.sda.mapper.parser.ParseResult;
import com.sda.mapper.parser.UaServerDatasheetParser;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Evaluation Dataset — All Three Use Cases")
class EvaluationTest {

    private static SemanticReasonerService reasoner;
    private static Model model;

    @BeforeAll
    static void setUp() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        model = loader.getModel();
        var queryLoader = new SparqlQueryLoader();
        reasoner = new SemanticReasonerService(loader, queryLoader);
    }

    @Test
    @DisplayName("REQ-001 should match SVC-FlowmeterDiagnostics (Use Case A)")
    void shouldMatchReq001ToFlowmeterDiagnostics() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched(), "REQ-001 should match a service");
        assertEquals("Flowmeter Diagnostics Service", result.serviceName());
        assertEquals("OPC-UA", result.protocol());
        assertEquals("read-only", result.accessMode());
        assertTrue(result.recall() >= 0.90);
    }

    @Test
    @DisplayName("REQ-002 should match SVC-FermentationSkid (Use Case B)")
    void shouldMatchReq002ToFermentationSkid() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-002");
        assertTrue(result.matched(), "REQ-002 should match a service");
        assertEquals("Fermentation Skid Service", result.serviceName());
    }

    @Test
    @DisplayName("REQ-003 should match SVC-HVACTempAnalysis (Use Case C)")
    void shouldMatchReq003ToHvacTempAnalysis() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-003");
        assertTrue(result.matched(), "REQ-003 should match a service");
        assertEquals("HVAC Temperature Analysis Service", result.serviceName());
    }

    @Test
    @DisplayName("Cross-plant query should find temperature services")
    void shouldFindCrossPlantTemperatureServices() {
        ClassPathResource resource = new ClassPathResource("queries/cross-plant-temperature.rq");
        assertTrue(resource.exists());

        try (InputStream in = resource.getInputStream()) {
            String queryStr = new String(in.readAllBytes());
            Query query = QueryFactory.create(queryStr);

            try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
                ResultSet rs = qe.execSelect();
                List<String> labels = new ArrayList<>();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.next();
                    labels.add(sol.getLiteral("label").getString());
                }
                assertFalse(labels.isEmpty(),
                    "Cross-plant query should return temperature services");
                assertTrue(labels.stream().anyMatch(l -> l.contains("HVAC")),
                    "Should find HVAC temperature services");
            }
        } catch (Exception e) {
            fail("Cross-plant query failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Parse MTP AML fermentation skid complete should produce valid triples")
    void shouldParseMtpAmlFermentationSkidComplete() {
        MTPAmlParser parser = new MTPAmlParser();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/mtp-fermentation-skid-complete.xml")) {
            assertNotNull(input, "Test resource mtp-fermentation-skid-complete.xml must exist");
            ParseResult result = parser.parse(input);
            assertNotNull(result);
            assertEquals("MTP-AML", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Parse OPC UA datasheet AHU-01 complete should produce valid triples")
    void shouldParseOpcUaDatasheetAhu01Complete() {
        UaServerDatasheetParser parser = new UaServerDatasheetParser();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/uaserver-ahu01-complete.json")) {
            assertNotNull(input, "Test resource uaserver-ahu01-complete.json must exist");
            ParseResult result = parser.parse(input);
            assertNotNull(result);
            assertEquals("OPC-UA-Datasheet", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }
}
