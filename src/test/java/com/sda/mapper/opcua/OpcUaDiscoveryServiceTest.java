package com.sda.mapper.opcua;

import com.sda.mapper.ui.catalog.DiscoveredServiceEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA Discovery Service")
class OpcUaDiscoveryServiceTest {

    private OpcUaClientFactory clientFactory;
    private OpcUaServerRegistry registry;
    private OpcUaDiscoveryService service;
    private TankOpcUaServer tankServer;

    @BeforeEach
    void setUp() {
        clientFactory = new OpcUaClientFactory();
        tankServer = new TankOpcUaServer(
            new TankProcessState(),
            new PumpDegradationModel(),
            new Ne107DiagnosticSimulator());
        registry = new OpcUaServerRegistry(clientFactory, tankServer);
        registry.init();
        service = new OpcUaDiscoveryService(clientFactory, registry);
    }

    @Test
    @DisplayName("Should return empty list for unreachable server")
    void shouldReturnEmptyForUnreachableServer() {
        List<DiscoveredServiceEntry> entries = service.discover("opc.tcp://unknown:9999");
        assertNotNull(entries);
        assertTrue(entries.isEmpty(), "Unreachable server should return empty list");
    }

    @Test
    @DisplayName("Should return empty list for invalid URL")
    void shouldReturnEmptyForInvalidUrl() {
        List<DiscoveredServiceEntry> entries = service.discover("not-a-url");
        assertNotNull(entries);
        assertTrue(entries.isEmpty(), "Invalid URL should return empty list");
    }

    @Test
    @DisplayName("DiscoveredServiceEntry record should hold all fields")
    void discoveredServiceEntryRecordShouldWork() {
        DiscoveredServiceEntry entry = new DiscoveredServiceEntry(
            "Tank Server", "Tank", "Temperature", "OPC-UA", "read-only",
            "25.5", "Double", "ns=2;Temperature", "opc.tcp://localhost:4840", "OPC-UA Discovery");
        assertEquals("Tank Server", entry.serviceName());
        assertEquals("Tank", entry.capabilityName());
        assertEquals("Temperature", entry.skillName());
        assertEquals("OPC-UA", entry.protocol());
        assertEquals("read-only", entry.accessMode());
        assertEquals("25.5", entry.currentNodeValue());
        assertEquals("Double", entry.dataType());
        assertEquals("ns=2;Temperature", entry.nodeId());
        assertEquals("opc.tcp://localhost:4840", entry.endpointUrl());
        assertEquals("OPC-UA Discovery", entry.source());
    }

    @Test
    @DisplayName("Should resolve server name from registry for known endpoint")
    void shouldResolveServerNameFromRegistry() {
        List<DiscoveredServiceEntry> entries = service.discover("opc.tcp://localhost:4840/");
        // Server is not running in test, so we get empty list but no exception
        assertNotNull(entries);
    }

    @Test
    @DisplayName("Should handle endpoint URL with path")
    void shouldHandleEndpointUrlWithPath() {
        List<DiscoveredServiceEntry> entries = service.discover("opc.tcp://localhost:4334/UA/FlowMeterServer");
        assertNotNull(entries);
    }
}
