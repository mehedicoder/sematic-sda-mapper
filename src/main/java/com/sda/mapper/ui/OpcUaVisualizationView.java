package com.sda.mapper.ui;

import com.sda.mapper.opcua.OpcUaServerRegistry;
import com.sda.mapper.opcua.OpcUaVisualizationService;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Route(value = "opcua-visualization", layout = MainLayout.class)
@PageTitle("OPC UA Monitor")
public class OpcUaVisualizationView extends VerticalLayout {

    private final OpcUaVisualizationService visualizationService;

    private Grid<OpcUaServerRegistry.ServerInfo> serverGrid;
    private VerticalLayout serverCardsLayout;
    private Timer refreshTimer;

    public OpcUaVisualizationView(OpcUaVisualizationService visualizationService) {
        this.visualizationService = visualizationService;
        buildLayout();
        refreshData();
        startPolling();
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("OPC UA Server Monitor"));
        add(new Text("Overview of all known OPC UA servers and their live variable values."));

        buildServerGrid();
        serverCardsLayout = new VerticalLayout();
        serverCardsLayout.setWidthFull();
        serverCardsLayout.setPadding(false);
        add(serverCardsLayout);
    }

    private void buildServerGrid() {
        serverGrid = new Grid<>();
        serverGrid.addColumn(OpcUaServerRegistry.ServerInfo::name).setHeader("Name");
        serverGrid.addColumn(OpcUaServerRegistry.ServerInfo::type).setHeader("Type");
        serverGrid.addColumn(s -> s.host() + ":" + s.port()).setHeader("Host:Port");
        serverGrid.addColumn(OpcUaServerRegistry.ServerInfo::path).setHeader("Path");
        serverGrid.addColumn(OpcUaServerRegistry.ServerInfo::connected)
            .setHeader("Status");
        serverGrid.setWidthFull();
        add(serverGrid);
        add(new Hr());
    }

    private void refreshData() {
        List<OpcUaServerRegistry.ServerInfo> servers = visualizationService.getAllServerSnapshots().stream()
            .map(s -> new OpcUaServerRegistry.ServerInfo(
                s.name(), "", 0, "", s.endpointUrl(),
                OpcUaServerRegistry.ServerType.valueOf(s.type()), s.connected()))
            .toList();

        serverGrid.setItems(servers);

        serverCardsLayout.removeAll();

        List<OpcUaVisualizationService.ServerSnapshot> snapshots = visualizationService.getAllServerSnapshots();
        for (OpcUaVisualizationService.ServerSnapshot snapshot : snapshots) {
            serverCardsLayout.add(createServerCard(snapshot));
        }
    }

    private VerticalLayout createServerCard(OpcUaVisualizationService.ServerSnapshot snapshot) {
        var card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle()
            .set("border-radius", "12px")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)")
            .set("margin-bottom", "12px");

        var header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        Span statusIcon = new Span(snapshot.connected() ? "+" : "x");
        statusIcon.getStyle()
            .set("font-weight", "bold")
            .set("color", snapshot.connected()
                ? "var(--lumo-success-color)"
                : "var(--lumo-error-color)")
            .set("font-size", "16px");
        header.add(statusIcon);

        header.add(new H3(snapshot.name()));
        header.setFlexGrow(1, new H3(snapshot.name()));

        Span statusBadge = new Span(snapshot.connected() ? "Connected" : "Disconnected");
        statusBadge.getStyle()
            .set("padding", "2px 10px")
            .set("border-radius", "4px")
            .set("font-size", "12px")
            .set("font-weight", "bold")
            .set("color", "white")
            .set("background", snapshot.connected()
                ? "var(--lumo-success-color)"
                : "var(--lumo-error-color)");
        header.add(statusBadge);

        Span typeBadge = new Span(snapshot.type());
        typeBadge.getStyle()
            .set("padding", "2px 8px")
            .set("border-radius", "4px")
            .set("font-size", "11px")
            .set("background", "var(--lumo-contrast-10pct-color)");
        header.add(typeBadge);

        card.add(header);

        Span endpoint = new Span(snapshot.endpointUrl());
        endpoint.getStyle().set("font-size", "12px").set("color", "var(--lumo-secondary-text-color)");
        card.add(endpoint);

        if (snapshot.nodeValues().isEmpty()) {
            card.add(new Text("No data available"));
        } else {
            var grid = new Grid<Map.Entry<String, OpcUaVisualizationService.NodeValue>>();
            grid.addColumn(e -> e.getValue().label()).setHeader("Node");
            grid.addColumn(e -> e.getValue().displayValue()).setHeader("Value");
            grid.addColumn(e -> e.getValue().unit()).setHeader("Unit");
            grid.setItems(snapshot.nodeValues().entrySet());
            grid.setWidthFull();
            grid.setHeight("300px");
            grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_COMPACT);
            card.add(grid);
        }

        return card;
    }

    private void startPolling() {
        refreshTimer = new Timer("OPC-UA-MultiServer-Poller", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    com.vaadin.flow.component.UI ui = getUI().orElse(null);
                    if (ui != null) {
                        ui.access(() -> refreshData());
                    }
                } catch (Exception ignored) {
                }
            }
        }, 3000, 5000);
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }
}
