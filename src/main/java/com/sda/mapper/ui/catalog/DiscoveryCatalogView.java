package com.sda.mapper.ui.catalog;

import com.sda.mapper.opcua.OpcUaDiscoveryService;
import com.sda.mapper.opcua.OpcUaServerRegistry;
import com.sda.mapper.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * OPC UA Service Discovery view.
 * Connects to a live OPC UA server, browses its address space,
 * and displays discovered variable nodes as service catalog entries.
 */
@Route(value = "catalog/discover", layout = MainLayout.class)
@PageTitle("OPC UA Discovery")
public class DiscoveryCatalogView extends VerticalLayout {

    private final OpcUaDiscoveryService discoveryService;
    private final OpcUaServerRegistry serverRegistry;

    private final ComboBox<OpcUaServerRegistry.ServerInfo> serverCombo;
    private final TextField customUrlField;
    private final Button discoverButton;
    private final Grid<DiscoveredServiceEntry> grid;
    private final VerticalLayout detailPanel;
    private final Span statusLabel;
    private ListDataProvider<DiscoveredServiceEntry> dataProvider;

    public DiscoveryCatalogView(OpcUaDiscoveryService discoveryService,
                                 OpcUaServerRegistry serverRegistry) {
        this.discoveryService = discoveryService;
        this.serverRegistry = serverRegistry;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var header = new H2("OPC UA Service Discovery");
        header.addClassNames(LumoUtility.Margin.Bottom.NONE);

        var subtitle = new Paragraph("Discover services from live OPC UA servers");
        subtitle.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);

        // Server selector
        serverCombo = new ComboBox<>("Server");
        serverCombo.setItems(serverRegistry.getAllServers());
        serverCombo.setItemLabelGenerator(info -> info.name() + " (" + info.endpointUrl() + ")");
        serverCombo.setWidth("400px");
        serverCombo.setPlaceholder("Select a server...");

        // Custom URL field
        customUrlField = new TextField("Custom Endpoint URL");
        customUrlField.setPlaceholder("opc.tcp://host:port");
        customUrlField.setWidth("400px");
        customUrlField.setVisible(false);

        serverCombo.addValueChangeListener(e -> {
            customUrlField.setVisible(e.getValue() == null);
        });

        // Discover button
        discoverButton = new Button("Discover", new Icon(VaadinIcon.SEARCH), e -> doDiscover());
        discoverButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Status
        statusLabel = new Span("Select a server and click Discover");
        statusLabel.addClassNames(LumoUtility.TextColor.SECONDARY);

        var controlBar = new HorizontalLayout(serverCombo, customUrlField, discoverButton, statusLabel);
        controlBar.setAlignItems(FlexComponent.Alignment.END);
        controlBar.setSpacing(true);
        controlBar.addClassName(LumoUtility.Margin.Bottom.SMALL);

        // Grid
        dataProvider = new ListDataProvider<>(new ArrayList<>());
        grid = createGrid(dataProvider);

        // Detail panel
        detailPanel = new VerticalLayout();
        detailPanel.setWidth("380px");
        detailPanel.setPadding(false);
        detailPanel.setSpacing(false);
        detailPanel.add(new Span("Select a node to view details"));

        // Split layout
        var scroller = new Scroller(grid);
        scroller.setWidthFull();
        scroller.setHeightFull();

        var splitLayout = new com.vaadin.flow.component.splitlayout.SplitLayout(scroller, detailPanel);
        splitLayout.setSplitterPosition(70);
        splitLayout.setWidthFull();
        splitLayout.setHeightFull();

