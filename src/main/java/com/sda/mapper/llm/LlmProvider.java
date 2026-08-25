package com.sda.mapper.llm;

import java.util.Objects;

/**
 * LLM provider presets. Every provider is accessed through the OpenAI-compatible
 * Chat Completions API, so a single client implementation serves OpenRouter,
 * OpenAI, local Ollama, and any custom OpenAI-compatible endpoint.
 */
public enum LlmProvider {

    OPENROUTER("https://openrouter.ai/api/v1"),
    OPENAI("https://api.openai.com/v1"),
    OLLAMA("http://localhost:11434/v1"),
    AZURE(null),
    CUSTOM(null);

    private final String defaultBaseUrl;

    LlmProvider(String defaultBaseUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
    }

    /** Default API base URL for this provider, or {@code null} if custom. */
    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    /** Whether this provider requires the OpenRouter referral headers. */
    public boolean isOpenRouter() {
        return this == OPENROUTER;
    }

    public static LlmProvider from(String value) {
        if (value == null) return OPENROUTER;
        for (LlmProvider p : values()) {
            if (p.name().equalsIgnoreCase(value.trim())) {
                return p;
            }
        }
        return CUSTOM;
    }

    public String effectiveBaseUrl(String configuredBaseUrl) {
        return Objects.requireNonNullElse(configuredBaseUrl, defaultBaseUrl());
    }
}
