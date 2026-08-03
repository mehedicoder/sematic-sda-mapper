package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.PipelineStage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.EnumMap;
import java.util.Map;

/**
 * Horizontal pipeline stepper showing the five stages
 * Parse &rarr; Lift &rarr; Infer &rarr; Match &rarr; Manifest.
 */
public class PipelineStepper extends HorizontalLayout {

    private final Map<PipelineStage, Div> steps = new EnumMap<>(PipelineStage.class);

    /**
     * Creates the stepper with all five stages in pending state.
     */
    public PipelineStepper() {
        addClassName("hub-stepper");
        int index = 1;
        for (PipelineStage stage : PipelineStage.values()) {
            Div step = new Div();
            step.addClassName("hub-step");
            step.setId("hub-step-" + stage.name().toLowerCase());

            Span number = new Span(String.valueOf(index));
            number.addClassName("hub-step-number");

            Span label = new Span(stage.label());
            label.addClassName("hub-step-label");

            step.add(number, label);
            steps.put(stage, step);
            add(step);
            index++;
        }
    }

    /**
     * Marks all five stages as completed (a full pipeline run finished).
     */
    public void setCompleted() {
        for (PipelineStage stage : PipelineStage.values()) {
            steps.get(stage).addClassName("hub-step--done");
        }
    }

    /**
     * Resets all stages to the pending state.
     */
    public void reset() {
        for (PipelineStage stage : PipelineStage.values()) {
            steps.get(stage).removeClassName("hub-step--done");
        }
    }
}
