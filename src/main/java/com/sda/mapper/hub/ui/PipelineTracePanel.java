package com.sda.mapper.hub.ui;

import com.sda.mapper.observability.PipelineEvent;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trace panel of the pipeline observability console: shows the current run id
 * and the per-stage durations and outcomes recorded for it.
 */
public class PipelineTracePanel extends VerticalLayout {

    private final Pre runHeader = new Pre("No pipeline run yet");
    private final Map<String, StringBuilder> stageLines = new LinkedHashMap<>();

    /**
     * Creates the trace panel.
     */
    public PipelineTracePanel() {
        addClassName("pipeline-trace-panel");
        setSpacing(false);
        runHeader.addClassName("trace-run-header");
        add(runHeader);
    }

    /**
     * Applies one audit event, updating the run header and stage rows.
     *
     * @param event the recorded stage transition
     */
    public void onEvent(PipelineEvent event) {
        runHeader.setText("Run " + event.runId());
        StringBuilder line = stageLines.computeIfAbsent(event.stage(),
            s -> {
                StringBuilder b = new StringBuilder();
                addStageLine(b, s.toString(), "started");
                return b;
            });
        switch (event.status()) {
            case PipelineEvent.SUCCEEDED -> appendOutcome(line, event,
                event.durationMs() != null ? event.durationMs() + "ms" : "");
            case PipelineEvent.FAILED -> appendOutcome(line, event,
                "FAILED" + (event.detail() != null && !event.detail().isBlank()
                    ? " — " + event.detail() : ""));
            default -> { /* STARTED already rendered */ }
        }
    }

    /**
     * Clears all recorded stages.
     */
    public void reset() {
        stageLines.clear();
        removeAll();
        runHeader.setText("No pipeline run yet");
        add(runHeader);
    }

    private void addStageLine(StringBuilder builder, String stage, String state) {
        builder.append(stage).append(": ").append(state).append('\n');
        render();
    }

    private void appendOutcome(StringBuilder line, PipelineEvent event, String outcome) {
        int idx = line.indexOf(event.stage());
        if (idx >= 0) {
            int start = idx + event.stage().length() + 2;
            int end = line.indexOf("\n", start);
            if (end < 0) {
                end = line.length();
            }
            String suffix = outcome.isBlank() ? "" : " (" + outcome + ")";
            line.replace(start, end, event.status().toLowerCase() + suffix);
        }
        render();
    }

    private void render() {
        removeAll();
        add(runHeader);
        Pre body = new Pre(String.join("\n",
            stageLines.values().stream().map(StringBuilder::toString).toList()));
        body.addClassName("trace-body");
        add(body);
    }
}
