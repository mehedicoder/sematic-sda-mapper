package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Coverage dashboard: one stat card per coverage status, the overall coverage
 * percentage and a four-segment overall progress bar.
 */
public class CoverageDashboard extends VerticalLayout {

    private static final int CLASS_COUNT = 24;

    private final Span presentValue;
    private final Span inferredValue;
    private final Span partialValue;
    private final Span missingValue;
    private final Span percentValue;
    private final Div progressSegmentPresent;
    private final Div progressSegmentInferred;
    private final Div progressSegmentPartial;
    private final Div progressSegmentMissing;

    /**
     * Creates the dashboard with four cards and the progress bar.
     */
    public CoverageDashboard() {
        addClassName("hub-dashboard");
        setPadding(false);
        setSpacing(true);

        HorizontalLayout cards = new HorizontalLayout(
            createCard("Present", presentValue = new Span("0"), "hub-card--present"),
            createCard("Inferred", inferredValue = new Span("0"), "hub-card--inferred"),
            createCard("Partial", partialValue = new Span("0"), "hub-card--partial"),
            createCard("Missing", missingValue = new Span("0"), "hub-card--missing")
        );
        cards.addClassName("hub-dashboard-cards");
        cards.setWidthFull();
        cards.setSpacing(true);
        add(cards);

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        Span coverageLabel = new Span("Overall coverage");
        coverageLabel.addClassName("hub-section-label");
        percentValue = new Span("0.0%");
        percentValue.addClassName("hub-coverage-percent");
        headerRow.add(coverageLabel, percentValue);
        add(headerRow);

        Div progress = new Div();
        progress.addClassName("hub-progress");
        progress.setWidthFull();
        progressSegmentPresent = createSegment("hub-progress--present");
        progressSegmentInferred = createSegment("hub-progress--inferred");
        progressSegmentPartial = createSegment("hub-progress--partial");
        progressSegmentMissing = createSegment("hub-progress--missing");
        progress.add(progressSegmentPresent, progressSegmentInferred,
            progressSegmentPartial, progressSegmentMissing);
        add(progress);
    }

    /**
     * Updates the dashboard from the given coverage report.
     *
     * @param report the coverage report
     */
    public void update(GapReport report) {
        presentValue.setText(String.valueOf(report.count(CoverageStatus.PRESENT)));
        inferredValue.setText(String.valueOf(report.count(CoverageStatus.INFERRED)));
        partialValue.setText(String.valueOf(report.count(CoverageStatus.PARTIAL)));
        missingValue.setText(String.valueOf(report.count(CoverageStatus.MISSING)));
        percentValue.setText(String.format("%.1f%%", report.coveragePercent()));

        int present = report.count(CoverageStatus.PRESENT);
        int inferred = report.count(CoverageStatus.INFERRED);
        int partial = report.count(CoverageStatus.PARTIAL);
        int missing = report.count(CoverageStatus.MISSING);
        setWidth(progressSegmentPresent, present);
        setWidth(progressSegmentInferred, inferred);
        setWidth(progressSegmentPartial, partial);
        setWidth(progressSegmentMissing, missing);
    }

    private static VerticalLayout createCard(String label, Span value, String cssClass) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("hub-card");
        card.addClassName(cssClass);
        card.setPadding(true);
        card.setSpacing(false);

        value.addClassName("hub-card-value");
        Span caption = new Span(label);
        caption.addClassName("hub-card-label");

        card.add(value, caption);
        return card;
    }

    private static Div createSegment(String cssClass) {
        Div segment = new Div();
        segment.addClassName("hub-progress-segment");
        segment.addClassName(cssClass);
        setWidth(segment, 0);
        return segment;
    }

    private static void setWidth(Div segment, int count) {
        double percent = CLASS_COUNT == 0 ? 0.0 : (double) count / CLASS_COUNT * 100.0;
        segment.getStyle().set("width", String.format("%.2f%%", percent));
    }

    /**
     * Returns the currently displayed count for the given status.
     *
     * @param status the coverage status
     * @return the count shown on the card
     */
    public int getCount(CoverageStatus status) {
        return switch (status) {
            case PRESENT -> Integer.parseInt(presentValue.getText());
            case INFERRED -> Integer.parseInt(inferredValue.getText());
            case PARTIAL -> Integer.parseInt(partialValue.getText());
            case MISSING -> Integer.parseInt(missingValue.getText());
        };
    }
}
