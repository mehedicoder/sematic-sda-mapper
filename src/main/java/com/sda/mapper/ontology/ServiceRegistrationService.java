package com.sda.mapper.ontology;

import com.sda.mapper.error.OntologyConflictException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the service registration workflow:
 * conflict detection, source tagging, TTL persistence, and ontology reload.
 */
@Service
public class ServiceRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRegistrationService.class);

    private final OntologyConflictDetector conflictDetector;
    private final OntologyPersistenceService persistenceService;
    private final OntologyModelLoader ontologyModelLoader;

    public ServiceRegistrationService(
            OntologyConflictDetector conflictDetector,
            OntologyPersistenceService persistenceService,
            OntologyModelLoader ontologyModelLoader) {
        this.conflictDetector = conflictDetector;
        this.persistenceService = persistenceService;
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Registers a parsed service model into the ontology.
     * <ol>
     *   <li>Detects conflicts with existing individuals</li>
     *   <li>Tags all individuals with {@code sda:source "uploaded"}</li>
     *   <li>Persists to the uploaded instances TTL file</li>
     *   <li>Reloads the ontology model</li>
     * </ol>
     *
     * @param parsedModel the model from a parser containing new service triples
     * @return the number of triples persisted
     * @throws OntologyConflictException if conflicts are detected
     */
    public long registerService(Model parsedModel) {
        Model existingModel = ontologyModelLoader.getModel();

        List<String> conflicts = conflictDetector.detectConflicts(parsedModel, existingModel);
        if (!conflicts.isEmpty()) {
            throw new OntologyConflictException(
                "Registration blocked: " + String.join("; ", conflicts));
        }

        tagSource(parsedModel, "uploaded");

        Model merged = loadExistingUploaded();
        merged.add(parsedModel);

        persistenceService.saveUploadedInstances(merged);
        ontologyModelLoader.reload();

        long persisted = merged.size();
        log.info("Service registered: {} triples persisted, ontology reloaded", persisted);
        return persisted;
    }

    /**
     * Clears all uploaded instances and reloads the ontology.
     */
    public void clearAllUploaded() {
        persistenceService.clearUploadedInstances();
        ontologyModelLoader.reload();
        log.info("All uploaded instances cleared, ontology reloaded");
    }

    /**
     * Returns the current number of uploaded triples on disk.
     */
    public long getUploadedTripleCount() {
        Model uploaded = persistenceService.loadUploadedInstances();
        return uploaded.size();
    }

    private Model loadExistingUploaded() {
        if (persistenceService.hasUploadedInstances()) {
            return persistenceService.loadUploadedInstances();
        }
        return org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
    }

    private void tagSource(Model model, String sourceValue) {
        StmtIterator iter = model.listStatements((Resource) null, RDF.type, (org.apache.jena.rdf.model.RDFNode) null);
        while (iter.hasNext()) {
            Resource subject = iter.next().getSubject();
            if (subject.isURIResource()) {
                model.removeAll(subject,
                    model.createProperty("http://www.example.org/sda#source"), (org.apache.jena.rdf.model.RDFNode) null);
                model.add(subject,
                    model.createProperty("http://www.example.org/sda#source"),
                    model.createLiteral(sourceValue));
            }
        }
    }
}
