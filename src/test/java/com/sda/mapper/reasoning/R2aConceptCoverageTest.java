package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.support.DemoAboxBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("R2a requiresConcept filter — concept coverage blocks unmatched skills")
class R2aConceptCoverageTest {

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "https://example.org/ontology/sda#";

    private OntologyModelLoader loader;
    private SemanticReasonerService reasoner;

    @BeforeEach
    void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-unified.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        var queryLoader = new SparqlQueryLoader();
        var deployService = new RDeployService(loader);
        reasoner = new SemanticReasonerService(loader, queryLoader, deployService);
    }

    @Test
    @DisplayName("REQ-001 should match when DC-Flowmeter-FT101 covers required concept")
    void shouldMatchWhenConceptIsProvided() {
        Model model = loader.getModel();

        boolean hasRequiresConcept = model.contains(
            model.createResource(INST_NS + "SK-FlowmeterDiagnostics"),
            model.createProperty(SDA_NS + "requiresConcept"),
            model.createResource(INST_NS + "PA-DIM-FlowConcept"));

        boolean hasConceptCoverage = hasConceptCoverage(model,
            INST_NS + "DC-Flowmeter-FT101", INST_NS + "PA-DIM-FlowConcept");

        assertTrue(hasRequiresConcept, "SK-FlowmeterDiagnostics should requireConcept :PA-DIM-FlowConcept");
        assertTrue(hasConceptCoverage,
            "DC-Flowmeter-FT101 should cover :PA-DIM-FlowConcept via mapsVariable + describedBy");

        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertTrue(result.matched(),
            "REQ-001 should match because DC-Flowmeter-FT101 maps a ProcessVariable described by the concept SK-FlowmeterDiagnostics requires");
    }

    @Test
    @DisplayName("REQ-001 should NOT match when DC-Flowmeter-FT101 no longer covers required concept")
    void shouldNotMatchWhenConceptIsRemoved() {
        Model model = loader.getModel();

        assertTrue(hasConceptCoverage(model,
            INST_NS + "DC-Flowmeter-FT101", INST_NS + "PA-DIM-FlowConcept"),
            "Pre-condition: DC-Flowmeter-FT101 should cover the concept before removal");

        removeConceptCoverage(model, INST_NS + "DC-Flowmeter-FT101", INST_NS + "PA-DIM-FlowConcept");

        assertFalse(hasConceptCoverage(model,
            INST_NS + "DC-Flowmeter-FT101", INST_NS + "PA-DIM-FlowConcept"),
            "DC-Flowmeter-FT101 should no longer cover the concept after removal");

        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertFalse(result.matched(),
            "REQ-001 should NOT match because no DataConnector covers :PA-DIM-FlowConcept which SK-FlowmeterDiagnostics requires");
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

        boolean anyCoversMissing = model.contains(null,
            model.createProperty(SDA_NS + "describedBy"), missingConcept);

        assertFalse(anyCoversMissing,
            "No ProcessVariable should be described by :PA-DIM-MissingConcept");

        ServiceMatchResult result = reasoner.matchByRequirementId("REQ-001");
        assertFalse(result.matched(),
            "REQ-001 should NOT match because :PA-DIM-MissingConcept is required but not covered");
    }

    @Test
    @DisplayName("REQ-001 should match again after restoring concept coverage")
    void shouldMatchAgainAfterRestoringConcept() {
        Model model = loader.getModel();

        removeConceptCoverage(model, INST_NS + "DC-Flowmeter-FT101", INST_NS + "PA-DIM-FlowConcept");
        assertFalse(hasConceptCoverage(model,
            INST_NS + "DC-Flowmeter-FT101", INST_NS + "PA-DIM-FlowConcept"),
            "Removed concept coverage");

        ServiceMatchResult noMatch = reasoner.matchByRequirementId("REQ-001");
        assertFalse(noMatch.matched(), "Should not match after removing concept coverage");

        restoreConceptCoverage(model, INST_NS + "DC-Flowmeter-FT101", INST_NS + "PA-DIM-FlowConcept");
        assertTrue(hasConceptCoverage(model,
            INST_NS + "DC-Flowmeter-FT101", INST_NS + "PA-DIM-FlowConcept"),
            "Restored concept coverage");

        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());

        ServiceMatchResult match = reasoner.matchByRequirementId("REQ-001");
        assertTrue(match.matched(),
            "Should match again after restoring concept coverage");
    }

    private static boolean hasConceptCoverage(Model model, String connectorUri, String conceptUri) {
        Property mapsVariable = model.createProperty(SDA_NS + "mapsVariable");
        Property describedBy = model.createProperty(SDA_NS + "describedBy");
        Resource connector = model.createResource(connectorUri);
        Resource concept = model.createResource(conceptUri);

        return model.listStatements(connector, mapsVariable, (RDFNode) null).toList().stream()
            .anyMatch(stmt -> stmt.getObject().isResource()
                && stmt.getResource().hasProperty(describedBy, concept));
    }

    private static void removeConceptCoverage(Model model, String connectorUri, String conceptUri) {
        Property mapsVariable = model.createProperty(SDA_NS + "mapsVariable");
        Property describedBy = model.createProperty(SDA_NS + "describedBy");
        Resource connector = model.createResource(connectorUri);

        for (var stmt : model.listStatements(connector, mapsVariable, (RDFNode) null).toList()) {
            if (stmt.getObject().isResource()) {
                model.remove(stmt.getResource(), describedBy, model.createResource(conceptUri));
            }
        }
    }

    private static void restoreConceptCoverage(Model model, String connectorUri, String conceptUri) {
        Property mapsVariable = model.createProperty(SDA_NS + "mapsVariable");
        Property describedBy = model.createProperty(SDA_NS + "describedBy");
        Resource connector = model.createResource(connectorUri);

        for (var stmt : model.listStatements(connector, mapsVariable, (RDFNode) null).toList()) {
            if (stmt.getObject().isResource()) {
                stmt.getResource().addProperty(describedBy, model.createResource(conceptUri));
            }
        }
    }
}
