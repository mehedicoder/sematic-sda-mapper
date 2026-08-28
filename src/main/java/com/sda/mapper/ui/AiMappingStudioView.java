package com.sda.mapper.ui;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.PipelineResult;
import com.sda.mapper.llm.LlmEntityTranslationService;
import com.sda.mapper.llm.LlmLiftAugmentor;
import com.sda.mapper.llm.LlmLiftSuggestion;
import com.sda.mapper.llm.LlmMappingAgent;
import com.sda.mapper.llm.LlmNotConfiguredException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI Mapping Studio (paper &sect;4.2/&sect;4.3, Summary &sect;5) &mdash; an LLM-first, separate
 * workbench distinct from the deterministic {@code MappingHubView}.
 *
 * <p>Users paste a device description or a free-text requirement, run the LLM-augmented
 * translation (Phase 22: LLM-TRANSLATE / LLM-REQUIRE), then review the drafted SDA ontology
 * entities and Accept/Reject each. Accepted entities flow through the exact same manual-entry
 * pipeline as human-typed data ({@code MappingHubService.addManualEntity}), so the deterministic
 * SPARQL matcher remains the source of truth.</p>
 */
@Route(value = "ai-mapping", layout = MainLayout.class)
@PageTitle("AI Mapping Studio")
@CssImport("./hub-styles.css")
public class AiMappingStudioView extends VerticalLayout {

    private final MappingHubService hubService;
    private final LlmEntityTranslationService llmService;
    private final LlmLiftAugmentor liftAugmentor;
    private final LlmMappingAgent mappingAgent;

    private final TextArea inputArea;
    private final Select<String> inputMode;
    private final Select<String> formatSelect;
    private final com.vaadin.flow.component.button.Button translateButton;
    private final VerticalLayout draftPanel;
    private final VerticalLayout liftPanel;
    private final VerticalLayout summaryPanel;
    private final Map<CanonicalEntity, HorizontalLayout> draftCards = new IdentityHashMap<>();
    private final Map<LlmLiftSuggestion, HorizontalLayout> liftCards = new IdentityHashMap<>();

    public AiMappingStudioView(MappingHubService hubService, LlmEntityTranslationService llmService,
                               LlmLiftAugmentor liftAugmentor, LlmMappingAgent mappingAgent) {
        this.hubService = hubService;
        this.llmService = llmService;
        this.liftAugmentor = liftAugmentor;
        this.mappingAgent = mappingAgent;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("hub-root");
        getElement().setAttribute("theme", "dark");

        var title = new H2("AI Mapping Studio");
        title.addClassName("section-title");
        title.getStyle().set("margin-top", "0");
        add(title);
        add(new Span("LLM-assisted translation of device descriptions and requirements into the "
                + "CSS-SDA ontology. Review each draft and Accept to feed it into the mapping pipeline."));

        inputMode = new Select<>();
        inputMode.setLabel("Input type");
        inputMode.setItems("Device description", "Free-text requirement");
        inputMode.setValue("Device description");

        formatSelect = new Select<>();
        formatSelect.setLabel("Format");
        formatSelect.setItems("AutomationML", "OPC UA NodeSet", "AAS", "PA-DIM");
        formatSelect.setValue("AutomationML");
        formatSelect.setVisible(true);

        inputMode.addValueChangeListener(e ->
                formatSelect.setVisible("Device description".equals(e.getValue())));

        inputArea = new TextArea("Paste a device description or requirement text");
        inputArea.setWidthFull();
        inputArea.setHeight("220px");

        var translate = new Button("Translate with AI", e -> runTranslation());
        translate.addThemeName("primary");
        if (!llmService.isAvailable()) {
            translate.setEnabled(false);
            translate.setTooltipText("LLM not configured — set APP_LLM_ENABLED=true and an API key");
        }
        this.translateButton = translate;

        var inputRow = new HorizontalLayout(inputMode, formatSelect);
        inputRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        add(inputRow, inputArea, translate);

        draftPanel = new VerticalLayout();
        draftPanel.addClassName("hub-dashboard-panel");
        draftPanel.setPadding(false);
        draftPanel.setSpacing(true);
        add(draftPanel);

        var liftButton = new Button("Suggest lift relations (AI)", e -> runLiftAugmentation());
        if (!liftAugmentor.isAvailable()) {
            liftButton.setEnabled(false);
            liftButton.setTooltipText("LLM not configured — set APP_LLM_ENABLED=true and an API key");
        }
        add(liftButton);

        liftPanel = new VerticalLayout();
        liftPanel.addClassName("hub-dashboard-panel");
        liftPanel.setPadding(false);
        liftPanel.setSpacing(true);
        add(liftPanel);

        var agentButton = new Button("Run with AI agent (end-to-end)", e -> runAgent());
        agentButton.addThemeName("primary");
        if (!mappingAgent.isAvailable()) {
            agentButton.setEnabled(false);
            agentButton.setTooltipText("LLM not configured — set APP_LLM_ENABLED=true and an API key");
        }
        add(agentButton);

        summaryPanel = new VerticalLayout();
        summaryPanel.setPadding(false);
        summaryPanel.setSpacing(true);
        add(summaryPanel);
    }

