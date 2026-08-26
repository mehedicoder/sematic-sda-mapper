package com.sda.mapper.deployment.orchestration;

import com.sda.mapper.deployment.DockerComposeManifestService;
import com.sda.mapper.deployment.KubernetesManifestService;
import com.sda.mapper.deployment.ManifestService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeploymentManagerService should orchestrate a semantic match as a workload")
class DeploymentManagerServiceTest {

    private static ServiceMatchResult flowmeterMatch() {
        return new ServiceMatchResult(
                true,
                "Flowmeter Diagnostics Service",
                "unknown:latest",
                "Flowmeter Diagnostics",
                "Flowmeter Diagnostics Skill",
                "OPC-UA",
                "read-only",
                "CB-Flowmeter-OPCUA",
                "DC-FT101-OPCUA",
                "DEPLOY-OnPrem",
                0.95,
                50,
                "matched",
                "Matched",
                "REQ-001",
                "SIL-1",
                 "SoftRealTime",
                 "OT-Zone-1",
                 null);
    }

    private DeploymentManagerService service(HealthChecker checker) {
        ManifestService manifestService =
                new ManifestService(new DockerComposeManifestService(), new KubernetesManifestService());
        return new DeploymentManagerService(manifestService, List.of(new LocalDryRunBackend(checker)));
    }

    @Test
    @DisplayName("Should generate a manifest and reach Ready for a healthy rollout")
    void shouldDeployMatchToReady() {
        DeploymentManagerService manager = service(new NoOpHealthChecker());

        OrchestrationResult result = manager.apply(flowmeterMatch(), OrchestrationTarget.localDryRun());

        assertThat(result.status()).isEqualTo(RolloutStatus.READY);
        assertThat(result.appliedManifest()).contains("sda.ontology/network-zone");
        assertThat(result.appliedManifest()).contains("OT-Zone-1");
    }

    @Test
    @DisplayName("Should roll back when the health gate fails")
    void shouldRollBackOnUnhealthy() {
        DeploymentManagerService manager = service(r -> HealthStatus.DOWN);

        OrchestrationResult result = manager.apply(flowmeterMatch(), OrchestrationTarget.localDryRun());

        assertThat(result.status()).isEqualTo(RolloutStatus.ROLLED_BACK);
    }

    @Test
    @DisplayName("Should return a dry-run plan without applying")
    void shouldDryRun() {
        DeploymentManagerService manager = service(new NoOpHealthChecker());

        OrchestrationResult result = manager.dryRun(flowmeterMatch(), OrchestrationTarget.localDryRun());

        assertThat(result.isPlanned()).isTrue();
    }
}
