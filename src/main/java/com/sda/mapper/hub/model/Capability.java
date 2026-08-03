package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code Capability} ontology class (L2).
 */
public record Capability(
    String id,
    String name,
    String description,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public Capability(String id, String name, String description) {
        this(id, name, description, Optional.empty(), false);
    }

    public Capability(String id, String name, String description, String sourceFileName) {
        this(id, name, description, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "Capability";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description
        );
    }
}
