package com.sda.mapper.deployment.orchestration;

import com.sda.mapper.deployment.ManifestResult;
import com.sda.mapper.deployment.ManifestService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Deployment Manager (paper §4.3, Figure 6): orchestrates a semantically matched
 * service as a workload according to its infrastructure-level deployment
 * constraints.
 * <p>
 * The matched {@link ServiceMatchResult} already carries the {@code boundContext}
 * (deployment context) information; {@link ManifestService} derives the manifest
 * {@code nodeSelector} / constraints from it, so the R-Deploy binding is wired
 * straight through to the generated manifest. This service then applies the
 * manifest via a pluggable {@link OrchestrationBackend} (default:
 * {@link LocalDryRunBackend}), supporting dry-run, health-gated rollout, and
 * rollback.
 */
@Service
public class DeploymentManagerService {

    private static final Logger log = LoggerFactory.getLogger(DeploymentManagerService.class);

    private final ManifestService manifestService;
    private final List<OrchestrationBackend> backends;

    public DeploymentManagerService(ManifestService manifestService,
                                     List<OrchestrationBackend> backends) {
        this.manifestService = manifestService;
        this.backends = backends;
    }

    /**
     * Generates manifests for a match and returns a dry-run plan without applying.
     */
    public OrchestrationResult dryRun(ServiceMatchResult match, OrchestrationTarget target) {
        ManifestResult manifests = manifestService.generateManifests(match);
        return resolve(target).apply(
                OrchestrationRequest.of(manifests.kubernetesYaml(), target, true), null);
    }

    /**
     * Generates manifests for a match and applies them via the resolved backend,
     * enforcing a health-gated rollout with automatic rollback on failure.
     */
    public OrchestrationResult apply(ServiceMatchResult match, OrchestrationTarget target) {
        ManifestResult manifests = manifestService.generateManifests(match);
        log.info("Deploying service {} to target {}", manifests.serviceName(), target.name());
        return resolve(target).apply(
                OrchestrationRequest.of(manifests.kubernetesYaml(), target, false), null);
    }

    /** Rolls back a previously applied rollout. */
    public OrchestrationResult rollback(OrchestrationResult result) {
        return resolve(result.target()).rollback(result.handleId());
    }

    /** Queries the current state of a rollout handle. */
    public OrchestrationResult status(String handleId, OrchestrationTarget target) {
        return resolve(target).status(handleId);
    }

    private OrchestrationBackend resolve(OrchestrationTarget target) {
        return backends.stream()
                .filter(b -> b.supports(target))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No orchestration backend registered for target " + target));
    }
}
