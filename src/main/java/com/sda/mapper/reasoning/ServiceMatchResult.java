package com.sda.mapper.reasoning;

/**
 * Result of a semantic service matching query executed against the CSS-SDA ontology.
 * Contains all matched service details extracted from SPARQL query solution variables.
 *
 * @param matched              whether a matching service was found
 * @param serviceName          rdfs:label of the matched Service
 * @param containerImage       sda:image container image reference
 * @param capabilityName       rdfs:label of the matched OfferedCapability
 * @param protocol             sda:protocol from the CommunicationBinding
 * @param accessMode           sda:accessMode from the CommunicationBinding
 * @param connector            rdfs:label of the DataConnector
 * @param deploymentContext    rdfs:label of the DeploymentContext
 * @param recall               sda:reportedRecall value
 * @param latencyMs            sda:reportedLatency value in milliseconds
 * @param explanation          human-readable explanation of the match result
 */
public record ServiceMatchResult(boolean matched, String serviceName, String containerImage,
                                  String capabilityName, String protocol, String accessMode,
                                  String connector, String deploymentContext,
                                  double recall, int latencyMs, String explanation) {

    /**
     * Creates a no-match result with the given reason.
     *
     * @param reason explanation of why no match was found
     * @return a {@code ServiceMatchResult} with {@code matched=false}
     */
    public static ServiceMatchResult noMatch(String reason) {
        return new ServiceMatchResult(false, null, null, null, null, null,
            null, null, 0.0, 0, reason);
    }
}
