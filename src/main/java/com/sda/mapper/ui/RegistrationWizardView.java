package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Registration wizard view for registering new CSS-SDA service instances.
 */
@Route(value = "register", layout = MainLayout.class)
@PageTitle("Registration Wizard")
public class RegistrationWizardView extends VerticalLayout {

    public RegistrationWizardView() {
        add(new H2("Registration Wizard"));
        add(new Paragraph("Register a new service through the multi-step wizard."));
    }
}
