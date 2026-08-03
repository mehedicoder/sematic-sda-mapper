package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code ClusterNode} ontology class (L2b).
 */
public record ClusterNode(
    String id,
    String name,
    String address,
    String role,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public ClusterNode(String id, String name, String address, String role) {
        this(id, name, address, role, Optional.empty(), false);
    }

    public ClusterNode(String id, String name, String address, String role, String sourceFileName) {
        this(id, name, address, role, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "ClusterNode";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "address", address,
            "role", role
        );
    }
}
