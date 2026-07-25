package com.sda.mapper.imports;

import com.sda.mapper.ontology.OntologyModelLoader;
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
 * Detects conflicts between a merged model and the existing ontology.
 * Conflicts include duplicate assets, conflicting protocols, and
 * conflicting security modes.
 */
@Component
public class ConflictDetectionService {

    private static final Logger log = LoggerFactory.getLogger(ConflictDetectionService.class);

    private final OntologyModelLoader ontologyModelLoader;

    public ConflictDetectionService(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Detects conflicts between a merged model and the existing ontology.
     *
     * @param mergedModel the model containing newly merged individuals
     * @return list of conflict descriptions
     */
    public List<Conflict> detectConflicts(Model mergedModel) {
        List<Conflict> conflicts = new ArrayList<>();

        if (!ontologyModelLoader.isLoaded()) {
            log.debug("Ontology not loaded, skipping conflict detection");
            return conflicts;
        }

        Model existingModel = ontologyModelLoader.getModel();
        detectDuplicateAssets(mergedModel, existingModel, conflicts);
        detectProtocolConflicts(mergedModel, existingModel, conflicts);

        if (!conflicts.isEmpty()) {
            log.warn("Detected {} conflicts during merge", conflicts.size());
        }
        return conflicts;
    }

    private void detectDuplicateAssets(Model newModel, Model existingModel, List<Conflict> conflicts) {
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
                conflicts.add(new Conflict(
                    ConflictType.DUPLICATE_ASSET,
                    typeName + " with label \"" + label + "\" already exists in ontology",
                    "merge"
                ));
            }
        }
    }

    private void detectProtocolConflicts(Model newModel, Model existingModel, List<Conflict> conflicts) {
        String protocolProp = "http://www.example.org/sda#protocol";
        StmtIterator iter = newModel.listStatements((Resource) null,
            newModel.createProperty(protocolProp), (RDFNode) null);

        while (iter.hasNext()) {
            var stmt = iter.next();
            Resource subject = stmt.getSubject();
            String newProtocol = stmt.getObject().isLiteral()
                ? stmt.getObject().asLiteral().getString() : null;

            if (newProtocol == null || !subject.isURIResource()) {
                continue;
            }

            String label = getLabel(subject, newModel);
            existingModel
                .listSubjectsWithProperty(newModel.createProperty(protocolProp))
                .filterKeep(existing -> {
                    String existingLabel = getLabel(existing, existingModel);
                    return label != null && label.equals(existingLabel);
                })
                .forEachRemaining(existing -> {
                    String existingProtocol = existingModel
                        .getProperty(existing, newModel.createProperty(protocolProp))
                        .getObject().isLiteral()
                        ? existingModel.getProperty(existing, newModel.createProperty(protocolProp))
                            .getObject().asLiteral().getString()
                        : null;

                    if (existingProtocol != null && !newProtocol.equals(existingProtocol)) {
                        conflicts.add(new Conflict(
                            ConflictType.PROTOCOL_MISMATCH,
                            "Protocol mismatch for \"" + label + "\": " + newProtocol + " vs " + existingProtocol,
                            "replace"
                        ));
                    }
                });
        }
    }

    private String getLabel(Resource resource, Model model) {
        var stmt = model.getProperty(resource, RDFS.label);
        if (stmt != null && stmt.getString() != null) {
            return stmt.getString();
        }
        return resource.getLocalName();
    }

    /**
     * Types of conflicts that can be detected.
     */
    public enum ConflictType {
        DUPLICATE_ASSET,
        PROTOCOL_MISMATCH,
        SECURITY_MODE_MISMATCH,
        ENGINEERING_UNIT_MISMATCH
    }

    /**
     * A detected conflict with type, description, and suggested resolution.
     */
    public record Conflict(ConflictType type, String description, String suggestedResolution) {
    }
}
