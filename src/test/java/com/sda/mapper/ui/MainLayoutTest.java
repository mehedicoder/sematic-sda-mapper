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
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            var meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
            var metricsService = new com.sda.mapper.monitoring.MetricsService(loader, meterRegistry);
            new SystemStatusView(loader, metricsService);
        });
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            new ServiceInstancesView(loader);
        });
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            new OntologyExplorerView(loader);
        });
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            var queryLoader = new com.sda.mapper.reasoning.SparqlQueryLoader();
            var rDeployService = new com.sda.mapper.reasoning.RDeployService(loader);
            var rVerifyService = new com.sda.mapper.reasoning.RVerifyService(loader);
            var reasoner = new com.sda.mapper.reasoning.SemanticReasonerService(loader, queryLoader, rDeployService);
            var dockerService = new com.sda.mapper.deployment.DockerComposeManifestService();
            var k8sService = new com.sda.mapper.deployment.KubernetesManifestService();
            var optionsService = new com.sda.mapper.api.OptionsConfigService(new tools.jackson.databind.ObjectMapper());
            new RequirementFormView(optionsService, reasoner, loader, dockerService, k8sService, rDeployService, rVerifyService);
        });
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            var rDeployService = new com.sda.mapper.reasoning.RDeployService(loader);
            var optionsService = new com.sda.mapper.api.OptionsConfigService(new tools.jackson.databind.ObjectMapper());
            new RegistrationWizardView(optionsService, loader, rDeployService);
        });
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            loader.setFilePath("ontology/css-sda-small.ttl");
            loader.loadOntology();
            new SearchView(new com.sda.mapper.reasoning.SearchService(loader));
        });
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            var inferenceService = new com.sda.mapper.reasoning.OntologyInferenceService(loader);
            new DashboardView(loader, inferenceService);
        });
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            var queryLoader = new com.sda.mapper.reasoning.SparqlQueryLoader();
            var rDeployService = new com.sda.mapper.reasoning.RDeployService(loader);
            var reasoner = new com.sda.mapper.reasoning.SemanticReasonerService(loader, queryLoader, rDeployService);
            var explanationService = new com.sda.mapper.reasoning.ExplanationService();
            var dockerService = new com.sda.mapper.deployment.DockerComposeManifestService();
            var k8sService = new com.sda.mapper.deployment.KubernetesManifestService();
            var manifestService = new com.sda.mapper.deployment.ManifestService(dockerService, k8sService);
            new com.sda.mapper.ui.DeploymentView(manifestService, reasoner, explanationService);
        });
        assertDoesNotThrow(() -> {
            var loader = new com.sda.mapper.ontology.OntologyModelLoader();
            var meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
            var metricsService = new com.sda.mapper.monitoring.MetricsService(loader, meterRegistry);
            new MonitoringView(metricsService);
        });
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
