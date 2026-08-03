package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code ServiceInstance} ontology class (L2).
 */
public record ServiceInstance(
    String id,
    String serviceId,
    String endpointUrl,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public ServiceInstance(String id, String serviceId, String endpointUrl) {
        this(id, serviceId, endpointUrl, Optional.empty(), false);
    }

    public ServiceInstance(String id, String serviceId, String endpointUrl, String sourceFileName) {
        this(id, serviceId, endpointUrl, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "ServiceInstance";
    }

    @Override
    public String name() {
        return serviceId.isBlank() ? id : serviceId;
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "serviceId", serviceId,
            "endpointUrl", endpointUrl
        );
    }
}
