package com.sda.mapper.hub.ui;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.RequirementService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.support.DemoAboxBuilder;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end UI checks for requirement matching: a user enters (or loads) a
 * requirement in the {@link RequirementMatchingPanel} and obtains the matched
 * service together with its human-readable reasoning, entirely from the UI.
 */
@DisplayName("Requirement matching reasoning from the UI")
class RequirementMatchingPanelE2eTest {

    private static OntologyModelLoader loader;
    private RequirementMatchingPanel panel;

    @BeforeAll
    static void setUpOntology() {
        loader = new OntologyModelLoader();
        loader.setFilePath("ontology/sda_ontology_v2_structured.ttl");
        loader.loadOntology();
        DemoAboxBuilder.apply(loader.getModel());
    }

    @BeforeEach
    void setUp() {
        com.vaadin.flow.component.UI.setCurrent(new com.vaadin.flow.component.UI());
        var reasoner = new SemanticReasonerService(
            loader, new SparqlQueryLoader(), new RDeployService(loader));
        panel = new RequirementMatchingPanel(new RequirementService(loader, reasoner));
    }

    @AfterEach
    void tearDown() {
        com.vaadin.flow.component.UI.setCurrent(null);
    }

    private void setField(String caption, String value) {
        TextField field = findByType(panel, TextField.class).stream()
            .filter(f -> caption.equals(f.getLabel()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Text field not found: " + caption));
        field.setValue(value == null ? "" : value);
    }

    private void clickButton(String label) {
        Button button = findByType(panel, Button.class).stream()
            .filter(b -> label.equals(b.getText()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Button not found: " + label));
        button.click();
    }

    private String resultText() {
        Pre resultArea = null;
        for (var field : RequirementMatchingPanel.class.getDeclaredFields()) {
            if (field.getName().equals("resultArea")) {
                field.setAccessible(true);
                try {
                    resultArea = (Pre) field.get(panel);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        assertNotNull(resultArea, "result area must exist");
        StringBuilder sb = new StringBuilder();
        append(resultArea.getElement(), sb);
        return sb.toString();
    }

    private static <T extends Component> List<T> findByType(Component root, Class<T> type) {
        java.util.List<T> found = new java.util.ArrayList<>();
        collect(root, type, found);
        return found;
    }

    private static <T extends Component> void collect(Component root, Class<T> type, List<T> found) {
        if (type.isInstance(root)) {
            found.add(type.cast(root));
        }
        root.getChildren().forEach(child -> collect(child, type, found));
    }

    private static void append(com.vaadin.flow.dom.Element element, StringBuilder sb) {
        String own = element.getText();
        if (own != null && !own.isBlank()) {
            sb.append(own);
        }
        element.getChildren().forEach(child -> append(child, sb));
    }

    @Test
    @DisplayName("Should match REQ-001 from the UI and show the service with full reasoning")
    void shouldMatchExistingRequirementAndShowReasoning() {
        setField("Match existing requirement by ID", "REQ-001");
        clickButton("Match");

        String result = resultText();
        assertTrue(result.contains("Matched service : Flowmeter Diagnostics Service"),
            "result must name the matched service, got: " + result);
        assertTrue(result.contains("OPC-UA"), "protocol shown");
        assertTrue(result.contains("read-only"), "access mode shown");
        assertTrue(result.contains("Recall"), "recall shown");
        assertTrue(result.contains("Latency"), "latency shown");
        assertTrue(result.contains("Explanation"), "explanation block shown");
        String explanation = result.substring(result.indexOf("Explanation")).trim();
        assertTrue(explanation.length() > "Explanation".length(),
            "reasoning text present after the Explanation label");
    }

    @Test
    @DisplayName("Should create-and-match a user-defined requirement on the fly from the UI")
    void shouldCreateAndMatchUserDefinedRequirement() {
        setField("Requirement ID", "REQ-UAT-PVA-1");
        setField("Capability type", "ProcessVariableAnalysis");
        setField("Protocol", "OPC-UA");

        clickButton("Create & Match");

        String result = resultText();
        assertTrue(result.contains("Process Variable Analytics"),
            "user-defined PVA requirement must resolve to the analytics service, got: " + result);
    }

    @Test
    @DisplayName("Should load an existing requirement into the form for review before matching")
    void shouldLoadExistingRequirementIntoForm() {
        setField("Match existing requirement by ID", "REQ-001");
        clickButton("Match");

        Optional<TextField> reqId = findByType(panel, TextField.class).stream()
            .filter(f -> "Requirement ID".equals(f.getLabel()))
            .findFirst();
        assertTrue(reqId.isPresent());
        org.junit.jupiter.api.Assertions.assertEquals(
            "REQ-001", reqId.get().getValue(),
            "existing requirement id is loaded into the editable form");
    }

    @Test
    @DisplayName("Should show a clear no-match explanation for an unknown requirement")
    void shouldShowNoMatchForUnknownRequirement() {
        setField("Match existing requirement by ID", "REQ-UAT-UNKNOWN");
        clickButton("Match");

        String result = resultText();
        assertTrue(result.startsWith("No match found."),
            "unknown requirement yields the no-match view");
        assertTrue(result.contains("Unknown requirement"),
            "no-match explanation names the cause");
    }

    @Test
    @DisplayName("Should create and match an any-ID requirement from the Match field plus a capability type")
    void shouldCreateAndMatchAnyIdWithCapabilityFromMatchFieldOnly() {
        setField("Match existing requirement by ID", "REQ-UAT-PVA-2");
        setField("Capability type", "ProcessVariableAnalysis");

        clickButton("Match");

        String result = resultText();
        assertTrue(result.contains("Process Variable Analytics"),
            "any-ID + capability type must create on the fly and match (manual §8.4), got: "
                + result);
    }
}
