package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Result of the semantic SPARQL match between an offered and a required
 * capability (paper, Step 5). Carries the matched skill plus the endpoint and
 * SIL evidence from the communication and deployment dimensions.
 */
public record CapabilityMatch(
    String id,
    String name,
    String requiredCapabilityId,
    String offeredCapabilityId,
    String skillId,
    String serviceName,
    String endpointUrl,
    String protocol,
    String networkZone,
    String safetyIntegrityLevel,
    Optional<String> sourceFileName,
    boolean inferred
) implements CanonicalEntity {

    public CapabilityMatch(String id, String requiredCapabilityId, String offeredCapabilityId,
                           String skillId, String serviceName, String endpointUrl, String protocol,
                           String networkZone, String safetyIntegrityLevel) {
        this(id, serviceName, requiredCapabilityId, offeredCapabilityId, skillId, serviceName,
            endpointUrl, protocol, networkZone, safetyIntegrityLevel, Optional.empty(), true);
    }

    public CapabilityMatch(String id, String requiredCapabilityId, String offeredCapabilityId,
                           String skillId, String serviceName, String endpointUrl, String protocol,
                           String networkZone, String safetyIntegrityLevel, String sourceFileName) {
        this(id, serviceName, requiredCapabilityId, offeredCapabilityId, skillId, serviceName,
            endpointUrl, protocol, networkZone, safetyIntegrityLevel, Optional.of(sourceFileName), true);
    }

    @Override
    public String className() {
        return "CapabilityMatch";
    }

    @Override
    public Map<String, Object> attributes() {
        return Attributes.of(
            "id", id,
            "name", name,
            "requiredCapabilityId", requiredCapabilityId,
            "offeredCapabilityId", offeredCapabilityId,
            "skillId", skillId,
            "serviceName", serviceName,
            "endpointUrl", endpointUrl,
            "protocol", protocol,
            "networkZone", networkZone,
            "safetyIntegrityLevel", safetyIntegrityLevel
        );
    }
}
