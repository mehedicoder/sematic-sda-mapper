package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around Eclipse Milo OPC UA client for reading and writing variables.
 */
@Component
public class OpcUaClientWrapper {

    private static final Logger log = LoggerFactory.getLogger(OpcUaClientWrapper.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private OpcUaClient client;
    private OpcUaEndpointConfig currentConfig;

    /**
     * Connects to an OPC UA server.
     */
    public CompletableFuture<Void> connect(OpcUaEndpointConfig config) {
        this.currentConfig = config;
        log.info("Connecting to OPC UA server: {}", config.getEndpointUrl());

        return CompletableFuture.runAsync(() -> {
            try {
                client = OpcUaClient.create(config.getEndpointUrl());
                client.connect().get(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                log.info("Connected to OPC UA server: {}", config.getEndpointUrl());
            } catch (Exception e) {
                log.error("Failed to connect to OPC UA server: {}", config.getEndpointUrl(), e);
                throw new RuntimeException("Failed to connect to OPC UA server", e);
            }
        });
    }

    /**
     * Reads a variable value from the server.
     */
    public CompletableFuture<Object> readVariableValue(OpcUaNodeConfig nodeConfig) {
        if (client == null) {
            throw new IllegalStateException("Client not connected");
        }

        NodeId nodeId = new NodeId(nodeConfig.namespaceIndex(), nodeConfig.identifier());
        ReadValueId readValueId = new ReadValueId(nodeId, null, null, null);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                DataValue dataValue = client.read(0.0, null, List.of(readValueId))
                    .get(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .getResults()[0];
                
                Variant value = dataValue.getValue();
                log.debug("Read value from {}: {}", nodeConfig.toNodeIdString(), value);
                return value != null ? value.getValue() : null;
            } catch (Exception e) {
                log.error("Failed to read variable: {}", nodeConfig.toNodeIdString(), e);
                throw new RuntimeException("Failed to read variable", e);
            }
        });
    }

    /**
     * Writes a value to a variable on the server.
     */
    public CompletableFuture<StatusCode> writeVariableValue(OpcUaNodeConfig nodeConfig, Object value) {
        if (client == null) {
            throw new IllegalStateException("Client not connected");
        }

        NodeId nodeId = new NodeId(nodeConfig.namespaceIndex(), nodeConfig.identifier());
        WriteValue writeValue = new WriteValue(nodeId, null, null, new DataValue(new Variant(value)));
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                StatusCode statusCode = client.write(List.of(writeValue))
                    .get(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .getResults()[0];
                log.debug("Wrote value to {}: {}", nodeConfig.toNodeIdString(), statusCode);
                return statusCode;
            } catch (Exception e) {
                log.error("Failed to write variable: {}", nodeConfig.toNodeIdString(), e);
                throw new RuntimeException("Failed to write variable", e);
            }
        });
    }

    /**
     * Disconnects from the OPC UA server.
     */
    public CompletableFuture<Void> disconnect() {
        if (client == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                client.disconnect().get(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                log.info("Disconnected from OPC UA server");
            } catch (Exception e) {
                log.error("Failed to disconnect from OPC UA server", e);
            } finally {
                client = null;
            }
        });
    }

    /**
     * Checks if the client is connected.
     */
    public boolean isConnected() {
        return client != null;
    }

    /**
     * Returns the current endpoint configuration.
     */
    public OpcUaEndpointConfig getCurrentConfig() {
        return currentConfig;
    }
}
