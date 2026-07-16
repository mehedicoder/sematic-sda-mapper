package com.sda.mapper.ui;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.SearchService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchView UI Rendering")
class SearchViewTest {

    @Test
    @DisplayName("Should define SearchView extending VerticalLayout")
    void shouldExtendVerticalLayout() {
        assertTrue(VerticalLayout.class.isAssignableFrom(SearchView.class));
    }

    @Test
    @DisplayName("Should have @Route annotation with search path")
    void shouldHaveRouteAnnotation() {
        Route route = SearchView.class.getAnnotation(Route.class);
        assertNotNull(route, "SearchView must have @Route annotation");
        assertEquals("search", route.value(), "Route value should be 'search'");
    }

    @Test
    @DisplayName("Should have @PageTitle annotation with Search")
    void shouldHavePageTitleAnnotation() {
        PageTitle pageTitle = SearchView.class.getAnnotation(PageTitle.class);
        assertNotNull(pageTitle, "SearchView must have @PageTitle annotation");
        assertEquals("Search", pageTitle.value());
    }

    @Test
    @DisplayName("Should create SearchView with Grid component")
    void shouldHaveGridComponent() {
        var loader = new OntologyModelLoader();
        loader.setFilePath("ontology/css-sda-small.ttl");
        loader.loadOntology();
        var searchService = new SearchService(loader);
        var view = new SearchView(searchService);
        assertNotNull(view);

        var grid = findGrid(view);
        assertNotNull(grid, "SearchView should contain a Grid component");
        assertNotNull(grid.getColumns(), "Grid should have columns defined");
        assertFalse(grid.getColumns().isEmpty(), "Grid should have at least one column");
    }

    private static Grid<?> findGrid(com.vaadin.flow.component.Component component) {
        if (component instanceof Grid<?> grid) {
            return grid;
        }
        if (component instanceof VerticalLayout layout) {
            for (var child : layout.getChildren().toList()) {
                var found = findGrid(child);
                if (found != null) return found;
            }
        }
        return null;
    }
}
