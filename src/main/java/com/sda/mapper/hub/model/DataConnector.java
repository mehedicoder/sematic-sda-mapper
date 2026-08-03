package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code DataConnector} ontology class (L3).
 */
public record DataConnector(
    String id,
    String name,
    String description,
    String nodeId,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public DataConnector(String id, String name, String description, String nodeId) {
        this(id, name, description, nodeId, Optional.empty(), false);
    }

    public DataConnector(String id, String name, String description, String nodeId, String sourceFileName) {
        this(id, name, description, nodeId, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "DataConnector";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "nodeId", nodeId
        );
    }
}
