package com.sda.mapper.hub.ui;

import com.sda.mapper.llm.ConceptHint;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * UI surface for the opt-in LLM concept-suggestion feature. Exposes a single
 * explicit "Suggest Concepts (AI)" button (never auto-called) that asks the
 * mapping hub service to translate the latest device description into candidate
 * SDA ontology concepts. Each suggested concept can be accepted individually;
 * accepting it routes the concept through the standard manual-entry pipeline
 * path, so the LLM output is a suggestion and never overrides parser, lift or
 * match results.
 */
public class ConceptSuggestionPanel extends VerticalLayout {

    private final Button suggestButton;
    private final Span empty;
    private final VerticalLayout list;

    /**
     * Creates the suggestion panel.
     *
     * @param llmAvailable     whether the AI button should be enabled
     * @param suggestSupplier  supplies the candidate concepts from the hub service
     * @param onAccept         invoked with a concept when the user accepts it
     */
    public ConceptSuggestionPanel(boolean llmAvailable,
                                  Supplier<List<ConceptHint>> suggestSupplier,
                                  Consumer<ConceptHint> onAccept) {
        setPadding(false);
        setSpacing(true);
        addClassName("hub-suggestion-panel");

        empty = new Span("No AI suggestions yet. Upload a device description and click "
            + "Suggest Concepts (AI).");
        empty.addClassName("hub-suggestion-empty");

        list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(true);

        suggestButton = new Button("Suggest Concepts (AI)");
        suggestButton.addThemeName("secondary");
        setSuggestEnabled(llmAvailable);
        if (!llmAvailable) {
            suggestButton.setTooltipText("Enable LLM translation in Settings to use AI suggestions");
        }
        suggestButton.addClickListener(e -> {
            try {
                List<ConceptHint> hints = new ArrayList<>(suggestSupplier.get());
                render(hints, onAccept);
            } catch (RuntimeException ex) {
                list.removeAll();
                Span error = new Span("AI suggestion failed: " + ex.getMessage());
                error.addClassName("hub-suggestion-error");
                list.add(error);
            }
        });

        add(suggestButton, empty, list);
    }

    /**
     * Enables or disables the suggestion button (e.g. when the LLM feature is
     * toggled at runtime in Settings).
     *
     * @param enabled whether the button should be enabled
     */
    public void setSuggestEnabled(boolean enabled) {
        suggestButton.setEnabled(enabled);
        if (enabled) {
            suggestButton.setTooltipText("Translate the uploaded device description into SDA concepts");
        }
    }

    /**
     * Returns the suggestion trigger button (used by view tests).
     *
     * @return the suggest button
     */
    public Button getSuggestButton() {
        return suggestButton;
    }

    private void render(List<ConceptHint> hints, Consumer<ConceptHint> onAccept) {
        empty.setVisible(hints.isEmpty());
        list.removeAll();
        for (ConceptHint hint : hints) {
            Div row = new Div();
            row.addClassName("hub-suggestion-row");

            StringBuilder text = new StringBuilder();
            text.append(hint.className()).append(": ").append(hint.label() != null ? hint.label() : "");
            if (hint.reason() != null && !hint.reason().isBlank()) {
                text.append(" — ").append(hint.reason());
            }
            Span info = new Span(text.toString());
            info.addClassName("hub-suggestion-info");

            Button add = new Button("Add");
            add.addThemeName("small");
            add.addClickListener(ev -> onAccept.accept(hint));

            row.add(info, add);
            list.add(row);
        }
    }
}
