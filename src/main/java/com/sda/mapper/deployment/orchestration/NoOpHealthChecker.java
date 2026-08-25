package com.sda.mapper.deployment.orchestration;

import org.springframework.stereotype.Component;

/**
 * Default {@link HealthChecker} that always reports {@link HealthStatus#UP}.
 * Used by the local dry-run backend so a rollout reaches Ready without any
 * external health endpoint. Inject a custom {@link HealthChecker} to simulate
 * failure paths in tests or to gate on a real endpoint in production.
 */
@Component
public class NoOpHealthChecker implements HealthChecker {

    @Override
    public HealthStatus check(OrchestrationResult result) {
        return HealthStatus.UP;
    }
}
