package com.sda.mapper.api.dto;

import com.sda.mapper.reasoning.ServiceMatchResult;

/**
 * Response payload for a semantic requirement matching request.
 */
public record MatchRequirementResponse(
    boolean matched,
    String serviceName,
    String containerImage,
    String capabilityName,
    String protocol,
    String accessMode,
    String connector,
    String deploymentContext,
    double recall,
    int latencyMs,
    String explanation
) {
    /**
     * Creates a response from a {@link ServiceMatchResult}.
     *
     * @param result the service match result from the reasoner
     * @return the response DTO
     */
    public static MatchRequirementResponse fromServiceMatchResult(ServiceMatchResult result) {
        return new MatchRequirementResponse(
            result.matched(),
            result.serviceName(),
            result.containerImage(),
            result.capabilityName(),
            result.protocol(),
            result.accessMode(),
            result.connector(),
            result.deploymentContext(),
            result.recall(),
            result.latencyMs(),
            result.explanation()
        );
    }
}
