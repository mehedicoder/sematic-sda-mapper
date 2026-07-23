package com.sda.mapper.ontology;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OntologyConflictDetector deduplication logic")
class OntologyConflictDetectorTest {

    private static OntologyConflictDetector detector;
    private static Model ontologyModel;

    @BeforeAll
    static void setUp() {
        detector = new OntologyConflictDetector();
        var loader = new OntologyModelLoader();
        loader.setFilePaths(List.of("ontology/css-sda-small.ttl"));
        loader.loadOntology();
        ontologyModel = loader.getModel();
    }

    @Test
    @DisplayName("Should return empty list when no conflicts exist")
    void shouldReturnEmptyWhenNoConflicts() {
        Model newModel = ModelFactory.createDefaultModel();
        Resource svc = newModel.createResource("http://www.example.org/instance#SVC-NewUnique");
        svc.addProperty(RDFS.label, "Completely Unique Service");
        svc.addProperty(RDF.type, newModel.createResource("http://www.example.org/sda#Service"));

        List<String> conflicts = detector.detectConflicts(newModel, ontologyModel);
        assertTrue(conflicts.isEmpty(), "Should not conflict with unique label");
    }

    @Test
    @DisplayName("Should detect conflict when same label and type exist")
    void shouldDetectConflict() {
        Model newModel = ModelFactory.createDefaultModel();
        Resource svc = newModel.createResource("http://www.example.org/instance#SVC-Duplicate");
        svc.addProperty(RDFS.label, "Flowmeter Diagnostics Service");
        svc.addProperty(RDF.type, newModel.createResource("http://www.example.org/sda#Service"));

        List<String> conflicts = detector.detectConflicts(newModel, ontologyModel);
        assertFalse(conflicts.isEmpty(), "Should detect conflict with existing service label");
    }

    @Test
    @DisplayName("Should not conflict when same label but different type")
    void shouldNotConflictDifferentType() {
        Model newModel = ModelFactory.createDefaultModel();
        Resource req = newModel.createResource("http://www.example.org/instance#REQ-New");
        req.addProperty(RDFS.label, "Flowmeter Diagnostics Service");
        req.addProperty(RDF.type, newModel.createResource("http://www.example.org/sda#Requirement"));

        List<String> conflicts = detector.detectConflicts(newModel, ontologyModel);
        assertTrue(conflicts.isEmpty(),
            "Same label with different type should not conflict");
    }

    @Test
    @DisplayName("Should not conflict when same type but different label")
    void shouldNotConflictDifferentLabel() {
        Model newModel = ModelFactory.createDefaultModel();
        Resource svc = newModel.createResource("http://www.example.org/instance#SVC-New2");
        svc.addProperty(RDFS.label, "Brand New Unique Service Name");
        svc.addProperty(RDF.type, newModel.createResource("http://www.example.org/sda#Service"));

        List<String> conflicts = detector.detectConflicts(newModel, ontologyModel);
        assertTrue(conflicts.isEmpty(),
            "Same type with different label should not conflict");
    }

    @Test
    @DisplayName("Should handle model with no type statements gracefully")
    void shouldHandleUntypedModel() {
        Model newModel = ModelFactory.createDefaultModel();
        Resource r = newModel.createResource("http://example.org/Untyped");
        r.addProperty(RDFS.label, "Some Label");

        List<String> conflicts = detector.detectConflicts(newModel, ontologyModel);
        assertTrue(conflicts.isEmpty(), "Untyped resources should not cause conflicts");
    }

    @Test
    @DisplayName("Should handle model with no label gracefully")
    void shouldHandleLabellessModel() {
        Model newModel = ModelFactory.createDefaultModel();
        Resource svc = newModel.createResource("http://example.org/NoLabel");
        svc.addProperty(RDF.type, newModel.createResource("http://www.example.org/sda#Service"));

        List<String> conflicts = detector.detectConflicts(newModel, ontologyModel);
        // Should use localName, which is unique
        assertTrue(conflicts.isEmpty(),
            "Resource using localName as fallback should not conflict");
    }
}
