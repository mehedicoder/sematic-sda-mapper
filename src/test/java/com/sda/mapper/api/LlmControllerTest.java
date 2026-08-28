package com.sda.mapper.api;

import com.sda.mapper.api.dto.LlmStatusResponse;
import com.sda.mapper.api.dto.LlmTranslateRequest;
import com.sda.mapper.llm.LlmProperties;
import com.sda.mapper.llm.LlmTranslationService;
import com.sda.mapper.llm.TranslationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LlmController should expose translation and status endpoints")
class LlmControllerTest {

    @Mock
    private LlmTranslationService translationService;

    @Mock
    private LlmProperties properties;

    @InjectMocks
    private LlmController controller;

    @Test
    @DisplayName("Status endpoint should report configured state without leaking the key")
    void statusShouldReportConfigured() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getProvider()).thenReturn("OPENROUTER");
        when(properties.getModel()).thenReturn("openrouter/free");
        when(properties.isConfigured()).thenReturn(true);

        LlmStatusResponse status = controller.status();

        assertThat(status.enabled()).isTrue();
        assertThat(status.configured()).isTrue();
        assertThat(status.provider()).isEqualTo("OPENROUTER");
    }

    @Test
    @DisplayName("Translate should return 409 when the feature is unavailable")
    void translateShouldReturnConflictWhenUnavailable() {
        when(translationService.isAvailable()).thenReturn(false);

        ResponseEntity<?> response = controller.translate(new LlmTranslateRequest("AAS", "{}"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Translate should return 200 with hints when available")
    void translateShouldReturnOkWhenAvailable() {
        TranslationResult result = new TranslationResult("AAS", "model", List.of(), "raw");
        when(translationService.isAvailable()).thenReturn(true);
        when(translationService.translate("AAS", "{}")).thenReturn(result);

        ResponseEntity<?> response = controller.translate(new LlmTranslateRequest("AAS", "{}"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(result);
    }
}
