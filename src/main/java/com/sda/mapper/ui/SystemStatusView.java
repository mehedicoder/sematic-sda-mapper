package com.sda.mapper.ui;

import com.sda.mapper.monitoring.MetricsService;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.ui.component.DashboardCard;
import com.vaadin.flow.component.html.H2;
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

import java.util.Map;

/**
 * Landing page showing system health overview and quick navigation.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("System Status")
public class SystemStatusView extends VerticalLayout {

    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";

    public SystemStatusView(OntologyModelLoader ontologyModelLoader,
                            MetricsService metricsService) {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var header = new H2("System Status");
        header.addClassNames(LumoUtility.Margin.Bottom.SMALL);
        add(header);

        var welcome = new Paragraph("Welcome to the CSS-SDA Mapper — Semantic Service-to-Deployment Mapping.");
        welcome.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.SMALL);
        add(welcome);

        // Health overview cards
        Map<String, Object> metrics = metricsService.getSystemMetrics();

        var servicesCard = new DashboardCard(
            VaadinIcon.CONNECT, "Services",
            getMetricInt(metrics, "ontologyServiceCount"),
            "var(--lumo-primary-color)");

        var triplesCard = new DashboardCard(
            VaadinIcon.BOOK, "Ontology Triples",
            getMetricInt(metrics, "ontologyTripleCount"),
            "var(--lumo-success-color)");

        var classesCard = new DashboardCard(
            VaadinIcon.CUBE, "Classes",
            getMetricInt(metrics, "ontologyClassCount"),
            "var(--lumo-contrast-color)");

        var matchCard = new DashboardCard(
            VaadinIcon.CLIPBOARD_TEXT, "Match Requests",
            getMetricInt(metrics, "matchRequestCount"),
            "var(--lumo-warning-color)");

        var uptimeCard = new DashboardCard(
            VaadinIcon.CLOCK, "Uptime",
            0, "var(--lumo-error-color)");
        uptimeCard.updateValue(0);

        var cardsRow = new HorizontalLayout(
            servicesCard, triplesCard, classesCard, matchCard);
        cardsRow.setWidthFull();
        cardsRow.setSpacing(true);
        cardsRow.setAlignItems(FlexComponent.Alignment.STRETCH);
        add(cardsRow);

        add(new Hr());

        // Status details
        var statusSection = new VerticalLayout();
        statusSection.setPadding(false);
        statusSection.setSpacing(true);

        var statusHeader = new Span("Health Details");
        statusHeader.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        statusSection.add(statusHeader);

        addStatusRow(statusSection, "Ontology",
            ontologyModelLoader.isLoaded() ? "Loaded" : "Not loaded",
            ontologyModelLoader.isLoaded());
        addStatusRow(statusSection, "Triples",
            String.valueOf(getMetricInt(metrics, "ontologyTripleCount")), true);
        addStatusRow(statusSection, "Classes",
            String.valueOf(getMetricInt(metrics, "ontologyClassCount")), true);
        addStatusRow(statusSection, "CPU",
            String.format("%.1f%%", getDouble(metrics, "cpuUsage") * 100), true);
        addStatusRow(statusSection, "Memory",
            getMetricLong(metrics, "memoryUsedMb") + " / " + getMetricLong(metrics, "memoryTotalMb") + " MB",
            true);
        addStatusRow(statusSection, "Uptime",
            getString(metrics, "uptime"), true);

        add(statusSection);
        add(new Hr());

        // Quick links
        var linksHeader = new Span("Quick Actions");
        linksHeader.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        add(linksHeader);

        var linksRow = new HorizontalLayout();
        linksRow.setSpacing(true);
        linksRow.setPadding(false);

        linksRow.add(createQuickLink(VaadinIcon.CONNECT, "Service Instances", ServiceInstancesView.class));
        linksRow.add(createQuickLink(VaadinIcon.CLIPBOARD_TEXT, "Requirement Form", RequirementFormView.class));
        linksRow.add(createQuickLink(VaadinIcon.BOOK, "Ontology Explorer", OntologyExplorerView.class));
        linksRow.add(createQuickLink(VaadinIcon.SEARCH, "Search", SearchView.class));
        linksRow.add(createQuickLink(VaadinIcon.ROCKET, "Deployment", DeploymentView.class));
        linksRow.add(createQuickLink(VaadinIcon.EYE, "Monitoring", MonitoringView.class));

        add(linksRow);
    }

    private void addStatusRow(VerticalLayout parent, String label, String value, boolean ok) {
        var icon = new Icon(ok ? VaadinIcon.CHECK_CIRCLE : VaadinIcon.WARNING);
        icon.setColor(ok ? "var(--lumo-success-color)" : "var(--lumo-error-color)");
        icon.setSize("16px");

        var labelText = new Span(label + ": ");
        labelText.addClassNames(LumoUtility.TextColor.SECONDARY);

        var valueText = new Span(value);
        valueText.addClassNames(LumoUtility.FontWeight.BOLD);

        var row = new HorizontalLayout(icon, labelText, valueText);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setPadding(false);
        parent.add(row);
    }

    private HorizontalLayout createQuickLink(VaadinIcon iconType, String label,
                                              Class<? extends com.vaadin.flow.component.Component> viewClass) {
        var icon = new Icon(iconType);
        icon.setSize("20px");
        var link = new com.vaadin.flow.component.html.Anchor("/" + getRoutePath(viewClass), label);
        link.addClassNames(LumoUtility.TextColor.SECONDARY);

        var layout = new HorizontalLayout(icon, link);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.setPadding(false);
        return layout;
    }

    private static String getRoutePath(Class<?> viewClass) {
        var route = viewClass.getAnnotation(com.vaadin.flow.router.Route.class);
        return route != null ? route.value() : "";
    }

    private static int getMetricInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    private static long getMetricLong(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.longValue() : 0;
    }

    private static double getDouble(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "N/A";
    }
}
