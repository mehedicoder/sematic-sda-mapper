package com.sda.mapper.llm;

/**
 * A single suggested ontology concept produced by the LLM translation of a
 * device description.
 *
 * @param className the suggested SDA ontology class
 * @param label     a human-readable instance name
 * @param reason    why the concept applies to the input
 */
public record ConceptHint(String className, String label, String reason) {
}
