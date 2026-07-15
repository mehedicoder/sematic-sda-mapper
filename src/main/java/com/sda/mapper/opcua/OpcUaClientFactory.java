package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory for creating and managing OPC UA client connections.
 */
@Component
public class OpcUaClientFactory {

    private final ConcurrentMap<String, OpcUaClientWrapper> clients = new ConcurrentHashMap<>();

    /**
     * Creates a new OPC UA client wrapper for the given endpoint configuration.
     */
    public OpcUaClientWrapper createClient(OpcUaEndpointConfig config) {
        String key = config.getEndpointUrl();
        
        return clients.computeIfAbsent(key, k -> {
            OpcUaClientWrapper client = new OpcUaClientWrapper();
            return client;
        });
    }

    /**
     * Gets an existing client for the given endpoint URL.
     */
    public OpcUaClientWrapper getClient(String endpointUrl) {
        return clients.get(endpointUrl);
    }

    /**
     * Removes a client from the factory.
     */
    public void removeClient(String endpointUrl) {
        clients.remove(endpointUrl);
    }

    /**
     * Disconnects all clients.
     */
    public void disconnectAll() {
        clients.values().forEach(OpcUaClientWrapper::disconnect);
        clients.clear();
    }
}
