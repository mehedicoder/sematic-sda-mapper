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
 *
 * <p>Completed stages render a green check, the active stage a pulsing blue
 * indicator and pending stages stay muted.</p>
 */
public class PipelineStepper extends HorizontalLayout {

    private final Map<PipelineStage, Div> steps = new EnumMap<>(PipelineStage.class);
    private final Map<PipelineStage, Span> numbers = new EnumMap<>(PipelineStage.class);
    private final Map<PipelineStage, Integer> ordinals = new EnumMap<>(PipelineStage.class);

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
            numbers.put(stage, number);
            ordinals.put(stage, index);
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
            numbers.get(stage).setText("\u2713");
        }
    }

    /**
     * Resets all stages to the pending state.
     */
    public void reset() {
        for (PipelineStage stage : PipelineStage.values()) {
            steps.get(stage).removeClassName("hub-step--done");
            steps.get(stage).removeClassName("hub-step--active");
            numbers.get(stage).setText(String.valueOf(ordinals.get(stage)));
        }
    }

    /**
     * Plays the stage progression animation (active pulse then completed per
     * stage) and invokes the callback once all stages have completed.
     *
     * <p>The pipeline itself runs synchronously; this animation gives the user
     * a visual walk-through of the five stages before the results render.</p>
     *
     * @param onFinished callback invoked after the last stage completes
     */
    public void runStages(Runnable onFinished) {
        reset();
        String script = "(async () => {"
            + "  const stages = [...this.querySelectorAll('.hub-step')];"
            + "  for (let i = 0; i < stages.length; i++) {"
            + "    if (i > 0) stages[i - 1].classList.remove('hub-step--active');"
            + "    stages[i].classList.add('hub-step--active');"
            + "    await new Promise(resolve => setTimeout(resolve, 220));"
            + "    stages[i].classList.remove('hub-step--active');"
            + "    stages[i].classList.add('hub-step--done');"
            + "  }"
            + "  return true;"
            + "})()";
        getElement().executeJs(script)
            .then(result -> onFinished.run(), error -> onFinished.run());
    }
}
