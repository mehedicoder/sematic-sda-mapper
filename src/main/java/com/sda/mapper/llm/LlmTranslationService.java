package com.sda.mapper.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM-based semantic translation (paper p.7-8, p.12): maps a device description
 * in AutomationML / OPC UA NodeSet / AAS format to candidate SDA ontology
 * concepts. The actual model call is delegated to a {@link ChatCompletionsClient}
 * so the transport can be stubbed in tests without contacting any provider.
 *
 * <p>The feature is opt-in: it only runs when {@link LlmProperties#isConfigured()}
 * is true (enabled + API key present). The default provider is OpenRouter with a
 * free model.</p>
 */
@Service
public class LlmTranslationService {

    private static final Logger log = LoggerFactory.getLogger(LlmTranslationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            You are a semantic mapping assistant for the CSS-SDA / SDA ontology used by an \
            Industry 4.0 service mapping tool. Given a device description (AutomationML/CAEX, \
            OPC UA NodeSet, or AAS format), identify the SDA ontology concepts it realises.
            Return ONLY a JSON array (no prose, no markdown fences) where each element is an \
            object with: "className" (an SDA ontology class such as Capability, Skill, Service, \
            CommunicationBinding, DataConnector, Asset, Sensor, ProcessVariable, Observation, or \
            DeploymentContext), "label" (a human-readable instance name), and "reason" \
            (why this concept applies to the input).
            """;

    private final LlmProperties properties;
    private final ChatCompletionsClient client;

    public LlmTranslationService(LlmProperties properties, ChatCompletionsClient client) {
        this.properties = properties;
        this.client = client;
    }

    /**
     * Translates a device description into candidate SDA ontology concepts.
     *
     * @param format the description format (AutomationML / OPC UA NodeSet / AAS)
     * @param content the raw device description text
     * @return the translation result with parsed concept hints
     * @throws LlmNotConfiguredException if the feature is disabled or unkeyed
     */
    public TranslationResult translate(String format, String content) {
        if (!properties.isConfigured()) {
            throw new LlmNotConfiguredException(
                    "LLM translation is not configured. Enable it and provide an API key.");
        }

        String userPrompt = "Device description format: " + format
                + "\n\nContent:\n" + content;
        String raw = client.complete(SYSTEM_PROMPT, userPrompt);
        List<ConceptHint> hints = parseHints(raw);

        log.info("LLM translated {} description into {} concept hint(s)", format, hints.size());
        return new TranslationResult(format, properties.getModel(), hints, raw);
    }

    /** Whether the feature can currently be used. */
    public boolean isAvailable() {
        return properties.isConfigured();
    }

    private List<ConceptHint> parseHints(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String json = raw.trim();
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end <= start) {
            return List.of();
        }
        json = json.substring(start, end + 1);
        try {
            JsonNode array = MAPPER.readTree(json);
            List<ConceptHint> hints = new ArrayList<>();
            if (array.isArray()) {
                for (JsonNode node : array) {
                    hints.add(new ConceptHint(
                            node.path("className").asText(null),
                            node.path("label").asText(null),
                            node.path("reason").asText(null)));
                }
            }
            return hints;
        } catch (Exception e) {
            log.warn("Could not parse LLM concept hints as JSON array; returning empty", e);
            return List.of();
        }
    }
}
