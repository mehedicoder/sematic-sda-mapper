package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.model.CoverageEntry;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.Recommendation;
import com.sda.mapper.hub.model.UploadedFileInfo;
import com.sda.mapper.reasoning.ReasoningTree;

import java.util.List;
import java.util.Optional;

/**
 * Result of a mapping hub pipeline run: the final model, the inferred entities,
 * the coverage report, the semantic matches, the recommendations, the console
 * log lines and the ontology reasoning trees derived during the match stage.
 *
 * @param file            the uploaded file that triggered the run, or {@code null} for re-runs
 * @param model           the canonical model after the run
 * @param inferred        the inferred entities
 * @param gapReport       the coverage report
 * @param matches         the semantic capability matches from the SPARQL match stage
 * @param recommendations the context-aware recommendations
 * @param consoleLines    console log lines for the pipeline stages
 * @param reasoningTrees  the ontology reasoning trees for each matched requirement,
 *                        in match resolution order
 */
public record PipelineResult(
    UploadedFileInfo file,
    CanonicalAssetModel model,
    List<CanonicalEntity> inferred,
    GapReport gapReport,
    List<CapabilityMatch> matches,
    List<Recommendation> recommendations,
    List<String> consoleLines,
    List<ReasoningTree> reasoningTrees
) {

    /**
     * Creates a pipeline result without reasoning trees (no ontology reasoner
     * wired into the run).
     */
    public PipelineResult(UploadedFileInfo file,
                          CanonicalAssetModel model,
                          List<CanonicalEntity> inferred,
                          GapReport gapReport,
                          List<CapabilityMatch> matches,
                          List<Recommendation> recommendations,
                          List<String> consoleLines) {
        this(file, model, inferred, gapReport, matches, recommendations, consoleLines, List.of());
    }

    /**
     * Finds the coverage entry for the given ontology class.
     *
     * @param className the ontology class name
     * @return the coverage entry, or empty when unknown
     */
    public Optional<CoverageEntry> entryFor(String className) {
        return gapReport.forClass(className);
    }
}
