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

/**
 * Loads the CSS-SDA ontology (RDF/Turtle) into an Apache Jena Model at application startup.
 * The ontology file path is configured via {@code app.ontology.file-path} in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "app.ontology")
public class OntologyModelLoader {

    private static final Logger log = LoggerFactory.getLogger(OntologyModelLoader.class);

    private String filePath = "ontology/css-sda-small.ttl";
    private Model model;
    private boolean loaded = false;

    @PostConstruct
    public void loadOntology() {
        log.info("Loading ontology from classpath: {}", filePath);
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            if (!resource.exists()) {
                throw new OntologyLoadingException(
                    "Ontology file not found on classpath: " + filePath);
            }
            model = ModelFactory.createDefaultModel();
            try (InputStream in = resource.getInputStream()) {
                model.read(in, null, "TTL");
            }
            loaded = true;
            long classCount = model.listSubjectsWithProperty(
                org.apache.jena.vocabulary.RDF.type,
                org.apache.jena.vocabulary.RDFS.Class).toList().size();
            log.info("Ontology loaded successfully: {} triples, {} classes",
                model.size(), classCount);
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
