package com.sda.mapper.llm;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LLM mapping agent (agentic, paper Summary &sect;5: "LLM orchestrated automation
 * ... Agentic AI"). Orchestrates the Phase 22 sub-steps (22a translate &rarr; 22b
 * lift &rarr; 22d match-augment) over a whole use case, reusing the deterministic
 * {@link MappingHubService} pipeline as the execution engine. Every proposed entity
 * is materialised through the same manual-entry path as human-typed data
 * ({@code addManualEntity}); the deterministic matcher remains authoritative.
 *
 * <p>Invoked only via an explicit "Run with AI agent" action &mdash; the deterministic
 * fallback (plain {@code reRunPipeline}) is always available.</p>
 */
@Service
public class LlmMappingAgent {

    private static final Logger log = LoggerFactory.getLogger(LlmMappingAgent.class);

    private final LlmEntityTranslationService entityTranslator;
    private final LlmLiftAugmentor liftAugmentor;
    private final LlmMatchAugmentor matchAugmentor;
    private final LlmProperties properties;

    public LlmMappingAgent(LlmEntityTranslationService entityTranslator,
                           LlmLiftAugmentor liftAugmentor,
                           LlmMatchAugmentor matchAugmentor,
                           LlmProperties properties) {
        this.entityTranslator = entityTranslator;
        this.liftAugmentor = liftAugmentor;
        this.matchAugmentor = matchAugmentor;
        this.properties = properties;
    }

    /** Whether the agent can run (all sub-services configured). */
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    /**
     * Runs the LLM-augmented mapping end-to-end for a use-case description.
     *
     * @param hub            the mapping hub whose pipeline executes each step
     * @param useCaseText    the device description or free-text requirement
     * @param isRequirement  whether {@code useCaseText} is a free-text requirement
     * @return the executed plan + exported manifest + coverage
     * @throws LlmNotConfiguredException if the feature is disabled or unkeyed
     */
    public LlmAgentResult run(MappingHubService hub, String useCaseText, boolean isRequirement) {
        if (!properties.isConfigured()) {
            throw new LlmNotConfiguredException(
                "LLM mapping agent is not configured. Enable it and provide an API key.");
        }
        List<LlmAgentStep> steps = new ArrayList<>();

        List<CanonicalEntity> entities = isRequirement
            ? entityTranslator.translateRequirement(useCaseText)
            : entityTranslator.translateDeviceDescription("AutomationML", useCaseText);
        for (CanonicalEntity entity : entities) {
            hub.addManualEntity(entity);
        }
        steps.add(new LlmAgentStep("LLM-TRANSLATE",
            "Translated " + entities.size() + " entities from the use-case text"));
        hub.reRunPipeline();

        List<CanonicalEntity> model = hub.getSession().getModel().allEntities();
        List<LlmLiftSuggestion> lifts = liftAugmentor.suggestLifts(model);
        for (LlmLiftSuggestion suggestion : lifts) {
            String targetId = "ai-lift-" + UUID.randomUUID();
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("id", targetId);
            attributes.put("name", suggestion.targetName());
            attributes.put("relation", suggestion.relation());
            attributes.put("sourceEntityId", suggestion.entityId());
            hub.addManualEntity(new ManualEntity(
                suggestion.targetClass(), targetId, suggestion.targetName(), attributes));
        }
        steps.add(new LlmAgentStep("LLM-LIFT",
            "Proposed " + lifts.size() + " missing lift relation(s)"));
        PipelineResult result = hub.reRunPipeline();

        LlmGapResult gaps = matchAugmentor.suggestGapDrafts(
            result.gapReport(), result.model().allEntities());
        steps.add(new LlmAgentStep("LLM-MATCH-AUGMENT",
            "Proposed " + gaps.drafts().size() + " gap-fill draft(s)"));

        String manifest = hub.buildManifest();
        steps.add(new LlmAgentStep("MANIFEST", "Exported complete-manifest.json"));

        double coverage = result.gapReport().coveragePercent();
        log.info("LLM mapping agent completed: {} step(s), coverage {:.1f}%", steps.size(), coverage);
        return new LlmAgentResult(
            new LlmAgentRunPlan(steps, "LLM-augmented mapping completed"), manifest, coverage);
    }
}
