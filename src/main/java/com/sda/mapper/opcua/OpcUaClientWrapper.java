package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.NamespaceTable;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper around Eclipse Milo OPC UA client for reading, writing, and browsing variables.
 */
@Component
public class OpcUaClientWrapper {

    private static final Logger log = LoggerFactory.getLogger(OpcUaClientWrapper.class);
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private OpcUaClient client;
    private OpcUaEndpointConfig currentConfig;
    private volatile boolean connected = false;

    /**
     * Connects to an OPC UA server. Reuses the existing session if already connected
     * to the same endpoint, avoiding session exhaustion on the server.
     */
    public CompletableFuture<Void> connect(OpcUaEndpointConfig config) {
        String endpointUrl = config.getEndpointUrl();

        if (connected && client != null
            && config.getEndpointUrl().equals(
                currentConfig != null ? currentConfig.getEndpointUrl() : null)) {
            log.debug("Reusing existing OPC UA connection to {}", endpointUrl);
            return CompletableFuture.completedFuture(null);
        }

        this.currentConfig = config;
        log.info("Connecting to OPC UA server: {}", endpointUrl);

        return CompletableFuture.runAsync(() -> {
            try {
                if (client != null) {
                    try {
                        client.disconnect().get(1000, TimeUnit.MILLISECONDS);
                    } catch (Exception ignored) {
                    }
                }
                client = OpcUaClient.create(endpointUrl);
                client.connect().get(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                connected = true;
                log.info("Connected to OPC UA server: {}", endpointUrl);
            } catch (Exception e) {
                log.error("Failed to connect to OPC UA server: {}", endpointUrl, e);
                client = null;
                connected = false;
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
        ReadValueId readValueId = new ReadValueId(nodeId, UInteger.valueOf(13), null, null);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                DataValue dataValue = client.read(0.0, TimestampsToReturn.Both, List.of(readValueId))
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
     * Browses the children of a node and returns reference descriptions.
     *
     * @param nodeId the node to browse (null for root/ObjectsFolder)
     * @return list of child reference descriptions
     */
    public CompletableFuture<List<ReferenceDescription>> browse(NodeId nodeId) {
        if (client == null) {
            throw new IllegalStateException("Client not connected");
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                BrowseDescription browse = new BrowseDescription(
                    nodeId,
                    BrowseDirection.Forward,
                    Identifiers.HierarchicalReferences,
                    Boolean.TRUE,
                    UInteger.valueOf(0),
                    UInteger.valueOf(0x3F)
                );

                BrowseResult result = client.browse(browse)
                    .get(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                List<ReferenceDescription> refs = new ArrayList<>();
                ReferenceDescription[] references = result.getReferences();
                if (references != null) {
                    for (ReferenceDescription ref : references) {
                        refs.add(ref);
                    }
                }
                return refs;
            } catch (Exception e) {
                log.error("Failed to browse node {}: {}", nodeId, e.getMessage());
                return List.of();
            }
        });
    }

    /**
     * Reads the browse name and value of a node.
     */
    public CompletableFuture<NodeSnapshot> readNodeSnapshot(OpcUaNodeConfig nodeConfig) {
        if (client == null) {
            throw new IllegalStateException("Client not connected");
        }

        NodeId nodeId = new NodeId(nodeConfig.namespaceIndex(), nodeConfig.identifier());
        ReadValueId readValueId = new ReadValueId(nodeId, UInteger.valueOf(13), null, null);

        return CompletableFuture.supplyAsync(() -> {
            try {
                DataValue dataValue = client.read(0.0, TimestampsToReturn.Both, List.of(readValueId))
                    .get(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .getResults()[0];

                Variant value = dataValue.getValue();
                Object rawValue = value != null ? value.getValue() : null;
                String displayValue = rawValue != null ? String.valueOf(rawValue) : "—";
                return new NodeSnapshot(nodeConfig.toNodeIdString(), displayValue, rawValue);
            } catch (Exception e) {
                log.debug("Failed to read node snapshot {}: {}", nodeConfig.toNodeIdString(), e.getMessage());
                return new NodeSnapshot(nodeConfig.toNodeIdString(), "error", null);
            }
        });
    }

    /**
     * Disconnects from the OPC UA server.
     */
    public CompletableFuture<Void> disconnect() {
        if (client == null) {
            connected = false;
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
                connected = false;
            }
        });
    }

    /**
     * Checks if the client is connected.
     */
    public boolean isConnected() {
        return connected && client != null;
    }

    /**
     * Returns the current endpoint configuration.
     */
    public OpcUaEndpointConfig getCurrentConfig() {
        return currentConfig;
    }

    /**
     * Returns the namespace table from the connected client.
     */
    public NamespaceTable getNamespaceTable() {
        if (client == null) {
            return new NamespaceTable();
        }
        return client.getNamespaceTable();
    }

    public record NodeSnapshot(String nodeId, String displayValue, Object rawValue) {
    }
}
