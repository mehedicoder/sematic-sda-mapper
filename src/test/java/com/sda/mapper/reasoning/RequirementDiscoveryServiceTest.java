package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.support.DemoAboxBuilder;
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
        loader.setFilePath("ontology/css-sda-unified.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        var queryLoader = new SparqlQueryLoader();
        discoveryService = new RequirementDiscoveryService(loader, queryLoader);
    }

    @Test
    @DisplayName("Should discover at least 3 requirements from ontology")
    void shouldDiscoverAtLeast3Requirements() {
        List<String> ids = discoveryService.discoverRequirementIds();
        assertTrue(ids.size() >= 3,
            "Ontology should contain at least REQ-001, REQ-002, REQ-003, found: " + ids);
    }

    @Test
    @DisplayName("Should discover REQ-001 in the result set")
    void shouldDiscoverReq001() {
        List<String> ids = discoveryService.discoverRequirementIds();
        assertTrue(ids.contains("REQ-001"),
            "REQ-001 must be discovered from ontology");
    }

    @Test
    @DisplayName("Should discover REQ-002 in the result set")
    void shouldDiscoverReq002() {
        List<String> ids = discoveryService.discoverRequirementIds();
        assertTrue(ids.contains("REQ-002"),
            "REQ-002 must be discovered from ontology");
    }

    @Test
    @DisplayName("Should discover REQ-003 in the result set")
    void shouldDiscoverReq003() {
        List<String> ids = discoveryService.discoverRequirementIds();
        assertTrue(ids.contains("REQ-003"),
            "REQ-003 must be discovered from ontology");
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
    @DisplayName("Should return requirement IDs in sorted order")
    void shouldReturnSortedIds() {
        List<String> ids = discoveryService.discoverRequirementIds();
        List<String> sorted = ids.stream().sorted().toList();
        assertEquals(sorted, ids, "Requirement IDs should be sorted alphabetically");
    }
}
