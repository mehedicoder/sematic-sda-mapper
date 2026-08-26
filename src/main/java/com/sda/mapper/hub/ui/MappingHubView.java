package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.error.MappingException;
import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.sda.mapper.hub.parser.ParsingException;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.PipelineResult;
import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyLayer;

import com.sda.mapper.service.OntologyDataService;
import com.sda.mapper.ui.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.sda.mapper.observability.PipelineEvent;
import com.sda.mapper.ui.SdaAppShell;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

/**
 * Universal semantic mapping hub: ingests AAS XML, MTP AML and OPC UA JSON
 * asset descriptions and runs Parse &rarr; Lift &rarr; Infer &rarr; Match
 * &rarr; Manifest with per-layer coverage, gap actions and context-aware
 * recommendations.
 *
 * <p>Embeds the live pipeline observability console ({@link StageTimeline},
 * {@link PipelineTracePanel}, {@link PipelineConsolePanel}); server push is
 * enabled application-wide via {@link SdaAppShell} so streamed audit events
 * render without manual refresh.</p>
 */
@Route(value = "mapping-hub", layout = MainLayout.class)
@PageTitle("Mapping Hub")
@CssImport("./hub-styles.css")
public class MappingHubView extends VerticalLayout {

    private final MappingHubService hubService;
    private final OntologyDataService ontologyDataService;

    private final ConsolePanel console;
    private final LayerTabs layerTabs;
    private final ClassListPanel classListPanel;
    private final ClassDetailPanel detailPanel;
    private final VerticalLayout fileQueue;
    private final VerticalLayout dashboard;
    private Button exportButton;

    private final StageTimeline stageTimeline = new StageTimeline();
    private final PipelineTracePanel tracePanel = new PipelineTracePanel();
    private final PipelineConsolePanel pipelineConsole = new PipelineConsolePanel();
    private Consumer<PipelineEvent> auditListener;

    private String selectedLayerId = "ALL";
    private String selectedClassName;
    private PipelineResult lastResult;

    /**
     * Creates the mapping hub view.
     *
     * @param hubService                the mapping hub pipeline service
     * @param ontologyDataService       the ontology data service
     * @param manualEntryFormFactory    the ontology-driven manual entry form factory
     */
    public MappingHubView(MappingHubService hubService, OntologyDataService ontologyDataService,
                            ManualEntryFormFactory manualEntryFormFactory) {
        this.hubService = hubService;
        this.ontologyDataService = ontologyDataService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("hub-root");
        getElement().setAttribute("theme", "dark");

        add(createHeader());

        console = new ConsolePanel();
        fileQueue = new VerticalLayout();
        fileQueue.addClassName("hub-file-queue");
        fileQueue.setPadding(false);
        fileQueue.setSpacing(false);

        var uploadPanel = createUploadSection();
        uploadPanel.add(fileQueue);

        var uploadAndConsole = new HorizontalLayout(uploadPanel, console);
        uploadAndConsole.addClassName("hub-upload-row");
        uploadAndConsole.setWidthFull();
        uploadAndConsole.setSpacing(true);
        uploadAndConsole.setAlignItems(FlexComponent.Alignment.STRETCH);
        add(uploadAndConsole);

        dashboard = new VerticalLayout();
        dashboard.addClassName("hub-dashboard-panel");
        dashboard.setPadding(false);
        dashboard.setSpacing(true);
        add(dashboard);

        add(createObservabilitySection());

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

    private VerticalLayout createObservabilitySection() {
        H2 title = new H2("Pipeline Observability");
        title.addClassNames(LumoUtility.Margin.Bottom.NONE, "section-title");

        var timelineRow = new HorizontalLayout(stageTimeline);
        timelineRow.setSpacing(true);

        var traceAndConsole = new HorizontalLayout(tracePanel, pipelineConsole);
        traceAndConsole.setWidthFull();
        traceAndConsole.setSpacing(true);
        traceAndConsole.setAlignItems(FlexComponent.Alignment.STRETCH);

        var section = new VerticalLayout(title, timelineRow, traceAndConsole);
        section.addClassName("hub-observability-panel");
        section.setPadding(true);
        section.setSpacing(true);
        return section;
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        auditListener = event -> getUI().ifPresent(ui -> ui.access(() -> {
            stageTimeline.onEvent(event);
            tracePanel.onEvent(event);
            pipelineConsole.onEvent(event);
        }));
        hubService.getAuditPublisher().addListener(auditListener);
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        if (auditListener != null) {
            hubService.getAuditPublisher().removeListener(auditListener);
            auditListener = null;
        }
        super.onDetach(detachEvent);
    }

    private VerticalLayout createUploadSection() {
        VerticalLayout section = new VerticalLayout();
        section.addClassName("hub-upload-area");
        section.setPadding(false);
        section.setSpacing(true);

        HorizontalLayout badges = new HorizontalLayout(
            formatBadge("AAS .xml", "hub-format--aas"),
            formatBadge("MTP .aml/.xml", "hub-format--mtp"),
            formatBadge("OPC UA .json", "hub-format--opcua")
        );
        badges.setSpacing(true);

        UploadDropzone dropzone = new UploadDropzone(
            "Drop AAS .xml, MTP .aml/.xml, AutomationML .aml/.xml, or OPC UA .json here",
            List.of(AssetFileFormat.values()),
            this::onUpload,
            this::showError);

        section.add(badges);
        section.add(dropzone.get());
        return section;
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

        var actions = new HorizontalLayout(runButton, resetButton, exportButton);
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
        renderFileQueue();
        renderDashboard();
        layerTabs.update(ontologyDataService.getLayers(), lastResult.gapReport(), selectedLayerId);
        renderClassList();
        renderDetail();
        exportButton.setEnabled(true);
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
        if (hubService.getSession().getFiles().isEmpty()) {
            Span empty = new Span("No files uploaded");
            empty.addClassName("hub-recommendations-empty");
            fileQueue.add(empty);
            return;
        }
        for (UploadedFileInfo file : hubService.getSession().getFiles()) {
            HorizontalLayout row = new HorizontalLayout();
            row.addClassName("hub-file-row");
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            Span name = new Span(file.fileName());
            name.addClassName("hub-file-name");
            Span format = formatBadge(file.format().label(), "hub-format--" + formatClass(file.format()));
            Button remove = new Button("Remove");
            remove.setEnabled(false);
            remove.setTooltipText("Reset clears the current session file queue");
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
            case OPCUA_JSON -> "opcua";
            case AUTOMATION_ML -> "aml";
        };
    }

    private void exportManifest() {
        String json = hubService.buildManifest();
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        UI.getCurrent().getPage().openUnsafe("data:application/json;base64," + base64, "_blank");
        Notification.show("Manifest exported", 3000, Notification.Position.TOP_CENTER);
    }

    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_CENTER);
    }
}
