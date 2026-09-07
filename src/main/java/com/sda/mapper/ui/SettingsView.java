package com.sda.mapper.ui;

import com.sda.mapper.llm.LlmProperties;
import com.sda.mapper.llm.LlmProvider;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Settings view for user preferences and application configuration.
 */
@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
@CssImport("./dark-view.css")
public class SettingsView extends VerticalLayout {

    @Autowired
    private LlmProperties llmProperties;

    public SettingsView() {
        getElement().setAttribute("theme", "dark");
        addClassName("dark-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Settings"));

        addUserProfileSection();
        add(new Hr());
        addAppearanceSection();
        add(new Hr());
        addLlmSection();
        add(new Hr());
        addGeneralSection();
    }

    private LlmProperties props() {
        return llmProperties != null ? llmProperties : new LlmProperties();
    }

    private void addUserProfileSection() {
        var userProfileHeader = new H3("User Profile");
        var userIcon = new Icon(VaadinIcon.USER);
        userIcon.getStyle().set("width", "24px").set("height", "24px");
        var userLabel = new Span("admin");
        userLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
        var userRow = new HorizontalLayout(userIcon, userLabel);
        userRow.setAlignItems(FlexComponent.Alignment.CENTER);
        userRow.setSpacing(true);
        add(userProfileHeader, userRow);
    }

    private void addAppearanceSection() {
        var themeHeader = new H3("Appearance");
        var themeDescription = new Paragraph("Toggle between light and dark mode.");
        var themeToggle = ThemeConfig.createThemeToggleButton();
        var themeRow = new HorizontalLayout(themeToggle, new Span("Dark Mode"));
        themeRow.setAlignItems(FlexComponent.Alignment.CENTER);
        themeRow.setSpacing(true);
        add(themeHeader, themeDescription, themeRow);
    }

    private void addLlmSection() {
        LlmProperties props = props();
        var header = new H3("LLM Semantic Translation");
        var description = new Paragraph(
                "Optional AI assistance that maps device descriptions (AutomationML, OPC UA "
                        + "NodeSet, AAS) to SDA ontology concepts. Defaults to OpenRouter with a "
                        + "free model. Configure a provider API key to enable.");

        Checkbox enabled = new Checkbox("Enable LLM translation");
        enabled.setValue(props.isEnabled());

        ComboBox<LlmProvider> provider = new ComboBox<>("Provider");
        provider.setItems(LlmProvider.values());
        provider.setItemLabelGenerator(p -> p.name());
        provider.setValue(props.provider());

        PasswordField apiKey = new PasswordField("API key");
        apiKey.setPlaceholder("sk-... or OpenRouter key");
        apiKey.setWidthFull();
        apiKey.setValue(props.getApiKey() == null ? "" : props.getApiKey());

        TextField model = new TextField("Model");
        model.setValue(props.getModel());
        model.setHelperText("Free default: openrouter/free");

        TextField baseUrl = new TextField("Base URL (custom/Azure)");
        baseUrl.setValue(props.getBaseUrl() == null ? "" : props.getBaseUrl());
        baseUrl.setVisible(props.provider() == LlmProvider.CUSTOM
                || props.provider() == LlmProvider.AZURE);

        provider.addValueChangeListener(e -> {
            LlmProvider p = e.getValue();
            baseUrl.setVisible(p == LlmProvider.CUSTOM || p == LlmProvider.AZURE);
            if (p != null && p.defaultBaseUrl() != null && (baseUrl.getValue() == null || baseUrl.getValue().isBlank())) {
                baseUrl.setPlaceholder(p.defaultBaseUrl());
            }
        });

        Button save = new Button("Save LLM settings", e -> {
            LlmProperties target = llmProperties;
            if (target != null) {
                target.setEnabled(enabled.getValue());
                target.setProvider(provider.getValue() == null ? "OPENROUTER" : provider.getValue().name());
                target.setApiKey(apiKey.getValue());
                target.setModel(model.getValue());
                target.setBaseUrl(baseUrl.getValue());
            }
            Notification.show("LLM settings saved");
        });

        FormLayout form = new FormLayout(enabled, provider, apiKey, model, baseUrl, save);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setColspan(apiKey, 1);
        form.setColspan(baseUrl, 1);

        add(header, description, form);
    }

    private void addGeneralSection() {
        var generalHeader = new H3("General");
        add(generalHeader, new Paragraph("Manage language, notification, and search preferences."));
    }
}
