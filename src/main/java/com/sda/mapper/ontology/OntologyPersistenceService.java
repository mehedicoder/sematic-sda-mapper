package com.sda.mapper.ontology;

import com.sda.mapper.error.OntologyLoadingException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles persistence of uploaded service ontology individuals to a TTL file on disk.
 * The file is loaded alongside the base ontology at startup.
 */
@Service
@ConfigurationProperties(prefix = "app.ontology")
public class OntologyPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(OntologyPersistenceService.class);

    private String uploadedFilePath = "ontology/uploaded-instances.ttl";

    /**
     * Saves a Jena Model of parsed service triples to the uploaded instances TTL file.
     * Overwrites any previous uploads (last-write-wins).
     *
     * @param model the parsed model to persist
     * @throws OntologyLoadingException if the file cannot be written
     */
    public void saveUploadedInstances(Model model) {
        Path path = resolvePath();
        ensureParentDirectory(path);
        try (OutputStream out = Files.newOutputStream(path)) {
            model.write(out, "TTL");
            log.info("Saved {} triples to uploaded instances file: {}", model.size(), path);
        } catch (IOException e) {
            throw new OntologyLoadingException(
                "Failed to write uploaded instances to: " + path, e);
        }
    }

    /**
     * Reads the uploaded instances TTL file into a Jena Model.
     *
     * @return the model containing uploaded triples, or an empty model if the file does not exist
     * @throws OntologyLoadingException if the file exists but cannot be parsed
     */
    public Model loadUploadedInstances() {
        Path path = resolvePath();
        Model model = ModelFactory.createDefaultModel();
        if (!Files.exists(path)) {
            log.debug("No uploaded instances file found at: {}", path);
            return model;
        }
        try (InputStream in = Files.newInputStream(path)) {
            model.read(in, null, "TTL");
            log.info("Loaded {} triples from uploaded instances: {}", model.size(), path);
            return model;
        } catch (IOException e) {
            throw new OntologyLoadingException(
                "Failed to read uploaded instances from: " + path, e);
        } catch (Exception e) {
            throw new OntologyLoadingException(
                "Failed to parse uploaded instances file (invalid TTL?): " + path, e);
        }
    }

    /**
     * Deletes the uploaded instances TTL file if it exists.
     *
     * @return true if the file was deleted, false if it did not exist
     * @throws OntologyLoadingException if deletion fails
     */
    public boolean clearUploadedInstances() {
        Path path = resolvePath();
        if (!Files.exists(path)) {
            log.debug("No uploaded instances file to delete at: {}", path);
            return false;
        }
        try {
            Files.delete(path);
            log.info("Deleted uploaded instances file: {}", path);
            return true;
        } catch (IOException e) {
            throw new OntologyLoadingException(
                "Failed to delete uploaded instances file: " + path, e);
        }
    }

    /**
     * Returns true if an uploaded instances file exists on disk.
     */
    public boolean hasUploadedInstances() {
        return Files.exists(resolvePath());
    }

    /**
     * Returns the resolved filesystem path for the uploaded instances file.
     */
    public Path resolvePath() {
        return Paths.get(uploadedFilePath).toAbsolutePath().normalize();
    }

    public String getUploadedFilePath() {
        return uploadedFilePath;
    }

    public void setUploadedFilePath(String uploadedFilePath) {
        this.uploadedFilePath = uploadedFilePath;
    }

    private void ensureParentDirectory(Path path) {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new OntologyLoadingException(
                    "Failed to create directory for uploaded instances: " + parent, e);
            }
        }
    }
}
