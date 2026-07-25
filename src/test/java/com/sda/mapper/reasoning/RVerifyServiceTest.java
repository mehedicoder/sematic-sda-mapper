package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RVerifyService — SWRL R-Verify rule equivalent")
class RVerifyServiceTest {

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";

    private static OntologyModelLoader loader;
    private static RDeployService deployService;
    private static RVerifyService verifyService;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        deployService = new RDeployService(loader);
        verifyService = new RVerifyService(loader);
    }

    @BeforeEach
    void resetLifecycle() {
        loader.loadOntology();
        createTestRequirements(loader.getModel());
    }

    private void createTestRequirements(Model model) {
        Resource reqTest001 = model.createResource(INST_NS + "REQ-TEST-001");
        reqTest001.addProperty(RDF.type, model.createResource(SDA_NS + "Requirement"));
        reqTest001.addProperty(model.createProperty(SDA_NS + "requirementID"), "REQ-TEST-001");

        Resource recallCheck = model.createResource(INST_NS + "RecallCheck");
        recallCheck.addProperty(RDFS.label, "Recall Check");
        recallCheck.addProperty(model.createProperty(SDA_NS + "criterionType"), "performance");
        recallCheck.addProperty(model.createProperty(SDA_NS + "criterionMetric"), "reportedRecall");
        recallCheck.addProperty(model.createProperty(SDA_NS + "criterionOperator"), ">=");
        recallCheck.addProperty(model.createProperty(SDA_NS + "criterionThreshold"), "0.90");

        Resource latencyCheck = model.createResource(INST_NS + "LatencyCheck");
        latencyCheck.addProperty(RDFS.label, "Latency Check");
        latencyCheck.addProperty(model.createProperty(SDA_NS + "criterionType"), "performance");
        latencyCheck.addProperty(model.createProperty(SDA_NS + "criterionMetric"), "reportedLatency");
        latencyCheck.addProperty(model.createProperty(SDA_NS + "criterionOperator"), "<=");
        latencyCheck.addProperty(model.createProperty(SDA_NS + "criterionThreshold"), "60");

        reqTest001.addProperty(model.createProperty(SDA_NS + "verifiedBy"), recallCheck);
        reqTest001.addProperty(model.createProperty(SDA_NS + "verifiedBy"), latencyCheck);

        Resource reqTest003 = model.createResource(INST_NS + "REQ-TEST-003");
        reqTest003.addProperty(RDF.type, model.createResource(SDA_NS + "Requirement"));
        reqTest003.addProperty(model.createProperty(SDA_NS + "requirementID"), "REQ-TEST-003");
    }

    @Test
    @DisplayName("Should return not-verified when requirement has no matchedCandidate")
    void shouldReturnNotVerifiedWhenNotMatched() {
        RVerifyService.VerifyResult result = verifyService.execute("REQ-USER-001");
        assertFalse(result.verified(),
            "Should not verify a requirement that hasn't been matched");
        assertTrue(result.summary().contains("Not matched"),
            "Summary should indicate not matched");
    }

    @Test
    @DisplayName("Should verify REQ-TEST-001 after R-Deploy when recall=0.94 >= 0.90 and latency=45 <= 60")
    void shouldVerifyReqTest001AfterDeploy() {
        deployService.execute("REQ-TEST-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-TEST-001");
        assertTrue(result.verified(),
            "REQ-TEST-001 should verify: SVC-FlowmeterDiagnostics has recall=0.94 >= 0.90 and latency=45 <= 60");
        assertEquals(2, result.criteria().size(),
            "REQ-TEST-001 should have 2 verification criteria (RecallCheck + LatencyCheck)");
    }

    @Test
    @DisplayName("Should transition lifecycleStatus to Verified after successful verification")
    void shouldTransitionToVerified() {
        deployService.execute("REQ-TEST-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        verifyService.execute("REQ-TEST-001");
        String status = deployService.getLifecycleStatus("REQ-TEST-001");
        assertEquals("Verified", status,
            "lifecycleStatus should be 'Verified' after successful R-Verify");
    }

    @Test
    @DisplayName("Should pass RecallCheck criterion for SVC-FlowmeterDiagnostics")
    void shouldPassRecallCheck() {
        deployService.execute("REQ-TEST-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-TEST-001");
        RVerifyService.CriterionResult recallCheck = result.criteria().stream()
            .filter(c -> c.name().equals("Recall Check"))
            .findFirst()
            .orElse(null);

        assertNotNull(recallCheck, "Recall Check criterion should exist");
        assertTrue(recallCheck.passed(),
            "RecallCheck should pass: recall=0.94 >= threshold=0.90");
    }

    @Test
    @DisplayName("Should pass LatencyCheck criterion for SVC-FlowmeterDiagnostics")
    void shouldPassLatencyCheck() {
        deployService.execute("REQ-TEST-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-TEST-001");
        RVerifyService.CriterionResult latencyCheck = result.criteria().stream()
            .filter(c -> c.name().equals("Latency Check"))
            .findFirst()
            .orElse(null);

        assertNotNull(latencyCheck, "Latency Check criterion should exist");
        assertTrue(latencyCheck.passed(),
            "LatencyCheck should pass: latency=45 <= threshold=60");
    }

    @Test
    @DisplayName("Should include detail string in criterion results")
    void shouldIncludeDetailInCriterionResults() {
        deployService.execute("REQ-TEST-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-TEST-001");
        for (RVerifyService.CriterionResult cr : result.criteria()) {
            assertNotNull(cr.detail(), "Detail should not be null for " + cr.name());
            assertFalse(cr.detail().isBlank(), "Detail should not be blank for " + cr.name());
        }
    }

    @Test
    @DisplayName("Should handle requirement with no verification criteria")
    void shouldHandleNoCriteria() {
        deployService.execute("REQ-TEST-003", "HVAC Temperature Analysis Service",
            "BACnet binding for AHU-01", "AHU-01 OPC-UA gateway connector",
            "OT Zone 3 deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-TEST-003");
        assertFalse(result.verified(),
            "REQ-TEST-003 has no verifiedBy criteria — should return not verified");
    }
}
