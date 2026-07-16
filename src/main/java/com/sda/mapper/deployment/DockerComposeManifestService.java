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
 * Generates Docker Compose YAML from a {@link ServiceMatchResult}.
 * Produces a valid {@code docker-compose.yml} with service image,
 * restart policy, environment variables, and optional host network mode.
 */
@Service
public class DockerComposeManifestService {

    private static final Logger log = LoggerFactory.getLogger(DockerComposeManifestService.class);

    /**
     * Generates a Docker Compose YAML string for the matched service.
     *
     * @param result the semantic match result
     * @return the Docker Compose YAML content
     */
    public String generate(ServiceMatchResult result) {
        log.debug("Generating Docker Compose manifest for service: {}", result.serviceName());

        Map<String, Object> compose = new LinkedHashMap<>();
        compose.put("version", "3.8");

        Map<String, Object> services = new LinkedHashMap<>();

        String safeName = toServiceName(result.serviceName());
        Map<String, Object> serviceDef = new LinkedHashMap<>();

        serviceDef.put("image", result.containerImage() != null ? result.containerImage() : "unknown:latest");
        serviceDef.put("restart", "unless-stopped");

        List<String> envVars = buildEnvironmentVariables(result);
        if (!envVars.isEmpty()) {
            serviceDef.put("environment", envVars);
        }

        if (isOnPlantNetwork(result.deploymentContext())) {
            serviceDef.put("network_mode", "host");
        }

        services.put(safeName, serviceDef);
        compose.put("services", services);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);

        String output = yaml.dump(compose);
        log.debug("Generated Docker Compose YAML ({} chars)", output.length());
        return output;
    }

    private List<String> buildEnvironmentVariables(ServiceMatchResult result) {
        List<String> env = new ArrayList<>();

        if (result.protocol() != null) {
            env.add("PROTOCOL=" + result.protocol());
        }
        if (result.accessMode() != null) {
            env.add("ACCESS_MODE=" + result.accessMode());
        }
        if (result.deploymentContext() != null) {
            env.add("DEPLOYMENT_CONTEXT=" + result.deploymentContext());
        }
        env.add("RECALL=" + result.recall());
        env.add("LATENCY_MS=" + result.latencyMs());

        return env;
    }

    private boolean isOnPlantNetwork(String deploymentContext) {
        return deploymentContext != null
            && deploymentContext.toLowerCase().contains("plant");
    }

    private static String toServiceName(String serviceName) {
        if (serviceName == null) return "unknown-service";
        return serviceName.toLowerCase()
            .replaceAll("[^a-z0-9-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
