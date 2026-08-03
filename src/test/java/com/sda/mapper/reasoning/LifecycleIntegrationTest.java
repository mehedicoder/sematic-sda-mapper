package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.support.DemoAboxBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Full lifecycle integration — Defined → Matched → Verified")
class LifecycleIntegrationTest {

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "https://example.org/ontology/sda#";

    private static OntologyModelLoader loader;
    private static SemanticReasonerService reasoner;
    private static RDeployService deployService;
    private static RVerifyService verifyService;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-unified.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        var queryLoader = new SparqlQueryLoader();
        deployService = new RDeployService(loader);
        verifyService = new RVerifyService(loader);
        reasoner = new SemanticReasonerService(loader, queryLoader, deployService);
    }

    @Test
    @DisplayName("REQ-001 lifecycle: Defined → SPARQL match → Matched → Verify → Verified")
    void shouldCompleteFullLifecycleForReq001() {
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());

        String initialStatus = deployService.getLifecycleStatus("REQ-001");
        assertEquals("Defined", initialStatus,
            "REQ-001 should start with lifecycleStatus=Defined");
        assertFalse(deployService.isMatched("REQ-001"),
            "REQ-001 should not be matched initially");

        // Step 2: SPARQL match + R-Deploy
        ServiceMatchResult matchResult = reasoner.matchByRequirementId("REQ-001");
        assertTrue(matchResult.matched(), "REQ-001 should match a service");
        assertEquals("Flowmeter Diagnostics Service", matchResult.serviceName());

        // Step 3: After R-Deploy, should be Matched
        String afterDeploy = deployService.getLifecycleStatus("REQ-001");
        assertEquals("Matched", afterDeploy,
            "REQ-001 should be Matched after R-Deploy");
        assertTrue(deployService.isMatched("REQ-001"),
            "REQ-001 should report isMatched=true");

        // Step 4: R-Verify
        RVerifyService.VerifyResult verifyResult = verifyService.execute("REQ-001");
        assertTrue(verifyResult.verified(),
            "REQ-001 should verify: recall=0.94 >= 0.90 and latency=45 <= 60");
        assertEquals(2, verifyResult.criteria().size());

        // Step 5: After R-Verify, should be Verified
        String afterVerify = deployService.getLifecycleStatus("REQ-001");
        assertEquals("Verified", afterVerify,
            "REQ-001 should be Verified after successful R-Verify");
    }

    @Test
    @DisplayName("R-Deploy should populate all bound properties")
    void shouldPopulateAllBoundProperties() {
        reasoner.matchByRequirementId("REQ-001");

        var model = loader.getModel();
        var req = model.createResource(INST_NS + "REQ-001");

        assertTrue(req.hasProperty(model.createProperty(SDA_NS + "boundService")),
            "Should have boundService");
        assertTrue(req.hasProperty(model.createProperty(SDA_NS + "boundBinding")),
            "Should have boundBinding");
        assertTrue(req.hasProperty(model.createProperty(SDA_NS + "boundConnector")),
            "Should have boundConnector");
        assertTrue(req.hasProperty(model.createProperty(SDA_NS + "boundContext")),
            "Should have boundContext");
        assertTrue(req.hasProperty(model.createProperty(SDA_NS + "matchedCandidate")),
            "Should have matchedCandidate");
    }

    @Test
    @DisplayName("Match result should include lifecycleStatus=Matched after R-Deploy")
    void shouldIncludeLifecycleStatusInMatchResult() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertEquals("Matched", result.lifecycleStatus(),
            "Match result should report lifecycleStatus=Matched");
        assertEquals("REQ-001", result.requirementId(),
            "Match result should include requirementId");
    }

    @Test
    @DisplayName("REQ-001 should have all 4 bound* properties pointing to correct resources")
    void shouldBindToCorrectResources() {
        reasoner.matchByRequirementId("REQ-001");

        var model = loader.getModel();
        var req = model.createResource(INST_NS + "REQ-001");

        var boundService = req.getPropertyResourceValue(
            model.createProperty(SDA_NS + "boundService"));
        assertNotNull(boundService, "boundService should not be null");
        assertEquals("SVC-FlowmeterDiagnostics", boundService.getLocalName(),
            "boundService should point to SVC-FlowmeterDiagnostics");

        var boundContext = req.getPropertyResourceValue(
            model.createProperty(SDA_NS + "boundContext"));
        assertNotNull(boundContext, "boundContext should not be null");
        assertEquals("DEPLOY-OnPrem", boundContext.getLocalName(),
            "boundContext should point to DEPLOY-OnPrem");
    }

    @Test
    @DisplayName("REQ-002 lifecycle: R-Deploy works for fermentation skid")
    void shouldCompleteLifecycleForReq002() {
        reasoner.matchByRequirementId("REQ-002");

        String status = deployService.getLifecycleStatus("REQ-002");
        assertEquals("Matched", status,
            "REQ-002 should be Matched after R-Deploy");
    }

    @Test
    @DisplayName("R2a requiresConcept filter should still return match when concept is provided")
    void shouldMatchWhenConceptIsProvided() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched(),
            "REQ-001 should match because DC-Flowmeter-FT101 maps a ProcessVariable described by :PA-DIM-FlowConcept which SK-FlowmeterDiagnostics requiresConcept");
    }
}
