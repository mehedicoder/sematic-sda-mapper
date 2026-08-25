package com.sda.mapper.deployment.orchestration;

/**
 * Probes the health of an applied workload during the Deployment Manager's
 * health-gated rollout stage. Implementations may call an actuator endpoint,
 * a Prometheus scrape, or a synthetic probe; the default {@link NoOpHealthChecker}
 * reports {@link HealthStatus#UP} so the local dry-run reaches Ready without an
 * external dependency.
 */
@FunctionalInterface
public interface HealthChecker {

    HealthStatus check(OrchestrationResult result);
}
