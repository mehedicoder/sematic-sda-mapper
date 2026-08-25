package com.sda.mapper.hub.ui;

import com.sda.mapper.api.dto.MatchRequirementRequest;
import com.sda.mapper.reasoning.RequirementService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.spring.annotation.SpringComponent;

import java.util.Objects;

/**
 * Reusable requirement definition &amp; matching panel.
 *
 * <p>Lets a user define a requirement (name, description, capability type and
 * constraints) — persisted into the live ontology — and match it against the
 * SDA service catalog using the generic matcher. Any requirement modeled with
 * the standard SDA properties can be matched; no per-requirement query is needed.
 * The "Match existing requirement by ID" field accepts any ID — if the ID is not
 * yet in the model, supplying a capability type / constraints creates it on the
 * fly and matches immediately.
 */
@SpringComponent
@UIScope
public class RequirementMatchingPanel extends VerticalLayout {

    private final RequirementService requirementService;

    private final TextField reqId = new TextField("Requirement ID");
    private final TextField reqName = new TextField("Requirement name");
    private final TextArea reqDescription = new TextArea("Requirement description");
    private final TextField capabilityType = new TextField("Capability type");
    private final TextField protocol = new TextField("Protocol");
    private final TextField accessMode = new TextField("Access mode");
    private final TextField minRecall = new TextField("Min recall (e.g. 0.90)");
    private final TextField maxLatencyMs = new TextField("Max latency (ms)");
    private final TextField networkZone = new TextField("Network zone");
    private final TextField safetyIntegrityLevel = new TextField("Safety integrity level");
    private final TextField determinismClass = new TextField("Determinism class");

    private final TextField matchExistingId = new TextField("Match existing requirement by ID");
    private final Pre resultArea = new Pre("");

    public RequirementMatchingPanel(RequirementService requirementService) {
        this.requirementService = Objects.requireNonNull(requirementService,
            "RequirementService is required");
        setPadding(true);
        setSpacing(true);
        setWidthFull();

        reqId.setPlaceholder("REQ-USER-001 (or any ID, e.g. REQ-ABC)");
        reqName.setPlaceholder("My analytics requirement");
        reqDescription.setPlaceholder("Describe what the service should do...");
        capabilityType.setPlaceholder("ProcessVariableAnalysis");
        protocol.setPlaceholder("OPC-UA");
        accessMode.setPlaceholder("read-only");

        var create = new Button("Create Requirement", e -> createRequirement(false));
        var createAndMatch = new Button("Create & Match", e -> createRequirement(true));
        var matchExisting = new Button("Match", e -> matchExisting());

        FormLayout form = new FormLayout(
            reqId, reqName, capabilityType, protocol, accessMode,
            minRecall, maxLatencyMs, networkZone, safetyIntegrityLevel, determinismClass,
            create, createAndMatch);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("800px", 2));
        form.setColspan(reqDescription, 2);
        form.setColspan(createAndMatch, 2);

        var matchRow = new VerticalLayout(
            new H3("Match an existing requirement (any ID)"),
            new HorizontalLayout(matchExistingId, matchExisting));
        matchRow.setSpacing(true);

        resultArea.setWidthFull();
        resultArea.getStyle().set("white-space", "pre-wrap");

        add(reqDescription, form, new Hr(), matchRow, new Hr(), resultArea);
    }

    private MatchRequirementRequest buildRequest() {
        return new MatchRequirementRequest(
            reqId.getValue(),
            reqName.getValue(),
            reqDescription.getValue(),
            capabilityType.getValue(),
            null,
            blankToNull(protocol.getValue()),
            blankToNull(accessMode.getValue()),
            null,
            blankToNull(safetyIntegrityLevel.getValue()),
            blankToNull(networkZone.getValue()),
            blankToNull(determinismClass.getValue()),
            parseDouble(minRecall.getValue()),
            parseInteger(maxLatencyMs.getValue()),
            null,
            null);
    }

    private void createRequirement(boolean andMatch) {
        if (reqId.isEmpty()) {
            Notification.show("Requirement ID is required");
            return;
        }
        try {
            requirementService.persist(buildRequest());
            Notification.show("Requirement " + reqId.getValue() + " created");
            if (andMatch) {
                showResult(requirementService.match(reqId.getValue()));
            }
        } catch (Exception ex) {
            Notification.show("Failed to create requirement: " + ex.getMessage());
        }
    }

    private void matchExisting() {
        String id = matchExistingId.getValue();
        if (id == null || id.isBlank()) {
            id = reqId.getValue();
        }
        if (id == null || id.isBlank()) {
            Notification.show("Enter a requirement ID (any value, e.g. REQ-ABC)");
            return;
        }
        // Load the requirement (if it exists) into the editable fields so the user
        // can review and change the ID / description / constraints before running.
        MatchRequirementRequest loaded = requirementService.readRequest(id);
        if (loaded != null) {
            populateForm(loaded);
            Notification.show("Loaded " + id + " — edit the fields if needed, then Create & Match");
        }
        MatchRequirementRequest request = buildRequest();
        showResult(requirementService.matchOrCreate(request));
    }

    /**
     * Copies a requirement's values into the editable text fields (null-safe), so
     * the user can inspect and modify them before persisting / matching.
     */
    private void populateForm(MatchRequirementRequest req) {
        if (req == null) {
            return;
        }
        reqId.setValue(req.requirementId() != null ? req.requirementId() : "");
        reqName.setValue(req.requirementName() != null ? req.requirementName() : "");
        reqDescription.setValue(req.requirementDescription() != null ? req.requirementDescription() : "");
        capabilityType.setValue(req.capabilityType() != null ? req.capabilityType() : "");
        protocol.setValue(req.protocol() != null ? req.protocol() : "");
        accessMode.setValue(req.accessMode() != null ? req.accessMode() : "");
        networkZone.setValue(req.networkZone() != null ? req.networkZone() : "");
        safetyIntegrityLevel.setValue(req.safetyIntegrityLevel() != null ? req.safetyIntegrityLevel() : "");
        determinismClass.setValue(req.determinismClass() != null ? req.determinismClass() : "");
        minRecall.setValue(req.minRecall() != null ? String.valueOf(req.minRecall()) : "");
        maxLatencyMs.setValue(req.maxLatencyMs() != null ? String.valueOf(req.maxLatencyMs()) : "");
    }

    private void showResult(ServiceMatchResult result) {
        if (result == null || !result.matched()) {
            resultArea.setText("No match found.\n"
                + (result != null && result.explanation() != null ? result.explanation() : ""));
            return;
        }
        String text = """
            Matched service : %s
            Capability      : %s
            Protocol        : %s
            Access mode     : %s
            Recall          : %.2f
            Latency (ms)    : %d
            Deployment      : %s
            Lifecycle       : %s
            Explanation     : %s
            """.formatted(
            result.serviceName(),
            result.capabilityName(),
            result.protocol(),
            result.accessMode(),
            result.recall(),
            result.latencyMs(),
            result.deploymentContext(),
            result.lifecycleStatus(),
            result.explanation());
        resultArea.setText(text);
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private static Double parseDouble(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInteger(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
