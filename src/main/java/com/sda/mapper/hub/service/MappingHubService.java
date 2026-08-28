package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.EntityOrigin;
import com.sda.mapper.hub.model.RequiredCapability;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.hub.model.Recommendation;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.sda.mapper.hub.persistence.CanonicalTripleMapper;
import com.sda.mapper.hub.persistence.MappingHubGraphService;
import com.sda.mapper.hub.persistence.MappingHubPersistenceService;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.ParsingException;
import com.sda.mapper.llm.ConceptHint;
import com.sda.mapper.llm.LlmEntityTranslationService;
import com.sda.mapper.llm.LlmGapResult;
import com.sda.mapper.llm.LlmLiftAugmentor;
import com.sda.mapper.llm.LlmLiftSuggestion;
import com.sda.mapper.llm.LlmMatchAugmentor;
import com.sda.mapper.llm.LlmNotConfiguredException;
import com.sda.mapper.llm.LlmTranslationService;
import com.sda.mapper.llm.TranslationResult;
import com.sda.mapper.observability.PipelineAuditPublisher;
import com.sda.mapper.reasoning.SemanticReasonerService;
import com.sda.mapper.reasoning.ServiceMatchResult;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import com.sda.mapper.observability.PipelineMetrics;
import com.sda.mapper.observability.PipelineStageObserver;
import com.sda.mapper.observability.PipelineTracingContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private static final Logger log = LoggerFactory.getLogger(MappingHubService.class);

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
    private final SemanticReasonerService semanticReasonerService;
    private final SparqlQueryLoader sparqlQueryLoader;

    private LlmEntityTranslationService aiEntityTranslator;
    private LlmLiftAugmentor aiLiftAugmentor;
    private LlmMatchAugmentor aiMatchAugmentor;
    private boolean runWithLlm = false;
    private List<LlmLiftSuggestion> llmLiftSuggestions = new ArrayList<>();
    private LlmGapResult llmGapResult;

    private PipelineResult lastResult;
    private String lastRawFormat;
    private String lastRawContent;

    /**
     * Opt-in LLM augmentation wiring (Phase 22). When {@code runWithLlm} is true and the
     * augmentors are available, the deterministic pipeline additionally runs the LLM-LIFT and
     * LLM-MATCH-AUGMENT steps. The default path is unchanged when this is not set.
     */
    public void setLlmAugmentors(LlmEntityTranslationService entityTranslator,
                                 LlmLiftAugmentor liftAugmentor,
                                 LlmMatchAugmentor matchAugmentor) {
        this.aiEntityTranslator = entityTranslator;
        this.aiLiftAugmentor = liftAugmentor;
        this.aiMatchAugmentor = matchAugmentor;
    }

    public void setRunWithLlm(boolean runWithLlm) {
        this.runWithLlm = runWithLlm;
    }

    public boolean isRunWithLlm() {
        return runWithLlm;
    }

    public List<LlmLiftSuggestion> getLlmLiftSuggestions() {
        return List.copyOf(llmLiftSuggestions);
    }

    public LlmGapResult getLlmGapResult() {
        return llmGapResult;
    }

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
            llmTranslationService,
            null,
            null);
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
                              LlmTranslationService llmTranslationService,
                              SemanticReasonerService semanticReasonerService,
                              SparqlQueryLoader sparqlQueryLoader) {
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
        this.semanticReasonerService = semanticReasonerService;
        this.sparqlQueryLoader = sparqlQueryLoader;
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
            if (format == AssetFileFormat.ABOX_TTL) {
                parsed = parseAndMergeAbox(fileName, content);
            } else {
                parsed = parserRegistry.parse(new ByteArrayInputStream(content), fileName, format);
            }
            markUploadedFileParsed(fileId);
        } catch (ParsingException e) {
            markUploadedFileFailed(fileId, e.getMessage());
            throw e;
        }
        logs.add("[PARSE] Extracted " + parsed.allEntities().size() + " entities from " + fileName);

        session.ingest(info, parsed, content);
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
        removeAllAboxFromSharedModel();
        deletePersistedSession();
        session.reset();
        this.lastRawFormat = null;
        this.lastRawContent = null;
    }

    /**
     * Removes a single uploaded file from the session and rebuilds the canonical
     * model from the remaining uploads, then re-runs the dynamic pipeline stages
     * so coverage and matches reflect the removal.
     *
     * @param index 0-based position of the file in the upload queue
     * @return the pipeline result after rebuilding and re-running
     */
    public PipelineResult removeUploadedFile(int index) {
        List<MappingHubSession.UploadedFile> uploads = session.getUploads();
        if (index >= 0 && index < uploads.size()
                && uploads.get(index).format() == AssetFileFormat.ABOX_TTL) {
            removeAboxFromSharedModel(uploads.get(index).content());
        }
        session.removeFileAt(index);
        rebuildModelFromUploads();
        return reRunPipeline();
    }

    /**
     * Rebuilds the accumulated canonical model by re-parsing every remaining
     * uploaded file. Required because the model is a merged accumulation with no
     * per-entity provenance, so removing one file means recomputing from the
     * others.
     */
    private void rebuildModelFromUploads() {
        session.getModel().clear();
        for (MappingHubSession.UploadedFile uf : session.getUploads()) {
            try {
                CanonicalAssetModel parsed = parserRegistry.parse(
                    new ByteArrayInputStream(uf.content()), uf.fileName(), uf.format());
                session.getModel().merge(parsed);
            } catch (ParsingException e) {
                log.warn("Skipping re-parse of {} during file removal: {}",
                    uf.fileName(), e.getMessage());
            }
        }
    }

    /**
     * Parses an ABox Turtle file, merges its triples into the shared reasoning
     * model (the Service Catalogue) so the semantic matcher can reason over the
     * uploaded supply/asset/edge individuals, and projects the declared
     * individuals onto the session's canonical model (resolving subclass types
     * to their canonical parent class) so coverage, lift, inference and the
     * manifest entity listing reflect them.
     *
     * @param fileName name of the uploaded file
     * @param content  the file content
     * @return the projected canonical model
     * @throws ParsingException when the content is not valid Turtle
     */
    private CanonicalAssetModel parseAndMergeAbox(String fileName, byte[] content) {
        Model raw = ModelFactory.createDefaultModel();
        try (InputStream in = new ByteArrayInputStream(content)) {
            raw.read(in, null, "TTL");
        } catch (Exception e) {
            throw new ParsingException(fileName, "Invalid ABox TTL: " + e.getMessage());
        }
        Model shared = sharedCatalogModel();
        if (shared != null) {
            shared.add(raw);
            log.info("Merged {} ABox triples from {} into the shared catalog model",
                raw.size(), fileName);
        } else {
            log.warn("No shared ontology model available; ABox triples from {} were not merged",
                fileName);
        }
        return parserRegistry.parse(new ByteArrayInputStream(content), fileName, AssetFileFormat.ABOX_TTL);
    }

    /**
     * Removes the triples of an uploaded ABox file from the shared reasoning
     * model, reversing {@link #parseAndMergeAbox}.
     *
     * @param content the previously uploaded file content
     */
    private void removeAboxFromSharedModel(byte[] content) {
        Model shared = sharedCatalogModel();
        if (shared == null) {
            return;
        }
        Model raw = ModelFactory.createDefaultModel();
        try (InputStream in = new ByteArrayInputStream(content)) {
            raw.read(in, null, "TTL");
        } catch (Exception e) {
            log.warn("Skipping ABox removal from shared model: {}", e.getMessage());
            return;
        }
        shared.remove(raw);
    }

    /**
     * Removes every uploaded ABox file's triples from the shared reasoning model.
     */
    private void removeAllAboxFromSharedModel() {
        Model shared = sharedCatalogModel();
        if (shared == null) {
            return;
        }
        for (MappingHubSession.UploadedFile uf : session.getUploads()) {
            if (uf.format() == AssetFileFormat.ABOX_TTL) {
                removeAboxFromSharedModel(uf.content());
            }
        }
    }

    /**
     * Returns the shared reasoning model (the Service Catalogue) used for
     * semantic matching, or {@code null} when no reasoner is wired.
     *
     * @return the shared Jena model, or {@code null}
     */
    private Model sharedCatalogModel() {
        return semanticReasonerService != null ? semanticReasonerService.getOntologyModel() : null;
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

        if (runWithLlm && aiLiftAugmentor != null && aiLiftAugmentor.isAvailable()) {
            try (PipelineStageObserver.Stage stage = stageObserver.begin("LLM-LIFT", runId, session.getId(), null)) {
                llmLiftSuggestions = aiLiftAugmentor.suggestLifts(model.allEntities());
                for (LlmLiftSuggestion suggestion : llmLiftSuggestions) {
                    String targetId = "ai-lift-" + UUID.randomUUID();
                    Map<String, Object> attributes = new LinkedHashMap<>();
                    attributes.put("id", targetId);
                    attributes.put("name", suggestion.targetName());
                    attributes.put("relation", suggestion.relation());
                    attributes.put("sourceEntityId", suggestion.entityId());
                    model.addEntity(new ManualEntity(
                        suggestion.targetClass(), targetId, suggestion.targetName(), attributes));
                }
                stage.detail("LLM proposed " + llmLiftSuggestions.size() + " lift relation(s)");
            } catch (Exception e) {
                log.warn("LLM-LIFT skipped: {}", e.getMessage());
            }
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
                if (semanticReasonerService != null) {
                    for (MappingHubSession.ActiveRequirement active : session.getActiveRequirements()) {
                        ServiceMatchResult catalogueMatch;
                        if (active.capabilityType() != null && !active.capabilityType().isBlank()) {
                            catalogueMatch = semanticReasonerService.matchManualRequirement(
                                active.id(), active.capabilityType());
                        } else {
                            catalogueMatch = semanticReasonerService.matchByRequirementId(active.id());
                        }
                        if (catalogueMatch != null && catalogueMatch.matched()) {
                            matches.add(toCapabilityMatch(catalogueMatch, active.id()));
                        }
                    }
                    for (RequiredCapability rc : model.requiredCapabilities()) {
                        if (rc.functionClass() != null && !rc.functionClass().isBlank()) {
                            ServiceMatchResult catalogueMatch = semanticReasonerService
                                .matchManualRequirement(rc.id(), rc.functionClass());
                            if (catalogueMatch != null && catalogueMatch.matched()) {
                                matches.add(toCapabilityMatch(catalogueMatch, rc.id()));
                            }
                        }
                    }
                }
            metrics.recordMatchResult(matches.isEmpty() ? "no-hit" : "hit");
            logs.add("[MATCH] SPARQL match: " + matches.size() + " semantic capability match(es)");
            for (CapabilityMatch match : matches) {
                logs.add("[MATCH] " + match.requiredCapabilityId() + " \u2192 " + match.offeredCapabilityId()
                    + " via " + match.skillId() + " \u2014 " + match.endpointUrl());
            }

            recommendations = recommendationService.recommend(
                report, session.getUploadedFormats());
            logs.add("[MATCH] " + recommendations.size() + " recommendation(s) for supplementary uploads");

            if (runWithLlm && aiMatchAugmentor != null && aiMatchAugmentor.isAvailable()) {
                try (PipelineStageObserver.Stage llmStage = stageObserver.begin("LLM-MATCH-AUGMENT", runId, session.getId(), null)) {
                    llmGapResult = aiMatchAugmentor.suggestGapDrafts(report, model.allEntities());
                    llmStage.detail("LLM proposed " + llmGapResult.drafts().size() + " gap-fill draft(s)");
                } catch (Exception e) {
                    log.warn("LLM-MATCH-AUGMENT skipped: {}", e.getMessage());
                }
            }

            stage.detail("Coverage " + String.format(java.util.Locale.ROOT, "%.1f%%", report.coveragePercent())
                + ", " + matches.size() + " SPARQL match(es), "
                + recommendations.size() + " recommendation(s)");
        }

        try (PipelineStageObserver.Stage stage = stageObserver.begin("CATALOG", runId, session.getId(), null)) {
            int projected = projectCatalog();
            stage.detail("Projected " + projected + " offered services into catalog_service");
            logs.add("[CATALOG] Persisted " + projected
                + " offered services to catalog_service (source of truth for Service Catalog view)");
        } catch (Exception e) {
            log.warn("Catalog projection skipped: {}", e.getMessage());
        }

        completeRun(runId, report);
        PipelineResult result = new PipelineResult(file, model, inferred, report, matches, recommendations, List.copyOf(logs));
        this.lastResult = result;
        return result;
    }

    /**
     * Projects the offered services (base ontology unioned with every uploaded
     * instance triple for the session) into the durable {@code catalog_service}
     * table, which the Service Catalog view reads as its source of truth. This
     * makes all uploaded services visible in the catalogue and survives restarts.
     *
     * @return the number of projected service rows
     */
    private int projectCatalog() {
        if (sparqlQueryLoader == null || graphService == null) {
            return 0;
        }
        Model unified = graphService.hydrateUnifiedGraph(session.getId());
        String queryStr = sparqlQueryLoader.loadQuery("queries/service-catalog.rq");
        Query query = QueryFactory.create(queryStr);
        List<MappingHubPersistenceService.CatalogServiceRow> rows = new ArrayList<>();
        try (QueryExecution qe = QueryExecutionFactory.create(query, unified)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                String iri = sol.contains("service") && sol.get("service").isResource()
                    ? sol.getResource("service").getURI() : null;
                if (iri == null) {
                    continue;
                }
                rows.add(new MappingHubPersistenceService.CatalogServiceRow(
                    iri,
                    getLiteral(sol, "serviceLabel"),
                    getLiteral(sol, "capabilityLabel"),
                    getLiteral(sol, "skillLabel"),
                    getLiteral(sol, "protocol"),
                    getLiteral(sol, "accessMode"),
                    getLiteral(sol, "securityMode"),
                    sol.contains("recall") && sol.get("recall").isLiteral()
                        ? sol.getLiteral("recall").getDouble() : 0.0,
                    sol.contains("latency") && sol.get("latency").isLiteral()
                        ? sol.getLiteral("latency").getInt() : 0,
                    getLiteral(sol, "connectorLabel"),
                    getLiteral(sol, "deployLabel"),
                    getLiteral(sol, "sil"),
                    getLiteral(sol, "determinism"),
                    getLiteral(sol, "networkZone")));
            }
        }
        persistenceService.replaceCatalogServices(session.getId(), rows);
        return rows.size();
    }

    private static String getLiteral(QuerySolution sol, String var) {
        if (sol.contains(var) && sol.get(var).isLiteral()) {
            return sol.getLiteral(var).getString();
        }
        return null;
    }

    private CapabilityMatch toCapabilityMatch(ServiceMatchResult r, String originalReqId) {
        String displayReq = originalReqId != null && !originalReqId.isBlank()
            ? originalReqId
            : (r.requirementId() != null ? r.requirementId().replace("manual-", "") : "unknown");
        String id = "HUB-REQ-" + displayReq;
        return new CapabilityMatch(
            id,
            displayReq,
            r.capabilityName() != null ? r.capabilityName() : "",
            r.skillName() != null ? r.skillName() : "",
            r.serviceName() != null ? r.serviceName() : "",
            r.bindingEndpointUri() != null && !r.bindingEndpointUri().isBlank()
                ? r.bindingEndpointUri()
                : (r.bindingLabel() != null ? r.bindingLabel() : ""),
            r.protocol() != null ? r.protocol() : "",
            r.networkZone() != null ? r.networkZone() : "",
            r.safetyIntegrityLevel() != null ? r.safetyIntegrityLevel() : "");
    }

    /**
     * Returns the requirements available in the Service Catalogue that the user
     * can activate for matching in the hub.
     *
     * @return the catalogue requirements, or empty when the reasoner is disabled
     */
    public List<com.sda.mapper.reasoning.SemanticReasonerService.CatalogueRequirement>
            getAvailableRequirements() {
        if (semanticReasonerService == null) {
            return List.of();
        }
        return semanticReasonerService.listCatalogueRequirements();
    }

    /**
     * Activates a catalogue requirement and re-runs the pipeline so its match
     * against the Service Catalogue is resolved and shown.
     *
     * @param requirementId the requirement identifier to activate
     * @return the pipeline result after re-running
     */
    public PipelineResult addActiveRequirement(String requirementId) {
        session.addActiveRequirement(requirementId);
        return reRunPipeline();
    }

    /**
     * Returns the capability types advertised by the offered capabilities in the
     * catalogue, for use as options when a user defines a manual requirement.
     *
     * @return the offered capability types, or empty when the reasoner is disabled
     */
    public List<String> getOfferedCapabilityTypes() {
        if (semanticReasonerService == null) {
            return List.of();
        }
        return semanticReasonerService.listOfferedCapabilityTypes();
    }

    /**
     * Registers a user-defined requirement (identifier, description and required
     * capability type) and re-runs the pipeline so it is matched against the
     * Service Catalogue via its required capability.
     *
     * @param requirementId the user-supplied requirement identifier
     * @param description   the requirement description
     * @param capabilityType the required capability type
     * @return the pipeline result after re-running
     */
    public PipelineResult addManualRequirement(String requirementId, String description,
                                              String capabilityType) {
        session.addActiveRequirement(requirementId, description, capabilityType);
        return reRunPipeline();
    }

    /**
     * Deactivates a previously activated requirement and re-runs the pipeline.
     *
     * @param requirementId the requirement identifier to deactivate
     * @return the pipeline result after re-running
     */
    public PipelineResult removeActiveRequirement(String requirementId) {
        session.removeActiveRequirement(requirementId);
        return reRunPipeline();
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
