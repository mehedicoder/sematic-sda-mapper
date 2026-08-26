package com.sda.mapper.observability;

import com.sda.mapper.reasoning.ServiceMatchResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Derives per-dimension pass/fail tags for a match result, mirroring the
 * dimensions of {@link com.sda.mapper.reasoning.ExplanationService} so that
 * trace tags and user-facing explanations always agree.
 *
 * <p>This is observability-only classification of an already-computed result —
 * matching decisions themselves remain in SPARQL.</p>
 */
@Component
public class MatchDimensionTagger {

    /** Span-attribute prefix used for dimension tags. */
    public static final String TAG_PREFIX = "match.dimension.";

    /**
     * Computes the dimension map for a match result.
     *
     * @param result the match result to classify
     * @return ordered dimension → pass/fail map (never {@code null})
     */
    public Map<String, String> dimensions(ServiceMatchResult result) {
        Map<String, String> dims = new LinkedHashMap<>();
        if (result == null) {
            return dims;
        }
        boolean matched = result.matched();
        dims.put("capability", passFail(matched, result.capabilityName() != null && !result.capabilityName().isBlank()));
        dims.put("protocol", passFail(matched, result.protocol() != null && !result.protocol().isBlank()));
        dims.put("accessMode", passFail(matched, result.accessMode() != null && !result.accessMode().isBlank()));
        dims.put("deployment", passFail(matched, result.deploymentContext() != null && !result.deploymentContext().isBlank()));
        dims.put("recall", passFail(matched, result.recall() > 0));
        dims.put("latency", passFail(matched, result.latencyMs() > 0));
        dims.put("r2a-concept", passFail(matched,
            result.explanation() == null || !result.explanation().toLowerCase().contains("concept")));
        return dims;
    }

    /**
     * Renders the failed dimensions as a compact summary for audit detail fields.
     *
     * @param result the match result to classify
     * @return e.g. {@code "failed dimensions: protocol, deployment"} or {@code null} when nothing failed
     */
    public String failureSummary(ServiceMatchResult result) {
        StringJoiner failed = new StringJoiner(", ");
        dimensions(result).forEach((dim, status) -> {
            if ("fail".equals(status)) {
                failed.add(dim);
            }
        });
        return failed.length() == 0 ? null : "failed dimensions: " + failed;
    }

    private static String passFail(boolean matched, boolean positiveSignal) {
        if (matched && positiveSignal) {
            return "pass";
        }
        return positiveSignal ? "pass" : "fail";
    }
}
