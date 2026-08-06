package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.support.DemoAboxBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RVerifyService — SWRL R-Verify rule equivalent")
class RVerifyServiceTest {

    private static OntologyModelLoader loader;
    private static RDeployService deployService;
    private static RVerifyService verifyService;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        deployService = new RDeployService(loader);
        verifyService = new RVerifyService(loader);
    }

    @BeforeEach
    void resetLifecycle() {
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
    }

    @Test
    @DisplayName("Should return not-verified when requirement has no matchedCandidate")
    void shouldReturnNotVerifiedWhenNotMatched() {
        RVerifyService.VerifyResult result = verifyService.execute("REQ-002");
        assertFalse(result.verified(),
            "Should not verify a requirement that hasn't been matched");
        assertTrue(result.summary().contains("Not matched"),
            "Summary should indicate not matched");
    }

    @Test
    @DisplayName("Should verify REQ-001 after R-Deploy when recall=0.94 >= 0.90 and latency=45 <= 60")
    void shouldVerifyReq001AfterDeploy() {
        deployService.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-001");
        assertTrue(result.verified(),
            "REQ-001 should verify: SVC-FlowmeterDiagnostics has recall=0.94 >= 0.90 and latency=45 <= 60");
        assertEquals(2, result.criteria().size(),
            "REQ-001 should have 2 verification criteria (RecallCheck + LatencyCheck)");
    }

    @Test
    @DisplayName("Should transition lifecycleStatus to Verified after successful verification")
    void shouldTransitionToVerified() {
        deployService.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        verifyService.execute("REQ-001");
        String status = deployService.getLifecycleStatus("REQ-001");
        assertEquals("Verified", status,
            "lifecycleStatus should be 'Verified' after successful R-Verify");
    }

    @Test
    @DisplayName("Should pass RecallCheck criterion for SVC-FlowmeterDiagnostics")
    void shouldPassRecallCheck() {
        deployService.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-001");
        RVerifyService.CriterionResult recallCheck = result.criteria().stream()
            .filter(c -> c.name().equals("CRIT-REQ001-Recall"))
            .findFirst()
            .orElse(null);

        assertNotNull(recallCheck, "CRIT-REQ001-Recall criterion should exist");
        assertTrue(recallCheck.passed(),
            "RecallCheck should pass: recall=0.94 >= threshold=0.90");
    }

    @Test
    @DisplayName("Should pass LatencyCheck criterion for SVC-FlowmeterDiagnostics")
    void shouldPassLatencyCheck() {
        deployService.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-001");
        RVerifyService.CriterionResult latencyCheck = result.criteria().stream()
            .filter(c -> c.name().equals("CRIT-REQ001-Latency"))
            .findFirst()
            .orElse(null);

        assertNotNull(latencyCheck, "CRIT-REQ001-Latency criterion should exist");
        assertTrue(latencyCheck.passed(),
            "LatencyCheck should pass: latency=45 <= threshold=60");
    }

    @Test
    @DisplayName("Should include detail string in criterion results")
    void shouldIncludeDetailInCriterionResults() {
        deployService.execute("REQ-001", "Flowmeter Diagnostics Service",
            "OPC-UA binding for flowmeter", "FT101 flowmeter OPC-UA connector",
            "On-premise deployment");

        RVerifyService.VerifyResult result = verifyService.execute("REQ-001");
        for (RVerifyService.CriterionResult cr : result.criteria()) {
            assertNotNull(cr.detail(), "Detail should not be null for " + cr.name());
            assertFalse(cr.detail().isBlank(), "Detail should not be blank for " + cr.name());
        }
    }

    @Test
    @DisplayName("Should return not verified when requirement has no VerificationCriterion")
    void shouldHandleNoCriteria() {
        deployService.execute("REQ-003", "HVAC Temperature Analysis Service",
            "OPC-UA binding for AHU-01", "AHU-01 OPC-UA connector",
            "OT Zone 3 deployment");

        // Remove all verifiedBy links so the requirement has no criteria at all
        loader.getModel().getResource("http://www.example.org/instance#REQ-003")
            .removeAll(loader.getModel().createProperty(
                "http://sda-platform.org/ontology#verifiedBy"));

        RVerifyService.VerifyResult result = verifyService.execute("REQ-003");
        assertFalse(result.verified(),
            "REQ-003 without VerificationCriterion should return not verified");
        assertTrue(result.summary().contains("No verification criteria"),
            "Summary should indicate no verification criteria were defined");
    }
}
