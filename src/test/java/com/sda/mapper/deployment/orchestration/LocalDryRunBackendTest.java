package com.sda.mapper.deployment.orchestration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalDryRunBackend should orchestrate manifests without an external cluster")
class LocalDryRunBackendTest {

    private static final String VALID_YAML =
            "apiVersion: apps/v1\n" +
            "kind: Deployment\n" +
            "metadata:\n" +
            "  name: demo-service\n" +
            "spec:\n" +
            "  replicas: 1\n";

    @Test
    @DisplayName("Should reach Ready for a valid manifest with a healthy probe")
    void shouldReachReadyWhenHealthy() {
        LocalDryRunBackend backend = new LocalDryRunBackend(new NoOpHealthChecker());

        OrchestrationResult result = backend.apply(
                OrchestrationRequest.of(VALID_YAML, OrchestrationTarget.localDryRun(), false), null);

        assertThat(result.status()).isEqualTo(RolloutStatus.READY);
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.appliedManifest()).contains("demo-service");
    }

    @Test
    @DisplayName("Should roll back when the health gate keeps failing")
    void shouldRollBackOnUnhealthyProbe() {
        HealthChecker alwaysDown = r -> HealthStatus.DOWN;
        LocalDryRunBackend backend = new LocalDryRunBackend(alwaysDown);

        OrchestrationResult result = backend.apply(
                OrchestrationRequest.of(VALID_YAML, OrchestrationTarget.localDryRun(), false), null);

        assertThat(result.status()).isEqualTo(RolloutStatus.ROLLED_BACK);
        assertThat(result.isRolledBack()).isTrue();
        assertThat(result.appliedManifest()).isNull();
    }

    @Test
    @DisplayName("Should produce a plan only on dry-run and not store a deployment")
    void shouldPlanOnlyOnDryRun() {
        LocalDryRunBackend backend = new LocalDryRunBackend(new NoOpHealthChecker());

        OrchestrationResult result = backend.apply(
                OrchestrationRequest.of(VALID_YAML, OrchestrationTarget.localDryRun(), true), null);

        assertThat(result.isPlanned()).isTrue();
        assertThat(backend.status(result.handleId()).status()).isEqualTo(RolloutStatus.ROLLED_BACK);
    }

    @Test
    @DisplayName("Should fail fast on invalid manifest YAML")
    void shouldFailOnInvalidYaml() {
        LocalDryRunBackend backend = new LocalDryRunBackend(new NoOpHealthChecker());

        OrchestrationResult result = backend.apply(
                OrchestrationRequest.of("::: not yaml :::", OrchestrationTarget.localDryRun(), false), null);

        assertThat(result.status()).isEqualTo(RolloutStatus.FAILED);
    }

    @Test
    @DisplayName("Should report every rollout transition to the observer")
    void shouldReportTransitions() {
        List<RolloutStatus> transitions = new ArrayList<>();
        RolloutObserver observer = (status, message) -> transitions.add(status);
        LocalDryRunBackend backend = new LocalDryRunBackend(new NoOpHealthChecker());

        backend.apply(OrchestrationRequest.of(VALID_YAML, OrchestrationTarget.localDryRun(), false), observer);

        assertThat(transitions).contains(RolloutStatus.PENDING, RolloutStatus.APPLYING, RolloutStatus.READY);
    }

    @Test
    @DisplayName("Should only support the LOCAL_DRY_RUN target")
    void shouldOnlySupportLocalDryRun() {
        LocalDryRunBackend backend = new LocalDryRunBackend(new NoOpHealthChecker());

        assertThat(backend.supports(OrchestrationTarget.localDryRun())).isTrue();
        assertThat(backend.supports(OrchestrationTarget.kubernetes("prod"))).isFalse();
    }
}
