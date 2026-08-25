package com.sda.mapper.ui;

import com.sda.mapper.ui.component.NotificationCenter;
import com.sda.mapper.ui.component.NotificationPanel;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Image;
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
        banner.setWidth("1200px");
        banner.getStyle()
            .set("cursor", "pointer")
            .set("object-fit", "contain");
        banner.addClickListener(e -> UI.getCurrent().navigate(OntologyView.class));

        var notificationCenter = new NotificationCenter();
        var notificationPanel = new NotificationPanel(notificationCenter);

        var rightControls = new HorizontalLayout(notificationPanel);
        rightControls.setAlignItems(FlexComponent.Alignment.CENTER);
        rightControls.setSpacing(false);

        var toolbar = new HorizontalLayout(toggle, banner, rightControls);
        toolbar.setWidthFull();
        toolbar.setHeight("64px");
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setSpacing(true);
        addToNavbar(toolbar);
    }

    private void createDrawerNavigation() {
        addToDrawer(createDrawerLink(VaadinIcon.SHARE, "Ontology Browser", OntologyView.class));
        addToDrawer(createDrawerLink(VaadinIcon.CLUSTER, "Mapping Hub", com.sda.mapper.hub.ui.MappingHubView.class));
        addToDrawer(createDrawerLink(VaadinIcon.GROUP, "Sessions", com.sda.mapper.hub.ui.SessionOverviewView.class));
        addToDrawer(createDrawerLink(VaadinIcon.BOOK, "Service Catalog", com.sda.mapper.ui.catalog.ServiceCatalogView.class));
        addToDrawer(createDrawerLink(VaadinIcon.CONNECT, "OPC UA Discovery", com.sda.mapper.ui.catalog.DiscoveryCatalogView.class));
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
