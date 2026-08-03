package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code SafetyClass} ontology class (L2b).
 */
public record SafetyClass(
    String id,
    String name,
    String description,
    String level,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public SafetyClass(String id, String name, String description, String level) {
        this(id, name, description, level, Optional.empty(), false);
    }

    public SafetyClass(String id, String name, String description, String level, String sourceFileName) {
        this(id, name, description, level, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "SafetyClass";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "level", level
        );
    }
}
