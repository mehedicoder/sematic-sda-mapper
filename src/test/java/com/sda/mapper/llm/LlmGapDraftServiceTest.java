package com.sda.mapper.llm;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("LlmGapDraftService")
class LlmGapDraftServiceTest {

    private static final String VALID_JSON = "{"
        + "\"drafts\":[{\"className\":\"RequiredCapability\",\"name\":\"Diagnostic Capability\","
        + "\"rationale\":\"flowmeter diagnostics\"}],\"explanations\":[]}";

    private LlmGapDraftService service(boolean configured, String reply) {
        LlmProperties props = mock(LlmProperties.class);
        when(props.isConfigured()).thenReturn(configured);
        ChatCompletionsClient client = mock(ChatCompletionsClient.class);
        when(client.complete(anyString(), anyString())).thenReturn(reply);
        LlmMatchAugmentor augmentor = new LlmMatchAugmentor(props, client);
        return new LlmGapDraftService(augmentor, props);
    }

    private static GapReport gapReport() {
        GapReport report = mock(GapReport.class);
        var entry = mock(com.sda.mapper.hub.model.CoverageEntry.class);
        when(entry.className()).thenReturn("RequiredCapability");
        when(entry.status()).thenReturn(CoverageStatus.PARTIAL);
        when(report.entries()).thenReturn(List.of(entry));
        return report;
    }

    @Test
    @DisplayName("delegates to the augmentor and returns drafts")
    void delegatesToAugmentor() {
        LlmGapDraftService service = service(true, VALID_JSON);
        LlmGapResult result = service.explainGaps(gapReport(), List.of());
        assertEquals(1, result.drafts().size());
        assertEquals("RequiredCapability", result.drafts().get(0).className());
    }

    @Test
    @DisplayName("disabled feature throws LlmNotConfiguredException")
    void notConfiguredThrows() {
        LlmGapDraftService service = service(false, VALID_JSON);
        assertThrows(LlmNotConfiguredException.class,
            () -> service.explainGaps(gapReport(), List.of()));
    }
}
