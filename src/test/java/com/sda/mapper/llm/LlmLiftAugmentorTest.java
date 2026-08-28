package com.sda.mapper.llm;

import com.sda.mapper.hub.model.CanonicalEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("LlmLiftAugmentor")
class LlmLiftAugmentorTest {

    private static final String VALID_JSON = "[{"
        + "\"entityId\":\"svc-1\","
        + "\"entityClass\":\"Service\","
        + "\"relation\":\"realisedBy\","
        + "\"targetClass\":\"Skill\","
        + "\"targetName\":\"Pump Monitoring Skill\","
        + "\"rationale\":\"the service is realised by this skill\"}]";

    private LlmLiftAugmentor service(boolean configured, String reply) {
        LlmProperties props = mock(LlmProperties.class);
        when(props.isConfigured()).thenReturn(configured);
        ChatCompletionsClient client = mock(ChatCompletionsClient.class);
        when(client.complete(anyString(), anyString())).thenReturn(reply);
        return new LlmLiftAugmentor(props, client);
    }

    private static CanonicalEntity entity(String id, String className, String name) {
        CanonicalEntity e = mock(CanonicalEntity.class);
        when(e.id()).thenReturn(id);
        when(e.className()).thenReturn(className);
        when(e.name()).thenReturn(name);
        return e;
    }

    @Test
    @DisplayName("suggests a valid lift relation for a known entity")
    void suggestsValidRelation() {
        LlmLiftAugmentor service = service(true, VALID_JSON);
        List<LlmLiftSuggestion> suggestions =
            service.suggestLifts(List.of(entity("svc-1", "Service", "Pump Service")));
        assertEquals(1, suggestions.size());
        LlmLiftSuggestion s = suggestions.get(0);
        assertEquals("svc-1", s.entityId());
        assertEquals("realisedBy", s.relation());
        assertEquals("Skill", s.targetClass());
        assertEquals("Pump Monitoring Skill", s.targetName());
    }

    @Test
    @DisplayName("disabled feature throws LlmNotConfiguredException")
    void notConfiguredThrows() {
        LlmLiftAugmentor service = service(false, VALID_JSON);
        assertThrows(LlmNotConfiguredException.class,
            () -> service.suggestLifts(List.of(entity("svc-1", "Service", "Pump Service"))));
    }

    @Test
    @DisplayName("invalid relation is skipped")
    void skipsInvalidRelation() {
        String json = "[{\"entityId\":\"svc-1\",\"entityClass\":\"Service\","
            + "\"relation\":\"bogusRelation\",\"targetClass\":\"Skill\","
            + "\"targetName\":\"X\",\"rationale\":\"r\"}]";
        LlmLiftAugmentor service = service(true, json);
        assertTrue(service.suggestLifts(List.of(entity("svc-1", "Service", "Pump Service"))).isEmpty());
    }

    @Test
    @DisplayName("suggestion for an unknown entityId is skipped")
    void skipsUnknownEntity() {
        LlmLiftAugmentor service = service(true, VALID_JSON);
        assertTrue(service.suggestLifts(List.of(entity("other", "Service", "Other"))).isEmpty());
    }

    @Test
    @DisplayName("empty input yields no suggestions without calling the model")
    void emptyInputYieldsNothing() {
        LlmLiftAugmentor service = service(true, VALID_JSON);
        assertTrue(service.suggestLifts(List.of()).isEmpty());
    }
}
