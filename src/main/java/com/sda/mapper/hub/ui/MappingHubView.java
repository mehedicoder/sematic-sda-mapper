package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.error.MappingException;
import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.sda.mapper.hub.parser.ParsingException;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.MappingHubSession;
import com.sda.mapper.hub.service.PipelineResult;
import com.sda.mapper.llm.ConceptHint;
import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyLayer;
import com.sda.mapper.observability.ReasoningTracePublisher;

import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.service.OntologyDataService;
import com.sda.mapper.ui.MainLayout;
import com.sda.mapper.ui.SdaAppShell;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Universal semantic mapping hub: ingests AAS XML, MTP AML and OPC UA JSON
 * asset descriptions and runs Parse &rarr; Lift &rarr; Infer &rarr; Match
 * &rarr; Manifest with per-layer coverage, gap actions and context-aware
 * recommendations.
 *
 * <p>Consolidates pipeline observability: the run log ({@link ConsolePanel})
 * still records every stage, and a {@link ReasoningTreePanel} renders the live
 * ontology reasoning chain derived during the MATCH stage (requirement &rarr;
 * capability &rarr; skill &rarr; service &rarr; binding/deployment context).
 * Server push is enabled application-wide via {@link SdaAppShell}.</p>
 */
@Route(value = "mapping-hub", layout = MainLayout.class)
@PageTitle("Mapping Hub")
@CssImport("./hub-styles.css")
public class MappingHubView extends VerticalLayout {

    private final MappingHubService hubService;
    private final OntologyDataService ontologyDataService;

    private final ConsolePanel console;
    private final ReasoningTreePanel reasoningTreePanel;
    private final LayerTabs layerTabs;
    private final ClassListPanel classListPanel;
    private final ClassDetailPanel detailPanel;
    private final VerticalLayout fileQueue;
    private final VerticalLayout dashboard;
    private final ConceptSuggestionPanel suggestionPanel;
    private final ComboBox<String> requirementCombo;
    private final HorizontalLayout activeRequirementsLayout;
    private Button exportButton;
    private Button exportReasoningButton;

    private String selectedLayerId = "ALL";
    private String selectedClassName;
    private PipelineResult lastResult;

    /**
     * Creates the mapping hub view.
     *
     * @param hubService                the mapping hub pipeline service
     * @param ontologyDataService       the ontology data service
     * @param manualEntryFormFactory    the ontology-driven manual entry form factory
     * @param reasoningTracePublisher   the reasoning-trace publisher for the live match tree
     */
    public MappingHubView(MappingHubService hubService, OntologyDataService ontologyDataService,
                            ManualEntryFormFactory manualEntryFormFactory,
                            ReasoningTracePublisher reasoningTracePublisher) {
        this.hubService = hubService;
        this.ontologyDataService = ontologyDataService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("hub-root");
        getElement().setAttribute("theme", "dark");

        add(createHeader());

        console = new ConsolePanel();
        reasoningTreePanel = new ReasoningTreePanel(reasoningTracePublisher);
        fileQueue = new VerticalLayout();
        fileQueue.addClassName("hub-file-queue");
        fileQueue.setPadding(false);
        fileQueue.setSpacing(false);

        suggestionPanel = new ConceptSuggestionPanel(
            hubService.isLlmAvailable(),
            hubService::suggestConcepts,
            this::onConceptSuggestionAdd);

        requirementCombo = new ComboBox<>("Requirement");
        requirementCombo.setWidth("320px");
        requirementCombo.setAllowCustomValue(false);
        activeRequirementsLayout = new HorizontalLayout();
        activeRequirementsLayout.addClassName("hub-active-requirements");
        activeRequirementsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        activeRequirementsLayout.setWrap(true);

        var reasoningSection = createReasoningSection();
        var uploadPanel = createUploadSection();
        uploadPanel.add(fileQueue);

        var topRow = new HorizontalLayout(uploadPanel, reasoningSection);
        topRow.addClassName("hub-top-row");
        topRow.setWidthFull();
        topRow.setSpacing(true);
        topRow.setAlignItems(FlexComponent.Alignment.STRETCH);
        topRow.setFlexGrow(1, reasoningSection);
        topRow.setFlexGrow(1, uploadPanel);
        add(topRow);

        add(createRequirementBar());

        var suggestionSection = createSuggestionSection();
        suggestionSection.setVisible(false);
        add(suggestionSection);

        layerTabs = new LayerTabs(this::selectLayer);
        add(layerTabs);

        classListPanel = new ClassListPanel(this::selectClass);
        detailPanel = new ClassDetailPanel(ontologyDataService, manualEntryFormFactory,
            this::onSupplementaryUpload,
            this::showError,
            this::onManualSave,
            this::onEntityEdit);
        detailPanel.setWidthFull();

        var body = new HorizontalLayout(classListPanel, detailPanel);
        body.addClassName("hub-body");
        body.setSizeFull();
        body.setSpacing(true);
        body.setAlignItems(FlexComponent.Alignment.STRETCH);
        body.setFlexGrow(1, detailPanel);
        add(body);
        setFlexGrow(1, body);

        dashboard = new VerticalLayout();
        dashboard.addClassName("hub-dashboard-panel");
        dashboard.setPadding(false);
        dashboard.setSpacing(true);
        add(dashboard);

        add(createActionBar());
        runInitialPipeline();
    }

