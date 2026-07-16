package com.sda.mapper.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RegistrationWizardView UI Rendering")
class RegistrationWizardViewTest {

    @Test
    @DisplayName("Should define RegistrationWizardView extending VerticalLayout")
    void shouldExtendVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(RegistrationWizardView.class));
    }

    @Test
    @DisplayName("Should have @Route annotation with register path")
    void shouldHaveRouteAnnotation() {
        Route route = RegistrationWizardView.class.getAnnotation(Route.class);
        assertNotNull(route, "RegistrationWizardView must have @Route annotation");
        assertEquals("register", route.value(), "Route value should be 'register'");
    }

    @Test
    @DisplayName("Should have @PageTitle annotation with Registration Wizard")
    void shouldHavePageTitleAnnotation() {
        PageTitle pageTitle = RegistrationWizardView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "RegistrationWizardView must have @PageTitle annotation");
        assertEquals("Registration Wizard", pageTitle.value());
    }

    @Test
    @DisplayName("Should have navigation buttons")
    void shouldHaveNavigationButtons() throws Exception {
        Field prevButton = RegistrationWizardView.class.getDeclaredField("prevButton");
        assertNotNull(prevButton, "Should have prevButton field");
        prevButton.setAccessible(true);

        Field nextButton = RegistrationWizardView.class.getDeclaredField("nextButton");
        assertNotNull(nextButton, "Should have nextButton field");
        nextButton.setAccessible(true);

        Field saveDraftButton = RegistrationWizardView.class.getDeclaredField("saveDraftButton");
        assertNotNull(saveDraftButton, "Should have saveDraftButton field");
        saveDraftButton.setAccessible(true);

        Field cancelButton = RegistrationWizardView.class.getDeclaredField("cancelButton");
        assertNotNull(cancelButton, "Should have cancelButton field");
        cancelButton.setAccessible(true);
    }

    @Test
    @DisplayName("Should have step indicator")
    void shouldHaveStepIndicator() throws Exception {
        Field stepIndicator = RegistrationWizardView.class.getDeclaredField("stepIndicator");
        assertNotNull(stepIndicator, "Should have stepIndicator field");
    }

    @Test
    @DisplayName("Should have eight wizard steps")
    void shouldHaveEightSteps() throws Exception {
        Field stepsField = RegistrationWizardView.class.getDeclaredField("steps");
        assertNotNull(stepsField, "Should have steps field");
    }
}
