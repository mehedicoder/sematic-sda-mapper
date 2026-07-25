package com.sda.mapper.ui.importwizard;

import com.sda.mapper.imports.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.*;
import java.util.function.Consumer;

/**
 * Form for manually entering missing semantic field values.
 * Groups items by entity and provides input fields per missing property.
 */
public class SemanticCompletionForm extends VerticalLayout {

    private final ImportSessionService sessionService;
    private final UUID sessionId;
    private final List<MissingItem> items;
    private final Consumer<List<MissingItem>> onCompleted;
    private final List<MissingItem> completedItems = new ArrayList<>();
    private final Map<MissingItem, TextField> inputFields = new LinkedHashMap<>();

    public SemanticCompletionForm(ImportSessionService sessionService,
                                   UUID sessionId,
                                   List<MissingItem> items,
                                   Consumer<List<MissingItem>> onCompleted) {
        this.sessionService = sessionService;
        this.sessionId = sessionId;
        this.items = items;
        this.onCompleted = onCompleted;

        setPadding(false);
        setSpacing(true);
        setSizeFull();

        var header = new H3("Complete Missing Information");
        add(header);

        var description = new Paragraph("Fill in the missing fields below. Required fields are marked with *.");
        description.addClassNames(LumoUtility.TextColor.SECONDARY);
        add(description);

        var form = new FormLayout();
        form.setWidthFull();

        for (MissingItem item : items) {
            var field = new TextField(item.fieldPath());
            field.setPlaceholder("Enter value for " + item.fieldPath());
            field.setWidthFull();
            field.setRequiredIndicatorVisible(item.resolvable());

            inputFields.put(item, field);
            form.addFormItem(field, item.entityLabel() + " (" + item.entityType() + ")");
        }

        add(form);

        var buttonLayout = new HorizontalLayout();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setSpacing(true);

        var saveAllButton = new Button("Save All Values", e -> saveAllValues());
        saveAllButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var skipButton = new Button("Skip All", e -> skipAll());
        skipButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveAllButton, skipButton);
        add(buttonLayout);
    }

    private void saveAllValues() {
        List<MissingItem> newlyCompleted = new ArrayList<>();

        for (var entry : inputFields.entrySet()) {
            MissingItem item = entry.getKey();
            TextField field = entry.getValue();
            String value = field.getValue();

            if (value != null && !value.isBlank()) {
                try {
                    sessionService.addManualEntry(sessionId, item.entityUri(), item.fieldPath(), value);
                    newlyCompleted.add(item);
                    field.setEnabled(false);
                    field.getElement().getStyle().set("background-color", "var(--lumo-success-10pct-color)");
                } catch (Exception e) {
                    Notification.show("Failed to save: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        }

        completedItems.addAll(newlyCompleted);
        Notification.show(String.format("Saved %d value(s)", newlyCompleted.size()), 2000,
                Notification.Position.BOTTOM_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        onCompleted.accept(newlyCompleted);
    }

    private void skipAll() {
        onCompleted.accept(new ArrayList<>(items));
        Notification.show("Skipped all items", 2000, Notification.Position.BOTTOM_CENTER)
            .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }
}
