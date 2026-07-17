package com.sda.mapper.ui;

import com.sda.mapper.api.OptionsConfigService;
import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.RDeployService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Multi-step registration wizard for creating new CSS-SDA requirements.
 * Steps: General Info, Communication, Capabilities, Skills, Deployment,
 *        Semantic Mapping, Review, Register.
 */
@Route(value = "register", layout = MainLayout.class)
@PageTitle("Registration Wizard")
public class RegistrationWizardView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(RegistrationWizardView.class);
    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final OptionsConfigService optionsConfigService;
    private final OntologyModelLoader ontologyModelLoader;
    private final RDeployService rDeployService;

    private int currentStep = 0;
    private final List<VerticalLayout> steps = new ArrayList<>();
    private final Span stepIndicator = new Span();
    private final Button prevButton = new Button("Previous");
    private final Button nextButton = new Button("Next");
    private final Button saveDraftButton = new Button("Save Draft");
    private final Button cancelButton = new Button("Cancel");

    // Step 0 – General Info
    private final TextField titleField = new TextField("Title");
    private final TextArea descriptionField = new TextArea("Description");
    private final ComboBox<String> requirementTypeCombo = new ComboBox<>("Requirement Type");

    // Step 1 – Communication
    private final ComboBox<String> protocolCombo = new ComboBox<>("Protocol");
    private final ComboBox<String> accessModeCombo = new ComboBox<>("Access Mode");
    private final ComboBox<String> networkZoneCombo = new ComboBox<>("Network Zone");

    // Step 2 – Capabilities
    private final ComboBox<String> functionClassCombo = new ComboBox<>("Function Class");
    private final ComboBox<String> engineeringUnitCombo = new ComboBox<>("Engineering Unit");

    // Step 3 – Skills
    private final NumberField nominalRangeMinField = new NumberField("Nominal Range Min");
    private final NumberField nominalRangeMaxField = new NumberField("Nominal Range Max");

    // Step 4 – Deployment
    private final ComboBox<String> determinismCombo = new ComboBox<>("Determinism Class");
    private final ComboBox<String> safetyIntegrityCombo = new ComboBox<>("Safety Integrity Level");

    // Step 5 – Semantic Mapping
    private final NumberField minRecallField = new NumberField("Min Recall");
    private final NumberField maxLatencyField = new NumberField("Max Latency (ms)");

    // Step 6 – Review (summary labels rendered dynamically)
    private final VerticalLayout reviewLayout = new VerticalLayout();

    public RegistrationWizardView(OptionsConfigService optionsConfigService,
                                  OntologyModelLoader ontologyModelLoader,
                                  RDeployService rDeployService) {
        this.optionsConfigService = optionsConfigService;
        this.ontologyModelLoader = ontologyModelLoader;
        this.rDeployService = rDeployService;

        setWidthFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Registration Wizard"));
        add(new Paragraph("Register a new requirement through the multi-step wizard."));

        buildSteps();
        buildNavigationButtons();
        showStep(0);
    }

    private void buildSteps() {
        steps.add(buildGeneralInfoStep());
        steps.add(buildCommunicationStep());
        steps.add(buildCapabilitiesStep());
        steps.add(buildSkillsStep());
        steps.add(buildDeploymentStep());
        steps.add(buildSemanticMappingStep());
        steps.add(buildReviewStep());
        steps.add(buildRegisterStep());

        for (VerticalLayout step : steps) {
            step.setVisible(false);
            add(step);
        }
    }

    private VerticalLayout buildGeneralInfoStep() {
        var layout = new VerticalLayout(new H3("Step 1: General Info"));
        loadOptionsIntoCombo(requirementTypeCombo, "requirementType");
        layout.add(titleField, descriptionField, requirementTypeCombo);
        return layout;
    }

    private VerticalLayout buildCommunicationStep() {
        var layout = new VerticalLayout(new H3("Step 2: Communication"));
        loadOptionsIntoCombo(protocolCombo, "protocol");
        loadOptionsIntoCombo(accessModeCombo, "accessMode");
        loadOptionsIntoCombo(networkZoneCombo, "networkZone");
        layout.add(protocolCombo, accessModeCombo, networkZoneCombo);
        return layout;
    }

    private VerticalLayout buildCapabilitiesStep() {
        var layout = new VerticalLayout(new H3("Step 3: Capabilities"));
        loadOptionsIntoCombo(functionClassCombo, "functionClass");
        loadOptionsIntoCombo(engineeringUnitCombo, "engineeringUnit");
        layout.add(functionClassCombo, engineeringUnitCombo);
        return layout;
    }

    private VerticalLayout buildSkillsStep() {
        var layout = new VerticalLayout(new H3("Step 4: Skills"));
        layout.add(nominalRangeMinField, nominalRangeMaxField);
        return layout;
    }

    private VerticalLayout buildDeploymentStep() {
        var layout = new VerticalLayout(new H3("Step 5: Deployment"));
        loadOptionsIntoCombo(determinismCombo, "determinismClass");
        loadOptionsIntoCombo(safetyIntegrityCombo, "safetyIntegrityLevel");
        layout.add(determinismCombo, safetyIntegrityCombo);
        return layout;
    }

    private VerticalLayout buildSemanticMappingStep() {
        var layout = new VerticalLayout(new H3("Step 6: Semantic Mapping"));
        minRecallField.setValue(0.90);
        maxLatencyField.setValue(60.0);
        layout.add(minRecallField, maxLatencyField);
        return layout;
    }

    private VerticalLayout buildReviewStep() {
        var layout = new VerticalLayout(new H3("Step 7: Review"));
        layout.add(reviewLayout);
        return layout;
    }

    private VerticalLayout buildRegisterStep() {
        var layout = new VerticalLayout(new H3("Step 8: Register"));
        var registerButton = new Button("Register", e -> registerRequirement());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(new Paragraph("Click Register to persist this requirement to the ontology."),
            registerButton);
        return layout;
    }

    private void buildNavigationButtons() {
        prevButton.addClickListener(e -> {
            if (currentStep > 0) {
                showStep(currentStep - 1);
            }
        });
        nextButton.addClickListener(e -> {
            if (currentStep < steps.size() - 1) {
                if (currentStep == steps.size() - 3) {
                    populateReview();
                }
                showStep(currentStep + 1);
            }
        });
        saveDraftButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SystemStatusView.class)));

        stepIndicator.addClassNames("registration-step-indicator");

        var navBar = new HorizontalLayout(stepIndicator, prevButton, nextButton, saveDraftButton, cancelButton);
        navBar.setAlignItems(FlexComponent.Alignment.CENTER);
        navBar.setSpacing(true);
        navBar.setWidthFull();
        add(navBar);
        add(new Hr());
    }

    private void showStep(int index) {
        steps.get(currentStep).setVisible(false);
        currentStep = index;
        steps.get(currentStep).setVisible(true);

        prevButton.setEnabled(currentStep > 0);
        nextButton.setEnabled(currentStep < steps.size() - 1);
        stepIndicator.setText("Step " + (currentStep + 1) + " of " + steps.size());
    }

    private void populateReview() {
        reviewLayout.removeAll();
        addReviewLine("Title", titleField.getValue());
        addReviewLine("Description", descriptionField.getValue());
        addReviewLine("Requirement Type", requirementTypeCombo.getValue());
        addReviewLine("Protocol", protocolCombo.getValue());
        addReviewLine("Access Mode", accessModeCombo.getValue());
        addReviewLine("Network Zone", networkZoneCombo.getValue());
        addReviewLine("Function Class", functionClassCombo.getValue());
        addReviewLine("Engineering Unit", engineeringUnitCombo.getValue());
        addReviewLine("Nominal Range Min", String.valueOf(nominalRangeMinField.getValue()));
        addReviewLine("Nominal Range Max", String.valueOf(nominalRangeMaxField.getValue()));
        addReviewLine("Determinism Class", determinismCombo.getValue());
        addReviewLine("Safety Integrity Level", safetyIntegrityCombo.getValue());
        addReviewLine("Min Recall", String.valueOf(minRecallField.getValue()));
        addReviewLine("Max Latency (ms)", String.valueOf(maxLatencyField.getValue()));
    }

    private void addReviewLine(String label, String value) {
        var row = new HorizontalLayout(
            new Span(label + ": "),
            new Span(value != null ? value : "")
        );
        reviewLayout.add(row);
    }

    private void registerRequirement() {
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
            addPropertyIfPresent(model, requirement, "requirementType", requirementTypeCombo.getValue());
            addPropertyIfPresent(model, requirement, "protocol", protocolCombo.getValue());
            addPropertyIfPresent(model, requirement, "accessMode", accessModeCombo.getValue());
            addPropertyIfPresent(model, requirement, "networkZone", networkZoneCombo.getValue());
            addPropertyIfPresent(model, requirement, "functionClass", functionClassCombo.getValue());
            addPropertyIfPresent(model, requirement, "engineeringUnit", engineeringUnitCombo.getValue());
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
            Notification.show("Requirement registered: " + requirementId, 3000,
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            log.error("Registration error", ex);
            Notification.show("Registration error: " + ex.getMessage(), 5000,
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

    private void loadOptionsIntoCombo(ComboBox<String> combo, String fieldName) {
        try {
            var options = optionsConfigService.getOptions(fieldName);
            combo.setItems(options.stream().map(o -> o.value()).toList());
        } catch (Exception ignored) {
            // Options may not be available in all environments
        }
    }
}
