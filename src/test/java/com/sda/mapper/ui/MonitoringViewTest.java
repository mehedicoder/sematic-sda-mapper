package com.sda.mapper.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MonitoringView UI Rendering")
class MonitoringViewTest {

    @Test
    @DisplayName("MonitoringView should extend VerticalLayout")
    void shouldExtendVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(MonitoringView.class));
    }

    @Test
    @DisplayName("Should have @Route annotation with monitoring path")
    void shouldHaveRouteAnnotation() {
        Route route = MonitoringView.class.getAnnotation(Route.class);
        assertNotNull(route, "MonitoringView must have @Route annotation");
        assertEquals("monitoring", route.value());
    }

    @Test
    @DisplayName("Should have @PageTitle annotation with Monitoring")
    void shouldHavePageTitleAnnotation() {
        PageTitle pageTitle = MonitoringView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "MonitoringView must have @PageTitle annotation");
        assertEquals("Monitoring", pageTitle.value());
    }
}
