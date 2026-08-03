package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code NetworkZone} ontology class (L2b).
 */
public record NetworkZone(
    String id,
    String name,
    String description,
    String cidr,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public NetworkZone(String id, String name, String description, String cidr) {
        this(id, name, description, cidr, Optional.empty(), false);
    }

    public NetworkZone(String id, String name, String description, String cidr, String sourceFileName) {
        this(id, name, description, cidr, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "NetworkZone";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "cidr", cidr
        );
    }
}
