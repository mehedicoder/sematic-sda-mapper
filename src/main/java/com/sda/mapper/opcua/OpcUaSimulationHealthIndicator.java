package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Health indicator for OPC UA simulation servers.
 */
@Component
public class OpcUaSimulationHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(OpcUaSimulationHealthIndicator.class);

    private final OpcUaClientFactory clientFactory;
    private boolean healthy = false;
    private String lastError = "";

    @Value("${simulation.opcua.enabled:true}")
    private boolean simulationEnabled;

    @Value("${simulation.tank.host:localhost}")
    private String tankHost;

    @Value("${simulation.tank.port:4334}")
    private int tankPort;

    public OpcUaSimulationHealthIndicator(OpcUaClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Health health() {
        if (simulationEnabled) {
            return Health.unknown()
                .withDetail("opcua-external", "not-needed")
                .withDetail("hint", "Built-in Java OPC UA server is active on port 4840")
                .build();
        }

        String endpointUrl = String.format("opc.tcp://%s:%d/UA/FlowMeterServer", tankHost, tankPort);

        try {
            OpcUaClientWrapper client = clientFactory.getClient(endpointUrl);

            if (client != null && client.isConnected()) {
                healthy = true;
                return Health.up()
                    .withDetail("opcua-server", "connected")
                    .withDetail("endpoint", endpointUrl)
                    .build();
            } else {
                healthy = false;
                return Health.unknown()
                    .withDetail("opcua-server", "not-connected")
                    .withDetail("endpoint", endpointUrl)
                    .withDetail("hint", "OPC UA server not configured or not running")
                    .build();
            }
        } catch (Exception e) {
            healthy = false;
            lastError = e.getMessage();
            log.error("OPC UA health check failed", e);
            return Health.down()
                .withDetail("opcua-server", "error")
                .withDetail("error", lastError)
                .build();
        }
    }

    /**
     * Returns whether the OPC UA server is healthy.
     */
    public boolean isHealthy() {
        return healthy;
    }
}
