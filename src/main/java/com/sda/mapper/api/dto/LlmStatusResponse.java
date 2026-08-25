package com.sda.mapper.api.dto;

/**
 * Status of the optional LLM translation feature. The API key is intentionally
 * never exposed here.
 *
 * @param enabled    whether the feature master switch is on
 * @param provider   the configured provider name
 * @param model      the configured model id
 * @param configured whether a usable API key is present (feature callable)
 */
public record LlmStatusResponse(boolean enabled, String provider, String model, boolean configured) {
}
