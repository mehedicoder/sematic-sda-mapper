package com.sda.mapper.deployment;

import com.sda.mapper.reasoning.ServiceMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KubernetesManifestService manifest generation")
class KubernetesManifestServiceTest {

    private KubernetesManifestService service;

    private static final ServiceMatchResult MATCHED_RESULT = new ServiceMatchResult(
        true, "Flowmeter Diagnostics Service", "ghcr.io/sda/flowmeter-diag:1.0",
        "FlowRate Diagnostics", "Flowmeter Diagnostics Skill", "OPC-UA", "read-only", "OPC-UA binding", "OPC-UA Gateway", "Plant Network",
        0.95, 45, "Matched", "Defined", null, "SIL-2", "HardRealTime", null, null
    );

    @BeforeEach
    void setUp() {
        service = new KubernetesManifestService();
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
    @DisplayName("Should contain apiVersion and kind in generated YAML")
    void shouldContainApiVersionAndKind() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("apiVersion: apps/v1"),
            "YAML should contain apiVersion: apps/v1");
        assertTrue(yaml.contains("kind: Deployment"),
            "YAML should contain kind: Deployment");
    }

    @Test
    @DisplayName("Should contain nodeSelector with zone label from deployment context")
    void shouldContainNodeSelectorWithZoneLabel() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("sda.ontology/zone"),
            "YAML should contain sda.ontology/zone node selector");
        assertTrue(yaml.contains("Plant Network"),
            "YAML should contain the deployment context value");
    }

    @Test
    @DisplayName("Should contain container image in generated YAML")
    void shouldContainContainerImage() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("ghcr.io/sda/flowmeter-diag:1.0"),
            "YAML should contain the container image");
    }

    @Test
    @DisplayName("Should contain environment variables in generated YAML")
    void shouldContainEnvironmentVariables() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("PROTOCOL"), "YAML should contain PROTOCOL env var name");
        assertTrue(yaml.contains("OPC-UA"), "YAML should contain OPC-UA protocol value");
        assertTrue(yaml.contains("ACCESS_MODE"), "YAML should contain ACCESS_MODE env var");
    }

    @Test
    @DisplayName("Should set replicas to 1")
    void shouldSetReplicasToOne() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("replicas: 1"),
            "YAML should set replicas to 1");
    }

    @Test
    @DisplayName("Should contain nodeSelector with protocol label")
    void shouldContainNodeSelectorWithProtocolLabel() {
        String yaml = service.generate(MATCHED_RESULT);
        assertTrue(yaml.contains("sda.ontology/protocol"),
            "YAML should contain sda.ontology/protocol node selector");
    }
}
