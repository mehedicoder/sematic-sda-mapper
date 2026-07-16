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
}
