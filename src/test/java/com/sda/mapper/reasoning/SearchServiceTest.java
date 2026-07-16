package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchService ontology search")
class SearchServiceTest {

    private static SearchService searchService;

    @BeforeAll
    static void setUp() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        searchService = new SearchService(loader);
    }

    @Test
    @DisplayName("Should find Flowmeter entities with correct type")
    void shouldFindFlowmeterEntities() {
        List<SearchResult> results = searchService.search("Flowmeter", null);
        assertFalse(results.isEmpty(), "Should find results for Flowmeter");
        assertTrue(results.stream().anyMatch(r -> r.name().contains("Flowmeter")),
                "At least one result should contain Flowmeter in its name");
        assertTrue(results.stream().anyMatch(r -> r.similarityScore() > 0.0),
                "Results should have positive similarity scores");
    }

    @Test
    @DisplayName("Should find REQ-001 requirement")
    void shouldFindRequirementById() {
        List<SearchResult> results = searchService.search("REQ-001", null);
        assertFalse(results.isEmpty(), "Should find REQ-001");
        assertTrue(results.stream().anyMatch(r -> r.ontologyPath().equals("REQ-001")),
                "Should match on local name REQ-001");
    }

    @Test
    @DisplayName("Should filter results by entity type")
    void shouldFilterByEntityType() {
        List<SearchResult> allResults = searchService.search("Flowmeter", null);
        List<SearchResult> requirementOnly = searchService.search("Flowmeter", "Requirement");
        List<SearchResult> serviceOnly = searchService.search("Flowmeter", "Service");

        assertFalse(allResults.isEmpty(), "All results should not be empty");

        for (SearchResult r : requirementOnly) {
            assertEquals("Requirement", r.type(), "Filtered results should match entity type");
        }
        for (SearchResult r : serviceOnly) {
            assertEquals("Service", r.type(), "Filtered results should match entity type");
        }
    }

    @Test
    @DisplayName("Should return empty list for empty query")
    void shouldReturnEmptyForEmptyQuery() {
        List<SearchResult> results = searchService.search("", null);
        assertTrue(results.isEmpty(), "Empty query should return empty list");
    }

    @Test
    @DisplayName("Should return empty list for null query")
    void shouldReturnEmptyForNullQuery() {
        List<SearchResult> results = searchService.search(null, null);
        assertTrue(results.isEmpty(), "Null query should return empty list");
    }

    @Test
    @DisplayName("Should return empty list for nonexistent term")
    void shouldReturnEmptyForNonexistentTerm() {
        List<SearchResult> results = searchService.search("xyzzy_nonexistent_12345", null);
        assertTrue(results.isEmpty(), "Nonexistent term should return empty list");
    }
}
