package com.sda.mapper.hub.service;

import com.sda.mapper.hub.error.MappingException;
import com.sda.mapper.hub.model.CapabilityMatch;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the downstream deployment artifacts (paper, Deployment Manager) from
 * the reasoned semantic capability matches (R-Deploy output).
 *
 * <p>For every matched capability the generator emits a deterministic Docker
 * Compose service definition and a Kubernetes Deployment + Service pair. Both
 * are serialized with SnakeYAML and then re-parsed to guarantee the produced
 * text is well-formed YAML before it is exported (AGENTS.md: validate generated
 * YAML by parsing it when YAML generation changes).</p>
 */
public class DeploymentManifestGenerator {

    /**
     * Deployment artifacts for a single reasoned capability match.
     *
     * @param dockerCompose  the Docker Compose service definition (YAML)
     * @param kubernetes     the Kubernetes Deployment + Service manifest (YAML)
     */
    public record DeploymentArtifacts(String dockerCompose, String kubernetes) {
    }

    private final Yaml dumper;
    private final Yaml parser;

    public DeploymentManifestGenerator() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.dumper = new Yaml(options);
        this.parser = new Yaml();
    }

    /**
     * Builds the Docker Compose and Kubernetes artifacts for one match.
     *
     * @param match the reasoned capability match
     * @return the generated, parse-validated deployment artifacts
     * @throws MappingException when the generated YAML cannot be serialized or re-parsed
     */
    public DeploymentArtifacts generate(CapabilityMatch match) {
        try {
            String compose = dumper.dump(buildCompose(match));
            String kubernetes = dumper.dumpAll(buildKubernetes(match).iterator());
            validate(compose);
            validateAll(kubernetes);
            return new DeploymentArtifacts(compose, kubernetes);
        } catch (MappingException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingException("Failed to generate deployment manifest YAML", e);
        }
    }

    private Map<String, Object> buildCompose(CapabilityMatch match) {
        String serviceKey = sanitize(match.serviceName().isBlank()
            ? match.offeredCapabilityId() : match.serviceName());

        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("SDA_ENDPOINT_URL", blankTo(match.endpointUrl(), "tcp://localhost:4840"));
        environment.put("SDA_PROTOCOL", blankTo(match.protocol(), "OPC-UA"));
        environment.put("SDA_OFFERED_CAPABILITY", match.offeredCapabilityId());
        environment.put("SDA_REQUIRED_CAPABILITY", match.requiredCapabilityId());
        environment.put("SDA_SKILL", match.skillId());

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("image", "sda/" + serviceKey + ":latest");
        service.put("container_name", serviceKey);
        service.put("environment", environment);
        service.put("networks", List.of("sda-edge"));

        Map<String, Object> services = new LinkedHashMap<>();
        services.put(serviceKey, service);

        Map<String, Object> network = new LinkedHashMap<>();
        network.put("driver", "bridge");
        Map<String, Object> networks = new LinkedHashMap<>();
        networks.put("sda-edge", network);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("services", services);
        root.put("networks", networks);
        return root;
    }

    private List<Map<String, Object>> buildKubernetes(CapabilityMatch match) {
        String name = sanitize(match.serviceName().isBlank()
            ? match.offeredCapabilityId() : match.serviceName());

        Map<String, Object> deploymentLabels = new LinkedHashMap<>();
        deploymentLabels.put("sda.capability.offered", match.offeredCapabilityId());
        deploymentLabels.put("sda.capability.required", match.requiredCapabilityId());
        deploymentLabels.put("app", name);

        Map<String, Object> containerEnvEndpoint = new LinkedHashMap<>();
        containerEnvEndpoint.put("name", "SDA_ENDPOINT_URL");
        containerEnvEndpoint.put("value", blankTo(match.endpointUrl(), "tcp://localhost:4840"));
        Map<String, Object> containerEnvProtocol = new LinkedHashMap<>();
        containerEnvProtocol.put("name", "SDA_PROTOCOL");
        containerEnvProtocol.put("value", blankTo(match.protocol(), "OPC-UA"));

        Map<String, Object> container = new LinkedHashMap<>();
        container.put("name", name);
        container.put("image", "sda/" + name + ":latest");
        container.put("env", List.of(containerEnvEndpoint, containerEnvProtocol));

        Map<String, Object> podTemplate = new LinkedHashMap<>();
        podTemplate.put("metadata", Map.of("labels", Map.of("app", name)));
        podTemplate.put("spec", Map.of("containers", List.of(container)));

        Map<String, Object> deploymentSpec = new LinkedHashMap<>();
        deploymentSpec.put("replicas", 1);
        deploymentSpec.put("selector", Map.of("matchLabels", Map.of("app", name)));
        deploymentSpec.put("template", podTemplate);

        Map<String, Object> deployment = new LinkedHashMap<>();
        deployment.put("apiVersion", "apps/v1");
        deployment.put("kind", "Deployment");
        deployment.put("metadata", Map.of("name", name, "labels", deploymentLabels));
        deployment.put("spec", deploymentSpec);

        Map<String, Object> servicePort = new LinkedHashMap<>();
        servicePort.put("protocol", "TCP");
        servicePort.put("port", 80);
        servicePort.put("targetPort", 8080);

        Map<String, Object> service = new LinkedHashMap<>();
        service.put("apiVersion", "v1");
        service.put("kind", "Service");
        service.put("metadata", Map.of("name", name));
        service.put("spec", Map.of(
            "selector", Map.of("app", name),
            "ports", List.of(servicePort)));

        List<Map<String, Object>> docs = new ArrayList<>();
        docs.add(deployment);
        docs.add(service);
        return docs;
    }

    private void validate(String yaml) {
        parser.load(yaml);
    }

    private void validateAll(String yaml) {
        List<Object> docs = new ArrayList<>();
        parser.loadAll(yaml).forEach(docs::add);
        if (docs.isEmpty()) {
            throw new MappingException("Generated Kubernetes manifest contained no documents");
        }
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "sda-service";
        }
        String sanitized = value.trim().toLowerCase()
            .replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        return sanitized.isBlank() ? "sda-service" : sanitized;
    }
}
