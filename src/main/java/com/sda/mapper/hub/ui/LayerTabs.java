package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.model.OntologyLayer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Layer navigation tabs with a missing-count badge per layer, following the
 * CSS-SDA hierarchy. The badge is hidden when the layer has no missing class.
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
        String text = missing > 0 ? label + " (" + missing + ")" : label;
        Button button = new Button(text);
        button.addClassName("hub-tab");
        if (cssKey != null) {
            button.addClassName("hub-tab--" + cssKey);
        }
        if (id.equals(selected)) {
            button.addClassName("hub-tab--active");
        }
        button.addClickListener(e -> onSelect.accept(id));
        return button;
    }

    private static int countMissing(GapReport report, String layerId) {
        return (int) report.entries().stream()
            .filter(e -> e.status() == CoverageStatus.MISSING)
            .filter(e -> "ALL".equals(layerId) || e.layerId().equals(layerId))
            .count();
    }
}
