package com.sda.mapper.llm;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.GapReport;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Higher-level entry point for the hub recommendation panel (Phase 22d): given a
 * {@link GapReport} it delegates to {@link LlmMatchAugmentor} and returns the
 * ready-to-accept gap-fill drafts plus natural-language explanations. The surfaced
 * drafts must be Accepted through the existing manual-entry path before they enter
 * the deterministic model.
 */
@Service
public class LlmGapDraftService {

    private final LlmMatchAugmentor augmentor;
    private final LlmProperties properties;

    public LlmGapDraftService(LlmMatchAugmentor augmentor, LlmProperties properties) {
        this.augmentor = augmentor;
        this.properties = properties;
    }

    /** Whether the feature can currently be used. */
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    /**
     * Explains and drafts fills for the current coverage gaps.
     *
     * @param report the current gap report
     * @param model  the current lifted model (context)
     * @return draft entities + explanations
     * @throws LlmNotConfiguredException if the feature is disabled or unkeyed
     */
    public LlmGapResult explainGaps(GapReport report, List<CanonicalEntity> model) {
        if (!properties.isConfigured()) {
            throw new LlmNotConfiguredException(
                "LLM gap drafting is not configured. Enable it and provide an API key.");
        }
        return augmentor.suggestGapDrafts(report, model);
    }
}
