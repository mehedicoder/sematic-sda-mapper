package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA Client Wrapper")
class OpcUaClientWrapperTest {

    private OpcUaClientWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new OpcUaClientWrapper();
    }

    @AfterEach
    void tearDown() {
        wrapper.disconnect().join();
    }

    @Test
    @DisplayName("Should report not connected when no connection has been made")
    void shouldReportNotConnectedInitially() {
        assertFalse(wrapper.isConnected());
    }

    @Test
    @DisplayName("Should return null config when no connection has been made")
    void shouldReturnNullConfigInitially() {
        assertNull(wrapper.getCurrentConfig());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when reading variable while disconnected")
    void shouldThrowOnReadWhenDisconnected() {
        OpcUaNodeConfig nodeConfig = OpcUaNodeConfig.ofNumeric(2, 2258);

        assertThrows(IllegalStateException.class,
            () -> wrapper.readVariableValue(nodeConfig));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when writing variable while disconnected")
    void shouldThrowOnWriteWhenDisconnected() {
        OpcUaNodeConfig nodeConfig = OpcUaNodeConfig.ofNumeric(2, 2258);

        assertThrows(IllegalStateException.class,
            () -> wrapper.writeVariableValue(nodeConfig, 42.0));
    }

    @Test
    @DisplayName("Should return completed future when disconnecting while already disconnected")
    void shouldReturnCompletedFutureOnDisconnectWhenDisconnected() {
        var future = wrapper.disconnect();

        assertNotNull(future);
        assertTrue(future.isDone());
        assertNull(future.join());
    }

    @Test
    @DisplayName("Should be idempotent when disconnecting multiple times")
    void shouldBeIdempotentOnMultipleDisconnects() {
        wrapper.disconnect().join();
        wrapper.disconnect().join();

        assertFalse(wrapper.isConnected());
    }

    @Test
    @DisplayName("Should handle connection failure to non-existent server gracefully")
    void shouldHandleConnectionFailureGracefully() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 1, "/UA/Test");

        CompletionException ex = assertThrows(CompletionException.class,
            () -> wrapper.connect(config).join());

        assertInstanceOf(RuntimeException.class, ex.getCause());
        assertFalse(wrapper.isConnected());
    }

    @Test
    @DisplayName("Should store endpoint config after failed connection attempt")
    void shouldStoreConfigAfterFailedConnection() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 1, "/UA/Test");

        try {
            wrapper.connect(config).join();
        } catch (Exception ignored) {
        }

        assertEquals(config, wrapper.getCurrentConfig());
    }

    @Test
    @DisplayName("Should not be connected after failed connection attempt")
    void shouldNotBeConnectedAfterFailedConnection() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 1, "/UA/Test");

        try {
            wrapper.connect(config).join();
        } catch (Exception ignored) {
        }

        assertFalse(wrapper.isConnected());
    }

    @Test
    @DisplayName("Should disconnect cleanly after failed connection attempt")
    void shouldDisconnectCleanly() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 1, "/UA/Test");

        try {
            wrapper.connect(config).join();
        } catch (Exception ignored) {
        }

        wrapper.disconnect().join();

        assertFalse(wrapper.isConnected());
    }
}
