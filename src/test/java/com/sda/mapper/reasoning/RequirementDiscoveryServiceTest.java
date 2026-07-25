package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RequirementDiscoveryService ontology query")
class RequirementDiscoveryServiceTest {

    private static RequirementDiscoveryService discoveryService;

    @BeforeAll
    static void setUp() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        var queryLoader = new SparqlQueryLoader();
        discoveryService = new RequirementDiscoveryService(loader, queryLoader);
    }

    @Test
    @DisplayName("Should discover zero requirements from empty ontology")
    void shouldDiscoverZeroRequirements() {
        List<String> ids = discoveryService.discoverRequirementIds();
        assertEquals(0, ids.size(),
            "Ontology should contain no built-in requirements, found: " + ids);
    }

    @Test
    @DisplayName("Should return empty list when ontology not loaded")
    void shouldReturnEmptyWhenNotLoaded() {
        var unloadedLoader = new OntologyModelLoader();
        var queryLoader = new SparqlQueryLoader();
        var service = new RequirementDiscoveryService(unloadedLoader, queryLoader);
        List<String> ids = service.discoverRequirementIds();
        assertNotNull(ids);
        assertTrue(ids.isEmpty(), "Should return empty list when ontology is not loaded");
    }

    @Test
    @DisplayName("Should return empty list for empty ontology")
    void shouldReturnEmptyForEmptyOntology() {
        List<String> ids = discoveryService.discoverRequirementIds();
        assertNotNull(ids);
        assertTrue(ids.isEmpty(), "Should return empty list when no requirements exist");
    }
}
