package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code SkillParameter} ontology class (L2).
 */
public record SkillParameter(
    String id,
    String skillId,
    String name,
    String functionClass,
    String engineeringUnit,
    String nominalRangeMin,
    String nominalRangeMax,
    String dataType,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public SkillParameter(String id, String skillId, String name, String engineeringUnit,
                          String nominalRangeMin, String nominalRangeMax, String dataType) {
        this(id, skillId, name, null, engineeringUnit, nominalRangeMin, nominalRangeMax, dataType, Optional.empty(), false);
    }

    public SkillParameter(String id, String skillId, String name, String engineeringUnit,
                          String nominalRangeMin, String nominalRangeMax, String dataType, String sourceFileName) {
        this(id, skillId, name, null, engineeringUnit, nominalRangeMin, nominalRangeMax, dataType, Optional.of(sourceFileName), false);
    }

    public SkillParameter(String id, String skillId, String name, String functionClass,
                          String engineeringUnit, String nominalRangeMin, String nominalRangeMax, String dataType, String sourceFileName) {
        this(id, skillId, name, functionClass, engineeringUnit, nominalRangeMin, nominalRangeMax, dataType, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "SkillParameter";
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
            "dataType", dataType
        );
    }
}
