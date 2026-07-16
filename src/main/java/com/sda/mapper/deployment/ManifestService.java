package com.sda.mapper.deployment;

import com.sda.mapper.reasoning.ServiceMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates Docker Compose and Kubernetes manifest generation
 * from a semantic service match result.
 */
@Service
public class ManifestService {

    private static final Logger log = LoggerFactory.getLogger(ManifestService.class);

    private final DockerComposeManifestService dockerComposeService;
    private final KubernetesManifestService kubernetesService;

    public ManifestService(DockerComposeManifestService dockerComposeService,
                           KubernetesManifestService kubernetesService) {
        this.dockerComposeService = dockerComposeService;
        this.kubernetesService = kubernetesService;
    }

    /**
     * Generates both Docker Compose and Kubernetes manifests for a match result.
     *
     * @param result the semantic match result
     * @return a {@link ManifestResult} containing both YAML manifests
     */
    public ManifestResult generateManifests(ServiceMatchResult result) {
        log.info("Generating manifests for service: {}", result.serviceName());

        String dockerComposeYaml = dockerComposeService.generate(result);
        String kubernetesYaml = kubernetesService.generate(result);

        return new ManifestResult(
            dockerComposeYaml,
            kubernetesYaml,
            result.serviceName(),
            result.containerImage()
        );
    }
}
