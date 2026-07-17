package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.BrowseDirection;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class OpcUaVisualizationService {

    private static final Logger log = LoggerFactory.getLogger(OpcUaVisualizationService.class);

    private static final List<KnownNode> KNOWN_NODES = List.of(
        new KnownNode("Level", "Level", "cm"),
        new KnownNode("FlowRateIn", "Flow In", "L/min"),
        new KnownNode("FlowRateOut", "Flow Out", "L/min"),
        new KnownNode("Temperature", "Temperature", "°C"),
        new KnownNode("FlowRate", "Flow Rate", "L/min"),
        new KnownNode("Vibration", "Vibration", "mm/s"),
        new KnownNode("PumpTemperature", "Pump Temp", "°C"),
        new KnownNode("Status", "Status", ""),
        new KnownNode("FlowRate_Status", "Flow Diag Status", ""),
        new KnownNode("FlowRate_Diag", "Flow Diag", ""),
        new KnownNode("Vibration_Status", "Vib Diag Status", ""),
        new KnownNode("Vibration_Diag", "Vib Diag", ""),
        new KnownNode("PumpTemperature_Status", "Temp Diag Status", ""),
        new KnownNode("PumpTemperature_Diag", "Temp Diag", "")
    );

    private final OpcUaClientFactory clientFactory;
    private final OpcUaServerRegistry serverRegistry;
    private final TankProcessState tankState;
    private final PumpDegradationModel pumpModel;
    private final Ne107DiagnosticSimulator diagnostics;

    @Value("${simulation.opcua.port:4840}")
    private int opcUaPort;

    public OpcUaVisualizationService(OpcUaClientFactory clientFactory,
                                      OpcUaServerRegistry serverRegistry,
                                      TankProcessState tankState,
                                      PumpDegradationModel pumpModel,
                                      Ne107DiagnosticSimulator diagnostics) {
        this.clientFactory = clientFactory;
        this.serverRegistry = serverRegistry;
        this.tankState = tankState;
        this.pumpModel = pumpModel;
        this.diagnostics = diagnostics;
    }

    /**
     * Returns a snapshot of all servers with their current status and node values.
     */
    public List<ServerSnapshot> getAllServerSnapshots() {
        List<ServerSnapshot> snapshots = new ArrayList<>();
        for (OpcUaServerRegistry.ServerInfo server : serverRegistry.getAllServers()) {
            snapshots.add(getServerSnapshot(server));
        }
        return snapshots;
    }

    /**
     * Returns a snapshot for a single server by endpoint URL.
     */
    public ServerSnapshot getServerSnapshot(OpcUaServerRegistry.ServerInfo serverInfo) {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local(
            serverInfo.host(), serverInfo.port(), serverInfo.path());
        OpcUaClientWrapper client = clientFactory.createClient(config);

        boolean connected = false;
        Map<String, NodeValue> nodeValues = new LinkedHashMap<>();

        try {
            client.connect(config).get(3000, TimeUnit.MILLISECONDS);
            connected = client.isConnected();
        } catch (Exception e) {
            log.debug("Could not connect to {}: {}", serverInfo.endpointUrl(), e.getMessage());
        }

        if (connected) {
            for (KnownNode knownNode : KNOWN_NODES) {
                OpcUaNodeConfig nodeConfig = OpcUaNodeConfig.ofString(2, knownNode.nodeId());
                try {
                    Object value = client.readVariableValue(nodeConfig).get(2000, TimeUnit.MILLISECONDS);
                    String display = formatValue(value, knownNode);
                    nodeValues.put(knownNode.nodeId(), new NodeValue(knownNode.label(), display, knownNode.unit(), value));
                } catch (Exception e) {
                    nodeValues.put(knownNode.nodeId(), new NodeValue(knownNode.label(), "—", knownNode.unit(), null));
                }
            }
        } else {
            if ("Built-in Tank Server".equals(serverInfo.name()) || serverInfo.port() == 4840) {
                nodeValues.putAll(buildFallbackNodeValues());
            }
        }

        return new ServerSnapshot(
            serverInfo.name(),
            serverInfo.host(),
            serverInfo.port(),
            serverInfo.path(),
            serverInfo.endpointUrl(),
            serverInfo.type().name(),
            connected,
            nodeValues
        );
    }

    private Map<String, NodeValue> buildFallbackNodeValues() {
        Map<String, NodeValue> values = new LinkedHashMap<>();

        values.put("Level", new NodeValue("Level", String.format("%.1f", tankState.getLevelCm()), "cm", tankState.getLevelCm()));
        values.put("FlowRateIn", new NodeValue("Flow In", String.format("%.2f", tankState.getFlowInLMin()), "L/min", tankState.getFlowInLMin()));
        values.put("FlowRateOut", new NodeValue("Flow Out", String.format("%.2f", tankState.getFlowOutLMin()), "L/min", tankState.getFlowOutLMin()));
        values.put("Temperature", new NodeValue("Temperature", String.format("%.1f", tankState.getTemperatureC()), "°C", tankState.getTemperatureC()));
        values.put("FlowRate", new NodeValue("Flow Rate", String.format("%.2f", pumpModel.getEffectiveFlowLMin()), "L/min", pumpModel.getEffectiveFlowLMin()));
        values.put("Vibration", new NodeValue("Vibration", String.format("%.2f", pumpModel.getVibration()), "mm/s", pumpModel.getVibration()));
        values.put("PumpTemperature", new NodeValue("Pump Temp", String.format("%.1f", pumpModel.getTemperature()), "°C", pumpModel.getTemperature()));

        Ne107DiagnosticSimulator.DiagnosticResult deviceDiag =
            diagnostics.simulatePumpDiagnostics("DeviceStatus",
                pumpModel.getHealthPercent(), pumpModel.getVibration(), pumpModel.getTemperature());
        values.put("Status", new NodeValue("Status", String.valueOf(deviceDiag.getStatusCode()), "", deviceDiag.getStatusCode()));

        String[][] diagVars = {{"FlowRate", "Flow Rate"}, {"Vibration", "Vibration"}, {"PumpTemperature", "Temperature"}};
        for (String[] pair : diagVars) {
            Ne107DiagnosticSimulator.DiagnosticResult result =
                diagnostics.simulatePumpDiagnostics(pair[1],
                    pumpModel.getHealthPercent(), pumpModel.getVibration(), pumpModel.getTemperature());
            values.put(pair[0] + "_Status", new NodeValue(pair[0] + " Diag Status",
                String.valueOf(result.getStatusCode()), "", result.getStatusCode()));
            values.put(pair[0] + "_Diag", new NodeValue(pair[0] + " Diag",
                result.diagnosticString(), "", result.diagnosticString()));
        }

        return values;
    }

    private String formatValue(Object value, KnownNode node) {
        if (value == null) return "—";
        if (value instanceof Number n) {
            return String.format("%.2f", n.doubleValue());
        }
        return String.valueOf(value);
    }

    /**
     * Browses all variable nodes on a server and reads their current values.
     */
    public List<BrowsedNode> browseServerNodes(String endpointUrl) {
        OpcUaServerRegistry.ServerInfo serverInfo = serverRegistry.getServer(endpointUrl);
        if (serverInfo == null) {
            log.debug("No server info found for {}", endpointUrl);
            return List.of();
        }

        OpcUaEndpointConfig config = OpcUaEndpointConfig.local(
            serverInfo.host(), serverInfo.port(), serverInfo.path());
        OpcUaClientWrapper client = clientFactory.createClient(config);

        try {
            client.connect(config).get(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("Could not connect to {} for browsing: {}", endpointUrl, e.getMessage());
            return List.of();
        }

        List<BrowsedNode> result = new ArrayList<>();
        try {
            browseRecursive(client, Identifiers.ObjectsFolder, result, 0, 3);
        } catch (Exception e) {
            log.debug("Browse failed for {}: {}", endpointUrl, e.getMessage());
        }
        log.debug("Browsed {} nodes from {}", result.size(), endpointUrl);
        return result;
    }

    private void browseRecursive(OpcUaClientWrapper client, NodeId nodeId,
                                  List<BrowsedNode> result, int depth, int maxDepth) {
        if (depth >= maxDepth) return;

        List<ReferenceDescription> refs;
        try {
            refs = client.browse(nodeId).get(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("Browse failed for node {}: {}", nodeId, e.getMessage());
            return;
        }

        var nsTable = client.getNamespaceTable();
        for (ReferenceDescription ref : refs) {
            NodeId childNodeId = ref.getNodeId().toNodeId(nsTable).orElse(null);
            if (childNodeId == null) {
                log.debug("Could not resolve ExpandedNodeId for ref: {}", ref.getBrowseName());
                continue;
            }

            String browseName = ref.getBrowseName() != null ? ref.getBrowseName().getName() : "";
            String displayName = ref.getDisplayName() != null
                ? ref.getDisplayName().getText() : browseName;
            String nodeIdStr = String.format("ns=%d;%s", childNodeId.getNamespaceIndex().intValue(),
                childNodeId.getIdentifier());

            // Try to read value — if it succeeds, this is a variable node
            try {
                OpcUaNodeConfig nodeConfig = OpcUaNodeConfig.ofString(
                    childNodeId.getNamespaceIndex().intValue(),
                    String.valueOf(childNodeId.getIdentifier()));
                Object rawValue = client.readVariableValue(nodeConfig).get(2000, TimeUnit.MILLISECONDS);
                String displayValue = rawValue != null ? String.valueOf(rawValue) : "—";
                String dataType = rawValue != null ? rawValue.getClass().getSimpleName() : "";
                result.add(new BrowsedNode(nodeIdStr, browseName, displayName,
                    displayValue, dataType, rawValue));
            } catch (Exception e) {
                // Read failed — this is likely an object/method folder, recurse into it
                browseRecursive(client, childNodeId, result, depth + 1, maxDepth);
            }
        }
    }

    /**
     * Legacy method: returns the old OpcUaSnapshot for backward compatibility.
     */
    public OpcUaSnapshot getCurrentSnapshot() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", opcUaPort, "/");
        OpcUaClientWrapper client = clientFactory.createClient(config);

        if (!client.isConnected()) {
            try {
                client.connect(config).join();
            } catch (Exception e) {
                return buildSnapshotFromModel();
            }
        }

        return readSnapshotFromServer(client);
    }

    private OpcUaSnapshot readSnapshotFromServer(OpcUaClientWrapper client) {
        return new OpcUaSnapshot(
            readDouble(client, OpcUaNodeConfig.ofString(2, "Level")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "FlowRateIn")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "FlowRateOut")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "Temperature")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "FlowRate")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "Vibration")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "PumpTemperature")),
            readPumpStatus(client)
        );
    }

    private OpcUaSnapshot buildSnapshotFromModel() {
        Ne107DiagnosticSimulator.DiagnosticResult diagResult =
            diagnostics.simulatePumpDiagnostics(
                "DeviceStatus",
                pumpModel.getHealthPercent(),
                pumpModel.getVibration(),
                pumpModel.getTemperature());

        return new OpcUaSnapshot(
            tankState.getLevelCm(),
            tankState.getFlowInLMin(),
            tankState.getFlowOutLMin(),
            tankState.getTemperatureC(),
            pumpModel.getEffectiveFlowLMin(),
            pumpModel.getVibration(),
            pumpModel.getTemperature(),
            diagResult.diagnosticString()
        );
    }

    private Double readDouble(OpcUaClientWrapper client, OpcUaNodeConfig node) {
        try {
            Object value = client.readVariableValue(node).join();
            if (value instanceof Number n) return n.doubleValue();
            return null;
        } catch (Exception e) {
            log.debug("Failed to read {}: {}", node.toNodeIdString(), e.getMessage());
            return null;
        }
    }

    private String readPumpStatus(OpcUaClientWrapper client) {
        try {
            Object value = client.readVariableValue(OpcUaNodeConfig.ofString(2, "Status")).join();
            if (value instanceof Number n) {
                int statusCode = n.intValue();
                return switch (statusCode) {
                    case 0 -> "Good";
                    case 1 -> "Maintenance";
                    case 2 -> "FunctionCheck";
                    case 3 -> "Failure";
                    default -> "Unknown(" + statusCode + ")";
                };
            }
            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private record KnownNode(String nodeId, String label, String unit) {
    }

    public record ServerSnapshot(
        String name,
        String host,
        int port,
        String path,
        String endpointUrl,
        String type,
        boolean connected,
        Map<String, NodeValue> nodeValues
    ) {
    }

    public record NodeValue(String label, String displayValue, String unit, Object rawValue) {
    }

    public record BrowsedNode(String nodeId, String browseName, String displayName,
                               String displayValue, String dataType, Object rawValue) {
    }

    public record BrowseTreeNode(
        String nodeId,
        String browseName,
        String displayName,
        String nodeClass,
        Object value,
        String dataType,
        boolean hasChildren
    ) {
    }

    /**
     * Returns root-level children of a server's address space (children of ObjectsFolder).
     * Used to populate the initial tree view.
     */
    public List<BrowseTreeNode> browseAddressSpace(String endpointUrl) {
        return browseChildren(endpointUrl, Identifiers.ObjectsFolder);
    }

    /**
     * Returns children of a specific node for lazy tree expansion.
     * Per OPC UA Part 5 §8.2, browses HierarchicalReferences forward.
     */
    public List<BrowseTreeNode> browseChildren(String endpointUrl, NodeId parentNodeId) {
        OpcUaServerRegistry.ServerInfo serverInfo = serverRegistry.getServer(endpointUrl);
        if (serverInfo == null) {
            log.debug("No server info found for {}", endpointUrl);
            return List.of();
        }

        OpcUaEndpointConfig config = OpcUaEndpointConfig.local(
            serverInfo.host(), serverInfo.port(), serverInfo.path());
        OpcUaClientWrapper client = clientFactory.createClient(config);

        try {
            client.connect(config).get(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("Could not connect to {} for browsing: {}", endpointUrl, e.getMessage());
            return List.of();
        }

        List<BrowseTreeNode> result = new ArrayList<>();
        var nsTable = client.getNamespaceTable();

        List<ReferenceDescription> refs;
        try {
            refs = client.browse(parentNodeId).get(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("Browse failed for node {}: {}", parentNodeId, e.getMessage());
            return List.of();
        }

        for (ReferenceDescription ref : refs) {
            NodeId childNodeId = ref.getNodeId().toNodeId(nsTable).orElse(null);
            if (childNodeId == null) {
                continue;
            }

            String browseName = ref.getBrowseName() != null ? ref.getBrowseName().getName() : "";
            String displayName = ref.getDisplayName() != null
                ? ref.getDisplayName().getText() : browseName;
            String nodeIdStr = String.format("ns=%d;%s",
                childNodeId.getNamespaceIndex().intValue(), childNodeId.getIdentifier());
            String nodeClass = ref.getNodeClass() != null ? ref.getNodeClass().name() : "Unknown";

            boolean isVariable = "Variable".equals(nodeClass);
            Object value = null;
            String dataType = "";

            if (isVariable) {
                try {
                    OpcUaNodeConfig nodeConfig = OpcUaNodeConfig.ofString(
                        childNodeId.getNamespaceIndex().intValue(),
                        String.valueOf(childNodeId.getIdentifier()));
                    Object rawValue = client.readVariableValue(nodeConfig).get(2000, TimeUnit.MILLISECONDS);
                    value = rawValue;
                    dataType = rawValue != null ? rawValue.getClass().getSimpleName() : "";
                } catch (Exception e) {
                    log.debug("Failed to read {}: {}", nodeIdStr, e.getMessage());
                }
            }

            boolean hasChildren = !isVariable;
            result.add(new BrowseTreeNode(nodeIdStr, browseName, displayName,
                nodeClass, value, dataType, hasChildren));
        }

        log.debug("Browsed {} children from {} on {}", result.size(), parentNodeId, endpointUrl);
        return result;
    }
}
