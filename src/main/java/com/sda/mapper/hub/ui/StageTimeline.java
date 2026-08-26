package com.sda.mapper.hub.ui;

import com.sda.mapper.observability.PipelineEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Live stage timeline for the pipeline observability console: one chip per
 * pipeline stage that switches from waiting &rarr; running &rarr; succeeded /
 * failed as audit events stream in.
 */
public class StageTimeline extends HorizontalLayout {

    private static final String[] STAGES = {"PARSE", "INSTANTIATE", "LIFT", "INFER", "MATCH"};

    private final Map<String, Span> badges = new LinkedHashMap<>();

    /**
     * Creates the timeline with all known stages in execution order.
     */
    public StageTimeline() {
        setSpacing(true);
        addClassName("pipeline-stage-timeline");
        for (String stage : STAGES) {
            Span badge = new Span(stage);
            badge.addClassName("stage-badge");
            badge.addClassName("stage-waiting");
            badges.put(stage, badge);
            add(badge);
        }
    }

    /**
     * Applies one audit event to the matching stage badge.
     *
     * @param event the recorded stage transition
     */
    public void onEvent(PipelineEvent event) {
        Span badge = badges.get(event.stage());
        if (badge == null) {
            return;
        }
        badge.removeClassNames("stage-waiting", "stage-running", "stage-succeeded", "stage-failed");
        switch (event.status()) {
            case PipelineEvent.STARTED -> {
                badge.addClassName("stage-running");
                badge.setText(event.stage() + " …");
            }
            case PipelineEvent.SUCCEEDED -> {
                badge.addClassName("stage-succeeded");
                badge.setText(event.stage()
                    + (event.durationMs() != null ? " ✓ " + event.durationMs() + "ms" : ""));
            }
            case PipelineEvent.FAILED -> {
                badge.addClassName("stage-failed");
                badge.setText(event.stage() + " ✗");
            }
            default -> badge.addClassName("stage-waiting");
        }
    }

    /**
     * Resets every badge to the waiting state.
     */
    public void reset() {
        badges.forEach((stage, badge) -> {
            badge.removeClassNames("stage-running", "stage-succeeded", "stage-failed");
            badge.addClassName("stage-waiting");
            badge.setText(stage);
        });
    }
}
