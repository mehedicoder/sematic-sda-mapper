package com.sda.mapper.opcua;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespace;
import org.eclipse.milo.opcua.sdk.server.api.SimpleAddressSpaceFilter;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.AddressSpaceFilter;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.api.methods.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DiagnosticInfo;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom OPC UA namespace that exposes Tank1 and Pump variables.
 * Extends ManagedNamespace to integrate with Milo's address space management.
 */
class TankNamespace extends ManagedNamespace {

    private static final Logger log = LoggerFactory.getLogger(TankNamespace.class);

    private final TankProcessState tankState;
    private final PumpDegradationModel pumpModel;
    private final Ne107DiagnosticSimulator diagnostics;

    private final Map<String, UaVariableNode> variableNodes = new ConcurrentHashMap<>();

    TankNamespace(OpcUaServer server, TankProcessState tankState,
                  PumpDegradationModel pumpModel, Ne107DiagnosticSimulator diagnostics) {
        super(server, "urn:sda:mapper:tank");
        this.tankState = tankState;
        this.pumpModel = pumpModel;
        this.diagnostics = diagnostics;

        int ns = getNamespaceIndex().intValue();

        // Tank1 object under ObjectsFolder
        UaObjectNode tankFolder = createObject(null, ns, "Tank1");
        registerNode(tankFolder, Identifiers.ObjectsFolder);

        createDoubleVariable(tankFolder, ns, "Level", tankState.getLevelCm(),
            UByte.valueOf(1), 200);
        createDoubleVariable(tankFolder, ns, "FlowRateIn", tankState.getFlowInLMin(),
            UByte.valueOf(3), 200);
        createDoubleVariable(tankFolder, ns, "FlowRateOut", tankState.getFlowOutLMin(),
            UByte.valueOf(3), 200);
        createDoubleVariable(tankFolder, ns, "Temperature", tankState.getTemperatureC(),
            UByte.valueOf(1), 500);

        // Pump1 object under Tank1
        UaObjectNode pumpFolder = createObject(tankFolder, ns, "Pump1");

        createDoubleVariable(pumpFolder, ns, "FlowRate", pumpModel.getEffectiveFlowLMin(),
            UByte.valueOf(1), 200);
        createDoubleVariable(pumpFolder, ns, "Vibration", pumpModel.getVibration(),
            UByte.valueOf(1), 200);
        createDoubleVariableWithId(pumpFolder, ns, "PumpTemperature", "Temperature",
            pumpModel.getTemperature(), UByte.valueOf(1), 500);

        // NE 107 aggregate status — UInt32 per NAMUR NE 107
        createUInt32Variable(pumpFolder, ns, "Status", 0, 500);

        // Per-variable NE 107 diagnostic variables
        createUInt32Variable(pumpFolder, ns, "FlowRate_Status", 0, 500);
        createStringVariable(pumpFolder, ns, "FlowRate_Diag", "", 500);
        createUInt32Variable(pumpFolder, ns, "Vibration_Status", 0, 500);
        createStringVariable(pumpFolder, ns, "Vibration_Diag", "", 500);
        createUInt32Variable(pumpFolder, ns, "Temperature_Status", 0, 500);
        createStringVariable(pumpFolder, ns, "Temperature_Diag", "", 500);

        // Methods
        createSetInletFlowMethod(tankFolder, ns);
        createSetDegradationMethod(pumpFolder, ns);

        log.info("TankNamespace created: ns={}", ns);
    }

