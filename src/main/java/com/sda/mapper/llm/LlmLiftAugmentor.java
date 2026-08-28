package com.sda.mapper.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.hub.model.CanonicalEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LLM lift augmentor (paper &sect;4.3 &mdash; "Capability/Skill mapping based on LLM", and
 * constraints "using LLMs, especially for Skill ... and AutomationML").
 *
 * <p>For entities the deterministic {@code LiftService} leaves unlifted (no
 * {@code mapsToCapability}/{@code realisedBy}/{@code exposedAs}/{@code operatesOn}), this service
 * asks the LLM to propose the missing object properties. The result is a list of
 * {@link LlmLiftSuggestion} drafts &mdash; human-in-the-loop, never silently applied. The transport
 * is delegated to {@link ChatCompletionsClient} so it can be stubbed in tests.</p>
 */
@Service
public class LlmLiftAugmentor {

    private static final Logger log = LoggerFactory.getLogger(LlmLiftAugmentor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> VALID_RELATIONS = Set.of(
        "mapsToCapability", "realisedBy", "exposedAs", "operatesOn");

    private static final String SYSTEM_PROMPT = """
            You are a semantic mapping assistant for the CSS-SDA / SDA ontology used by an \
            Industry 4.0 service mapping tool. Some already-lifted ontology entities are missing \
            their linking object properties (relations). Propose the missing, ontologically valid \
            relations so the model is connected.
            Return ONLY a JSON array (no prose, no markdown fences). Each element is an object:
              "entityId": the id of the source entity (exactly as given in the input)
              "entityClass": the className of the source entity
              "relation": one of mapsToCapability, realisedBy, exposedAs, operatesOn
              "targetClass": the class of the target individual (Capability, Skill, Service, \
            ProcessVariable, Asset, DataConnector, CommunicationBinding)
              "targetName": a human-readable name for the target individual to create/link
              "rationale": a short reason for the proposal
            Ontological validity rules:
              - Skill mapsToCapability Capability
              - Service realisedBy Skill
              - CommunicationBinding exposedAs Service
              - DataConnector operatesOn Asset or ProcessVariable
            Only output relations from that list and only for entityIds present in the input.
            """;

    private final LlmProperties properties;
    private final ChatCompletionsClient client;

    public LlmLiftAugmentor(LlmProperties properties, ChatCompletionsClient client) {
        this.properties = properties;
        this.client = client;
    }

    /** Whether the feature can currently be used. */
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    /**
     * Proposes missing lift relations for the given entities.
     *
     * @param entities the already-lifted entities to augment
     * @return draft lift suggestions (invalid/unknown entries are skipped)
     * @throws LlmNotConfiguredException if the feature is disabled or unkeyed
     */
    public List<LlmLiftSuggestion> suggestLifts(List<CanonicalEntity> entities) {
        if (!properties.isConfigured()) {
            throw new LlmNotConfiguredException(
                "LLM lift augmentation is not configured. Enable it and provide an API key.");
        }
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        String payload = describe(entities);
        String raw = client.complete(SYSTEM_PROMPT, "Existing entities:\n" + payload);
        return parse(raw, entities);
    }

    private String describe(List<CanonicalEntity> entities) {
        StringBuilder sb = new StringBuilder();
        for (CanonicalEntity e : entities) {
            sb.append("- id=").append(e.id())
                .append(", class=").append(e.className())
                .append(", name=").append(e.name())
                .append("\n");
        }
        return sb.toString();
    }

    private List<LlmLiftSuggestion> parse(String raw, List<CanonicalEntity> entities) {
        List<LlmLiftSuggestion> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        String json = raw.trim();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return result;
        }
        json = json.substring(start, end + 1);
        Set<String> knownIds = new HashSet<>();
        for (CanonicalEntity e : entities) {
            knownIds.add(e.id());
        }
        try {
            JsonNode array = MAPPER.readTree(json);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    String entityId = node.path("entityId").asText(null);
                    String entityClass = node.path("entityClass").asText(null);
                    String relation = node.path("relation").asText(null);
                    String targetClass = node.path("targetClass").asText(null);
                    String targetName = node.path("targetName").asText(null);
                    String rationale = node.path("rationale").asText("");
                    if (entityId == null || relation == null || targetClass == null || targetName == null) {
                        continue;
                    }
                    if (!VALID_RELATIONS.contains(relation)) {
                        log.warn("Skipping LLM lift suggestion with invalid relation '{}'", relation);
                        continue;
                    }
                    if (!knownIds.contains(entityId)) {
                        log.warn("Skipping LLM lift suggestion for unknown entityId '{}'", entityId);
                        continue;
                    }
                    result.add(new LlmLiftSuggestion(
                        entityId, entityClass, relation, targetClass, targetName, rationale));
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse LLM lift suggestions as JSON array; returning empty", e);
        }
        log.info("LLM produced {} lift suggestion(s)", result.size());
        return result;
    }
}
