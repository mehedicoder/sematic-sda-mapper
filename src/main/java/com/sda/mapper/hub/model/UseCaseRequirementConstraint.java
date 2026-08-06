package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code UseCaseRequirementConstraint} ontology class (L1).
 */
public record UseCaseRequirementConstraint(
    String id,
    String name,
    String expression,
    String description,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public UseCaseRequirementConstraint(String id, String name, String expression, String description) {
        this(id, name, expression, description, Optional.empty(), false);
    }

    public UseCaseRequirementConstraint(String id, String name, String expression, String description, String sourceFileName) {
        this(id, name, expression, description, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "UseCaseRequirementConstraint";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "expression", expression,
            "description", description
        );
    }
}
