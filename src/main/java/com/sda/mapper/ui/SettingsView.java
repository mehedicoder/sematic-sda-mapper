package com.sda.mapper.ui;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Settings view for user preferences and application configuration.
 */
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
public class SettingsView extends VerticalLayout {

    public SettingsView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Settings"));

        // User profile section
        var userProfileHeader = new H3("User Profile");
        var userIcon = new Icon(VaadinIcon.USER);
        userIcon.getStyle().set("width", "24px").set("height", "24px");
        var userLabel = new Span("admin");
        userLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
        var userRow = new HorizontalLayout(userIcon, userLabel);
        userRow.setAlignItems(FlexComponent.Alignment.CENTER);
        userRow.setSpacing(true);

        add(userProfileHeader, userRow);

        add(new Hr());

        // Theme section
        var themeHeader = new H3("Appearance");
        var themeDescription = new Paragraph("Toggle between light and dark mode.");
        var themeToggle = ThemeConfig.createThemeToggleButton();
        var themeRow = new HorizontalLayout(themeToggle, new Span("Dark Mode"));
        themeRow.setAlignItems(FlexComponent.Alignment.CENTER);
        themeRow.setSpacing(true);

        add(themeHeader, themeDescription, themeRow);

        add(new Hr());

        // General section
        var generalHeader = new H3("General");
        add(generalHeader, new Paragraph("Manage language, notification, and search preferences."));
    }
}
