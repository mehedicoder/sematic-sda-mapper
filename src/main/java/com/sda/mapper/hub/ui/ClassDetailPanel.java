package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageEntry;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyLayer;
import com.sda.mapper.service.OntologyDataService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Inspector for a selected ontology class: status header, provenance, raw
 * entity data and gap actions (supplementary upload and manual entry).
 */
public class ClassDetailPanel extends VerticalLayout {

    private final OntologyDataService ontologyDataService;
    private final BiConsumer<String, byte[]> onSupplementaryUpload;
    private final Consumer<String> onError;
    private final Consumer<CanonicalEntity> onManualSave;

    private boolean manualFormVisible;
    private CanonicalAssetModel currentModel;
    private List<CanonicalEntity> currentInferred;

    /**
     * Creates the detail panel.
     */
    public ClassDetailPanel(OntologyDataService ontologyDataService,
                            BiConsumer<String, byte[]> onSupplementaryUpload,
                            Consumer<String> onError,
                            Consumer<CanonicalEntity> onManualSave) {
        this.ontologyDataService = ontologyDataService;
        this.onSupplementaryUpload = onSupplementaryUpload;
        this.onError = onError;
        this.onManualSave = onManualSave;
        setPadding(true);
        setSpacing(true);
        addClassName("hub-detail-panel");
    }

    /**
     * Re-renders the panel for the given class.
     *
     * @param className the selected class name, or {@code null} for the empty state
     * @param report    the current coverage report
     * @param model     the canonical model
     * @param inferred  the inferred entities
     * @param files     the uploaded files (for provenance)
     */
    public void update(String className, GapReport report, CanonicalAssetModel model,
                       List<CanonicalEntity> inferred, List<UploadedFileInfo> files) {
        removeAll();
        manualFormVisible = false;
        currentModel = model;
        currentInferred = inferred;
        if (className == null) {
            add(createEmptyState());
            return;
        }
        Optional<OntologyClass> clazz = ontologyDataService.getClassByName(className);
        if (clazz.isEmpty()) {
            add(createEmptyState());
            return;
        }

        CoverageEntry entry = report.forClass(className)
            .orElse(CoverageEntry.missing(className, clazz.get().layerId()));
        OntologyLayer layer = ontologyDataService.getLayerById(entry.layerId()).orElse(null);

        add(createHeader(clazz.get(), entry, layer));
        add(createProvenance(entry, files, model, inferred));
        add(createRawData(clazz.get().name(), model, inferred));
        add(createGapAnalysis(className, entry));
        add(createGapActions(clazz.get().name(), entry));
    }

    private HorizontalLayout createHeader(OntologyClass clazz, CoverageEntry entry, OntologyLayer layer) {
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        header.setSpacing(true);

        Span code = new Span(clazz.code());
        code.addClassName("hub-class-code");

        H3 title = new H3(clazz.name());
        title.addClassName("hub-detail-title");
        title.getStyle().set("margin", "0");

        Span status = new Span(entry.status().label());
        status.addClassName("hub-status-pill");
        status.addClassName("hub-status--" + entry.status().name().toLowerCase());

        header.add(code, title, status);
        if (layer != null) {
            Span layerPill = new Span(layer.label());
            layerPill.addClassName("hub-layer-pill");
            header.add(layerPill);
        }
        return header;
    }

