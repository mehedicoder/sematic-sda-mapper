package com.sda.mapper.imports;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Semantic Validation Service")
class SemanticValidationServiceTest {

    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";
    private static final String INST_NS = "http://www.example.org/instance#";

    private SemanticValidationService service;

    @BeforeEach
    void setUp() {
        OntologyModelLoader loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        service = new SemanticValidationService(new MissingInformationDetector(loader));
    }

    @Test
    @DisplayName("Should return COMPLETE for fully linked ontology model")
    void shouldReturnCompleteForFullyLinkedModel() {
        Model model = ModelFactory.createDefaultModel();
        addFullyLinkedService(model);

        var result = service.validate(model);
        assertEquals("COMPLETE", result.status());
        assertFalse(result.isBlocked());
    }

    @Test
    @DisplayName("Should return BLOCKED when service has no CommunicationBinding")
    void shouldReturnBlockedForMissingBinding() {
        Model model = ModelFactory.createDefaultModel();
        Resource svcRes = model.createResource(INST_NS + "TestService");
        svcRes.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        svcRes.addProperty(RDFS.label, "Test Service");

        var result = service.validate(model);
        assertEquals("BLOCKED", result.status());
        assertTrue(result.isBlocked());
        assertTrue(result.missingItems().stream()
            .anyMatch(i -> i.category() == MissingCategory.MISSING_COMMUNICATION_BINDING));
    }

    @Test
    @DisplayName("Should return BLOCKED when service has no DeploymentContext")
    void shouldReturnBlockedForMissingDeployment() {
        Model model = ModelFactory.createDefaultModel();
        Resource svcRes = model.createResource(INST_NS + "TestService");
        svcRes.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        svcRes.addProperty(RDFS.label, "Test Service");
        Resource binding = model.createResource(INST_NS + "TestBinding");
        binding.addProperty(RDF.type, model.createResource(CSS_NS + "CommunicationBinding"));
        svcRes.addProperty(model.createProperty(SDA_NS + "usesBinding"), binding);

        var result = service.validate(model);
        assertEquals("BLOCKED", result.status());
    }

    @Test
    @DisplayName("Should include missing items in result")
    void shouldIncludeMissingItems() {
        Model model = ModelFactory.createDefaultModel();
        Resource deviceRes = model.createResource(INST_NS + "TestDevice");
        deviceRes.addProperty(RDF.type, model.createResource(SDA_NS + "FieldDevice"));
        deviceRes.addProperty(RDFS.label, "Test Device");

        var result = service.validate(model);
        assertNotNull(result.missingItems());
        assertFalse(result.missingItems().isEmpty());
    }

    private void addFullyLinkedService(Model model) {
        Resource service = model.createResource(INST_NS + "TestService");
        service.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        service.addProperty(RDFS.label, "Test Service");

        Resource skill = model.createResource(INST_NS + "TestSkill");
        skill.addProperty(RDF.type, model.createResource(CSS_NS + "Skill"));
        skill.addProperty(model.createProperty(SDA_NS + "isImplementedBy"), service);

        Resource capability = model.createResource(INST_NS + "TestCapability");
        capability.addProperty(RDF.type, model.createResource(CSS_NS + "Capability"));
        capability.addProperty(model.createProperty(SDA_NS + "realisedBy"), skill);

        Resource binding = model.createResource(INST_NS + "TestBinding");
        binding.addProperty(RDF.type, model.createResource(CSS_NS + "CommunicationBinding"));
        service.addProperty(model.createProperty(SDA_NS + "usesBinding"), binding);

        Resource deploy = model.createResource(INST_NS + "TestDeploy");
        deploy.addProperty(RDF.type, model.createResource(CSS_NS + "DeploymentContext"));
        deploy.addProperty(model.createProperty(SDA_NS + "safetyIntegrityLevel"), "SIL-0");
        deploy.addProperty(model.createProperty(SDA_NS + "networkZone"), "OT-Zone-1");
        deploy.addProperty(model.createProperty(SDA_NS + "determinismClass"), "SoftRealTime");
        service.addProperty(model.createProperty(SDA_NS + "isDeployedBy"), deploy);
    }
}
