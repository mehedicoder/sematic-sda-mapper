package com.sda.mapper.evaluation;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.RequirementService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.api.dto.MatchRequirementRequest;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the generic matcher serves ANY requirement modeled with the standard
 * SDA properties (no per-requirement query), including user-defined requirements
 * with protocol/access-mode constraints.
 */
@DisplayName("Generic requirement matching — any requirement")
class GenericRequirementMatchingTest {

    private static final String SDA = "http://sda-platform.org/ontology#";
    private static final String INST = "http://sda-platform.org/instance#";

    private static SemanticReasonerService reasoner;
    private static RequirementService requirementService;
    private static Model model;

    @BeforeAll
    static void setUp() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();
        model = loader.getModel();
        buildMinimalService(model);

        var queryLoader = new SparqlQueryLoader();
        var rDeploy = new RDeployService(loader);
        reasoner = new SemanticReasonerService(loader, queryLoader, rDeploy);
        requirementService = new RequirementService(loader, reasoner);
    }

    private static void buildMinimalService(Model m) {
        Resource svc = m.createResource(INST + "SVC-Test");
        svc.addProperty(RDF.type, m.createResource(SDA + "Service"));
        svc.addProperty(m.createProperty(SDA + "reportedRecall"), m.createTypedLiteral(0.95));
        svc.addProperty(m.createProperty(SDA + "reportedLatency"), m.createTypedLiteral(50));

        Resource bind = m.createResource(INST + "B-Test");
        bind.addProperty(RDF.type, m.createResource(SDA + "CommunicationBinding"));
        bind.addProperty(m.createProperty(SDA + "protocol"), "OPC-UA");
        bind.addProperty(m.createProperty(SDA + "accessMode"), "read-only");
        svc.addProperty(m.createProperty(SDA + "hasBinding"), bind);

        Resource conn = m.createResource(INST + "C-Test");
        conn.addProperty(RDF.type, m.createResource(SDA + "DataConnector"));
        conn.addProperty(RDFS.label, "conn-test");
        svc.addProperty(m.createProperty(SDA + "hasConnector"), conn);

        Resource dep = m.createResource(INST + "D-Test");
        dep.addProperty(RDF.type, m.createResource(SDA + "DeploymentContext"));
        dep.addProperty(RDFS.label, "dep-test");
        dep.addProperty(m.createProperty(SDA + "onPlantNetwork"), m.createTypedLiteral(true));
        svc.addProperty(m.createProperty(SDA + "isDeployedBy"), dep);

        Resource oc = m.createResource(INST + "OC-Test");
        oc.addProperty(RDF.type, m.createResource(SDA + "OfferedCapability"));
        oc.addProperty(m.createProperty(SDA + "capabilityType"), "FlowmeterDiagnostics");
        Resource sk = m.createResource(INST + "SK-Test");
        sk.addProperty(RDF.type, m.createResource(SDA + "Skill"));
        sk.addProperty(m.createProperty(SDA + "isImplementedBy"), svc);
        oc.addProperty(m.createProperty(SDA + "realisedBy"), sk);

        addRequirement(m, "REQ-OPCUA", "OPC-UA", "read-only");
        addRequirement(m, "REQ-PROFINET", "PROFINET", "read-write");
        addRequirement(m, "REQ-NONE", null, null);
    }

    private static void addRequirement(Model m, String id, String protocol, String accessMode) {
        Resource req = m.createResource(INST + id);
        req.addProperty(RDF.type, m.createResource(SDA + "UseCaseRequirement"));
        req.addProperty(m.createProperty(SDA + "requirementID"), id);
        Resource rc = m.createResource(INST + "RC-" + id);
        rc.addProperty(RDF.type, m.createResource(SDA + "RequiredCapability"));
        rc.addProperty(m.createProperty(SDA + "capabilityType"), "FlowmeterDiagnostics");
        req.addProperty(m.createProperty(SDA + "mapsToCapability"), rc);
        if (protocol != null) {
            req.addProperty(m.createProperty(SDA + "protocol"), protocol);
        }
        if (accessMode != null) {
            req.addProperty(m.createProperty(SDA + "accessMode"), accessMode);
        }
    }

    @Test
    @DisplayName("User requirement with matching protocol should match the service")
    void reqWithMatchingProtocolMatches() {
        var r = reasoner.matchByRequirementId("REQ-OPCUA");
        assertTrue(r.matched());
        assertEquals("SVC-Test", r.serviceName());
    }

    @Test
    @DisplayName("User requirement with non-matching protocol should NOT match")
    void reqWithWrongProtocolDoesNotMatch() {
        var r = reasoner.matchByRequirementId("REQ-PROFINET");
        assertFalse(r.matched(), "PROFINET requirement must not match an OPC-UA-only service");
    }

    @Test
    @DisplayName("Requirement without protocol constraint should match")
    void reqWithoutProtocolMatches() {
        var r = reasoner.matchByRequirementId("REQ-NONE");
        assertTrue(r.matched());
        assertEquals("SVC-Test", r.serviceName());
    }

    @Test
    @DisplayName("Unknown requirement ID returns no match")
    void unknownRequirementNoMatch() {
        var r = reasoner.matchByRequirementId("REQ-DOES-NOT-EXIST");
        assertFalse(r.matched());
    }

    @Test
    @DisplayName("RequirementService.persist then match should find the service")
    void persistThenMatch() {
        requirementService.persist(new MatchRequirementRequest(
            "REQ-USER-1", "User analytics", "desc", "FlowmeterDiagnostics",
            null, "OPC-UA", "read-only", null, null, null, null,
            0.90, 60, null, null));
        var r = reasoner.matchByRequirementId("REQ-USER-1");
        assertTrue(r.matched());
        assertEquals("SVC-Test", r.serviceName());
    }

    @Test
    @DisplayName("matchOrCreate with arbitrary ID and capability type should create and match")
    void matchOrCreateArbitraryIdMatches() {
        var r = requirementService.matchOrCreate(new MatchRequirementRequest(
            "REQ-ABC", "Ad-hoc requirement", "desc", "FlowmeterDiagnostics",
            null, "OPC-UA", "read-only", null, null, null, null,
            0.90, 60, null, null));
        assertTrue(r.matched(), "An arbitrary ID (REQ-ABC) with a capability type must match");
        assertEquals("SVC-Test", r.serviceName());
    }

    @Test
    @DisplayName("matchOrCreate with unknown ID and no spec should return no match")
    void matchOrCreateUnknownIdNoSpecNoMatch() {
        var r = requirementService.matchOrCreate(new MatchRequirementRequest(
            "REQ-NO-SPEC", null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null));
        assertFalse(r.matched(), "Unknown ID without a capability/constraints cannot match");
    }
}
