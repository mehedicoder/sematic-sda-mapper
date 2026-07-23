package com.sda.mapper.ui.catalog;

/**
 * Read-only DTO representing a single service entry from the CSS-SDA ontology catalog.
 * Populated by SPARQL query results and displayed in the Service Catalog UI.
 */
public record ServiceCatalogEntry(
    String serviceName,
    String capabilityName,
    String skillName,
    String protocol,
    String accessMode,
    String securityMode,
    double recall,
    int latencyMs,
    String containerImage,
    String connectorName,
    String informationModel,
    String deploymentName,
    String safetyIntegrityLevel,
    String determinismClass,
    String networkZone,
    String source
) {
    public String recallFormatted() {
        return String.format("%.2f", recall);
    }

    public String latencyFormatted() {
        return latencyMs + "ms";
    }
}
