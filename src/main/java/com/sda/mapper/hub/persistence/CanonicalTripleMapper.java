package com.sda.mapper.hub.persistence;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.EntityOrigin;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts Mapping Hub canonical entities into durable CSS-SDA RDF instance
 * triples. The mapper is deterministic so re-processing the same canonical
 * entity produces stable subject and predicate IRIs.
 */
@Component
public class CanonicalTripleMapper {

    private static final String SDA_NS = "http://sda-platform.org/ontology#";
    private static final String INST_NS = "http://www.example.org/mapping-hub/";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    private static final String XSD_STRING = "http://www.w3.org/2001/XMLSchema#string";

    /**
     * Converts canonical entities into instance triples for a pipeline stage.
     *
     * @param sessionId     owning session id
     * @param sourceFileId  source file id, when file-derived
     * @param pipelineRunId pipeline run id, when known
     * @param stageName     pipeline stage name
     * @param origin        semantic origin of the entities
     * @param manualEntry   whether the entities are manual entries
     * @param entities      canonical entities to convert
     * @return RDF triples ready for storage
     */
    public List<InstanceTriple> toTriples(UUID sessionId, UUID sourceFileId, UUID pipelineRunId,
                                          String stageName, EntityOrigin origin, boolean manualEntry,
                                          List<? extends CanonicalEntity> entities) {
        List<InstanceTriple> triples = new ArrayList<>();
        for (CanonicalEntity entity : entities) {
            String subject = subjectIri(entity);
            triples.add(resourceTriple(sessionId, sourceFileId, pipelineRunId, subject,
                RDF_TYPE, SDA_NS + entity.className(), origin, stageName,
                entity.inferred(), manualEntry));
            triples.add(literalTriple(sessionId, sourceFileId, pipelineRunId, subject,
                RDFS_LABEL, entity.name(), origin, stageName, entity.inferred(), manualEntry));
            for (Map.Entry<String, Object> attribute : entity.attributes().entrySet()) {
                if (attribute.getValue() == null || String.valueOf(attribute.getValue()).isBlank()) {
                    continue;
                }
                triples.add(literalTriple(sessionId, sourceFileId, pipelineRunId, subject,
                    SDA_NS + attribute.getKey(), String.valueOf(attribute.getValue()), origin,
                    stageName, entity.inferred(), manualEntry));
            }
        }
        return triples;
    }

    /**
     * Creates a stable subject IRI for a canonical entity.
     *
     * @param entity the canonical entity
     * @return a stable Mapping Hub instance IRI
     */
    public String subjectIri(CanonicalEntity entity) {
        return INST_NS + sanitize(entity.className()) + "/" + sanitize(entity.id());
    }

    private static InstanceTriple resourceTriple(UUID sessionId, UUID sourceFileId, UUID pipelineRunId,
                                                 String subjectIri, String predicateIri, String objectIri,
                                                 EntityOrigin origin, String stageName,
                                                 boolean inferred, boolean manualEntry) {
        return new InstanceTriple(UUID.randomUUID(), sessionId, sourceFileId, pipelineRunId,
            subjectIri, predicateIri, objectIri, false, null, null, origin, stageName,
            inferred, manualEntry);
    }

    private static InstanceTriple literalTriple(UUID sessionId, UUID sourceFileId, UUID pipelineRunId,
                                                String subjectIri, String predicateIri, String value,
                                                EntityOrigin origin, String stageName,
                                                boolean inferred, boolean manualEntry) {
        return new InstanceTriple(UUID.randomUUID(), sessionId, sourceFileId, pipelineRunId,
            subjectIri, predicateIri, value == null ? "" : value, true, XSD_STRING, null,
            origin, stageName, inferred, manualEntry);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
