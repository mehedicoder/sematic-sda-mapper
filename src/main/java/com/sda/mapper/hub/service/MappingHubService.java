package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.EntityOrigin;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.Recommendation;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.sda.mapper.hub.persistence.CanonicalTripleMapper;
import com.sda.mapper.hub.persistence.MappingHubGraphService;
import com.sda.mapper.hub.persistence.MappingHubPersistenceService;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.ParsingException;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the five pipeline stages of the mapping hub:
 * Parse &rarr; Instantiate &rarr; Lift &rarr; Infer &rarr; Match.
 *
 * <p>Uploads enrich the session-scoped model incrementally; re-running the
 * pipeline recomputes lift, inference, matching and recommendations over the
 * accumulated state.</p>
 */
@Service
public class MappingHubService {

    private final CanonicalParserRegistry parserRegistry;
    private final HubFileTypeDetector fileTypeDetector;
    private final LiftService liftService;
    private final InferenceService inferenceService;
    private final MatchingService matchingService;
    private final SemanticMatchService semanticMatchService;
    private final RecommendationService recommendationService;
    private final ManifestExportService manifestExportService;
    private final MappingHubSession session;
    private final MappingHubPersistenceService persistenceService;
    private final CanonicalTripleMapper tripleMapper;
    private final MappingHubGraphService graphService;

    /**
     * Creates the mapping hub service with all pipeline stages.
     */
    public MappingHubService(CanonicalParserRegistry parserRegistry,
                             HubFileTypeDetector fileTypeDetector,
                             LiftService liftService,
                             InferenceService inferenceService,
                             MatchingService matchingService,
                             SemanticMatchService semanticMatchService,
                             RecommendationService recommendationService,
                             ManifestExportService manifestExportService,
                             MappingHubSession session) {
        this(parserRegistry, fileTypeDetector, liftService, inferenceService, matchingService,
            semanticMatchService, recommendationService, manifestExportService, session,
            null, null, null);
    }

    /**
     * Creates the mapping hub service with database-backed instance graph
     * persistence enabled.
     */
    @Autowired
    public MappingHubService(CanonicalParserRegistry parserRegistry,
                             HubFileTypeDetector fileTypeDetector,
                             LiftService liftService,
                             InferenceService inferenceService,
                             MatchingService matchingService,
                             SemanticMatchService semanticMatchService,
                             RecommendationService recommendationService,
                             ManifestExportService manifestExportService,
                             @Lazy MappingHubSession session,
                             MappingHubPersistenceService persistenceService,
                             CanonicalTripleMapper tripleMapper,
                             MappingHubGraphService graphService) {
        this.parserRegistry = parserRegistry;
        this.fileTypeDetector = fileTypeDetector;
        this.liftService = liftService;
        this.inferenceService = inferenceService;
        this.matchingService = matchingService;
        this.semanticMatchService = semanticMatchService;
        this.recommendationService = recommendationService;
        this.manifestExportService = manifestExportService;
        this.session = session;
        this.persistenceService = persistenceService;
        this.tripleMapper = tripleMapper;
        this.graphService = graphService;
    }

    /**
     * Detects the format of an uploaded file, parses it into the session model
     * and runs the full pipeline.
     *
     * @param fileName name of the uploaded file
     * @param content  the file content
     * @return the pipeline result
     * @throws ParsingException when the format is unsupported or parsing fails
     */
    public PipelineResult processUpload(String fileName, byte[] content) {
        List<String> logs = new ArrayList<>();
        AssetFileFormat format = fileTypeDetector.detect(fileName, new ByteArrayInputStream(content))
            .orElseThrow(() -> new ParsingException(fileName, "Unsupported or unrecognised file format"));
        UploadedFileInfo info = new UploadedFileInfo(fileName, format, content.length);
        UUID fileId = saveUploadedFile(info, content);
        logs.add("[PARSE] " + info.describe());

        CanonicalAssetModel parsed;
        try {
            parsed = parserRegistry.parse(new ByteArrayInputStream(content), fileName, format);
            markUploadedFileParsed(fileId);
        } catch (ParsingException e) {
            markUploadedFileFailed(fileId, e.getMessage());
            throw e;
        }
        logs.add("[PARSE] Extracted " + parsed.allEntities().size() + " entities from " + fileName);

        session.ingest(info, parsed);
        return runStages(info, logs, fileId, parsed.allEntities());
    }

