package com.sda.mapper.ontology;

import com.sda.mapper.error.OntologyLoadingException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Loads the CSS-SDA ontology (RDF/Turtle) into an Apache Jena Model at application startup.
 * The ontology file path is configured via {@code app.ontology.file-path} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.ontology")
public class OntologyModelLoader {

    private static final Logger log = LoggerFactory.getLogger(OntologyModelLoader.class);

    private List<String> filePaths = List.of("ontology/css-sda-small.ttl");
    private String uploadedFilePath = "ontology/uploaded-instances.ttl";
    private Model model;
    private boolean loaded = false;

    @PostConstruct
    public void loadOntology() {
        model = ModelFactory.createDefaultModel();
        int totalTriples = 0;

        for (String filePath : filePaths) {
            log.info("Loading ontology from classpath: {}", filePath);
            try {
                ClassPathResource resource = new ClassPathResource(filePath);
                if (!resource.exists()) {
                    throw new OntologyLoadingException(
                        "Ontology file not found on classpath: " + filePath);
                }
                Model fileModel = ModelFactory.createDefaultModel();
                try (InputStream in = resource.getInputStream()) {
                    fileModel.read(in, null, "TTL");
                }
                model.add(fileModel);
                totalTriples += fileModel.size();
                log.info("Loaded {}: {} triples", filePath, fileModel.size());
            } catch (OntologyLoadingException e) {
                throw e;
            } catch (IOException e) {
                throw new OntologyLoadingException(
                    "Failed to read ontology file: " + filePath, e);
            } catch (Exception e) {
                throw new OntologyLoadingException(
                    "Failed to parse ontology file (invalid Turtle?): " + filePath, e);
            }
        }

        loadUploadedInstances();

        loaded = true;
        long classCount = model.listSubjectsWithProperty(
            org.apache.jena.vocabulary.RDF.type,
            org.apache.jena.vocabulary.RDFS.Class).toList().size();
        log.info("Ontology loaded successfully: {} total triples, {} classes from {} files",
            model.size(), classCount, filePaths.size());
    }

    private void loadUploadedInstances() {
        Path uploadedPath = Paths.get(uploadedFilePath).toAbsolutePath().normalize();
        if (!Files.exists(uploadedPath)) {
            log.debug("No uploaded instances file at: {}", uploadedPath);
            return;
        }
        try (InputStream in = Files.newInputStream(uploadedPath)) {
            Model uploadedModel = ModelFactory.createDefaultModel();
            uploadedModel.read(in, null, "TTL");
            model.add(uploadedModel);
            log.info("Loaded uploaded instances: {} triples from {}", uploadedModel.size(), uploadedPath);
        } catch (IOException e) {
            throw new OntologyLoadingException(
                "Failed to read uploaded instances file: " + uploadedPath, e);
        } catch (Exception e) {
            throw new OntologyLoadingException(
                "Failed to parse uploaded instances file (invalid TTL?): " + uploadedPath, e);
        }
    }

    /**
     * Returns the loaded Jena Model.
     *
     * @return the ontology model
     * @throws OntologyLoadingException if the model has not been loaded
     */
    public Model getModel() {
        if (!loaded || model == null) {
            throw new OntologyLoadingException("Ontology has not been loaded yet");
        }
        return model;
    }

    /**
     * Returns whether the ontology has been successfully loaded.
     */
    public boolean isLoaded() {
        return loaded;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    public String getUploadedFilePath() {
        return uploadedFilePath;
    }

    public void setUploadedFilePath(String uploadedFilePath) {
        this.uploadedFilePath = uploadedFilePath;
    }

    /**
     * Reloads the ontology model from all configured files.
     * Used after uploaded instances are added or removed.
     */
    public synchronized void reload() {
        log.info("Reloading ontology model from {} files", filePaths.size());
        loaded = false;
        loadOntology();
    }

    /**
     * @deprecated Use {@link #getFilePaths()} instead.
     */
    @Deprecated
    public String getFilePath() {
        return filePaths.isEmpty() ? null : filePaths.get(0);
    }

    /**
     * @deprecated Use {@link #setFilePaths(List)} instead.
     */
    @Deprecated
    public void setFilePath(String filePath) {
        this.filePaths = List.of(filePath);
    }
}
