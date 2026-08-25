package com.sda.mapper.llm;

import java.util.List;

/**
 * Result of an LLM-based semantic translation of a device description.
 *
 * @param format   the input description format (AutomationML / OPC UA NodeSet / AAS)
 * @param model    the LLM model that produced the result
 * @param hints    suggested ontology concepts
 * @param raw      the raw model response (for transparency / debugging)
 */
public record TranslationResult(String format, String model, List<ConceptHint> hints, String raw) {
}
