package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code Skill} ontology class (L2).
 */
public record Skill(
    String id,
    String name,
    String description,
    String capabilityId,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public Skill(String id, String name, String description, String capabilityId) {
        this(id, name, description, capabilityId, Optional.empty(), false);
    }

    public Skill(String id, String name, String description, String capabilityId, String sourceFileName) {
        this(id, name, description, capabilityId, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "Skill";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "capabilityId", capabilityId
        );
    }
}
