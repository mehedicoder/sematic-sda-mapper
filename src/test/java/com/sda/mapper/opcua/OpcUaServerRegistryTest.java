package com.sda.mapper.opcua;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA Server Registry")
class OpcUaServerRegistryTest {

    private OpcUaServerRegistry registry;
    private OpcUaClientFactory clientFactory;
    private TankOpcUaServer tankServer;

    @BeforeEach
    void setUp() {
        clientFactory = new OpcUaClientFactory();
        tankServer = new TankOpcUaServer(
            new TankProcessState(),
            new PumpDegradationModel(),
            new Ne107DiagnosticSimulator());
        registry = new OpcUaServerRegistry(clientFactory, tankServer);
    }

    @Test
    @DisplayName("Should register servers on init when simulation is enabled")
    void shouldRegisterServers() {
        registry.init();
        assertFalse(registry.getAllServers().isEmpty(), "Should have at least one registered server");
    }

    @Test
    @DisplayName("Should add a custom server")
    void shouldAddCustomServer() {
        registry.init();
        int before = registry.getAllServers().size();
        registry.addCustomServer("Test Server", "10.0.0.1", 4840, "/UA/Test");
        assertEquals(before + 1, registry.getAllServers().size());
    }

    @Test
    @DisplayName("Should remove a custom server")
    void shouldRemoveCustomServer() {
        registry.init();
        registry.addCustomServer("Temp Server", "10.0.0.2", 4841, "/");
        int afterAdd = registry.getAllServers().size();
        registry.removeCustomServer("opc.tcp://10.0.0.2:4841/");
        assertEquals(afterAdd - 1, registry.getAllServers().size());
    }

    @Test
    @DisplayName("Should notify listeners when servers change")
    void shouldNotifyListeners() {
        registry.init();
        boolean[] notified = {false};
        registry.addListener(servers -> notified[0] = true);
        registry.addCustomServer("Listener Test", "10.0.0.3", 4842, "/");
        assertTrue(notified[0], "Listener should be notified");
    }

    @Test
    @DisplayName("Should return server by endpoint URL")
    void shouldReturnServerByEndpointUrl() {
        registry.init();
        registry.addCustomServer("Lookup Test", "10.0.0.4", 4843, "/UA/Lookup");
        OpcUaServerRegistry.ServerInfo info = registry.getServer("opc.tcp://10.0.0.4:4843/UA/Lookup");
        assertNotNull(info, "Should find server by endpoint URL");
        assertEquals("Lookup Test", info.name());
    }

    @Test
    @DisplayName("Should return null for unknown endpoint URL")
    void shouldReturnNullForUnknown() {
        registry.init();
        assertNull(registry.getServer("opc.tcp://unknown:9999/"), "Should return null for unknown URL");
    }
}