    private Div createProvenance(CoverageEntry entry, List<UploadedFileInfo> files,
                                 CanonicalAssetModel model, List<CanonicalEntity> inferred) {
        Div provenance = new Div();
        provenance.addClassName("hub-provenance");
        provenance.add(createSectionLabel("Provenance"));

        Map<String, String> originByFile = files.stream()
            .collect(Collectors.toMap(UploadedFileInfo::fileName,
                f -> f.format().origin().label(), (a, b) -> a));

        List<String> explicitLines = new ArrayList<>();
        List<CanonicalEntity> explicit = model.getEntitiesForClass(entry.className());
        for (CanonicalEntity entity : explicit) {
            String source = entity.sourceFileName().orElse("");
            String origin = originByFile.getOrDefault(source, "Unknown origin");
            String line = source.isBlank()
                ? "Found via " + origin + " \u2192 LIFT"
                : "Found in " + source + " via " + origin + " \u2192 LIFT";
            if (!explicitLines.contains(line)) {
                explicitLines.add(line);
            }
        }

        Set<String> inferredSources = new LinkedHashSet<>();
        for (CanonicalEntity entity : inferred) {
            if (entity.className().equals(entry.className())) {
                entity.sourceFileName().ifPresent(inferredSources::add);
            }
        }

        if (entry.status() == CoverageStatus.MISSING) {
            Span line = new Span("Not found. Upload a supplementary file or enter the data manually.");
            line.addClassName("hub-provenance-line");
            provenance.add(line);
        } else {
            for (String line : explicitLines) {
                Span row = new Span(line);
                row.addClassName("hub-provenance-line");
                provenance.add(row);
            }
            if (!inferredSources.isEmpty()) {
                Span row = new Span("Inferred from: " + String.join(", ", inferredSources));
                row.addClassName("hub-provenance-line");
                provenance.add(row);
            } else if (explicitLines.isEmpty()) {
                Span row = new Span("Inferred from the mapped model data");
                row.addClassName("hub-provenance-line");
                provenance.add(row);
            }
        }
        return provenance;
    }

    private VerticalLayout createRawData(String className, CanonicalAssetModel model,
                                         List<CanonicalEntity> inferred) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(true);
        section.add(createSectionLabel("AAS Data Mapping"));

        List<CanonicalEntity> entities = new ArrayList<>(model.getEntitiesForClass(className));
        entities.addAll(inferred.stream()
            .filter(e -> e.className().equals(className))
            .toList());

        if (entities.isEmpty()) {
            Span none = new Span("No data available for this class yet.");
            none.addClassName("hub-raw-empty");
            section.add(none);
            return section;
        }

