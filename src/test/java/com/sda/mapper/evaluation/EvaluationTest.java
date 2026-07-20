package com.sda.mapper.evaluation;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.parser.MTPAmlParser;
import com.sda.mapper.parser.ParseResult;
import com.sda.mapper.parser.UaServerDatasheetParser;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Evaluation Dataset — All Three Use Cases")
class EvaluationTest {

    private static SemanticReasonerService reasoner;
    private static OntologyModelLoader loader;
    private static Model model;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        model = loader.getModel();
        var queryLoader = new SparqlQueryLoader();
        var rDeployService = new RDeployService(loader);
        reasoner = new SemanticReasonerService(loader, queryLoader, rDeployService);
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
    @DisplayName("REQ-001 match should populate connector and deploymentContext")
    void shouldPopulateConnectorAndDeploymentContext() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched());
        assertNotNull(result.connector(), "Connector should not be null");
        assertNotNull(result.deploymentContext(), "DeploymentContext should not be null");
        assertTrue(result.connector().contains("FT101"),
            "Connector label should reference FT101");
        assertTrue(result.deploymentContext().contains("On-premise"),
            "DeploymentContext label should reference On-premise");
    }

    @Test
    @DisplayName("REQ-002 match should populate connector and deploymentContext")
    void shouldPopulateConnectorAndDeploymentContextReq002() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-002");
        assertTrue(result.matched());
        assertNotNull(result.connector(), "Connector should not be null");
        assertNotNull(result.deploymentContext(), "DeploymentContext should not be null");
    }

    @Test
    @DisplayName("REQ-003 match should populate connector and deploymentContext")
    void shouldPopulateConnectorAndDeploymentContextReq003() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-003");
        assertTrue(result.matched());
        assertNotNull(result.connector(), "Connector should not be null");
        assertNotNull(result.deploymentContext(), "DeploymentContext should not be null");
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
    @DisplayName("Mode B with SIL filter should match FermentationSkid")
    void shouldMatchModeBWithSilFilter() {
        Map<String, String> params = new HashMap<>();
        params.put("protocol", "PROFINET");
        params.put("accessMode", "read-write");
        params.put("safetyIntegrityLevel", "SIL-2");
        params.put("determinismClass", "HardRealTime");
        ServiceMatchResult result = reasoner.matchInline(params);
        assertTrue(result.matched(), "Mode B with PROFINET + SIL-2 should match");
        assertEquals("Fermentation Skid Service", result.serviceName());
    }

    @Test
    @DisplayName("Mode B with networkZone filter should match HVACTempAnalysis")
    void shouldMatchModeBWithNetworkZoneFilter() {
        Map<String, String> params = new HashMap<>();
        params.put("protocol", "OPC-UA");
        params.put("accessMode", "read-only");
        params.put("networkZone", "OT-Zone-3");
        ServiceMatchResult result = reasoner.matchInline(params);
        assertTrue(result.matched(), "Mode B with OPC-UA + OT-Zone-3 should match");
        assertEquals("HVAC Temperature Analysis Service", result.serviceName());
    }

    @Test
    @DisplayName("Mode B with wrong SIL should not match")
    void shouldNotMatchModeBWithWrongSil() {
        Map<String, String> params = new HashMap<>();
        params.put("protocol", "PROFINET");
        params.put("accessMode", "read-write");
        params.put("safetyIntegrityLevel", "SIL-3");
        ServiceMatchResult result = reasoner.matchInline(params);
        assertFalse(result.matched(), "Mode B with wrong SIL should not match");
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
    @DisplayName("Parse MTP AML should produce Jena Model with triples")
    void shouldParseMtpAmlWithModel() {
        MTPAmlParser parser = new MTPAmlParser();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid-complete.xml")) {
            assertNotNull(input, "Test resource req002-mtp-fermentation-skid-complete.xml must exist");
            ParseResult result = parser.parse(input);
            assertNotNull(result);
            assertEquals("MTP-AML", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
            assertNotNull(result.model(), "Model should not be null");
            assertTrue(result.model().size() > 0, "Model should contain triples");
            assertTrue(result.model().containsResource(
                result.model().createResource("http://www.example.org/sda#Service")),
                "Model should contain Service resources");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Parse OPC UA datasheet should produce Jena Model with triples")
    void shouldParseOpcUaDatasheetWithModel() {
        UaServerDatasheetParser parser = new UaServerDatasheetParser();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req003-uaserver-ahu01-complete.json")) {
            assertNotNull(input, "Test resource req003-uaserver-ahu01-complete.json must exist");
            ParseResult result = parser.parse(input);
            assertNotNull(result);
            assertEquals("OPC-UA-Datasheet", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
            assertNotNull(result.model(), "Model should not be null");
            assertTrue(result.model().size() > 0, "Model should contain triples");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Parse MTP AML fermentation skid complete should produce valid triples")
    void shouldParseMtpAmlFermentationSkidComplete() {
        MTPAmlParser parser = new MTPAmlParser();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("examples/req002-mtp-fermentation-skid-complete.xml")) {
            assertNotNull(input, "Test resource req002-mtp-fermentation-skid-complete.xml must exist");
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
                .getResourceAsStream("examples/req003-uaserver-ahu01-complete.json")) {
            assertNotNull(input, "Test resource req003-uaserver-ahu01-complete.json must exist");
            ParseResult result = parser.parse(input);
            assertNotNull(result);
            assertEquals("OPC-UA-Datasheet", result.fileType());
            assertTrue(result.tripleCount() > 0, "Should produce at least one triple");
        } catch (Exception e) {
            fail("Should not throw: " + e.getMessage());
        }
    }
}
