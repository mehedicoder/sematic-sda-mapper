package com.sda.mapper.deployment.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Dependency-free orchestration backend that simulates applying a manifest to a
 * local dry-run target. It validates the manifest YAML, simulates the rollout
 * lifecycle (Pending -&gt; Applying -&gt; Ready), enforces a health-gated rollout
 * via an injectable {@link HealthChecker}, and performs rollback on health-gate
 * failure. No external cluster or client dependency is required, so the full
 * Deployment Manager test suite passes without infrastructure.
 * <p>
 * A real Kubernetes backend (applying via a Kubernetes client) is an extension
 * point that requires infrastructure approval and is intentionally not shipped.
 */
@Service
public class LocalDryRunBackend implements OrchestrationBackend {

    private static final Logger log = LoggerFactory.getLogger(LocalDryRunBackend.class);

    private final HealthChecker healthChecker;
    private final ConcurrentMap<String, RolloutState> store = new ConcurrentHashMap<>();

    @Autowired
    public LocalDryRunBackend(HealthChecker healthChecker) {
        this.healthChecker = healthChecker != null ? healthChecker : new NoOpHealthChecker();
    }

    /** Test-only constructor with a default no-op health checker. */
    public LocalDryRunBackend() {
        this(new NoOpHealthChecker());
    }

    @Override
    public String name() {
        return "local-dry-run";
    }

    @Override
    public boolean supports(OrchestrationTarget target) {
        return target != null && target.type() == OrchestrationTarget.Type.LOCAL_DRY_RUN;
    }

    @Override
    public OrchestrationResult apply(OrchestrationRequest request, RolloutObserver observer) {
        RolloutObserver obs = observer != null ? observer : RolloutObserver.NOOP;
        String handleId = java.util.UUID.randomUUID().toString();

        if (request.manifestYaml() == null || request.manifestYaml().isBlank()) {
            return fail(handleId, request, obs, "Empty manifest");
        }

        try {
            new Yaml().load(request.manifestYaml());
        } catch (YAMLException e) {
            log.warn("LocalDryRunBackend: invalid manifest YAML: {}", e.getMessage());
            return fail(handleId, request, obs, "Invalid manifest YAML: " + e.getMessage());
        }

        if (request.dryRun()) {
            obs.onTransition(RolloutStatus.PLANNED, "Dry-run plan generated; manifest not applied");
            return new OrchestrationResult(handleId, RolloutStatus.PLANNED,
                    "Dry-run plan; not applied", request.manifestYaml(), request.target());
        }

        obs.onTransition(RolloutStatus.PENDING, "Applying manifest to local dry-run target");
        store.put(handleId, new RolloutState(request.manifestYaml()));
        obs.onTransition(RolloutStatus.APPLYING, "Manifest applied");

        OrchestrationResult snapshot = new OrchestrationResult(handleId, RolloutStatus.APPLYING,
                "applied", request.manifestYaml(), request.target());

        boolean healthy = false;
        int attempts = Math.max(1, request.healthCheckAttempts());
        for (int i = 0; i < attempts; i++) {
            if (healthChecker.check(snapshot).isHealthy()) {
                healthy = true;
                break;
            }
            try {
                Thread.sleep(Math.max(0, request.healthCheckIntervalMillis()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (healthy) {
            store.get(handleId).status = RolloutStatus.READY;
            obs.onTransition(RolloutStatus.READY, "Workload reached Ready");
            return new OrchestrationResult(handleId, RolloutStatus.READY,
                    "Workload Ready", request.manifestYaml(), request.target());
        }

        obs.onTransition(RolloutStatus.FAILED, "Health gate failed");
        obs.onTransition(RolloutStatus.ROLLING_BACK, "Rolling back");
        store.remove(handleId);
        obs.onTransition(RolloutStatus.ROLLED_BACK, "Rollback complete");
        return new OrchestrationResult(handleId, RolloutStatus.ROLLED_BACK,
                "Rolled back after failed health gate", null, request.target());
    }

    @Override
    public OrchestrationResult rollback(String handleId) {
        RolloutState removed = store.remove(handleId);
        if (removed == null) {
            log.debug("LocalDryRunBackend: no active deployment for handle {}", handleId);
        }
        return new OrchestrationResult(handleId, RolloutStatus.ROLLED_BACK,
                "Rolled back", null, OrchestrationTarget.localDryRun());
    }

    @Override
    public OrchestrationResult status(String handleId) {
        RolloutState state = store.get(handleId);
        RolloutStatus status = state == null ? RolloutStatus.ROLLED_BACK : state.status;
        return new OrchestrationResult(handleId, status, "status",
                state == null ? null : state.manifest, OrchestrationTarget.localDryRun());
    }

    private OrchestrationResult fail(String handleId, OrchestrationRequest request,
                                     RolloutObserver obs, String message) {
        log.warn("LocalDryRunBackend: {}", message);
        obs.onTransition(RolloutStatus.FAILED, message);
        return new OrchestrationResult(handleId, RolloutStatus.FAILED, message, null, request.target());
    }

    private static final class RolloutState {
        volatile RolloutStatus status;
        final String manifest;

        RolloutState(String manifest) {
            this.manifest = manifest;
            this.status = RolloutStatus.APPLYING;
        }
    }
}
