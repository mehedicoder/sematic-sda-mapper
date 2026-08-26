package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.parser.AasModelParser;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.MtpModelParser;
import com.sda.mapper.hub.parser.OpcUaModelParser;
import com.sda.mapper.hub.service.InferenceService;
import com.sda.mapper.hub.service.LiftService;
import com.sda.mapper.hub.service.ManifestExportService;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.MappingHubSession;
import com.sda.mapper.hub.service.MatchingService;
import com.sda.mapper.hub.service.PipelineResult;
import com.sda.mapper.hub.service.RecommendationService;
import com.sda.mapper.hub.service.SemanticMatchService;
import com.sda.mapper.reasoning.RequirementService;
import com.sda.mapper.reasoning.RDeployService;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.service.OntologyDataService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Pre;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end UI checks for matching service reasoning: a user uploads a device
 * description and runs the pipeline entirely from {@link MappingHubView}, and a
 * user performs requirement matching with human-readable reasoning output.
 */
@DisplayName("Mapping Hub end-to-end pipeline from the UI")
class MappingHubViewPipelineE2eTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new UI();
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    private MappingHubView createView() {
        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        MappingHubService hubService = new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            matchingService,
            new SemanticMatchService(new SparqlQueryLoader()),
            recommendationService,
            new ManifestExportService(matchingService, recommendationService),
            new MappingHubSession());
        return new MappingHubView(hubService, ontologyDataService,
            new com.sda.mapper.hub.ui.ManualEntryFormFactory(ontologyDataService));
    }

    private byte[] fixture(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Fixture not found: " + path);
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void upload(MappingHubView view, String fileName, byte[] content) throws Exception {
        Method onUpload = MappingHubView.class.getDeclaredMethod("onUpload", String.class, byte[].class);
        onUpload.setAccessible(true);
        onUpload.invoke(view, fileName, content);
    }

    private static <T extends Component> Optional<T> findByText(Component root, Class<T> type, String text) {
        if (type.isInstance(root) && text.equals(root.getElement().getText())) {
            return Optional.of(type.cast(root));
        }
        for (Component child : root.getChildren().toList()) {
            Optional<T> hit = findByText(child, type, text);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }

    private static String recursiveText(Component component) {
        StringBuilder sb = new StringBuilder();
        appendText(component.getElement(), sb);
        return sb.toString();
    }

    private static void appendText(com.vaadin.flow.dom.Element element, StringBuilder sb) {
        String own = element.getText();
        if (own != null && !own.isBlank()) {
            sb.append(own);
        }
        element.getChildren().forEach(child -> appendText(child, sb));
    }

    @Test
    @DisplayName("Should run the full pipeline from an upload and show match reasoning in the console")
    void shouldRunPipelineFromUploadAndShowMatchesInConsole() throws Exception {
        MappingHubView view = createView();

        upload(view, "pump-aas.json", fixture("/device-descriptions/aas/pump-aas.json"));

        ConsolePanel console = field(view, "console", ConsolePanel.class);
        List<String> lines = console.getLines();
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[PARSE]")), "parse step logged");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[INSTANTIATE]")), "instantiate step logged");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[LIFT]")), "lift step logged");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[INFER]")), "infer step logged");
        assertTrue(lines.stream().anyMatch(l ->
                l.startsWith("[MATCH]") && l.contains("SPARQL match:")),
            "semantic SPARQL matching must be executed and reported");

        PipelineResult lastResult = field(view, "lastResult", PipelineResult.class);
        assertNotNull(lastResult, "pipeline result stored after upload");
        assertTrue(lastResult.gapReport().coveragePercent() > 0, "coverage computed");
    }

    @Test
    @DisplayName("Should update the live observability timeline and trace panel during a UI-run pipeline")
    void shouldStreamObservabilityDuringUiRun() throws Exception {
        MappingHubView view = createView();
        MappingHubService hubService = field(view, "hubService", MappingHubService.class);

        List<com.sda.mapper.observability.PipelineEvent> streamed =
            new java.util.concurrent.CopyOnWriteArrayList<>();
        hubService.getAuditPublisher().addListener(streamed::add);

        upload(view, "pump-aas.json", fixture("/device-descriptions/aas/pump-aas.json"));

        // The run published one audit event stream for every stage transition.
        assertTrue(streamed.stream().anyMatch(e ->
                "LIFT".equals(e.stage()) && com.sda.mapper.observability.PipelineEvent.SUCCEEDED.equals(e.status())),
            "lift stage audited during the UI-run pipeline");
        assertTrue(streamed.stream().anyMatch(e ->
                "MATCH".equals(e.stage()) && com.sda.mapper.observability.PipelineEvent.SUCCEEDED.equals(e.status())),
            "match stage audited during the UI-run pipeline");

        // Deliver the same events to the panels exactly as the push listener does.
        StageTimeline timeline = field(view, "stageTimeline", StageTimeline.class);
        PipelineConsolePanel pipelineConsole = field(view, "pipelineConsole", PipelineConsolePanel.class);
        PipelineTracePanel tracePanel = field(view, "tracePanel", PipelineTracePanel.class);
        streamed.forEach(timeline::onEvent);
        streamed.forEach(pipelineConsole::onEvent);
        streamed.forEach(tracePanel::onEvent);

        String timelineText = recursiveText(timeline);
        assertTrue(timelineText.contains("LIFT"), "timeline renders stage badges");
        assertTrue(timelineText.contains("\u2713"),
            "succeeded stages show a check mark with duration");
        assertTrue(pipelineConsole.getLines().stream()
                .anyMatch(line -> line.contains("[LIFT]")),
            "streamed audit events appear in the observability console");
        assertTrue(recursiveText(tracePanel).contains("Run "),
            "trace panel shows the current run id");
    }

    @Test
    @DisplayName("Should re-run the whole pipeline from the Run Pipeline button")
    void shouldReRunPipelineFromButton() throws Exception {
        MappingHubView view = createView();
        upload(view, "pump-aas.json", fixture("/device-descriptions/aas/pump-aas.json"));

        Button runButton = findByText(view, Button.class, "Run Pipeline")
            .orElseThrow(() -> new AssertionError("Run Pipeline button must exist"));
        runButton.click();

        ConsolePanel console = field(view, "console", ConsolePanel.class);
        assertTrue(console.getLines().stream().anyMatch(l -> l.contains("Re-running pipeline")),
            "re-run output replaced the console content");
        assertTrue(console.getLines().stream().anyMatch(l -> l.startsWith("[MATCH]")),
            "re-run executed semantic matching");
        PipelineResult lastResult = field(view, "lastResult", PipelineResult.class);
        assertNotNull(lastResult, "a fresh pipeline result is available after re-run");
    }

    @Test
    @DisplayName("Should surface manual requirement entry as pipeline input from the UI")
    void shouldAcceptManualRequirementAndRerunFromUi() throws Exception {
        MappingHubView view = createView();
        upload(view, "pump-aas.json", fixture("/device-descriptions/aas/pump-aas.json"));

        MappingHubService hubService = field(view, "hubService", MappingHubService.class);
        PipelineResult result = hubService.addManualEntity(
            new com.sda.mapper.hub.model.ManualEntity(
                "UseCaseRequirement", "REQ-UAT-E2E", "UAT Requirement",
                java.util.Map.of(
                    "id", "REQ-UAT-E2E",
                    "name", "UAT Requirement",
                    "requirementID", "REQ-UAT-E2E",
                    "lifecycleStatus", "Defined")));

        assertTrue(result.consoleLines().stream()
                .anyMatch(l -> l.startsWith("[INSTANTIATE]") && l.contains("manual")),
            "manual entry is instantiated as RDF triples and re-runs the pipeline");
        assertTrue(result.consoleLines().stream().anyMatch(l -> l.startsWith("[MATCH]")));
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(target);
    }
}
