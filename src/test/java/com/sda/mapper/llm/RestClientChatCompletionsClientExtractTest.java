package com.sda.mapper.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestClientChatCompletionsClient should extract the assistant content")
class RestClientChatCompletionsClientExtractTest {

    private RestClientChatCompletionsClient client() {
        LlmProperties props = new LlmProperties();
        props.setApiKey("test");
        return new RestClientChatCompletionsClient(props);
    }

    @Test
    @DisplayName("Should extract content from a standard OpenAI-compatible response")
    void shouldExtractContent() {
        String response = """
                {"id":"1","choices":[{"index":0,"message":{"role":"assistant",\
                "content":"[{\\"className\\":\\"Service\\"}]"},"finish_reason":"stop"}]}
                """;
        String content = client().extractContent(response);
        assertThat(content).contains("className").contains("Service");
    }

    @Test
    @DisplayName("Should surface provider error messages")
    void shouldSurfaceError() {
        String response = """
                {"error":{"message":"invalid api key","type":"auth_error"}}
                """;
        String content = client().extractContent(response);
        assertThat(content).contains("invalid api key");
    }

    @Test
    @DisplayName("Should return empty string for null/blank response")
    void shouldHandleBlank() {
        assertThat(client().extractContent(null)).isEmpty();
        assertThat(client().extractContent("   ")).isEmpty();
    }
}
