package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Ontology explorer view for browsing CSS-SDA class hierarchy.
 */
@Route(value = "ontology", layout = MainLayout.class)
@PageTitle("Ontology Explorer")
public class OntologyExplorerView extends VerticalLayout {

    public OntologyExplorerView() {
        add(new H2("Ontology Explorer"));
        add(new Paragraph("Browse and explore the CSS-SDA ontology model."));
    }
}
