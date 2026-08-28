package com.sda.mapper.llm;

/**
 * A draft relation the LLM proposes during the LIFT augmentation stage (paper:
 * "Capability/Skill mapping based on LLM [14]"). The source entity already exists
 * in the hub model; the LLM proposes a missing object property linking it to a
 * target individual. Human-in-the-loop: the suggestion must be Accepted before it
 * enters the model via the manual-entry path.
 *
 * @param entityId    id of the source entity (must already exist in the model)
 * @param entityClass className of the source entity
 * @param relation    proposed object property (mapsToCapability / realisedBy / exposedAs / operatesOn)
 * @param targetClass className of the target individual to link/create
 * @param targetName  human-readable name for the target individual
 * @param rationale   short natural-language justification
 */
public record LlmLiftSuggestion(
        String entityId,
        String entityClass,
        String relation,
        String targetClass,
        String targetName,
        String rationale) {
}
