package com.sda.mapper.ui;

import com.sda.mapper.deployment.ManifestResult;
import com.sda.mapper.deployment.ManifestService;
import com.sda.mapper.reasoning.ExplanationService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.ui.component.DashboardCard;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deployment view showing generated Docker Compose and Kubernetes manifests
 * for matched services across different environment tiers.
 */
@Route(value = "deployment", layout = MainLayout.class)
@PageTitle("Deployment")
public class DeploymentView extends VerticalLayout {

    private final ManifestService manifestService;
    private final SemanticReasonerService reasonerService;
    private final ExplanationService explanationService;

    private final VerticalLayout manifestDisplay = new VerticalLayout();

    private static final Map<String, String> ENVIRONMENTS = new LinkedHashMap<>();
    static {
        ENVIRONMENTS.put("Development", "Local development and testing environment");
        ENVIRONMENTS.put("Testing", "Integration and staging environment");
        ENVIRONMENTS.put("Production", "Live production deployment environment");
    }

    public DeploymentView(ManifestService manifestService,
                          SemanticReasonerService reasonerService,
                          ExplanationService explanationService) {
        this.manifestService = manifestService;
        this.reasonerService = reasonerService;
        this.explanationService = explanationService;

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(new H2("Deployment Manifests"));
        add(new Paragraph("View and generate Docker Compose and Kubernetes deployment manifests for matched services."));

        add(createEnvironmentCards());
        add(createManifestSection());
    }

    private Component createEnvironmentCards() {
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.setSpacing(true);

        for (var entry : ENVIRONMENTS.entrySet()) {
            DashboardCard card = new DashboardCard(
                VaadinIcon.CUBE, entry.getKey(), 0, getEnvironmentColor(entry.getKey()));
            cards.add(card);
        }

        return cards;
    }

    private Component createManifestSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setSpacing(true);

        Button generateBtn = new Button("Generate Manifests", VaadinIcon.COG.create());
        generateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateBtn.addClickListener(e -> generateManifests());

        Tabs envTabs = new Tabs();
        Tab devTab = new Tab("Development");
        Tab testTab = new Tab("Testing");
        Tab prodTab = new Tab("Production");
        envTabs.add(devTab, testTab, prodTab);

        manifestDisplay.setWidthFull();
        manifestDisplay.setSpacing(true);
        manifestDisplay.setVisible(false);

        section.add(generateBtn, envTabs, manifestDisplay);
        return section;
    }

    private void generateManifests() {
        manifestDisplay.removeAll();
        manifestDisplay.setVisible(true);

        ServiceMatchResult result = reasonerService.matchByRequirementId("REQ-001");
        ManifestResult manifests = manifestService.generateManifests(result);

        String explanation = explanationService.explain(result);

        HorizontalLayout yamlColumns = new HorizontalLayout();
        yamlColumns.setWidthFull();
        yamlColumns.setSpacing(true);

        yamlColumns.add(createYamlPanel("Docker Compose", manifests.dockerComposeYaml()));
        yamlColumns.add(createYamlPanel("Kubernetes Deployment", manifests.kubernetesYaml()));

        manifestDisplay.add(yamlColumns);

        H3 explanationHeader = new H3("Match Explanation");
        TextArea explanationArea = new TextArea();
        explanationArea.setValue(explanation);
        explanationArea.setWidthFull();
        explanationArea.setReadOnly(true);
        explanationArea.setHeight("250px");

        manifestDisplay.add(explanationHeader, explanationArea);
    }

    private Component createYamlPanel(String title, String yaml) {
        VerticalLayout panel = new VerticalLayout();
        panel.setWidthFull();
        panel.setSpacing(false);

        H3 header = new H3(title);
        header.addClassNames(LumoUtility.FontSize.MEDIUM);

        TextArea yamlArea = new TextArea();
        yamlArea.setValue(yaml);
        yamlArea.setWidthFull();
        yamlArea.setReadOnly(true);
        yamlArea.setHeight("400px");
        yamlArea.addClassName("yaml-display");

        panel.add(header, yamlArea);
        return panel;
    }

    private String getEnvironmentColor(String env) {
        return switch (env) {
            case "Development" -> "var(--lumo-success-color)";
            case "Testing" -> "var(--lumo-warning-color)";
            case "Production" -> "var(--lumo-error-color)";
            default -> "var(--lumo-primary-color)";
        };
    }
}