        add(header, subtitle, controlBar, splitLayout);
        setFlexGrow(1, splitLayout);
    }

    private void doDiscover() {
        String endpointUrl = resolveEndpointUrl();
        if (endpointUrl == null) {
            Notification.show("Please select a server or enter a custom URL",
                3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        discoverButton.setEnabled(false);
        statusLabel.setText("Discovering...");
        statusLabel.removeClassName(LumoUtility.TextColor.SECONDARY);
        statusLabel.addClassName(LumoUtility.TextColor.PRIMARY);

        try {
            List<DiscoveredServiceEntry> entries = discoveryService.discover(endpointUrl);
            dataProvider = new ListDataProvider<>(entries);
            grid.setDataProvider(dataProvider);

            statusLabel.setText(entries.size() + " nodes discovered from " + endpointUrl);
            statusLabel.removeClassName(LumoUtility.TextColor.PRIMARY);
            statusLabel.addClassName(LumoUtility.TextColor.SUCCESS);

            if (entries.isEmpty()) {
                Notification.show("No variable nodes found at " + endpointUrl,
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
            }
        } catch (Exception ex) {
            statusLabel.setText("Discovery failed: " + ex.getMessage());
            statusLabel.removeClassName(LumoUtility.TextColor.PRIMARY);
            statusLabel.addClassName(LumoUtility.TextColor.ERROR);
            Notification.show("Discovery failed: " + ex.getMessage(),
                5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            discoverButton.setEnabled(true);
        }
    }

    private String resolveEndpointUrl() {
        OpcUaServerRegistry.ServerInfo selected = serverCombo.getValue();
        if (selected != null) {
            return selected.endpointUrl();
        }
        String custom = customUrlField.getValue();
        if (custom != null && !custom.isBlank()) {
            return custom.trim();
        }
        return null;
    }

    private Grid<DiscoveredServiceEntry> createGrid(ListDataProvider<DiscoveredServiceEntry> dataProvider) {
        var g = new Grid<>(DiscoveredServiceEntry.class, false);
        g.setDataProvider(dataProvider);
        g.setWidthFull();
        g.setHeightFull();
        g.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        g.setSelectionMode(Grid.SelectionMode.SINGLE);
        g.getElement().setAttribute("theme", "column-borders");

        g.addColumn(DiscoveredServiceEntry::serviceName)
            .setHeader("Service")
            .setFlexGrow(2)
            .setWidth("200px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(DiscoveredServiceEntry::capabilityName)
            .setHeader("Capability")
            .setFlexGrow(1)
            .setWidth("160px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(DiscoveredServiceEntry::skillName)
            .setHeader("Skill")
            .setFlexGrow(1)
            .setWidth("160px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(DiscoveredServiceEntry::protocol)
            .setHeader("Protocol")
            .setWidth("120px")
            .setResizable(true);

        g.addColumn(DiscoveredServiceEntry::accessMode)
            .setHeader("Access Mode")
            .setWidth("120px")
            .setResizable(true);

        g.addColumn(DiscoveredServiceEntry::currentNodeValue)
            .setHeader("Value")
            .setWidth("120px")
            .setResizable(true);

        g.addColumn(DiscoveredServiceEntry::dataType)
            .setHeader("Data Type")
            .setWidth("100px")
            .setResizable(true);

        g.addColumn(DiscoveredServiceEntry::nodeId)
            .setHeader("Node ID")
            .setWidth("160px")
            .setResizable(true);

        g.addColumn(DiscoveredServiceEntry::endpointUrl)
            .setHeader("Endpoint")
            .setFlexGrow(1)
            .setWidth("220px")
            .setResizable(true);

        g.addSelectionListener(e -> {
            var selected = e.getFirstSelectedItem();
            selected.ifPresentOrElse(
                this::showNodeDetail,
                () -> detailPanel.removeAll()
            );
        });

        return g;
    }

    private void showNodeDetail(DiscoveredServiceEntry entry) {
        detailPanel.removeAll();

        var title = new H3(entry.skillName());
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);

        detailPanel.add(title);
        detailPanel.add(createDetailSection("Service", entry.serviceName()));
        detailPanel.add(createDetailSection("Capability", entry.capabilityName()));
        detailPanel.add(createDetailSection("Source", entry.source()));

        var commHeader = new H3("Communication");
        commHeader.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.MEDIUM);
        detailPanel.add(commHeader);
        detailPanel.add(createDetailSection("Protocol", entry.protocol()));
        detailPanel.add(createDetailSection("Access Mode", entry.accessMode()));

        var nodeHeader = new H3("Node Details");
        nodeHeader.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.MEDIUM);
        detailPanel.add(nodeHeader);
        detailPanel.add(createDetailSection("Node ID", entry.nodeId()));
        detailPanel.add(createDetailSection("Data Type", entry.dataType()));
        detailPanel.add(createDetailSection("Current Value", entry.currentNodeValue()));

        var endpointHeader = new H3("Endpoint");
        endpointHeader.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.MEDIUM);
        detailPanel.add(endpointHeader);
        detailPanel.add(createDetailSection("URL", entry.endpointUrl()));
    }

    private VerticalLayout createDetailSection(String label, String value) {
        var labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        var valueSpan = new Span(value != null ? value : "\u2014");
        valueSpan.addClassNames(LumoUtility.FontSize.SMALL);

        var layout = new VerticalLayout(labelSpan, valueSpan);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        return layout;
    }
}
