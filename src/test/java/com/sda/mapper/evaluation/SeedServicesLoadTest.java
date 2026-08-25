package com.sda.mapper.evaluation;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the runtime seed ABox (the split {@code ontology/seed-services.ttl} service
 * side plus {@code ontology/seed-requirements.ttl} requirement side) loads together
 * with the TBox and that every paper use-case requirement matches its expected SDA
 * service via {@link SemanticReasonerService}. Mirrors the production ontology wiring
 * in {@code application.yml} ({@code app.ontology.file-paths}).
 */
@DisplayName("Runtime Seed Services — paper use-case matching")
class SeedServicesLoadTest {

    private static SemanticReasonerService reasoner;

    @BeforeAll
    static void setUp() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.setFilePaths(List.of(
            "ontology/sda_ontology_v2_structured.ttl",
            "ontology/seed-services.ttl",
            "ontology/seed-requirements.ttl"));
        loader.loadOntology();
        var queryLoader = new SparqlQueryLoader();
        var rDeployService = new RDeployService(loader);
        reasoner = new SemanticReasonerService(loader, queryLoader, rDeployService);
    }

    @Test
    @DisplayName("REQ-001 should match Flowmeter Diagnostics Service")
    void req001() {
        ServiceMatchResult r = reasoner.matchByRequirementId("REQ-001");
        assertTrue(r.matched());
        assertEquals("Flowmeter Diagnostics Service", r.serviceName());
    }

    @Test
    @DisplayName("REQ-002 should match Fermentation Skid Service")
    void req002() {
        ServiceMatchResult r = reasoner.matchByRequirementId("REQ-002");
        assertTrue(r.matched());
        assertEquals("Fermentation Skid Service", r.serviceName());
    }

    @Test
    @DisplayName("REQ-003 should match HVAC Temperature Analysis Service")
    void req003() {
        ServiceMatchResult r = reasoner.matchByRequirementId("REQ-003");
        assertTrue(r.matched());
        assertEquals("HVAC Temperature Analysis Service", r.serviceName());
    }

    @Test
    @DisplayName("REQ-004 should match Process Variable Analytics Service")
    void req004() {
        ServiceMatchResult r = reasoner.matchByRequirementId("REQ-004");
        assertTrue(r.matched());
        assertEquals("Process Variable Analytics Service", r.serviceName());
    }
}
