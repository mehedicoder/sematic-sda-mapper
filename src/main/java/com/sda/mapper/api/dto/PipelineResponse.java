package com.sda.mapper.api.dto;

import com.sda.mapper.reasoning.ServiceMatchResult;

import java.util.List;
import java.util.Map;

/**
 * Response payload for the end-to-end pipeline execution.
 * Contains parse metadata, per-requirement match results with manifests,
 * and pipeline warnings.
 */
public record PipelineResponse(
    String fileType,
    int parsedTripleCount,
    int liftedTripleCount,
    int inferredTripleCount,
    long elapsedMs,
    Map<String, RequirementMatch> matches,
    List<String> warnings
) {

    /**
     * A single requirement match with generated deployment manifests.
     */
    public record RequirementMatch(
        boolean matched,
        String serviceName,
        String containerImage,
        String capabilityName,
        String skillName,
        String protocol,
        String accessMode,
        String bindingLabel,
        String connector,
        String deploymentContext,
        double recall,
        int latencyMs,
        String explanation,
        String lifecycleStatus,
        String safetyIntegrityLevel,
        String determinismClass,
        String networkZone,
        String dockerComposeManifest,
        String kubernetesManifest
    ) {

        /**
         * Creates a {@link RequirementMatch} from a {@link ServiceMatchResult} with generated manifests.
         *
         * @param result                the service match result
         * @param dockerComposeManifest Docker Compose YAML
         * @param kubernetesManifest    Kubernetes Deployment YAML
         * @return the requirement match DTO
         */
        public static RequirementMatch withManifests(ServiceMatchResult result,
                                                      String dockerComposeManifest,
                                                      String kubernetesManifest) {
            return new RequirementMatch(
                result.matched(),
                result.serviceName(),
                result.containerImage(),
                result.capabilityName(),
                result.skillName(),
                result.protocol(),
                result.accessMode(),
                result.bindingLabel(),
                result.connector(),
                result.deploymentContext(),
                result.recall(),
                result.latencyMs(),
                result.explanation(),
                result.lifecycleStatus(),
                result.safetyIntegrityLevel(),
                result.determinismClass(),
                result.networkZone(),
                dockerComposeManifest,
                kubernetesManifest
            );
        }

        /**
         * Creates a no-match {@link RequirementMatch}.
         *
         * @param requirementId the requirement ID
         * @param explanation   why no match was found
         * @return the requirement match DTO with matched=false
         */
        public static RequirementMatch noMatch(String requirementId, String explanation) {
            return new RequirementMatch(
                false, null, null, null, null, null, null,
                null, null, null, 0.0, 0, explanation,
                "Defined", null, null, null, null, null
            );
        }
    }
}
