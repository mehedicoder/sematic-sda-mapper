package com.sda.mapper.llm;

import java.util.List;

/**
 * The run plan an agentic mapping orchestrator emits (Phase 22e).
 *
 * @param steps   the sequenced LLM-augmented stages executed
 * @param summary a one-line summary of the run
 */
public record LlmAgentRunPlan(List<LlmAgentStep> steps, String summary) {
}
