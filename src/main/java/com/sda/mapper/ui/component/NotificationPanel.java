package com.sda.mapper.ui.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A Vaadin component showing a notification bell icon with badge count.
 * Clicking the bell shows a dropdown popover with recent notifications.
 */
@Tag("notification-panel")
public class NotificationPanel extends HtmlComponent {

    private final NotificationCenter notificationCenter;
    private final Span badgeSpan;
    private final Popover popover;

    /**
     * Creates a notification panel bound to the given notification center.
     *
     * @param notificationCenter the shared notification center
     */
    public NotificationPanel(NotificationCenter notificationCenter) {
        this.notificationCenter = notificationCenter;

        badgeSpan = new Span("0");
        badgeSpan.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.BOLD);
        badgeSpan.getStyle()
            .set("background", "var(--lumo-error-color)")
            .set("color", "white")
            .set("border-radius", "50%")
            .set("min-width", "18px")
            .set("height", "18px")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("position", "absolute")
            .set("top", "-4px")
            .set("right", "-4px");

        Button bellButton = new Button(new Icon(VaadinIcon.BELL));
        bellButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        bellButton.getElement().getStyle().set("position", "relative");

        Span badgeWrapper = new Span(badgeSpan);
        badgeWrapper.getElement().getStyle()
            .set("position", "relative")
            .set("display", "inline-flex");
        badgeWrapper.add(bellButton);

        popover = new Popover(bellButton);
        popover.addThemeVariants(PopoverVariant.ARROW);
        popover.setPosition(PopoverPosition.BOTTOM_END);
        popover.setWidth("360px");
        popover.setModal(false);

        VerticalLayout popoverContent = new VerticalLayout();
        popoverContent.setPadding(true);
        popoverContent.setSpacing(false);
        popoverContent.setWidthFull();

        Span header = new Span("Notifications");
        header.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        popoverContent.add(header);

        ListBox<String> notificationList = new ListBox<>();
        notificationList.setWidthFull();
        notificationList.setMinHeight("200px");
        popoverContent.add(notificationList);

        bellButton.addClickListener(e -> {
            updateBadge();
            populateList(notificationList);
            popover.open();
        });

        getElement().appendChild(badgeWrapper.getElement());
    }

    private void updateBadge() {
        int count = notificationCenter.getCount();
        badgeSpan.setText(String.valueOf(count));
        badgeSpan.setVisible(count > 0);
    }

    private void populateList(ListBox<String> listBox) {
        List<NotificationCenter.NotificationItem> items = notificationCenter.getNotifications();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        if (items.isEmpty()) {
            listBox.setItems(List.of("No notifications yet."));
        } else {
            listBox.setItems(items.stream()
                .map(item -> "[%s] %s: %s".formatted(
                    item.timestamp().format(fmt),
                    item.type(),
                    item.title()))
                .toList());
        }
    }
}
