package com.sda.mapper.ui;

import com.sda.mapper.api.Option;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Requirement form view for creating and matching requirements.
 * Provides inline field matching (Mode B) and requirement persistence.
 */
@Route(value = "requirements", layout = MainLayout.class)
@PageTitle("Requirement Form")
public class RequirementFormView extends VerticalLayout {

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

    public RequirementFormView() {
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
            String json = """
                {
                  "protocol": "%s",
                  "accessMode": "%s",
                  "minRecall": %s,
                  "maxLatencyMs": %s
                }
                """.formatted(
                protocolCombo.getValue() != null ? protocolCombo.getValue() : "OPC-UA",
                accessModeCombo.getValue() != null ? accessModeCombo.getValue() : "read-only",
                minRecallField.getValue() != null ? minRecallField.getValue() : 0.90,
                maxLatencyField.getValue() != null ? maxLatencyField.getValue().intValue() : 60
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("/api/v1/requirements/match"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            resultsPanel.removeAll();
            resultsPanel.setVisible(true);

            if (response.statusCode() == 200) {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var node = mapper.readTree(response.body());
                boolean matched = node.get("matched").asBoolean();

                if (matched) {
                    resultsPanel.add(new Paragraph("Matched service: "
                        + node.get("serviceName").asText()));
                    resultsPanel.add(new Paragraph("Capability: "
                        + node.get("capabilityName").asText()));
                    resultsPanel.add(new Paragraph("Recall: "
                        + node.get("recall").asDouble()));
                    resultsPanel.add(new Paragraph("Latency: "
                        + node.get("latencyMs").asInt() + "ms"));
                    resultsPanel.add(new Paragraph("Explanation: "
                        + node.get("explanation").asText()));
                } else {
                    resultsPanel.add(new Paragraph("No matching service found."));
                    resultsPanel.add(new Paragraph(node.get("explanation").asText()));
                }
            } else {
                resultsPanel.add(new Paragraph("Match request failed with status "
                    + response.statusCode()));
            }
        } catch (Exception ex) {
            Notification.show("Match error: " + ex.getMessage(), 5000,
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void saveRequirement() {
        try {
            String json = """
                {
                  "title": "%s",
                  "description": "%s",
                  "functionClass": "%s",
                  "protocol": "%s",
                  "accessMode": "%s",
                  "engineeringUnit": "%s",
                  "networkZone": "%s",
                  "determinismClass": "%s",
                  "safetyIntegrityLevel": "%s",
                  "nominalRangeMin": %s,
                  "nominalRangeMax": %s,
                  "minRecall": %s,
                  "maxLatencyMs": %s
                }
                """.formatted(
                escapeJson(titleField.getValue()),
                escapeJson(descriptionField.getValue()),
                escapeJson(functionClassField.getValue()),
                escapeJson(protocolCombo.getValue()),
                escapeJson(accessModeCombo.getValue()),
                escapeJson(engineeringUnitField.getValue()),
                escapeJson(networkZoneCombo.getValue()),
                escapeJson(determinismCombo.getValue()),
                escapeJson(safetyIntegrityCombo.getValue()),
                nominalRangeMinField.getValue() != null ? nominalRangeMinField.getValue() : "null",
                nominalRangeMaxField.getValue() != null ? nominalRangeMaxField.getValue() : "null",
                minRecallField.getValue() != null ? minRecallField.getValue() : "null",
                maxLatencyField.getValue() != null ? maxLatencyField.getValue().intValue() : "null"
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("/api/v1/requirements"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                Notification.show("Requirement saved successfully", 3000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("Save failed: " + response.body(), 5000,
                    Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception ex) {
            Notification.show("Save error: " + ex.getMessage(), 5000,
                Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private static String escapeJson(String value) {
        return value != null ? value.replace("\"", "\\\"") : "";
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
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("/api/v1/options/" + fieldName))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Option> options = mapper.readValue(response.body(),
                    mapper.getTypeFactory().constructCollectionType(List.class, Option.class));
                combo.setItems(options.stream().map(Option::value).toList());
            }
        } catch (Exception ignored) {
            // Options may not be available in all environments
        }
    }
}

