package com.sda.mapper.llm;

/**
 * Result of an agentic mapping run (Phase 22e): the executed plan plus the
 * resulting {@code complete-manifest.json} and its coverage.
 *
 * @param plan           the steps the agent ran
 * @param manifest       the exported {@code complete-manifest.json} body
 * @param coveragePercent the final coverage percentage
 */
public record LlmAgentResult(LlmAgentRunPlan plan, String manifest, double coveragePercent) {
}
