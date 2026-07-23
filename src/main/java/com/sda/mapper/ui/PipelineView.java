package com.sda.mapper.ui;

import com.sda.mapper.deployment.DockerComposeManifestService;
import com.sda.mapper.deployment.KubernetesManifestService;
import com.sda.mapper.reasoning.EndToEndPipelineService;
import com.sda.mapper.reasoning.EndToEndPipelineService.PipelineResult;
import com.sda.mapper.reasoning.RequirementDiscoveryService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.sda.mapper.error.ParserException;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vaadin view for executing the full end-to-end semantic mapping pipeline.
 * Users upload an input file (MTP AML, OPC UA Server Datasheet, or AAS),
 * optionally select which requirements to match, and view per-requirement
 * results including generated Docker Compose and Kubernetes manifests.
 */
@Route(value = "pipeline", layout = MainLayout.class)
@PageTitle("Semantic Reasoner")
public class PipelineView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(PipelineView.class);

    private final EndToEndPipelineService pipelineService;
    private final DockerComposeManifestService dockerComposeService;
    private final KubernetesManifestService kubernetesService;
    private final RequirementDiscoveryService requirementDiscoveryService;

    private final MemoryBuffer buffer;
    private final Upload upload;
    private final CheckboxGroup<String> requirementFilter;
    private final Checkbox persistToOntology;
    private final VerticalLayout resultsLayout;

    // New requirement form fields
    private final TextField newReqTitle = new TextField("Title");
    private final TextArea newReqDescription = new TextArea("Description");
    private final ComboBox<String> newReqProtocol = new ComboBox<>("Protocol");
    private final ComboBox<String> newReqAccessMode = new ComboBox<>("Access Mode");
    private final NumberField newReqMinRecall = new NumberField("Min Recall");
    private final NumberField newReqMaxLatency = new NumberField("Max Latency (ms)");
    private final ComboBox<String> newReqNetworkZone = new ComboBox<>("Network Zone");
    private final ComboBox<String> newReqDeterminism = new ComboBox<>("Determinism Class");
    private final ComboBox<String> newReqSil = new ComboBox<>("Safety Integrity Level");

    public PipelineView(EndToEndPipelineService pipelineService,
                        DockerComposeManifestService dockerComposeService,
                        KubernetesManifestService kubernetesService,
                        RequirementDiscoveryService requirementDiscoveryService) {
        this.pipelineService = pipelineService;
        this.dockerComposeService = dockerComposeService;
        this.kubernetesService = kubernetesService;
        this.requirementDiscoveryService = requirementDiscoveryService;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Semantic Reasoner"));
        add(new Paragraph("Upload an input file (MTP AML .xml/.aml, OPC UA Server Datasheet .json, or AAS .xml) "
            + "to execute the full semantic mapping pipeline: Parse → Lift → Infer → Match → Manifest."));

        buffer = new MemoryBuffer();
        upload = new Upload(buffer);
        upload.setWidthFull();
        upload.setAcceptedFileTypes(
            "application/xml", "text/xml",
            "application/json",
            "application/rdf+xml"
        );
        upload.setMaxFiles(1);

        requirementFilter = new CheckboxGroup<>();
        requirementFilter.setLabel("Requirements to match");
        List<String> discovered = requirementDiscoveryService.discoverRequirementIds();
        requirementFilter.setItems(discovered);
        requirementFilter.setValue(Set.copyOf(discovered));
        requirementFilter.setWidthFull();

        persistToOntology = new Checkbox("Persist parsed triples to ontology (Service Catalog)");
        persistToOntology.setWidthFull();

        upload.addSucceededListener(event ->
            handleUpload(event.getFileName(), buffer.getInputStream()));
        upload.addFileRemovedListener(event ->
            log.info("Upload cancelled: {}", event.getFileName()));

        add(upload);
        add(requirementFilter);
        add(persistToOntology);
        add(buildNewRequirementSection());
        add(new Hr());

        resultsLayout = new VerticalLayout();
        resultsLayout.setWidthFull();
        resultsLayout.setVisible(false);
        add(resultsLayout);
    }

    private Details buildNewRequirementSection() {
        newReqProtocol.setItems("OPC-UA", "PROFINET", "BACnet", "HTTP", "MQTT");
        newReqProtocol.setPlaceholder("Select protocol");
        newReqAccessMode.setItems("read-only", "read-write", "write-only");
        newReqAccessMode.setPlaceholder("Select access mode");
        newReqNetworkZone.setItems("OT-Zone-1", "OT-Zone-2", "OT-Zone-3");
        newReqNetworkZone.setPlaceholder("Select zone");
        newReqDeterminism.setItems("HardRealTime", "SoftRealTime", "NonRealTime");
        newReqDeterminism.setPlaceholder("Select determinism");
        newReqSil.setItems("SIL-0", "SIL-1", "SIL-2", "SIL-3", "SIL-4");
        newReqSil.setPlaceholder("Select SIL");

        newReqMinRecall.setValue(0.90);
        newReqMaxLatency.setValue(60.0);

        FormLayout form = new FormLayout();
        form.add(newReqTitle, newReqDescription, newReqProtocol, newReqAccessMode,
            newReqMinRecall, newReqMaxLatency, newReqNetworkZone, newReqDeterminism, newReqSil);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("400px", 2),
            new FormLayout.ResponsiveStep("800px", 3));

        Details details = new Details("Create New Requirement (optional)", form);
        details.setOpened(false);
        return details;
    }

    private Map<String, String> collectNewRequirementParams() {
        Map<String, String> params = new HashMap<>();
        if (newReqTitle.getValue() != null && !newReqTitle.getValue().isBlank()) {
            params.put("title", newReqTitle.getValue());
        }
        if (newReqDescription.getValue() != null && !newReqDescription.getValue().isBlank()) {
            params.put("description", newReqDescription.getValue());
        }
        if (newReqProtocol.getValue() != null) {
            params.put("protocol", newReqProtocol.getValue());
        }
        if (newReqAccessMode.getValue() != null) {
            params.put("accessMode", newReqAccessMode.getValue());
        }
        if (newReqMinRecall.getValue() != null) {
            params.put("minRecall", String.valueOf(newReqMinRecall.getValue()));
        }
        if (newReqMaxLatency.getValue() != null) {
            params.put("maxLatency", String.valueOf(newReqMaxLatency.getValue().intValue()));
        }
        if (newReqNetworkZone.getValue() != null) {
            params.put("networkZone", newReqNetworkZone.getValue());
        }
        if (newReqDeterminism.getValue() != null) {
            params.put("determinismClass", newReqDeterminism.getValue());
        }
        if (newReqSil.getValue() != null) {
            params.put("safetyIntegrityLevel", newReqSil.getValue());
        }
        return params;
    }

    private void handleUpload(String filename, InputStream inputStream) {
        try {
            List<String> selectedReqs = List.copyOf(requirementFilter.getValue());
            if (selectedReqs.isEmpty()) {
                selectedReqs = requirementDiscoveryService.discoverRequirementIds();
            }

            Map<String, String> newReqParams = collectNewRequirementParams();
            if (newReqParams.containsKey("title") || newReqParams.containsKey("protocol")) {
                log.info("Creating new requirement from form: {}", newReqParams);
            }

            PipelineResult result = pipelineService.execute(filename, inputStream, selectedReqs, newReqParams,
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
        panel.add(createSemanticChain(reqId, match));
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

    private VerticalLayout createSemanticChain(String reqId, ServiceMatchResult match) {
        VerticalLayout chain = new VerticalLayout();
        chain.setWidthFull();
        chain.setSpacing(false);
        chain.addClassName("semantic-chain");

        H3 chainHeader = new H3("Semantic Link Chain");
        chainHeader.addClassNames(LumoUtility.FontSize.SMALL);
        chain.add(chainHeader);

        HorizontalLayout links = new HorizontalLayout();
        links.setWidthFull();
        links.setSpacing(true);
        links.setWrap(true);
        links.setAlignItems(FlexComponent.Alignment.CENTER);

        links.add(createChainNode(reqId, "Requirement"));
        links.add(createChainArrow("mapsToCapability"));
        links.add(createChainNode(match.capabilityName(), "Capability"));
        links.add(createChainArrow("realisedBy"));
        links.add(createChainNode(match.skillName(), "Skill"));
        links.add(createChainArrow("isImplementedBy"));
        links.add(createChainNode(match.serviceName(), "Service"));
        links.add(createChainArrow("usesBinding"));
        links.add(createChainNode(match.bindingLabel(), "CommunicationBinding"));
        links.add(createChainArrow("usesConnector"));
        links.add(createChainNode(match.connector(), "DataConnector"));
        links.add(createChainArrow("isDeployedBy"));
        links.add(createChainNode(match.deploymentContext(), "DeploymentContext"));

        chain.add(links);
        return chain;
    }

    private VerticalLayout createChainNode(String label, String type) {
        VerticalLayout node = new VerticalLayout();
        node.setWidth(null);
        node.setSpacing(false);
        node.setPadding(false);
        node.addClassName("chain-node");

        Span nameSpan = new Span(label != null ? label : "N/A");
        nameSpan.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.SMALL);

        Span typeSpan = new Span(type);
        typeSpan.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

        node.add(nameSpan, typeSpan);
        return node;
    }

    private Span createChainArrow(String property) {
        Span arrow = new Span(" → " + property + " → ");
        arrow.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.TERTIARY);
        arrow.addClassName("chain-arrow");
        return arrow;
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
