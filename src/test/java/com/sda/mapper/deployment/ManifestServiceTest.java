package com.sda.mapper.deployment;

import com.sda.mapper.reasoning.ServiceMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ManifestService orchestrator")
class ManifestServiceTest {

    private ManifestService manifestService;

    private static final ServiceMatchResult MATCHED_RESULT = new ServiceMatchResult(
        true, "Flowmeter Diagnostics Service", "ghcr.io/sda/flowmeter-diag:1.0",
        "FlowRate Diagnostics", "OPC-UA", "read-only", "OPC-UA binding", "OPC-UA Gateway", "Plant Network",
        0.95, 45, "Matched", "Defined", null
    );

    @BeforeEach
    void setUp() {
        manifestService = new ManifestService(
            new DockerComposeManifestService(),
            new KubernetesManifestService()
        );
    }

    @Test
    @DisplayName("Should return both Docker Compose and Kubernetes YAML")
    void shouldReturnBothYamls() {
        ManifestResult result = manifestService.generateManifests(MATCHED_RESULT);
        assertNotNull(result.dockerComposeYaml(), "Docker Compose YAML should not be null");
        assertNotNull(result.kubernetesYaml(), "Kubernetes YAML should not be null");
    }

    @Test
    @DisplayName("Both YAMLs should be valid and parseable by SnakeYAML")
    void shouldProduceValidYamls() {
        ManifestResult result = manifestService.generateManifests(MATCHED_RESULT);
        Yaml parser = new Yaml();
        assertDoesNotThrow(() -> parser.load(result.dockerComposeYaml()),
            "Docker Compose YAML should be parseable");
        assertDoesNotThrow(() -> parser.load(result.kubernetesYaml()),
            "Kubernetes YAML should be parseable");
    }

    @Test
    @DisplayName("Should preserve service name and container image in result")
    void shouldPreserveServiceInfo() {
        ManifestResult result = manifestService.generateManifests(MATCHED_RESULT);
        assertEquals("Flowmeter Diagnostics Service", result.serviceName());
        assertEquals("ghcr.io/sda/flowmeter-diag:1.0", result.containerImage());
    }

    @Test
    @DisplayName("Should handle result with null container image")
    void shouldHandleNullContainerImage() {
        ServiceMatchResult noImageResult = new ServiceMatchResult(
            true, "Test Service", null,
            "Test Capability", "MQTT", "read-write", "MQTT binding", null, "Cloud Zone",
            0.80, 100, "Matched", "Defined", null
        );
        ManifestResult result = manifestService.generateManifests(noImageResult);
        assertNotNull(result.dockerComposeYaml());
        assertNotNull(result.kubernetesYaml());
        assertNull(result.containerImage());
    }
}
