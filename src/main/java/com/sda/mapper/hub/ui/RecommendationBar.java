package com.sda.mapper.hub.ui;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.Recommendation;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Persistent recommendations bar with inline upload buttons per recommended
 * format. Buttons vanish once their format has been uploaded.
 */
public class RecommendationBar extends VerticalLayout {

    private BiConsumer<String, byte[]> onUpload;
    private Consumer<String> onError;

    /**
     * Creates the recommendations bar.
     */
    public RecommendationBar() {
        addClassName("hub-recommendations");
        setPadding(false);
        setSpacing(true);
    }

    /**
     * Re-renders the recommendations for the given report state.
     *
     * @param recommendations the recommendations to show
     * @param uploaded        formats already uploaded
     * @param onUpload        callback with the file name and content
     * @param onError         callback for upload errors
     */
    public void update(List<Recommendation> recommendations, Set<AssetFileFormat> uploaded,
                       BiConsumer<String, byte[]> onUpload, Consumer<String> onError) {
        this.onUpload = onUpload;
        this.onError = onError;
        removeAll();

        Span title = new Span("Recommended next uploads");
        title.addClassName("hub-section-label");
        add(title);

        if (recommendations.isEmpty()) {
            Span done = new Span("All format gaps are addressed. You can still add more files.");
            done.addClassName("hub-recommendations-empty");
            add(done);
            return;
        }

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        for (Recommendation recommendation : recommendations) {
            row.add(createCard(recommendation));
        }
        add(row);
    }

    private Div createCard(Recommendation recommendation) {
        Div card = new Div();
        card.addClassName("hub-recommendation-card");

        Span format = new Span(recommendation.format().label());
        format.addClassName("hub-recommendation-format");
        Span reason = new Span(recommendation.reason());
        reason.addClassName("hub-recommendation-reason");

        UploadDropzone dropzone = new UploadDropzone(
            "Upload " + recommendation.format().label(),
            List.of(recommendation.format()),
            onUpload, onError);

        card.add(format, reason, dropzone.get());
        return card;
    }
}
