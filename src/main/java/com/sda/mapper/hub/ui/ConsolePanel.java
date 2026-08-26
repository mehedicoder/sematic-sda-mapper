package com.sda.mapper.hub.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/**
 * Live monospace console that renders the pipeline stage log lines.
 */
public class ConsolePanel extends VerticalLayout {

    private final Div output;

    /**
     * Creates the console with a capped-height scrollable output area.
     */
    public ConsolePanel() {
        addClassName("hub-console");
        setPadding(false);
        setSpacing(true);

        output = new Div();
        output.addClassName("hub-console-output");
        output.setWidthFull();
        output.getStyle().set("font-family", "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace");

        Scroller scroller = new Scroller(output);
        scroller.setWidthFull();
        scroller.setHeight("100px");
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        add(scroller);
    }

    /**
     * Replaces the console content with the given log lines and scrolls down.
     *
     * @param lines the log lines to render
     */
    public void logLines(List<String> lines) {
        output.removeAll();
        if (lines.isEmpty()) {
            return;
        }
        for (String line : lines) {
            Div row = new Div();
            row.addClassName("hub-console-line");
            row.setText(line);
            output.add(row);
        }
        getElement().executeJs(
            "const el = this.querySelector('.hub-console-output');"
                + "if (el) el.scrollTop = el.scrollHeight;");
    }

    /**
     * Clears the console output.
     */
    public void clear() {
        output.removeAll();
    }

    /**
     * Returns the currently rendered log lines.
     *
     * @return the log lines
     */
    public List<String> getLines() {
        return output.getChildren()
            .map(c -> c.getElement().getText())
            .toList();
    }
}
