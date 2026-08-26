package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.hub.model.UseCaseRequirement;
import com.sda.mapper.hub.parser.AasModelParser;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.MtpModelParser;
import com.sda.mapper.hub.parser.OpcUaModelParser;
import com.sda.mapper.hub.parser.ParsingException;
import com.sda.mapper.hub.persistence.CanonicalTripleMapper;
import com.sda.mapper.hub.persistence.MappingHubGraphService;
import com.sda.mapper.hub.persistence.MappingHubPersistenceService;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.service.OntologyDataService;
import com.sda.mapper.llm.ChatCompletionsClient;
import com.sda.mapper.llm.ConceptHint;
import com.sda.mapper.llm.LlmNotConfiguredException;
import com.sda.mapper.llm.LlmProperties;
import com.sda.mapper.llm.LlmTranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Mapping hub pipeline service")
class MappingHubServiceTest {

    private MappingHubService createService() {
        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        return new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            matchingService,
            new SemanticMatchService(new SparqlQueryLoader()),
            recommendationService,
            new ManifestExportService(matchingService, recommendationService),
            new MappingHubSession(),
            null);
    }

    private byte[] fixture(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, "Fixture not found: " + path);
            return in.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Should run the full pipeline on an AAS JSON upload")
    void shouldRunFullPipelineOnAasUpload() {
        MappingHubService service = createService();
        PipelineResult result = service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        assertEquals("PumpModule-FT101", result.model().assets().get(0).name());
        assertEquals(1, service.getSession().getFiles().size());
        assertEquals(Set.of(AssetFileFormat.AAS_JSON), service.getSession().getUploadedFormats());
        assertEquals(62.5, result.gapReport().coveragePercent());
        assertEquals(12, result.gapReport().count(CoverageStatus.PRESENT));
        assertEquals(6, result.gapReport().count(CoverageStatus.INFERRED));
        assertEquals(6, result.gapReport().count(CoverageStatus.MISSING));
    }

    @Test
    @DisplayName("Should log every pipeline stage to the console")
    void shouldLogAllStages() {
        MappingHubService service = createService();
        PipelineResult result = service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        List<String> lines = result.consoleLines();
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[PARSE]")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[INSTANTIATE]")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[LIFT]")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[INFER]")));
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("[MATCH]")));
    }

    @Test
    @DisplayName("Should derive inferred entities such as sensors and capabilities")
    void shouldDeriveInferredEntities() {
        MappingHubService service = createService();
        PipelineResult result = service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        assertTrue(result.inferred().stream().anyMatch(e -> e.className().equals("Sensor")));
        assertTrue(result.inferred().stream().anyMatch(e -> e.className().equals("Capability")));
        assertTrue(result.inferred().stream().allMatch(e -> e.inferred()));
    }

    @Test
    @DisplayName("Should re-run the pipeline without a new file")
    void shouldReRunPipeline() {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        PipelineResult rerun = service.reRunPipeline();

        assertNull(rerun.file());
        assertEquals(1, service.getSession().getFiles().size(),
            "Re-run must not duplicate uploaded files");
        assertEquals(12, rerun.gapReport().count(CoverageStatus.PRESENT));
    }

    @Test
    @DisplayName("Should keep the model stable across re-runs")
    void shouldKeepModelStableAcrossReRuns() {
        MappingHubService service = createService();
        PipelineResult first = service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));
        PipelineResult second = service.reRunPipeline();

        assertEquals(first.model().allEntities().size(), second.model().allEntities().size());
    }

    @Test
    @DisplayName("Should build a manifest after an upload")
    void shouldBuildManifest() throws Exception {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        String manifest = service.buildManifest();
        assertTrue(manifest.contains("\"manifestVersion\""));
        assertTrue(manifest.contains("\"Asset\""));
        assertTrue(manifest.contains("62.5"));
    }

    @Test
    @DisplayName("Should reset all session state")
    void shouldReset() {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        service.reset();

        assertTrue(service.getSession().getFiles().isEmpty());
        assertTrue(service.getSession().getUploadedFormats().isEmpty());
        assertTrue(service.getSession().getModel().isEmpty());
        assertTrue(service.getSession().getInferred().isEmpty());
    }

    @Test
    @DisplayName("Should remove a single uploaded file and rebuild the model from the rest")
    void shouldRemoveUploadedFileAndRebuildModel() {
        MappingHubService service = createService();
        service.processUpload("a.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));
        service.processUpload("b.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));
        assertEquals(2, service.getSession().getFiles().size());

        PipelineResult afterRemove = service.removeUploadedFile(0);

        assertEquals(1, service.getSession().getFiles().size());
        assertFalse(afterRemove.model().isEmpty(),
            "model must be rebuilt from the remaining upload");
        assertTrue(afterRemove.model().assets().stream()
            .anyMatch(a -> "PumpModule-FT101".equals(a.name())));
    }

    @Test
    @DisplayName("Should clear the model when the last uploaded file is removed")
    void shouldClearModelWhenLastFileRemoved() {
        MappingHubService service = createService();
        service.processUpload("only.json", fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));
        assertEquals(1, service.getSession().getFiles().size());

        PipelineResult afterRemove = service.removeUploadedFile(0);

        assertTrue(service.getSession().getFiles().isEmpty());
        assertTrue(afterRemove.model().isEmpty());
    }

    @Test
    @DisplayName("Should fail with a ParsingException for unsupported files")
    void shouldRejectUnsupportedFile() {
        MappingHubService service = createService();
        assertThrows(ParsingException.class, () ->
            service.processUpload("notes.txt", "hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Should restore persisted entities into a fresh session and rebuild coverage")
    void shouldResumeSessionFromPersistence() {
        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        MappingHubPersistenceService persistence = mock(MappingHubPersistenceService.class);
        MappingHubSession hubSession = new MappingHubSession();
        UUID persistedId = UUID.randomUUID();
        Asset asset = new Asset("P1", "PumpModule-FT101", "Example GmbH", "SN-1", "Pump",
            Optional.of("pump-aas.json"), false);
        when(persistence.loadCanonicalEntities(persistedId)).thenReturn(List.of(asset));

        MappingHubService service = new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            matchingService,
            new SemanticMatchService(new SparqlQueryLoader()),
            recommendationService,
            new ManifestExportService(matchingService, recommendationService),
            hubSession,
            persistence,
            mock(CanonicalTripleMapper.class),
            mock(MappingHubGraphService.class),
            null);

        PipelineResult result = service.resumeSession(persistedId);

        assertEquals(1, hubSession.getModel().assets().size());
        assertEquals("PumpModule-FT101", hubSession.getModel().assets().get(0).name());
        assertTrue(result.gapReport().coveragePercent() > 0,
            "Coverage must be rebuilt from the restored base data");
        assertTrue(result.consoleLines().stream().anyMatch(l -> l.startsWith("[RESTORE]")));
        verify(persistence).loadCanonicalEntities(persistedId);
    }

    @Test
    @DisplayName("Should reject resume when no persisted entities exist")
    void shouldRejectResumeWithoutPersistedEntities() {
        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        MappingHubPersistenceService persistence = mock(MappingHubPersistenceService.class);
        UUID persistedId = UUID.randomUUID();
        when(persistence.loadCanonicalEntities(persistedId)).thenReturn(List.of());

        MappingHubService service = new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            matchingService,
            new SemanticMatchService(new SparqlQueryLoader()),
            recommendationService,
            new ManifestExportService(matchingService, recommendationService),
            new MappingHubSession(),
            persistence,
            mock(CanonicalTripleMapper.class),
            mock(MappingHubGraphService.class),
            null);

        assertThrows(SessionResumeException.class, () -> service.resumeSession(persistedId));
    }

    @Test
    @DisplayName("Should enrich the session model with supplementary uploads")
    void shouldEnrichWithSupplementaryUploads() {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        PipelineResult result = service.processUpload("pump-opcua.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-opcua-datasheet.json"));

        assertEquals(2, service.getSession().getFiles().size());
        assertEquals(2, service.getSession().getUploadedFormats().size());
        assertTrue(result.gapReport().forClass("Asset").orElseThrow().status()
            == CoverageStatus.PRESENT);
    }

    @Test
    @DisplayName("Should apply an edited Application/Requirements Layer entity and re-run the pipeline")
    void shouldApplyEditedL1Entity() {
        MappingHubService service = createService();
        CanonicalEntity original = new ManualEntity("UseCaseRequirement", "REQ-EDIT-1", "Original",
            java.util.Map.of("id", "REQ-EDIT-1", "name", "Original",
                "description", "old description", "priority", "High"));
        service.addManualEntity(original);
        assertEquals("Original", service.getSession().getModel().requirements().get(0).name());

        CanonicalEntity edited = new ManualEntity("UseCaseRequirement", "REQ-EDIT-1", "Edited",
            java.util.Map.of("id", "REQ-EDIT-1", "name", "Edited",
                "description", "new description", "priority", "Low",
                "requirementID", "REQ-EDIT-1", "lifecycleStatus", "Defined",
                "rationale", "Pump flow must be observable",
                "acceptanceCondition", "Flow within 0-100 l/min"));
        PipelineResult result = service.updateEntity(edited);

        UseCaseRequirement updated = service.getSession().getModel().requirements().get(0);
        assertEquals("Edited", updated.name());
        assertEquals("new description", updated.description());
        assertEquals("Low", updated.priority());
        assertEquals("REQ-EDIT-1", updated.requirementID());
        assertEquals("Defined", updated.lifecycleStatus());
        assertEquals("Pump flow must be observable", updated.rationale());
        assertEquals("Flow within 0-100 l/min", updated.acceptanceCondition());
        assertTrue(result.consoleLines().stream().anyMatch(l -> l.startsWith("[EDIT]")),
            "Editing must log an [EDIT] stage");
    }

    @Test
    @DisplayName("Should report LLM unavailable and reject suggestions when no LLM service is wired")
    void shouldRejectLlmWhenNotConfigured() {
        MappingHubService service = createService();
        assertTrue(!service.isLlmAvailable(), "LLM must be unavailable without a wired service");
        assertThrows(LlmNotConfiguredException.class, service::suggestConcepts);
        assertThrows(LlmNotConfiguredException.class,
            () -> service.addConceptSuggestion(
                new ConceptHint("RequiredCapability", "x", "y", "z")));
    }

    @Test
    @DisplayName("Should translate an upload into a concept suggestion and produce a match when accepted")
    void shouldSuggestAndApplyConceptFromLlm() {
        LlmProperties props = new LlmProperties();
        props.setEnabled(true);
        props.setApiKey("test-key");
        ChatCompletionsClient stub = (system, user) -> "["
            + "{\"className\":\"RequiredCapability\",\"label\":\"FlowControl demand\","
            + "\"reason\":\"pump needs flow control\",\"capabilityType\":\"FlowControl\"},"
            + "{\"className\":\"CommunicationBinding\",\"label\":\"OPC-UA\",\"reason\":\"uses opcua\"}]";
        MappingHubService service = createServiceWithLlm(new LlmTranslationService(props, stub));
        service.processUpload("pump-aas.json",
            fixture("/device-descriptions/use-cases/REQ-001-flowmeter-diagnostics/pump-aas.json"));

        assertTrue(service.isLlmAvailable(), "LLM must be available when configured");
        java.util.List<ConceptHint> hints = service.suggestConcepts();
        assertEquals(1, hints.size(), "unsupported CommunicationBinding hint must be skipped");
        assertEquals("RequiredCapability", hints.get(0).className());

        PipelineResult result = service.addConceptSuggestion(hints.get(0));
        assertTrue(result.matches().size() >= 1,
            "accepted AI suggestion must reason to a semantic capability match");
    }

    private MappingHubService createServiceWithLlm(LlmTranslationService llm) {
        OntologyDataService ontologyDataService = new OntologyDataService();
        MatchingService matchingService = new MatchingService(ontologyDataService);
        RecommendationService recommendationService = new RecommendationService();
        return new MappingHubService(
            new CanonicalParserRegistry(List.of(
                new AasModelParser(), new MtpModelParser(), new OpcUaModelParser())),
            new HubFileTypeDetector(),
            new LiftService(),
            new InferenceService(),
            matchingService,
            new SemanticMatchService(new SparqlQueryLoader()),
            recommendationService,
            new ManifestExportService(matchingService, recommendationService),
            new MappingHubSession(),
            llm);
    }
}
