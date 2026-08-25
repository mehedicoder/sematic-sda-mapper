package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code UseCase} ontology class (L1).
 */
public record UseCase(
    String id,
    String name,
    String description,
    String priority,
    String businessGoal,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public UseCase(String id, String name, String description, String priority) {
        this(id, name, description, priority, "", Optional.empty(), false);
    }

    public UseCase(String id, String name, String description, String priority, String sourceFileName) {
        this(id, name, description, priority, "", Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "UseCase";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "priority", priority,
            "businessGoal", businessGoal
        );
    }
}
