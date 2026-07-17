package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Registry of all known OPC UA servers. Enumerates servers from application config
 * and tracks their live connection status.
 */
@Component
public class OpcUaServerRegistry {

    private static final Logger log = LoggerFactory.getLogger(OpcUaServerRegistry.class);
    private static final int PROBE_TIMEOUT_MS = 2000;

    private final OpcUaClientFactory clientFactory;
    private final TankOpcUaServer tankServer;

    @Value("${simulation.opcua.enabled:false}")
    private boolean simulationEnabled;

    @Value("${simulation.opcua.port:4840}")
    private int simulationPort;

    @Value("${simulation.tank.host:localhost}")
    private String tankHost;

    @Value("${simulation.tank.port:4334}")
    private int tankPort;

    @Value("${simulation.tank.path:/UA/FlowMeterServer}")
    private String tankPath;

    private final Map<String, ServerInfo> knownServers = new ConcurrentHashMap<>();
    private final List<ServerInfoListener> listeners = new ArrayList<>();

    public OpcUaServerRegistry(OpcUaClientFactory clientFactory, TankOpcUaServer tankServer) {
        this.clientFactory = clientFactory;
        this.tankServer = tankServer;
    }

    @PostConstruct
    public void init() {
        registerServers();
        log.info("OPC UA Server Registry initialized with {} servers", knownServers.size());
    }

    private void registerServers() {
        if (simulationEnabled) {
            OpcUaEndpointConfig builtInConfig = OpcUaEndpointConfig.local("localhost", simulationPort, "/");
            knownServers.put(builtInConfig.getEndpointUrl(), new ServerInfo(
                "Built-in Tank Server",
                "localhost",
                simulationPort,
                "/",
                builtInConfig.getEndpointUrl(),
                ServerType.JAVA_MILO,
                tankServer.isRunning()
            ));
        }

        OpcUaEndpointConfig externalConfig = OpcUaEndpointConfig.local(tankHost, tankPort, tankPath);
        knownServers.put(externalConfig.getEndpointUrl(), new ServerInfo(
            "External Tank Simulation",
            tankHost,
            tankPort,
            tankPath,
            externalConfig.getEndpointUrl(),
            ServerType.NODE_JS,
            false
        ));
    }

    public List<ServerInfo> getAllServers() {
        return List.copyOf(knownServers.values());
    }

    public ServerInfo getServer(String endpointUrl) {
        return knownServers.get(endpointUrl);
    }

    public void addCustomServer(String name, String host, int port, String path) {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local(host, port, path);
        ServerInfo info = new ServerInfo(name, host, port, path, config.getEndpointUrl(), ServerType.CUSTOM, false);
        knownServers.put(config.getEndpointUrl(), info);
        log.info("Added custom OPC UA server: {} at {}", name, config.getEndpointUrl());
        notifyListeners();
    }

    public void removeCustomServer(String endpointUrl) {
        knownServers.remove(endpointUrl);
        clientFactory.removeClient(endpointUrl);
        log.info("Removed OPC UA server: {}", endpointUrl);
        notifyListeners();
    }

    public ServerInfo probeServer(String endpointUrl) {
        OpcUaEndpointConfig config = parseEndpointUrl(endpointUrl);
        if (config == null) {
            return null;
        }

        OpcUaClientWrapper client = clientFactory.createClient(config);
        boolean connected = false;
        try {
            client.connect(config).get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            connected = client.isConnected();
        } catch (Exception e) {
            log.debug("Probe failed for {}: {}", endpointUrl, e.getMessage());
        }

        ServerInfo existing = knownServers.get(endpointUrl);
        ServerInfo updated = new ServerInfo(
            existing != null ? existing.name() : "Discovered Server",
            config.host(),
            config.port(),
            config.resourcePath(),
            endpointUrl,
            existing != null ? existing.type() : ServerType.DISCOVERED,
            connected
        );
        knownServers.put(endpointUrl, updated);
        notifyListeners();
        return updated;
    }

    public List<ServerInfo> probeAllServers() {
        List<ServerInfo> results = new ArrayList<>();
        for (String url : knownServers.keySet()) {
            results.add(probeServer(url));
        }
        return results;
    }

    public void addListener(ServerInfoListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (ServerInfoListener listener : listeners) {
            try {
                listener.onServersChanged(getAllServers());
            } catch (Exception e) {
                log.debug("Listener notification failed: {}", e.getMessage());
            }
        }
    }

    private OpcUaEndpointConfig parseEndpointUrl(String url) {
        try {
            String withoutProtocol = url.replace("opc.tcp://", "");
            String[] hostPort = withoutProtocol.split(":", 2);
            String host = hostPort[0];
            String portAndPath = hostPort.length > 1 ? hostPort[1] : "4840/";
            int slashIdx = portAndPath.indexOf('/');
            int port = slashIdx > 0 ? Integer.parseInt(portAndPath.substring(0, slashIdx)) : Integer.parseInt(portAndPath);
            String path = slashIdx > 0 ? portAndPath.substring(slashIdx) : "/";
            return OpcUaEndpointConfig.local(host, port, path);
        } catch (Exception e) {
            log.debug("Failed to parse endpoint URL: {}", url);
            return null;
        }
    }

    public record ServerInfo(
        String name,
        String host,
        int port,
        String path,
        String endpointUrl,
        ServerType type,
        boolean connected
    ) {
    }

    public enum ServerType {
        JAVA_MILO,
        NODE_JS,
        NODE_RED,
        CUSTOM,
        DISCOVERED
    }

    @FunctionalInterface
    public interface ServerInfoListener {
        void onServersChanged(List<ServerInfo> servers);
    }
}
