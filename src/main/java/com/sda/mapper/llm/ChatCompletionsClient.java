package com.sda.mapper.llm;

/**
 * Minimal chat-completions transport used by the LLM translation feature.
 * Implementations perform the actual HTTP call to an OpenAI-compatible endpoint.
 * A stub implementation is used in tests so no external service is contacted.
 */
public interface ChatCompletionsClient {

    /**
     * Sends a system + user prompt and returns the assistant message content.
     *
     * @param systemPrompt instructions for the model
     * @param userPrompt   the device description / task content
     * @return the model's text response
     */
    String complete(String systemPrompt, String userPrompt);
}
