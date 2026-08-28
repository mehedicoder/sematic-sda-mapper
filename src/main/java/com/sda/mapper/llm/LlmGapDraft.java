package com.sda.mapper.llm;

/**
 * A ready-to-accept entity draft the LLM proposes to fill a coverage gap
 * (paper: generative-AI "next steps" of automated engineering).
 *
 * @param className the ontology class to create (must be a valid SDA class)
 * @param name      the display name for the drafted individual
 * @param rationale short natural-language justification
 */
public record LlmGapDraft(String className, String name, String rationale) {
}
