package com.sda.mapper.opcua;

import com.sda.mapper.opcua.OpcUaVisualizationService.BrowsedNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpcUaVisualizationService browseServerNodes")
class OpcUaVisualizationServiceBrowseTest {

    private OpcUaClientFactory clientFactory;
    private OpcUaServerRegistry registry;
    private OpcUaVisualizationService service;
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
        service = new OpcUaVisualizationService(clientFactory, registry,
            new TankProcessState(), new PumpDegradationModel(), new Ne107DiagnosticSimulator());
    }

    @Test
    @DisplayName("Should return empty list for unknown endpoint URL")
    void shouldReturnEmptyForUnknownServer() {
        List<BrowsedNode> nodes = service.browseServerNodes("opc.tcp://unknown:9999/");
        assertNotNull(nodes);
        assertTrue(nodes.isEmpty(), "Unknown server should return empty list");
    }

    @Test
    @DisplayName("BrowsedNode record should hold all fields")
    void browsedNodeRecordShouldWork() {
        BrowsedNode node = new BrowsedNode("ns=2;Level", "Level", "Level", "50.0", "Double", 50.0);
        assertEquals("ns=2;Level", node.nodeId());
        assertEquals("Level", node.browseName());
        assertEquals("Level", node.displayName());
        assertEquals("50.0", node.displayValue());
        assertEquals("Double", node.dataType());
        assertEquals(50.0, node.rawValue());
    }
}
