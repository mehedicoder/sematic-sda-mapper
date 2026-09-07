package com.sda.mapper.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OpcUaVisualizationView UI Rendering")
class OpcUaVisualizationViewTest {

    @Test
    @DisplayName("OpcUaVisualizationView should extend VerticalLayout")
    void shouldExtendVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(OpcUaVisualizationView.class));
    }

    @Test
    @DisplayName("Should have @Route annotation with opcua-visualization path")
    void shouldHaveRouteAnnotation() {
        Route route = OpcUaVisualizationView.class.getAnnotation(Route.class);
        assertNotNull(route, "OpcUaVisualizationView must have @Route annotation");
        assertEquals("opcua-visualization", route.value());
    }

    @Test
    @DisplayName("Should have @PageTitle annotation with OPC UA Monitor")
    void shouldHavePageTitleAnnotation() {
        PageTitle pageTitle = OpcUaVisualizationView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "OpcUaVisualizationView must have @PageTitle annotation");
        assertEquals("OPC UA Monitor", pageTitle.value());
    }

    @Test
    @DisplayName("Should use the dark theme matching the other hub views")
    void shouldUseDarkTheme() {
        OpcUaVisualizationView view = new OpcUaVisualizationView(requireService());
        assertTrue(view.getElement().getAttribute("theme").contains("dark"),
            "view theme must include the Lumo dark token");
        assertTrue(view.getElement().getClassList().contains("dark-view"),
            "view must carry the shared dark-view background class");
    }

    private com.sda.mapper.opcua.OpcUaVisualizationService requireService() {
        return org.mockito.Mockito.mock(com.sda.mapper.opcua.OpcUaVisualizationService.class);
    }
}
