package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code InformationModel} ontology class (L3).
 */
public record InformationModel(
    String id,
    String name,
    String description,
    String modelType,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public InformationModel(String id, String name, String description, String modelType) {
        this(id, name, description, modelType, Optional.empty(), false);
    }

    public InformationModel(String id, String name, String description, String modelType, String sourceFileName) {
        this(id, name, description, modelType, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "InformationModel";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "description", description,
            "modelType", modelType
        );
    }
}