    private VerticalLayout createHeader() {
        H2 title = new H2("Semantic Mapping Hub");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE, "section-title");
        title.getStyle().set("margin-top", "0");

        var header = new VerticalLayout(title);
        header.setPadding(false);
        header.setSpacing(false);
        return header;
    }

    private VerticalLayout createSuggestionSection() {
        H2 title = new H2("AI Concept Suggestions");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE, "section-title");

        var section = new VerticalLayout(title, suggestionPanel);
        section.addClassName("hub-suggestion-section");
        section.setPadding(true);
        section.setSpacing(true);
        return section;
    }

    private VerticalLayout createReasoningSection() {
        H2 title = new H2("Ontology Reasoning (Match)");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE, "section-title");

        var section = new VerticalLayout(title, reasoningTreePanel);
        section.addClassName("hub-reasoning-section");
        section.setPadding(true);
        section.setSpacing(true);
        return section;
    }

    private VerticalLayout createUploadSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassName("hub-upload-area");
        section.setPadding(false);
        section.setSpacing(true);

        List<Span> badgeSpans = new ArrayList<>();
        for (AssetFileFormat format : AssetFileFormat.values()) {
            badgeSpans.add(formatBadge(formatBadgeText(format), "hub-format--" + formatClass(format)));
        }
        HorizontalLayout badges = new HorizontalLayout(badgeSpans.toArray(new Span[0]));
        badges.setSpacing(true);

        UploadDropzone dropzone = new UploadDropzone(
            "Drop AAS .xml/.json, MTP .aml/.xml, OPC UA .json/NodeSet .xml, AutomationML .aml/.xml, "
                + "or a Requirement / ABox .ttl here",
            List.of(AssetFileFormat.values()),
            this::onUpload,
            this::showError);

        section.add(badges);
        section.add(dropzone.get());
        return section;
    }

    private static String formatBadgeText(AssetFileFormat format) {
        return switch (format) {
            case AAS_XML -> "AAS .xml";
            case AAS_JSON -> "AAS .json";
            case MTP_AML -> "MTP .aml/.xml";
            case OPCUA_JSON -> "OPC UA .json";
            case OPCUA_XML -> "OPC UA NodeSet .xml/.aml";
            case AUTOMATION_ML -> "AutomationML .aml/.xml";
            case REQUIREMENT_TTL -> "Requirement .ttl/.rdf";
            case ABOX_TTL -> "ABox .ttl/.rdf";
        };
    }

    private Span formatBadge(String label, String cssClass) {
        Span badge = new Span(label);
        badge.addClassName("hub-format-badge");
        badge.addClassName(cssClass);
        return badge;
    }

    private HorizontalLayout createActionBar() {
        Button runButton = new Button("Run Pipeline");
        runButton.addThemeName("primary");
        runButton.addClickListener(e -> {
            lastResult = hubService.reRunPipeline();
            refresh();
        });

        Button resetButton = new Button("Reset");
        resetButton.addClickListener(e -> {
            hubService.reset();
            selectedClassName = null;
            lastResult = hubService.reRunPipeline();
            refresh();
        });

        exportButton = new Button("Export Complete Manifest");
        exportButton.addThemeName("primary");
        exportButton.addClickListener(e -> exportManifest());

        exportReasoningButton = new Button("Export Reasoning Tree");
        exportReasoningButton.addClickListener(e -> exportReasoningTree());

        var actions = new HorizontalLayout(runButton, resetButton, exportButton, exportReasoningButton);
        actions.setSpacing(true);
        return actions;
    }

    private void runInitialPipeline() {
        lastResult = hubService.reRunPipeline();
        refresh();
    }

    private void onUpload(String fileName, byte[] content) {
        try {
            lastResult = hubService.processUpload(fileName, content);
            selectedClassName = null;
            refresh();
            showApplicationRequirementsLayer();
            Notification.show("Parsed " + fileName, 3000, Notification.Position.TOP_CENTER);
        } catch (ParsingException e) {
            showError(e.getMessage());
        } catch (MappingException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Brings the Application / Requirements Layer (L1) to the front after an
     * upload so the user can review and edit its fields before triggering the
     * pipeline.
     */
    private void showApplicationRequirementsLayer() {
        selectedLayerId = "L1";
        layerTabs.update(ontologyDataService.getLayers(), lastResult.gapReport(), selectedLayerId);
        selectedClassName = pickApplicationRequirementsClass();
        renderClassList();
        renderDetail();
    }

    private String pickApplicationRequirementsClass() {
        List<OntologyClass> l1 = ontologyDataService.getClassesByLayer("L1");
        return l1.stream().map(OntologyClass::name)
            .filter(name -> "UseCaseRequirement".equals(name))
            .findFirst()
            .orElseGet(() -> l1.isEmpty() ? null : l1.get(0).name());
    }

    /**
     * Applies an edited Application / Requirements Layer entity, re-runs the
     * pipeline with the reviewed data and refreshes the UI.
     */
    private void onEntityEdit(CanonicalEntity oldEntity, CanonicalEntity updated) {
        lastResult = hubService.updateEntity(updated);
        refresh();
        Notification.show("Updated " + updated.className() + " — pipeline re-run", 3000,
            Notification.Position.TOP_CENTER);
    }

    private void onSupplementaryUpload(String fileName, byte[] content) {
        try {
            lastResult = hubService.processUpload(fileName, content);
            refresh();
            Notification.show("Parsed " + fileName, 3000, Notification.Position.TOP_CENTER);
        } catch (ParsingException e) {
            showError(e.getMessage());
        }
    }

    private void onManualSave(CanonicalEntity entity) {
        lastResult = hubService.addManualEntity(entity);
        refresh();
        Notification.show("Added " + entity.className() + " manually", 3000,
            Notification.Position.TOP_CENTER);
    }

    /**
     * Accepts an AI-suggested concept from the suggestion panel, converts it to
     * a manual entity and re-runs the pipeline through the standard manual-entry
     * path (so the LLM output is a suggestion, never an override).
     *
     * @param hint the accepted concept hint
     */
    private void onConceptSuggestionAdd(ConceptHint hint) {
        try {
            lastResult = hubService.addConceptSuggestion(hint);
            refresh();
            Notification.show("Added " + (hint.label() != null ? hint.label() : hint.className())
                + " (AI suggestion)", 3000, Notification.Position.TOP_CENTER);
        } catch (MappingException e) {
            showError(e.getMessage());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    /**
     * Selects a layer tab and re-renders the class browser.
     *
     * @param layerId the layer id or {@code "ALL"}
     */
    public void selectLayer(String layerId) {
        selectedLayerId = layerId;
        layerTabs.update(ontologyDataService.getLayers(), lastResult.gapReport(), selectedLayerId);
        renderClassList();
    }

    /**
     * Selects a class and re-renders the class browser and detail panel.
     *
     * @param className the ontology class name
     */
    public void selectClass(String className) {
        selectedClassName = className;
        renderClassList();
        renderDetail();
    }

    private void refresh() {
        console.logLines(lastResult.consoleLines());
        reasoningTreePanel.show(lastResult.reasoningTrees());
        renderFileQueue();
        renderDashboard();
        renderActiveRequirements();
        layerTabs.update(ontologyDataService.getLayers(), lastResult.gapReport(), selectedLayerId);
        renderClassList();
        renderDetail();
        exportButton.setEnabled(true);
        exportReasoningButton.setEnabled(true);
        suggestionPanel.setSuggestEnabled(hubService.isLlmAvailable());
    }

    /**
     * Builds the requirement activation bar: a dropdown of catalogue
     * requirements the user can add to the pipeline, plus chips for the
     * requirements currently active.
     *
     * @return the requirement bar layout
     */
    private HorizontalLayout createRequirementBar() {
        Span label = new Span("Active requirements");
        label.addClassName("hub-section-label");

        Button addReq = new Button("Add");
        addReq.addThemeName("primary");
        addReq.addClickListener(e -> {
            String reqId = requirementCombo.getValue();
            if (reqId == null || reqId.isBlank()) {
                Notification.show("Select a requirement first", 2000,
                    Notification.Position.TOP_CENTER);
                return;
            }
            lastResult = hubService.addActiveRequirement(reqId);
            requirementCombo.clear();
            refresh();
        });

        var bar = new HorizontalLayout(label, requirementCombo, addReq, activeRequirementsLayout);
        bar.addClassName("hub-requirement-bar");
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.setSpacing(true);
        populateRequirementCombo();
        return bar;
    }

    private void populateRequirementCombo() {
        List<SemanticReasonerService.CatalogueRequirement> requirements =
            hubService.getAvailableRequirements();
        Map<String, String> idToLabel = requirements.stream().collect(Collectors.toMap(
            SemanticReasonerService.CatalogueRequirement::id,
            r -> r.label() + " (" + r.id() + ")",
            (a, b) -> a, LinkedHashMap::new));
        requirementCombo.setItems(idToLabel.keySet());
        requirementCombo.setItemLabelGenerator(id -> idToLabel.getOrDefault(id, id));
    }

    private void renderActiveRequirements() {
        activeRequirementsLayout.removeAll();
        for (MappingHubSession.ActiveRequirement active :
                hubService.getSession().getActiveRequirements()) {
            String reqId = active.id();
            Span chip = new Span(reqId + (active.capabilityType() != null
                ? " (" + active.capabilityType() + ")" : ""));
            chip.addClassName("hub-requirement-chip");
            Button remove = new Button("×");
            remove.addClassName("hub-requirement-remove");
            remove.addClickListener(e -> {
                lastResult = hubService.removeActiveRequirement(reqId);
                refresh();
            });
            var chipRow = new HorizontalLayout(chip, remove);
            chipRow.setAlignItems(FlexComponent.Alignment.CENTER);
            chipRow.setSpacing(false);
            activeRequirementsLayout.add(chipRow);
        }
    }

    private void renderClassList() {
        List<OntologyClass> classes = "ALL".equals(selectedLayerId)
            ? classesForAllLayers()
            : classesForLayer(selectedLayerId);
        classListPanel.update(classes, ontologyDataService, lastResult.gapReport(),
            selectedClassName, lastResult.model());
    }

    /**
     * Flattens the hierarchy for the All Layers tab: every numbered class in
     * diagram order followed by its derived subclasses.
     */
    private List<OntologyClass> classesForAllLayers() {
        List<OntologyClass> classes = new ArrayList<>();
        for (OntologyLayer layer : ontologyDataService.getLayers()) {
            classes.addAll(classesForLayer(layer.id()));
        }
        return classes;
    }

    /**
     * Flattens the hierarchy for one layer: the layer's numbered classes in
     * diagram order, each followed by its derived subclasses.
     */
    private List<OntologyClass> classesForLayer(String layerId) {
        List<OntologyClass> classes = new ArrayList<>();
        for (OntologyClass clazz : ontologyDataService.getClassesByLayer(layerId)) {
            classes.add(clazz);
            classes.addAll(ontologyDataService.getSubClassesOf(clazz.name()));
        }
        return classes;
    }

    private void renderDetail() {
        detailPanel.update(selectedClassName, lastResult.gapReport(), lastResult.model(),
            lastResult.inferred(), hubService.getSession().getFiles());
    }

    private void renderFileQueue() {
        fileQueue.removeAll();
        Span title = new Span("Uploaded files");
        title.addClassName("hub-section-label");
        fileQueue.add(title);
        List<UploadedFileInfo> files = hubService.getSession().getFiles();
        if (files.isEmpty()) {
            Span empty = new Span("No files uploaded");
            empty.addClassName("hub-recommendations-empty");
            fileQueue.add(empty);
            return;
        }
        for (int i = 0; i < files.size(); i++) {
            UploadedFileInfo file = files.get(i);
            HorizontalLayout row = new HorizontalLayout();
            row.addClassName("hub-file-row");
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            Span name = new Span(file.fileName());
            name.addClassName("hub-file-name");
            Span format = formatBadge(file.format().label(), "hub-format--" + formatClass(file.format()));
            Button remove = new Button("Remove");
            int index = i;
            remove.addClickListener(e -> {
                lastResult = hubService.removeUploadedFile(index);
                refresh();
            });
            row.add(name, format, remove);
            fileQueue.add(row);
        }
    }

    private void renderDashboard() {
        dashboard.removeAll();
        dashboard.add(progressBar());
    }

    private Div progressBar() {
        Div bar = new Div();
        bar.addClassName("hub-progress");
        addSegment(bar, "present", lastResult.gapReport().count(CoverageStatus.PRESENT));
        addSegment(bar, "inferred", lastResult.gapReport().count(CoverageStatus.INFERRED));
        addSegment(bar, "partial", lastResult.gapReport().count(CoverageStatus.PARTIAL));
        addSegment(bar, "missing", lastResult.gapReport().count(CoverageStatus.MISSING));
        return bar;
    }

    private void addSegment(Div bar, String status, int count) {
        Div segment = new Div();
        segment.addClassNames("hub-progress-segment", "hub-progress--" + status);
        segment.getStyle().set("flex-grow", String.valueOf(Math.max(count, 0)));
        bar.add(segment);
    }

    private static String formatClass(AssetFileFormat format) {
        return switch (format) {
            case AAS_XML, AAS_JSON -> "aas";
            case MTP_AML -> "mtp";
            case OPCUA_JSON, OPCUA_XML -> "opcua";
            case AUTOMATION_ML -> "aml";
            case REQUIREMENT_TTL -> "req";
            case ABOX_TTL -> "abox";
        };
    }

    private void exportManifest() {
        String json = hubService.buildManifest();
        UI.getCurrent().getPage().executeJs(
            "const a=document.createElement('a');"
                + "a.href='data:application/json;charset=utf-8,'+encodeURIComponent($0);"
                + "a.download='complete-manifest.json';"
                + "document.body.appendChild(a);a.click();document.body.removeChild(a);",
            json);
        Notification.show("Manifest exported", 3000, Notification.Position.TOP_CENTER);
    }

    private void exportReasoningTree() {
        String json = hubService.buildReasoningTreeJson();
        UI.getCurrent().getPage().executeJs(
            "const a=document.createElement('a');"
                + "a.href='data:application/json;charset=utf-8,'+encodeURIComponent($0);"
                + "a.download='reasoning-tree.json';"
                + "document.body.appendChild(a);a.click();document.body.removeChild(a);",
            json);
        Notification.show("Reasoning tree exported", 3000, Notification.Position.TOP_CENTER);
    }

    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_CENTER);
    }
}
