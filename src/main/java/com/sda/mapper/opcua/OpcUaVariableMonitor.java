package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * Monitors OPC UA variables and publishes updates.
 */
@Component
public class OpcUaVariableMonitor {

    private static final Logger log = LoggerFactory.getLogger(OpcUaVariableMonitor.class);
    private static final int POLL_INTERVAL_MS = 500;

    private final OpcUaClientFactory clientFactory;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentMap<String, ScheduledFuture<?>> monitors = new ConcurrentHashMap<>();

    public OpcUaVariableMonitor(OpcUaClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /**
     * Starts monitoring a variable.
     */
    public void startMonitoring(String endpointUrl, OpcUaNodeConfig nodeConfig, 
                                 java.util.function.Consumer<Object> callback) {
        String key = endpointUrl + ":" + nodeConfig.toNodeIdString();

        if (monitors.containsKey(key)) {
            log.warn("Already monitoring variable: {}", key);
            return;
        }

        OpcUaClientWrapper client = clientFactory.createClient(
            OpcUaEndpointConfig.local(
                extractHost(endpointUrl),
                extractPort(endpointUrl),
                extractPath(endpointUrl)
            )
        );

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!client.isConnected()) {
                    client.connect(OpcUaEndpointConfig.local(
                        extractHost(endpointUrl),
                        extractPort(endpointUrl),
                        extractPath(endpointUrl)
                    )).join();
                }

                Object value = client.readVariableValue(nodeConfig).join();
                callback.accept(value);
            } catch (Exception e) {
                log.error("Failed to read variable: {}", key, e);
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);

        monitors.put(key, future);
        log.info("Started monitoring variable: {}", key);
    }

    /**
     * Stops monitoring a variable.
     */
    public void stopMonitoring(String endpointUrl, OpcUaNodeConfig nodeConfig) {
        String key = endpointUrl + ":" + nodeConfig.toNodeIdString();
        ScheduledFuture<?> future = monitors.remove(key);
        
        if (future != null) {
            future.cancel(false);
            log.info("Stopped monitoring variable: {}", key);
        }
    }

    /**
     * Stops all monitors.
     */
    public void stopAll() {
        monitors.values().forEach(future -> future.cancel(false));
        monitors.clear();
        scheduler.shutdown();
    }

    private String extractHost(String endpointUrl) {
        // opc.tcp://host:port/path -> host
        String withoutProtocol = endpointUrl.replace("opc.tcp://", "");
        return withoutProtocol.split(":")[0];
    }

    private int extractPort(String endpointUrl) {
        // opc.tcp://host:port/path -> port
        String withoutProtocol = endpointUrl.replace("opc.tcp://", "");
        String hostPort = withoutProtocol.split("/")[0];
        return Integer.parseInt(hostPort.split(":")[1]);
    }

    private String extractPath(String endpointUrl) {
        // opc.tcp://host:port/path -> /path
        String withoutProtocol = endpointUrl.replace("opc.tcp://", "");
        String withoutHost = withoutProtocol.split("/", 1)[1];
        return "/" + withoutHost;
    }
}
