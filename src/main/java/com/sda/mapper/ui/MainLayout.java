package com.sda.mapper.ui;

import com.sda.mapper.ui.component.NotificationCenter;
import com.sda.mapper.ui.component.NotificationPanel;
import com.sda.mapper.ui.component.SearchBar;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.UI;
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

        var banner = new Image("/images/banner.svg", "Otto von Guericke Universität Magdeburg – SDA Platform");
        banner.setHeight("64px");
        banner.getStyle()
            .set("cursor", "pointer")
            .set("flex-grow", "1");
        banner.addClickListener(e -> UI.getCurrent().navigate(SystemStatusView.class));

        var themeToggle = ThemeConfig.createThemeToggleButton();

        var userIcon = new Icon(VaadinIcon.USER);
        var userLabel = new Span("admin");
        userLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        var userProfile = new HorizontalLayout(userIcon, userLabel);
        userProfile.setAlignItems(FlexComponent.Alignment.CENTER);
        userProfile.setSpacing(false);

        var searchBar = new SearchBar();
        searchBar.setWidth("350px");
        searchBar.addSearchListener(event ->
            UI.getCurrent().navigate(SearchView.class));

        var notificationCenter = new NotificationCenter();
        var notificationPanel = new NotificationPanel(notificationCenter);

        var rightControls = new HorizontalLayout(notificationPanel, themeToggle, userProfile);
        rightControls.setAlignItems(FlexComponent.Alignment.CENTER);
        rightControls.setSpacing(false);

        var toolbar = new HorizontalLayout(toggle, banner, searchBar, rightControls);
        toolbar.setWidthFull();
        toolbar.setHeight("64px");
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setSpacing(true);
        toolbar.setFlexGrow(1, banner);
        addToNavbar(toolbar);
    }

    private void createDrawerNavigation() {
        var logo = new Image("/images/industrie40_logo.png", "Plattform Industrie 4.0");
        logo.setHeight("28px");
        logo.addClassName("drawer-logo");
        logo.getStyle()
            .set("object-fit", "contain")
            .set("object-position", "left center");
        addToDrawer(logo);

        addToDrawer(createDrawerLink(VaadinIcon.BELL, "System Status", SystemStatusView.class));
        addToDrawer(createDrawerLink(VaadinIcon.CONNECT, "Service Instances", ServiceInstancesView.class));
        addToDrawer(createDrawerLink(VaadinIcon.CONNECT, "Upload", UploadView.class));
        addToDrawer(createDrawerLink(VaadinIcon.BOOK, "Ontology Explorer", OntologyExplorerView.class));
        addToDrawer(createDrawerLink(VaadinIcon.CLIPBOARD_TEXT, "Requirement Form", RequirementFormView.class));
        addToDrawer(createDrawerLink(VaadinIcon.PLUS_CIRCLE, "Registration Wizard", RegistrationWizardView.class));
        addToDrawer(createDrawerLink(VaadinIcon.SEARCH, "Search", SearchView.class));
        addToDrawer(createDrawerLink(VaadinIcon.CHART, "Dashboard", DashboardView.class));
        addToDrawer(createDrawerLink(VaadinIcon.ROCKET, "Deployment", DeploymentView.class));
        addToDrawer(createDrawerLink(VaadinIcon.EYE, "Monitoring", MonitoringView.class));
        addToDrawer(createDrawerLink(VaadinIcon.DASHBOARD, "OPC UA Monitor", OpcUaVisualizationView.class));
        addToDrawer(createDrawerLink(VaadinIcon.COG, "Settings", SettingsView.class));
    }

    private HorizontalLayout createDrawerLink(VaadinIcon iconType, String label, Class<? extends com.vaadin.flow.component.Component> viewClass) {
        var icon = new Icon(iconType);
        icon.addClassNames(LumoUtility.TextColor.SECONDARY);
        var link = new RouterLink(label, viewClass);
        link.addClassNames(LumoUtility.TextColor.SECONDARY);
        var layout = new HorizontalLayout(icon, link);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.setWidthFull();
        layout.addClassName("drawer-nav-link");
        return layout;
    }
}
