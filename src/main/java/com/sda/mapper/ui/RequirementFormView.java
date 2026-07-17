package com.sda.mapper.ui;

import com.sda.mapper.api.OptionsConfigService;
import com.sda.mapper.api.dto.MatchRequirementRequest;
import com.sda.mapper.api.dto.MatchRequirementResponse;
import com.sda.mapper.deployment.DockerComposeManifestService;
import com.sda.mapper.deployment.KubernetesManifestService;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.RVerifyService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Requirement form view for creating and matching requirements.
 * Provides inline field matching (Mode B) and requirement persistence.
 */
@Route(value = "requirements", layout = MainLayout.class)
@PageTitle("Requirement Form")
public class RequirementFormView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(RequirementFormView.class);
    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final OptionsConfigService optionsConfigService;
    private final SemanticReasonerService reasonerService;
    private final OntologyModelLoader ontologyModelLoader;
    private final DockerComposeManifestService dockerComposeService;
    private final KubernetesManifestService kubernetesService;
    private final RDeployService rDeployService;
    private final RVerifyService rVerifyService;

    private final TextField titleField = new TextField("Title");
    private final TextArea descriptionField = new TextArea("Description");
    private final TextField functionClassField = new TextField("Function Class");
    private final ComboBox<String> protocolCombo = new ComboBox<>("Protocol");
    private final ComboBox<String> accessModeCombo = new ComboBox<>("Access Mode");
    private final TextField engineeringUnitField = new TextField("Engineering Unit");
    private final ComboBox<String> networkZoneCombo = new ComboBox<>("Network Zone");
    private final ComboBox<String> determinismCombo = new ComboBox<>("Determinism Class");
    private final ComboBox<String> safetyIntegrityCombo = new ComboBox<>("Safety Integrity Level");
    private final NumberField nominalRangeMinField = new NumberField("Nominal Range Min");
    private final NumberField nominalRangeMaxField = new NumberField("Nominal Range Max");
    private final NumberField minRecallField = new NumberField("Min Recall");
    private final NumberField maxLatencyField = new NumberField("Max Latency (ms)");
    private final VerticalLayout resultsPanel = new VerticalLayout();
    private String lastMatchedRequirementId = null;

    public RequirementFormView(OptionsConfigService optionsConfigService,
                               SemanticReasonerService reasonerService,
                               OntologyModelLoader ontologyModelLoader,
                               DockerComposeManifestService dockerComposeService,
                               KubernetesManifestService kubernetesService,
                               RDeployService rDeployService,
                               RVerifyService rVerifyService) {
        this.optionsConfigService = optionsConfigService;
        this.reasonerService = reasonerService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.dockerComposeService = dockerComposeService;
        this.kubernetesService = kubernetesService;
        this.rDeployService = rDeployService;
        this.rVerifyService = rVerifyService;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Requirement Form"));
        add(new Paragraph("Create new requirements and match against available services."));

        loadComboOptions();

        var formLayout = new FormLayout();
        formLayout.add(titleField, descriptionField, functionClassField,
            protocolCombo, accessModeCombo, engineeringUnitField,
            networkZoneCombo, determinismCombo, safetyIntegrityCombo,
            nominalRangeMinField, nominalRangeMaxField, minRecallField, maxLatencyField);
        formLayout.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("400px", 2),
            new FormLayout.ResponsiveStep("800px", 3));
        add(formLayout);

        var matchButton = new Button("Match", e -> matchRequirement());
        matchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var saveButton = new Button("Save Requirement", e -> saveRequirement());
        saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        add(matchButton, saveButton);

        resultsPanel.setWidthFull();
        resultsPanel.setVisible(false);
        add(new H3("Results"));
        add(resultsPanel);
    }

    private void matchRequirement() {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("protocol", protocolCombo.getValue() != null ? protocolCombo.getValue() : "OPC-UA");
            params.put("accessMode", accessModeCombo.getValue() != null ? accessModeCombo.getValue() : "read-only");
            if (minRecallField.getValue() != null) {
                params.put("minRecall", String.valueOf(minRecallField.getValue()));
            }
            if (maxLatencyField.getValue() != null) {
                params.put("maxLatency", String.valueOf(maxLatencyField.getValue().intValue()));
            }

            ServiceMatchResult result = reasonerService.matchInline(params);

            resultsPanel.removeAll();
            resultsPanel.setVisible(true);

            if (result.matched()) {
                lastMatchedRequirementId = result.requirementId();

                String dockerComposeYaml = dockerComposeService.generate(result);
                String kubernetesYaml = kubernetesService.generate(result);

                var lifecycleSpan = new com.vaadin.flow.component.html.Span(
                    result.lifecycleStatus() != null ? result.lifecycleStatus() : "Matched");
                lifecycleSpan.getElement().getThemeList().add("badge success");

                resultsPanel.add(new Paragraph("Lifecycle Status: "),
                    lifecycleSpan);
                resultsPanel.add(new Paragraph("Matched service: "
                    + result.serviceName()));
                if (result.bindingLabel() != null) {
                    resultsPanel.add(new Paragraph("Binding: "
                        + result.bindingLabel()));
                }
                resultsPanel.add(new Paragraph("Capability: "
                    + result.capabilityName()));
                resultsPanel.add(new Paragraph("Recall: "
                    + result.recall()));
                resultsPanel.add(new Paragraph("Latency: "
                    + result.latencyMs() + "ms"));
                resultsPanel.add(new Paragraph("Explanation: "
                    + result.explanation()));

                var verifyButton = new Button("Verify", e -> verifyRequirement());
                verifyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                verifyButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
                resultsPanel.add(verifyButton);
            } else {
                resultsPanel.add(new Paragraph("No matching service found."));
                resultsPanel.add(new Paragraph(result.explanation()));
            }
        } catch (Exception ex) {
            log.error("Match error", ex);
            Notification.show("Match error: " + ex.getMessage(), 5000,
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void verifyRequirement() {
        if (lastMatchedRequirementId == null) {
            Notification.show("No matched requirement to verify", 3000,
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        try {
            RVerifyService.VerifyResult result = rVerifyService.execute(lastMatchedRequirementId);

            var statusSpan = new com.vaadin.flow.component.html.Span(
                result.verified() ? "Verified" : "Not Verified");
            statusSpan.getElement().getThemeList().add(
                result.verified() ? "badge success" : "badge error");

            resultsPanel.add(new Paragraph("Verification: "), statusSpan);
            resultsPanel.add(new Paragraph("Summary: "
                + result.summary()));

            for (var criterion : result.criteria()) {
                var crSpan = new com.vaadin.flow.component.html.Span(
                    criterion.name() + ": " + (criterion.passed() ? "PASS" : "FAIL"));
                crSpan.getElement().getThemeList().add(
                    criterion.passed() ? "badge success" : "badge error");
                resultsPanel.add(crSpan);
                resultsPanel.add(new Paragraph(criterion.detail()));
            }
        } catch (Exception ex) {
            log.error("Verify error", ex);
            Notification.show("Verify error: " + ex.getMessage(), 5000,
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void saveRequirement() {
        try {
            String requirementId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Model model = ontologyModelLoader.getModel();
            String resourceUri = INST_NS + requirementId;

            Resource requirement = model.createResource(resourceUri);
            requirement.addProperty(
                model.createProperty(RDF_TYPE),
                model.createResource(SDA_NS + "Requirement"));

            addPropertyIfPresent(model, requirement, "title", titleField.getValue());
            addPropertyIfPresent(model, requirement, "description", descriptionField.getValue());
            addPropertyIfPresent(model, requirement, "functionClass", functionClassField.getValue());
            addPropertyIfPresent(model, requirement, "protocol", protocolCombo.getValue());
            addPropertyIfPresent(model, requirement, "accessMode", accessModeCombo.getValue());
            addPropertyIfPresent(model, requirement, "engineeringUnit", engineeringUnitField.getValue());
            addPropertyIfPresent(model, requirement, "networkZone", networkZoneCombo.getValue());
            addPropertyIfPresent(model, requirement, "determinismClass", determinismCombo.getValue());
            addPropertyIfPresent(model, requirement, "safetyIntegrityLevel", safetyIntegrityCombo.getValue());
            if (nominalRangeMinField.getValue() != null) {
                addPropertyIfPresent(model, requirement, "nominalRangeMin",
                    String.valueOf(nominalRangeMinField.getValue()));
            }
            if (nominalRangeMaxField.getValue() != null) {
                addPropertyIfPresent(model, requirement, "nominalRangeMax",
                    String.valueOf(nominalRangeMaxField.getValue()));
            }

            log.info("Persisted requirement {} to ontology model", requirementId);
            Notification.show("Requirement saved: " + requirementId, 3000,
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            log.error("Save error", ex);
            Notification.show("Save error: " + ex.getMessage(), 5000,
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void addPropertyIfPresent(Model model, Resource resource,
                                       String propertyName, String value) {
        if (value != null && !value.isBlank()) {
            resource.addProperty(model.createProperty(SDA_NS + propertyName), value);
        }
    }

    private void loadComboOptions() {
        loadOptionsIntoCombo(protocolCombo, "protocol");
        loadOptionsIntoCombo(accessModeCombo, "accessMode");
        loadOptionsIntoCombo(networkZoneCombo, "networkZone");
        loadOptionsIntoCombo(determinismCombo, "determinismClass");
        loadOptionsIntoCombo(safetyIntegrityCombo, "safetyIntegrityLevel");
    }

    private void loadOptionsIntoCombo(ComboBox<String> combo, String fieldName) {
        try {
            var options = optionsConfigService.getOptions(fieldName);
            combo.setItems(options.stream().map(o -> o.value()).toList());
        } catch (Exception ignored) {
            // Options may not be available in all environments
        }
    }
}
