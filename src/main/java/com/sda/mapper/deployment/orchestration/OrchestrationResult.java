package com.sda.mapper.deployment.orchestration;

/**
 * Outcome of an orchestration operation (apply, dry-run, rollback, or status).
 *
 * @param handleId       opaque handle identifying this rollout (for status/rollback)
 * @param status         current {@link RolloutStatus}
 * @param message        human-readable detail
 * @param appliedManifest the manifest that was applied (null after rollback)
 * @param target         the target environment
 */
public record OrchestrationResult(
        String handleId,
        RolloutStatus status,
        String message,
        String appliedManifest,
        OrchestrationTarget target) {

    public boolean isHealthy() {
        return status == RolloutStatus.READY;
    }

    public boolean isRolledBack() {
        return status == RolloutStatus.ROLLED_BACK;
    }

    public boolean isPlanned() {
        return status == RolloutStatus.PLANNED;
    }
}
