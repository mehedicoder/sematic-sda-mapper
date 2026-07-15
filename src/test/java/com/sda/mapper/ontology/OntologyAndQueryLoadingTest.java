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
    private static final String QUERY_PATH = "queries/match-service.rq";

    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";

    @Test
    @DisplayName("Should load ontology TTL file into Jena model")
    void shouldLoadOntologyFile() throws IOException {
        ClassPathResource resource = new ClassPathResource(ONTOLOGY_PATH);
        assertTrue(resource.exists(), "Ontology file must exist on classpath: " + ONTOLOGY_PATH);

        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = resource.getInputStream()) {
            model.read(in, null, "TTL");
        }

        assertFalse(model.isEmpty(), "Ontology model must not be empty");

        assertTrue(model.containsResource(model.createResource(SDA_NS + "Service")),
            "Ontology must contain sda:Service class");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "RequiredCapability")),
            "Ontology must contain sda:RequiredCapability class");
        assertTrue(model.containsResource(model.createResource(CSS_NS + "Capability")),
            "Ontology must contain css:Capability class");
    }

    @Test
    @DisplayName("Should parse SPARQL query file with Jena QueryFactory")
    void shouldParseSparqlQuery() throws IOException {
        ClassPathResource resource = new ClassPathResource(QUERY_PATH);
        assertTrue(resource.exists(), "SPARQL query file must exist on classpath: " + QUERY_PATH);

        String queryStr;
        try (InputStream in = resource.getInputStream()) {
            queryStr = new String(in.readAllBytes());
        }

        Query query = QueryFactory.create(queryStr);
        assertNotNull(query, "Parsed query must not be null");
        assertTrue(query.isSelectType(), "Query must be a SELECT query");
        assertFalse(query.getResultVars().isEmpty(), "Query must have at least one result variable");
    }
}
