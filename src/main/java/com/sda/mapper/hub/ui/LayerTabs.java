package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.LayerCoverage;
import com.sda.mapper.model.OntologyLayer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Layer navigation tabs with a per-layer coverage percentage badge, following
 * the CSS-SDA hierarchy. The badge shows the aggregated coverage percentage of
 * each layer (or the overall percentage for the All Layers tab).
 */
public class LayerTabs extends HorizontalLayout {

    private final Consumer<String> onSelect;

    /**
     * Creates the layer tabs.
     *
     * @param onSelect callback with the layer id or {@code "ALL"}
     */
    public LayerTabs(Consumer<String> onSelect) {
        this.onSelect = onSelect;
        addClassName("hub-tabs");
        setPadding(false);
        setSpacing(true);
    }

    /**
     * Re-renders the tabs for the given layers and coverage report.
     *
     * @param layers   the five CSS-SDA layers
     * @param report   the current coverage report
     * @param selected the currently selected layer id
     */
    public void update(List<OntologyLayer> layers, GapReport report, String selected) {
        removeAll();
        add(createTab("ALL", "All Layers", null, report, selected));
        for (OntologyLayer layer : layers) {
            add(createTab(layer.id(), layer.label(), layer.cssKey(), report, selected));
        }
    }

    private Button createTab(String id, String label, String cssKey,
                             GapReport report, String selected) {
        int missing = countMissing(report, id);
        Button button = new Button(label);
        button.addClassName("hub-tab");
        if (cssKey != null) {
            button.addClassName("hub-tab--" + cssKey);
        }
        if (id.equals(selected)) {
            button.addClassName("hub-tab--active");
        }
        Span badge = createBadge(report, id);
        button.getElement().appendChild(badge.getElement());
        if (missing > 0) {
            Span gapBadge = new Span(String.valueOf(missing));
            gapBadge.addClassName("hub-tab-gap-badge");
            gapBadge.getElement().setAttribute("title", "Missing classes in this layer");
            button.getElement().appendChild(gapBadge.getElement());
        }
        button.addClickListener(e -> onSelect.accept(id));
        return button;
    }

    private Span createBadge(GapReport report, String layerId) {
        Span badge = new Span(coverageLabel(report, layerId));
        badge.addClassName("hub-tab-badge");
        badge.addClassName("hub-tab-badge--coverage");
        badge.getElement().setAttribute("title", "Layer coverage percentage");
        return badge;
    }

    private static String coverageLabel(GapReport report, String layerId) {
        if ("ALL".equals(layerId)) {
            return Math.round(report.coveragePercent()) + "%";
        }
        return report.layerCoverage().stream()
            .filter(l -> l.layerId().equals(layerId))
            .map(LayerCoverage::coveragePercent)
            .findFirst()
            .map(pct -> Math.round(pct) + "%")
            .orElse("0%");
    }

    private static int countMissing(GapReport report, String layerId) {
        return (int) report.entries().stream()
            .filter(e -> e.status() == CoverageStatus.MISSING)
            .filter(e -> "ALL".equals(layerId) || e.layerId().equals(layerId))
            .count();
    }
}
