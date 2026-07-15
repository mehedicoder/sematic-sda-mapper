package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Requirement form view for creating and matching requirements.
 */
@Route(value = "requirements", layout = MainLayout.class)
@PageTitle("Requirement Form")
public class RequirementFormView extends VerticalLayout {

    public RequirementFormView() {
        add(new H2("Requirement Form"));
        add(new Paragraph("Create new requirements and match against available services."));
    }
}
