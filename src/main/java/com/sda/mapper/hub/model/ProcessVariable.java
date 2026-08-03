package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code ProcessVariable} ontology class (L4).
 */
public record ProcessVariable(
    String id,
    String name,
    String unit,
    String nodeId,
    String assetId,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public ProcessVariable(String id, String name, String unit, String nodeId, String assetId) {
        this(id, name, unit, nodeId, assetId, Optional.empty(), false);
    }

    public ProcessVariable(String id, String name, String unit, String nodeId, String assetId, String sourceFileName) {
        this(id, name, unit, nodeId, assetId, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "ProcessVariable";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "unit", unit,
            "nodeId", nodeId,
            "assetId", assetId
        );
    }
}
