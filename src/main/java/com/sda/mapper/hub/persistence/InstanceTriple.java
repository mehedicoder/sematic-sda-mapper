package com.sda.mapper.hub.persistence;

import com.sda.mapper.hub.model.EntityOrigin;

import java.util.UUID;

/**
 * Database-backed RDF instance triple for the Mapping Hub ABox graph.
 *
 * @param id                triple identifier
 * @param sessionId         owning mapping hub session
 * @param sourceFileId      source uploaded file, when file-derived
 * @param pipelineRunId     pipeline run that created the triple
 * @param subjectIri        RDF subject IRI
 * @param predicateIri      RDF predicate IRI
 * @param objectValue       object IRI or literal lexical value
 * @param objectIsLiteral   whether the object is a literal
 * @param objectDatatypeIri literal datatype IRI, when known
 * @param objectLanguage    literal language tag, when known
 * @param origin            parsed, lifted, inferred, manual, or imported origin
 * @param stageName         pipeline stage that created the triple
 * @param inferred          whether the triple was inferred
 * @param manualEntry       whether the triple came from a manual UI entry
 */
public record InstanceTriple(
    UUID id,
    UUID sessionId,
    UUID sourceFileId,
    UUID pipelineRunId,
    String subjectIri,
    String predicateIri,
    String objectValue,
    boolean objectIsLiteral,
    String objectDatatypeIri,
    String objectLanguage,
    EntityOrigin origin,
    String stageName,
    boolean inferred,
    boolean manualEntry
) {
}
