package com.sda.mapper.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Main application layout with AppLayout rail navigation.
 * Follows the CSS-SDA hierarchy: Intent → Capability → Skill → Service → CommunicationBinding → DataConnector → Deployment.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.NAVBAR);
        setDrawerOpened(true);

        createHeader();
        createDrawerNavigation();
    }

    private void createHeader() {
        var toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Toggle navigation");

        var title = new H1("CSS-SDA Mapper");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.AUTO);

        var themeToggle = ThemeConfig.createThemeToggleButton();

        var userIcon = new Icon(VaadinIcon.USER);
        var userLabel = new Span("admin");
        userLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        var userProfile = new HorizontalLayout(userIcon, userLabel);
        userProfile.setAlignItems(FlexComponent.Alignment.CENTER);
        userProfile.setSpacing(false);
        userProfile.addClassNames(LumoUtility.Margin.End.MEDIUM);

        var headerBar = new HorizontalLayout(toggle, title, themeToggle, userProfile);
        headerBar.setWidthFull();
        headerBar.setAlignItems(FlexComponent.Alignment.CENTER);
        headerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        addToNavbar(headerBar);
    }

    private void createDrawerNavigation() {
        var subtitle = new H2("Semantic Mapping");
        subtitle.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        addToDrawer(subtitle);

        addToDrawer(new RouterLink("System Status", SystemStatusView.class));
        addToDrawer(new RouterLink("Service Instances", ServiceInstancesView.class));
        addToDrawer(new RouterLink("Ontology Explorer", OntologyExplorerView.class));
        addToDrawer(new RouterLink("Requirement Form", RequirementFormView.class));
        addToDrawer(new RouterLink("Registration Wizard", RegistrationWizardView.class));
        addToDrawer(new RouterLink("Search", SearchView.class));
        addToDrawer(new RouterLink("Dashboard", DashboardView.class));
        addToDrawer(new RouterLink("Deployment", DeploymentView.class));
        addToDrawer(new RouterLink("Monitoring", MonitoringView.class));
        addToDrawer(new RouterLink("Settings", SettingsView.class));
    }
}
