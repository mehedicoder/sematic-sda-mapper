package com.sda.mapper.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MainLayout UI Rendering")
class MainLayoutTest {

    @Test
    @DisplayName("Should define MainLayout extending AppLayout")
    void testMainLayoutExtendsAppLayout() {
        assertTrue(AppLayout.class.isAssignableFrom(MainLayout.class));
    }

    @Test
    @DisplayName("ThemeConfig should create a toggle button")
    void testThemeConfigCreatesButton() {
        Button toggle = ThemeConfig.createThemeToggleButton();
        assertNotNull(toggle);
        assertEquals("Toggle dark mode", toggle.getElement().getAttribute("aria-label"));
    }

    @Test
    @DisplayName("All view classes should be instantiable")
    void testAllViewClassesExist() {
        assertDoesNotThrow(() -> new SystemStatusView());
        assertDoesNotThrow(() -> new ServiceInstancesView());
        assertDoesNotThrow(() -> new OntologyExplorerView());
        assertDoesNotThrow(() -> new RequirementFormView());
        assertDoesNotThrow(() -> new RegistrationWizardView());
        assertDoesNotThrow(() -> new SearchView());
        assertDoesNotThrow(() -> new DashboardView());
        assertDoesNotThrow(() -> new DeploymentView());
        assertDoesNotThrow(() -> new MonitoringView());
        assertDoesNotThrow(() -> new SettingsView());
    }
}