    private void createSetInletFlowMethod(UaObjectNode tankFolder, int ns) {
        NodeId methodNodeId = new NodeId(ns, "SetInletFlow");
        UaMethodNode methodNode = new UaMethodNode(
            getNodeContext(),
            methodNodeId,
            new QualifiedName(ns, "SetInletFlow"),
            LocalizedText.english("SetInletFlow"),
            LocalizedText.english("Sets the inlet flow rate of the tank in L/min"),
            UInteger.valueOf(0),
            UInteger.valueOf(0),
            Boolean.TRUE,
            Boolean.TRUE
        );

        methodNode.setInputArguments(new org.eclipse.milo.opcua.stack.core.types.structured.Argument[]{
            new org.eclipse.milo.opcua.stack.core.types.structured.Argument(
                "value", Identifiers.Double, -1,
                new UInteger[]{}, LocalizedText.english("New inlet flow rate in L/min")
            )
        });

        methodNode.setOutputArguments(new org.eclipse.milo.opcua.stack.core.types.structured.Argument[]{
            new org.eclipse.milo.opcua.stack.core.types.structured.Argument(
                "result", Identifiers.String, -1,
                new UInteger[]{}, LocalizedText.english("Confirmation message")
            )
        });

        methodNode.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {
                Variant[] inputArgs = request.getInputArguments();
                if (inputArgs == null || inputArgs.length == 0) {
                    return new CallMethodResult(
                        StatusCode.BAD,
                        new StatusCode[]{StatusCode.BAD},
                        new DiagnosticInfo[]{},
                        new Variant[]{}
                    );
                }
                double inletFlow = ((Number) inputArgs[0].getValue()).doubleValue();
                tankState.setFlowInLMin(inletFlow);
                log.info("SetInletFlow invoked: inletFlow={} L/min", inletFlow);
                String msg = String.format("Inlet flow set to %.1f L/min", inletFlow);
                return new CallMethodResult(
                    StatusCode.GOOD,
                    new StatusCode[]{StatusCode.GOOD},
                    new DiagnosticInfo[]{},
                    new Variant[]{new Variant(msg)}
                );
            }
        });

        getNodeManager().addNode(methodNode);
        getNodeManager().addReference(new Reference(
            tankFolder.getNodeId(),
            Identifiers.HasComponent,
            methodNodeId.expanded(),
            true
        ));
    }

    private void createSetDegradationMethod(UaObjectNode pumpFolder, int ns) {
        NodeId methodNodeId = new NodeId(ns, "SetDegradation");
        UaMethodNode methodNode = new UaMethodNode(
            getNodeContext(),
            methodNodeId,
            new QualifiedName(ns, "SetDegradation"),
            LocalizedText.english("SetDegradation"),
            LocalizedText.english("Sets pump health percentage (0-100) for fault injection"),
            UInteger.valueOf(0),
            UInteger.valueOf(0),
            Boolean.TRUE,
            Boolean.TRUE
        );

        methodNode.setInputArguments(new org.eclipse.milo.opcua.stack.core.types.structured.Argument[]{
            new org.eclipse.milo.opcua.stack.core.types.structured.Argument(
                "healthPercent", Identifiers.Double, -1,
                new UInteger[]{}, LocalizedText.english("Health percentage (0-100)")
            )
        });

        methodNode.setOutputArguments(new org.eclipse.milo.opcua.stack.core.types.structured.Argument[]{
            new org.eclipse.milo.opcua.stack.core.types.structured.Argument(
                "result", Identifiers.String, -1,
                new UInteger[]{}, LocalizedText.english("Confirmation message")
            )
        });

        methodNode.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public CallMethodResult invoke(AccessContext accessContext, CallMethodRequest request) {
                Variant[] inputArgs = request.getInputArguments();
                if (inputArgs == null || inputArgs.length == 0) {
                    return new CallMethodResult(
                        StatusCode.BAD,
                        new StatusCode[]{StatusCode.BAD},
                        new DiagnosticInfo[]{},
                        new Variant[]{}
                    );
                }
                double health = Math.max(0, Math.min(100,
                    ((Number) inputArgs[0].getValue()).doubleValue()));
                pumpModel.setHealthPercent(health);
                log.info("SetDegradation invoked: health={}%", health);
                String msg = String.format("Pump health set to %.0f%%", health);
                return new CallMethodResult(
                    StatusCode.GOOD,
                    new StatusCode[]{StatusCode.GOOD},
                    new DiagnosticInfo[]{},
                    new Variant[]{new Variant(msg)}
                );
            }
        });

        getNodeManager().addNode(methodNode);
        getNodeManager().addReference(new Reference(
            pumpFolder.getNodeId(),
            Identifiers.HasComponent,
            methodNodeId.expanded(),
            true
        ));
    }

    @Override
    public AddressSpaceFilter getFilter() {
        int ns = getNamespaceIndex().intValue();
        return SimpleAddressSpaceFilter.create(
            nodeId -> nodeId.getNamespaceIndex().intValue() == ns
                || nodeId.equals(Identifiers.ObjectsFolder)
        );
    }

    void updateVariableValues() {
        int ns = getNamespaceIndex().intValue();

        // Tank variables
        setVariableValue(new NodeId(ns, "Level"), tankState.getLevelCm());
        setVariableValue(new NodeId(ns, "FlowRateIn"), tankState.getFlowInLMin());
        setVariableValue(new NodeId(ns, "FlowRateOut"), tankState.getFlowOutLMin());
        setVariableValue(new NodeId(ns, "Temperature"), tankState.getTemperatureC());

        // Pump variables — FlowRate is coupled to tank inflow via effective flow
        setVariableValue(new NodeId(ns, "FlowRate"), pumpModel.getEffectiveFlowLMin());
        setVariableValue(new NodeId(ns, "Vibration"), pumpModel.getVibration());
        setVariableValue(new NodeId(ns, "PumpTemperature"), pumpModel.getTemperature());

        // NE 107 aggregate status (UInt32)
        Ne107DiagnosticSimulator.DiagnosticResult deviceStatus =
            diagnostics.simulatePumpDiagnostics(
                "DeviceStatus",
                pumpModel.getHealthPercent(),
                pumpModel.getVibration(),
                pumpModel.getTemperature());
        setUInt32Value(new NodeId(ns, "Status"), deviceStatus.getStatusCode());

        // Per-variable NE 107 diagnostics
        String[] diagVars = {"FlowRate", "Vibration", "PumpTemperature"};
        for (String varName : diagVars) {
            Ne107DiagnosticSimulator.DiagnosticResult result =
                diagnostics.simulatePumpDiagnostics(
                    varName.equals("PumpTemperature") ? "Temperature" : varName,
                    pumpModel.getHealthPercent(),
                    pumpModel.getVibration(),
                    pumpModel.getTemperature());
            setUInt32Value(new NodeId(ns, varName + "_Status"), result.getStatusCode());
            setStringValue(new NodeId(ns, varName + "_Diag"), result.diagnosticString());
        }
    }

    @Override
    public void onDataItemsCreated(List<org.eclipse.milo.opcua.sdk.server.api.DataItem> dataItems) {
    }

    @Override
    public void onDataItemsModified(List<org.eclipse.milo.opcua.sdk.server.api.DataItem> dataItems) {
    }

    @Override
    public void onDataItemsDeleted(List<org.eclipse.milo.opcua.sdk.server.api.DataItem> dataItems) {
    }

    @Override
    public void onMonitoringModeChanged(List<org.eclipse.milo.opcua.sdk.server.api.MonitoredItem> monitoredItems) {
    }

    private void setVariableValue(NodeId nodeId, double value) {
        UaVariableNode node = variableNodes.get(nodeId.toString());
        if (node != null) {
            node.setValue(new DataValue(new Variant(value)));
        }
    }

    private void setUInt32Value(NodeId nodeId, int value) {
        UaVariableNode node = variableNodes.get(nodeId.toString());
        if (node != null) {
            node.setValue(new DataValue(new Variant(UInteger.valueOf(value))));
        }
    }

    private void setStringValue(NodeId nodeId, String value) {
        UaVariableNode node = variableNodes.get(nodeId.toString());
        if (node != null) {
            node.setValue(new DataValue(new Variant(value != null ? value : "")));
        }
    }

    private UaObjectNode createObject(UaObjectNode parent, int ns, String browseName) {
        NodeId nodeId = new NodeId(ns, browseName);
        UaObjectNode object = new UaObjectNode(
            getNodeContext(),
            nodeId,
            new QualifiedName(ns, browseName),
            LocalizedText.english(browseName)
        );
        getNodeManager().addNode(object);
        if (parent != null) {
            getNodeManager().addReference(new Reference(
                parent.getNodeId(),
                Identifiers.Organizes,
                nodeId.expanded(),
                true
            ));
        }
        return object;
    }

    private void createDoubleVariable(UaObjectNode parent, int ns, String name,
                                       double initialValue, UByte accessLevel,
                                       int samplingIntervalMs) {
        createDoubleVariableWithId(parent, ns, name, name, initialValue, accessLevel,
            samplingIntervalMs);
    }

    private void createDoubleVariableWithId(UaObjectNode parent, int ns, String nodeIdName,
                                             String browseName, double initialValue,
                                             UByte accessLevel, int samplingIntervalMs) {
        NodeId nodeId = new NodeId(ns, nodeIdName);
        UaVariableNode variable = new UaVariableNode(
            getNodeContext(),
            nodeId,
            new QualifiedName(ns, browseName),
            LocalizedText.english(browseName)
        );
        variable.setDataType(Identifiers.Double);
        variable.setValue(new DataValue(new Variant(initialValue)));
        variable.setValueRank(0);
        variable.setAccessLevel(accessLevel);
        variable.setUserAccessLevel(accessLevel);
        variable.setMinimumSamplingInterval((double) samplingIntervalMs);

        getNodeManager().addNode(variable);
        getNodeManager().addReference(new Reference(
            parent.getNodeId(),
            Identifiers.HasComponent,
            nodeId.expanded(),
            true
        ));

        variableNodes.put(nodeId.toString(), variable);
    }

    private void createUInt32Variable(UaObjectNode parent, int ns, String name,
                                       int initialValue, int samplingIntervalMs) {
        NodeId nodeId = new NodeId(ns, name);
        UaVariableNode variable = new UaVariableNode(
            getNodeContext(),
            nodeId,
            new QualifiedName(ns, name),
            LocalizedText.english(name)
        );
        variable.setDataType(Identifiers.UInt32);
        variable.setValue(new DataValue(new Variant(UInteger.valueOf(initialValue))));
        variable.setValueRank(0);
        variable.setAccessLevel(UByte.valueOf(1));
        variable.setUserAccessLevel(UByte.valueOf(1));
        variable.setMinimumSamplingInterval((double) samplingIntervalMs);

        getNodeManager().addNode(variable);
        getNodeManager().addReference(new Reference(
            parent.getNodeId(),
            Identifiers.HasComponent,
            nodeId.expanded(),
            true
        ));

        variableNodes.put(nodeId.toString(), variable);
    }

    private void createStringVariable(UaObjectNode parent, int ns, String name,
                                       String initialValue, int samplingIntervalMs) {
        NodeId nodeId = new NodeId(ns, name);
        UaVariableNode variable = new UaVariableNode(
            getNodeContext(),
            nodeId,
            new QualifiedName(ns, name),
            LocalizedText.english(name)
        );
        variable.setDataType(Identifiers.String);
        variable.setValue(new DataValue(new Variant(initialValue != null ? initialValue : "")));
        variable.setValueRank(0);
        variable.setAccessLevel(UByte.valueOf(1));
        variable.setUserAccessLevel(UByte.valueOf(1));
        variable.setMinimumSamplingInterval((double) samplingIntervalMs);

        getNodeManager().addNode(variable);
        getNodeManager().addReference(new Reference(
            parent.getNodeId(),
            Identifiers.HasComponent,
            nodeId.expanded(),
            true
        ));

        variableNodes.put(nodeId.toString(), variable);
    }

    private void registerNode(UaObjectNode node, NodeId parentNodeId) {
        getNodeManager().addReference(new Reference(
            parentNodeId,
            Identifiers.Organizes,
            node.getNodeId().expanded(),
            true
        ));
    }
}

