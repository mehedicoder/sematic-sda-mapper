package com.sda.mapper.llm;

/**
 * Raised when the LLM translation feature is invoked but not configured
 * (disabled or missing API key).
 */
public class LlmNotConfiguredException extends RuntimeException {

    public LlmNotConfiguredException(String message) {
        super(message);
    }
}
