package com.sda.mapper.deployment.orchestration;

/**
 * Pluggable backend that applies a manifest to a target environment.
 * <p>
 * The Deployment Manager resolves a backend by {@link #supports(OrchestrationTarget)}.
 * The shipped {@link LocalDryRunBackend} provides a fully tested, dependency-free
 * implementation. A real Kubernetes backend is an extension point that requires a
 * Kubernetes client dependency and infrastructure approval.
 */
public interface OrchestrationBackend {

    /** Stable backend identifier (e.g. "local-dry-run"). */
    String name();

    /** Whether this backend can serve the given target. */
    boolean supports(OrchestrationTarget target);

    /** Apply (or dry-run) the request, returning the resulting rollout state. */
    OrchestrationResult apply(OrchestrationRequest request, RolloutObserver observer);

    /** Roll back a previously applied handle. */
    OrchestrationResult rollback(String handleId);

    /** Query the current state of a handle. */
    OrchestrationResult status(String handleId);
}
