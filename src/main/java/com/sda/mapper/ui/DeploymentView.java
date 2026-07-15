package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Deployment view showing generated Docker Compose and Kubernetes manifests.
 */
@Route(value = "deployment", layout = MainLayout.class)
@PageTitle("Deployment")
public class DeploymentView extends VerticalLayout {

    public DeploymentView() {
        add(new H2("Deployment"));
        add(new Paragraph("View and generate Docker Compose and Kubernetes deployment manifests."));
    }
}
