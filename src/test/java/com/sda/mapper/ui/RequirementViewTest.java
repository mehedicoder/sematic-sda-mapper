package com.sda.mapper.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Requirement View UI")
class RequirementViewTest {

    private RequirementView createView() {
        return new RequirementView();
    }

    @Test
    @DisplayName("Should be a VerticalLayout")
    void shouldBeVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(RequirementView.class));
    }

    @Test
    @DisplayName("Should have Route annotation with requirement path")
    void shouldHaveRouteAnnotation() {
        Route route = RequirementView.class.getAnnotation(Route.class);
        assertNotNull(route, "RequirementView must have @Route annotation");
        assertEquals("requirement", route.value());
    }

    @Test
    @DisplayName("Should have correct @PageTitle")
    void shouldHaveCorrectPageTitle() {
        PageTitle pageTitle = RequirementView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "RequirementView must have @PageTitle annotation");
        assertEquals("Requirement", pageTitle.value());
    }

    @Test
    @DisplayName("Should create RequirementView and render components")
    void shouldCreateAndRender() {
        RequirementView view = createView();
        assertNotNull(view);
        assertTrue(view.getChildren().count() > 0, "View should contain child components");
    }
}
