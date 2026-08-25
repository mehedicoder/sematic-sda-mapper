package com.sda.mapper.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible Chat Completions client used by the LLM translation feature.
 * Works against OpenRouter (default, free models), OpenAI, local Ollama, and any
 * custom OpenAI-compatible endpoint by changing {@code baseUrl} / {@code apiKey}.
 * Provider-specific headers (OpenRouter referral) are added automatically.
 */
@Component
public class RestClientChatCompletionsClient implements ChatCompletionsClient {

    private static final Logger log = LoggerFactory.getLogger(RestClientChatCompletionsClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmProperties properties;
    private final RestClient restClient;

    public RestClientChatCompletionsClient(LlmProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        this.restClient = RestClient.builder()
                .baseUrl(properties.effectiveBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        Map<String, Object> request = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)),
                "temperature", properties.getTemperature(),
                "max_tokens", properties.getMaxTokens());

        String json;
        try {
            json = MAPPER.writeValueAsString(request);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize LLM request", e);
        }

        log.debug("Calling LLM provider {} model {}", properties.provider(), properties.getModel());

        String response = restClient.post()
                .uri("/chat/completions")
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .headers(headers -> {
                    if (properties.provider().isOpenRouter()) {
                        headers.set("HTTP-Referer", properties.getReferer());
                        headers.set("X-Title", properties.getTitle());
                    }
                })
                .body(json)
                .retrieve()
                .body(String.class);

        return extractContent(response);
    }

    /** Extracts the assistant message content from an OpenAI-compatible response. */
    String extractContent(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }
        try {
            JsonNode root = MAPPER.readTree(response);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                return choices.get(0).path("message").path("content").asText("");
            }
            if (root.path("error").isObject()) {
                return "[provider error] " + root.path("error").path("message").asText(response);
            }
        } catch (Exception e) {
            log.warn("Could not parse LLM response as JSON; returning raw body", e);
        }
        return response;
    }
}
