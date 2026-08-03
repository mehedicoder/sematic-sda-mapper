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
import java.util.List;

/**
 * Loads the CSS-SDA ontology (RDF/Turtle) into an Apache Jena Model at application startup.
 * The ontology file path is configured via {@code app.ontology.file-path} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.ontology")
public class OntologyModelLoader {

    private static final Logger log = LoggerFactory.getLogger(OntologyModelLoader.class);

    private List<String> filePaths = List.of("ontology/css-sda-unified.ttl");
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

        loaded = true;
        long classCount = model.listSubjectsWithProperty(
            org.apache.jena.vocabulary.RDF.type,
            org.apache.jena.vocabulary.RDFS.Class).toList().size();
        log.info("Ontology loaded successfully: {} total triples, {} classes from {} files",
            model.size(), classCount, filePaths.size());
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
