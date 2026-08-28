package com.sda.mapper.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.hub.model.ManualEntityConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LLM-based entity translation (paper &sect;4.2/&sect;4.3 &mdash; "translation based on LLMs").
 *
 * <p>Extends the Phase 18 concept-hint translator into a full pipeline stage: given a device
 * description (AutomationML / OPC UA NodeSet / AAS / PA-DIM) or a free-text requirement, it asks
 * the LLM to return SDA ontology individuals as a JSON array and converts them into typed
 * {@link CanonicalEntity} records via {@link ManualEntityConverter}. The produced entities enter
 * the deterministic mapping hub through the standard manual-entry path
 * ({@code MappingHubService.addManualEntity}), so the LLM never overrides parser/lift/match results.</p>
 *
 * <p>The transport is delegated to {@link ChatCompletionsClient} so it can be stubbed in tests
 * without contacting any provider. The feature is opt-in: it only runs when
 * {@link LlmProperties#isConfigured()} is true.</p>
 */
@Service
public class LlmEntityTranslationService {

    private static final Logger log = LoggerFactory.getLogger(LlmEntityTranslationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DEVICE_SYSTEM_PROMPT = """
            You are a semantic mapping assistant for the CSS-SDA / SDA ontology used by an \
            Industry 4.0 service mapping tool. Given a device description (AutomationML/CAEX, \
            OPC UA NodeSet, AAS, or PA-DIM), extract the SDA ontology individuals it realises.
            Return ONLY a JSON array (no prose, no markdown fences). Each element is an object:
              "className": one of Service, Skill, Capability, CommunicationBinding, DataConnector, \
            Asset, Sensor, ProcessVariable, DeploymentContext, NetworkZone, SemanticConcept
              "name": a human-readable instance name
              "attributes": an object of domain attributes for that class (e.g. \
            Service:{protocol,description}, CommunicationBinding:{protocol,endpointUrl,securityMode,\
            securityPolicy}, DataConnector:{nodeId}, RequiredCapability:{functionClass}, \
            Capability:{description})
            Only output classes and attributes from the list above.
            """;

    private static final String REQUIREMENT_SYSTEM_PROMPT = """
            You are a semantic mapping assistant for the CSS-SDA / SDA ontology used by an \
            Industry 4.0 service mapping tool. Given a free-text requirement, extract the SDA \
            ontology individuals needed to model it.
            Return ONLY a JSON array (no prose, no markdown fences). Each element is an object:
              "className": one of UseCaseRequirement, RequiredCapability, VerificationCriterion, \
            Stakeholder, Capability
              "name": a human-readable instance name
              "attributes": an object of domain attributes (UseCaseRequirement:{description,\
            rationale,acceptanceCondition}, RequiredCapability:{functionClass}, \
            VerificationCriterion:{criterion,method})
            Only output classes and attributes from the list above.
            """;

    private final LlmProperties properties;
    private final ChatCompletionsClient client;

    public LlmEntityTranslationService(LlmProperties properties, ChatCompletionsClient client) {
        this.properties = properties;
        this.client = client;
    }

    /**
     * Translates a device description into typed SDA ontology entities.
     *
     * @param format  the description format (AutomationML / OPC UA NodeSet / AAS / PA-DIM)
     * @param content the raw device description text
     * @return the usable canonical entities (unsupported classes are skipped)
     * @throws LlmNotConfiguredException if the feature is disabled or unkeyed
     */
    public List<CanonicalEntity> translateDeviceDescription(String format, String content) {
        return translate(DEVICE_SYSTEM_PROMPT,
            "Device description format: " + format + "\n\nContent:\n" + content);
    }

    /**
     * Translates a free-text requirement into typed SDA ontology entities.
     *
     * @param text the requirement text
     * @return the usable canonical entities (unsupported classes are skipped)
     * @throws LlmNotConfiguredException if the feature is disabled or unkeyed
     */
    public List<CanonicalEntity> translateRequirement(String text) {
        return translate(REQUIREMENT_SYSTEM_PROMPT, "Requirement text:\n" + text);
    }

    /** Whether the feature can currently be used. */
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    private List<CanonicalEntity> translate(String systemPrompt, String userPrompt) {
        if (!properties.isConfigured()) {
            throw new LlmNotConfiguredException(
                "LLM translation is not configured. Enable it and provide an API key.");
        }
        String raw = client.complete(systemPrompt, userPrompt);
        List<CanonicalEntity> entities = new ArrayList<>();
        for (ManualEntity manual : parseEntities(raw)) {
            try {
                entities.add(ManualEntityConverter.toTyped(manual));
            } catch (IllegalArgumentException e) {
                log.warn("Skipping LLM entity with unsupported class '{}' (label: {})",
                    manual.className(), manual.name());
            }
        }
        log.info("LLM translated input into {} usable entit(ies)", entities.size());
        return entities;
    }

    private List<ManualEntity> parseEntities(String raw) {
        List<ManualEntity> result = new ArrayList<>();
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
        try {
            JsonNode array = MAPPER.readTree(json);
            if (array.isArray()) {
                for (JsonNode node : array) {
                    String className = node.path("className").asText(null);
                    String name = node.path("name").asText(null);
                    if (className == null || name == null) {
                        continue;
                    }
                    String id = "ai-" + UUID.randomUUID();
                    Map<String, Object> attributes = new LinkedHashMap<>();
                    attributes.put("id", id);
                    attributes.put("name", name);
                    JsonNode attrNode = node.get("attributes");
                    if (attrNode != null && attrNode.isObject()) {
                        attrNode.fields().forEachRemaining(
                            entry -> attributes.put(entry.getKey(), entry.getValue().asText("")));
                    }
                    result.add(new ManualEntity(className, id, name, attributes));
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse LLM entities as JSON array; returning empty", e);
        }
        return result;
    }
}
