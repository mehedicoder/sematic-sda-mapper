package com.sda.mapper.llm;

/**
 * A human-readable explanation of why a coverage gap exists and how it could be
 * closed, produced by the LLM for the hub recommendation panel.
 *
 * @param className    the ontology class that is PARTIAL/MISSING
 * @param status       the coverage status (PARTIAL / MISSING)
 * @param explanation  the natural-language explanation
 */
public record LlmGapExplanation(String className, String status, String explanation) {
}
