package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CapabilityMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Deployment manifest generator (Docker Compose + Kubernetes)")
class DeploymentManifestGeneratorTest {

    private final DeploymentManifestGenerator generator = new DeploymentManifestGenerator();

    private CapabilityMatch sampleMatch() {
        return new CapabilityMatch("MATCH-1", "RC-JOURNEY", "OC-pumping",
            "SKILL-pumping", "Pump FlowControl Service", "opc.tcp://pump01:4840",
            "OPC-UA", "edge-zone", "2");
    }

    @Test
    @DisplayName("Should generate parse-valid Docker Compose YAML for a match")
    void shouldGenerateValidDockerCompose() {
        DeploymentManifestGenerator.DeploymentArtifacts artifacts = generator.generate(sampleMatch());

        assertNotNull(artifacts.dockerCompose());
        Object root = new Yaml().load(artifacts.dockerCompose());
        assertTrue(root instanceof java.util.Map, "compose root is a YAML map");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) root;
        assertTrue(map.containsKey("services"), "compose defines services");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> services = (java.util.Map<String, Object>) map.get("services");
        assertEquals(1, services.size());
        assertTrue(services.containsKey("pump-flowcontrol-service"),
            "service key is a sanitized DNS name");
    }

    @Test
    @DisplayName("Should generate parse-valid Kubernetes Deployment + Service YAML")
    void shouldGenerateValidKubernetesManifest() {
        DeploymentManifestGenerator.DeploymentArtifacts artifacts = generator.generate(sampleMatch());

        assertNotNull(artifacts.kubernetes());
        List<Object> docs = new java.util.ArrayList<>();
        new Yaml().loadAll(artifacts.kubernetes()).forEach(docs::add);
        assertEquals(2, docs.size(), "Kubernetes manifest has Deployment and Service documents");

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> deployment = (java.util.Map<String, Object>) docs.get(0);
        assertEquals("Deployment", deployment.get("kind"));
        assertEquals("apps/v1", deployment.get("apiVersion"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> deploymentMeta = (java.util.Map<String, Object>) deployment.get("metadata");
        assertEquals("pump-flowcontrol-service", deploymentMeta.get("name"));
    }

    @Test
    @DisplayName("Should fall back to a safe service name when the service name is blank")
    void shouldFallbackOnBlankServiceName() {
        CapabilityMatch blank = new CapabilityMatch("MATCH-2", "RC-2", "OC-2",
            "SK-2", "", "", "OPC-UA", "", "");
        DeploymentManifestGenerator.DeploymentArtifacts artifacts = generator.generate(blank);
        assertFalse(artifacts.dockerCompose().isBlank());
        assertFalse(artifacts.kubernetes().isBlank());
        assertTrue(new Yaml().load(artifacts.dockerCompose()) instanceof java.util.Map);
    }

    @Test
    @DisplayName("Should be deterministic across invocations")
    void shouldBeDeterministic() {
        DeploymentManifestGenerator.DeploymentArtifacts first = generator.generate(sampleMatch());
        DeploymentManifestGenerator.DeploymentArtifacts second = generator.generate(sampleMatch());
        assertEquals(first.dockerCompose(), second.dockerCompose());
        assertEquals(first.kubernetes(), second.kubernetes());
    }
}
