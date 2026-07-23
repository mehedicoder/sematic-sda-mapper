package com.sda.mapper.ontology;

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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OntologyPersistenceService TTL file operations")
class OntologyPersistenceServiceTest {

    @TempDir
    Path tempDir;

    private OntologyPersistenceService service;
    private Path uploadedFile;

    @BeforeEach
    void setUp() {
        uploadedFile = tempDir.resolve("uploaded-instances.ttl");
        service = new OntologyPersistenceService();
        service.setUploadedFilePath(uploadedFile.toString());
    }

    @AfterEach
    void tearDown() {
        service.clearUploadedInstances();
    }

    @Test
    @DisplayName("Should save model to TTL file")
    void shouldSaveModel() throws Exception {
        Model model = createTestModel();
        service.saveUploadedInstances(model);
        assertTrue(Files.exists(uploadedFile), "TTL file should be created");
        assertTrue(Files.size(uploadedFile) > 0, "TTL file should not be empty");
    }

    @Test
    @DisplayName("Should load model from TTL file")
    void shouldLoadModel() {
        Model original = createTestModel();
        service.saveUploadedInstances(original);

        Model loaded = service.loadUploadedInstances();
        assertEquals(original.size(), loaded.size(), "Loaded model should have same triple count");
    }

    @Test
    @DisplayName("Should return empty model when no file exists")
    void shouldReturnEmptyModelWhenNoFile() {
        Model loaded = service.loadUploadedInstances();
        assertNotNull(loaded, "Should never return null");
        assertEquals(0, loaded.size(), "Should be empty when no file exists");
    }

    @Test
    @DisplayName("Should delete TTL file")
    void shouldDeleteFile() {
        service.saveUploadedInstances(createTestModel());
        assertTrue(service.hasUploadedInstances());

        boolean deleted = service.clearUploadedInstances();
        assertTrue(deleted, "Should return true when file was deleted");
        assertFalse(Files.exists(uploadedFile), "File should be deleted");
    }

    @Test
    @DisplayName("Should return false when deleting non-existent file")
    void shouldReturnFalseWhenNoFile() {
        boolean deleted = service.clearUploadedInstances();
        assertFalse(deleted, "Should return false when file did not exist");
    }

    @Test
    @DisplayName("Should report uploaded instances existence")
    void shouldReportExistence() {
        assertFalse(service.hasUploadedInstances(), "Should be false before save");
        service.saveUploadedInstances(createTestModel());
        assertTrue(service.hasUploadedInstances(), "Should be true after save");
    }

    @Test
    @DisplayName("Should overwrite existing file on second save")
    void shouldOverwriteOnSecondSave() throws Exception {
        service.saveUploadedInstances(createTestModel());
        long sizeAfterFirst = Files.size(uploadedFile);

        Model largerModel = createTestModel();
        Resource extra = largerModel.createResource("http://example.org/extra");
        extra.addProperty(RDFS.label, "Extra");
        extra.addProperty(RDF.type, largerModel.createResource("http://example.org/sda#Service"));
        service.saveUploadedInstances(largerModel);

        long sizeAfterSecond = Files.size(uploadedFile);
        assertTrue(sizeAfterSecond > sizeAfterFirst,
            "Second save should produce larger file due to additional triple");
    }

    @Test
    @DisplayName("Should create parent directories if missing")
    void shouldCreateParentDirs() throws Exception {
        Path nested = tempDir.resolve("sub/dir/uploaded.ttl");
        service.setUploadedFilePath(nested.toString());
        service.saveUploadedInstances(createTestModel());
        assertTrue(Files.exists(nested), "Nested file should be created with parent dirs");
    }

    @Test
    @DisplayName("Should resolve path as absolute normalized")
    void shouldResolveAbsolutePath() {
        Path resolved = service.resolvePath();
        assertTrue(resolved.isAbsolute(), "Path should be absolute");
        assertEquals(uploadedFile.toAbsolutePath().normalize(), resolved);
    }

    private Model createTestModel() {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("sda", "http://www.example.org/sda#");
        Resource svc = model.createResource("http://www.example.org/instance#SVC-Test-1");
        svc.addProperty(RDFS.label, "Test Service");
        svc.addProperty(RDF.type, model.createResource("http://www.example.org/sda#Service"));
        return model;
    }
}
