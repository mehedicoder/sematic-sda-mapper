package com.sda.mapper.ontology;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Ontology and SPARQL Query Loading")
class OntologyAndQueryLoadingTest {

    private static final String ONTOLOGY_PATH = "ontology/css-sda-small.ttl";
    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";
    private static final String INST_NS = "http://www.example.org/instance#";

    private Model loadOntology() throws IOException {
        ClassPathResource resource = new ClassPathResource(ONTOLOGY_PATH);
        assertTrue(resource.exists(), "Ontology file must exist on classpath: " + ONTOLOGY_PATH);

        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = resource.getInputStream()) {
            model.read(in, null, "TTL");
        }
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
        assertTrue(model.containsResource(model.createResource(CSS_NS + "Capability")),
            "Ontology must contain css:Capability class");
        assertTrue(model.containsResource(model.createResource(CSS_NS + "Skill")),
            "Ontology must contain css:Skill class");
        assertTrue(model.containsResource(model.createResource(CSS_NS + "UseCase")),
            "Ontology must contain css:UseCase class");
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
    @DisplayName("Should parse SPARQL query file with Jena QueryFactory")
    void shouldParseAllSparqlQueries() throws IOException {
        String[] queryFiles = {
            "queries/cross-plant-temperature.rq",
            "queries/discover-requirements.rq",
            "queries/service-catalog.rq"
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
