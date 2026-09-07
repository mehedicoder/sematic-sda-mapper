package com.sda.mapper.ui.catalog;

import com.sda.mapper.hub.ui.RequirementMatchingPanel;
import com.sda.mapper.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

/**
 * Service Catalog view for browsing all pre-built CSS-SDA services from the ontology.
 * Displays services in a filterable Grid with protocol, recall, latency,
 * deployment details, and a card-based detail panel for the full CSS-SDA chain.
 */
@Route(value = "catalog", layout = MainLayout.class)
@PageTitle("Service Catalog")
@CssImport("./dark-view.css")
public class ServiceCatalogView extends VerticalLayout {

    private final ServiceCatalogService catalogService;
    private final RequirementMatchingPanel requirementMatchingPanel;
    private final Grid<ServiceCatalogEntry> grid;
    private final VerticalLayout detailPanel;
    private final ListDataProvider<ServiceCatalogEntry> dataProvider;
    private final ComboBox<String> protocolFilter;
    private final ComboBox<String> deploymentZoneFilter;
    private final TextField searchField;

    public ServiceCatalogView(ServiceCatalogService catalogService,
                              RequirementMatchingPanel requirementMatchingPanel) {
        this.catalogService = catalogService;
        this.requirementMatchingPanel = requirementMatchingPanel;
        getElement().setAttribute("theme", "dark");
        addClassName("dark-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Initialize filter components first (final fields)
        searchField = new TextField();
        protocolFilter = new ComboBox<>("Protocol");
        deploymentZoneFilter = new ComboBox<>("Deployment Zone");

        var header = new H2("Service Catalog");
        header.addClassNames(LumoUtility.Margin.Bottom.NONE);

        var subtitle = new Paragraph("Browse pre-built CSS-SDA services from the ontology catalog");
        subtitle.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);

        var headerLayout = new VerticalLayout(header, subtitle);
        headerLayout.setPadding(false);
        headerLayout.setSpacing(false);

        List<ServiceCatalogEntry> entries = catalogService.loadCatalog();
        dataProvider = new ListDataProvider<>(entries);

        // Configure filters and wire listeners
        configureFilters();

        // Grid
        grid = createGrid(dataProvider);

        // Detail panel
        detailPanel = new VerticalLayout();
        detailPanel.setWidth("380px");
        detailPanel.setPadding(false);
        detailPanel.setSpacing(false);
        detailPanel.add(new Span("Select a service to view details"));

        // Split layout: grid left, detail right
        var scroller = new Scroller(grid);
        scroller.setWidthFull();
        scroller.setHeightFull();

        var splitLayout = new com.vaadin.flow.component.splitlayout.SplitLayout(scroller, detailPanel);
        splitLayout.setSplitterPosition(70);
        splitLayout.setWidthFull();
        splitLayout.setHeightFull();

        add(headerLayout, createFilterBar(), splitLayout);
        setFlexGrow(1, splitLayout);

        var matchDetails = new Details(new Span("Match a Requirement against this Catalog"),
            requirementMatchingPanel);
        matchDetails.setWidthFull();
        matchDetails.addClassName("catalog-requirement-details");
        add(matchDetails);
    }

