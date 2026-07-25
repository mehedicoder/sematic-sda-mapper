package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Full lifecycle integration — Defined → Matched → Verified")
class LifecycleIntegrationTest {

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";

    private static OntologyModelLoader loader;
    private static SemanticReasonerService reasoner;
    private static RDeployService deployService;
    private static RVerifyService verifyService;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        createTestRequirements();
        var queryLoader = new SparqlQueryLoader();
        deployService = new RDeployService(loader);
        verifyService = new RVerifyService(loader);
        reasoner = new SemanticReasonerService(loader, queryLoader, deployService);
    }

    private static void createTestRequirements() {
        Model model = loader.getModel();

        Property type = model.createProperty(RDF.type.getURI());
        Property requirementID = model.createProperty(SDA_NS + "requirementID");
        Property protocol = model.createProperty(SDA_NS + "protocol");
        Property accessMode = model.createProperty(SDA_NS + "accessMode");
        Property safetyIntegrityLevel = model.createProperty(SDA_NS + "safetyIntegrityLevel");
        Property determinismClass = model.createProperty(SDA_NS + "determinismClass");
        Property hasConstraint = model.createProperty(SDA_NS + "hasConstraint");
        Property verifiedBy = model.createProperty(SDA_NS + "verifiedBy");
        Property constraintType = model.createProperty(SDA_NS + "constraintType");
        Property constraintValue = model.createProperty(SDA_NS + "constraintValue");
        Property criterionType = model.createProperty(SDA_NS + "criterionType");
        Property criterionMetric = model.createProperty(SDA_NS + "criterionMetric");
        Property criterionOperator = model.createProperty(SDA_NS + "criterionOperator");
        Property criterionThreshold = model.createProperty(SDA_NS + "criterionThreshold");

        Resource requirementClass = model.createResource(SDA_NS + "Requirement");

        // REQ-TEST-001: Flowmeter-like, matches SVC-FlowmeterDiagnostics via OPC-UA/read-only
        // SIL-0 is required to disambiguate from SVC-FermentationSkid which also has OPC-UA/read-only binding
        Resource req1 = model.createResource(INST_NS + "REQ-TEST-001");
        req1.addProperty(RDF.type, requirementClass);
        req1.addProperty(requirementID, "REQ-TEST-001");
        req1.addProperty(protocol, "OPC-UA");
        req1.addProperty(accessMode, "read-only");
        req1.addProperty(safetyIntegrityLevel, "SIL-0");

        Resource recallConstraint1 = model.createResource(INST_NS + "Constraint-Recall-001");
        recallConstraint1.addProperty(constraintType, "recall");
        recallConstraint1.addProperty(constraintValue, "0.90");
        req1.addProperty(hasConstraint, recallConstraint1);

        Resource latencyConstraint1 = model.createResource(INST_NS + "Constraint-Latency-001");
        latencyConstraint1.addProperty(constraintType, "latency");
        latencyConstraint1.addProperty(constraintValue, "60");
        req1.addProperty(hasConstraint, latencyConstraint1);

        Resource recallCheck = model.createResource(INST_NS + "RecallCheck");
        recallCheck.addProperty(RDFS.label, "Recall Check");
        recallCheck.addProperty(criterionType, "performance");
        recallCheck.addProperty(criterionMetric, "reportedRecall");
        recallCheck.addProperty(criterionOperator, ">=");
        recallCheck.addProperty(criterionThreshold, "0.90");
        req1.addProperty(verifiedBy, recallCheck);

        Resource latencyCheck = model.createResource(INST_NS + "LatencyCheck");
        latencyCheck.addProperty(RDFS.label, "Latency Check");
        latencyCheck.addProperty(criterionType, "performance");
        latencyCheck.addProperty(criterionMetric, "reportedLatency");
        latencyCheck.addProperty(criterionOperator, "<=");
        latencyCheck.addProperty(criterionThreshold, "60");
        req1.addProperty(verifiedBy, latencyCheck);

        // REQ-TEST-002: Fermentation-like, matches SVC-FermentationSkid via PROFINET/read-write
        Resource req2 = model.createResource(INST_NS + "REQ-TEST-002");
        req2.addProperty(RDF.type, requirementClass);
        req2.addProperty(requirementID, "REQ-TEST-002");
        req2.addProperty(protocol, "PROFINET");
        req2.addProperty(accessMode, "read-write");
        req2.addProperty(safetyIntegrityLevel, "SIL-2");
        req2.addProperty(determinismClass, "HardRealTime");

        Resource recallConstraint2 = model.createResource(INST_NS + "Constraint-Recall-002");
        recallConstraint2.addProperty(constraintType, "recall");
        recallConstraint2.addProperty(constraintValue, "0.90");
        req2.addProperty(hasConstraint, recallConstraint2);

        Resource latencyConstraint2 = model.createResource(INST_NS + "Constraint-Latency-002");
        latencyConstraint2.addProperty(constraintType, "latency");
        latencyConstraint2.addProperty(constraintValue, "60");
        req2.addProperty(hasConstraint, latencyConstraint2);
    }

    @Test
    @DisplayName("REQ-TEST-001 lifecycle: Unknown → SPARQL match → Matched → Verify → Verified")
    void shouldCompleteFullLifecycleForReq001() {
        loader.loadOntology();
        createTestRequirements();

        String initialStatus = deployService.getLifecycleStatus("REQ-TEST-001");
        assertEquals("Unknown", initialStatus,
            "REQ-TEST-001 should start with lifecycleStatus=Unknown (no value set in ontology)");
        assertFalse(deployService.isMatched("REQ-TEST-001"),
            "REQ-TEST-001 should not be matched initially");

        ServiceMatchResult matchResult = reasoner.matchByRequirementId("REQ-TEST-001");
        assertTrue(matchResult.matched(), "REQ-TEST-001 should match a service");
        assertEquals("Flowmeter Diagnostics Service", matchResult.serviceName());

        String afterDeploy = deployService.getLifecycleStatus("REQ-TEST-001");
        assertEquals("Matched", afterDeploy,
            "REQ-TEST-001 should be Matched after R-Deploy");
        assertTrue(deployService.isMatched("REQ-TEST-001"),
            "REQ-TEST-001 should report isMatched=true");

        RVerifyService.VerifyResult verifyResult = verifyService.execute("REQ-TEST-001");
        assertTrue(verifyResult.verified(),
            "REQ-TEST-001 should verify: recall=0.94 >= 0.90 and latency=45 <= 60");
        assertEquals(2, verifyResult.criteria().size());

        String afterVerify = deployService.getLifecycleStatus("REQ-TEST-001");
        assertEquals("Verified", afterVerify,
            "REQ-TEST-001 should be Verified after successful R-Verify");
    }

    @Test
    @DisplayName("R-Deploy should populate all bound properties")
    void shouldPopulateAllBoundProperties() {
        reasoner.matchByRequirementId("REQ-TEST-001");

        var model = loader.getModel();
        var req = model.getResource(INST_NS + "REQ-TEST-001");

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
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-TEST-001");
        assertEquals("Matched", result.lifecycleStatus(),
            "Match result should report lifecycleStatus=Matched");
        assertEquals("REQ-TEST-001", result.requirementId(),
            "Match result should include requirementId");
    }

    @Test
    @DisplayName("REQ-TEST-001 should have all 4 bound* properties pointing to correct resources")
    void shouldBindToCorrectResources() {
        reasoner.matchByRequirementId("REQ-TEST-001");

        var model = loader.getModel();
        var req = model.getResource(INST_NS + "REQ-TEST-001");

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
    @DisplayName("REQ-TEST-002 lifecycle: R-Deploy works for fermentation skid")
    void shouldCompleteLifecycleForReq002() {
        reasoner.matchByRequirementId("REQ-TEST-002");

        String status = deployService.getLifecycleStatus("REQ-TEST-002");
        assertEquals("Matched", status,
            "REQ-TEST-002 should be Matched after R-Deploy");
    }

    @Test
    @DisplayName("R2a requiresConcept filter should still return match when concept is provided")
    void shouldMatchWhenConceptIsProvided() {
        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-TEST-001");
        assertTrue(result.matched(),
            "REQ-TEST-001 should match because DC-Flowmeter-FT101 providesConcept :PA-DIM-FlowConcept which SK-FlowmeterDiagnostics requiresConcept");
    }
}
