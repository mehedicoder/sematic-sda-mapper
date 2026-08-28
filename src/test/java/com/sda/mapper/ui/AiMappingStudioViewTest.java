package com.sda.mapper.ui;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.model.Service;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.MappingHubSession;
import com.sda.mapper.hub.service.PipelineResult;
import com.sda.mapper.llm.LlmAgentResult;
import com.sda.mapper.llm.LlmAgentRunPlan;
import com.sda.mapper.llm.LlmAgentStep;
import com.sda.mapper.llm.LlmEntityTranslationService;
import com.sda.mapper.llm.LlmLiftAugmentor;
import com.sda.mapper.llm.LlmLiftSuggestion;
import com.sda.mapper.llm.LlmMappingAgent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AI Mapping Studio UI")
class AiMappingStudioViewTest {

    private final MappingHubService hub = mock(MappingHubService.class);
    private final LlmEntityTranslationService llm = mock(LlmEntityTranslationService.class);
    private final LlmLiftAugmentor lift = mock(LlmLiftAugmentor.class);
    private final LlmMappingAgent agent = mock(LlmMappingAgent.class);
    private final PipelineResult result = mock(PipelineResult.class);
    private final GapReport report = mock(GapReport.class);

    private AiMappingStudioView createView() {
        return new AiMappingStudioView(hub, llm, lift, agent);
    }

    private static Object getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("view is annotated with route /ai-mapping and page title")
    void navigationMetadata() {
        Route route = AiMappingStudioView.class.getAnnotation(Route.class);
        assertEquals("ai-mapping", route.value());
        assertEquals(MainLayout.class, route.layout());
        assertEquals("AI Mapping Studio",
                AiMappingStudioView.class.getAnnotation(PageTitle.class).value());
    }

    @Test
    @DisplayName("translate button drafts entities and Accept feeds the pipeline")
    void translateAndAccept() {
        when(llm.isAvailable()).thenReturn(true);
        CanonicalEntity entity = new Service("ai-1", "Flowmeter Diagnostics Service",
                "", "OPC-UA", Optional.of("manual-entry"), false);
        when(llm.translateDeviceDescription("AutomationML", "desc")).thenReturn(List.of(entity));
        when(hub.addManualEntity(any())).thenReturn(result);
        when(hub.reRunPipeline()).thenReturn(result);
        when(result.gapReport()).thenReturn(report);
        when(report.coveragePercent()).thenReturn(100.0);
        when(result.matches()).thenReturn(List.of());

        AiMappingStudioView view = createView();
        var inputArea = (com.vaadin.flow.component.textfield.TextArea) getField(view, "inputArea");
        inputArea.setValue("desc");

        view.runTranslation();
        var draftPanel = (VerticalLayout) getField(view, "draftPanel");
        assertEquals(1, draftPanel.getChildren().count());

        view.acceptEntity(entity);
        verify(hub).addManualEntity(entity);
        verify(hub).reRunPipeline();
        assertTrue(draftPanel.getChildren().findAny().isEmpty());
    }

    @Test
    @DisplayName("disabled LLM disables the translate button")
    void disabledWhenLlmUnavailable() {
        when(llm.isAvailable()).thenReturn(false);
        AiMappingStudioView view = createView();
        var translate = (com.vaadin.flow.component.button.Button) getField(view, "translateButton");
        assertFalse(translate.isEnabled());
    }

    @Test
    @DisplayName("lift augmentor drafts relation suggestions and Accept materialises the target entity")
    void liftAugmentationAndAccept() {
        when(lift.isAvailable()).thenReturn(true);

        MappingHubSession session = mock(MappingHubSession.class);
        CanonicalAssetModel model = mock(CanonicalAssetModel.class);
        CanonicalEntity existing = mock(CanonicalEntity.class);
        when(existing.id()).thenReturn("svc-1");
        when(model.allEntities()).thenReturn(List.of(existing));
        when(hub.getSession()).thenReturn(session);
        when(session.getModel()).thenReturn(model);

        LlmLiftSuggestion suggestion = new LlmLiftSuggestion(
                "svc-1", "Service", "realisedBy", "Skill", "Pump Monitoring Skill", "r");
        when(lift.suggestLifts(any())).thenReturn(List.of(suggestion));

        when(hub.addManualEntity(any())).thenReturn(result);
        when(hub.reRunPipeline()).thenReturn(result);
        when(result.gapReport()).thenReturn(report);
        when(report.coveragePercent()).thenReturn(80.0);
        when(result.matches()).thenReturn(List.of());

        AiMappingStudioView view = createView();
        view.runLiftAugmentation();
        var liftPanel = (VerticalLayout) getField(view, "liftPanel");
        assertEquals(1, liftPanel.getChildren().count());

        view.acceptLiftSuggestion(suggestion);
        verify(hub).addManualEntity(any(com.sda.mapper.hub.model.ManualEntity.class));
        verify(hub).reRunPipeline();
    }

    @Test
    @DisplayName("Run with AI agent produces a plan + manifest summary")
    void runAgentProducesPlan() {
        when(agent.isAvailable()).thenReturn(true);
        LlmAgentResult agentResult = new LlmAgentResult(
                new LlmAgentRunPlan(List.of(new LlmAgentStep("LLM-TRANSLATE", "translated")), "done"),
                "{}", 72.0);
        when(agent.run(any(), anyString(), anyBoolean())).thenReturn(agentResult);

        AiMappingStudioView view = createView();
        var inputArea = (com.vaadin.flow.component.textfield.TextArea) getField(view, "inputArea");
        inputArea.setValue("a flowmeter requirement");
        view.runAgent();

        var summary = (VerticalLayout) getField(view, "summaryPanel");
        assertEquals(2, summary.getChildren().count());
    }
}
