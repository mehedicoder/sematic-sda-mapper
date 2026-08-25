package com.sda.mapper.deployment.orchestration;

/**
 * Observer invoked on every rollout state transition, used for logging and
 * test assertions. The no-op instance is used when no observer is supplied.
 */
@FunctionalInterface
public interface RolloutObserver {

    void onTransition(RolloutStatus status, String message);

    RolloutObserver NOOP = (status, message) -> { };
}
