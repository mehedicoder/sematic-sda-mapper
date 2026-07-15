package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * View showing registered service instances.
 */
@Route(value = "services", layout = MainLayout.class)
@PageTitle("Service Instances")
public class ServiceInstancesView extends VerticalLayout {

    public ServiceInstancesView() {
        add(new H2("Service Instances"));
        add(new Paragraph("Registered CSS-SDA service instances will appear here."));
    }
}
