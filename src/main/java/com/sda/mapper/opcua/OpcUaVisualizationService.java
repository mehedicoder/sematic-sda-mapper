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
        new KnownNode("Tank1.Level", "Level", "cm"),
        new KnownNode("Tank1.FlowRateIn", "Flow In", "L/min"),
        new KnownNode("Tank1.FlowRateOut", "Flow Out", "L/min"),
        new KnownNode("Tank1.Temperature", "Temperature", "°C"),
        new KnownNode("Pump.FlowRate", "Flow Rate", "L/min"),
        new KnownNode("Pump.Vibration", "Vibration", "mm/s"),
        new KnownNode("Pump.Temperature", "Pump Temp", "°C"),
        new KnownNode("Pump.Status", "Status", "")
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
            serverInfo.endpointUrl(),
            serverInfo.type().name(),
            connected,
            nodeValues
        );
    }

    private Map<String, NodeValue> buildFallbackNodeValues() {
        Map<String, NodeValue> values = new LinkedHashMap<>();
        Ne107DiagnosticSimulator.DiagnosticResult diagResult =
            diagnostics.simulatePumpDiagnostics(
                "DeviceStatus",
                pumpModel.getHealthPercent(),
                pumpModel.getVibration(),
                pumpModel.getTemperature());

        values.put("Tank1.Level", new NodeValue("Level", String.format("%.1f", tankState.getLevelCm()), "cm", tankState.getLevelCm()));
        values.put("Tank1.FlowRateIn", new NodeValue("Flow In", String.format("%.2f", tankState.getFlowInLMin()), "L/min", tankState.getFlowInLMin()));
        values.put("Tank1.FlowRateOut", new NodeValue("Flow Out", String.format("%.2f", tankState.getFlowOutLMin()), "L/min", tankState.getFlowOutLMin()));
        values.put("Tank1.Temperature", new NodeValue("Temperature", String.format("%.1f", tankState.getTemperatureC()), "°C", tankState.getTemperatureC()));
        values.put("Pump.FlowRate", new NodeValue("Flow Rate", String.format("%.2f", pumpModel.getEffectiveFlowLMin()), "L/min", pumpModel.getEffectiveFlowLMin()));
        values.put("Pump.Vibration", new NodeValue("Vibration", String.format("%.2f", pumpModel.getVibration()), "mm/s", pumpModel.getVibration()));
        values.put("Pump.Temperature", new NodeValue("Pump Temp", String.format("%.1f", pumpModel.getTemperature()), "°C", pumpModel.getTemperature()));
        values.put("Pump.Status", new NodeValue("Status", diagResult.diagnosticString(), "", diagResult.diagnosticString()));
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
        return result;
    }

    private void browseRecursive(OpcUaClientWrapper client, NodeId nodeId,
                                  List<BrowsedNode> result, int depth, int maxDepth) {
        if (depth >= maxDepth) return;

        List<ReferenceDescription> refs;
        try {
            refs = client.browse(nodeId).get(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return;
        }

        var nsTable = client.getNamespaceTable();
        for (ReferenceDescription ref : refs) {
            NodeId childNodeId = ref.getNodeId().toNodeId(nsTable).orElse(null);
            if (childNodeId == null) continue;

            boolean isVariable = isVariableNode(ref, nsTable);
            if (isVariable) {
                String browseName = ref.getBrowseName() != null ? ref.getBrowseName().getName() : "";
                String displayName = ref.getDisplayName() != null
                    ? ref.getDisplayName().getText() : browseName;
                String nodeIdStr = String.format("ns=%d;%s", childNodeId.getNamespaceIndex().intValue(),
                    childNodeId.getIdentifier());

                Object rawValue = null;
                String displayValue = "—";
                String dataType = "";
                try {
                    OpcUaNodeConfig nodeConfig = OpcUaNodeConfig.ofString(
                        childNodeId.getNamespaceIndex().intValue(),
                        String.valueOf(childNodeId.getIdentifier()));
                    rawValue = client.readVariableValue(nodeConfig).get(2000, TimeUnit.MILLISECONDS);
                    displayValue = rawValue != null ? String.valueOf(rawValue) : "—";
                    dataType = rawValue != null ? rawValue.getClass().getSimpleName() : "";
                } catch (Exception e) {
                    displayValue = "error";
                }

                result.add(new BrowsedNode(nodeIdStr, browseName, displayName,
                    displayValue, dataType, rawValue));
            } else {
                browseRecursive(client, childNodeId, result, depth + 1, maxDepth);
            }
        }
    }

    private boolean isVariableNode(ReferenceDescription ref, org.eclipse.milo.opcua.stack.core.NamespaceTable nsTable) {
        NodeId typeDef = ref.getTypeDefinition()
            .toNodeId(nsTable)
            .orElse(null);
        if (typeDef == null) return false;
        return Identifiers.BaseDataVariableType.equals(typeDef)
            || Identifiers.PropertyType.equals(typeDef)
            || Identifiers.DataItemType.equals(typeDef)
            || Identifiers.AnalogItemType.equals(typeDef);
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
            readDouble(client, OpcUaNodeConfig.ofString(2, "Tank1.Level")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "Tank1.FlowRateIn")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "Tank1.FlowRateOut")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "Tank1.Temperature")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "Pump.FlowRate")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "Pump.Vibration")),
            readDouble(client, OpcUaNodeConfig.ofString(2, "Pump.Temperature")),
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
            Object value = client.readVariableValue(OpcUaNodeConfig.ofString(2, "Pump.Status")).join();
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
}
