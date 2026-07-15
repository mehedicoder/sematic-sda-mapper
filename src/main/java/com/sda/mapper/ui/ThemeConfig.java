package com.sda.mapper.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.Lumo;

import java.io.Serializable;

/**
 * Manages Lumo light/dark mode toggle with persisted user preference.
 * Stores the theme choice in the browser's localStorage via Vaadin's WebStorage API.
 */
public class ThemeConfig implements Serializable {

    private static final String THEME_KEY = "sda-mapper-theme";

    /**
     * Creates a theme toggle button that switches between light and dark modes.
     *
     * @return a Button that toggles the Lumo theme
     */
    public static Button createThemeToggleButton() {
        Button toggle = new Button();
        toggle.addThemeVariants(ButtonVariant.LUMO_ICON);
        toggle.getElement().setAttribute("aria-label", "Toggle dark mode");
        toggle.setIcon(new Icon(VaadinIcon.MOON_O));
        toggle.addClickListener(e -> {
            UI ui = UI.getCurrent();
            if (ui == null) return;
            boolean isDark = isDarkMode(ui);
            setTheme(ui, !isDark);
        });
        return toggle;
    }

    /**
     * Applies the persisted theme preference on UI attach.
     *
     * @param ui the current Vaadin UI
     */
    public static void applyPersistedTheme(UI ui) {
        ui.getPage().executeJs(
            "return localStorage.getItem('" + THEME_KEY + "')"
        ).then(String.class, value -> {
            if ("dark".equals(value)) {
                setDark(ui);
            } else {
                setLight(ui);
            }
        });
    }

    /**
     * Sets the theme to dark or light and persists the preference.
     */
    private static void setTheme(UI ui, boolean dark) {
        if (dark) {
            setDark(ui);
        } else {
            setLight(ui);
        }
        ui.getPage().executeJs(
            "localStorage.setItem('" + THEME_KEY + "', '" + (dark ? "dark" : "light") + "')"
        );
    }

    private static void setDark(UI ui) {
        ui.getElement().setAttribute("theme", Lumo.DARK);
    }

    private static void setLight(UI ui) {
        ui.getElement().removeAttribute("theme");
    }

    private static boolean isDarkMode(UI ui) {
        String theme = ui.getElement().getAttribute("theme");
        return Lumo.DARK.equals(theme);
    }
}