    /**
     * Runs the LLM translation over the current input and renders draft cards. Package-private for tests.
     */
    void runTranslation() {
        draftPanel.removeAll();
        summaryPanel.removeAll();
        draftCards.clear();
        String text = inputArea.getValue();
        if (text == null || text.isBlank()) {
            notify("Provide a device description or requirement first");
            return;
        }
        List<CanonicalEntity> entities;
        try {
            entities = "Free-text requirement".equals(inputMode.getValue())
                    ? llmService.translateRequirement(text)
                    : llmService.translateDeviceDescription(formatSelect.getValue(), text);
        } catch (LlmNotConfiguredException ex) {
            notify(ex.getMessage());
            return;
        }
        if (entities.isEmpty()) {
            draftPanel.add(new Span("No entities were extracted from the input."));
            return;
        }
        for (CanonicalEntity entity : entities) {
            draftPanel.add(createDraftCard(entity));
        }
    }

    private HorizontalLayout createDraftCard(CanonicalEntity entity) {
        var summary = new Span(entity.className() + " — " + entity.name());
        summary.addClassName("hub-entity-name");

        var accept = new Button("Accept", e -> acceptEntity(entity));
        accept.addThemeName("primary");
        var reject = new Button("Reject", e -> {
            HorizontalLayout card = draftCards.remove(entity);
            if (card != null) {
                draftPanel.remove(card);
            }
        });

        var card = new HorizontalLayout(summary, accept, reject);
        card.addClassName("hub-entity-card");
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        card.setWidthFull();
        draftCards.put(entity, card);
        return card;
    }

    /**
     * Accepts a drafted entity: feeds it into the deterministic pipeline. Package-private for tests.
     */
    void acceptEntity(CanonicalEntity entity) {
        PipelineResult result;
        try {
            hubService.addManualEntity(entity);
            result = hubService.reRunPipeline();
            notify("Added " + entity.name() + " — pipeline re-run");
        } catch (RuntimeException ex) {
            notify("Failed to add " + entity.name() + ": " + ex.getMessage());
            return;
        }
        HorizontalLayout card = draftCards.remove(entity);
        if (card != null) {
            draftPanel.remove(card);
        }
        if (result != null) {
            summaryPanel.removeAll();
            summaryPanel.add(new Span("Coverage "
                    + String.format(java.util.Locale.ROOT, "%.1f", result.gapReport().coveragePercent())
                    + "% — " + result.matches().size() + " semantic match(es)"));
        }
    }

    private void notify(String message) {
        if (com.vaadin.flow.component.UI.getCurrent() != null) {
            Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        }
    }

