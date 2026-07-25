package com.sda.mapper.ui;

import com.sda.mapper.deployment.DockerComposeManifestService;
import com.sda.mapper.deployment.KubernetesManifestService;
import com.sda.mapper.reasoning.EndToEndPipelineService;
import com.sda.mapper.reasoning.EndToEndPipelineService.PipelineResult;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.sda.mapper.error.ParserException;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Vaadin view for executing the full end-to-end semantic mapping pipeline.
 * Users upload an input file (MTP AML, OPC UA Server Datasheet, or AAS),
 * specify requirement parameters manually, and view the match result
 * including generated Docker Compose and Kubernetes manifests.
 */
@Route(value = "pipeline", layout = MainLayout.class)
@PageTitle("Semantic Reasoner")
public class PipelineView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(PipelineView.class);

    private final EndToEndPipelineService pipelineService;
    private final DockerComposeManifestService dockerComposeService;
    private final KubernetesManifestService kubernetesService;

    private final MemoryBuffer buffer;
    private final Upload upload;
    private final Checkbox persistToOntology;
    private final VerticalLayout resultsLayout;

    private final ComboBox<String> protocolField;
    private final ComboBox<String> accessModeField;
    private final NumberField minRecallField;
    private final NumberField maxLatencyField;
    private final Checkbox onPlantNetworkField;
    private final ComboBox<String> silField;
    private final ComboBox<String> determinismField;
    private final ComboBox<String> networkZoneField;

    public PipelineView(EndToEndPipelineService pipelineService,
                        DockerComposeManifestService dockerComposeService,
                        KubernetesManifestService kubernetesService) {
        this.pipelineService = pipelineService;
        this.dockerComposeService = dockerComposeService;
        this.kubernetesService = kubernetesService;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Semantic Reasoner"));
        add(new Paragraph("Upload an input file (MTP AML .xml/.aml, OPC UA Server Datasheet .json, or AAS .xml) "
            + "and specify requirement parameters to execute the semantic mapping pipeline."));

        buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        upload.setWidthFull();
        upload.setAcceptedFileTypes(
            "application/xml", "text/xml",
            "application/json",
            "application/rdf+xml"
        );
        upload.setMaxFiles(1);

        persistToOntology = new Checkbox("Persist parsed triples to ontology (Service Catalog)");
        persistToOntology.setWidthFull();

        protocolField = new ComboBox<>("Protocol");
        protocolField.setItems("OPC-UA", "PROFINET", "BACnet", "HTTP", "MQTT");
        protocolField.setValue("OPC-UA");

        accessModeField = new ComboBox<>("Access Mode");
        accessModeField.setItems("read-only", "read-write", "write-only");
        accessModeField.setValue("read-only");

        minRecallField = new NumberField("Min Recall");
        minRecallField.setValue(0.90);

        maxLatencyField = new NumberField("Max Latency (ms)");
        maxLatencyField.setValue(60.0);

        onPlantNetworkField = new Checkbox("On Plant Network", true);

        silField = new ComboBox<>("Safety Integrity Level");
        silField.setItems("SIL-0", "SIL-1", "SIL-2", "SIL-3", "SIL-4");
        silField.setPlaceholder("Optional");

        determinismField = new ComboBox<>("Determinism Class");
        determinismField.setItems("HardRealTime", "SoftRealTime", "NonRealTime");
        determinismField.setPlaceholder("Optional");

        networkZoneField = new ComboBox<>("Network Zone");
        networkZoneField.setItems("OT-Zone-1", "OT-Zone-2", "OT-Zone-3");
        networkZoneField.setPlaceholder("Optional");

        FormLayout reqForm = new FormLayout();
        reqForm.add(protocolField, accessModeField, minRecallField, maxLatencyField,
            onPlantNetworkField, silField, determinismField, networkZoneField);
        reqForm.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("400px", 2),
            new FormLayout.ResponsiveStep("800px", 4));

        Details requirementDetails = new Details("Requirement Parameters", reqForm);
        requirementDetails.setOpened(true);

        upload.addSucceededListener(event ->
            handleUpload(event.getFileName(), buffer.getInputStream()));
        upload.addFileRemovedListener(event ->
            log.info("Upload cancelled: {}", event.getFileName()));

        add(upload);
        add(requirementDetails);
        add(persistToOntology);
        add(new Hr());

        resultsLayout = new VerticalLayout();
        resultsLayout.setWidthFull();
        resultsLayout.setVisible(false);
        add(resultsLayout);
    }

    private Map<String, String> collectRequirementParams() {
        Map<String, String> params = new HashMap<>();
        if (protocolField.getValue() != null) params.put("protocol", protocolField.getValue());
        if (accessModeField.getValue() != null) params.put("accessMode", accessModeField.getValue());
        if (minRecallField.getValue() != null) params.put("minRecall", String.valueOf(minRecallField.getValue()));
        if (maxLatencyField.getValue() != null) params.put("maxLatency", String.valueOf(maxLatencyField.getValue().intValue()));
        params.put("onPlantNetwork", String.valueOf(Boolean.TRUE.equals(onPlantNetworkField.getValue())));
        if (silField.getValue() != null) params.put("safetyIntegrityLevel", silField.getValue());
        if (determinismField.getValue() != null) params.put("determinismClass", determinismField.getValue());
        if (networkZoneField.getValue() != null) params.put("networkZone", networkZoneField.getValue());
        return params;
    }

    private void handleUpload(String filename, InputStream inputStream) {
        try {
            Map<String, String> reqParams = collectRequirementParams();
            log.info("Pipeline match with params: {}", reqParams);

            PipelineResult result = pipelineService.execute(filename, inputStream, reqParams,
                Boolean.TRUE.equals(persistToOntology.getValue()));
            showResults(filename, result);
        } catch (ParserException e) {
            log.warn("Pipeline parse failed for {}: {}", filename, e.getMessage());
            showError(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected pipeline error for {}: {}", filename, e.getMessage(), e);
            showError("Unexpected error: " + e.getMessage());
        }
    }

    private void showResults(String filename, PipelineResult result) {
        resultsLayout.removeAll();
        resultsLayout.setVisible(true);

        resultsLayout.add(new H3("Pipeline Results — " + filename));

        Div stats = new Div();
        stats.setWidthFull();
        stats.add(
            new Span("File type: " + result.fileType()),
            new Span("Parsed triples: " + result.parsedTripleCount()),
            new Span("Lifted triples: " + result.liftedTripleCount()),
            new Span("Inferred triples: " + result.inferredTripleCount()),
            new Span("Elapsed: " + result.elapsedMs() + " ms")
        );
        stats.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "4px");
        resultsLayout.add(stats);

        if (result.persisted()) {
            Span persistedBadge = new Span("Persisted to ontology catalog");
            persistedBadge.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.SUCCESS);
            resultsLayout.add(persistedBadge);
        }

        if (!result.warnings().isEmpty()) {
            resultsLayout.add(new H3("Warnings"));
            result.warnings().forEach(w -> resultsLayout.add(new Text("⚠ " + w)));
        }

        resultsLayout.add(new Hr());

        for (Map.Entry<String, ServiceMatchResult> entry : result.matches().entrySet()) {
            String reqId = entry.getKey();
            ServiceMatchResult matchResult = entry.getValue();

            if (matchResult.matched()) {
                String dockerComposeYaml = dockerComposeService.generate(matchResult);
                String kubernetesYaml = kubernetesService.generate(matchResult);
                resultsLayout.add(createMatchPanel(reqId, matchResult, dockerComposeYaml, kubernetesYaml));
            } else {
                resultsLayout.add(createNoMatchPanel(reqId, matchResult));
            }
        }

        Notification.show("Pipeline completed: " + result.matches().size() + " requirements evaluated",
            3000, Notification.Position.BOTTOM_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private VerticalLayout createMatchPanel(String reqId, ServiceMatchResult match,
                                             String dockerComposeYaml, String kubernetesYaml) {
        VerticalLayout panel = new VerticalLayout();
        panel.setWidthFull();
        panel.setSpacing(true);
        panel.addClassName("pipeline-match-panel");

        H3 header = new H3(reqId + " — Matched: " + match.serviceName());
        header.addClassNames("pipeline-match-header");

        panel.add(header);
        panel.add(createMatchDetails(match));

        TextArea explanationArea = new TextArea("Explanation");
        explanationArea.setValue(match.explanation());
        explanationArea.setWidthFull();
        explanationArea.setReadOnly(true);
        explanationArea.setHeight("120px");

        Tabs yamlTabs = new Tabs();
        Tab dockerTab = new Tab("Docker Compose");
        Tab k8sTab = new Tab("Kubernetes");
        yamlTabs.add(dockerTab, k8sTab);

        TextArea dockerArea = createYamlArea(dockerComposeYaml);
        TextArea k8sArea = createYamlArea(kubernetesYaml);
        k8sArea.setVisible(false);

        yamlTabs.addSelectedChangeListener(event -> {
            boolean dockerSelected = event.getSelectedTab() == dockerTab;
            dockerArea.setVisible(dockerSelected);
            k8sArea.setVisible(!dockerSelected);
        });

        panel.add(explanationArea, yamlTabs, dockerArea, k8sArea);
        return panel;
    }

    private HorizontalLayout createMatchDetails(ServiceMatchResult match) {
        Div details = new Div();
        details.setWidthFull();
        details.getStyle()
            .set("display", "flex").set("flex-wrap", "wrap").set("gap", "4px 12px");
        details.add(new Span("Protocol: " + match.protocol()));
        details.add(new Span("Access Mode: " + match.accessMode()));
        details.add(new Span("Recall: " + match.recall()));
        details.add(new Span("Latency: " + match.latencyMs() + " ms"));
        if (match.safetyIntegrityLevel() != null) {
            details.add(new Span("SIL: " + match.safetyIntegrityLevel()));
        }
        if (match.determinismClass() != null) {
            details.add(new Span("Determinism: " + match.determinismClass()));
        }
        if (match.networkZone() != null) {
            details.add(new Span("Zone: " + match.networkZone()));
        }
        HorizontalLayout wrapper = new HorizontalLayout(details);
        wrapper.setWidthFull();
        return wrapper;
    }

    private VerticalLayout createNoMatchPanel(String reqId, ServiceMatchResult match) {
        VerticalLayout panel = new VerticalLayout();
        panel.setWidthFull();
        panel.setSpacing(true);
        panel.addClassName("pipeline-no-match-panel");

        H3 header = new H3(reqId + " — No Match");
        header.addClassNames("pipeline-no-match-header");

        TextArea explanationArea = new TextArea("Explanation");
        explanationArea.setValue(match.explanation());
        explanationArea.setWidthFull();
        explanationArea.setReadOnly(true);
        explanationArea.setHeight("100px");

        panel.add(header, explanationArea);
        return panel;
    }

    private TextArea createYamlArea(String yaml) {
        TextArea area = new TextArea();
        area.setValue(yaml);
        area.setWidthFull();
        area.setReadOnly(true);
        area.setHeight("400px");
        area.addClassName("yaml-display");
        return area;
    }

    private void showError(String message) {
        resultsLayout.removeAll();
        resultsLayout.setVisible(true);

        resultsLayout.add(new H3("Pipeline Error"));
        resultsLayout.add(new Text(message));

        Notification.show("Pipeline failed: " + message,
            5000, Notification.Position.BOTTOM_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
