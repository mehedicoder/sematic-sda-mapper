package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code Stakeholder} ontology class (L1).
 */
public record Stakeholder(
    String id,
    String name,
    String role,
    String organization,
    String contact,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public Stakeholder(String id, String name, String role, String organization, String contact) {
        this(id, name, role, organization, contact, Optional.empty(), false);
    }

    public Stakeholder(String id, String name, String role, String organization, String contact, String sourceFileName) {
        this(id, name, role, organization, contact, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "Stakeholder";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "role", role,
            "organization", organization,
            "contact", contact
        );
    }
}
