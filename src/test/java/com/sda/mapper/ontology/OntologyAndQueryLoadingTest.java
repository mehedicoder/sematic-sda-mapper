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

    private static final String ONTOLOGY_PATH = "ontology/css-sda-unified.ttl";
    private static final String SDA_NS = "https://example.org/ontology/sda#";
    private static final String INST_NS = "http://www.example.org/instance#";

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
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Requirement")),
            "Ontology must contain sda:Requirement class");
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
        assertTrue(model.containsResource(model.createResource(INST_NS + "OC-ACM-Flowmeter")),
            "Ontology must contain OC-ACM-Flowmeter capability");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SK-FlowmeterDiagnostics")),
            "Ontology must contain SK-FlowmeterDiagnostics skill");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-FlowmeterDiagnostics")),
            "Ontology must contain SVC-FlowmeterDiagnostics service");
        assertTrue(model.containsResource(model.createResource(INST_NS + "CB-Flowmeter-OPCUA")),
            "Ontology must contain CB-Flowmeter-OPCUA binding");
        assertTrue(model.containsResource(model.createResource(INST_NS + "DC-Flowmeter-FT101")),
            "Ontology must contain DC-Flowmeter-FT101 connector");
        assertTrue(model.containsResource(model.createResource(INST_NS + "DEPLOY-OnPrem")),
            "Ontology must contain DEPLOY-OnPrem context");
    }

    @Test
    @DisplayName("Should contain Use Case B instances (REQ-002, SVC-FermentationSkid)")
    void shouldContainUseCaseBInstances() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(INST_NS + "REQ-002")),
            "Ontology must contain REQ-002 requirement");
        assertTrue(model.containsResource(model.createResource(INST_NS + "OC-TempControl")),
            "Ontology must contain OC-TempControl capability");
        assertTrue(model.containsResource(model.createResource(INST_NS + "OC-DosingControl")),
            "Ontology must contain OC-DosingControl capability");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SK-FermentationControl")),
            "Ontology must contain SK-FermentationControl skill");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SK-DosingControl")),
            "Ontology must contain SK-DosingControl skill");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-FermentationSkid")),
            "Ontology must contain SVC-FermentationSkid service");
        assertTrue(model.containsResource(model.createResource(INST_NS + "DEPLOY-Critical")),
            "Ontology must contain DEPLOY-Critical context");
        assertTrue(model.containsResource(model.createResource(INST_NS + "Skid-A1")),
            "Ontology must contain Skid-A1 MTPModule");
    }

    @Test
    @DisplayName("Should contain Use Case C instances (REQ-003, SVC-HVACTempAnalysis)")
    void shouldContainUseCaseCInstances() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(INST_NS + "REQ-003")),
            "Ontology must contain REQ-003 requirement");
        assertTrue(model.containsResource(model.createResource(INST_NS + "OC-HVACTempMonitoring")),
            "Ontology must contain OC-HVACTempMonitoring capability");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SK-HVACTempAnalysis")),
            "Ontology must contain SK-HVACTempAnalysis skill");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-HVACTempAnalysis")),
            "Ontology must contain SVC-HVACTempAnalysis service");
        assertTrue(model.containsResource(model.createResource(INST_NS + "AHU-01")),
            "Ontology must contain AHU-01 BACnetDevice");
        assertTrue(model.containsResource(model.createResource(INST_NS + "EM-01")),
            "Ontology must contain EM-01 EnergyMeter");
        assertTrue(model.containsResource(model.createResource(INST_NS + "DEPLOY-OTZone3")),
            "Ontology must contain DEPLOY-OTZone3 context");
    }

    @Test
    @DisplayName("Should contain NE 107 diagnostic classes and individuals")
    void shouldContainNe107DiagnosticClasses() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(SDA_NS + "NE107Status")),
            "Ontology must contain NE107Status class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "NE107SubCode")),
            "Ontology must contain NE107SubCode class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "DegradedMode")),
            "Ontology must contain DegradedMode class");

        assertTrue(model.containsResource(model.createResource(INST_NS + "NE107-Good")),
            "Ontology must contain NE107-Good individual");
        assertTrue(model.containsResource(model.createResource(INST_NS + "NE107-Maintenance")),
            "Ontology must contain NE107-Maintenance individual");
        assertTrue(model.containsResource(model.createResource(INST_NS + "NE107-Failure")),
            "Ontology must contain NE107-Failure individual");

        assertTrue(model.containsResource(model.createResource(INST_NS + "NE107-Sensor")),
            "Ontology must contain NE107-Sensor sub-code");
        assertTrue(model.containsResource(model.createResource(INST_NS + "NE107-WireBreak")),
            "Ontology must contain NE107-WireBreak sub-code");
        assertTrue(model.containsResource(model.createResource(INST_NS + "NE107-ShortCircuit")),
            "Ontology must contain NE107-ShortCircuit sub-code");
        assertTrue(model.containsResource(model.createResource(INST_NS + "NE107-Function")),
            "Ontology must contain NE107-Function sub-code");
    }

    @Test
    @DisplayName("Should contain negative candidates for SPARQL exclusion tests")
    void shouldContainNegativeCandidates() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-WrongProtocol")),
            "Ontology must contain SVC-WrongProtocol negative candidate");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-WrongCapability")),
            "Ontology must contain SVC-WrongCapability negative candidate");
        assertTrue(model.containsResource(model.createResource(INST_NS + "SVC-NE107Failure")),
            "Ontology must contain SVC-NE107Failure negative candidate");
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
    @DisplayName("Should contain capability matching classes and datatype properties")
    void shouldContainCapabilityMatchingVocabulary() throws IOException {
        Model model = loadOntology();

        assertTrue(model.containsResource(model.createResource(SDA_NS + "OfferedCapability")),
            "Ontology must contain sda:OfferedCapability class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "RequiredCapability")),
            "Ontology must contain sda:RequiredCapability class");
        assertTrue(model.getResource(SDA_NS + "OfferedCapability")
                .hasProperty(org.apache.jena.vocabulary.RDFS.subClassOf,
                    model.createResource(SDA_NS + "Capability")),
            "sda:OfferedCapability must be a sub-class of sda:Capability");

        assertTrue(model.containsResource(model.createResource(SDA_NS + "functionClass")),
            "Ontology must contain sda:functionClass property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "engineeringUnit")),
            "Ontology must contain sda:engineeringUnit property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "nominalRange_min")),
            "Ontology must contain sda:nominalRange_min property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "nominalRange_max")),
            "Ontology must contain sda:nominalRange_max property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "safetyIntegrityLevel")),
            "Ontology must contain sda:safetyIntegrityLevel property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "networkZone")),
            "Ontology must contain sda:networkZone property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "protocol")),
            "Ontology must contain sda:protocol property");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "endpointUrl")),
            "Ontology must contain sda:endpointUrl property");
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
