package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code UseCaseRequirementParameter} ontology class (L1).
 */
public record UseCaseRequirementParameter(
    String id,
    String name,
    String value,
    String unit,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public UseCaseRequirementParameter(String id, String name, String value, String unit) {
        this(id, name, value, unit, Optional.empty(), false);
    }

    public UseCaseRequirementParameter(String id, String name, String value, String unit, String sourceFileName) {
        this(id, name, value, unit, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "UseCaseRequirementParameter";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "value", value,
            "unit", unit
        );
    }
}
