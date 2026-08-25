package com.sda.mapper.deployment.orchestration;

/**
 * A request to apply (or dry-run) a generated manifest against an
 * {@link OrchestrationTarget}.
 *
 * @param manifestYaml            the manifest YAML to apply (Kubernetes or Docker Compose)
 * @param target                  the target environment
 * @param planName                human-readable name for the rollout
 * @param dryRun                  when true, validates and plans only; the manifest is not applied
 * @param healthCheckAttempts     number of health-gate probes before declaring failure
 * @param healthCheckIntervalMillis delay between health-gate probes
 * @param timeoutMillis           overall rollout timeout (advisory)
 */
public record OrchestrationRequest(
        String manifestYaml,
        OrchestrationTarget target,
        String planName,
        boolean dryRun,
        int healthCheckAttempts,
        long healthCheckIntervalMillis,
        long timeoutMillis) {

    public static OrchestrationRequest of(String manifestYaml, OrchestrationTarget target, boolean dryRun) {
        return new OrchestrationRequest(manifestYaml, target, "deployment", dryRun, 3, 50, 5_000);
    }
}
