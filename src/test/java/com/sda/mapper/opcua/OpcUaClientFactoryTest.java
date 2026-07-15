package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA Client Factory")
class OpcUaClientFactoryTest {

    private OpcUaClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = new OpcUaClientFactory();
    }

    @AfterEach
    void tearDown() {
        factory.disconnectAll();
    }

    @Test
    @DisplayName("Should create client wrapper from valid endpoint config")
    void shouldCreateClientFromValidConfig() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 4840, "/UA/Test");

        OpcUaClientWrapper client = factory.createClient(config);

        assertNotNull(client);
        assertFalse(client.isConnected());
    }

    @Test
    @DisplayName("Should return same client for identical endpoint URL")
    void shouldReturnSameClientForIdenticalUrl() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 4840, "/UA/Test");

        OpcUaClientWrapper first = factory.createClient(config);
        OpcUaClientWrapper second = factory.createClient(config);

        assertSame(first, second);
    }

    @Test
    @DisplayName("Should create different clients for different endpoint URLs")
    void shouldCreateDifferentClientsForDifferentUrls() {
        OpcUaEndpointConfig config1 = OpcUaEndpointConfig.local("localhost", 4840, "/UA/Server1");
        OpcUaEndpointConfig config2 = OpcUaEndpointConfig.local("localhost", 4841, "/UA/Server2");

        OpcUaClientWrapper client1 = factory.createClient(config1);
        OpcUaClientWrapper client2 = factory.createClient(config2);

        assertNotSame(client1, client2);
    }

    @Test
    @DisplayName("Should return null for unknown endpoint URL")
    void shouldReturnNullForUnknownUrl() {
        OpcUaClientWrapper client = factory.getClient("opc.tcp://unknown:4840/UA/Test");

        assertNull(client);
    }

    @Test
    @DisplayName("Should return client after creation via getClient")
    void shouldReturnClientAfterCreation() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 4840, "/UA/Test");

        factory.createClient(config);
        OpcUaClientWrapper client = factory.getClient(config.getEndpointUrl());

        assertNotNull(client);
    }

    @Test
    @DisplayName("Should remove client for known endpoint URL")
    void shouldRemoveClientForKnownUrl() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 4840, "/UA/Test");

        factory.createClient(config);
        factory.removeClient(config.getEndpointUrl());
        OpcUaClientWrapper client = factory.getClient(config.getEndpointUrl());

        assertNull(client);
    }

    @Test
    @DisplayName("Should handle removing non-existent client gracefully")
    void shouldHandleRemovingNonExistentClient() {
        factory.removeClient("opc.tcp://nonexistent:4840/UA/Test");
    }

    @Test
    @DisplayName("Should clear all clients on disconnectAll")
    void shouldClearAllClientsOnDisconnectAll() {
        OpcUaEndpointConfig config1 = OpcUaEndpointConfig.local("localhost", 4840, "/UA/Server1");
        OpcUaEndpointConfig config2 = OpcUaEndpointConfig.local("localhost", 4841, "/UA/Server2");

        factory.createClient(config1);
        factory.createClient(config2);

        factory.disconnectAll();

        assertNull(factory.getClient(config1.getEndpointUrl()));
        assertNull(factory.getClient(config2.getEndpointUrl()));
    }

    @Test
    @DisplayName("Should handle disconnectAll when no clients exist")
    void shouldHandleDisconnectAllWhenEmpty() {
        factory.disconnectAll();
    }
}
