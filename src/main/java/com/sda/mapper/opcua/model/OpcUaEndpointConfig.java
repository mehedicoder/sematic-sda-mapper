package com.sda.mapper.opcua.model;

/**
 * Configuration for an OPC UA endpoint.
 */
public record OpcUaEndpointConfig(
    String host,
    int port,
    String resourcePath,
    SecurityPolicy securityPolicy,
    AuthPolicy authPolicy,
    boolean useSsl
) {
    /**
     * Security policy for OPC UA connection.
     */
    public enum SecurityPolicy {
        None,
        Basic128Rsa15,
        Basic256,
        Basic256Sha256
    }

    /**
     * Authentication policy for OPC UA connection.
     */
    public enum AuthPolicy {
        Anonymous,
        UserName,
        Certificate
    }

    /**
     * Returns the endpoint URL.
     */
    public String getEndpointUrl() {
        String protocol = useSsl ? "opc.tcp" : "opc.tcp";
        return String.format("%s://%s:%d%s", protocol, host, port, resourcePath);
    }

    /**
     * Creates a default endpoint configuration for local simulation.
     */
    public static OpcUaEndpointConfig local(String host, int port, String resourcePath) {
        return new OpcUaEndpointConfig(host, port, resourcePath, SecurityPolicy.None, AuthPolicy.Anonymous, false);
    }

    /**
     * Creates an endpoint configuration for secure connection.
     */
    public static OpcUaEndpointConfig secure(String host, int port, String resourcePath) {
        return new OpcUaEndpointConfig(host, port, resourcePath, SecurityPolicy.Basic256Sha256, AuthPolicy.Anonymous, true);
    }
}
