package com.sda.mapper.deployment;

import com.sda.mapper.reasoning.ServiceMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DockerComposeManifestService manifest generation")
class DockerComposeManifestServiceTest {

    private DockerComposeManifestService service;

    private static final ServiceMatchResult MATCHED_RESULT = new ServiceMatchResult(
        true, "Flowmeter Diagnostics Service", "ghcr.io/sda/flowmeter-diag:1.0",
        "FlowRate Diagnostics", "OPC-UA", "read-only", "OPC-UA Gateway", "Plant Network",
        0.95, 45, "Matched"
    );

    @BeforeEach
    void setUp() {
        service = new DockerComposeManifestService();
    }

    @Test
    @DisplayName("Should produce valid YAML parseable by SnakeYAML")
    void shouldProduceValidYaml() {
        String yaml = service.generate(MATCHED_RESULT);
        assertNotNull(yaml);
        Yaml parser = new Yaml();
        assertDoesNotThrow(() -> parser.load(yaml), "Generated YAML should be parseable");
    }

    @Test
    @DisplayName("Should contain the service name in generated YAML")
    void shouldContainServiceName() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("flowmeter-diagnostics-service"),
            "YAML should contain the normalized service name");
    }

    @Test
    @DisplayName("Should contain the container image in generated YAML")
    void shouldContainContainerImage() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("ghcr.io/sda/flowmeter-diag:1.0"),
            "YAML should contain the container image");
    }

    @Test
    @DisplayName("Should contain restart policy in generated YAML")
    void shouldContainRestartPolicy() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("unless-stopped"),
            "YAML should contain restart: unless-stopped");
    }

    @Test
    @DisplayName("Should contain environment variables in generated YAML")
    void shouldContainEnvironmentVariables() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("PROTOCOL=OPC-UA"),
            "YAML should contain PROTOCOL env var");
        assertTrue(yaml.contains("ACCESS_MODE=read-only"),
            "YAML should contain ACCESS_MODE env var");
        assertTrue(yaml.contains("RECALL="),
            "YAML should contain RECALL env var");
    }

    @Test
    @DisplayName("Should set network_mode host when deployment context contains plant")
    void shouldSetNetworkModeHostForPlantContext() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("network_mode: host"),
            "YAML should set network_mode: host for plant network deployment context");
    }

    @Test
    @DisplayName("Should not set network_mode when deployment context is not plant")
    void shouldNotSetNetworkModeForNonPlantContext() {
        ServiceMatchResult cloudResult = new ServiceMatchResult(
            true, "Cloud Analytics", "analytics:latest",
            "Analytics", "MQTT", "read-write", "MQTT Broker", "Cloud Zone",
            0.88, 120, "Matched"
        );
        String yaml = service.generate(cloudResult);
        assertFalse(yaml.contains("network_mode"),
            "YAML should not contain network_mode for non-plant deployment context");
    }

    @Test
    @DisplayName("Should set version 3.8 in compose file")
    void shouldSetVersion() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("3.8"), "YAML should specify version 3.8");
    }
}
