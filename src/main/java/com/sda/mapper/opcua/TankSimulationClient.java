package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

/**
 * Client for connecting to the Node.js tank simulation server.
 */
@Component
public class TankSimulationClient {

    private static final Logger log = LoggerFactory.getLogger(TankSimulationClient.class);

    private final OpcUaClientFactory clientFactory;

    @Value("${simulation.tank.host:localhost}")
    private String host;

    @Value("${simulation.tank.port:4334}")
    private int port;

    @Value("${simulation.tank.path:/UA/FlowMeterServer}")
    private String path;

    private OpcUaClientWrapper client;

    public TankSimulationClient(OpcUaClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @PostConstruct
    public void connect() {
        try {
            OpcUaEndpointConfig config = OpcUaEndpointConfig.local(host, port, path);
            client = clientFactory.createClient(config);
            client.connect(config).join();
            log.info("Connected to tank simulation server at {}:{}", host, port);
        } catch (Exception e) {
            log.warn("Failed to connect to tank simulation server: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        if (client != null) {
            client.disconnect().join();
        }
    }

    /**
     * Reads the tank level.
     */
    public CompletableFuture<Double> readLevel() {
        OpcUaNodeConfig node = OpcUaNodeConfig.ofString(2, "s=Level");
        return client.readVariableValue(node).thenApply(v -> (Double) v);
    }

    /**
     * Reads the pump vibration.
     */
    public CompletableFuture<Double> readPumpVibration() {
        OpcUaNodeConfig node = OpcUaNodeConfig.ofString(2, "s=Vibration");
        return client.readVariableValue(node).thenApply(v -> (Double) v);
    }

    /**
     * Reads the pump temperature.
     */
    public CompletableFuture<Double> readPumpTemperature() {
        OpcUaNodeConfig node = OpcUaNodeConfig.ofString(2, "s=Temperature");
        return client.readVariableValue(node).thenApply(v -> (Double) v);
    }

    /**
     * Returns whether the client is connected.
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
