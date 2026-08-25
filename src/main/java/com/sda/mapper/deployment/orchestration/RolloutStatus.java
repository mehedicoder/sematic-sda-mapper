package com.sda.mapper.deployment.orchestration;

/**
 * Lifecycle states of a deployment rollout as driven by the Deployment Manager.
 * <p>
 * Paper reference (§4.3, Figure 6): the Deployment Manager orchestrates selected
 * services as Kubernetes workloads according to infrastructure-level deployment
 * constraints, including rollback on failure.
 */
public enum RolloutStatus {
    /** Dry-run plan produced; manifest was not applied. */
    PLANNED,
    /** Rollout accepted, waiting to be applied. */
    PENDING,
    /** Manifest applied, awaiting health gate. */
    APPLYING,
    /** Health gate passed; workload is Ready. */
    READY,
    /** Health gate failed. */
    FAILED,
    /** Rollback in progress. */
    ROLLING_BACK,
    /** Rollback completed; no workload remains. */
    ROLLED_BACK
}
