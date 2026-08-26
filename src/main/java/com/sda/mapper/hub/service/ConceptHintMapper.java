package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.llm.ConceptHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Normalises LLM {@link ConceptHint}s into the typed manual-entry model used by
 * the mapping hub pipeline.
 *
 * <p>Only classes that {@link com.sda.mapper.hub.model.ManualEntityConverter}
 * can ingest are accepted; unsupported classes are skipped with a warning so a
 * noisy model response never breaks the manual-entry path. The mapping is a
 * suggestion source only &mdash; the produced entities enter the pipeline
 * through the exact same manual-entry path as human-typed data.</p>
 */
public final class ConceptHintMapper {

    private static final Logger log = LoggerFactory.getLogger(ConceptHintMapper.class);

    /**
     * Ontology classes the manual-entry converter can ingest. Mirrors the
     * classes handled by {@code ManualEntityConverter}; any hint outside this
     * set is skipped.
     */
    private static final Set<String> SUPPORTED = Set.of(
        "Stakeholder", "UseCase", "UseCaseRequirement", "Requirement", "RequiredCapability",
        "UseCaseRequirementParameter", "RequirementParameter", "UseCaseRequirementConstraint",
        "RequirementConstraint", "VerificationCriterion", "Capability", "Sensor", "ServiceInstance",
        "DeploymentContext", "ClusterNode", "NetworkZone");

    private ConceptHintMapper() {
    }

    /**
     * Keeps only the hints whose class the manual-entry converter supports,
     * logging a warning for every skipped (unsupported) hint.
     *
     * @param hints the raw LLM hints
     * @return the supported subset, in original order
     */
    public static List<ConceptHint> filterSupported(List<ConceptHint> hints) {
        List<ConceptHint> supported = new ArrayList<>();
        for (ConceptHint hint : hints) {
            if (hint.className() != null && SUPPORTED.contains(hint.className())) {
                supported.add(hint);
            } else {
                log.warn("Skipping LLM concept hint with unsupported or unknown class '{}' (label: {})",
                    hint.className(), hint.label());
            }
        }
        return supported;
    }

    /**
     * Builds a typed {@link ManualEntity} from a supported concept hint.
     *
     * @param hint the supported concept hint (className must be in {@link #SUPPORTED})
     * @return a manual entity ready for {@code ManualEntityConverter.toTyped}
     * @throws IllegalArgumentException when the hint's class is unsupported
     */
    public static ManualEntity toManualEntity(ConceptHint hint) {
        if (hint.className() == null || !SUPPORTED.contains(hint.className())) {
            throw new IllegalArgumentException(
                "Cannot map LLM concept hint for unsupported class: " + hint.className());
        }
        String name = (hint.label() != null && !hint.label().isBlank()) ? hint.label() : hint.className();
        String id = "ai-" + UUID.randomUUID();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("id", id);
        attributes.put("name", name);
        switch (hint.className()) {
            case "RequiredCapability" -> attributes.put("functionClass",
                (hint.capabilityType() != null && !hint.capabilityType().isBlank())
                    ? hint.capabilityType() : name);
            case "Capability", "UseCaseRequirement", "Requirement" -> {
                if (hint.reason() != null && !hint.reason().isBlank()) {
                    attributes.put("description", hint.reason());
                }
            }
            default -> { /* name-only entity */ }
        }
        return new ManualEntity(hint.className(), id, name, attributes);
    }
}
