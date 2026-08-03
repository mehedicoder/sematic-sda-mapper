package com.sda.mapper.ontology;

import com.sda.mapper.error.OntologyLoadingException;
import com.sda.mapper.support.DemoAboxBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OntologyModelLoader")
class OntologyModelLoaderTest {

    private static final String SDA_NS = "https://example.org/ontology/sda#";
    private static final String INST_NS = "http://www.example.org/instance#";

    private OntologyModelLoader loadWithDemoAbox() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-unified.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        return loader;
    }

    @Test
    @DisplayName("Should load ontology from classpath and return non-empty Jena Model")
    void shouldLoadOntologySuccessfully() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-unified.ttl");
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
        Model model = loadWithDemoAbox().getModel();

        assertTrue(model.containsResource(model.createResource(SDA_NS + "Service")),
            "Model must contain sda:Service");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "CommunicationBinding")),
            "Model must contain sda:CommunicationBinding");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "DataConnector")),
            "Model must contain sda:DataConnector");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "DeploymentContext")),
            "Model must contain sda:DeploymentContext");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Requirement")),
            "Model must contain sda:Requirement");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Capability")),
            "Model must contain sda:Capability");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "Skill")),
            "Model must contain sda:Skill");
        assertTrue(model.containsResource(model.createResource(SDA_NS + "UseCase")),
            "Model must contain sda:UseCase");
    }

    @Test
    @DisplayName("Should contain Requirement properties from paper Figure 5")
    void shouldContainRequirementProperties() {
        Model model = loadWithDemoAbox().getModel();

        Resource req001 = model.getResource(INST_NS + "REQ-001");
        assertNotNull(req001, "REQ-001 must exist");
        assertEquals("REQ-001", req001.getProperty(model.createProperty(SDA_NS + "requirementID")).getString());
        assertEquals("Flowmeter Asset-Health Monitoring", req001.getProperty(model.createProperty(SDA_NS + "title")).getString());
        assertNotNull(req001.getProperty(model.createProperty(SDA_NS + "description")), "REQ-001 must have description");
        assertNotNull(req001.getProperty(model.createProperty(SDA_NS + "rationale")), "REQ-001 must have rationale");
        assertNotNull(req001.getProperty(model.createProperty(SDA_NS + "acceptanceCondition")), "REQ-001 must have acceptanceCondition");
        assertEquals("AssetDiagnostics", req001.getProperty(model.createProperty(SDA_NS + "requirementType")).getString());
        assertEquals("High", req001.getProperty(model.createProperty(SDA_NS + "hasPriority")).getString());
    }

    @Test
    @DisplayName("Should contain Capability properties from paper Figure 5")
    void shouldContainCapabilityProperties() {
        Model model = loadWithDemoAbox().getModel();

        Resource oc = model.getResource(INST_NS + "OC-ACM-Flowmeter");
        assertNotNull(oc, "OC-ACM-Flowmeter must exist");
        assertEquals("ACM-001", oc.getProperty(model.createProperty(SDA_NS + "capabilityID")).getString());
        assertEquals("AssetConditionMonitoring", oc.getProperty(model.createProperty(SDA_NS + "capabilityName")).getString());
        assertNotNull(oc.getProperty(model.createProperty(SDA_NS + "description")), "Capability must have description");
        assertEquals("1.0", oc.getProperty(model.createProperty(SDA_NS + "version")).getString());
    }

    @Test
    @DisplayName("Should contain nominalRange data properties on Skill instances")
    void shouldContainNominalRangeOnSkills() {
        Model model = loadWithDemoAbox().getModel();

        Resource skill = model.getResource(INST_NS + "SK-FlowmeterDiagnostics");
        assertNotNull(skill, "SK-FlowmeterDiagnostics must exist");
        Property minProp = model.createProperty(SDA_NS + "nominalRange_min");
        Property maxProp = model.createProperty(SDA_NS + "nominalRange_max");
        assertNotNull(skill.getProperty(minProp), "Skill must have nominalRange_min");
        assertNotNull(skill.getProperty(maxProp), "Skill must have nominalRange_max");
        assertEquals(0.0, skill.getProperty(minProp).getDouble(), 0.001);
        assertEquals(100.0, skill.getProperty(maxProp).getDouble(), 0.001);
    }
}
