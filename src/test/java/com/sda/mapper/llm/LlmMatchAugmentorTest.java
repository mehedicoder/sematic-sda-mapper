package com.sda.mapper.llm;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("LlmMatchAugmentor")
class LlmMatchAugmentorTest {

    private static final String VALID_JSON = "{"
        + "\"drafts\":[{\"className\":\"Capability\",\"name\":\"Flowmeter Diagnostics Capability\","
        + "\"rationale\":\"required by the NE107 requirement\"}],"
        + "\"explanations\":[{\"className\":\"Capability\",\"status\":\"MISSING\","
        + "\"explanation\":\"no capability was lifted from the datasheet\"}]}";

    private LlmMatchAugmentor service(boolean configured, String reply) {
        LlmProperties props = mock(LlmProperties.class);
        when(props.isConfigured()).thenReturn(configured);
        ChatCompletionsClient client = mock(ChatCompletionsClient.class);
        when(client.complete(anyString(), anyString())).thenReturn(reply);
        return new LlmMatchAugmentor(props, client);
    }

    private static GapReport gapReport() {
        GapReport report = mock(GapReport.class);
        var entry = mock(com.sda.mapper.hub.model.CoverageEntry.class);
        when(entry.className()).thenReturn("Capability");
        when(entry.status()).thenReturn(CoverageStatus.MISSING);
        when(report.entries()).thenReturn(List.of(entry));
        return report;
    }

    @Test
    @DisplayName("produces gap-fill drafts and explanations")
    void producesDraftsAndExplanations() {
        LlmMatchAugmentor service = service(true, VALID_JSON);
        LlmGapResult result = service.suggestGapDrafts(gapReport(), List.of());
        assertEquals(1, result.drafts().size());
        assertEquals("Capability", result.drafts().get(0).className());
        assertEquals("Flowmeter Diagnostics Capability", result.drafts().get(0).name());
        assertEquals(1, result.explanations().size());
        assertEquals("MISSING", result.explanations().get(0).status());
    }

    @Test
    @DisplayName("disabled feature throws LlmNotConfiguredException")
    void notConfiguredThrows() {
        LlmMatchAugmentor service = service(false, VALID_JSON);
        assertThrows(LlmNotConfiguredException.class,
            () -> service.suggestGapDrafts(gapReport(), List.of()));
    }

    @Test
    @DisplayName("draft with invalid class is skipped")
    void skipsInvalidClass() {
        String json = "{\"drafts\":[{\"className\":\"BogusClass\",\"name\":\"x\",\"rationale\":\"r\"}],"
            + "\"explanations\":[]}";
        LlmMatchAugmentor service = service(true, json);
        assertTrue(service.suggestGapDrafts(gapReport(), List.of()).drafts().isEmpty());
    }

    @Test
    @DisplayName("code-fenced JSON is tolerated")
    void toleratesCodeFences() {
        LlmMatchAugmentor service = service(true, "```json\n" + VALID_JSON + "\n```");
        assertEquals(1, service.suggestGapDrafts(gapReport(), List.of()).drafts().size());
    }
}
