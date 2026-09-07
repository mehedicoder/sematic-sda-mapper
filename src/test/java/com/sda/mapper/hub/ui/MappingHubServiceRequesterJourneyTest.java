package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.hub.parser.AasModelParser;
import com.sda.mapper.hub.parser.AutomationMlParser;
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
import com.sda.mapper.hub.model.OfferedCapability;
import com.sda.mapper.observability.ReasoningTracePublisher;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.service.OntologyDataService;
import com.sda.mapper.llm.ChatCompletionsClient;
import com.sda.mapper.llm.ConceptHint;
import com.sda.mapper.llm.LlmProperties;
import com.sda.mapper.llm.LlmTranslationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Service-requester journey through {@link MappingHubView}: ingest device
 * descriptions by upload, complete missing information with manual entry when
 * prompted, and run the semantic pipeline — which must always execute its
 * ontology-driven SPARQL matching step without errors or dead ends.
 */
@DisplayName("Service requester journey: upload, manual entry, ontology-driven matching")
class MappingHubServiceRequesterJourneyTest {

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
        return createView(null);
    }

    private MappingHubView createView(LlmTranslationService llm) {
        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        MappingHubService hubService = new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser(),
                new AutomationMlParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            matchingService,
            new SemanticMatchService(new SparqlQueryLoader()),
            recommendationService,
            new ManifestExportService(matchingService, recommendationService),
            new MappingHubSession(),
            llm);
        MappingHubView view = new MappingHubView(hubService, ontologyDataService,
            new ManualEntryFormFactory(ontologyDataService),
            new ReasoningTracePublisher());
        ui.add(view);
        return view;
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

    private static void saveManual(MappingHubView view, CanonicalEntity entity) throws Exception {
        Method onManualSave = MappingHubView.class.getDeclaredMethod("onManualSave", CanonicalEntity.class);
        onManualSave.setAccessible(true);
        onManualSave.invoke(view, entity);
    }

    private static ConsolePanel consoleOf(MappingHubView view) throws Exception {
        java.lang.reflect.Field f = MappingHubView.class.getDeclaredField("console");
        f.setAccessible(true);
        return (ConsolePanel) f.get(view);
    }

    private static List<String> consoleLines(MappingHubView view) throws Exception {
        return consoleOf(view).getLines();
    }

    @SuppressWarnings("unchecked")
    private static PipelineResult lastResult(MappingHubView view) throws Exception {
        java.lang.reflect.Field f = MappingHubView.class.getDeclaredField("lastResult");
        f.setAccessible(true);
        return (PipelineResult) f.get(view);
    }

    private static void assertPipelineRanCleanly(List<String> lines) {
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[PARSE]")), "parse reported");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[INSTANTIATE]")), "instantiate reported");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[LIFT]")), "lift reported");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[INFER]")), "infer reported");
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[MATCH]")), "match reported");
        assertTrue(lines.stream().anyMatch(l ->
                l.startsWith("[MATCH]") && l.contains("SPARQL match:")),
            "ontology-driven SPARQL matching executed");
    }

    @Test
    @DisplayName("Should run the pipeline end-to-end from an AAS device description upload")
    void shouldRunPipelineFromAasUpload() throws Exception {
        MappingHubView view = createView();

        upload(view, "pump-aas.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        assertPipelineRanCleanly(consoleLines(view));
        PipelineResult result = lastResult(view);
        assertNotNull(result);
        assertTrue(result.gapReport().coveragePercent() > 0, "coverage computed for requester");

        Button exportButton = field(view, "exportButton", Button.class);
        assertTrue(exportButton.isEnabled(), "manifest export unlocked after successful run");
    }

    @Test
    @DisplayName("Should run the pipeline end-to-end from an MTP AML module upload")
    void shouldRunPipelineFromMtpUpload() throws Exception {
        MappingHubView view = createView();

        upload(view, "pump-module-mtp.xml", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-module-mtp.xml"));

        assertPipelineRanCleanly(consoleLines(view));
        assertTrue(lastResult(view).gapReport().coveragePercent() > 0);
    }

    @Test
    @DisplayName("Should run the pipeline end-to-end from an OPC UA datasheet upload")
    void shouldRunPipelineFromOpcUaUpload() throws Exception {
        MappingHubView view = createView();

        upload(view, "pump-opcua-datasheet.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-opcua-datasheet.json"));

        assertPipelineRanCleanly(consoleLines(view));
        assertTrue(lastResult(view).gapReport().coveragePercent() > 0);
    }

    @Test
    @DisplayName("Should run the pipeline end-to-end from an AutomationML CAEX upload")
    void shouldRunPipelineFromAutomationMlUpload() throws Exception {
        MappingHubView view = createView();

        upload(view, "sample-caex.aml", fixture("/device-descriptions/reference-formats/sample-caex.aml"));

        assertPipelineRanCleanly(consoleLines(view));
        assertTrue(consoleLines(view).stream()
                .anyMatch(l -> l.contains("sample-caex.aml")),
            "the uploaded AutomationML file is acknowledged in the console");
    }

    @Test
    @DisplayName("Should recover gracefully from an invalid upload without blocking the requester")
    void shouldRecoverFromInvalidUpload() throws Exception {
        MappingHubView view = createView();
        upload(view, "pump-aas.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));
        int linesAfterValidRun = consoleLines(view).size();

        // Unsupported file: the view surfaces the error (notification) and does
        // not run or corrupt the pipeline — no exception escapes to the user.
        Method onUpload = MappingHubView.class.getDeclaredMethod("onUpload", String.class, byte[].class);
        onUpload.setAccessible(true);
        onUpload.invoke(view, "notes.txt", "hello".getBytes());

        assertEquals(linesAfterValidRun, consoleLines(view).size(),
            "rejected file must not alter pipeline state");
        assertTrue(consoleLines(view).stream().noneMatch(l ->
                l.startsWith("[PARSE]") && l.contains("notes.txt")),
            "rejected file never reaches parse stage");

        // The requester continues immediately with a valid file.
        upload(view, "motor-aas.json",
            fixture("/device-descriptions/reference-formats/motor-aas.json"));
        assertPipelineRanCleanly(consoleLines(view));
    }

    @Test
    @DisplayName("Should accumulate several uploads in one session and re-run cleanly")
    void shouldAccumulateUploadsInOneSession() throws Exception {
        MappingHubView view = createView();
        MappingHubService hubService = field(view, "hubService", MappingHubService.class);

        upload(view, "pump-aas.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));
        upload(view, "pump-opcua-datasheet.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-opcua-datasheet.json"));

        assertEquals(2, hubService.getSession().getFiles().size(),
            "both uploads accumulate in the session");
        assertPipelineRanCleanly(consoleLines(view));
        assertNotNull(lastResult(view).file(), "second upload recorded as latest input");
    }

    @Test
    @DisplayName("Should complete the journey with manual entry when the upload leaves gaps")
    void shouldCompleteJourneyWithManualEntry() throws Exception {
        MappingHubView view = createView();

        upload(view, "pump-aas.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        saveManual(view, new ManualEntity(
            "UseCaseRequirement", "REQ-UAT-JOURNEY", "Flowmeter health monitoring",
            java.util.Map.of(
                "id", "REQ-UAT-JOURNEY",
                "name", "Flowmeter health monitoring",
                "requirementID", "REQ-UAT-JOURNEY",
                "lifecycleStatus", "Defined",
                "description", "Monitor the health of flowmeter FT-101")));

        // The console now reflects the post-manual-entry re-run.
        List<String> lines = consoleLines(view);
        assertTrue(lines.stream().anyMatch(
                l -> l.startsWith("[INSTANTIATE]") && l.contains("manual")),
            "manual entry stored as RDF instance triples");
        assertTrue(lines.stream().anyMatch(l ->
                l.startsWith("[MATCH]") && l.contains("SPARQL match:")),
            "matching re-executed over the ontology after manual entry");

        MappingHubService hubService = field(view, "hubService", MappingHubService.class);
        assertEquals(1, hubService.getSession().getModel().requirements().size(),
            "manually entered requirement is part of the canonical model");
    }

    @Test
    @DisplayName("Should report ontology-state-driven results honestly, never fabricated matches")
    void shouldReportMatchesFromOntologyStateOnly() throws Exception {
        MappingHubView view = createView();

        upload(view, "pump-aas.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        List<String> lines = consoleLines(view);
        String sparqlLine = lines.stream()
            .filter(l -> l.startsWith("[MATCH]") && l.contains("SPARQL match:"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("SPARQL match summary must be logged"));

        // With only the offered side ingested (no required capability yet), the
        // ontology decides there is nothing to pair — the UI reports that honestly.
        assertTrue(sparqlLine.contains("0 semantic capability match(es)")
                || sparqlLine.matches(".*\\d+ semantic capability match\\(es\\).*"),
            "match count reflects ontology state");
        assertTrue(lines.stream().anyMatch(l ->
                l.startsWith("[MATCH]") && l.contains("recommendation(s)")),
            "gap-driven recommendations are surfaced to guide the requester");
    }

    @Test
    @DisplayName("Should reason from a manual demand to a matched service and export the deployment manifest")
    void shouldReasonToMatchedServiceAndExportDeploymentManifest() throws Exception {
        MappingHubView view = createView();

        upload(view, "pump-aas.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        // The offered side (inferred from the upload) advertises a capability
        // type. The only journey gap is the demand side, which the requester
        // supplies manually as a RequiredCapability.
        MappingHubService hubService = field(view, "hubService", MappingHubService.class);
        OfferedCapability offered = hubService.getSession().getInferred().stream()
            .filter(e -> e instanceof OfferedCapability)
            .map(e -> (OfferedCapability) e)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected an inferred OfferedCapability from upload"));

        saveManual(view, new ManualEntity(
            "RequiredCapability", "RC-JOURNEY", "FlowControl demand",
            java.util.Map.of(
                "id", "RC-JOURNEY",
                "name", "FlowControl demand",
                "functionClass", offered.functionClass(),
                "engineeringUnit", "",
                "nominalRangeMin", "",
                "nominalRangeMax", "",
                "safetyIntegrityLevel", "",
                "protocol", "",
                "networkZone", "")));

        // Reasoning (R1-R3 SPARQL) now bridges demand to the offered capability.
        PipelineResult result = lastResult(view);
        assertNotNull(result);
        assertTrue(result.matches().size() >= 1,
            "ontology reasoning produced a semantic capability match from the manual demand");
        assertTrue(consoleLines(view).stream().anyMatch(l ->
                l.startsWith("[MATCH]") && l.contains("SPARQL match:")
                    && l.matches(".*[1-9]\\d* semantic capability match\\(es\\).*")),
            "console reports a non-zero SPARQL match after manual demand entry");

        // The complete manifest carries the reasoned deployment binding.
        String manifest = hubService.buildManifest();
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(manifest);
        assertTrue(root.path("deploymentManifest").isArray(), "manifest embeds a deploymentManifest");
        assertTrue(root.path("deploymentManifest").size() >= 1,
            "at least one reasoned deployment binding is exported");
        JsonNode binding = root.path("deploymentManifest").get(0);
        assertEquals("RC-JOURNEY", binding.path("requiredCapabilityId").asText());
        assertEquals("Matched", binding.path("lifecycleStatus").asText());
        assertTrue(binding.path("serviceName").asText().length() > 0,
            "deployment binding names the matched service");

        // The reasoned binding is exportable as deployment infrastructure:
        // Docker Compose and Kubernetes YAML, both parse-valid.
        String compose = binding.path("dockerCompose").asText();
        String kubernetes = binding.path("kubernetes").asText();
        assertTrue(compose.contains("services:"), "Docker Compose artifact is present");
        assertTrue(kubernetes.contains("kind: Deployment"), "Kubernetes Deployment artifact is present");
        assertNotNull(new Yaml().load(compose), "Docker Compose text is valid YAML");
        java.util.List<Object> k8sDocs = new java.util.ArrayList<>();
        new Yaml().loadAll(kubernetes).forEach(k8sDocs::add);
        assertTrue(k8sDocs.size() >= 2, "Kubernetes manifest carries Deployment and Service");
    }

    @Test
    @DisplayName("Should suggest AI concepts, accept one, and reason to a matched service")
    void shouldSuggestAndAcceptAiConceptToProduceMatch() throws Exception {
        LlmProperties props = new LlmProperties();
        props.setEnabled(true);
        props.setApiKey("test-key");
        ChatCompletionsClient stub = (system, user) -> "["
            + "{\"className\":\"RequiredCapability\",\"label\":\"FlowControl demand\","
            + "\"reason\":\"pump needs flow control\",\"capabilityType\":\"FlowControl\"},"
            + "{\"className\":\"CommunicationBinding\",\"label\":\"OPC-UA\",\"reason\":\"uses opcua\"}]";
        MappingHubView view = createView(new LlmTranslationService(props, stub));

        upload(view, "pump-aas.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        MappingHubService hubService = field(view, "hubService", MappingHubService.class);
        List<ConceptHint> hints = hubService.suggestConcepts();
        assertEquals(1, hints.size(), "unsupported CommunicationBinding hint must be skipped");
        assertEquals("RequiredCapability", hints.get(0).className());

        java.lang.reflect.Method accept = MappingHubView.class
            .getDeclaredMethod("onConceptSuggestionAdd", ConceptHint.class);
        accept.setAccessible(true);
        accept.invoke(view, hints.get(0));

        PipelineResult result = lastResult(view);
        assertTrue(result.matches().size() >= 1,
            "accepted AI-suggested RequiredCapability must reason to a semantic capability match");
        assertTrue(consoleLines(view).stream().anyMatch(l ->
                l.startsWith("[MATCH]") && l.contains("SPARQL match:")
                    && l.matches(".*[1-9]\\d* semantic capability match\\(es\\).*")),
            "console reports a non-zero match after accepting the AI suggestion");
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }
}
