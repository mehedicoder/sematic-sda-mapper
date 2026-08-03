package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Canonical entity for the {@code CommunicationBinding} ontology class (L3).
 */
public record CommunicationBinding(
    String id,
    String name,
    String protocol,
    String endpointUrl,
    String securityMode,
    String securityPolicy,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public CommunicationBinding(String id, String name, String protocol, String endpointUrl,
                                String securityMode, String securityPolicy) {
        this(id, name, protocol, endpointUrl, securityMode, securityPolicy, Optional.empty(), false);
    }

    public CommunicationBinding(String id, String name, String protocol, String endpointUrl,
                                String securityMode, String securityPolicy, String sourceFileName) {
        this(id, name, protocol, endpointUrl, securityMode, securityPolicy, Optional.of(sourceFileName), false);
    }

    @Override
    public String className() {
        return "CommunicationBinding";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "protocol", protocol,
            "endpointUrl", endpointUrl,
            "securityMode", securityMode,
            "securityPolicy", securityPolicy
        );
    }
}
