package com.sda.mapper.ontology;

import com.sda.mapper.support.DemoAboxBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Ontology and SPARQL Query Loading")
class OntologyAndQueryLoadingTest {

    private static final String ONTOLOGY_PATH = "ontology/sda_ontology_v2_structured.ttl";
    private static final String SDA_NS = "http://sda-platform.org/ontology#";
    private static final String INST_NS = "http://sda-platform.org/instance#";

    private Model loadOntology() throws IOException {
        ClassPathResource resource = new ClassPathResource(ONTOLOGY_PATH);
        assertTrue(resource.exists(), "Ontology file must exist on classpath: " + ONTOLOGY_PATH);

        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = resource.getInputStream()) {
            model.read(in, null, "TTL");
        }
        DemoAboxBuilder.apply(model);
        return model;
    }

    @Test
    @DisplayName("Should load ontology TTL file into Jena model")
    void shouldLoadOntologyFile() throws IOException {
        Model model = loadOntology();
        assertFalse(model.isEmpty(), "Ontology model must not be empty");
    }

    @Test
    @DisplayName("Should contain all 8 CSS-SDA core classes")
    void shouldContainAllCssSdaClasses() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(SDA_NS + "Service")),
            "Ontology must contain sda:Service class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "CommunicationBinding")),
            "Ontology must contain sda:CommunicationBinding class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "DataConnector")),
            "Ontology must contain sda:DataConnector class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "DeploymentContext")),
            "Ontology must contain sda:DeploymentContext class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "UseCaseRequirement")),
            "Ontology must contain sda:UseCaseRequirement class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Capability")),
            "Ontology must contain sda:Capability class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Skill")),
            "Ontology must contain sda:Skill class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "UseCase")),
            "Ontology must contain sda:UseCase class");
    }

    @Test
    @DisplayName("Should contain Use Case A instances (REQ-001, SVC-FlowmeterDiagnostics)")
    void shouldContainUseCaseAInstances() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(INST_NS + "REQ-001")),
            "Ontology must contain REQ-001 requirement");
        assertTrue(model.containsResource(model.createResource(INST_NS + "CAP-FlowDiagnostics")),
            "Ontology must contain CAP-FlowDiagnostics capability");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SK-FlowmeterDiagnostics")),
            "Ontology must contain SK-FlowmeterDiagnostics skill");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-FlowmeterDiagnostics")),
            "Ontology must contain SVC-FlowmeterDiagnostics service");
        assertTrue(model.containsResource(model.createResource(INST_NS + "CB-FT101-OPCUA")),
            "Ontology must contain CB-FT101-OPCUA binding");
        assertTrue(model.containsResource(model.createResource(INST_NS + "DC-FT101-OPCUA")),
            "Ontology must contain DC-FT101-OPCUA connector");
        assertTrue(model.containsResource(model.createResource(INST_NS + "DEPLOY-OnPrem")),
            "Ontology must contain DEPLOY-OnPrem context");
    }

    @Test
    @DisplayName("Should contain Use Case B instances (REQ-002, SVC-FermentationSkid)")
    void shouldContainUseCaseBInstances() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(INST_NS + "REQ-002")),
            "Ontology must contain REQ-002 requirement");
        assertTrue(model.containsResource(model.createResource(INST_NS + "CAP-FermentationControl")),
            "Ontology must contain CAP-FermentationControl capability");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SK-FermentationControl")),
            "Ontology must contain SK-FermentationControl skill");
        assertTrue(model.containsResource(model.createResource(INST_NS + "PA-DIM-DosingConcept")),
            "Ontology must contain PA-DIM-DosingConcept semantic concept");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-FermentationSkid")),
            "Ontology must contain SVC-FermentationSkid service");
        assertTrue(model.containsResource(model.createResource(INST_NS + "DEPLOY-Critical")),
            "Ontology must contain DEPLOY-Critical context");
        assertTrue(model.containsResource(model.createResource(INST_NS + "Skid-A1")),
            "Ontology must contain Skid-A1 asset");
    }

    @Test
    @DisplayName("Should contain Use Case C instances (REQ-003, SVC-HVACTempAnalysis)")
    void shouldContainUseCaseCInstances() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(INST_NS + "REQ-003")),
            "Ontology must contain REQ-003 requirement");
        assertTrue(model.containsResource(model.createResource(INST_NS + "CAP-HVACTempMonitoring")),
            "Ontology must contain CAP-HVACTempMonitoring capability");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SK-HVACTempAnalysis")),
            "Ontology must contain SK-HVACTempAnalysis skill");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-HVACTempAnalysis")),
            "Ontology must contain SVC-HVACTempAnalysis service");
        assertTrue(model.containsResource(model.createResource(INST_NS + "AHU01")),
            "Ontology must contain AHU01 asset");
        assertTrue(model.containsResource(model.createResource(INST_NS + "DEPLOY-OTZone3")),
            "Ontology must contain DEPLOY-OTZone3 context");
    }

    @Test
    @DisplayName("Should contain Layer 4 diagnostic individuals (Asset, Sensor, Observation, ProcessVariable)")
    void shouldContainLayer4DiagnosticClasses() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(SDA_NS + "Asset")),
            "Ontology must contain Asset class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Sensor")),
            "Ontology must contain Sensor class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Observation")),
            "Ontology must contain Observation class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "ProcessVariable")),
            "Ontology must contain ProcessVariable class");

        assertTrue(model.containsResource(model.createResource(INST_NS + "FT101")),
            "Ontology must contain FT101 asset individual");
        assertTrue(model.containsResource(model.createResource(INST_NS + "Sensor-FT101")),
            "Ontology must contain Sensor-FT101 sensor individual");
        assertTrue(model.containsResource(model.createResource(INST_NS + "PV-Flow-FT101")),
            "Ontology must contain PV-Flow-FT101 process variable individual");
        assertTrue(model.containsResource(model.createResource(INST_NS + "OBS-FT101-1")),
            "Ontology must contain OBS-FT101-1 observation individual");
    }

    @Test
    @DisplayName("Should contain negative candidates for SPARQL exclusion tests")
    void shouldContainNegativeCandidates() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-Orphan")),
            "Ontology must contain SVC-Orphan negative candidate");
    }

    @Test
    @DisplayName("Should parse and execute SPARQL query for REQ-001")
    void shouldExecuteSparqlQueryForReq001() throws IOException {
        Model model = loadOntology();
        String queryStr = loadQuery("queries/match-service.rq");

        Query query = QueryFactory.create(queryStr);
        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            List<QuerySolution> solutions = new ArrayList<>();
            while (rs.hasNext()) {
                solutions.add(rs.next());
            }

            assertFalse(solutions.isEmpty(),
                "SPARQL query must return at least one result for REQ-001");

            QuerySolution sol = solutions.getFirst();
            assertEquals(INST_NS + "SVC-FlowmeterDiagnostics",
                sol.getResource("service").getURI(),
                "Query must return SVC-FlowmeterDiagnostics for REQ-001");
            assertEquals("OPC-UA",
                sol.getLiteral("protocol").getString(),
                "Returned service must use OPC-UA protocol");
            assertEquals("read-only",
                sol.getLiteral("accessMode").getString(),
                "Returned service must use read-only access mode");
        }
    }

    @Test
    @DisplayName("Should contain matching classes and datatype properties (v2 vocabulary)")
    void shouldContainMatchingVocabulary() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(SDA_NS + "UseCaseRequirement")),
            "Ontology must contain sda:UseCaseRequirement class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "UseCaseRequirementConstraint")),
            "Ontology must contain sda:UseCaseRequirementConstraint class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "UseCaseRequirementParameter")),
            "Ontology must contain sda:UseCaseRequirementParameter class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Capability")),
            "Ontology must contain sda:Capability class");

        assertTrue(model.containsResource(model.createResource(SDA_NS + "capabilityID")),
            "Ontology must contain sda:capabilityID property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "capabilityName")),
            "Ontology must contain sda:capabilityName property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "capabilityDescription")),
            "Ontology must contain sda:capabilityDescription property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "capabilityVersion")),
            "Ontology must contain sda:capabilityVersion property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "protocol")),
            "Ontology must contain sda:protocol property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "accessMode")),
            "Ontology must contain sda:accessMode property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "reportedRecall")),
            "Ontology must contain sda:reportedRecall property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "reportedLatency")),
            "Ontology must contain sda:reportedLatency property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "onPlantNetwork")),
            "Ontology must contain sda:onPlantNetwork property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "determinismClass")),
            "Ontology must contain sda:determinismClass property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "safetyIntegrityLevel")),
            "Ontology must contain sda:safetyIntegrityLevel property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "isHardConstraint")),
            "Ontology must contain sda:isHardConstraint property");
    }

    @Test
    @DisplayName("Should parse SPARQL query file with Jena QueryFactory")
    void shouldParseAllSparqlQueries() throws IOException {
        String[] queryFiles = {
            "queries/match-service.rq",
            "queries/match-service-req002.rq",
            "queries/match-service-req003.rq",
            "queries/cross-plant-temperature.rq",
            "queries/hub-match-offered-required.rq"
        };

        for (String queryFile : queryFiles) {
            String queryStr = loadQuery(queryFile);
            Query query = QueryFactory.create(queryStr);
            assertNotNull(query, "Parsed query must not be null for: " + queryFile);
            assertTrue(query.isSelectType(), "Query must be a SELECT query: " + queryFile);
        }
    }

    private String loadQuery(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertTrue(resource.exists(), "SPARQL query file must exist on classpath: " + path);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes());
        }
    }
}
