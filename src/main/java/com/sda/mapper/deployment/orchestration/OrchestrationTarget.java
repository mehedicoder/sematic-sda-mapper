package com.sda.mapper.deployment.orchestration;

import java.util.Objects;

/**
 * Describes the target environment a manifest is orchestrated against.
 * <p>
 * The {@link Type#LOCAL_DRY_RUN} target is fully implemented and tested without
 * any external cluster (see {@link LocalDryRunBackend}). A real
 * {@link Type#KUBERNETES} target requires a Kubernetes client dependency and
 * infrastructure approval per the project safety rules, and is therefore an
 * extension point rather than a shipped backend.
 */
public record OrchestrationTarget(String name, Type type) {

    public enum Type {
        LOCAL_DRY_RUN,
        KUBERNETES
    }

    public static OrchestrationTarget localDryRun() {
        return new OrchestrationTarget("local-dry-run", Type.LOCAL_DRY_RUN);
    }

    public static OrchestrationTarget kubernetes(String name) {
        return new OrchestrationTarget(Objects.requireNonNullElse(name, "kubernetes"), Type.KUBERNETES);
    }
}
