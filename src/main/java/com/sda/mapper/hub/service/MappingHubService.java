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
import com.sda.mapper.llm.ConceptHint;
import com.sda.mapper.llm.LlmNotConfiguredException;
import com.sda.mapper.llm.LlmTranslationService;
import com.sda.mapper.llm.TranslationResult;
import com.sda.mapper.observability.PipelineAuditPublisher;
import com.sda.mapper.observability.PipelineMetrics;
import com.sda.mapper.observability.PipelineStageObserver;
import com.sda.mapper.observability.PipelineTracingContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
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
    private final PipelineStageObserver stageObserver;
    private final PipelineTracingContext tracingContext;
    private final PipelineAuditPublisher auditPublisher;
    private final PipelineMetrics metrics;
    private final LlmTranslationService llmTranslationService;

    private PipelineResult lastResult;
    private String lastRawFormat;
    private String lastRawContent;

    /**
     * Creates the mapping hub service with all pipeline stages and default
     * (no-op) observability — for unit tests.
     */
    public MappingHubService(CanonicalParserRegistry parserRegistry,
                             HubFileTypeDetector fileTypeDetector,
                             LiftService liftService,
                             InferenceService inferenceService,
                             MatchingService matchingService,
                             SemanticMatchService semanticMatchService,
                             RecommendationService recommendationService,
                             ManifestExportService manifestExportService,
                             MappingHubSession session,
                             LlmTranslationService llmTranslationService) {
        this(parserRegistry, fileTypeDetector, liftService, inferenceService, matchingService,
            semanticMatchService, recommendationService, manifestExportService, session,
            null, null, null, llmTranslationService);
    }

    /**
     * Creates the mapping hub service with database-backed instance graph
     * persistence enabled and no-op observability.
     */
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
                             MappingHubGraphService graphService,
                             LlmTranslationService llmTranslationService) {
        this(parserRegistry, fileTypeDetector, liftService, inferenceService, matchingService,
            semanticMatchService, recommendationService, manifestExportService, session,
            persistenceService, tripleMapper, graphService,
            new PipelineStageObserver(ObservationRegistry.NOOP,
                new PipelineAuditPublisher(null), new PipelineMetrics(new SimpleMeterRegistry())),
            new PipelineTracingContext(ObservationRegistry.NOOP),
            new PipelineAuditPublisher(null),
            new PipelineMetrics(new SimpleMeterRegistry()),
            llmTranslationService);
    }

    /**
     * Creates the fully wired mapping hub service including pipeline
     * observability (tracing, audit, metrics).
     *
     * @param stageObserver  per-stage tracing/audit instrumentation
     * @param tracingContext run-scoped correlation context
     * @param auditPublisher audit event publisher (broadcasts to live listeners)
     * @param metrics        pipeline SLI metrics
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
                             MappingHubGraphService graphService,
                             PipelineStageObserver stageObserver,
                             PipelineTracingContext tracingContext,
                             PipelineAuditPublisher auditPublisher,
                             PipelineMetrics metrics,
                             LlmTranslationService llmTranslationService) {
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
        this.stageObserver = stageObserver;
        this.tracingContext = tracingContext;
        this.auditPublisher = auditPublisher;
        this.metrics = metrics;
        this.llmTranslationService = llmTranslationService;
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

        this.lastRawFormat = format.label();
        this.lastRawContent = new String(content, StandardCharsets.UTF_8);

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
        List<CapabilityMatch> matches = lastResult != null ? lastResult.matches() : List.of();
        return manifestExportService.build(session.getModel(), session.getInferred(),
            session.getFiles(), session.getUploadedFormats(), matches);
    }

    /**
     * Clears all accumulated session state.
     */
    public void reset() {
        deletePersistedSession();
        session.reset();
        this.lastRawFormat = null;
        this.lastRawContent = null;
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
     * Applies an edited entity (written back from the editable Application/
     * Requirements Layer review) and re-runs the dynamic pipeline stages so
     * coverage and matching reflect the change.
     *
     * @param updated the edited canonical entity (same id + class as the replaced one)
     * @return the updated pipeline result
     */
    public PipelineResult updateEntity(CanonicalEntity updated) {
        session.getModel().replaceEntity(updated);
        UUID runId = startRun();
        saveEntities(runId, null, "EDIT", EntityOrigin.MANUAL, true, List.of(updated));
        List<String> logs = new ArrayList<>();
        logs.add("[EDIT] Updated " + updated.className() + " " + updated.name()
            + " — re-running pipeline with reviewed data");
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
     * Returns the audit publisher for live pipeline event listeners (UI console).
     *
     * @return the pipeline audit publisher
     */
    public PipelineAuditPublisher getAuditPublisher() {
        return auditPublisher;
    }

    /**
     * Whether the optional LLM semantic-translation feature is currently usable
     * for concept suggestions.
     *
     * @return {@code true} when an LLM translation service is wired and
     *         configured (enabled + API key present)
     */
    public boolean isLlmAvailable() {
        return llmTranslationService != null && llmTranslationService.isAvailable();
    }

    /**
     * Suggests SDA ontology concepts for the most recently uploaded device
     * description by translating it through the optional LLM feature.
     *
     * <p>The returned hints are filtered to the classes the manual-entry path
     * can ingest; unsupported classes are skipped by {@link ConceptHintMapper}.
     * This is a suggestion source only &mdash; the concepts still have to be
     * explicitly accepted through {@link #addConceptSuggestion(ConceptHint)}
     * before they enter the pipeline, so LLM output can never silently override
     * parser, lift or match results.</p>
     *
     * @return the supported concept hints derived from the last upload
     * @throws LlmNotConfiguredException when the feature is unavailable or no
     *                                    device description has been uploaded
     */
    public List<ConceptHint> suggestConcepts() {
        if (llmTranslationService == null) {
            throw new LlmNotConfiguredException(
                "LLM translation is not available in this context.");
        }
        if (lastRawContent == null) {
            throw new LlmNotConfiguredException(
                "Upload a device description before requesting AI concept suggestions.");
        }
        long start = System.nanoTime();
        try {
            TranslationResult result = llmTranslationService.translate(lastRawFormat, lastRawContent);
            List<ConceptHint> supported = ConceptHintMapper.filterSupported(result.hints());
            metrics.recordLlmSuggestion("success", (System.nanoTime() - start) / 1_000_000);
            return supported;
        } catch (RuntimeException e) {
            metrics.recordLlmSuggestion("failure", (System.nanoTime() - start) / 1_000_000);
            throw e;
        }
    }

    /**
     * Accepts an AI-suggested concept, converting it into a manual entity and
     * feeding it through the standard manual-entry pipeline path.
     *
     * @param hint the concept hint to accept
     * @return the pipeline result after re-running with the accepted concept
     * @throws LlmNotConfiguredException when the feature is not available
     * @throws IllegalArgumentException  when the hint class cannot be ingested
     */
    public PipelineResult addConceptSuggestion(ConceptHint hint) {
        if (llmTranslationService == null) {
            throw new LlmNotConfiguredException(
                "LLM translation is not available in this context.");
        }
        com.sda.mapper.hub.model.ManualEntity manual = ConceptHintMapper.toManualEntity(hint);
        com.sda.mapper.hub.model.CanonicalEntity entity =
            com.sda.mapper.hub.model.ManualEntityConverter.toTyped(manual);
        return addManualEntity(entity);
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

    /**
     * Runs the dynamic stages inside one correlated run scope so every stage
     * produces a child span and an audit transition under a single trace.
     */
    private PipelineResult runStages(UploadedFileInfo file, List<String> logs,
                                     UUID fileId, List<CanonicalEntity> parsedEntities, UUID runId) {
        UUID effectiveRunId = runId != null ? runId : UUID.randomUUID();
        String sessionId = session.getId().toString();
        try (PipelineTracingContext.RunScope run =
                 tracingContext.openRun(effectiveRunId.toString(), sessionId, null)) {
            try {
                PipelineResult result = executeStages(file, logs, fileId, parsedEntities, effectiveRunId);
                metrics.recordRun("hub", "success");
                return result;
            } catch (RuntimeException e) {
                metrics.recordRun("hub", "failure");
                throw e;
            }
        }
    }

    private PipelineResult executeStages(UploadedFileInfo file, List<String> logs,
                                         UUID fileId, List<CanonicalEntity> parsedEntities, UUID runId) {
        CanonicalAssetModel model = session.getModel();

        try (PipelineStageObserver.Stage ignored = stageObserver.begin("INSTANTIATE", runId, session.getId(), null)) {
            if (!parsedEntities.isEmpty()) {
                saveEntities(runId, fileId, "INSTANTIATE", EntityOrigin.PARSED, false, parsedEntities);
                logs.add("[INSTANTIATE] Stored " + parsedEntities.size()
                    + " parsed entities as CSS-SDA RDF instance triples");
            } else {
                logs.add("[INSTANTIATE] No new parsed entities; using persisted session graph");
            }
        }

        List<CanonicalEntity> lifted;
        try (PipelineStageObserver.Stage stage = stageObserver.begin("LIFT", runId, session.getId(), null)) {
            lifted = liftService.lift(model);
            for (CanonicalEntity entity : lifted) {
                model.addEntity(entity);
            }
            saveEntities(runId, null, "LIFT", EntityOrigin.LIFTED, false, lifted);
            String detail = "Derived " + lifted.size() + " lifted entities";
            stage.detail(detail);
            logs.add("[LIFT] Derived " + lifted.size()
                + " lifted entities (semantic concepts / information models)");
        }

        List<CanonicalEntity> inferred;
        try (PipelineStageObserver.Stage stage = stageObserver.begin("INFER", runId, session.getId(), null)) {
            inferred = inferenceService.infer(model);
            session.setInferred(inferred);
            saveEntities(runId, null, "INFER", EntityOrigin.INFERRED, false, inferred);
            String detail = "Inferred " + inferred.size() + " entities";
            stage.detail(detail);
            logs.add("[INFER] Inferred " + inferred.size()
                + " entities (sensors, capabilities, instances, nodes, zones)");
        }

        GapReport report;
        List<CapabilityMatch> matches;
        List<Recommendation> recommendations;
        try (PipelineStageObserver.Stage stage = stageObserver.begin("MATCH", runId, session.getId(), null)) {
            report = matchingService.compute(model, inferred);
            logs.add(String.format("[MATCH] Coverage %.1f%% \u2014 %d present, %d inferred, %d partial, %d missing",
                report.coveragePercent(),
                report.count(CoverageStatus.PRESENT),
                report.count(CoverageStatus.INFERRED),
                report.count(CoverageStatus.PARTIAL),
                report.count(CoverageStatus.MISSING)));
            if (!report.blockingGaps().isEmpty()) {
                logs.add("[MATCH] " + report.blockingGaps().size() + " blocking gap(s) detected");
            }

            matches = semanticMatchService.match(model, inferred);
            metrics.recordMatchResult(matches.isEmpty() ? "no-hit" : "hit");
            logs.add("[MATCH] SPARQL match: " + matches.size() + " semantic capability match(es)");
            for (CapabilityMatch match : matches) {
                logs.add("[MATCH] " + match.requiredCapabilityId() + " \u2192 " + match.offeredCapabilityId()
                    + " via " + match.skillId() + " \u2014 " + match.endpointUrl());
            }

            recommendations = recommendationService.recommend(
                report, session.getUploadedFormats());
            logs.add("[MATCH] " + recommendations.size() + " recommendation(s) for supplementary uploads");

            stage.detail("Coverage " + String.format(java.util.Locale.ROOT, "%.1f%%", report.coveragePercent())
                + ", " + matches.size() + " SPARQL match(es), "
                + recommendations.size() + " recommendation(s)");
        }

        completeRun(runId, report);
        PipelineResult result = new PipelineResult(file, model, inferred, report, matches, recommendations, List.copyOf(logs));
        this.lastResult = result;
        return result;
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
