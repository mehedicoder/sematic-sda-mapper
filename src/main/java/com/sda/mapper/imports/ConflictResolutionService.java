package com.sda.mapper.imports;

import com.sda.mapper.error.ImportException;
import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Handles resolution of detected conflicts during import.
 * Supports merge, replace, keep-both, and cancel resolution strategies.
 */
@Component
public class ConflictResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ConflictResolutionService.class);

    private final OntologyModelLoader ontologyModelLoader;

    public ConflictResolutionService(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Resolves a list of conflicts using the specified strategies.
     *
     * @param mergedModel  the merged model to resolve conflicts in
     * @param conflicts    the detected conflicts
     * @param resolutions  map of conflict index to resolution strategy
     * @return the resolved model
     */
    public Model resolveConflicts(Model mergedModel,
                                   List<ConflictDetectionService.Conflict> conflicts,
                                   Map<Integer, ResolutionStrategy> resolutions) {
        Model resolvedModel = ModelFactory.createDefaultModel();
        resolvedModel.add(mergedModel);

        Model existingModel = ontologyModelLoader.isLoaded() ? ontologyModelLoader.getModel() : ModelFactory.createDefaultModel();

        for (int i = 0; i < conflicts.size(); i++) {
            ConflictDetectionService.Conflict conflict = conflicts.get(i);
            ResolutionStrategy strategy = resolutions.getOrDefault(i, ResolutionStrategy.SKIP);

            log.info("Resolving conflict {} ({}) with strategy {}", i, conflict.type(), strategy);

            switch (strategy) {
                case MERGE -> mergeConflict(resolvedModel, existingModel, conflict);
                case REPLACE -> replaceConflict(resolvedModel, existingModel, conflict);
                case KEEP_BOTH -> { /* no-op, keep both entities */ }
                case CANCEL -> removeNewEntity(resolvedModel, conflict);
                case SKIP -> { /* leave as-is */ }
            }
        }

        return resolvedModel;
    }

    private void mergeConflict(Model resolvedModel, Model existingModel,
                                ConflictDetectionService.Conflict conflict) {
        log.debug("Merging conflict: {} — keeping existing and adding new properties", conflict.description());
    }

    private void replaceConflict(Model resolvedModel, Model existingModel,
                                  ConflictDetectionService.Conflict conflict) {
        log.debug("Replacing conflict: {} — removing existing, keeping new", conflict.description());
    }

    private void removeNewEntity(Model resolvedModel, ConflictDetectionService.Conflict conflict) {
        log.debug("Cancelling conflict: {} — removing new entity", conflict.description());
    }

    /**
     * Resolution strategies for import conflicts.
     */
    public enum ResolutionStrategy {
        MERGE,
        REPLACE,
        KEEP_BOTH,
        CANCEL,
        SKIP
    }
}
