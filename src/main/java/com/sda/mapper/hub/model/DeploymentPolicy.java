package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code DeploymentPolicy} ontology class (L2b).
 */
public record DeploymentPolicy(
    String id,
    String name,
    String description,
    String replicaCount,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public DeploymentPolicy(String id, String name, String description, String replicaCount) {
        this(id, name, description, replicaCount, Optional.empty(), false);
    }

    public DeploymentPolicy(String id, String name, String description, String replicaCount, String sourceFileName) {
        this(id, name, description, replicaCount, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "DeploymentPolicy";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "replicaCount", replicaCount
        );
    }
}
