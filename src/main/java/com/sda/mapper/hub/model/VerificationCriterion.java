package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code VerificationCriterion} ontology class (L1).
 */
public record VerificationCriterion(
    String id,
    String name,
    String criterion,
    String method,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public VerificationCriterion(String id, String name, String criterion, String method) {
        this(id, name, criterion, method, Optional.empty(), false);
    }

    public VerificationCriterion(String id, String name, String criterion, String method, String sourceFileName) {
        this(id, name, criterion, method, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "VerificationCriterion";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "criterion", criterion,
            "method", method
        );
    }
}
