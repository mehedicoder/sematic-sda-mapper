package com.sda.mapper.api;

import com.sda.mapper.opcua.OpcUaClientFactory;
import com.sda.mapper.opcua.OpcUaDiscoveryService;
import com.sda.mapper.opcua.OpcUaServerRegistry;
import com.sda.mapper.opcua.TankOpcUaServer;
import com.sda.mapper.opcua.TankProcessState;
import com.sda.mapper.opcua.PumpDegradationModel;
import com.sda.mapper.opcua.Ne107DiagnosticSimulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Discovery Catalog Controller")
class DiscoveryCatalogControllerTest {

    private MockMvc mockMvc;
    private OpcUaClientFactory clientFactory;
    private OpcUaServerRegistry registry;
    private OpcUaDiscoveryService discoveryService;
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
        discoveryService = new OpcUaDiscoveryService(clientFactory, registry);
        DiscoveryCatalogController controller = new DiscoveryCatalogController(discoveryService, registry);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/catalog/discover should accept JSON body")
    void shouldAcceptJsonBody() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/discover")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endpointUrl\":\"opc.tcp://unknown:9999\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/catalog/discover/servers should return server list")
    void shouldReturnServerList() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/discover/servers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
