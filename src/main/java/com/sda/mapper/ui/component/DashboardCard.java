package com.sda.mapper.ui.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * A reusable animated dashboard card displaying an icon, title, numeric value,
 * and status color. Clicking the card navigates to the associated view.
 */
public class DashboardCard extends VerticalLayout {

    private final Span valueSpan;

    /**
     * Creates a dashboard card with the given icon, title, initial value, and status color.
     *
     * @param icon  VaadinIcon to display
     * @param title card title text
     * @param value initial numeric value
     * @param color CSS color string (e.g. "var(--lumo-primary-color)")
     * @param link  optional RouterLink for navigation; may be {@code null}
     */
    public DashboardCard(VaadinIcon icon, String title, int value,
                         String color, RouterLink link) {
        setPadding(true);
        setSpacing(false);
        setWidthFull();
        setMinHeight("140px");
        addClassName("dashboard-card");
        getStyle()
            .set("border-radius", "12px")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 8px rgba(0,0,0,0.08)")
            .set("cursor", link != null ? "pointer" : "default")
            .set("animation", "cardEntrance 0.4s ease-out both")
            .set("transition", "box-shadow 0.2s, transform 0.2s");

        var iconComponent = new Icon(icon);
        iconComponent.setColor(color);
        iconComponent.setSize("28px");

        var titleSpan = new Span(title);
        titleSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        valueSpan = new Span(String.valueOf(value));
        valueSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
        valueSpan.getStyle().set("color", color);

        var textLayout = new VerticalLayout(titleSpan, valueSpan);
        textLayout.setPadding(false);
        textLayout.setSpacing(false);

        var headerLayout = new HorizontalLayout(iconComponent, textLayout);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);
        headerLayout.setWidthFull();

        add(headerLayout);

        if (link != null) {
            addClickListener(e -> link.getUI().ifPresent(ui -> ui.navigate(link.getHref())));
        }
    }

    /**
     * Creates a dashboard card without navigation link.
     */
    public DashboardCard(VaadinIcon icon, String title, int value, String color) {
        this(icon, title, value, color, null);
    }

    /**
     * Updates the displayed numeric value.
     *
     * @param newValue the new value to display
     */
    public void updateValue(int newValue) {
        valueSpan.setText(String.valueOf(newValue));
    }
}