    /**
     * Re-runs Lift, Infer, Match and Manifest over the accumulated session
     * state without parsing a new file.
     *
     * @return the pipeline result
     */
    public PipelineResult reRunPipeline() {
        List<String> logs = new ArrayList<>();
        logs.add("[PARSE] Re-running pipeline on current session model");
        return runStages(null, logs, null, List.of());
    }

    /**
     * Builds the JSON manifest of the current mapping state.
     *
     * @return the JSON manifest
     */
    public String buildManifest() {
        return manifestExportService.build(session.getModel(), session.getInferred(),
            session.getFiles(), session.getUploadedFormats());
    }

    /**
     * Clears all accumulated session state.
     */
    public void reset() {
        deletePersistedSession();
        session.reset();
    }

    /**
     * Adds a manually entered entity, persists it as manual RDF instance data,
     * and re-runs the dynamic stages.
     *
     * @param entity manually entered canonical entity
     * @return the updated pipeline result
     */
    public PipelineResult addManualEntity(CanonicalEntity entity) {
        session.addManualEntity(entity);
        UUID runId = startRun();
        saveEntities(runId, null, "INSTANTIATE", EntityOrigin.MANUAL, true, List.of(entity));
        List<String> logs = new ArrayList<>();
        logs.add("[INSTANTIATE] Stored manual " + entity.className() + " as RDF instance triples");
        return runStages(null, logs, null, List.of(), runId);
    }

    /**
     * Returns the session-scoped state.
     *
     * @return the mapping hub session
     */
    public MappingHubSession getSession() {
        return session;
    }

    /**
     * Returns the recommendation service used for gap analysis.
     *
     * @return the recommendation service
     */
    public RecommendationService getRecommendationService() {
        return recommendationService;
    }

    /**
     * Hydrates the transient runtime graph from the read-only ontology and
     * persisted session triples.
     *
     * @return transient unified graph
     */
    public Model hydrateUnifiedGraph() {
        if (graphService == null) {
            return org.apache.jena.rdf.model.ModelFactory.createDefaultModel();
        }
        return graphService.hydrateUnifiedGraph(session.getId());
    }

    /**
     * Restores a persisted Mapping Hub session into the current session-scoped
     * state and re-runs the dynamic pipeline stages so coverage and matches are
     * rebuilt from the restored base data.
     *
     * @param sessionId persisted session id
     * @return the rebuilt pipeline result
     * @throws SessionResumeException when persistence is unavailable or the
     *                                session has no persisted entities
     */
    public PipelineResult resumeSession(UUID sessionId) {
        if (persistenceService == null) {
            throw new SessionResumeException("Persistence is not available to resume session " + sessionId);
        }
        List<CanonicalEntity> restored = persistenceService.loadCanonicalEntities(sessionId);
        if (restored.isEmpty()) {
            throw new SessionResumeException("No persisted entities found for session " + sessionId);
        }
        session.reset();
        session.bindUser(CurrentUser.username().orElse(null));
        CanonicalAssetModel model = session.getModel();
        for (CanonicalEntity entity : restored) {
            model.addEntity(entity);
        }
        List<String> logs = new ArrayList<>();
        logs.add("[RESTORE] Restored " + restored.size() + " persisted entities from session " + sessionId);
        return runStages(null, logs, null, restored);
    }

    private PipelineResult runStages(UploadedFileInfo file, List<String> logs,
                                     UUID fileId, List<CanonicalEntity> parsedEntities) {
        return runStages(file, logs, fileId, parsedEntities, startRun());
    }

