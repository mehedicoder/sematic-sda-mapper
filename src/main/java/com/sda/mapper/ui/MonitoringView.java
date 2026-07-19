package com.sda.mapper.ui;

import com.sda.mapper.monitoring.MetricsService;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.Map;

@Route(value = "monitoring", layout = MainLayout.class)
@PageTitle("Monitoring")
public class MonitoringView extends VerticalLayout {

    private final MetricsService metricsService;

    private ProgressBar cpuBar;
    private ProgressBar memoryBar;
    private Span ontologyTriplesSpan;
    private Span ontologyClassesSpan;
    private Span matchRequestsSpan;
    private Span uptimeSpan;

    public MonitoringView(MetricsService metricsService) {
        this.metricsService = metricsService;
        buildLayout();
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Monitoring"));

        Map<String, Object> metrics = metricsService != null
            ? metricsService.getSystemMetrics()
            : Map.of();

        add(createSystemMetricsCard(metrics));
        add(createOntologyStatsCard(metrics));
        add(createMatchRequestsCard(metrics));
    }

    private VerticalLayout createSystemMetricsCard(Map<String, Object> metrics) {
        var card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.addClassName("monitoring-card");
        card.getStyle()
            .set("border-radius", "12px")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");

        var header = new HorizontalLayout(new Icon(VaadinIcon.CHART), new Span("System Metrics"));
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        // CPU
        var cpuLabel = new Span("CPU Usage");
        cpuLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        cpuBar = new ProgressBar(0.0, 1.0);
        cpuBar.setWidthFull();
        double cpuUsage = getDouble(metrics, "cpuUsage");
        cpuBar.setValue(cpuUsage);
        var cpuValue = new Span(String.format("%.1f%%", cpuUsage * 100));
        cpuValue.addClassNames(LumoUtility.FontSize.SMALL);

        // Memory
        var memLabel = new Span("Memory Usage");
        memLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        memoryBar = new ProgressBar(0.0, 1.0);
        memoryBar.setWidthFull();
        long memUsed = getLong(metrics, "memoryUsedMb");
        long memTotal = getLong(metrics, "memoryTotalMb");
        double memRatio = memTotal > 0 ? (double) memUsed / memTotal : 0.0;
        memoryBar.setValue(memRatio);
        var memValue = new Span(memUsed + " / " + memTotal + " MB");
        memValue.addClassNames(LumoUtility.FontSize.SMALL);

        // Uptime
        var uptimeLabel = new Span("Uptime");
        uptimeLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        uptimeSpan = new Span(getString(metrics, "uptime"));
        uptimeSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);

        card.add(header);
        card.add(cpuLabel, cpuBar, cpuValue);
        card.add(memLabel, memoryBar, memValue);
        card.add(uptimeLabel, uptimeSpan);
        return card;
    }

    private VerticalLayout createOntologyStatsCard(Map<String, Object> metrics) {
        var card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.addClassName("monitoring-card");
        card.getStyle()
            .set("border-radius", "12px")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");

        var header = new HorizontalLayout(new Icon(VaadinIcon.BOOK), new Span("Ontology Statistics"));
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        var triplesLabel = new Span("Triples");
        triplesLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        ontologyTriplesSpan = new Span(String.valueOf(getLong(metrics, "ontologyTripleCount")));
        ontologyTriplesSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        var classesLabel = new Span("Classes");
        classesLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
        ontologyClassesSpan = new Span(String.valueOf(getLong(metrics, "ontologyClassCount")));
        ontologyClassesSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        card.add(header);
        card.add(triplesLabel, ontologyTriplesSpan);
        card.add(classesLabel, ontologyClassesSpan);
        return card;
    }

    private VerticalLayout createMatchRequestsCard(Map<String, Object> metrics) {
        var card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.addClassName("monitoring-card");
        card.getStyle()
            .set("border-radius", "12px")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)");

        var header = new HorizontalLayout(new Icon(VaadinIcon.CLIPBOARD_TEXT), new Span("Match Requests"));
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        matchRequestsSpan = new Span(String.valueOf(getLong(metrics, "matchRequestCount")));
        matchRequestsSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        card.add(header);
        card.add(matchRequestsSpan);
        return card;
    }

    private static double getDouble(Map<String, Object> metrics, String key) {
        Object val = metrics.get(key);
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private static long getLong(Map<String, Object> metrics, String key) {
        Object val = metrics.get(key);
        if (val instanceof Number n) return n.longValue();
        return 0;
    }

    private static String getString(Map<String, Object> metrics, String key) {
        Object val = metrics.get(key);
        return val != null ? val.toString() : "N/A";
    }
}
