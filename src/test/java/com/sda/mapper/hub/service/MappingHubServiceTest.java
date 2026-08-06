package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.CoverageStatus;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            new MappingHubSession());
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
            fixture("/device-descriptions/aas/pump-aas.json"));

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
            fixture("/device-descriptions/aas/pump-aas.json"));

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
            fixture("/device-descriptions/aas/pump-aas.json"));

        assertTrue(result.inferred().stream().anyMatch(e -> e.className().equals("Sensor")));
        assertTrue(result.inferred().stream().anyMatch(e -> e.className().equals("Capability")));
        assertTrue(result.inferred().stream().allMatch(e -> e.inferred()));
    }

    @Test
    @DisplayName("Should re-run the pipeline without a new file")
    void shouldReRunPipeline() {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json",
            fixture("/device-descriptions/aas/pump-aas.json"));

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
            fixture("/device-descriptions/aas/pump-aas.json"));
        PipelineResult second = service.reRunPipeline();

        assertEquals(first.model().allEntities().size(), second.model().allEntities().size());
    }

    @Test
    @DisplayName("Should build a manifest after an upload")
    void shouldBuildManifest() throws Exception {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json",
            fixture("/device-descriptions/aas/pump-aas.json"));

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
            fixture("/device-descriptions/aas/pump-aas.json"));

        service.reset();

        assertTrue(service.getSession().getFiles().isEmpty());
        assertTrue(service.getSession().getUploadedFormats().isEmpty());
        assertTrue(service.getSession().getModel().isEmpty());
        assertTrue(service.getSession().getInferred().isEmpty());
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
            mock(MappingHubGraphService.class));

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
            mock(MappingHubGraphService.class));

        assertThrows(SessionResumeException.class, () -> service.resumeSession(persistedId));
    }

    @Test
    @DisplayName("Should enrich the session model with supplementary uploads")
    void shouldEnrichWithSupplementaryUploads() {
        MappingHubService service = createService();
        service.processUpload("pump-aas.json",
            fixture("/device-descriptions/aas/pump-aas.json"));

        PipelineResult result = service.processUpload("pump-opcua.json",
            fixture("/device-descriptions/opcua/pump-opcua-datasheet.json"));

        assertEquals(2, service.getSession().getFiles().size());
        assertEquals(2, service.getSession().getUploadedFormats().size());
        assertTrue(result.gapReport().forClass("Asset").orElseThrow().status()
            == CoverageStatus.PRESENT);
    }
}
