package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageEntry;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.Recommendation;
import com.sda.mapper.hub.model.UploadedFileInfo;

import java.util.List;
import java.util.Optional;

/**
 * Result of a mapping hub pipeline run: the final model, the inferred entities,
 * the coverage report, the recommendations and the console log lines.
 *
 * @param file            the uploaded file that triggered the run, or {@code null} for re-runs
 * @param model           the canonical model after the run
 * @param inferred        the inferred entities
 * @param gapReport       the coverage report
 * @param recommendations the context-aware recommendations
 * @param consoleLines    console log lines for the pipeline stages
 */
public record PipelineResult(
    UploadedFileInfo file,
    CanonicalAssetModel model,
    List<CanonicalEntity> inferred,
    GapReport gapReport,
    List<Recommendation> recommendations,
    List<String> consoleLines
) {

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
