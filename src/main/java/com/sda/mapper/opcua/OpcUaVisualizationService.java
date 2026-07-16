package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpcUaVisualizationService {

    private static final Logger log = LoggerFactory.getLogger(OpcUaVisualizationService.class);

    private static final OpcUaNodeConfig NODE_TANK_LEVEL =
        OpcUaNodeConfig.ofString(2, "s=Tank1.Level");
    private static final OpcUaNodeConfig NODE_TANK_FLOW_IN =
        OpcUaNodeConfig.ofString(2, "s=Tank1.FlowRateIn");
    private static final OpcUaNodeConfig NODE_TANK_FLOW_OUT =
        OpcUaNodeConfig.ofString(2, "s=Tank1.FlowRateOut");
    private static final OpcUaNodeConfig NODE_TANK_TEMP =
        OpcUaNodeConfig.ofString(2, "s=Tank1.Temperature");
    private static final OpcUaNodeConfig NODE_PUMP_FLOW =
        OpcUaNodeConfig.ofString(2, "s=Pump.FlowRate");
    private static final OpcUaNodeConfig NODE_PUMP_VIBRATION =
        OpcUaNodeConfig.ofString(2, "s=Pump.Vibration");
    private static final OpcUaNodeConfig NODE_PUMP_TEMP =
        OpcUaNodeConfig.ofString(2, "s=Pump.Temperature");
    private static final OpcUaNodeConfig NODE_PUMP_STATUS =
        OpcUaNodeConfig.ofString(2, "s=Pump.Status");

    private final OpcUaClientFactory clientFactory;
    private final TankProcessState tankState;
    private final PumpDegradationModel pumpModel;
    private final Ne107DiagnosticSimulator diagnostics;

    @Value("${simulation.opcua.port:4840}")
    private int opcUaPort;

    public OpcUaVisualizationService(OpcUaClientFactory clientFactory,
                                      TankProcessState tankState,
                                      PumpDegradationModel pumpModel,
                                      Ne107DiagnosticSimulator diagnostics) {
        this.clientFactory = clientFactory;
        this.tankState = tankState;
        this.pumpModel = pumpModel;
        this.diagnostics = diagnostics;
    }

    public OpcUaSnapshot getCurrentSnapshot() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", opcUaPort, "/");
        OpcUaClientWrapper client = clientFactory.createClient(config);

        if (!client.isConnected()) {
            try {
                client.connect(config).join();
            } catch (Exception e) {
                log.debug("Could not connect to OPC UA server: {}", e.getMessage());
                return buildSnapshotFromModel();
            }
        }

        return readSnapshotFromServer(client);
    }

    private OpcUaSnapshot readSnapshotFromServer(OpcUaClientWrapper client) {
        return new OpcUaSnapshot(
            readDouble(client, NODE_TANK_LEVEL),
            readDouble(client, NODE_TANK_FLOW_IN),
            readDouble(client, NODE_TANK_FLOW_OUT),
            readDouble(client, NODE_TANK_TEMP),
            readDouble(client, NODE_PUMP_FLOW),
            readDouble(client, NODE_PUMP_VIBRATION),
            readDouble(client, NODE_PUMP_TEMP),
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
            Object value = client.readVariableValue(NODE_PUMP_STATUS).join();
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
            log.debug("Failed to read pump status: {}", e.getMessage());
            return "Unknown";
        }
    }
}
