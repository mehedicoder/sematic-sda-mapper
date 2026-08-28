package com.sda.mapper.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * LLM match augmentation + gap drafts (paper: generative-AI "next steps" of
 * LLM-based automation engineering; R2a coverage fill).
 *
 * <p>Given the current {@link GapReport}, asks the LLM to propose ready-to-accept
 * entity drafts that would close PARTIAL/MISSING gaps and to explain each gap in
 * natural language. Output is validated against a fixed set of valid SDA classes;
 * the deterministic {@code SemanticReasonerService} remains the source of truth for
 * matching &mdash; these drafts are advisory and enter the model only when Accepted.</p>
 */
@Service
public class LlmMatchAugmentor {

    private static final Logger log = LoggerFactory.getLogger(LlmMatchAugmentor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> VALID_CLASSES = Set.of(
        "UseCaseRequirement", "RequiredCapability", "Capability", "Skill", "Service",
        "CommunicationBinding", "DataConnector", "Asset", "Sensor", "ProcessVariable",
        "VerificationCriterion", "Stakeholder", "DeploymentContext", "NetworkZone",
        "SemanticConcept", "ClusterNode");

    private static final String SYSTEM_PROMPT = """
            You are a semantic mapping assistant for the CSS-SDA / SDA ontology used by an \
            Industry 4.0 service mapping tool. A coverage report shows which ontology classes \
            are PARTIAL or MISSING. Propose ready-to-accept entity drafts that would fill the \
            gaps, and explain each gap in natural language.
            Return ONLY a JSON object (no prose, no markdown fences) of the form:
              { "drafts": [ { "className": "...", "name": "...", "rationale": "..." } ],
                "explanations": [ { "className": "...", "status": "PARTIAL|MISSING", \
            "explanation": "..." } ] }
            Valid className values: UseCaseRequirement, RequiredCapability, Capability, Skill, \
            Service, CommunicationBinding, DataConnector, Asset, Sensor, ProcessVariable, \
            VerificationCriterion, Stakeholder, DeploymentContext, NetworkZone, SemanticConcept, \
            ClusterNode. Only output valid classNames.
            """;

    private final LlmProperties properties;
    private final ChatCompletionsClient client;

    public LlmMatchAugmentor(LlmProperties properties, ChatCompletionsClient client) {
        this.properties = properties;
        this.client = client;
    }

    /** Whether the feature can currently be used. */
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    /**
     * Proposes gap-fill drafts + explanations for the current coverage report.
     *
     * @param report the current gap report
     * @param model  the current lifted model (context for the LLM)
     * @return draft entities + explanations (invalid/unknown classes are skipped)
     * @throws LlmNotConfiguredException if the feature is disabled or unkeyed
     */
    public LlmGapResult suggestGapDrafts(GapReport report, List<CanonicalEntity> model) {
        if (!properties.isConfigured()) {
            throw new LlmNotConfiguredException(
                "LLM match augmentation is not configured. Enable it and provide an API key.");
        }
        String gapText = describeGaps(report);
        String raw = client.complete(SYSTEM_PROMPT, "Coverage gaps:\n" + gapText);
        return parse(raw);
    }

    private String describeGaps(GapReport report) {
        StringBuilder sb = new StringBuilder();
        for (var entry : report.entries()) {
            if (entry.status() == CoverageStatus.PARTIAL || entry.status() == CoverageStatus.MISSING) {
                sb.append("- ").append(entry.className()).append(" : ").append(entry.status()).append("\n");
            }
        }
        if (sb.length() == 0) {
            sb.append("(no PARTIAL/MISSING gaps)\n");
        }
        return sb.toString();
    }

    private LlmGapResult parse(String raw) {
        List<LlmGapDraft> drafts = new ArrayList<>();
        List<LlmGapExplanation> explanations = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return new LlmGapResult(drafts, explanations);
        }
        String json = raw.trim();
        if (json.startsWith("```")) {
            int end = json.lastIndexOf("```");
            json = json.substring(json.indexOf('\n') + 1, end < 0 ? json.length() : end).trim();
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.isObject()) {
                JsonNode draftsNode = root.has("drafts") ? root.get("drafts") : null;
                if (draftsNode != null && draftsNode.isArray()) {
                    for (JsonNode n : draftsNode) {
                        String className = n.path("className").asText(null);
                        String name = n.path("name").asText(null);
                        String rationale = n.path("rationale").asText("");
                        if (className == null || name == null) {
                            continue;
                        }
                        if (!VALID_CLASSES.contains(className)) {
                            log.warn("Skipping LLM gap draft with invalid class '{}'", className);
                            continue;
                        }
                        drafts.add(new LlmGapDraft(className, name, rationale));
                    }
                }
                JsonNode explanationsNode = root.has("explanations") ? root.get("explanations") : null;
                if (explanationsNode != null && explanationsNode.isArray()) {
                    for (JsonNode n : explanationsNode) {
                        String className = n.path("className").asText(null);
                        String status = n.path("status").asText("");
                        String explanation = n.path("explanation").asText("");
                        if (className == null) {
                            continue;
                        }
                        explanations.add(new LlmGapExplanation(className, status, explanation));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse LLM gap result as JSON; returning empty", e);
        }
        log.info("LLM produced {} gap draft(s) and {} explanation(s)", drafts.size(), explanations.size());
        return new LlmGapResult(drafts, explanations);
    }
}
