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

        UaObjectNode tankFolder = createObject(null, ns, "Tank1", "Tank1");
        registerNode(tankFolder, Identifiers.ObjectsFolder);

        createDoubleVariable(tankFolder, ns, "Level", tankState.getLevelCm());
        createDoubleVariable(tankFolder, ns, "FlowRateIn", tankState.getFlowInLMin());
        createDoubleVariable(tankFolder, ns, "FlowRateOut", tankState.getFlowOutLMin());
        createDoubleVariable(tankFolder, ns, "Temperature", tankState.getTemperatureC());

        UaObjectNode pumpFolder = createObject(tankFolder, ns, "Pump", "Pump");
        registerNode(pumpFolder, tankFolder.getNodeId());

        createDoubleVariable(pumpFolder, ns, "FlowRate", pumpModel.getEffectiveFlowLMin());
        createDoubleVariable(pumpFolder, ns, "Vibration", pumpModel.getVibration());
        createDoubleVariable(pumpFolder, ns, "Temperature", pumpModel.getTemperature());
        createDoubleVariable(pumpFolder, ns, "Status", 0.0);

        createSetInletFlowMethod(tankFolder, ns);

        log.info("TankNamespace created: ns={}", ns);
    }

    private void createSetInletFlowMethod(UaObjectNode tankFolder, int ns) {
        NodeId methodNodeId = new NodeId(ns, "Tank1.SetInletFlow");
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
                "inletFlowLMin", Identifiers.Double, -1,
                new UInteger[]{}, LocalizedText.english("Inlet flow rate in L/min")
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
                return new CallMethodResult(
                    StatusCode.GOOD,
                    new StatusCode[]{StatusCode.GOOD},
                    new DiagnosticInfo[]{},
                    new Variant[]{}
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
        setVariableValue(new NodeId(ns, "Tank1.Level"), tankState.getLevelCm());
        setVariableValue(new NodeId(ns, "Tank1.FlowRateIn"), tankState.getFlowInLMin());
        setVariableValue(new NodeId(ns, "Tank1.FlowRateOut"), tankState.getFlowOutLMin());
        setVariableValue(new NodeId(ns, "Tank1.Temperature"), tankState.getTemperatureC());

        setVariableValue(new NodeId(ns, "Pump.FlowRate"), pumpModel.getEffectiveFlowLMin());
        setVariableValue(new NodeId(ns, "Pump.Vibration"), pumpModel.getVibration());
        setVariableValue(new NodeId(ns, "Pump.Temperature"), pumpModel.getTemperature());

        Ne107DiagnosticSimulator.DiagnosticResult result =
            diagnostics.simulatePumpDiagnostics(
                "DeviceStatus",
                pumpModel.getHealthPercent(),
                pumpModel.getVibration(),
                pumpModel.getTemperature());
        setVariableValue(new NodeId(ns, "Pump.Status"), (double) result.getStatusCode());
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

    private UaObjectNode createObject(UaObjectNode parent, int ns, String browseName, String name) {
        NodeId nodeId = new NodeId(ns, name);
        UaObjectNode object = new UaObjectNode(
            getNodeContext(),
            nodeId,
            new QualifiedName(ns, name),
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

    private void createDoubleVariable(UaObjectNode parent, int ns, String name, double initialValue) {
        NodeId nodeId = new NodeId(ns, parent.getNodeId().getIdentifier() + "." + name);
        UaVariableNode variable = new UaVariableNode(
            getNodeContext(),
            nodeId,
            new QualifiedName(ns, name),
            LocalizedText.english(name)
        );
        variable.setDataType(Identifiers.Double);
        variable.setValue(new DataValue(new Variant(initialValue)));
        variable.setValueRank(0);
        variable.setAccessLevel(UByte.valueOf(1));
        variable.setUserAccessLevel(UByte.valueOf(1));

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
                    tankState.stepSimulation(1.0);
                    pumpModel.stepSimulation(1.0);
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
