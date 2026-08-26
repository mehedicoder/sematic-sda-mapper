package com.sda.mapper.hub.ui;

import com.sda.mapper.observability.PipelineEvent;
import com.vaadin.flow.component.page.Push;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Mapping Hub live observability console")
class MappingHubViewObservabilityTest {

    @Test
    @DisplayName("Should enable server push via the app shell so stage events render without refresh")
    void shouldEnablePush() {
        assertNotNull(com.sda.mapper.ui.SdaAppShell.class.getAnnotation(Push.class),
            "The app shell must enable @Push for live streaming into MappingHubView");
    }

    @Test
    @DisplayName("Should update the stage timeline from STARTED and SUCCEEDED events")
    void shouldUpdateTimelineFromEvents() {
        StageTimeline timeline = new StageTimeline();

        timeline.onEvent(new PipelineEvent(null, "run-1", "s1", null,
            "LIFT", PipelineEvent.STARTED, Instant.now(), null, null));
        timeline.onEvent(new PipelineEvent(null, "run-1", "s1", null,
            "LIFT", PipelineEvent.SUCCEEDED, Instant.now(), 12L, "Derived 3 entities"));

        String rendered = timeline.getChildren()
            .map(c -> c.getElement().getText())
            .collect(Collectors.joining("|"));
        assertTrue(rendered.contains("LIFT"), "timeline must contain the LIFT badge");
        assertTrue(rendered.contains("12ms"), "timeline must show the stage duration");
    }

    @Test
    @DisplayName("Should mark a failed stage in the timeline")
    void shouldMarkFailedStage() {
        StageTimeline timeline = new StageTimeline();
        timeline.onEvent(new PipelineEvent(null, "run-2", null, "REQ-001",
            "MATCH", PipelineEvent.FAILED, Instant.now(), 5L, "match failed"));

        String rendered = timeline.getChildren()
            .map(c -> c.getElement().getText())
            .collect(Collectors.joining("|"));
        assertTrue(rendered.contains("MATCH ✗"), "failed MATCH stage must show failure marker");
    }

    @Test
    @DisplayName("Should stream audit events into the console feed")
    void shouldStreamEventsToConsole() {
        PipelineConsolePanel panel = new PipelineConsolePanel();

        panel.onEvent(new PipelineEvent(1L, "run-3", "sess-1", "REQ-001",
            "INSTANTIATE", PipelineEvent.SUCCEEDED, Instant.parse("2026-08-26T10:00:00Z"), 4L,
            "Stored 9 parsed entities"));
        panel.onEvent(new PipelineEvent(2L, "run-3", "sess-1", null,
            "LIFT", PipelineEvent.STARTED, Instant.parse("2026-08-26T10:00:01Z"), null, null));

        List<String> lines = panel.getLines();
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("[10:00:00] [INSTANTIATE] SUCCEEDED (4ms)"));
        assertTrue(lines.get(0).contains("Stored 9 parsed entities"));
        assertTrue(lines.get(1).contains("[LIFT] STARTED"));

        panel.reset();
        assertTrue(panel.getLines().isEmpty());
    }

    @Test
    @DisplayName("Should render run id and stage rows in the trace panel")
    void shouldRenderTracePanelRows() {
        PipelineTracePanel panel = new PipelineTracePanel();
        panel.onEvent(new PipelineEvent(null, "run-abc", "s1", null,
            "PARSE", PipelineEvent.SUCCEEDED, Instant.now(), 3L, "Parsed 40 triples"));

        String rendered = panel.getElement().getTextRecursively();
        assertTrue(rendered.contains("run-abc"), "trace panel must show the run id");
        assertTrue(rendered.contains("PARSE"), "trace panel must show stage rows");
    }
}
