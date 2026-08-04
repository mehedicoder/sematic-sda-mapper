package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code RequiredCapability} ontology class (L2).
 *
 * <p>Represents a capability demanded by a stakeholder or use case. The SPARQL
 * match stage compares a RequiredCapability against the inferred
 * {@link OfferedCapability} across the capability, communication and
 * deployment dimensions (paper, Step 5).
 */
public record RequiredCapability(
    String id,
    String name,
    String functionClass,
    String engineeringUnit,
    String nominalRangeMin,
    String nominalRangeMax,
    String safetyIntegrityLevel,
    String protocol,
    String networkZone,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public RequiredCapability(String id, String name, String functionClass, String engineeringUnit,
                              String nominalRangeMin, String nominalRangeMax, String safetyIntegrityLevel,
                              String protocol, String networkZone) {
        this(id, name, functionClass, engineeringUnit, nominalRangeMin, nominalRangeMax,
            safetyIntegrityLevel, protocol, networkZone, Optional.empty(), false);
    }

    public RequiredCapability(String id, String name, String functionClass, String engineeringUnit,
                              String nominalRangeMin, String nominalRangeMax, String safetyIntegrityLevel,
                              String protocol, String networkZone, String sourceFileName) {
        this(id, name, functionClass, engineeringUnit, nominalRangeMin, nominalRangeMax,
            safetyIntegrityLevel, protocol, networkZone, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "RequiredCapability";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "functionClass", functionClass,
            "engineeringUnit", engineeringUnit,
            "nominalRangeMin", nominalRangeMin,
            "nominalRangeMax", nominalRangeMax,
            "safetyIntegrityLevel", safetyIntegrityLevel,
            "protocol", protocol,
            "networkZone", networkZone
        );
    }
}
