package com.sda.mapper.deployment;

import com.sda.mapper.reasoning.ServiceMatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates Kubernetes Deployment YAML from a {@link ServiceMatchResult}.
 * Produces a valid Kubernetes Deployment manifest with node selectors
 * for SIL, determinism, and zone constraints.
 */
@Service
public class KubernetesManifestService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesManifestService.class);

    /**
     * Generates a Kubernetes Deployment YAML string for the matched service.
     *
     * @param result the semantic match result
     * @return the Kubernetes Deployment YAML content
     */
    public String generate(ServiceMatchResult result) {
        log.debug("Generating Kubernetes Deployment manifest for service: {}", result.serviceName());

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("apiVersion", "apps/v1");
        manifest.put("kind", "Deployment");

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", toDeploymentName(result.serviceName()));
        manifest.put("metadata", metadata);

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("replicas", 1);

        Map<String, Object> selector = new LinkedHashMap<>();
        Map<String, Object> matchLabels = new LinkedHashMap<>();
        matchLabels.put("app", toDeploymentName(result.serviceName()));
        selector.put("matchLabels", matchLabels);
        spec.put("selector", selector);

        Map<String, Object> template = new LinkedHashMap<>();
        Map<String, Object> templateMetadata = new LinkedHashMap<>();
        templateMetadata.put("labels", Map.of("app", toDeploymentName(result.serviceName())));
        template.put("metadata", templateMetadata);

        Map<String, Object> podSpec = new LinkedHashMap<>();
        List<Map<String, Object>> containers = new ArrayList<>();

        Map<String, Object> container = new LinkedHashMap<>();
        container.put("name", toDeploymentName(result.serviceName()));
        container.put("image", result.containerImage() != null ? result.containerImage() : "unknown:latest");

        List<Map<String, String>> envVars = buildEnvironmentVariables(result);
        if (!envVars.isEmpty()) {
            container.put("env", envVars);
        }

        containers.add(container);
        podSpec.put("containers", containers);

        Map<String, String> nodeSelector = buildNodeSelector(result);
        if (!nodeSelector.isEmpty()) {
            podSpec.put("nodeSelector", nodeSelector);
        }

        template.put("spec", podSpec);
        spec.put("template", template);

        manifest.put("spec", spec);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);

        String output = yaml.dump(manifest);
        log.debug("Generated Kubernetes YAML ({} chars)", output.length());
        return output;
    }

    private List<Map<String, String>> buildEnvironmentVariables(ServiceMatchResult result) {
        List<Map<String, String>> env = new ArrayList<>();

        if (result.protocol() != null) {
            env.add(Map.of("name", "PROTOCOL", "value", result.protocol()));
        }
        if (result.accessMode() != null) {
            env.add(Map.of("name", "ACCESS_MODE", "value", result.accessMode()));
        }
        if (result.deploymentContext() != null) {
            env.add(Map.of("name", "DEPLOYMENT_CONTEXT", "value", result.deploymentContext()));
        }
        if (result.safetyIntegrityLevel() != null) {
            env.add(Map.of("name", "SAFETY_INTEGRITY_LEVEL", "value", result.safetyIntegrityLevel()));
        }
        if (result.determinismClass() != null) {
            env.add(Map.of("name", "DETERMINISM_CLASS", "value", result.determinismClass()));
        }
        if (result.networkZone() != null) {
            env.add(Map.of("name", "NETWORK_ZONE", "value", result.networkZone()));
        }
        env.add(Map.of("name", "RECALL", "value", String.valueOf(result.recall())));
        env.add(Map.of("name", "LATENCY_MS", "value", String.valueOf(result.latencyMs())));

        return env;
    }

    private Map<String, String> buildNodeSelector(ServiceMatchResult result) {
        Map<String, String> selector = new LinkedHashMap<>();

        if (result.deploymentContext() != null) {
            selector.put("sda.ontology/zone", result.deploymentContext());
        }
        if (result.protocol() != null) {
            selector.put("sda.ontology/protocol", result.protocol());
        }
        if (result.safetyIntegrityLevel() != null) {
            selector.put("sda.ontology/sil", result.safetyIntegrityLevel());
        }
        if (result.determinismClass() != null) {
            selector.put("sda.ontology/determinism", result.determinismClass());
        }
        if (result.networkZone() != null) {
            selector.put("sda.ontology/network-zone", result.networkZone());
        }

        return selector;
    }

    private static String toDeploymentName(String serviceName) {
        if (serviceName == null) return "unknown-service";
        return serviceName.toLowerCase()
            .replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
