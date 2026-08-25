package com.sda.mapper.deployment.orchestration;

/**
 * Result of a health probe performed by a {@link HealthChecker} during the
 * health-gated rollout stage of the Deployment Manager.
 */
public enum HealthStatus {
    UP,
    DOWN;

    public boolean isHealthy() {
        return this == UP;
    }
}