/**
 * In-process OPC UA server for tank simulation using Eclipse Milo.
 * Exposes Tank1 and Pump variables over OPC UA for external consumers.
 *
 * <p>Server starts on {@code @PostConstruct} if {@code simulation.opcua.enabled=true}
 * and stops on {@code @PreDestroy}. The simulation tick runs at 1-second intervals,
 * calling {@link TankProcessState#stepSimulation} and {@link PumpDegradationModel#stepSimulation}
 * and updating all OPC UA variable values.</p>
 */
@org.springframework.stereotype.Component
public class TankOpcUaServer {

    private static final Logger log = LoggerFactory.getLogger(TankOpcUaServer.class);

    private final TankProcessState tankState;
    private final PumpDegradationModel pumpModel;
    private final Ne107DiagnosticSimulator diagnostics;

    @org.springframework.beans.factory.annotation.Value("${simulation.opcua.enabled:false}")
    private boolean enabled;

    @org.springframework.beans.factory.annotation.Value("${simulation.opcua.port:4840}")
    private int port;

    private OpcUaServer server;
    private TankNamespace tankNamespace;
    private volatile boolean running = false;

    private final java.util.concurrent.ScheduledExecutorService scheduler =
        java.util.concurrent.Executors.newScheduledThreadPool(1);

