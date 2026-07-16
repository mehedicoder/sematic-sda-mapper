package com.sda.mapper.reasoning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Generates human-readable explanations for semantic service match results.
 * Provides detailed reasoning about capability, protocol, deployment context,
 * recall, and latency constraint evaluation.
 */
@Service
public class ExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ExplanationService.class);

    /**
     * Generates a multi-line explanation for the given match result.
     * Covers capability match, protocol match, deployment context match,
     * recall and latency constraint evaluation.
     *
     * @param result the service match result to explain
     * @return a human-readable explanation string
     */
    public String explain(ServiceMatchResult result) {
        if (result == null) {
            return "No result to explain.";
        }

        if (!result.matched()) {
            return explainNoMatch(result);
        }

        log.debug("Generating explanation for matched service: {}", result.serviceName());

        StringBuilder sb = new StringBuilder();
        sb.append("=== Semantic Match Explanation ===\n\n");

        sb.append("Service: ").append(result.serviceName()).append("\n");
        sb.append("Container Image: ").append(
            result.containerImage() != null ? result.containerImage() : "N/A").append("\n\n");

        sb.append("--- Capability Match ---\n");
        if (result.capabilityName() != null) {
            sb.append("Capability '").append(result.capabilityName())
              .append("' was matched from the ontology.\n");
            sb.append("The service implements a skill that realises this capability.\n");
        } else {
            sb.append("Capability information not available in the match result.\n");
        }
        sb.append("\n");

        sb.append("--- Protocol Match ---\n");
        if (result.protocol() != null) {
            sb.append("Communication protocol: ").append(result.protocol()).append("\n");
            if (result.accessMode() != null) {
                sb.append("Access mode: ").append(result.accessMode()).append("\n");
            }
            sb.append("Protocol constraint satisfied by the matched CommunicationBinding.\n");
        } else {
            sb.append("Protocol information not available.\n");
        }
        sb.append("\n");

        sb.append("--- Deployment Context Match ---\n");
        if (result.deploymentContext() != null) {
            sb.append("Deployment context: ").append(result.deploymentContext()).append("\n");
            sb.append("The service is deployed in a context that satisfies the requirement.\n");
        } else {
            sb.append("Deployment context information not available in the match result.\n");
        }
        sb.append("\n");

        sb.append("--- Performance Constraints ---\n");
        sb.append("Reported recall: ").append(String.format(Locale.ROOT, "%.2f", result.recall())).append("\n");
        sb.append("Reported latency: ").append(result.latencyMs()).append(" ms\n");
        sb.append("Both recall and latency constraints are satisfied.\n");

        return sb.toString();
    }

    private String explainNoMatch(ServiceMatchResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== No Match Found ===\n\n");

        if (result.explanation() != null && !result.explanation().isBlank()) {
            sb.append("Reason: ").append(result.explanation()).append("\n\n");
        }

        sb.append("Potential causes:\n");
        sb.append("- No service in the ontology satisfies all required constraints.\n");
        sb.append("- Protocol or access mode may not match any CommunicationBinding.\n");
        sb.append("- Recall or latency thresholds may be too restrictive.\n");
        sb.append("- Deployment context (network zone, plant network) may not be available.\n");

        return sb.toString();
    }
}
