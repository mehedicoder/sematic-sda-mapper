package com.sda.mapper.ui;

import com.sda.mapper.opcua.OpcUaClientFactory;
import com.sda.mapper.opcua.OpcUaDiscoveryService;
import com.sda.mapper.opcua.OpcUaServerRegistry;
import com.sda.mapper.opcua.TankOpcUaServer;
import com.sda.mapper.opcua.TankProcessState;
import com.sda.mapper.opcua.PumpDegradationModel;
import com.sda.mapper.opcua.Ne107DiagnosticSimulator;
import com.sda.mapper.ui.catalog.DiscoveryCatalogView;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA Discovery Catalog View")
class DiscoveryCatalogViewTest {

    private DiscoveryCatalogView createView() {
        OpcUaClientFactory clientFactory = new OpcUaClientFactory();
        TankOpcUaServer tankServer = new TankOpcUaServer(
            new TankProcessState(),
            new PumpDegradationModel(),
            new Ne107DiagnosticSimulator());
        OpcUaServerRegistry registry = new OpcUaServerRegistry(clientFactory, tankServer);
        registry.init();
        OpcUaDiscoveryService discoveryService = new OpcUaDiscoveryService(clientFactory, registry);
        return new DiscoveryCatalogView(discoveryService, registry);
    }

    @Test
    @DisplayName("Should have Route annotation with catalog/discover path")
    void shouldHaveRouteAnnotation() {
        Route route = DiscoveryCatalogView.class.getAnnotation(Route.class);
        assertNotNull(route, "DiscoveryCatalogView must have @Route annotation");
        assertEquals("catalog/discover", route.value());
    }

    @Test
    @DisplayName("Should have correct @PageTitle")
    void shouldHaveCorrectPageTitle() {
        PageTitle pageTitle = DiscoveryCatalogView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "DiscoveryCatalogView must have @PageTitle annotation");
        assertEquals("OPC UA Discovery", pageTitle.value());
    }

    @Test
    @DisplayName("Should create DiscoveryCatalogView with all dependencies")
    void shouldCreateViewWithDependencies() {
        DiscoveryCatalogView view = createView();
        assertNotNull(view);
    }

    @Test
    @DisplayName("Should render with child components")
    void shouldRenderWithChildComponents() {
        DiscoveryCatalogView view = createView();
        assertNotNull(view);
        assertTrue(view.getChildren().count() > 0, "View should contain child components");
    }
}
