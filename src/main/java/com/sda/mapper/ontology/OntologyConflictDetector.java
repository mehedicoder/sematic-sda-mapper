package com.sda.mapper.ontology;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects conflicts between a new parsed model and the existing ontology.
 * A conflict occurs when an individual with the same rdfs:label and rdf:type
 * already exists in the ontology.
 */
@Component
public class OntologyConflictDetector {

    private static final Logger log = LoggerFactory.getLogger(OntologyConflictDetector.class);

    /**
     * Checks a new model for individuals that conflict with an existing model.
     *
     * @param newModel      the model containing newly parsed individuals
     * @param existingModel the model containing the current ontology
     * @return a list of conflict descriptions (empty if no conflicts)
     */
    public List<String> detectConflicts(Model newModel, Model existingModel) {
        List<String> conflicts = new ArrayList<>();

        StmtIterator iter = newModel.listStatements((Resource) null, RDF.type, (RDFNode) null);
        while (iter.hasNext()) {
            var stmt = iter.next();
            Resource subject = stmt.getSubject();
            RDFNode type = stmt.getObject();

            if (!subject.isURIResource()) {
                continue;
            }

            String label = getLabel(subject, newModel);
            if (label == null) {
                continue;
            }

            boolean duplicate = existingModel
                .listSubjectsWithProperty(RDF.type, type)
                .filterKeep(existing -> {
                    String existingLabel = getLabel(existing, existingModel);
                    return label.equals(existingLabel);
                })
                .hasNext();

            if (duplicate) {
                String typeName = type.isURIResource() ? type.asResource().getLocalName() : type.toString();
                conflicts.add(typeName + " with label \"" + label + "\" already exists");
            }
        }

        if (!conflicts.isEmpty()) {
            log.warn("Detected {} conflicts during registration", conflicts.size());
        }
        return conflicts;
    }

    private String getLabel(Resource resource, Model model) {
        var stmt = model.getProperty(resource, RDFS.label);
        if (stmt != null && stmt.getString() != null) {
            return stmt.getString();
        }
        return resource.getLocalName();
    }
}
