package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Monitoring view with real-time charts and system metrics.
 */
@Route(value = "monitoring", layout = MainLayout.class)
@PageTitle("Monitoring")
public class MonitoringView extends VerticalLayout {

    public MonitoringView() {
        add(new H2("Monitoring"));
        add(new Paragraph("Real-time system metrics and OPC UA session monitoring."));
    }
}
