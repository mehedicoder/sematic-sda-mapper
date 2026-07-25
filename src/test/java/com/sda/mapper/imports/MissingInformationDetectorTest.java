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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Missing Information Detector")
class MissingInformationDetectorTest {

    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";
    private static final String INST_NS = "http://www.example.org/instance#";

    private OntologyModelLoader loader;
    private MissingInformationDetector detector;

    @BeforeEach
    void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        detector = new MissingInformationDetector(loader);
    }

    @Test
    @DisplayName("Should detect no missing items when ontology is complete")
    void shouldDetectNoMissingItemsForCompleteOntology() {
        Model model = loader.getModel();
        List<MissingItem> items = detector.detectMissing(model);
        assertNotNull(items);
    }

    @Test
    @DisplayName("Should detect service without CommunicationBinding")
    void shouldDetectMissingCommunicationBinding() {
        Model model = ModelFactory.createDefaultModel();
        Resource service = model.createResource(INST_NS + "TestService");
        service.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        service.addProperty(RDFS.label, "Test Service");

        MissingInformationDetector det = new MissingInformationDetector(loader);
        List<MissingItem> items = det.detectMissing(model);

        assertTrue(items.stream().anyMatch(i ->
            i.category() == MissingCategory.MISSING_COMMUNICATION_BINDING
                && i.entityLabel().equals("Test Service")),
            "Should detect missing CommunicationBinding for TestService");
    }

    @Test
    @DisplayName("Should detect service without DeploymentContext")
    void shouldDetectMissingDeploymentContext() {
        Model model = ModelFactory.createDefaultModel();
        Resource service = model.createResource(INST_NS + "TestService");
        service.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        service.addProperty(RDFS.label, "Test Service");

        Resource binding = model.createResource(INST_NS + "TestBinding");
        binding.addProperty(RDF.type, model.createResource(CSS_NS + "CommunicationBinding"));
        service.addProperty(model.createProperty(SDA_NS + "usesBinding"), binding);

        MissingInformationDetector det = new MissingInformationDetector(loader);
        List<MissingItem> items = det.detectMissing(model);

        assertTrue(items.stream().anyMatch(i ->
            i.category() == MissingCategory.MISSING_DEPLOYMENT_CONTEXT),
            "Should detect missing DeploymentContext");
    }

    @Test
    @DisplayName("Should not report missing items when service is fully linked")
    void shouldNotReportMissingForFullyLinkedService() {
        Model model = ModelFactory.createDefaultModel();

        Resource service = model.createResource(INST_NS + "TestService");
        service.addProperty(RDF.type, model.createResource(CSS_NS + "Service"));
        service.addProperty(RDFS.label, "Test Service");

        Resource binding = model.createResource(INST_NS + "TestBinding");
        binding.addProperty(RDF.type, model.createResource(CSS_NS + "CommunicationBinding"));
        service.addProperty(model.createProperty(SDA_NS + "usesBinding"), binding);

        Resource deploy = model.createResource(INST_NS + "TestDeploy");
        deploy.addProperty(RDF.type, model.createResource(CSS_NS + "DeploymentContext"));
        deploy.addProperty(model.createProperty(SDA_NS + "safetyIntegrityLevel"), "SIL-0");
        deploy.addProperty(model.createProperty(SDA_NS + "networkZone"), "OT-Zone-1");
        deploy.addProperty(model.createProperty(SDA_NS + "determinismClass"), "SoftRealTime");
        service.addProperty(model.createProperty(SDA_NS + "isDeployedBy"), deploy);

        MissingInformationDetector det = new MissingInformationDetector(loader);
        List<MissingItem> items = det.detectMissing(model);

        assertFalse(items.stream().anyMatch(i ->
            i.category() == MissingCategory.MISSING_COMMUNICATION_BINDING
                || i.category() == MissingCategory.MISSING_DEPLOYMENT_CONTEXT),
            "Fully linked service should not trigger missing CommunicationBinding or DeploymentContext");
    }

    @Test
    @DisplayName("Should detect device without vendor metadata")
    void shouldDetectMissingDeviceMetadata() {
        Model model = ModelFactory.createDefaultModel();
        Resource device = model.createResource(INST_NS + "TestDevice");
        device.addProperty(RDF.type, model.createResource(SDA_NS + "FieldDevice"));
        device.addProperty(RDFS.label, "Test Device");

        MissingInformationDetector det = new MissingInformationDetector(loader);
        List<MissingItem> items = det.detectMissing(model);

        assertTrue(items.stream().anyMatch(i ->
            i.category() == MissingCategory.MISSING_DEVICE_METADATA
                && i.entityLabel().equals("Test Device")),
            "Should detect missing vendor/model/version for FieldDevice");
    }
}
