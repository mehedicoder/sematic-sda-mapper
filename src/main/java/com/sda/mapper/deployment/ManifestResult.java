package com.sda.mapper.deployment;

/**
 * Result of manifest generation containing both Docker Compose and Kubernetes YAML.
 *
 * @param dockerComposeYaml the generated Docker Compose YAML content
 * @param kubernetesYaml    the generated Kubernetes Deployment YAML content
 * @param serviceName       the name of the matched service
 * @param containerImage    the container image reference
 */
public record ManifestResult(String dockerComposeYaml, String kubernetesYaml,
                              String serviceName, String containerImage) {
}
