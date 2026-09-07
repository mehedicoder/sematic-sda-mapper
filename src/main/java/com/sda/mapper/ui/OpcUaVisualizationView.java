package com.sda.mapper.ui;

import com.sda.mapper.opcua.OpcUaServerRegistry;
import com.sda.mapper.opcua.OpcUaVisualizationService;
import com.sda.mapper.opcua.OpcUaVisualizationService.BrowseTreeNode;
import com.sda.mapper.opcua.OpcUaVisualizationService.ServerSnapshot;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.hierarchy.AbstractHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

@Route(value = "opcua-visualization", layout = MainLayout.class)
@PageTitle("OPC UA Monitor")
@CssImport("./dark-view.css")
public class OpcUaVisualizationView extends VerticalLayout {

    private final OpcUaVisualizationService visualizationService;

    private VerticalLayout serverListLayout;
    private VerticalLayout browserLayout;
    private TreeGrid<BrowseTreeNode> treeGrid;
    private VerticalLayout detailPanel;
    private Span detailTitle;
    private VerticalLayout detailContent;
    private ServerSnapshot selectedSnapshot;
    private Timer refreshTimer;

    public OpcUaVisualizationView(OpcUaVisualizationService visualizationService) {
        this.visualizationService = visualizationService;
        buildLayout();
        refreshData();
        startPolling();
    }

    private void buildLayout() {
        getElement().setAttribute("theme", "dark");
        addClassName("dark-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("OPC UA Server Monitor"));
        add(new Text("Click a server to browse its address space hierarchy."));

        serverListLayout = new VerticalLayout();
        serverListLayout.setWidthFull();
        serverListLayout.setPadding(false);
        add(serverListLayout);

        buildBrowserLayout();
    }

    private void buildBrowserLayout() {
        browserLayout = new VerticalLayout();
        browserLayout.setWidthFull();
        browserLayout.setPadding(false);
        browserLayout.setSpacing(false);
        browserLayout.setVisible(false);

        var header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        header.setWidthFull();

        Button backButton = new Button("Back to Servers", e -> hideBrowser());
        backButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        header.add(backButton);

        detailTitle = new Span();
        detailTitle.getStyle().set("font-size", "18px").set("font-weight", "bold");
        header.add(detailTitle);
        header.setFlexGrow(1, detailTitle);

        browserLayout.add(header);
        browserLayout.add(new Hr());

        var content = new HorizontalLayout();
        content.setWidthFull();
        content.setHeightFull();
        content.setSpacing(true);

        treeGrid = new TreeGrid<>();
        treeGrid.setWidth("60%");
        treeGrid.setHeightFull();
        treeGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        treeGrid.addHierarchyColumn(this::nodeIconAndName).setHeader("Address Space");
        treeGrid.addColumn(BrowseTreeNode::nodeClass).setHeader("Type");
        treeGrid.addColumn(this::nodeValueColumn).setHeader("Value");
        treeGrid.addComponentColumn(this::createExpandIndicator)
            .setHeader("").setWidth("40px").setFlexGrow(0);

        detailPanel = new VerticalLayout();
        detailPanel.setWidth("40%");
        detailPanel.setPadding(true);
        detailPanel.setSpacing(false);
        detailPanel.getStyle()
            .set("border-left", "1px solid var(--lumo-contrast-20pct-color)")
            .set("padding-left", "16px");

        detailTitle = new Span("Select a node");
        detailTitle.getStyle().set("font-size", "16px").set("font-weight", "bold");
        detailPanel.add(detailTitle);

        detailContent = new VerticalLayout();
        detailContent.setPadding(false);
        detailContent.setSpacing(true);
        detailPanel.add(detailContent);

        treeGrid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(this::showNodeDetail);
        });

        content.add(treeGrid, detailPanel);
        content.setFlexGrow(3, treeGrid);
        content.setFlexGrow(2, detailPanel);
        browserLayout.add(content);

