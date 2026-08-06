package com.sda.mapper.ui;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.support.DemoAboxBuilder;
import com.sda.mapper.ui.catalog.ServiceCatalogService;
import com.sda.mapper.ui.catalog.ServiceCatalogView;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Service Catalog View UI")
class ServiceCatalogViewTest {

    private ServiceCatalogView createView() {
        OntologyModelLoader ontologyModelLoader = new OntologyModelLoader();
        ontologyModelLoader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        ontologyModelLoader.loadOntology();
        DemoAboxBuilder.apply(ontologyModelLoader.getModel());
        SparqlQueryLoader sparqlQueryLoader = new SparqlQueryLoader();
        ServiceCatalogService catalogService = new ServiceCatalogService(ontologyModelLoader, sparqlQueryLoader);
        return new ServiceCatalogView(catalogService);
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
}