        for (CanonicalEntity entity : entities) {
            section.add(createEntityCard(entity));
        }
        return section;
    }

    private Div createEntityCard(CanonicalEntity entity) {
        Div card = new Div();
        card.addClassName("hub-entity-card");

        Span name = new Span(entity.name());
        name.addClassName("hub-entity-name");
        card.add(name);

        if (entity.inferred()) {
            Span pill = new Span("inferred");
            pill.addClassName("hub-inferred-pill");
            card.add(pill);
        }

        for (Map.Entry<String, Object> attribute : entity.attributes().entrySet()) {
            Div row = new Div();
            row.addClassName("hub-attribute-row");
            Span key = new Span(attribute.getKey());
            key.addClassName("hub-attribute-key");
            Span value = new Span(String.valueOf(attribute.getValue()));
            value.addClassName("hub-attribute-value");
            row.add(key, value);
            card.add(row);
        }
        return card;
    }

    private VerticalLayout createGapAnalysis(String className, CoverageEntry entry) {
        VerticalLayout analysis = new VerticalLayout();
        analysis.setPadding(false);
        analysis.setSpacing(true);
        analysis.add(createSectionLabel("Gap analysis"));

        if (entry.status() == CoverageStatus.PRESENT) {
            Span ok = new Span("No gaps \u2014 all required data for " + className + " is present.");
            ok.addClassName("hub-gap-hint");
            analysis.add(ok);
            return analysis;
        }

        List<String> sources = entry.sources();
        Span source = new Span("AAS source: "
            + (sources.isEmpty() ? "Not found" : String.join(", ", sources)));
        source.addClassName("hub-provenance-line");
        analysis.add(source);

        if (!entry.missingFields().isEmpty()) {
            Span missing = new Span("Missing fields: " + String.join(", ", entry.missingFields()));
            missing.addClassName("hub-gap-hint");
            analysis.add(missing);
        }

        Span note = new Span(layerOne(entry.layerId())
            ? "Enter the missing data manually to close this gap."
            : "Upload a supplementary file of a matching format or enter the "
                + "data manually to close this gap.");
        note.addClassName("hub-gap-hint");
        analysis.add(note);
        return analysis;
    }

    private VerticalLayout createGapActions(String className, CoverageEntry entry) {
        VerticalLayout actions = new VerticalLayout();
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.add(createSectionLabel("Gap actions"));

        boolean layerOne = layerOne(entry.layerId());
        boolean manualSupported = ManualEntryFormFactory.supports(className);

        if (entry.status() != CoverageStatus.PRESENT && !layerOne) {
            Span hint = new Span("Upload a supplementary file to fill this gap");
            hint.addClassName("hub-gap-hint");
            actions.add(hint);
            UploadDropzone mini = new UploadDropzone("Upload supplementary file",
                List.of(AssetFileFormat.values()), onSupplementaryUpload, onError);
            actions.add(mini.get());
        }

        if (manualSupported) {
            if (entry.status() != CoverageStatus.PRESENT && layerOne) {
                Span hint = new Span("L1 gaps are filled by manual entry \u2014 "
                    + "file upload is not available for this layer.");
                hint.addClassName("hub-gap-hint");
                actions.add(hint);
            }
            Button toggle = new Button("Enter manually");
            toggle.addClassName("hub-manual-toggle");
            toggle.addClickListener(e -> {
                manualFormVisible = !manualFormVisible;
                toggleManualForm(className, toggle);
            });
            actions.add(toggle);
        } else {
            Span hint = new Span(layerOne
                ? "Manual entry is not available for " + className + "."
                : "Manual entry is not available for " + className
                    + " \u2014 upload a supplementary file instead.");
            hint.addClassName("hub-gap-hint");
            actions.add(hint);
        }
        return actions;
    }

    private static boolean layerOne(String layerId) {
        return "L1".equals(layerId);
    }

    private void toggleManualForm(String className, Button toggle) {
        if (manualFormVisible) {
            VerticalLayout form = ManualEntryFormFactory.create(className,
                dropdownOptions(className), entity -> {
                    manualFormVisible = false;
                    onManualSave.accept(entity);
                });
            if (form != null) {
                add(form);
                toggle.setText("Close manual entry");
            }
        } else {
            getChildren().filter(c -> c.hasClassName("hub-manual-form"))
                .toList()
                .forEach(this::remove);
            toggle.setText("Enter manually");
        }
    }

    private Map<String, List<String>> dropdownOptions(String className) {
        Map<String, List<String>> options = new LinkedHashMap<>();
        switch (className) {
            case "UseCase" -> options.put("targetAsset", namesOf("Asset"));
            case "Sensor" -> options.put("measuredVariable", namesOf("ProcessVariable"));
            case "Capability" -> options.put("mapsToRequirement", namesOf("UseCaseRequirement"));
            case "ServiceInstance" -> options.put("deployedOn", namesOf("ClusterNode"));
            case "VerificationCriterion" -> options.put("verifiesRequirement", namesOf("UseCaseRequirement"));
            default -> {
                // no dropdowns
            }
        }
        return options;
    }

    private List<String> namesOf(String entityClass) {
        List<String> names = new ArrayList<>();
        if (currentModel != null) {
            currentModel.getEntitiesForClass(entityClass)
                .forEach(entity -> names.add(entity.name()));
        }
        if (currentInferred != null) {
            currentInferred.stream()
                .filter(entity -> entity.className().equals(entityClass))
                .forEach(entity -> names.add(entity.name()));
        }
        return names.stream().distinct().toList();
    }

    private static Span createSectionLabel(String text) {
        Span label = new Span(text);
        label.addClassName("hub-section-label");
        return label;
    }

    private Div createEmptyState() {
        Div empty = new Div();
        empty.addClassName("hub-empty-state");
        empty.add(new Span("No class selected"));
        Span hint = new Span("Pick a class from the browser to inspect coverage, "
            + "provenance and gap actions.");
        hint.addClassName("hub-gap-hint");
        empty.add(hint);
        return empty;
    }
}
