package com.sda.mapper.ui;

import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyDatatypeProperty;
import com.sda.mapper.model.OntologyLayer;
import com.sda.mapper.model.OntologyRelation;
import com.sda.mapper.service.OntologyDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Interactive ontology visualizer for the CSS-SDA hierarchy.
 * Follows the layer navigation order Intent → Capability → Skill → Service →
 * CommunicationBinding → DataConnector → Deployment via layer tabs, a class
 * browser, a class inspector with a CSS-driven neighborhood graph and
 * outgoing/incoming relation cards.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Ontology Visualizer")
@CssImport("./styles.css")
public class OntologyView extends VerticalLayout {

    private static final String ALL_LAYERS = "ALL";

    private final OntologyDataService dataService;
    private final List<Button> tabButtons = new ArrayList<>();
    private final VerticalLayout classItemsPanel;
    private final VerticalLayout detailPanel;

    private String selectedLayerId = ALL_LAYERS;
    private String selectedClassName;

    public OntologyView(OntologyDataService dataService) {
        this.dataService = dataService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("ontology-root");

        add(createHeader());
        add(createStatsBar());
        add(createTabsBar());

        var classListPanel = createClassListPanel();
        classItemsPanel = new VerticalLayout();
        classItemsPanel.setPadding(false);
        classItemsPanel.setSpacing(false);
        var classScroller = new Scroller(classItemsPanel);
        classScroller.setWidthFull();
        classScroller.setHeightFull();
        classScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        classListPanel.add(classScroller);
        classListPanel.setFlexGrow(1, classScroller);

        detailPanel = new VerticalLayout();
        detailPanel.setWidthFull();
        detailPanel.setPadding(true);
        detailPanel.setSpacing(true);
        detailPanel.addClassName("ontology-detail-panel");

        var main = new HorizontalLayout(classListPanel, detailPanel);
        main.addClassName("ontology-main");
        main.setSpacing(true);
        main.setAlignItems(FlexComponent.Alignment.STRETCH);
        main.setSizeFull();
        main.setFlexGrow(1, detailPanel);

        add(main);
        setFlexGrow(1, main);

        renderClassList();
        renderDetail();
    }

    private VerticalLayout createHeader() {
        var title = new H2("Ontology Visualizer");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE, "section-title");
        title.getStyle().set("margin-top", "0");

