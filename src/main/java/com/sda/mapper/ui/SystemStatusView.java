package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * View showing system status and health information.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("System Status")
public class SystemStatusView extends VerticalLayout {

    public SystemStatusView() {
        add(new H2("System Status"));
        add(new Paragraph("Welcome to the CSS-SDA Mapper — Semantic Service-to-Deployment Mapping."));
        add(new Paragraph("Use the navigation drawer to explore services and system health."));
    }
}