    private PipelineResult runStages(UploadedFileInfo file, List<String> logs,
                                     UUID fileId, List<CanonicalEntity> parsedEntities, UUID runId) {
        CanonicalAssetModel model = session.getModel();

        if (!parsedEntities.isEmpty()) {
            saveEntities(runId, fileId, "INSTANTIATE", EntityOrigin.PARSED, false, parsedEntities);
            logs.add("[INSTANTIATE] Stored " + parsedEntities.size()
                + " parsed entities as CSS-SDA RDF instance triples");
        } else {
            logs.add("[INSTANTIATE] No new parsed entities; using persisted session graph");
        }

        List<CanonicalEntity> lifted = liftService.lift(model);
        for (CanonicalEntity entity : lifted) {
            model.addEntity(entity);
        }
        saveEntities(runId, null, "LIFT", EntityOrigin.LIFTED, false, lifted);
        logs.add("[LIFT] Derived " + lifted.size()
            + " lifted entities (semantic concepts / information models)");

        List<CanonicalEntity> inferred = inferenceService.infer(model);
        session.setInferred(inferred);
        saveEntities(runId, null, "INFER", EntityOrigin.INFERRED, false, inferred);
        logs.add("[INFER] Inferred " + inferred.size()
            + " entities (sensors, capabilities, instances, nodes, zones)");

        GapReport report = matchingService.compute(model, inferred);
        logs.add(String.format("[MATCH] Coverage %.1f%% \u2014 %d present, %d inferred, %d partial, %d missing",
            report.coveragePercent(),
            report.count(CoverageStatus.PRESENT),
            report.count(CoverageStatus.INFERRED),
            report.count(CoverageStatus.PARTIAL),
            report.count(CoverageStatus.MISSING)));
        if (!report.blockingGaps().isEmpty()) {
            logs.add("[MATCH] " + report.blockingGaps().size() + " blocking gap(s) detected");
        }

        List<CapabilityMatch> matches = semanticMatchService.match(model, inferred);
        logs.add("[MATCH] SPARQL match: " + matches.size() + " semantic capability match(es)");
        for (CapabilityMatch match : matches) {
            logs.add("[MATCH] " + match.requiredCapabilityId() + " \u2192 " + match.offeredCapabilityId()
                + " via " + match.skillId() + " \u2014 " + match.endpointUrl());
        }

        List<Recommendation> recommendations = recommendationService.recommend(
            report, session.getUploadedFormats());
        logs.add("[MATCH] " + recommendations.size() + " recommendation(s) for supplementary uploads");

        completeRun(runId, report);
        return new PipelineResult(file, model, inferred, report, matches, recommendations, List.copyOf(logs));
    }

    private UUID startRun() {
        if (persistenceService == null) {
            return UUID.randomUUID();
        }
        ensureSessionRow();
        return persistenceService.startPipelineRun(session.getId());
    }

    private UUID saveUploadedFile(UploadedFileInfo info, byte[] content) {
        if (persistenceService == null) {
            return null;
        }
        ensureSessionRow();
        return persistenceService.saveUploadedFile(session.getId(), info, content);
    }

    private void ensureSessionRow() {
        session.bindUser(CurrentUser.username().orElse(null));
        persistenceService.ensureSession(session.getId(), session.getUserId());
    }

    private void markUploadedFileParsed(UUID fileId) {
        if (persistenceService != null && fileId != null) {
            persistenceService.markUploadedFileParsed(fileId);
        }
    }

    private void markUploadedFileFailed(UUID fileId, String message) {
        if (persistenceService != null && fileId != null) {
            persistenceService.markUploadedFileFailed(fileId, message);
        }
    }

    private void saveEntities(UUID runId, UUID fileId, String stageName, EntityOrigin origin,
                              boolean manualEntry, List<? extends CanonicalEntity> entities) {
        if (persistenceService == null || tripleMapper == null || entities.isEmpty()) {
            return;
        }
        persistenceService.saveCanonicalEntities(session.getId(), runId, fileId, stageName, origin, entities);
        persistenceService.saveTriples(tripleMapper.toTriples(session.getId(), fileId, runId,
            stageName, origin, manualEntry, entities));
    }

    private void completeRun(UUID runId, GapReport report) {
        if (persistenceService != null) {
            persistenceService.completePipelineRun(session.getId(), runId,
                report.coveragePercent(), report.blockingGaps().size());
        }
    }

    private void deletePersistedSession() {
        if (persistenceService != null) {
            persistenceService.deleteSessionData(session.getId());
        }
    }
}
