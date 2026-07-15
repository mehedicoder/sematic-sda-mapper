package com.sda.mapper.opcua.model;

/**
 * Configuration for an OPC UA node.
 */
public record OpcUaNodeConfig(
    int namespaceIndex,
    String identifier,
    String browsePath
) {
    /**
     * Creates a node configuration from a string identifier.
     */
    public static OpcUaNodeConfig ofString(int namespaceIndex, String identifier) {
        return new OpcUaNodeConfig(namespaceIndex, identifier, null);
    }

    /**
     * Creates a node configuration from a numeric identifier.
     */
    public static OpcUaNodeConfig ofNumeric(int namespaceIndex, int identifier) {
        return new OpcUaNodeConfig(namespaceIndex, String.valueOf(identifier), null);
    }

    /**
     * Returns the node ID as a string.
     */
    public String toNodeIdString() {
        return String.format("ns=%d;%s", namespaceIndex, identifier);
    }
}
