package com.sda.mapper.ui.catalog;

/**
 * DTO representing a service discovered from a live OPC UA server.
 * Each entry maps one OPC UA variable node to a catalog row.
 */
public record DiscoveredServiceEntry(
    String serviceName,
    String capabilityName,
    String skillName,
    String protocol,
    String accessMode,
    String currentNodeValue,
    String dataType,
    String nodeId,
    String endpointUrl,
    String source
) {
}
