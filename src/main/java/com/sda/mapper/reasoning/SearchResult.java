package com.sda.mapper.reasoning;

/**
 * Represents a single search result from the ontology.
 *
 * @param name           human-readable label of the entity
 * @param type           ontology class name (e.g. "Requirement", "Service")
 * @param description    rdfs:comment or sda:description value
 * @param ontologyPath   local name fragment of the resource URI
 * @param similarityScore heuristic match score between 0.0 and 1.0
 */
public record SearchResult(String name, String type, String description,
                            String ontologyPath, double similarityScore) {
}
