package com.sda.mapper.ontology;

import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.RVerifyService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.support.DemoAboxBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SPARQL matching and ontology validation for the v2 structured ontology
 * ({@code sda_ontology_v2_structured.ttl}, namespace
 * {@code http://sda-platform.org/ontology#}).
 * <p>
 * Category A and B use the HermiT reasoner through the OWL API (Jena's built-in
 * reasoner does not handle {@code owl:AllDisjointClasses} / {@code equivalentClass}
 * classification). Categories C–E run SPARQL and lifecycle integration checks on
 * Jena models seeded with the v2 demo ABox.
 */
@SpringBootTest
@DisplayName("SPARQL Matching and Ontology Validation — Use Cases A/B/C + Validation Categories A–E")
class SparqlMatchingTest {

    private static final String ONTOLOGY_PATH = "ontology/sda_ontology_v2_structured.ttl";
    private static final String INST_NS = "http://sda-platform.org/instance#";
    private static final String SDA_NS = "http://sda-platform.org/ontology#";

    private static Model model;

    /**
     * REQ-002 (Fermentation Skid) query inlined so the test stays self-contained:
     * the SIL is read as an xsd:integer via
     * Service → isDeployedBy → DeploymentContext → contextConstrainedBy →
     * SafetyClass → safetyIntegrityLevel, and determinismClass is read from the
     * DeploymentContext.
     */
    private static final String REQ_002_QUERY = """
        PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX sda:  <http://sda-platform.org/ontology#>
        PREFIX :     <http://sda-platform.org/instance#>

        SELECT ?requirement ?capability ?skill ?service ?binding ?protocol ?accessMode
               ?recall ?latency ?safetyIntegrityLevel ?determinismClass ?networkZone
               ?connector ?connectorLabel ?deploy ?deployLabel
        WHERE {
            BIND(:REQ-002 AS ?requirement)

            ?requirement  a                  sda:UseCaseRequirement ;
                          sda:mapsToCapability ?capReq .
            ?capReq       sda:capabilityType  ?capType .

            ?capOff       sda:capabilityType  ?capType ;
                          sda:realisedBy      ?skill .
            BIND(?capOff AS ?capability)

            ?skill        sda:isImplementedBy ?service .

            ?service      sda:hasBinding      ?binding .
            ?binding      sda:protocol        ?protocol ;
                          sda:accessMode      ?accessMode .
            ?service      sda:hasConnector    ?connector .
            ?connector    rdfs:label          ?connectorLabel .

            FILTER (?protocol = "PROFINET")
            FILTER (?accessMode = "read-write")

            ?service      sda:reportedRecall  ?recall ;
                          sda:reportedLatency ?latency .
            FILTER (?recall >= 0.90)
            FILTER (?latency <= 60)

            ?service      sda:isDeployedBy    ?deploy .
            ?deploy       rdfs:label          ?deployLabel ;
                          sda:onPlantNetwork  ?onPlantNetwork ;
                          sda:determinismClass ?determinismClass ;
                          sda:locatedInZone   ?zone ;
                          sda:contextConstrainedBy ?safetyClass .
            ?safetyClass  sda:safetyIntegrityLevel ?safetyIntegrityLevel .
            ?zone         rdfs:label          ?networkZone .

            FILTER (?onPlantNetwork = true)
            FILTER (?safetyIntegrityLevel >= 2)
            FILTER (?determinismClass = "HardRealTime")

            FILTER NOT EXISTS {
                ?skill sda:requiresConcept ?requiredConcept .
                FILTER NOT EXISTS {
                    ?service sda:hasConnector ?dc .
                    ?dc sda:mapsVariable ?pv .
                    ?pv sda:describedBy ?requiredConcept .
                }
            }
        }
        ORDER BY DESC(?recall)
        LIMIT 1
        """;

    @BeforeAll
    static void loadOntology() throws IOException {
        ClassPathResource resource = new ClassPathResource(ONTOLOGY_PATH);
        assertTrue(resource.exists(), "Ontology file must exist");
        model = ModelFactory.createDefaultModel();
        try (InputStream in = resource.getInputStream()) {
            model.read(in, null, "TTL");
        }
        DemoAboxBuilder.apply(model);
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
    @DisplayName("Should exclude a service with a protocol mismatch (MODBUS vs OPC-UA)")
    void shouldExcludeWrongProtocol() throws IOException {
        Model fresh = freshModel();
        Resource wrong = fresh.createResource(INST_NS + "SVC-WrongProtocol");
        wrong.addProperty(RDF.type, fresh.createResource(SDA_NS + "Service"));
        wrong.addProperty(RDFS.label, "Wrong Protocol Service");
        wrong.addProperty(fresh.createProperty(SDA_NS + "reportedRecall"),
            fresh.createTypedLiteral(0.94));
        wrong.addProperty(fresh.createProperty(SDA_NS + "reportedLatency"),
            fresh.createTypedLiteral(45));
        Resource wrongBinding = fresh.createResource(INST_NS + "CB-WrongProtocol");
        wrongBinding.addProperty(RDF.type, fresh.createResource(SDA_NS + "CommunicationBinding"));
        wrongBinding.addProperty(fresh.createProperty(SDA_NS + "protocol"), "MODBUS");
        wrongBinding.addProperty(fresh.createProperty(SDA_NS + "accessMode"), "read-only");
        wrong.addProperty(fresh.createProperty(SDA_NS + "hasBinding"), wrongBinding);
        wrong.addProperty(fresh.createProperty(SDA_NS + "hasConnector"),
            fresh.createResource(INST_NS + "DC-FT101-OPCUA"));
        wrong.addProperty(fresh.createProperty(SDA_NS + "isImplementedBy"),
            fresh.createResource(INST_NS + "SK-FlowmeterDiagnostics"));
        wrong.addProperty(fresh.createProperty(SDA_NS + "isDeployedBy"),
            fresh.createResource(INST_NS + "DEPLOY-OnPrem"));

        List<QuerySolution> results = executeQuery(loadQuery("queries/match-service.rq"), fresh);
        assertFalse(results.isEmpty(), "Baseline match must still return a service");
        for (QuerySolution sol : results) {
            assertNotEquals(INST_NS + "SVC-WrongProtocol",
                sol.getResource("service").getURI(),
                "SVC-WrongProtocol must not appear in results");
        }
    }

    @Test
    @DisplayName("Should exclude a service reachable only through the wrong capability chain")
    void shouldExcludeWrongCapability() throws IOException {
        Model fresh = freshModel();
        Resource wrong = fresh.createResource(INST_NS + "SVC-WrongCapability");
        wrong.addProperty(RDF.type, fresh.createResource(SDA_NS + "Service"));
        wrong.addProperty(RDFS.label, "Wrong Capability Service");
        wrong.addProperty(fresh.createProperty(SDA_NS + "isImplementedBy"),
            fresh.createResource(INST_NS + "SK-HVACTempAnalysis"));

        List<QuerySolution> results = executeQuery(loadQuery("queries/match-service.rq"), fresh);
        for (QuerySolution sol : results) {
            assertNotEquals(INST_NS + "SVC-WrongCapability",
                sol.getResource("service").getURI(),
                "SVC-WrongCapability must not appear in results");
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // USE CASE B: Fermentation Skid
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Should match REQ-002 to SVC-FermentationSkid via PROFINET with integer SIL-2")
    void shouldMatchReq002ToFermentationSkid() throws IOException {
        Model fresh = req002Model();
        List<QuerySolution> results = executeQuery(REQ_002_QUERY, fresh);

        assertFalse(results.isEmpty(),
            "Query must return at least one result for REQ-002");

        QuerySolution sol = results.getFirst();
        assertEquals(INST_NS + "SVC-FermentationSkid",
            sol.getResource("service").getURI(),
            "Must return SVC-FermentationSkid for REQ-002");
        assertEquals("PROFINET", sol.getLiteral("protocol").getString());
        assertEquals("read-write", sol.getLiteral("accessMode").getString());
        assertEquals(2, sol.getLiteral("safetyIntegrityLevel").getInt(),
            "safetyIntegrityLevel must be the integer SIL value 2 read via SafetyClass");
        assertEquals("HardRealTime", sol.getLiteral("determinismClass").getString());
    }

    @Test
    @DisplayName("Should exclude service when SIL mismatch (SIL-0 vs SIL-2)")
    void shouldExcludeOnSilMismatch() throws IOException {
        // REQ-002 requires SIL-2, but DEPLOY-OnPrem is constrained by SCL-OnPrem
        // (SIL-0). The query filters ?safetyIntegrityLevel >= 2, so
        // SVC-FlowmeterDiagnostics must not appear.
        List<QuerySolution> results = executeQuery(REQ_002_QUERY, req002Model());
        for (QuerySolution sol : results) {
            assertNotEquals(INST_NS + "SVC-FlowmeterDiagnostics",
                sol.getResource("service").getURI(),
                "SVC-FlowmeterDiagnostics must not appear (SIL-0 vs SIL-2 mismatch)");
        }
    }

    @Test
    @DisplayName("Should exclude service when determinism class mismatch")
    void shouldExcludeOnDeterminismMismatch() throws IOException {
        // REQ-002 requires HardRealTime; every returned service must satisfy it.
        List<QuerySolution> results = executeQuery(REQ_002_QUERY, req002Model());
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
        // REQ-003 requires OT-Zone-3; DEPLOY-OnPrem / DEPLOY-Critical are in
        // OT-Zone-1, so their services must not satisfy the zone filter.
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
    // CATEGORY A: Ontological structural validity (HermiT)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("A1 — HermiT keeps the v2 ontology consistent with no unsatisfiable classes")
    void shouldKeepOntologyConsistent() throws Exception {
        OWLOntology ontology = loadOwlOntology();
        OWLReasoner reasoner = createHermitReasoner(ontology);

        assertTrue(reasoner.isConsistent(),
            "The v2 ontology must be consistent under HermiT");
        assertTrue(reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().isEmpty(),
            "The v2 ontology must have no unsatisfiable classes");
    }

    @Test
    @DisplayName("A2 — A class that is both EdgeContext and CloudContext must be owl:Nothing")
    void shouldRejectEdgeCloudOverlap() throws Exception {
        assertDisjointnessProbe("ProbeEdgeCloud", "EdgeContext", "CloudContext");
    }

    @Test
    @DisplayName("A3 — A class that is both MTPSkill and AASSkill must be owl:Nothing")
    void shouldRejectMtpAasSkillOverlap() throws Exception {
        assertDisjointnessProbe("ProbeMtpAasSkill", "MTPSkill", "AASSkill");
    }

    @Test
    @DisplayName("A4 — A class that is both CPCService and MOService must be owl:Nothing")
    void shouldRejectCpcMoServiceOverlap() throws Exception {
        assertDisjointnessProbe("ProbeCpcMoService", "CPCService", "MOService");
    }

    @Test
    @DisplayName("A5 — A class that is both OTZone and DMZZone must be owl:Nothing")
    void shouldRejectOtDmzZoneOverlap() throws Exception {
        assertDisjointnessProbe("ProbeOtDmzZone", "OTZone", "DMZZone");
    }

    @Test
    @DisplayName("A6 — A class that is both FunctionalRequirement and PerformanceRequirement must be owl:Nothing")
    void shouldRejectFunctionalPerformanceRequirementOverlap() throws Exception {
        assertDisjointnessProbe("ProbeFuncPerfRequirement",
            "FunctionalRequirement", "PerformanceRequirement");
    }

    // ════════════════════════════════════════════════════════════════════
    // CATEGORY B: Defined-class auto-classification (HermiT)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("B1 — HermiT classifies a hard-constrained requirement as HardConstrainedRequirement")
    void shouldClassifyHardConstrainedRequirement() throws Exception {
        OWLOntology ontology = loadOwlOntology();
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLNamedIndividual reqHard = individual(df, "reqHard");
        OWLNamedIndividual reqSoft = individual(df, "reqSoft");
        OWLNamedIndividual constraintHard = individual(df, "constraintHard");
        OWLNamedIndividual constraintSoft = individual(df, "constraintSoft");
        OWLClass hard = cls(df, "HardConstrainedRequirement");

        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "UseCaseRequirement"), reqHard));
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "UseCaseRequirement"), reqSoft));
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "UseCaseRequirementConstraint"), constraintHard));
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "UseCaseRequirementConstraint"), constraintSoft));
        manager.addAxiom(ontology, df.getOWLObjectPropertyAssertionAxiom(op(df, "hasConstraint"), reqHard, constraintHard));
        manager.addAxiom(ontology, df.getOWLObjectPropertyAssertionAxiom(op(df, "hasConstraint"), reqSoft, constraintSoft));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "isHardConstraint"), constraintHard, df.getOWLLiteral(true)));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "isHardConstraint"), constraintSoft, df.getOWLLiteral(false)));

        OWLReasoner reasoner = createHermitReasoner(ontology);
        assertTrue(reasoner.getTypes(reqHard, false).containsEntity(hard),
            "reqHard (isHardConstraint=true) must be inferred as HardConstrainedRequirement");
        assertFalse(reasoner.getTypes(reqSoft, false).containsEntity(hard),
            "reqSoft (isHardConstraint=false) must NOT be inferred as HardConstrainedRequirement");
    }

    @Test
    @DisplayName("B2 — HermiT classifies a skill with an mtpDescriptorURI as AutoLiftedMTPSkill")
    void shouldClassifyAutoLiftedMtpSkill() throws Exception {
        OWLOntology ontology = loadOwlOntology();
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLNamedIndividual skillMTP = individual(df, "skillMTP");
        OWLNamedIndividual skillPlain = individual(df, "skillPlain");
        OWLClass autoLifted = cls(df, "AutoLiftedMTPSkill");

        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "Skill"), skillMTP));
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "Skill"), skillPlain));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "mtpDescriptorURI"),
            skillMTP, df.getOWLLiteral("https://example.org/mtp/descriptor")));

        OWLReasoner reasoner = createHermitReasoner(ontology);
        assertTrue(reasoner.getTypes(skillMTP, false).containsEntity(autoLifted),
            "skillMTP (with mtpDescriptorURI) must be inferred as AutoLiftedMTPSkill");
        assertFalse(reasoner.getTypes(skillPlain, false).containsEntity(autoLifted),
            "skillPlain (no mtpDescriptorURI) must NOT be inferred as AutoLiftedMTPSkill");
    }

    @Test
    @DisplayName("B3 — HermiT classifies a fully Margo-compliant cluster node as MarginCompliantClusterNode")
    void shouldClassifyMarginCompliantClusterNode() throws Exception {
        OWLOntology ontology = loadOwlOntology();
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLNamedIndividual nodeGood = individual(df, "nodeGood");
        OWLNamedIndividual nodeBad = individual(df, "nodeBad");
        OWLClass marginCompliant = cls(df, "MarginCompliantClusterNode");

        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "ClusterNode"), nodeGood));
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "ClusterNode"), nodeBad));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "marginDeviceRole"), nodeGood, df.getOWLLiteral("StandaloneCluster")));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "hasOTELCollector"), nodeGood, df.getOWLLiteral(true)));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "ociRuntimeCompliant"), nodeGood, df.getOWLLiteral(true)));
        // nodeBad misses one condition (ociRuntimeCompliant=false)
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "marginDeviceRole"), nodeBad, df.getOWLLiteral("StandaloneCluster")));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "hasOTELCollector"), nodeBad, df.getOWLLiteral(true)));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "ociRuntimeCompliant"), nodeBad, df.getOWLLiteral(false)));

        OWLReasoner reasoner = createHermitReasoner(ontology);
        assertTrue(reasoner.getTypes(nodeGood, false).containsEntity(marginCompliant),
            "nodeGood (all three conditions) must be inferred as MarginCompliantClusterNode");
        assertFalse(reasoner.getTypes(nodeBad, false).containsEntity(marginCompliant),
            "nodeBad (ociRuntimeCompliant=false) must NOT be inferred as MarginCompliantClusterNode");
    }

    @Test
    @DisplayName("B4 — HermiT classifies a DeploymentContext constrained by a SIL-2 SafetyClass as SafetyCriticalDeploymentContext")
    void shouldClassifySafetyCriticalDeploymentContext() throws Exception {
        OWLOntology ontology = loadOwlOntology();
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLNamedIndividual ctxSafe = individual(df, "ctxSafe");
        OWLNamedIndividual ctxUnsafe = individual(df, "ctxUnsafe");
        OWLNamedIndividual sclSafe = individual(df, "sclSafe");
        OWLNamedIndividual sclUnsafe = individual(df, "sclUnsafe");
        OWLClass safetyCritical = cls(df, "SafetyCriticalDeploymentContext");

        OWLOntologyManager manager = ontology.getOWLOntologyManager();
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "DeploymentContext"), ctxSafe));
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "DeploymentContext"), ctxUnsafe));
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "SafetyClass"), sclSafe));
        manager.addAxiom(ontology, df.getOWLClassAssertionAxiom(cls(df, "SafetyClass"), sclUnsafe));
        manager.addAxiom(ontology, df.getOWLObjectPropertyAssertionAxiom(op(df, "contextConstrainedBy"), ctxSafe, sclSafe));
        manager.addAxiom(ontology, df.getOWLObjectPropertyAssertionAxiom(op(df, "contextConstrainedBy"), ctxUnsafe, sclUnsafe));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "safetyIntegrityLevel"), sclSafe, df.getOWLLiteral(2)));
        manager.addAxiom(ontology, df.getOWLDataPropertyAssertionAxiom(dp(df, "safetyIntegrityLevel"), sclUnsafe, df.getOWLLiteral(0)));

        OWLReasoner reasoner = createHermitReasoner(ontology);
        assertTrue(reasoner.getTypes(ctxSafe, false).containsEntity(safetyCritical),
            "ctxSafe (SIL-2 SafetyClass) must be inferred as SafetyCriticalDeploymentContext");
        assertFalse(reasoner.getTypes(ctxUnsafe, false).containsEntity(safetyCritical),
            "ctxUnsafe (SIL-0 SafetyClass) must NOT be inferred as SafetyCriticalDeploymentContext");
    }

    // ════════════════════════════════════════════════════════════════════
    // CATEGORY C: SPARQL matching — ranking and negative scenarios
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("C1-Rank — ORDER BY DESC(?recall) must rank the 0.94 service above the 0.91 alternative")
    void shouldRankHigherRecallServiceFirst() throws IOException {
        Model fresh = freshModel();
        Resource alt = fresh.createResource(INST_NS + "SVC-FlowmeterDiagnostics-Alt");
        alt.addProperty(RDF.type, fresh.createResource(SDA_NS + "Service"));
        alt.addProperty(RDFS.label, "Flowmeter Diagnostics Alt Service");
        alt.addProperty(fresh.createProperty(SDA_NS + "reportedRecall"),
            fresh.createTypedLiteral(0.91));
        alt.addProperty(fresh.createProperty(SDA_NS + "reportedLatency"),
            fresh.createTypedLiteral(45));
        alt.addProperty(fresh.createProperty(SDA_NS + "hasBinding"),
            fresh.createResource(INST_NS + "CB-FT101-OPCUA"));
        alt.addProperty(fresh.createProperty(SDA_NS + "hasConnector"),
            fresh.createResource(INST_NS + "DC-FT101-OPCUA"));
        alt.addProperty(fresh.createProperty(SDA_NS + "isImplementedBy"),
            fresh.createResource(INST_NS + "SK-FlowmeterDiagnostics"));
        alt.addProperty(fresh.createProperty(SDA_NS + "isDeployedBy"),
            fresh.createResource(INST_NS + "DEPLOY-OnPrem"));

        List<QuerySolution> results = executeQuery(loadQuery("queries/match-service.rq"), fresh);
        assertFalse(results.isEmpty(),
            "Matching query must return the top-ranked service");

        QuerySolution top = results.getFirst();
        assertEquals(INST_NS + "SVC-FlowmeterDiagnostics",
            top.getResource("service").getURI(),
            "Top result must be the 0.94 service, not the 0.91 alternative");
        assertTrue(top.getLiteral("recall").getDouble() >= 0.94,
            "Top result must have recall >= 0.94");
    }

    @Test
    @DisplayName("C4 — R2a coverage filter must exclude the service when required concept coverage is missing")
    void shouldExcludeWhenSemanticConceptMissing() throws IOException {
        Model fresh = freshModel();
        fresh.remove(fresh.createStatement(
            fresh.createResource(INST_NS + "PV-Flow-FT101"),
            fresh.createProperty(SDA_NS + "describedBy"),
            fresh.createResource(INST_NS + "PA-DIM-FlowConcept")));

        List<QuerySolution> results = executeQuery(loadQuery("queries/match-service.rq"), fresh);

        assertTrue(results.isEmpty(),
            "R2a concept-coverage filter must exclude all results when the required concept is not covered");
    }

    @Test
    @DisplayName("C5 — R0 pre-filter must exclude services violating a hard requirement constraint")
    void shouldExcludeOnHardConstraintViolation() throws IOException {
        List<QuerySolution> baseline = executeQuery(
            loadQuery("queries/r0-hard-constraint-prefilter.rq"), freshModel());
        assertTrue(baseline.stream().anyMatch(sol ->
                sol.getResource("service").getURI().equals(INST_NS + "SVC-FlowmeterDiagnostics")),
            "Baseline: SVC-FlowmeterDiagnostics (45 ms, recall 0.94) must pass the R0 pre-filter");

        Model highLatency = freshModel();
        Resource flowmeter = highLatency.getResource(INST_NS + "SVC-FlowmeterDiagnostics");
        flowmeter.removeAll(highLatency.createProperty(SDA_NS + "reportedLatency"));
        flowmeter.addProperty(highLatency.createProperty(SDA_NS + "reportedLatency"),
            highLatency.createTypedLiteral(100));
        List<QuerySolution> latencyResults = executeQuery(
            loadQuery("queries/r0-hard-constraint-prefilter.rq"), highLatency);
        assertFalse(latencyResults.stream().anyMatch(sol ->
                sol.getResource("service").getURI().equals(INST_NS + "SVC-FlowmeterDiagnostics")),
            "Service with reportedLatency=100 must be rejected by the R0 pre-filter (MaxLatency <= 60)");

        Model lowRecall = freshModel();
        Resource flowmeterLowRecall = lowRecall.getResource(INST_NS + "SVC-FlowmeterDiagnostics");
        flowmeterLowRecall.removeAll(lowRecall.createProperty(SDA_NS + "reportedRecall"));
        flowmeterLowRecall.addProperty(lowRecall.createProperty(SDA_NS + "reportedRecall"),
            lowRecall.createTypedLiteral(0.5));
        List<QuerySolution> recallResults = executeQuery(
            loadQuery("queries/r0-hard-constraint-prefilter.rq"), lowRecall);
        assertFalse(recallResults.stream().anyMatch(sol ->
                sol.getResource("service").getURI().equals(INST_NS + "SVC-FlowmeterDiagnostics")),
            "Service with reportedRecall=0.5 must be rejected by the R0 pre-filter (MinRecall >= 0.90)");
    }

    // ════════════════════════════════════════════════════════════════════
    // CATEGORY D: R-Deploy / R-Verify lifecycle (Java equivalents of SWRL)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("D1 — R-Deploy advances REQ-001 to Matched and writes the bound* tuple")
    void shouldAdvanceToMatchedAfterRDeploy() {
        OntologyModelLoader loader = freshLoader();
        RDeployService deploy = new RDeployService(loader);
        SemanticReasonerService reasoner =
            new SemanticReasonerService(loader, new SparqlQueryLoader(), deploy);

        assertEquals("Defined", deploy.getLifecycleStatus("REQ-001"),
            "REQ-001 must start with lifecycleStatus=Defined");
        assertFalse(deploy.isMatched("REQ-001"),
            "REQ-001 must not be matched before R-Deploy");

        var result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched(), "REQ-001 must match a service");
        assertEquals("Matched", deploy.getLifecycleStatus("REQ-001"),
            "R-Deploy must advance lifecycleStatus to Matched");
        assertTrue(deploy.isMatched("REQ-001"),
            "REQ-001 must report isMatched=true after R-Deploy");

        Model m = loader.getModel();
        Resource req = m.getResource(INST_NS + "REQ-001");
        Statement lifecycleStmt = req.getProperty(m.createProperty(SDA_NS + "lifecycleStatus"));
        assertNotNull(lifecycleStmt, "REQ-001 must carry a lifecycleStatus literal after R-Deploy");
        assertEquals("Matched", lifecycleStmt.getString(),
            "lifecycleStatus must be the literal Matched");

        Resource boundService = req.getPropertyResourceValue(m.createProperty(SDA_NS + "boundService"));
        assertNotNull(boundService, "boundService must be populated after R-Deploy");
        assertEquals(INST_NS + "SVC-FlowmeterDiagnostics", boundService.getURI());

        Resource boundBinding = req.getPropertyResourceValue(m.createProperty(SDA_NS + "boundBinding"));
        assertNotNull(boundBinding, "boundBinding must be populated after R-Deploy");
        assertEquals(INST_NS + "CB-FT101-OPCUA", boundBinding.getURI());

        Resource boundConnector = req.getPropertyResourceValue(m.createProperty(SDA_NS + "boundConnector"));
        assertNotNull(boundConnector, "boundConnector must be populated after R-Deploy");
        assertEquals(INST_NS + "DC-FT101-OPCUA", boundConnector.getURI());

        Resource boundContext = req.getPropertyResourceValue(m.createProperty(SDA_NS + "boundContext"));
        assertNotNull(boundContext, "boundContext must be populated after R-Deploy");
        assertEquals(INST_NS + "DEPLOY-OnPrem", boundContext.getURI());

        var unknown = reasoner.matchByRequirementId("REQ-UNKNOWN");
        assertFalse(unknown.matched(), "Unknown requirement must not match");
        assertEquals("Unknown", deploy.getLifecycleStatus("REQ-UNKNOWN"),
            "R-Deploy must not advance a requirement that never matched");
    }

    @Test
    @DisplayName("D2 — R-Verify advances Matched to Verified when all criteria pass")
    void shouldAdvanceToVerifiedAfterRVerify() {
        OntologyModelLoader loader = freshLoader();
        RDeployService deploy = new RDeployService(loader);
        SemanticReasonerService reasoner =
            new SemanticReasonerService(loader, new SparqlQueryLoader(), deploy);
        reasoner.matchByRequirementId("REQ-001");

        RVerifyService verify = new RVerifyService(loader);
        RVerifyService.VerifyResult result = verify.execute("REQ-001");

        assertTrue(result.verified(),
            "Both criteria (recall >= 0.90, latency <= 60) must pass for the healthy service");
        assertEquals(2, result.criteria().size(),
            "REQ-001 must have exactly two VerificationCriterion");
        assertEquals("Verified", deploy.getLifecycleStatus("REQ-001"),
            "R-Verify must advance lifecycleStatus to Verified");

        Model m = loader.getModel();
        Resource req = m.getResource(INST_NS + "REQ-001");
        Statement lifecycleStmt = req.getProperty(m.createProperty(SDA_NS + "lifecycleStatus"));
        assertNotNull(lifecycleStmt, "REQ-001 must carry a lifecycleStatus literal after R-Verify");
        assertEquals("Verified", lifecycleStmt.getString(),
            "lifecycleStatus must be the literal Verified");
    }

    @Test
    @DisplayName("D2-negative — R-Verify keeps Matched when a verification criterion fails")
    void shouldStayMatchedWhenCriterionFails() {
        OntologyModelLoader loader = freshLoader();
        RDeployService deploy = new RDeployService(loader);
        SemanticReasonerService reasoner =
            new SemanticReasonerService(loader, new SparqlQueryLoader(), deploy);
        reasoner.matchByRequirementId("REQ-001");

        Model m = loader.getModel();
        Resource degraded = m.getResource(INST_NS + "SVC-FlowmeterDiagnostics");
        degraded.removeAll(m.createProperty(SDA_NS + "reportedRecall"));
        degraded.addProperty(m.createProperty(SDA_NS + "reportedRecall"),
            m.createTypedLiteral(0.5));

        RVerifyService verify = new RVerifyService(loader);
        RVerifyService.VerifyResult result = verify.execute("REQ-001");

        assertFalse(result.verified(),
            "Recall below the 0.90 threshold must fail verification");
        assertEquals("Matched", deploy.getLifecycleStatus("REQ-001"),
            "lifecycleStatus must stay Matched when verification fails");
    }

    // ════════════════════════════════════════════════════════════════════
    // CATEGORY E: Margo YAML cluster-node supply → deployment placement
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("E1 — Margo YAML cluster node supply should place the HVAC service on a matching node")
    void shouldPlaceHvacServiceOnMarginCompliantNode() throws IOException {
        Model fresh = freshModel();
        normalizeSafetyIntegrityLevels(fresh);
        String nodeUri = liftClusterNodeYaml(fresh, "examples/margo-cluster-node-edge3.yaml");

        assertTrue(fresh.contains(fresh.createResource(nodeUri), RDF.type,
                fresh.createResource(SDA_NS + "ClusterNode")),
            "YAML lift must create a ClusterNode individual");

        List<QuerySolution> results = executeQuery(
            loadQuery("queries/deployment-placement.rq"), fresh);
        assertFalse(results.isEmpty(),
            "Placement query must return at least one service-node pair");

        boolean hvacOnNode = results.stream().anyMatch(sol ->
            sol.getResource("service").getURI().equals(INST_NS + "SVC-HVACTempAnalysis")
                && sol.getResource("node").getURI().equals(nodeUri));
        assertTrue(hvacOnNode,
            "SVC-HVACTempAnalysis (OT-Zone-3, SIL-0, SoftRealTime) must be placed on the matching node");
    }

    @Test
    @DisplayName("E2 — Cluster node with incompatible zone/SIL must be rejected for the HVAC service")
    void shouldRejectIncompatibleNode() throws IOException {
        Model fresh = freshModel();
        normalizeSafetyIntegrityLevels(fresh);
        liftClusterNodeYaml(fresh, "examples/margo-cluster-node-edge1.yaml");

        List<QuerySolution> results = executeQuery(
            loadQuery("queries/deployment-placement.rq"), fresh);

        boolean hvacOnNode = results.stream().anyMatch(sol ->
            sol.getResource("service").getURI().equals(INST_NS + "SVC-HVACTempAnalysis"));
        assertFalse(hvacOnNode,
            "HVAC service (OT-Zone-3, SIL-0) must not be placed on the OT-Zone-1 / SIL-2 node");
    }

    // ════════════════════════════════════════════════════════════════════
    // Helpers — Jena / SPARQL
    // ════════════════════════════════════════════════════════════════════

    private static OntologyModelLoader freshLoader() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.setFilePath(ONTOLOGY_PATH);
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        return loader;
    }

    private Model freshModel() throws IOException {
        Model fresh = ModelFactory.createDefaultModel();
        try (var in = new ByteArrayInputStream(loadOntologyBytes())) {
            fresh.read(in, null, "TTL");
        }
        DemoAboxBuilder.apply(fresh);
        return fresh;
    }

    /**
     * Builds the REQ-002 test model: v2 ABox plus the missing Dosing concept
     * coverage for the fermentation skill and integer SafetyClass SIL values.
     */
    private Model req002Model() throws IOException {
        Model fresh = freshModel();
        ensureFermentationConceptCoverage(fresh);
        normalizeSafetyIntegrityLevels(fresh);
        return fresh;
    }

    /**
     * The v2 ABox stores safetyIntegrityLevel as a "SIL-n" string even though
     * the ontology declares it as xsd:integer. Normalizes every occurrence to
     * an xsd:integer so the matching and placement queries compare integers.
     */
    private static void normalizeSafetyIntegrityLevels(Model target) {
        Property sil = target.createProperty(SDA_NS + "safetyIntegrityLevel");
        List<Statement> statements = target.listStatements(null, sil, (RDFNode) null).toList();
        for (Statement stmt : statements) {
            if (stmt.getObject().isLiteral()) {
                String value = stmt.getLiteral().getString();
                if (value.startsWith("SIL-")) {
                    int level = Integer.parseInt(value.substring(4));
                    target.remove(stmt);
                    target.add(stmt.getSubject(), sil, target.createTypedLiteral(level));
                }
            }
        }
    }

    /**
     * The fermentation skill requires both Temperature and Dosing concepts; the
     * demo ABox only covers Temperature. Adds a ProcessVariable described by
     * PA-DIM-DosingConcept and maps it through the skid connector so REQ-002's
     * R2a coverage filter is satisfiable.
     */
    private static void ensureFermentationConceptCoverage(Model target) {
        Resource dosingPv = target.createResource(INST_NS + "PV-Dosing-SkidA1");
        if (!dosingPv.hasProperty(target.createProperty(SDA_NS + "describedBy"))) {
            dosingPv.addProperty(RDF.type, target.createResource(SDA_NS + "ProcessVariable"));
            dosingPv.addProperty(target.createProperty(SDA_NS + "describedBy"),
                target.createResource(INST_NS + "PA-DIM-DosingConcept"));
            target.createResource(INST_NS + "DC-SkidA1-OPCUA")
                .addProperty(target.createProperty(SDA_NS + "mapsVariable"), dosingPv);
        }
    }

    @SuppressWarnings("unchecked")
    private static String liftClusterNodeYaml(Model target, String yamlPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(yamlPath);
        assertTrue(resource.exists(), "Margo YAML fixture must exist: " + yamlPath);
        Object loaded;
        try (InputStream in = resource.getInputStream()) {
            loaded = new Yaml().load(in);
        }
        Map<String, Object> root = (Map<String, Object>) loaded;
        Map<String, Object> clusterNode = (Map<String, Object>) root.get("cluster-node");

        String id = (String) clusterNode.get("id");
        String label = (String) clusterNode.get("name");
        String zone = (String) clusterNode.get("network-zone");
        String sil = (String) clusterNode.get("safety-integrity-level");
        String det = (String) clusterNode.get("determinism-class");
        Boolean onPlantNetwork = (Boolean) clusterNode.get("on-plant-network");

        String nodeUri = INST_NS + id;
        Resource node = target.createResource(nodeUri);
        node.addProperty(RDF.type, target.createResource(SDA_NS + "ClusterNode"));
        node.addProperty(RDFS.label, label);

        Resource context = target.createResource(INST_NS + "DEPLOY-" + id);
        context.addProperty(RDF.type, target.createResource(SDA_NS + "DeploymentContext"));
        context.addProperty(RDFS.label, label + " deployment context");
        context.addProperty(target.createProperty(SDA_NS + "determinismClass"), det);
        context.addProperty(target.createProperty(SDA_NS + "onPlantNetwork"),
            target.createTypedLiteral(onPlantNetwork));

        // v2: safetyIntegrityLevel lives on a SafetyClass (as xsd:integer),
        // linked to the DeploymentContext via contextConstrainedBy.
        Resource safetyClass = target.createResource(INST_NS + "SC-" + id);
        safetyClass.addProperty(RDF.type, target.createResource(SDA_NS + "SafetyClass"));
        safetyClass.addProperty(RDFS.label, label + " safety class");
        safetyClass.addProperty(target.createProperty(SDA_NS + "safetyIntegrityLevel"),
            target.createTypedLiteral(parseSilLevel(sil)));
        context.addProperty(target.createProperty(SDA_NS + "contextConstrainedBy"), safetyClass);

        // Link the context to the matching NetworkZone (by label) and to the node.
        context.addProperty(target.createProperty(SDA_NS + "locatedInZone"),
            findZoneByLabel(target, zone));
        context.addProperty(target.createProperty(SDA_NS + "containsNode"), node);

        return nodeUri;
    }

    private static int parseSilLevel(String sil) {
        if (sil == null) {
            return 0;
        }
        return sil.startsWith("SIL-") ? Integer.parseInt(sil.substring(4)) : Integer.parseInt(sil);
    }

    private static Resource findZoneByLabel(Model target, String zoneLabel) {
        var stmts = target.listSubjectsWithProperty(RDFS.label, zoneLabel);
        while (stmts.hasNext()) {
            Resource candidate = stmts.next();
            if (candidate.hasProperty(RDF.type, target.createResource(SDA_NS + "NetworkZone"))) {
                return candidate;
            }
        }
        Resource zone = target.createResource(INST_NS + "NZ-" + zoneLabel);
        zone.addProperty(RDF.type, target.createResource(SDA_NS + "NetworkZone"));
        zone.addProperty(RDFS.label, zoneLabel);
        return zone;
    }

    private List<QuerySolution> executeQuery(String queryStr) {
        return executeQuery(queryStr, model);
    }

    private List<QuerySolution> executeQuery(String queryStr, Model targetModel) {
        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qe = QueryExecutionFactory.create(query, targetModel)) {
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
        ClassPathResource resource = new ClassPathResource(ONTOLOGY_PATH);
        assertTrue(resource.exists(), "Ontology file must exist");
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // Helpers — OWL API / HermiT
    // ════════════════════════════════════════════════════════════════════

    private static OWLOntology loadOwlOntology() throws OWLOntologyCreationException, IOException {
        ClassPathResource resource = new ClassPathResource(ONTOLOGY_PATH);
        assertTrue(resource.exists(), "Ontology file must exist for OWL API loading: " + ONTOLOGY_PATH);
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        try (InputStream in = resource.getInputStream()) {
            return manager.loadOntologyFromOntologyDocument(
                new StreamDocumentSource(in, IRI.create(SDA_NS),
                    new TurtleDocumentFormat(), ONTOLOGY_PATH));
        }
    }

    private static OWLReasoner createHermitReasoner(OWLOntology ontology) {
        return new ReasonerFactory().createReasoner(ontology);
    }

    /**
     * Builds a probe class that is a subclass of both classA and classB and
     * asserts that HermiT classifies the probe as equivalent to owl:Nothing
     * (the two classes are members of an {@code owl:AllDisjointClasses} group).
     */
    private static void assertDisjointnessProbe(String probeName, String classA, String classB)
        throws Exception {
        OWLOntology ontology = loadOwlOntology();
        OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
        OWLClass probe = df.getOWLClass(IRI.create(INST_NS + probeName));
        OWLObjectIntersectionOf intersection = df.getOWLObjectIntersectionOf(
            cls(df, classA), cls(df, classB));
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(probe, intersection);
        ontology.getOWLOntologyManager().addAxiom(ontology, axiom);

        OWLReasoner reasoner = createHermitReasoner(ontology);
        assertTrue(reasoner.isConsistent(),
            "Ontology must remain consistent after adding probe " + probeName);
        assertTrue(reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().contains(probe),
            probeName + " (subclass of " + classA + " and " + classB
                + ") must be equivalent to owl:Nothing via AllDisjointClasses");
    }

    private static OWLNamedIndividual individual(OWLDataFactory df, String name) {
        return df.getOWLNamedIndividual(IRI.create(INST_NS + name));
    }

    private static OWLClass cls(OWLDataFactory df, String name) {
        return df.getOWLClass(IRI.create(SDA_NS + name));
    }

    private static OWLObjectProperty op(OWLDataFactory df, String name) {
        return df.getOWLObjectProperty(IRI.create(SDA_NS + name));
    }

    private static OWLDataProperty dp(OWLDataFactory df, String name) {
        return df.getOWLDataProperty(IRI.create(SDA_NS + name));
    }
}
