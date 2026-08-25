package com.sda.mapper.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the optional LLM-based semantic translation feature
 * (paper p.7-8, p.12). All values are user-configurable and bind from
 * {@code app.llm.*} properties (e.g. {@code application.yml}) or environment
 * variables (e.g. {@code APP_LLM_APIKEY}, {@code APP_LLM_BASEURL},
 * {@code APP_LLM_MODEL}). The Settings view can also mutate these fields at
 * runtime on the shared bean instance.
 *
 * <p>Defaults target OpenRouter with a free model so the feature works out of the
 * box once an API key is supplied; no key is bundled.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    /** Master switch for the LLM translation feature. */
    private boolean enabled = false;

    /** Provider preset; drives the default base URL and required headers. */
    private String provider = "OPENROUTER";

    /** Override base URL (required for CUSTOM / AZURE). */
    private String baseUrl;

    /** API key for the selected provider. Never exposed in status responses. */
    private String apiKey;

    /** Chat model id. Defaults to a free OpenRouter model. */
    private String model = "mistralai/mistral-7b-instruct:free";

    private double temperature = 0.2;
    private int maxTokens = 1024;
    private int timeoutMs = 30_000;

    /** OpenRouter referral headers (ignored for other providers). */
    private String referer = "sematic-sda-mapper";
    private String title = "Semantic SDA Mapper";

    public LlmProvider provider() {
        return LlmProvider.from(provider);
    }

    public String effectiveBaseUrl() {
        return provider().effectiveBaseUrl(baseUrl);
    }

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