    private void configureFilters() {
        searchField.setPlaceholder("Search services...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.setWidth("280px");
        searchField.addClassName(LumoUtility.Margin.Right.SMALL);
        searchField.getElement().setAttribute("aria-label", "Search services");
        searchField.addValueChangeListener(e -> applyFilters());

        protocolFilter.setItems("OPC-UA", "PROFINET", "BACnet", "HTTP");
        protocolFilter.setClearButtonVisible(true);
        protocolFilter.setPlaceholder("All protocols");
        protocolFilter.setWidth("160px");
        protocolFilter.addClassName(LumoUtility.Margin.Right.SMALL);
        protocolFilter.getElement().setAttribute("aria-label", "Filter by protocol");
        protocolFilter.addValueChangeListener(e -> applyFilters());

        deploymentZoneFilter.setItems("OT-Zone-1", "OT-Zone-3");
        deploymentZoneFilter.setClearButtonVisible(true);
        deploymentZoneFilter.setPlaceholder("All zones");
        deploymentZoneFilter.setWidth("160px");
        deploymentZoneFilter.getElement().setAttribute("aria-label", "Filter by deployment zone");
        deploymentZoneFilter.addValueChangeListener(e -> applyFilters());
    }

    private HorizontalLayout createFilterBar() {
        var resetButton = new Button(new Icon(VaadinIcon.REFRESH), e -> resetFilters());
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.getElement().setAttribute("aria-label", "Reset filters");

        var filterBar = new HorizontalLayout(searchField, protocolFilter, deploymentZoneFilter, resetButton);
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setSpacing(true);
        filterBar.addClassName(LumoUtility.Margin.Bottom.SMALL);
        return filterBar;
    }

    private Grid<ServiceCatalogEntry> createGrid(ListDataProvider<ServiceCatalogEntry> dataProvider) {
        var g = new Grid<>(ServiceCatalogEntry.class, false);
        g.setDataProvider(dataProvider);
        g.setWidthFull();
        g.setHeightFull();
        g.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        g.setSelectionMode(Grid.SelectionMode.SINGLE);
        g.getElement().setAttribute("theme", "column-borders");

        g.addColumn(ServiceCatalogEntry::serviceName)
            .setHeader("Service")
            .setKey("serviceName")
            .setFlexGrow(2)
            .setWidth("200px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(ServiceCatalogEntry::capabilityName)
            .setHeader("Capability")
            .setKey("capabilityName")
            .setFlexGrow(1)
            .setWidth("160px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(ServiceCatalogEntry::skillName)
            .setHeader("Skill")
            .setKey("skillName")
            .setFlexGrow(1)
            .setWidth("160px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(ServiceCatalogEntry::protocol)
            .setHeader("Protocol")
            .setKey("protocol")
            .setWidth("120px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(ServiceCatalogEntry::accessMode)
            .setHeader("Access Mode")
            .setKey("accessMode")
            .setWidth("120px")
            .setResizable(true);

        g.addColumn(ServiceCatalogEntry::securityMode)
            .setHeader("Security")
            .setKey("securityMode")
            .setWidth("130px")
            .setResizable(true);

        g.addColumn(ServiceCatalogEntry::recallFormatted)
            .setHeader("Recall")
            .setKey("recall")
            .setWidth("90px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(ServiceCatalogEntry::latencyFormatted)
            .setHeader("Latency")
            .setKey("latency")
            .setWidth("90px")
            .setResizable(true)
            .setSortable(true);

        g.addColumn(ServiceCatalogEntry::deploymentName)
            .setHeader("Deployment")
            .setKey("deploymentName")
            .setFlexGrow(1)
            .setWidth("160px")
            .setResizable(true);

        g.addColumn(ServiceCatalogEntry::networkZone)
            .setHeader("Zone")
            .setKey("networkZone")
            .setWidth("110px")
            .setResizable(true);

        g.addColumn(ServiceCatalogEntry::containerImage)
            .setHeader("Container Image")
            .setKey("containerImage")
            .setFlexGrow(2)
            .setWidth("220px")
            .setResizable(true);

        g.addSelectionListener(e -> {
            var selected = e.getFirstSelectedItem();
            selected.ifPresentOrElse(
                this::showServiceDetail,
                () -> detailPanel.removeAll()
            );
        });

        return g;
    }

    private void applyFilters() {
        String search = searchField.getValue();
        String protocol = protocolFilter.getValue();
        String zone = deploymentZoneFilter.getValue();

        dataProvider.setFilter(entry -> {
            boolean matchesSearch = search == null || search.isBlank()
                || containsIgnoreCase(entry.serviceName(), search)
                || containsIgnoreCase(entry.capabilityName(), search)
                || containsIgnoreCase(entry.skillName(), search)
                || containsIgnoreCase(entry.containerImage(), search);

            boolean matchesProtocol = protocol == null || protocol.isBlank()
                || protocol.equals(entry.protocol());

            boolean matchesZone = zone == null || zone.isBlank()
                || zone.equals(entry.networkZone());

            return matchesSearch && matchesProtocol && matchesZone;
        });
    }

    private void resetFilters() {
        searchField.clear();
        protocolFilter.clear();
        deploymentZoneFilter.clear();
        dataProvider.clearFilters();
    }

    private void showServiceDetail(ServiceCatalogEntry entry) {
        detailPanel.removeAll();

        var title = new H3(entry.serviceName());
        title.addClassNames(LumoUtility.Margin.Bottom.NONE);

        detailPanel.add(title);
        detailPanel.add(createDetailSection("Capability", entry.capabilityName()));
        detailPanel.add(createDetailSection("Skill", entry.skillName()));

        var bindingHeader = new H3("Communication Binding");
        bindingHeader.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.MEDIUM);
        detailPanel.add(bindingHeader);
        detailPanel.add(createDetailSection("Protocol", entry.protocol()));
        detailPanel.add(createDetailSection("Access Mode", entry.accessMode()));
        detailPanel.add(createDetailSection("Security Mode", entry.securityMode()));

        var connectorHeader = new H3("Data Connector");
        connectorHeader.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.MEDIUM);
        detailPanel.add(connectorHeader);
        detailPanel.add(createDetailSection("Connector", entry.connectorName()));
        detailPanel.add(createDetailSection("Information Model", entry.informationModel()));

        var deployHeader = new H3("Deployment Context");
        deployHeader.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.MEDIUM);
        detailPanel.add(deployHeader);
        detailPanel.add(createDetailSection("Deployment", entry.deploymentName()));
        detailPanel.add(createDetailSection("SIL", entry.safetyIntegrityLevel()));
        detailPanel.add(createDetailSection("Determinism", entry.determinismClass()));
        detailPanel.add(createDetailSection("Network Zone", entry.networkZone()));

        var perfHeader = new H3("Performance");
        perfHeader.addClassNames(LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.MEDIUM);
        detailPanel.add(perfHeader);
        detailPanel.add(createDetailSection("Recall", entry.recallFormatted()));
        detailPanel.add(createDetailSection("Latency", entry.latencyFormatted()));

        detailPanel.add(createDetailSection("Container Image", entry.containerImage()));
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

    private static boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase().contains(search.toLowerCase());
    }
}
