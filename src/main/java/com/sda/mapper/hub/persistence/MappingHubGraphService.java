package com.sda.mapper.hub.persistence;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Hydrates the runtime Mapping Hub graph by unioning the read-only base
 * ontology model with instance triples loaded from PostgreSQL.
 *
 * <p>The merged graph is transient and is never written back to the ontology
 * Turtle file or persisted as a merged model.</p>
 */
@Service
public class MappingHubGraphService {

    private final OntologyModelLoader ontologyModelLoader;
    private final MappingHubPersistenceService persistenceService;

    /**
     * Creates the graph hydration service.
     *
     * @param ontologyModelLoader read-only base ontology loader
     * @param persistenceService instance triple persistence
     */
    public MappingHubGraphService(OntologyModelLoader ontologyModelLoader,
                                  MappingHubPersistenceService persistenceService) {
        this.ontologyModelLoader = ontologyModelLoader;
        this.persistenceService = persistenceService;
    }

    /**
     * Builds a transient union of the base ontology and all persisted instance
     * triples for the session.
     *
     * @param sessionId mapping hub session id
     * @return transient Jena model
     */
    public Model hydrateUnifiedGraph(UUID sessionId) {
        Model unified = ModelFactory.createDefaultModel();
        unified.add(ontologyModelLoader.getModel());
        for (InstanceTriple triple : persistenceService.loadTriples(sessionId)) {
            addTriple(unified, triple);
        }
        return unified;
    }

    private static void addTriple(Model model, InstanceTriple triple) {
        Resource subject = model.createResource(triple.subjectIri());
        Property predicate = model.createProperty(triple.predicateIri());
        if (triple.objectIsLiteral()) {
            if (triple.objectDatatypeIri() != null && !triple.objectDatatypeIri().isBlank()) {
                var datatype = TypeMapper.getInstance().getTypeByName(triple.objectDatatypeIri());
                subject.addProperty(predicate, model.createTypedLiteral(triple.objectValue(), datatype));
            } else if (triple.objectLanguage() != null && !triple.objectLanguage().isBlank()) {
                subject.addProperty(predicate, triple.objectValue(), triple.objectLanguage());
            } else {
                subject.addProperty(predicate, triple.objectValue());
            }
        } else {
            subject.addProperty(predicate, model.createResource(triple.objectValue()));
        }
    }
}
