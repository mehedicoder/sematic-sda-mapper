package com.sda.mapper.ui;

import com.sda.mapper.hub.ui.RequirementMatchingPanel;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.RequirementService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.ui.catalog.ServiceCatalogEntry;
import com.sda.mapper.ui.catalog.ServiceCatalogService;
import com.sda.mapper.ui.catalog.ServiceCatalogView;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Service Catalog View UI")
class ServiceCatalogViewTest {

    private ServiceCatalogView createView() {
        OntologyModelLoader ontologyModelLoader = new OntologyModelLoader();
        ontologyModelLoader.setFilePaths(List.of(
            "ontology/sda_ontology_v2_structured.ttl",
            "ontology/seed-services.ttl",
            "ontology/seed-requirements.ttl"));
        ontologyModelLoader.loadOntology();
        SparqlQueryLoader sparqlQueryLoader = new SparqlQueryLoader();
        ServiceCatalogService catalogService = new ServiceCatalogService(ontologyModelLoader, sparqlQueryLoader);
        return new ServiceCatalogView(catalogService,
            new RequirementMatchingPanel(org.mockito.Mockito.mock(RequirementService.class)));
    }

    @Test
    @DisplayName("Should be a VerticalLayout")
    void shouldBeVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(ServiceCatalogView.class));
    }

    @Test
    @DisplayName("Should have Route annotation with catalog path")
    void shouldHaveRouteAnnotation() {
        Route route = ServiceCatalogView.class.getAnnotation(Route.class);
        assertNotNull(route, "ServiceCatalogView must have @Route annotation");
        assertEquals("catalog", route.value());
    }

    @Test
    @DisplayName("Should have correct @PageTitle")
    void shouldHaveCorrectPageTitle() {
        PageTitle pageTitle = ServiceCatalogView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "ServiceCatalogView must have @PageTitle annotation");
        assertEquals("Service Catalog", pageTitle.value());
    }

    @Test
    @DisplayName("Should create ServiceCatalogView with all dependencies")
    void shouldCreateViewWithDependencies() {
        ServiceCatalogView view = createView();
        assertNotNull(view);
    }

    @Test
    @DisplayName("Should render with catalog entries from ontology")
    void shouldRenderWithCatalogEntries() {
        ServiceCatalogView view = createView();
        assertNotNull(view);
        assertTrue(view.getChildren().count() > 0, "View should contain child components");
    }

    @Test
    @DisplayName("Should embed the requirement matching panel consolidated from the Mapping Hub")
    void shouldEmbedRequirementMatchingPanel() {
        ServiceCatalogView view = createView();
        assertTrue(containsType(view, RequirementMatchingPanel.class),
            "Catalog must embed the consolidated requirement matching panel");
    }

    @Test
    @DisplayName("Should display all seeded services from the ontology seed files")
    void shouldDisplayAllSeededServices() {
        ServiceCatalogView view = createView();
        Collection<ServiceCatalogEntry> entries = getDisplayedEntries(view);
        List<String> names = entries.stream().map(ServiceCatalogEntry::serviceName).toList();
        assertEquals(5, entries.size(), "All 5 seeded services must be shown");
        assertTrue(names.contains("Fermentation Skid Service"));
        assertTrue(names.contains("Flowmeter Diagnostics Service"));
        assertTrue(names.contains("HVAC Temperature Analysis Service"));
        assertTrue(names.contains("Orphan Service"));
        assertTrue(names.contains("Process Variable Analytics Service"));
    }

    private boolean containsType(com.vaadin.flow.component.Component component, Class<?> type) {
        if (type.isInstance(component)) {
            return true;
        }
        return component.getChildren().anyMatch(child -> containsType(child, type));
    }

    private Grid<ServiceCatalogEntry> findGrid(com.vaadin.flow.component.Component component) {
        if (component instanceof Grid) {
            return (Grid<ServiceCatalogEntry>) component;
        }
        return component.getChildren()
            .map(this::findGrid)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private Collection<ServiceCatalogEntry> getDisplayedEntries(ServiceCatalogView view) {
        Grid<ServiceCatalogEntry> grid = findGrid(view);
        assertNotNull(grid, "Catalog view must contain a Grid");
        return ((ListDataProvider<ServiceCatalogEntry>) grid.getDataProvider()).getItems();
    }
}
