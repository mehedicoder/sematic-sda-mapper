package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code Asset} ontology class (L4).
 */
public record Asset(
    String id,
    String name,
    String manufacturer,
    String serialNumber,
    String assetKind,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public Asset(String id, String name, String manufacturer, String serialNumber, String assetKind) {
        this(id, name, manufacturer, serialNumber, assetKind, Optional.empty(), false);
    }

    public Asset(String id, String name, String manufacturer, String serialNumber, String assetKind, String sourceFileName) {
        this(id, name, manufacturer, serialNumber, assetKind, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "Asset";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "manufacturer", manufacturer,
            "serialNumber", serialNumber,
            "assetKind", assetKind
        );
    }
}
