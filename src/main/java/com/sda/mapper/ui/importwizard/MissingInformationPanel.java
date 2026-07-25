package com.sda.mapper.ui.importwizard;

import com.sda.mapper.imports.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays missing semantic information items grouped by category,
 * with resolution recommendations per category.
 */
public class MissingInformationPanel extends VerticalLayout {

    public MissingInformationPanel(List<MissingItem> missingItems,
                                    ImportRecommendationService recommendationService) {
        setPadding(false);
        setSpacing(true);
        setSizeFull();

        var header = new H3("Missing Information");
        add(header);

        var categoryGroups = missingItems.stream()
            .collect(Collectors.groupingBy(MissingItem::category));

        for (var entry : categoryGroups.entrySet()) {
            MissingCategory category = entry.getKey();
            List<MissingItem> items = entry.getValue();

            var categoryCard = new VerticalLayout();
            categoryCard.addClassNames(LumoUtility.Background.BASE, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM, LumoUtility.BoxShadow.SMALL);
            categoryCard.setWidthFull();

            var categoryHeader = new HorizontalLayout();
            categoryHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            categoryHeader.setWidthFull();

            var categoryLabel = new Span(category.getDisplayName());
            categoryLabel.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE);

            var countBadge = new Span(String.format("%d item(s)", items.size()));
            countBadge.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            categoryHeader.add(categoryLabel, countBadge);

            var categoryDesc = new Paragraph(category.getDescription());
            categoryDesc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

            categoryCard.add(categoryHeader, categoryDesc);

            var itemGrid = new Grid<MissingItem>();
            itemGrid.addColumn(MissingItem::entityLabel).setHeader("Entity");
            itemGrid.addColumn(MissingItem::entityType).setHeader("Type");
            itemGrid.addColumn(MissingItem::description).setHeader("Issue");
            itemGrid.setItems(items);
            itemGrid.setWidthFull();
            itemGrid.setHeight("300px");

            categoryCard.add(itemGrid);

            var recommendations = recommendationService.recommend(items);
            if (recommendations.containsKey(category)) {
                var rec = recommendations.get(category);
                var recLayout = new HorizontalLayout();
                recLayout.setAlignItems(FlexComponent.Alignment.CENTER);
                recLayout.setSpacing(true);

                var recLabel = new Span("Suggested files: ");
                recLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
                recLayout.add(recLabel);

                for (FileType fileType : rec.suggestedFileTypes()) {
                    var fileTag = new Span(fileType.getDisplayName());
                    fileTag.addClassNames(LumoUtility.Background.CONTRAST_10, LumoUtility.Padding.XSMALL,
                        LumoUtility.BorderRadius.SMALL, LumoUtility.FontSize.SMALL);
                    recLayout.add(fileTag);
                }

                var descLabel = new Span(rec.description());
                descLabel.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                recLayout.add(descLabel);

                categoryCard.add(recLayout);
            }

            add(categoryCard);
        }
    }
}
