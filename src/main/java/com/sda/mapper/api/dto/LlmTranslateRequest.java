package com.sda.mapper.api.dto;

/**
 * Request body for the LLM translation endpoint.
 *
 * @param format  the device description format (AutomationML / OPC UA NodeSet / AAS)
 * @param content the raw device description text
 */
public record LlmTranslateRequest(String format, String content) {
}