        var header = new VerticalLayout(title);
        header.setPadding(false);
        header.setSpacing(false);
        return header;
    }

    private HorizontalLayout createStatsBar() {
        long total = dataService.getRelations().size();
        long core = dataService.getRelations().stream()
            .filter(r -> "core".equals(r.kind()))
            .count();
        long supporting = total - core;

        var layersPill = createStatPill("Layers", String.valueOf(dataService.getLayers().size()));
        var classesPill = createStatPill("Classes", String.valueOf(
            dataService.getClasses().size() + dataService.getSubClasses().size()));
        var derivedPill = createStatPill("Derived", String.valueOf(dataService.getSubClasses().size()));
        var relationsPill = createStatPill("Relations", String.valueOf(total));
        var corePill = createStatPill("Core", String.valueOf(core));
        var supportingPill = createStatPill("Supporting", String.valueOf(supporting));

        var stats = new HorizontalLayout(layersPill, classesPill, derivedPill,
            relationsPill, corePill, supportingPill);
        stats.addClassName("ontology-stats");
        stats.setPadding(false);
        stats.setSpacing(true);
        return stats;
    }

    private Span createStatPill(String label, String value) {
        var pill = new Span();
        pill.addClassName("ontology-stats-pill");
        pill.add(new Span(label + ": "));
        var strong = new Span(value);
        strong.getElement().getStyle().set("font-weight", "600");
        pill.add(strong);
        return pill;
    }

    private HorizontalLayout createTabsBar() {
        var tabs = new HorizontalLayout();
        tabs.addClassName("ontology-tabs");
        tabs.setPadding(false);
        tabs.setSpacing(true);

        tabs.add(createTabButton(ALL_LAYERS, "All Layers", null));
        for (OntologyLayer layer : dataService.getLayers()) {
            tabs.add(createTabButton(layer.id(), layer.label(), layer.cssKey()));
        }
        return tabs;
    }

    private Button createTabButton(String layerId, String label, String cssKey) {
        var button = new Button(label);
        button.addClassName("ontology-tab");
        button.getElement().setAttribute("data-layer", layerId);
        if (cssKey != null) {
            button.addClassName("ontology-tab--" + cssKey);
        }
        if (layerId.equals(selectedLayerId)) {
            button.addClassName("ontology-tab--active");
        }
        button.addClickListener(e -> selectLayer(layerId));
        tabButtons.add(button);
        return button;
    }

    /**
     * Selects a layer tab and re-renders the class browser.
     * Package-private for testability.
     *
     * @param layerId layer identifier or {@code "ALL"}
     */
    void selectLayer(String layerId) {
        selectedLayerId = layerId;
        for (Button tab : tabButtons) {
            tab.removeClassName("ontology-tab--active");
            if (layerId.equals(tab.getElement().getAttribute("data-layer"))) {
                tab.addClassName("ontology-tab--active");
            }
        }
        renderClassList();
    }

    /**
     * Selects a class and re-renders the class browser and inspector.
     * Package-private for testability.
     *
     * @param className class name (e.g. {@code "UseCaseRequirement"})
     */
    void selectClass(String className) {
        if (dataService.getClassByName(className).isPresent()) {
            selectedClassName = className;
            renderClassList();
            renderDetail();
        }
    }

    private VerticalLayout createClassListPanel() {
        var panel = new VerticalLayout();
        panel.setWidth("280px");
        panel.setMinWidth("280px");
        panel.setPadding(false);
        panel.setSpacing(true);
        panel.addClassName("ontology-class-list");

        var header = new Span("Classes");
        header.addClassName("section-title");
        panel.add(header);
        return panel;
    }

    private void renderClassList() {
        classItemsPanel.removeAll();
        if (ALL_LAYERS.equals(selectedLayerId)) {
            for (OntologyLayer layer : dataService.getLayers()) {
                classItemsPanel.add(createLayerHeader(layer));
                renderLayerClasses(layer.id());
            }
        } else {
            renderLayerClasses(selectedLayerId);
        }
    }

    private void renderLayerClasses(String layerId) {
        for (OntologyClass clazz : dataService.getClassesByLayer(layerId)) {
            classItemsPanel.add(createClassItem(clazz));
            for (OntologyClass sub : dataService.getSubClassesOf(clazz.name())) {
                classItemsPanel.add(createClassItem(sub));
            }
        }
    }

    private Span createLayerHeader(OntologyLayer layer) {
        var header = new Span(layer.label());
        header.addClassName("section-subtitle");
        header.getStyle()
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("text-transform", "uppercase")
            .set("letter-spacing", "0.05em")
            .set("padding", "10px 12px 4px 12px")
            .set("color", layer.colorHex());
        return header;
    }

    private Div createClassItem(OntologyClass clazz) {
        var item = new Div();
        item.addClassName("ontology-class-item");
        item.getStyle().set("font-family", "var(--lumo-font-family)");
        if (clazz.derived()) {
            item.addClassName("ontology-class-item--derived");
        }

        var code = new Span(clazz.code());
        code.addClassName("ontology-class-code");
        var name = new Span(clazz.name());
        name.getStyle().set("font-weight", "600");

        item.add(code, name);
        if (clazz.derived()) {
            var badge = new Span("subclass of " + clazz.parent());
            badge.addClassName("ontology-subclass-badge");
            item.add(badge);
        }

        dataService.getLayerById(clazz.layerId()).ifPresent(layer -> {
            item.addClassName("ontology-class-item--" + layer.cssKey());
            item.getStyle().set("border-left", "3px solid " + layer.colorHex());
        });
        if (clazz.name().equals(selectedClassName)) {
            item.addClassName("ontology-class-item--selected");
        }
        item.getElement().addEventListener("click", e -> selectClass(clazz.name()));
        return item;
    }

    private void renderDetail() {
        detailPanel.removeAll();
        Optional<OntologyClass> selected = dataService.getClassByName(selectedClassName);
        if (selected.isEmpty()) {
            detailPanel.add(createEmptyState());
            return;
        }

        OntologyClass clazz = selected.get();
        OntologyLayer layer = dataService.getLayerById(clazz.layerId()).orElse(null);

        var headerRow = new HorizontalLayout();
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);

        var codeBadge = new Span(clazz.code());
        codeBadge.addClassName("ontology-class-code");
        codeBadge.getStyle()
            .set("background-color", "#334155")
            .set("border-radius", "6px")
            .set("padding", "2px 8px");

        var title = new H3(clazz.name());
        title.addClassName("section-title");
        title.getStyle().set("margin", "0");

        headerRow.add(codeBadge, title);
        if (layer != null) {
            headerRow.add(createLayerPill(layer));
        }
        detailPanel.add(headerRow);

        if (clazz.derived()) {
            detailPanel.add(createSubclassNote(clazz));
        }

        detailPanel.add(new H3("Neighborhood Graph"));
        detailPanel.add(createGraph(clazz, layer));

        List<OntologyRelation> outgoing = dataService.getRelationsFrom(clazz.name());
        detailPanel.add(createRelationSection("Outgoing Relations", outgoing, true));

        List<OntologyRelation> incoming = dataService.getRelationsTo(clazz.name());
        detailPanel.add(createRelationSection("Incoming Relations", incoming, false));

        List<OntologyDatatypeProperty> props = dataService.getDatatypeProperties(clazz.name());
        detailPanel.add(createDatatypePropertySection(props));
    }

    private Div createEmptyState() {
        var empty = new Div();
        empty.addClassName("ontology-empty-state");
        empty.getStyle()
            .set("background-color", "#0f172a")
            .set("border", "1px solid #334155")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "32px")
            .set("min-height", "240px");
        empty.add(new Span("No class selected"));
        var hint = new Span("Pick a class from the browser to inspect its neighborhood and relations.");
        hint.getStyle().set("font-size", "var(--lumo-font-size-s)");
        empty.add(hint);
        return empty;
    }

    private Span createLayerPill(OntologyLayer layer) {
        var pill = new Span(layer.label());
        pill.addClassName("ontology-layer-pill");
        pill.getStyle()
            .set("background-color", layer.colorHex() + "22")
            .set("color", layer.colorHex())
            .set("border", "1px solid " + layer.colorHex());
        return pill;
    }

    private Div createSubclassNote(OntologyClass clazz) {
        var note = new Div();
        note.addClassName("ontology-subclass-note");
        var parent = new Span(clazz.parent());
        parent.getStyle().set("font-weight", "600").set("color", "#e2e8f0");
        var text = new Span("Subclass of ");
        var inherited = new Span(" — inherits the parent's layer and relations.");
        inherited.getStyle().set("color", "#64748b").set("font-size", "var(--lumo-font-size-s)");
        note.add(text, parent, inherited);
        return note;
    }

    private Div createGraph(OntologyClass clazz, OntologyLayer layer) {
        var graph = new Div();
        graph.addClassName("ontology-graph");

        var left = new Div();
        left.addClassName("graph-side");
        var center = createGraphNode(clazz.name(), layer, true);
        var right = new Div();
        right.addClassName("graph-side");

        for (OntologyRelation relation : dataService.getRelationsTo(clazz.name())) {
            var row = new Div();
            row.addClassName("graph-row");
            row.add(createGraphNode(relation.from(), null, false));
            row.add(createGraphEdge(relation, true));
            left.add(row);
        }
        for (OntologyRelation relation : dataService.getRelationsFrom(clazz.name())) {
            var row = new Div();
            row.addClassName("graph-row");
            row.add(createGraphEdge(relation, false));
            row.add(createGraphNode(relation.to(), null, false));
            right.add(row);
        }

        graph.add(left, center, right);
        return graph;
    }

    private Div createGraphEdge(OntologyRelation relation, boolean incoming) {
        var edge = new Div();
        edge.addClassName("graph-edge");
        if ("core".equals(relation.kind())) {
            edge.addClassName("graph-edge--core");
        }
        if (relation.inherited()) {
            edge.addClassName("graph-edge--inherited");
        }
        edge.setText(incoming ? "\u2192 " + relation.label() : relation.label() + " \u2192");
        return edge;
    }

    private Div createGraphNode(String className, OntologyLayer forcedLayer, boolean selected) {
        Optional<OntologyClass> clazz = dataService.getClassByName(className);
        if (clazz.isEmpty()) {
            var missing = new Div();
            missing.addClassName("node");
            missing.setText(className);
            return missing;
        }
        OntologyClass value = clazz.get();
        OntologyLayer layer = forcedLayer != null
            ? forcedLayer
            : dataService.getLayerById(value.layerId()).orElse(null);

        var node = new Div();
        node.addClassName("node");
        if (layer != null) {
            node.addClassName("node--" + layer.cssKey());
            node.getStyle().set("background-color", layer.colorHex() + "22");
        }
        if (selected) {
            node.addClassName("node--selected");
        }

        var code = new Span(value.code());
        code.addClassName("ontology-class-code");
        code.getStyle().set("min-width", "0");
        var name = new Span(value.name());
        name.getStyle().set("font-weight", "600");
        node.add(code, name);
        return node;
    }

    private VerticalLayout createRelationSection(String title, List<OntologyRelation> relations,
                                                 boolean outgoing) {
        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        var heading = new Span(title + " (" + relations.size() + ")");
        heading.addClassName("section-title");
        heading.getStyle().set("font-size", "var(--lumo-font-size-m)");
        section.add(heading);

        if (relations.isEmpty()) {
            var none = new Span("None");
            none.getStyle().set("color", "#64748b");
            section.add(none);
        } else {
            relations.stream()
                .map(r -> createRelationCard(r, outgoing))
                .forEach(section::add);
        }
        return section;
    }

    private Div createRelationCard(OntologyRelation relation, boolean outgoing) {
        var card = new Div();
        card.addClassName("ontology-card");
        card.addClassName("core".equals(relation.kind())
            ? "ontology-card--core"
            : "ontology-card--supporting");
        if (relation.inherited()) {
            card.addClassName("ontology-card--inherited");
        }
        card.getStyle().set("width", "100%");

        var labelRow = new Div();
        labelRow.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "space-between")
            .set("gap", "8px");

        var label = new Span(relation.label());
        label.getStyle().set("font-weight", "600");
        label.getStyle().set("color", "core".equals(relation.kind()) ? "#3b82f6" : "#94a3b8");
        labelRow.add(label);

        var kindBadge = new Span("core".equals(relation.kind()) ? "CORE" : "SUPPORTING");
        kindBadge.getStyle()
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("color", "#64748b")
            .set("border", "1px solid #334155")
            .set("border-radius", "999px")
            .set("padding", "0 8px");
        labelRow.add(kindBadge);

        if (relation.inherited()) {
            var inheritedBadge = new Span("INHERITED");
            inheritedBadge.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "#a855f7")
                .set("border", "1px dashed #a855f7")
                .set("border-radius", "999px")
                .set("padding", "0 8px");
            labelRow.add(inheritedBadge);
        }

        var path = new Span(relation.from() + "  \u2192  " + relation.to());
        path.getStyle().set("color", "#94a3b8");

        var domainRange = new Div();
        domainRange.getStyle()
            .set("display", "flex")
            .set("gap", "12px")
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("color", "#64748b");
        var domainLabel = new Span("Domain: " + relation.from());
        domainLabel.getStyle().set("color", "#10b981");
        var rangeLabel = new Span("Range: " + relation.to());
        rangeLabel.getStyle().set("color", "#f59e0b");
        domainRange.add(domainLabel, rangeLabel);

        card.add(labelRow, path, domainRange);
        if (relation.inverse() != null) {
            var inverse = new Span("Inverse: " + relation.inverse());
            inverse.getStyle().set("color", "#64748b").set("font-size", "var(--lumo-font-size-xs)");
            card.add(inverse);
        }
        return card;
    }

    private VerticalLayout createDatatypePropertySection(List<OntologyDatatypeProperty> props) {
        var section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);

        var heading = new Span("Datatype Properties (" + props.size() + ")");
        heading.addClassName("section-title");
        heading.getStyle().set("font-size", "var(--lumo-font-size-m)");
        section.add(heading);

        if (props.isEmpty()) {
            var none = new Span("None");
            none.getStyle().set("color", "#64748b");
            section.add(none);
        } else {
            for (OntologyDatatypeProperty prop : props) {
                section.add(createDatatypePropertyCard(prop));
            }
        }
        return section;
    }

    private Div createDatatypePropertyCard(OntologyDatatypeProperty prop) {
        var card = new Div();
        card.addClassName("ontology-card");
        card.addClassName("ontology-card--supporting");
        card.getStyle().set("width", "100%");

        var labelRow = new Div();
        labelRow.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "space-between")
            .set("gap", "8px");

        var label = new Span(prop.name());
        label.getStyle().set("font-weight", "600").set("color", "#94a3b8");
        labelRow.add(label);

        var typeBadge = new Span(prop.xsdType());
        typeBadge.getStyle()
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("color", "#f59e0b")
            .set("border", "1px solid #334155")
            .set("border-radius", "999px")
            .set("padding", "0 8px");
        labelRow.add(typeBadge);

        var domainRange = new Div();
        domainRange.getStyle()
            .set("display", "flex")
            .set("gap", "12px")
            .set("font-size", "var(--lumo-font-size-xs)")
            .set("color", "#64748b");
        var domainLabel = new Span("Domain: " + prop.className());
        domainLabel.getStyle().set("color", "#10b981");
        var rangeLabel = new Span("Range: " + prop.xsdType());
        rangeLabel.getStyle().set("color", "#f59e0b");
        domainRange.add(domainLabel, rangeLabel);

        card.add(labelRow, domainRange);
        return card;
    }
}
