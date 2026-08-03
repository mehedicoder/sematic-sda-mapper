package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code Service} ontology class (L2).
 */
public record Service(
    String id,
    String name,
    String description,
    String protocol,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public Service(String id, String name, String description, String protocol) {
        this(id, name, description, protocol, Optional.empty(), false);
    }

    public Service(String id, String name, String description, String protocol, String sourceFileName) {
        this(id, name, description, protocol, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "Service";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "protocol", protocol
        );
    }
}
