package com.sda.mapper.ontology;

import com.sda.mapper.error.OntologyConflictException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ServiceRegistrationService orchestration")
class ServiceRegistrationServiceTest {

    @TempDir
    Path tempDir;

    private OntologyModelLoader loader;
    private OntologyPersistenceService persistenceService;
    private OntologyConflictDetector conflictDetector;
    private ServiceRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePaths(java.util.List.of("ontology/css-sda-small.ttl"));
        loader.setUploadedFilePath(tempDir.resolve("uploaded.ttl").toString());
        loader.loadOntology();

        persistenceService = new OntologyPersistenceService();
        persistenceService.setUploadedFilePath(tempDir.resolve("uploaded.ttl").toString());

        conflictDetector = new OntologyConflictDetector();
        registrationService = new ServiceRegistrationService(
            conflictDetector, persistenceService, loader);
    }

    @AfterEach
    void tearDown() {
        persistenceService.clearUploadedInstances();
    }

    @Test
    @DisplayName("Should register new service without conflicts")
    void shouldRegisterNewService() {
        Model newModel = createUniqueModel("BrandNewService");
        long persisted = registrationService.registerService(newModel);

        assertTrue(persisted > 0, "Should persist at least one triple");
        assertTrue(persistenceService.hasUploadedInstances(), "File should exist after registration");
    }

    @Test
    @DisplayName("Should tag uploaded individuals with sda:source uploaded")
    void shouldTagWithUploadedSource() {
        Model newModel = createUniqueModel("SourceTagTest");
        registrationService.registerService(newModel);

        Model uploaded = persistenceService.loadUploadedInstances();
        var sourceProps = uploaded.listStatements(
            (Resource) null,
            uploaded.createProperty("http://www.example.org/sda#source"),
            (String) null);
        assertTrue(sourceProps.hasNext(), "Should have sda:source properties");
        assertEquals("uploaded", sourceProps.next().getString(),
            "Source should be 'uploaded'");
    }

    @Test
    @DisplayName("Should throw conflict exception for duplicate service")
    void shouldThrowOnConflict() {
        Model newModel = createUniqueModel("Flowmeter Diagnostics Service");
        assertThrows(OntologyConflictException.class,
            () -> registrationService.registerService(newModel),
            "Should throw when label matches existing service");
    }

    @Test
    @DisplayName("Should reload ontology model after registration")
    void shouldReloadModel() {
        long triplesBefore = loader.getModel().size();
        registrationService.registerService(createUniqueModel("ReloadTest"));
        long triplesAfter = loader.getModel().size();

        assertTrue(triplesAfter > triplesBefore,
            "Model should have more triples after registration");
    }

    @Test
    @DisplayName("Should clear uploaded instances and reload")
    void shouldClearAndReload() {
        registrationService.registerService(createUniqueModel("ClearTest"));
        assertTrue(persistenceService.hasUploadedInstances());

        registrationService.clearAllUploaded();
        assertFalse(persistenceService.hasUploadedInstances(),
            "File should be deleted after clear");
    }

    @Test
    @DisplayName("Should report uploaded triple count")
    void shouldReportTripleCount() {
        assertEquals(0, registrationService.getUploadedTripleCount(),
            "Should be zero before any registration");

        registrationService.registerService(createUniqueModel("CountTest"));
        assertTrue(registrationService.getUploadedTripleCount() > 0,
            "Should report positive count after registration");
    }

    @Test
    @DisplayName("Should merge multiple registrations")
    void shouldMergeRegistrations() {
        registrationService.registerService(createUniqueModel("MergeTest1"));
        registrationService.registerService(createUniqueModel("MergeTest2"));

        Model uploaded = persistenceService.loadUploadedInstances();
        assertTrue(uploaded.size() > 3,
            "Merged model should contain triples from both registrations");
    }

    private Model createUniqueModel(String label) {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("sda", "http://www.example.org/sda#");
        model.setNsPrefix("inst", "http://www.example.org/instance#");

        Resource svc = model.createResource("http://www.example.org/instance#SVC-" + label.replace(" ", ""));
        svc.addProperty(RDFS.label, label);
        svc.addProperty(RDF.type, model.createResource("http://www.example.org/sda#Service"));

        return model;
    }
}
