package com.sda.mapper.api;

import com.sda.mapper.api.dto.LlmStatusResponse;
import com.sda.mapper.api.dto.LlmTranslateRequest;
import com.sda.mapper.llm.LlmNotConfiguredException;
import com.sda.mapper.llm.LlmProperties;
import com.sda.mapper.llm.LlmTranslationService;
import com.sda.mapper.llm.TranslationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API for the optional LLM-based semantic translation feature. Exposes a
 * translation endpoint and a status endpoint. The API key is never returned.
 */
@RestController
@RequestMapping("/api/v1/llm")
public class LlmController {

    private static final Logger log = LoggerFactory.getLogger(LlmController.class);

    private final LlmTranslationService translationService;
    private final LlmProperties properties;

    public LlmController(LlmTranslationService translationService, LlmProperties properties) {
        this.translationService = translationService;
        this.properties = properties;
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translate(@RequestBody LlmTranslateRequest request) {
        if (!translationService.isAvailable()) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "llm_not_configured",
                    "message", "LLM translation is disabled or no API key is configured."));
        }
        try {
            TranslationResult result = translationService.translate(
                    request.format(), request.content());
            return ResponseEntity.ok(result);
        } catch (LlmNotConfiguredException e) {
            return ResponseEntity.status(409).body(Map.of("error", "llm_not_configured",
                    "message", e.getMessage()));
        } catch (Exception e) {
            log.warn("LLM translation failed", e);
            return ResponseEntity.status(502).body(Map.of("error", "llm_translation_failed",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public LlmStatusResponse status() {
        return new LlmStatusResponse(properties.isEnabled(), properties.getProvider(),
                properties.getModel(), properties.isConfigured());
    }
}
