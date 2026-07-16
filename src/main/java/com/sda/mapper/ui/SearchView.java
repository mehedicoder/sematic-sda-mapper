package com.sda.mapper.ui;

import com.sda.mapper.reasoning.SearchResult;
import com.sda.mapper.reasoning.SearchService;
import com.sda.mapper.ui.component.SearchBar;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Search view for semantic search across the ontology.
 * Provides a text search field with entity type filtering and result grid.
 */
@Route(value = "search", layout = MainLayout.class)
@PageTitle("Search")
public class SearchView extends VerticalLayout {

    private final SearchService searchService;
    private final SearchBar searchBar;
    private final Grid<SearchResult> grid;
    private final Span resultCount;
    private List<SearchResult> currentItems;

    public SearchView(SearchService searchService) {
        this.searchService = searchService;
        setSizeFull();
        setPadding(false);

        var header = new H2("Search");
        header.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        searchBar = new SearchBar();
        searchBar.setWidthFull();
        searchBar.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        resultCount = new Span("0 results");
        resultCount.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        grid = new Grid<>(SearchResult.class, false);
        grid.addColumn(SearchResult::name).setHeader("Name").setFlexGrow(1);
        grid.addColumn(SearchResult::type).setHeader("Type").setWidth("180px");
        grid.addColumn(SearchResult::description).setHeader("Description").setFlexGrow(2);
        grid.addColumn(SearchResult::similarityScore)
                .setHeader("Score")
                .setWidth("100px")
                .setKey("score");
        grid.setWidthFull();
        grid.setHeightFull();

        currentItems = new ArrayList<>();
        grid.setItems(currentItems);

        add(header, searchBar, resultCount, grid);
        setFlexGrow(1, grid);

        searchBar.addSearchListener(event -> performSearch(event.getQuery(), event.getEntityType()));
    }

    private void performSearch(String query, String entityType) {
        List<SearchResult> results = searchService.search(query, entityType);
        currentItems = new ArrayList<>(results);
        grid.setItems(currentItems);
        resultCount.setText(results.size() + " result" + (results.size() == 1 ? "" : "s"));
    }
}
