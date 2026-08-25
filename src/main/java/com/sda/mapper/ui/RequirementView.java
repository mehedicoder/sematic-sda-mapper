package com.sda.mapper.ui;

import com.sda.mapper.api.dto.MatchRequirementRequest;
import com.sda.mapper.reasoning.RequirementService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * Manual requirement entry and matching view.
 *
 * <p>Lets a user define a requirement (name, description, capability type and
 * constraints) — persisted into the live ontology — and match it against the
 * SDA service catalog using the generic matcher. Any requirement modeled with
 * the standard SDA properties can be matched; no per-requirement query is needed.
 */
@Route(value = "requirement", layout = MainLayout.class)
@PageTitle("Requirement")
public class RequirementView extends VerticalLayout {

    @Autowired
    private RequirementService requirementService;

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

    public RequirementView() {
        setPadding(true);
        setSpacing(true);
        setSizeFull();
        addClassName("requirement-view");

        add(new H2("Requirement Definition & Matching"));

        reqId.setPlaceholder("REQ-USER-001");
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
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("800px", 2));
        form.setColspan(reqDescription, 2);
        form.setColspan(createAndMatch, 2);

        var matchRow = new VerticalLayout(
            new H3("Match an existing requirement"),
            new com.vaadin.flow.component.orderedlayout.HorizontalLayout(matchExistingId, matchExisting));
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
        RequirementService svc = requirementService;
        if (svc == null) {
            Notification.show("Requirement service unavailable");
            return;
        }
        try {
            svc.persist(buildRequest());
            Notification.show("Requirement " + reqId.getValue() + " created");
            if (andMatch) {
                showResult(svc.match(reqId.getValue()));
            }
        } catch (Exception ex) {
            Notification.show("Failed to create requirement: " + ex.getMessage());
        }
    }

    private void matchExisting() {
        RequirementService svc = requirementService;
        if (svc == null) {
            Notification.show("Requirement service unavailable");
            return;
        }
        String id = matchExistingId.getValue();
        if (id == null || id.isBlank()) {
            Notification.show("Enter a requirement ID");
            return;
        }
        showResult(svc.match(id));
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
