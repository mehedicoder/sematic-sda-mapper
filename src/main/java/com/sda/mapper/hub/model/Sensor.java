package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code Sensor} ontology class (L4).
 */
public record Sensor(
    String id,
    String name,
    String assetId,
    String unit,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public Sensor(String id, String name, String assetId, String unit) {
        this(id, name, assetId, unit, Optional.empty(), false);
    }

    public Sensor(String id, String name, String assetId, String unit, String sourceFileName) {
        this(id, name, assetId, unit, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "Sensor";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "assetId", assetId,
            "unit", unit
        );
    }
}
