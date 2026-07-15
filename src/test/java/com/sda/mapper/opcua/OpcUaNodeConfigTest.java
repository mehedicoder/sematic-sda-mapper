package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaNodeConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA Node Config")
class OpcUaNodeConfigTest {

    @Test
    @DisplayName("Should store namespace index, identifier, and browse path")
    void shouldStoreAllFields() {
        OpcUaNodeConfig config = new OpcUaNodeConfig(2, "FlowRate", "/Objects/Tank/Pump1");

        assertEquals(2, config.namespaceIndex());
        assertEquals("FlowRate", config.identifier());
        assertEquals("/Objects/Tank/Pump1", config.browsePath());
    }

    @Test
    @DisplayName("Should create node config from string identifier")
    void shouldCreateFromStringIdentifier() {
        OpcUaNodeConfig config = OpcUaNodeConfig.ofString(1, "TemperatureSensor");

        assertEquals(1, config.namespaceIndex());
        assertEquals("TemperatureSensor", config.identifier());
        assertNull(config.browsePath());
    }

    @Test
    @DisplayName("Should create node config from numeric identifier")
    void shouldCreateFromNumericIdentifier() {
        OpcUaNodeConfig config = OpcUaNodeConfig.ofNumeric(0, 2258);

        assertEquals(0, config.namespaceIndex());
        assertEquals("2258", config.identifier());
        assertNull(config.browsePath());
    }

    @Test
    @DisplayName("Should return correct NodeId string for string identifier")
    void shouldReturnNodeIdStringForStringIdentifier() {
        OpcUaNodeConfig config = OpcUaNodeConfig.ofString(2, "FlowRate");

        assertEquals("ns=2;FlowRate", config.toNodeIdString());
    }

    @Test
    @DisplayName("Should return correct NodeId string for numeric identifier")
    void shouldReturnNodeIdStringForNumericIdentifier() {
        OpcUaNodeConfig config = OpcUaNodeConfig.ofNumeric(0, 2258);

        assertEquals("ns=0;2258", config.toNodeIdString());
    }

    @Test
    @DisplayName("Should return correct NodeId string with browse path")
    void shouldReturnNodeIdStringWithBrowsePath() {
        OpcUaNodeConfig config = new OpcUaNodeConfig(3, "PumpStatus", "/Objects/Pump");

        assertEquals("ns=3;PumpStatus", config.toNodeIdString());
    }

    @Test
    @DisplayName("Should handle namespace index zero")
    void shouldHandleNamespaceIndexZero() {
        OpcUaNodeConfig config = OpcUaNodeConfig.ofNumeric(0, 85);

        assertEquals("ns=0;85", config.toNodeIdString());
        assertEquals(0, config.namespaceIndex());
    }

    @Test
    @DisplayName("Should support equality via record semantics")
    void shouldSupportEquality() {
        OpcUaNodeConfig config1 = new OpcUaNodeConfig(1, "Sensor1", "/Objects/Sensor");
        OpcUaNodeConfig config2 = new OpcUaNodeConfig(1, "Sensor1", "/Objects/Sensor");

        assertEquals(config1, config2);
    }

    @Test
    @DisplayName("Should not be equal when identifiers differ")
    void shouldNotBeEqualWhenIdentifiersDiffer() {
        OpcUaNodeConfig config1 = OpcUaNodeConfig.ofString(1, "FlowRate");
        OpcUaNodeConfig config2 = OpcUaNodeConfig.ofString(1, "Temperature");

        assertNotEquals(config1, config2);
    }

    @Test
    @DisplayName("Should not be equal when namespace indices differ")
    void shouldNotBeEqualWhenNamespaceIndicesDiffer() {
        OpcUaNodeConfig config1 = OpcUaNodeConfig.ofString(1, "FlowRate");
        OpcUaNodeConfig config2 = OpcUaNodeConfig.ofString(2, "FlowRate");

        assertNotEquals(config1, config2);
    }
}
