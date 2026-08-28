package com.sda.mapper.llm;

/**
 * One step of an agentic mapping run plan (Phase 22e).
 *
 * @param name        the LLM-augmented stage name (LLM-TRANSLATE / LLM-LIFT / LLM-REQUIRE / LLM-MATCH-AUGMENT / MANIFEST)
 * @param description  what the step produced
 */
public record LlmAgentStep(String name, String description) {
}
