package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.Recommendation;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.sda.mapper.hub.parser.CanonicalParserRegistry;
import com.sda.mapper.hub.parser.HubFileTypeDetector;
import com.sda.mapper.hub.parser.ParsingException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the five pipeline stages of the mapping hub:
 * Parse &rarr; Lift &rarr; Infer &rarr; Match &rarr; Manifest.
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
                             @Lazy MappingHubSession session) {
        this.parserRegistry = parserRegistry;
        this.fileTypeDetector = fileTypeDetector;
        this.liftService = liftService;
        this.inferenceService = inferenceService;
        this.matchingService = matchingService;
        this.semanticMatchService = semanticMatchService;
        this.recommendationService = recommendationService;
        this.manifestExportService = manifestExportService;
        this.session = session;
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
        logs.add("[PARSE] " + info.describe());

        CanonicalAssetModel parsed = parserRegistry.parse(
            new ByteArrayInputStream(content), fileName, format);
        logs.add("[PARSE] Extracted " + parsed.allEntities().size() + " entities from " + fileName);

        session.ingest(info, parsed);
        return runStages(info, logs);
    }

    /**
     * Re-runs Lift, Infer, Match and Manifest over the accumulated session
     * state without parsing a new file.
     *
     * @return the pipeline result
     */
    public PipelineResult reRunPipeline() {
        List<String> logs = new ArrayList<>();
        logs.add("[MANIFEST] Re-running pipeline on current session model");
        return runStages(null, logs);
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
        session.reset();
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

    private PipelineResult runStages(UploadedFileInfo file, List<String> logs) {
        CanonicalAssetModel model = session.getModel();

        List<CanonicalEntity> lifted = liftService.lift(model);
        for (CanonicalEntity entity : lifted) {
            model.addEntity(entity);
        }
        logs.add("[LIFT] Derived " + lifted.size()
            + " lifted entities (semantic concepts / information models)");

        List<CanonicalEntity> inferred = inferenceService.infer(model);
        session.setInferred(inferred);
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
        logs.add("[MANIFEST] " + recommendations.size() + " recommendation(s) for supplementary uploads");

        return new PipelineResult(file, model, inferred, report, matches, recommendations, List.copyOf(logs));
    }
}
