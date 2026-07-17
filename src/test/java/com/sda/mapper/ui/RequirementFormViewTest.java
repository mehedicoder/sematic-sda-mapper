package com.sda.mapper.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RequirementFormView UI Rendering")
class RequirementFormViewTest {

    @Test
    @DisplayName("Should define RequirementFormView extending VerticalLayout")
    void shouldExtendVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(RequirementFormView.class));
    }

    @Test
    @DisplayName("Should have @Route annotation with requirements path")
    void shouldHaveRouteAnnotation() {
        Route route = RequirementFormView.class.getAnnotation(Route.class);
        assertNotNull(route, "RequirementFormView must have @Route annotation");
        assertEquals("requirements", route.value(), "Route value should be 'requirements'");
    }

    @Test
    @DisplayName("Should have @PageTitle annotation with Requirement Form")
    void shouldHavePageTitleAnnotation() {
        PageTitle pageTitle = RequirementFormView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "RequirementFormView must have @PageTitle annotation");
        assertEquals("Requirement Form", pageTitle.value());
    }

    @Test
    @DisplayName("Should have results panel for match output")
    void shouldHaveResultsPanel() throws Exception {
        Field resultsPanel = RequirementFormView.class.getDeclaredField("resultsPanel");
        assertNotNull(resultsPanel, "Should have resultsPanel field");
    }

    @Test
    @DisplayName("Should have numeric input fields")
    void shouldHaveNumericInputFields() throws Exception {
        Field minRecallField = RequirementFormView.class.getDeclaredField("minRecallField");
        assertNotNull(minRecallField, "Should have minRecallField");
        minRecallField.setAccessible(true);

        Field maxLatencyField = RequirementFormView.class.getDeclaredField("maxLatencyField");
        assertNotNull(maxLatencyField, "Should have maxLatencyField");
        maxLatencyField.setAccessible(true);
    }

    @Test
    @DisplayName("Should have lastMatchedRequirementId field for lifecycle tracking")
    void shouldHaveLastMatchedRequirementIdField() throws Exception {
        Field field = RequirementFormView.class.getDeclaredField("lastMatchedRequirementId");
        assertNotNull(field, "Should have lastMatchedRequirementId field");
        assertEquals(String.class, field.getType(),
            "lastMatchedRequirementId should be of type String");
    }

    @Test
    @DisplayName("Should have verifyRequirement method for R-Verify integration")
    void shouldHaveVerifyRequirementMethod() throws Exception {
        var method = RequirementFormView.class.getDeclaredMethod("verifyRequirement");
        assertNotNull(method, "Should have verifyRequirement method");
    }
}
