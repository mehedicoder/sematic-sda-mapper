package com.sda.mapper.llm;

import java.util.List;

/**
 * Combined LLM match-augmentation output: ready-to-accept gap-fill drafts plus
 * human-readable explanations for each gap.
 *
 * @param drafts       entities the LLM suggests creating to close gaps
 * @param explanations per-gap natural-language explanations
 */
public record LlmGapResult(List<LlmGapDraft> drafts, List<LlmGapExplanation> explanations) {
}
