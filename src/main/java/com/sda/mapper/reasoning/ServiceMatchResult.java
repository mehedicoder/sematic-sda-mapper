package com.sda.mapper.reasoning;

/**
 * Result of a semantic service matching query executed against the CSS-SDA ontology.
 * Contains all matched service details extracted from SPARQL query solution variables.
 *
 * @param matched              whether a matching service was found
 * @param serviceName          rdfs:label of the matched Service
 * @param containerImage       container image reference (not populated by v2 ontology)
 * @param capabilityName       rdfs:label of the matched Capability
 * @param skillName            rdfs:label of the matched Skill
 * @param protocol             sda:protocol from the CommunicationBinding
 * @param accessMode           sda:accessMode from the CommunicationBinding
 * @param bindingLabel         rdfs:label of the CommunicationBinding
 * @param connector            rdfs:label of the DataConnector
 * @param deploymentContext    rdfs:label of the DeploymentContext
 * @param recall               sda:reportedRecall value
 * @param latencyMs            sda:reportedLatency value in milliseconds
 * @param explanation          human-readable explanation of the match result
 * @param lifecycleStatus      current lifecycle status (Defined/Matched/Verified)
 * @param requirementId        the requirement ID that was matched
 * @param safetyIntegrityLevel sda:safetyIntegrityLevel from the linked SafetyClass (e.g. "SIL-2")
 * @param determinismClass     sda:determinismClass from the DeploymentContext (e.g. "HardRealTime")
 * @param networkZone          rdfs:label of the NetworkZone hosting the DeploymentContext (e.g. "OT-Zone-3")
 */
public record ServiceMatchResult(boolean matched, String serviceName, String containerImage,
                                  String capabilityName, String skillName,
                                  String protocol, String accessMode,
                                  String bindingLabel, String connector,
                                  String deploymentContext,
                                  double recall, int latencyMs, String explanation,
                                  String lifecycleStatus, String requirementId,
                                  String safetyIntegrityLevel, String determinismClass,
                                  String networkZone) {

    /**
     * Creates a no-match result with the given reason.
     *
     * @param reason explanation of why no match was found
     * @return a {@code ServiceMatchResult} with {@code matched=false}
     */
    public static ServiceMatchResult noMatch(String reason) {
        return new ServiceMatchResult(false, null, null, null, null, null, null,
            null, null, null, 0.0, 0, reason, "Defined", null, null, null, null);
    }
}
