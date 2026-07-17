package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("R2a requiresConcept filter — concept coverage blocks unmatched skills")
class R2aConceptCoverageTest {

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";

    private OntologyModelLoader loader;
    private SemanticReasonerService reasoner;

    @BeforeEach
    void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        var queryLoader = new SparqlQueryLoader();
        var deployService = new RDeployService(loader);
        reasoner = new SemanticReasonerService(loader, queryLoader, deployService);
    }

    @Test
    @DisplayName("REQ-001 should match when DC-Flowmeter-FT101 provides required concept")
    void shouldMatchWhenConceptIsProvided() {
        Model model = loader.getModel();

        boolean hasRequiresConcept = model.contains(
            model.createResource(INST_NS + "SK-FlowmeterDiagnostics"),
            model.createProperty(SDA_NS + "requiresConcept"),
            model.createResource(INST_NS + "PA-DIM-FlowConcept"));

        boolean hasProvidesConcept = model.contains(
            model.createResource(INST_NS + "DC-Flowmeter-FT101"),
            model.createProperty(SDA_NS + "providesConcept"),
            model.createResource(INST_NS + "PA-DIM-FlowConcept"));

        assertTrue(hasRequiresConcept, "SK-FlowmeterDiagnostics should requireConcept :PA-DIM-FlowConcept");
        assertTrue(hasProvidesConcept, "DC-Flowmeter-FT101 should provideConcept :PA-DIM-FlowConcept");

        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched(),
            "REQ-001 should match because DC-Flowmeter-FT101 provides the concept SK-FlowmeterDiagnostics requires");
    }

    @Test
    @DisplayName("REQ-001 should NOT match when DC-Flowmeter-FT101 no longer provides required concept")
    void shouldNotMatchWhenConceptIsRemoved() {
        Model model = loader.getModel();

        Property providesConcept = model.createProperty(SDA_NS + "providesConcept");
        Resource dc = model.createResource(INST_NS + "DC-Flowmeter-FT101");
        Resource concept = model.createResource(INST_NS + "PA-DIM-FlowConcept");

        assertTrue(dc.hasProperty(providesConcept, concept),
            "Pre-condition: DC-Flowmeter-FT101 should provideConcept before removal");

        model.remove(dc, providesConcept, concept);

        assertFalse(dc.hasProperty(providesConcept, concept),
            "DC-Flowmeter-FT101 should no longer provideConcept after removal");

        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertFalse(result.matched(),
            "REQ-001 should NOT match because no DataConnector provides :PA-DIM-FlowConcept which SK-FlowmeterDiagnostics requires");
    }

    @Test
    @DisplayName("REQ-001 should NOT match when SK-FlowmeterDiagnostics requires an additional unprovided concept")
    void shouldNotMatchWhenAdditionalConceptIsRequired() {
        Model model = loader.getModel();

        Property requiresConcept = model.createProperty(SDA_NS + "requiresConcept");
        Resource skill = model.createResource(INST_NS + "SK-FlowmeterDiagnostics");
        Resource missingConcept = model.createResource(INST_NS + "PA-DIM-MissingConcept");

        skill.addProperty(requiresConcept, missingConcept);

        assertTrue(skill.hasProperty(requiresConcept, missingConcept),
            "Pre-condition: skill should now requireConcept :PA-DIM-MissingConcept");

        boolean anyProvidesMissing = model.contains(null,
            model.createProperty(SDA_NS + "providesConcept"), missingConcept);

        assertFalse(anyProvidesMissing,
            "No DataConnector should provide :PA-DIM-MissingConcept");

        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertFalse(result.matched(),
            "REQ-001 should NOT match because :PA-DIM-MissingConcept is required but not provided");
    }

    @Test
    @DisplayName("REQ-001 should match again after restoring providesConcept")
    void shouldMatchAgainAfterRestoringConcept() {
        Model model = loader.getModel();

        Property providesConcept = model.createProperty(SDA_NS + "providesConcept");
        Resource dc = model.createResource(INST_NS + "DC-Flowmeter-FT101");
        Resource concept = model.createResource(INST_NS + "PA-DIM-FlowConcept");

        model.remove(dc, providesConcept, concept);
        assertFalse(dc.hasProperty(providesConcept, concept), "Removed providesConcept");

        ServiceMatchResult noMatch = reasoner.matchByRequirementId("REQ-001");
        assertFalse(noMatch.matched(), "Should not match after removing concept");

        dc.addProperty(providesConcept, concept);
        assertTrue(dc.hasProperty(providesConcept, concept), "Restored providesConcept");

        loader.loadOntology();

        ServiceMatchResult match = reasoner.matchByRequirementId("REQ-001");
        assertTrue(match.matched(),
            "Should match again after restoring providesConcept");
    }
}
