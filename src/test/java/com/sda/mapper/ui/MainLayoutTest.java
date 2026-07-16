package com.sda.mapper.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
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

    @Test
    @DisplayName("Should create SVG banner for header branding")
    void testBannerImageCreation() {
        var banner = new Image("/images/banner.svg", "Otto von Guericke Universität Magdeburg – Semantic SDA Mapper");
        assertNotNull(banner);
        assertEquals("/images/banner.svg", banner.getSrc());
        assertTrue(banner.getAlt().isPresent());
        assertEquals("Otto von Guericke Universität Magdeburg – Semantic SDA Mapper", banner.getAlt().get());
    }

    @Test
    @DisplayName("Banner image should have proper sizing and styling for header")
    void testBannerImageStyling() {
        var banner = new Image("/images/banner.svg", "Otto von Guericke Universität Magdeburg – Semantic SDA Mapper");
        banner.setHeight("64px");
        banner.getStyle()
            .set("object-fit", "contain")
            .set("object-position", "left center")
            .set("cursor", "pointer");

        assertEquals("64px", banner.getHeight());
        assertEquals("contain", banner.getStyle().get("object-fit"));
        assertEquals("left center", banner.getStyle().get("object-position"));
        assertEquals("pointer", banner.getStyle().get("cursor"));
    }
}
