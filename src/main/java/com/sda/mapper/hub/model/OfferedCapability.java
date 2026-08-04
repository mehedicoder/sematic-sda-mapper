package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code OfferedCapability} ontology class (L2).
 *
 * <p>Per the semantic mapping pipeline (paper, Step 4) an OfferedCapability is
 * <em>inferred</em> by the OWL reasoner from SkillParameter semantics
 * (functionClass, engineeringUnit, nominalRange and SIL), not parsed directly.
 */
public record OfferedCapability(
    String id,
    String skillId,
    String name,
    String functionClass,
    String engineeringUnit,
    String nominalRangeMin,
    String nominalRangeMax,
    String accuracy,
    String safetyIntegrityLevel,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public OfferedCapability(String id, String skillId, String name, String functionClass,
                             String engineeringUnit, String nominalRangeMin, String nominalRangeMax,
                             String accuracy, String safetyIntegrityLevel) {
        this(id, skillId, name, functionClass, engineeringUnit, nominalRangeMin, nominalRangeMax,
            accuracy, safetyIntegrityLevel, Optional.empty(), true);
    }

    public OfferedCapability(String id, String skillId, String name, String functionClass,
                             String engineeringUnit, String nominalRangeMin, String nominalRangeMax,
                             String accuracy, String safetyIntegrityLevel, String sourceFileName) {
        this(id, skillId, name, functionClass, engineeringUnit, nominalRangeMin, nominalRangeMax,
            accuracy, safetyIntegrityLevel, Optional.of(sourceFileName), true);
    }

    @Override
    public String className() {
        return "OfferedCapability";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "skillId", skillId,
            "name", name,
            "functionClass", functionClass,
            "engineeringUnit", engineeringUnit,
            "nominalRangeMin", nominalRangeMin,
            "nominalRangeMax", nominalRangeMax,
            "accuracy", accuracy,
            "safetyIntegrityLevel", safetyIntegrityLevel
        );
    }
}
