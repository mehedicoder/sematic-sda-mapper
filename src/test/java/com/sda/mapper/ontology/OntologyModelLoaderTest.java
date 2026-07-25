package com.sda.mapper.ontology;

import com.sda.mapper.error.OntologyLoadingException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OntologyModelLoader")
class OntologyModelLoaderTest {

    @Test
    @DisplayName("Should load ontology from classpath and return non-empty Jena Model")
    void shouldLoadOntologySuccessfully() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();

        assertTrue(loader.isLoaded(), "Loader should report loaded state");
        Model model = loader.getModel();
        assertNotNull(model, "Model must not be null");
        assertFalse(model.isEmpty(), "Model must contain triples");
        assertTrue(model.size() > 50, "Model should contain at least 50 triples");
    }

    @Test
    @DisplayName("Should throw OntologyLoadingException when ontology file is missing")
    void shouldThrowWhenFileMissing() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/nonexistent-file.ttl");

        var ex = assertThrows(OntologyLoadingException.class, loader::loadOntology,
            "Missing ontology file should throw OntologyLoadingException");
        assertTrue(ex.getMessage().contains("not found"),
            "Exception message should mention file not found");
        assertFalse(loader.isLoaded(), "Loader should not report loaded state");
    }

    @Test
    @DisplayName("Should throw OntologyLoadingException when ontology file has invalid Turtle syntax")
    void shouldThrowWhenFileInvalid() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/invalid-syntax.ttl");

        var ex = assertThrows(OntologyLoadingException.class, loader::loadOntology,
            "Invalid Turtle syntax should throw OntologyLoadingException");
        assertTrue(ex.getMessage().contains("invalid Turtle") || ex.getMessage().contains("Failed to parse"),
            "Exception message should mention parse failure");
        assertFalse(loader.isLoaded(), "Loader should not report loaded state");
    }

    @Test
    @DisplayName("Should throw OntologyLoadingException when getModel called before loadOntology")
    void shouldThrowWhenModelNotLoaded() {
        var loader = new OntologyModelLoader();

        assertThrows(OntologyLoadingException.class, loader::getModel,
            "getModel before load should throw OntologyLoadingException");
    }

    @Test
    @DisplayName("Should contain all 8 CSS-SDA core classes after loading")
    void shouldContainAllCssSdaClasses() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();

        Model model = loader.getModel();
        String sdaNs = "http://www.example.org/sda#";
        String cssNs = "http://www.example.org/css#";

        assertTrue(model.containsResource(model.createResource(sdaNs + "Service")),
            "Model must contain sda:Service");
        assertTrue(model.containsResource(model.createResource(sdaNs + "CommunicationBinding")),
            "Model must contain sda:CommunicationBinding");
        assertTrue(model.containsResource(model.createResource(sdaNs + "DataConnector")),
            "Model must contain sda:DataConnector");
        assertTrue(model.containsResource(model.createResource(sdaNs + "DeploymentContext")),
            "Model must contain sda:DeploymentContext");
        assertTrue(model.containsResource(model.createResource(sdaNs + "Requirement")),
            "Model must contain sda:Requirement");
        assertTrue(model.containsResource(model.createResource(cssNs + "Capability")),
            "Model must contain css:Capability");
        assertTrue(model.containsResource(model.createResource(cssNs + "Skill")),
            "Model must contain css:Skill");
        assertTrue(model.containsResource(model.createResource(cssNs + "UseCase")),
            "Model must contain css:UseCase");
    }

    @Test
    @DisplayName("Should contain sda:Requirement class definition and its property definitions")
    void shouldContainRequirementProperties() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        Model model = loader.getModel();
        String sdaNs = "http://www.example.org/sda#";

        Resource requirementClass = model.getResource(sdaNs + "Requirement");
        assertTrue(model.containsResource(requirementClass),
            "sda:Requirement class must be defined in the ontology");

        assertTrue(model.containsResource(model.createResource(sdaNs + "requirementID")),
            "sda:requirementID property must be defined");
        assertTrue(model.containsResource(model.createResource(sdaNs + "title")),
            "sda:title property must be defined");
        assertTrue(model.containsResource(model.createResource(sdaNs + "description")),
            "sda:description property must be defined");
        assertTrue(model.containsResource(model.createResource(sdaNs + "verifiedBy")),
            "sda:verifiedBy property must be defined");
        assertTrue(model.containsResource(model.createResource(sdaNs + "hasConstraint")),
            "sda:hasConstraint property must be defined");
    }

    @Test
    @DisplayName("Should contain Capability properties from paper Figure 5")
    void shouldContainCapabilityProperties() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        Model model = loader.getModel();
        String sdaNs = "http://www.example.org/sda#";
        String instNs = "http://www.example.org/instance#";

        Resource oc = model.getResource(instNs + "OC-ACM-Flowmeter");
        assertNotNull(oc, "OC-ACM-Flowmeter must exist");
        assertEquals("ACM-001", oc.getProperty(model.createProperty(sdaNs + "capabilityID")).getString());
        assertEquals("AssetConditionMonitoring", oc.getProperty(model.createProperty(sdaNs + "capabilityName")).getString());
        assertNotNull(oc.getProperty(model.createProperty(sdaNs + "description")), "Capability must have description");
        assertEquals("1.0", oc.getProperty(model.createProperty(sdaNs + "version")).getString());
    }

    @Test
    @DisplayName("Should contain nominalRange data properties on Skill instances")
    void shouldContainNominalRangeOnSkills() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        Model model = loader.getModel();
        String sdaNs = "http://www.example.org/sda#";
        String instNs = "http://www.example.org/instance#";

        Resource skill = model.getResource(instNs + "SK-FlowmeterDiagnostics");
        assertNotNull(skill, "SK-FlowmeterDiagnostics must exist");
        Property minProp = model.createProperty(sdaNs + "nominalRange_min");
        Property maxProp = model.createProperty(sdaNs + "nominalRange_max");
        assertNotNull(skill.getProperty(minProp), "Skill must have nominalRange_min");
        assertNotNull(skill.getProperty(maxProp), "Skill must have nominalRange_max");
        assertEquals(0.0, skill.getProperty(minProp).getDouble(), 0.001);
        assertEquals(100.0, skill.getProperty(maxProp).getDouble(), 0.001);
    }
}
