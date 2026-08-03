package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code Observation} ontology class (L4).
 */
public record Observation(
    String id,
    String processVariableId,
    String sensorId,
    String value,
    String timestamp,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public Observation(String id, String processVariableId, String sensorId, String value, String timestamp) {
        this(id, processVariableId, sensorId, value, timestamp, Optional.empty(), false);
    }

    public Observation(String id, String processVariableId, String sensorId, String value, String timestamp, String sourceFileName) {
        this(id, processVariableId, sensorId, value, timestamp, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "Observation";
    }

    @Override
    public String name() {
        return processVariableId + " observation";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "processVariableId", processVariableId,
            "sensorId", sensorId,
            "value", value,
            "timestamp", timestamp
        );
    }
}
