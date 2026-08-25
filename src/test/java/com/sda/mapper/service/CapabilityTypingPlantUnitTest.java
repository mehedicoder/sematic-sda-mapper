package com.sda.mapper.service;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Positive and negative TBox coverage for the Phase 15 ontology extensions that align
 * the v2 ontology with the paper's Figure 5 capability typing and plant-unit model:
 * {@code :AnalyticalFunction}, {@code :ControlFunction}, {@code :PlantUnit},
 * {@code :requiresFunction}/{@code :requiredByCapability}, and the widened
 * {@code :operatesOn} range ({@code :Asset | :PlantUnit}).
 *
 * <p>These reference concepts are intentionally kept out of the numbered-class
 * catalog (which is bound to the generated canonical-record layer) so the semantic
 * typing stays expressed in RDF/Turtle and SPARQL, per the project engineering rules.
 * They are verified here directly against the loaded Jena model.
 */
class CapabilityTypingPlantUnitTest {

    private static final String SDA_NS = "http://sda-platform.org/ontology#";
    private static Model model;

    @BeforeAll
    static void loadOntology() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.loadOntology();
        model = loader.getModel();
    }

    private Resource r(String local) {
        return model.getResource(SDA_NS + local);
    }

    @Test
    @DisplayName("Should expose the new AnalyticalFunction, ControlFunction and PlantUnit classes (paper Figure 5)")
    void shouldExposeNewClasses() {
        assertTrue(model.contains(r("AnalyticalFunction"), RDF.type, OWL.Class),
            "AnalyticalFunction must be an owl:Class");
        assertTrue(model.contains(r("ControlFunction"), RDF.type, OWL.Class),
            "ControlFunction must be an owl:Class");
        assertTrue(model.contains(r("PlantUnit"), RDF.type, OWL.Class),
            "PlantUnit must be an owl:Class");
    }

    @Test
    @DisplayName("Should NOT expose bogus function classes (negative coverage)")
    void shouldNotExposeBogusClasses() {
        assertFalse(model.contains(r("ImaginaryFunction"), RDF.type, OWL.Class),
            "ImaginaryFunction must not exist in the TBox");
    }

    @Test
    @DisplayName("Should expose requiresFunction as Capability -> AnalyticalFunction | ControlFunction")
    void shouldExposeRequiresFunction() {
        Resource req = r("requiresFunction");
        assertTrue(model.contains(req, RDF.type, OWL.ObjectProperty),
            "requiresFunction must be an owl:ObjectProperty");
        assertTrue(model.listStatements(req, RDFS.domain, r("Capability")).hasNext(),
            "requiresFunction domain must be Capability");
        assertTrue(model.listStatements(req, RDFS.range, r("AnalyticalFunction")).hasNext(),
            "requiresFunction range must include AnalyticalFunction");
        assertTrue(model.listStatements(req, RDFS.range, r("ControlFunction")).hasNext(),
            "requiresFunction range must include ControlFunction");
        assertTrue(model.contains(req, OWL.inverseOf, r("requiredByCapability")),
            "requiresFunction must be inverse of requiredByCapability");
    }

    @Test
    @DisplayName("Should expose requiredByCapability as the inverse of requiresFunction")
    void shouldExposeRequiredByCapability() {
        Resource inv = r("requiredByCapability");
        assertTrue(model.contains(inv, RDF.type, OWL.ObjectProperty),
            "requiredByCapability must be an owl:ObjectProperty");
        assertTrue(model.contains(inv, OWL.inverseOf, r("requiresFunction")),
            "requiredByCapability must be inverse of requiresFunction");
    }

    @Test
    @DisplayName("Should let operatesOn target both Asset and PlantUnit (paper Figure 5)")
    void shouldOperateOnAssetAndPlantUnit() {
        Resource op = r("operatesOn");
        assertTrue(model.listStatements(op, RDFS.domain, r("Capability")).hasNext(),
            "operatesOn domain must be Capability");
        assertTrue(model.listStatements(op, RDFS.range, r("Asset")).hasNext(),
            "operatesOn range must include Asset");
        assertTrue(model.listStatements(op, RDFS.range, r("PlantUnit")).hasNext(),
            "operatesOn range must include PlantUnit");
    }
}
