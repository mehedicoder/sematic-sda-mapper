package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Search view for semantic search across the ontology.
 */
@Route(value = "search", layout = MainLayout.class)
@PageTitle("Search")
public class SearchView extends VerticalLayout {

    public SearchView() {
        add(new H2("Search"));
        add(new Paragraph("Semantic search across requirements, capabilities, skills, and services."));
    }
}