        add(browserLayout);
    }

    private String nodeIconAndName(BrowseTreeNode node) {
        return node.displayName();
    }

    private String nodeValueColumn(BrowseTreeNode node) {
        if (node.value() != null) {
            return String.valueOf(node.value());
        }
        return node.hasChildren() ? "" : "—";
    }

    private Icon createExpandIndicator(BrowseTreeNode node) {
        if (node.hasChildren()) {
            return new Icon(VaadinIcon.FOLDER_O);
        }
        if ("Variable".equals(node.nodeClass())) {
            return new Icon(VaadinIcon.EYE);
        }
        if ("Method".equals(node.nodeClass())) {
            return new Icon(VaadinIcon.COG);
        }
        return new Icon(VaadinIcon.FILE);
    }

    private void showNodeDetail(BrowseTreeNode node) {
        detailContent.removeAll();

        detailTitle.setText(node.displayName());

        addDetailRow("Node ID", node.nodeId());
        addDetailRow("Browse Name", node.browseName());
        addDetailRow("Display Name", node.displayName());
        addDetailRow("Node Class", node.nodeClass());

        if ("Variable".equals(node.nodeClass())) {
            addDetailRow("Value", node.value() != null ? String.valueOf(node.value()) : "—");
            addDetailRow("Data Type", node.dataType());
        }

        if (node.hasChildren()) {
            var childCount = new Span();
            childCount.getStyle().set("font-size", "12px")
                .set("color", "var(--lumo-secondary-text-color)");
            List<BrowseTreeNode> children = visualizationService.browseChildren(
                selectedSnapshot.endpointUrl(),
                parseNodeId(node.nodeId()));
            childCount.setText(children.size() + " child node(s)");
            detailContent.add(childCount);
        }
    }

    private void addDetailRow(String label, String value) {
        var row = new VerticalLayout();
        row.setPadding(false);
        row.setSpacing(false);

        var labelSpan = new Span(label);
        labelSpan.getStyle().set("font-size", "11px")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-weight", "bold");
        row.add(labelSpan);

        var valueSpan = new Span(value != null ? value : "—");
        valueSpan.getStyle().set("font-size", "13px");
        row.add(valueSpan);

        detailContent.add(row);
    }

    private NodeId parseNodeId(String nodeIdStr) {
        try {
            String withoutNs = nodeIdStr.substring(nodeIdStr.indexOf(';') + 1);
            int nsIndex = Integer.parseInt(nodeIdStr.substring(3, nodeIdStr.indexOf(';')));
            return new NodeId(nsIndex, withoutNs);
        } catch (Exception e) {
            return NodeId.parse(nodeIdStr);
        }
    }

    private void showBrowser(ServerSnapshot snapshot) {
        selectedSnapshot = snapshot;
        serverListLayout.setVisible(false);
        browserLayout.setVisible(true);

        detailTitle.setText(snapshot.name() + " — " + snapshot.endpointUrl());
        treeGrid.setDataProvider(createTreeProvider(snapshot.endpointUrl()));
    }

    private void hideBrowser() {
        selectedSnapshot = null;
        browserLayout.setVisible(false);
        serverListLayout.setVisible(true);
    }

    private AbstractHierarchicalDataProvider<BrowseTreeNode, Void> createTreeProvider(String endpointUrl) {
        return new AbstractHierarchicalDataProvider<>() {
            @Override
            public int getChildCount(HierarchicalQuery<BrowseTreeNode, Void> query) {
                if (query.getParent() == null) {
                    return visualizationService.browseAddressSpace(endpointUrl).size();
                }
                return query.getParent().hasChildren()
                    ? visualizationService.browseChildren(endpointUrl,
                        parseNodeId(query.getParent().nodeId())).size()
                    : 0;
            }

            @Override
            public boolean hasChildren(BrowseTreeNode node) {
                return node.hasChildren();
            }

            @Override
            public Stream<BrowseTreeNode> fetchChildren(HierarchicalQuery<BrowseTreeNode, Void> query) {
                List<BrowseTreeNode> children;
                if (query.getParent() == null) {
                    children = visualizationService.browseAddressSpace(endpointUrl);
                } else {
                    children = visualizationService.browseChildren(endpointUrl,
                        parseNodeId(query.getParent().nodeId()));
                }
                int startIndex = query.getOffset();
                int endIndex = Math.min(startIndex + query.getLimit(), children.size());
                return children.subList(startIndex, endIndex).stream();
            }

            @Override
            public boolean isInMemory() {
                return false;
            }
        };
    }

    private void refreshData() {
        List<ServerSnapshot> snapshots = visualizationService.getAllServerSnapshots();

        serverListLayout.removeAll();
        for (ServerSnapshot snapshot : snapshots) {
            serverListLayout.add(createServerCard(snapshot));
        }
    }

    private VerticalLayout createServerCard(ServerSnapshot snapshot) {
        var card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle()
            .set("border-radius", "12px")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)")
            .set("margin-bottom", "12px")
            .set("cursor", "pointer");

        card.addClickListener(e -> showBrowser(snapshot));

        var header = new HorizontalLayout();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        Span statusIcon = new Span(snapshot.connected() ? "\u2713" : "\u2717");
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
