package com.sda.mapper.llm;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.hub.service.MappingHubService;
import com.sda.mapper.hub.service.MappingHubSession;
import com.sda.mapper.hub.service.PipelineResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("LlmMappingAgent")
class LlmMappingAgentTest {

    private LlmMappingAgent agent(boolean configured) {
        LlmProperties props = mock(LlmProperties.class);
        when(props.isConfigured()).thenReturn(configured);
        LlmEntityTranslationService translator = mock(LlmEntityTranslationService.class);
        LlmLiftAugmentor lift = mock(LlmLiftAugmentor.class);
        LlmMatchAugmentor match = mock(LlmMatchAugmentor.class);
        return new LlmMappingAgent(translator, lift, match, props);
    }

    private static MappingHubService wiredHub(PipelineResult pr) {
        MappingHubService hub = mock(MappingHubService.class);
        CanonicalEntity entity = mock(CanonicalEntity.class);
        CanonicalAssetModel model = mock(CanonicalAssetModel.class);
        MappingHubSession session = mock(MappingHubSession.class);
        when(model.allEntities()).thenReturn(List.of(entity));
        when(hub.getSession()).thenReturn(session);
        when(session.getModel()).thenReturn(model);
        when(hub.addManualEntity(any())).thenReturn(null);
        when(hub.reRunPipeline()).thenReturn(pr);
        when(hub.buildManifest()).thenReturn("{}");

        when(pr.gapReport()).thenReturn(mock(GapReport.class));
        when(pr.model()).thenReturn(model);
        when(pr.gapReport().coveragePercent()).thenReturn(60.0);
        return hub;
    }

    @Test
    @DisplayName("orchestrates translate -> lift -> match-augment -> manifest")
    void orchestratesFullPipeline() {
        LlmMappingAgent agent = agent(true);
        LlmEntityTranslationService translator = extract(agent, LlmEntityTranslationService.class);
        LlmLiftAugmentor lift = extract(agent, LlmLiftAugmentor.class);
        LlmMatchAugmentor match = extract(agent, LlmMatchAugmentor.class);

        CanonicalEntity entity = mock(CanonicalEntity.class);
        when(translator.translateDeviceDescription(anyString(), anyString())).thenReturn(List.of(entity));
        LlmLiftSuggestion suggestion = new LlmLiftSuggestion(
            "svc-1", "Service", "realisedBy", "Skill", "Skill X", "r");
        when(lift.suggestLifts(any())).thenReturn(List.of(suggestion));
        when(match.suggestGapDrafts(any(), any())).thenReturn(new LlmGapResult(List.of(), List.of()));

        MappingHubService hub = wiredHub(mock(PipelineResult.class));

        LlmAgentResult result = agent.run(hub, "a flowmeter description", false);
        assertEquals(4, result.plan().steps().size());
        assertEquals("{}", result.manifest());
        assertEquals(60.0, result.coveragePercent());
    }

    @Test
    @DisplayName("disabled feature throws LlmNotConfiguredException")
    void notConfiguredThrows() {
        LlmMappingAgent agent = agent(false);
        MappingHubService hub = mock(MappingHubService.class);
        assertThrows(LlmNotConfiguredException.class, () -> agent.run(hub, "x", false));
    }

    @SuppressWarnings("unchecked")
    private static <T> T extract(LlmMappingAgent agent, Class<T> type) {
        // The sub-services are package-private final fields; reach them via reflection for stubbing.
        try {
            var f = LlmMappingAgent.class.getDeclaredField(
                type == LlmEntityTranslationService.class ? "entityTranslator"
                    : type == LlmLiftAugmentor.class ? "liftAugmentor"
                    : "matchAugmentor");
            f.setAccessible(true);
            return (T) f.get(agent);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