    public TankOpcUaServer(TankProcessState tankState, PumpDegradationModel pumpModel,
                           Ne107DiagnosticSimulator diagnostics) {
        this.tankState = tankState;
        this.pumpModel = pumpModel;
        this.diagnostics = diagnostics;
    }

    @jakarta.annotation.PostConstruct
    public void start() {
        if (!enabled) {
            log.info("OPC UA simulation server disabled");
            return;
        }

        try {
            log.info("Starting OPC UA simulation server on port {}", port);

            EndpointConfiguration endpoint = EndpointConfiguration.newBuilder()
                .setBindAddress("0.0.0.0")
                .setBindPort(port)
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None)
                .addTokenPolicies(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS)
                .build();

            OpcUaServerConfig config = OpcUaServerConfig.builder()
                .setApplicationName(LocalizedText.english("Tank OPC UA Server"))
                .setApplicationUri("urn:sda:mapper:tank-server")
                .setEndpoints(Set.of(endpoint))
                .build();

            server = new OpcUaServer(config);

            tankNamespace = new TankNamespace(server, tankState, pumpModel, diagnostics);
            server.getAddressSpaceManager().register(tankNamespace);

            server.startup().get();
            running = true;

            scheduler.scheduleAtFixedRate(() -> {
                if (running) {
                    pumpModel.stepSimulation(1.0);
                    tankState.setFlowInLMin(pumpModel.getEffectiveFlowLMin());
                    tankState.stepSimulation(1.0);
                    tankNamespace.updateVariableValues();
                }
            }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);

            log.info("OPC UA simulation server started on port {}", port);
        } catch (Exception e) {
            log.error("Failed to start OPC UA simulation server", e);
        }
    }

    @jakarta.annotation.PreDestroy
    public void stop() {
        running = false;
        scheduler.shutdown();
        if (server != null) {
            try {
                server.shutdown().get();
            } catch (Exception e) {
                log.error("Error stopping OPC UA server", e);
            }
        }
        log.info("OPC UA simulation server stopped");
    }

    /**
     * Returns whether the server is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the server port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns whether the server is running.
     */
    public boolean isRunning() {
        return running;
    }
}
