package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CoverageEntry;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.service.OntologyDataService;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.function.Consumer;

/**
 * Class browser rows for the selected layer: one row per class with a status
 * dot, a diagram code, an explicit entity count and an "inferred" pill when
 * the class is only covered by inference.
 */
public class ClassListPanel extends VerticalLayout {

    private final Consumer<String> onSelectClass;

    /**
     * Creates the class list.
     *
     * @param onSelectClass callback with the selected class name
     */
    public ClassListPanel(Consumer<String> onSelectClass) {
        this.onSelectClass = onSelectClass;
        setPadding(false);
        setSpacing(true);
        setWidth("320px");
        setMinWidth("320px");
        addClassName("hub-class-list");
    }

    /**
     * Re-renders the rows for the given classes.
     *
     * @param classes      the classes to show
     * @param ontologyData the ontology data service (for layer colors)
     * @param report       the current coverage report
     * @param selectedName the currently selected class name
     * @param model        the canonical model
     */
    public void update(List<OntologyClass> classes, OntologyDataService ontologyData,
                       GapReport report, String selectedName, CanonicalAssetModel model) {
        removeAll();
        for (OntologyClass clazz : classes) {
            add(createRow(clazz, ontologyData, report, selectedName, model));
        }
    }

    private Div createRow(OntologyClass clazz, OntologyDataService ontologyData,
                          GapReport report, String selectedName, CanonicalAssetModel model) {
        Div row = new Div();
        row.addClassName("hub-class-item");

        ontologyData.getLayerById(clazz.layerId()).ifPresent(layer ->
            row.addClassName("hub-class-item--" + layer.cssKey()));

        CoverageStatus status = report.forClass(clazz.name())
            .map(CoverageEntry::status)
            .orElse(CoverageStatus.MISSING);

        Span dot = new Span();
        dot.addClassName("hub-status-dot");
        dot.addClassName("hub-status--" + status.name().toLowerCase());

        Span code = new Span(clazz.code());
        code.addClassName("hub-class-code");

        Span name = new Span(clazz.name());
        name.addClassName("hub-class-name");

        row.add(dot, code, name);

        int explicitCount = model.countForClass(clazz.name());
        if (explicitCount > 0) {
            Span countBadge = new Span(String.valueOf(explicitCount));
            countBadge.addClassName("hub-class-count");
            row.add(countBadge);
        }

        if (status == CoverageStatus.INFERRED) {
            Span pill = new Span("inferred");
            pill.addClassName("hub-inferred-pill");
            row.add(pill);
        }

        if (clazz.name().equals(selectedName)) {
            row.addClassName("hub-class-item--selected");
        }
        row.getElement().addEventListener("click", e -> onSelectClass.accept(clazz.name()));
        return row;
    }
}
