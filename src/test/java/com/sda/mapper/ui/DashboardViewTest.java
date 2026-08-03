package com.sda.mapper.ui;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.OntologyInferenceService;
import com.sda.mapper.support.DemoAboxBuilder;
import com.sda.mapper.ui.component.DashboardCard;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DashboardView UI Rendering")
class DashboardViewTest {

    private static OntologyModelLoader loader;
    private static OntologyInferenceService inferenceService;

    @BeforeAll
    static void setUp() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-unified.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
        inferenceService = new OntologyInferenceService(loader);
        inferenceService.runInference();
    }

    @Test
    @DisplayName("Should define DashboardView extending VerticalLayout")
    void shouldExtendVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(DashboardView.class));
    }

    @Test
    @DisplayName("Should have @Route annotation with dashboard path")
    void shouldHaveRouteAnnotation() {
        Route route = DashboardView.class.getAnnotation(Route.class);
        assertNotNull(route, "DashboardView must have @Route annotation");
        assertEquals("dashboard", route.value(), "Route value should be 'dashboard'");
    }

    @Test
    @DisplayName("Should have @PageTitle annotation with Dashboard")
    void shouldHavePageTitleAnnotation() {
        PageTitle pageTitle = DashboardView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "DashboardView must have @PageTitle annotation");
        assertEquals("Dashboard", pageTitle.value());
    }

    @Test
    @DisplayName("Should declare DashboardCard fields for services, capabilities, skills, mappings, and inferred")
    void shouldDeclareDashboardCardFields() throws Exception {
        Field servicesField = DashboardView.class.getDeclaredField("servicesCard");
        assertEquals(DashboardCard.class, servicesField.getType());

        Field capabilitiesField = DashboardView.class.getDeclaredField("capabilitiesCard");
        assertEquals(DashboardCard.class, capabilitiesField.getType());

        Field skillsField = DashboardView.class.getDeclaredField("skillsCard");
        assertEquals(DashboardCard.class, skillsField.getType());

        Field mappingsField = DashboardView.class.getDeclaredField("mappingsCard");
        assertEquals(DashboardCard.class, mappingsField.getType());

        Field inferredField = DashboardView.class.getDeclaredField("inferredCard");
        assertEquals(DashboardCard.class, inferredField.getType());
    }
}
