package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code DeploymentContext} ontology class (L2b).
 */
public record DeploymentContext(
    String id,
    String name,
    String description,
    String clusterType,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public DeploymentContext(String id, String name, String description, String clusterType) {
        this(id, name, description, clusterType, Optional.empty(), false);
    }

    public DeploymentContext(String id, String name, String description, String clusterType, String sourceFileName) {
        this(id, name, description, clusterType, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "DeploymentContext";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "clusterType", clusterType
        );
    }
}