    /**
     * Runs the full LLM-augmented mapping agent over the current input. Package-private for tests.
     */
    void runAgent() {
        summaryPanel.removeAll();
        String text = inputArea.getValue();
        if (text == null || text.isBlank()) {
            notify("Provide a device description or requirement first");
            return;
        }
        boolean isRequirement = "Free-text requirement".equals(inputMode.getValue());
        com.sda.mapper.llm.LlmAgentResult result;
        try {
            result = mappingAgent.run(hubService, text, isRequirement);
        } catch (LlmNotConfiguredException ex) {
            notify(ex.getMessage());
            return;
        } catch (RuntimeException ex) {
            notify("Agent run failed: " + ex.getMessage());
            return;
        }
        summaryPanel.add(new Span("LLM agent coverage "
                + String.format(java.util.Locale.ROOT, "%.1f", result.coveragePercent()) + "%"));
        for (com.sda.mapper.llm.LlmAgentStep step : result.plan().steps()) {
            summaryPanel.add(new Span(step.name() + " — " + step.description()));
        }
        notify("AI agent produced complete-manifest.json");
    }

    /**
     * Runs the LLM lift augmentor over the current model and renders lift drafts. Package-private for tests.
     */
    void runLiftAugmentation() {
        liftPanel.removeAll();
        liftCards.clear();
        List<CanonicalEntity> entities = hubService.getSession().getModel().allEntities();
        if (entities.isEmpty()) {
            notify("Upload or translate some entities first");
            return;
        }
        List<LlmLiftSuggestion> suggestions;
        try {
            suggestions = liftAugmentor.suggestLifts(entities);
        } catch (LlmNotConfiguredException ex) {
            notify(ex.getMessage());
            return;
        }
        if (suggestions.isEmpty()) {
            liftPanel.add(new Span("No missing lift relations were proposed."));
            return;
        }
        for (LlmLiftSuggestion suggestion : suggestions) {
            liftPanel.add(createLiftCard(suggestion));
        }
    }

    private HorizontalLayout createLiftCard(LlmLiftSuggestion suggestion) {
        var summary = new Span(suggestion.entityId() + " "
                + suggestion.relation() + " → " + suggestion.targetClass() + " \"" + suggestion.targetName() + "\"");
        summary.addClassName("hub-entity-name");

        var accept = new Button("Accept", e -> acceptLiftSuggestion(suggestion));
        accept.addThemeName("primary");
        var reject = new Button("Reject", e -> {
            HorizontalLayout card = liftCards.remove(suggestion);
            if (card != null) {
                liftPanel.remove(card);
            }
        });

        var card = new HorizontalLayout(summary, accept, reject);
        card.addClassName("hub-entity-card");
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        card.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        card.setWidthFull();
        liftCards.put(suggestion, card);
        return card;
    }

    /**
     * Accepts a lift suggestion: materialises the target individual via the manual-entry path. Package-private for tests.
     */
    void acceptLiftSuggestion(LlmLiftSuggestion suggestion) {
        String targetId = "ai-lift-" + UUID.randomUUID();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("id", targetId);
        attributes.put("name", suggestion.targetName());
        attributes.put("relation", suggestion.relation());
        attributes.put("sourceEntityId", suggestion.entityId());
        ManualEntity target = new ManualEntity(
                suggestion.targetClass(), targetId, suggestion.targetName(), attributes);
        PipelineResult result;
        try {
            hubService.addManualEntity(target);
            result = hubService.reRunPipeline();
            notify("Added " + suggestion.targetName() + " — pipeline re-run");
        } catch (RuntimeException ex) {
            notify("Failed to add lift target: " + ex.getMessage());
            return;
        }
        HorizontalLayout card = liftCards.remove(suggestion);
        if (card != null) {
            liftPanel.remove(card);
        }
        if (result != null) {
            summaryPanel.removeAll();
            summaryPanel.add(new Span("Coverage "
                    + String.format(java.util.Locale.ROOT, "%.1f", result.gapReport().coveragePercent())
                    + "% — " + result.matches().size() + " semantic match(es)"));
        }
    }
}
