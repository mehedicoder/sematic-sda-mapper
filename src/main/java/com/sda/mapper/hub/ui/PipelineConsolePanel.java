package com.sda.mapper.hub.ui;

import com.sda.mapper.observability.PipelineEvent;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Streaming event feed of the pipeline observability console: appends one line
 * per audit event, newest at the bottom, capped at 200 lines.
 */
public class PipelineConsolePanel extends VerticalLayout {

    private static final int MAX_LINES = 200;
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter
        .ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    private final Deque<String> lines = new ArrayDeque<>();
    private final Pre output = new Pre("");

    /**
     * Creates the console panel.
     */
    public PipelineConsolePanel() {
        addClassName("pipeline-console-panel");
        setSpacing(false);
        output.addClassName("pipeline-console-output");
        add(output);
    }

    /**
     * Appends one audit event to the feed.
     *
     * @param event the recorded stage transition
     */
    public void onEvent(PipelineEvent event) {
        StringBuilder line = new StringBuilder();
        line.append('[').append(TIMESTAMP.format(event.startedAt())).append("] ");
        line.append('[').append(event.stage()).append("] ");
        line.append(event.status());
        if (event.durationMs() != null) {
            line.append(" (").append(event.durationMs()).append("ms)");
        }
        if (event.detail() != null && !event.detail().isBlank()) {
            line.append(" \u2014 ").append(event.detail());
        }
        appendLine(line.toString());
    }

    /**
     * Clears the feed.
     */
    public void reset() {
        lines.clear();
        render();
    }

    /**
     * Returns the rendered lines (visible to tests).
     *
     * @return current console lines in order
     */
    public java.util.List<String> getLines() {
        return java.util.List.copyOf(lines);
    }

    private void appendLine(String line) {
        lines.addLast(line);
        while (lines.size() > MAX_LINES) {
            lines.removeFirst();
        }
        render();
    }

    private void render() {
        output.setText(String.join("\n", lines));
    }
}
