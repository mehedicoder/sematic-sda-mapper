package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code SemanticConcept} ontology class (L3).
 */
public record SemanticConcept(
    String id,
    String name,
    String description,
    String iri,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public SemanticConcept(String id, String name, String description, String iri) {
        this(id, name, description, iri, Optional.empty(), false);
    }

    public SemanticConcept(String id, String name, String description, String iri, String sourceFileName) {
        this(id, name, description, iri, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "SemanticConcept";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "iri", iri
        );
    }
}
