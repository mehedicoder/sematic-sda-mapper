package com.sda.mapper.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LlmTranslationService should translate device descriptions into ontology concepts")
class LlmTranslationServiceTest {

    private static final String SAMPLE_JSON = """
            [
              {"className":"Service","label":"Pump Analytics Service","reason":"Exposes an analytic function"},
              {"className":"CommunicationBinding","label":"OPC-UA Binding","reason":"Uses OPC-UA protocol"}
            ]
            """;

    private LlmProperties enabledProps() {
        LlmProperties props = new LlmProperties();
        props.setEnabled(true);
        props.setApiKey("test-key");
        props.setModel("openrouter/free");
        return props;
    }

    @Test
    @DisplayName("Should parse concept hints from a JSON model response")
    void shouldParseConceptHints() {
        ChatCompletionsClient stub = (system, user) -> SAMPLE_JSON;
        LlmTranslationService service = new LlmTranslationService(enabledProps(), stub);

        TranslationResult result = service.translate("AutomationML", "<CAEX>...</CAEX>");

        assertThat(result.hints()).hasSize(2);
        assertThat(result.hints().get(0).className()).isEqualTo("Service");
        assertThat(result.hints().get(0).label()).isEqualTo("Pump Analytics Service");
        assertThat(result.model()).isEqualTo("openrouter/free");
    }

    @Test
    @DisplayName("Should tolerate non-JSON model output and return no hints")
    void shouldTolerateNonJson() {
        ChatCompletionsClient stub = (system, user) -> "Sure, here are some concepts.";
        LlmTranslationService service = new LlmTranslationService(enabledProps(), stub);

        TranslationResult result = service.translate("AAS", "{}");

        assertThat(result.hints()).isEmpty();
        assertThat(result.raw()).contains("concepts");
    }

    @Test
    @DisplayName("Should throw when the feature is not configured")
    void shouldThrowWhenNotConfigured() {
        LlmProperties props = new LlmProperties(); // disabled, no key
        LlmTranslationService service = new LlmTranslationService(props, (s, u) -> "[]");

        assertThatThrownBy(() -> service.translate("AAS", "{}"))
                .isInstanceOf(LlmNotConfiguredException.class);
    }

    @Test
    @DisplayName("Should report availability based on configuration")
    void shouldReportAvailability() {
        LlmProperties props = enabledProps();
        LlmTranslationService service = new LlmTranslationService(props, (s, u) -> "[]");
        assertThat(service.isAvailable()).isTrue();

        props.setApiKey("");
        assertThat(service.isAvailable()).isFalse();
    }
}
