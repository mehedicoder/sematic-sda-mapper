package com.sda.mapper.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LlmProperties should bind provider presets and required configuration")
class LlmPropertiesTest {

    @Test
    @DisplayName("Should default to OpenRouter with its base URL")
    void shouldDefaultToOpenRouter() {
        LlmProperties props = new LlmProperties();
        assertThat(props.provider()).isEqualTo(LlmProvider.OPENROUTER);
        assertThat(props.effectiveBaseUrl()).isEqualTo("https://openrouter.ai/api/v1");
        assertThat(props.getModel()).isEqualTo("mistralai/mistral-7b-instruct:free");
    }

    @Test
    @DisplayName("Should use configured base URL for CUSTOM provider")
    void shouldUseCustomBaseUrl() {
        LlmProperties props = new LlmProperties();
        props.setProvider("CUSTOM");
        props.setBaseUrl("https://llm.example.com/v1");
        assertThat(props.effectiveBaseUrl()).isEqualTo("https://llm.example.com/v1");
    }

    @Test
    @DisplayName("Should default Ollama to local v1 endpoint")
    void shouldDefaultOllama() {
        LlmProperties props = new LlmProperties();
        props.setProvider("OLLAMA");
        assertThat(props.effectiveBaseUrl()).isEqualTo("http://localhost:11434/v1");
    }

    @Test
    @DisplayName("Should only be configured when enabled and keyed")
    void shouldRequireEnabledAndKey() {
        LlmProperties props = new LlmProperties();
        assertThat(props.isConfigured()).isFalse();

        props.setEnabled(true);
        assertThat(props.isConfigured()).isFalse();

        props.setApiKey("sk-test");
        assertThat(props.isConfigured()).isTrue();

        props.setApiKey("   ");
        assertThat(props.isConfigured()).isFalse();
    }
}
