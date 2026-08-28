package com.sda.mapper.llm;

import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("LlmEntityTranslationService")
class LlmEntityTranslationServiceTest {

    private static final String SERVICE_JSON = "[{\"className\":\"Service\","
        + "\"name\":\"Flowmeter Diagnostics Service\","
        + "\"attributes\":{\"protocol\":\"OPC-UA\",\"description\":\"diagnostics\"}}]";

    private LlmEntityTranslationService service(boolean configured, String reply) {
        LlmProperties props = mock(LlmProperties.class);
        when(props.isConfigured()).thenReturn(configured);
        ChatCompletionsClient client = mock(ChatCompletionsClient.class);
        when(client.complete(anyString(), anyString())).thenReturn(reply);
        return new LlmEntityTranslationService(props, client);
    }

    @Test
    @DisplayName("translateDeviceDescription converts a supported entity to a CanonicalEntity")
    void translatesSupportedEntity() {
        LlmEntityTranslationService service = service(true, SERVICE_JSON);
        List<CanonicalEntity> entities = service.translateDeviceDescription("AutomationML", "desc");
        assertEquals(1, entities.size());
        assertTrue(entities.get(0) instanceof Service);
        assertEquals("Flowmeter Diagnostics Service", entities.get(0).name());
    }

    @Test
    @DisplayName("unsupported classes are skipped, not failed")
    void skipsUnsupportedClass() {
        LlmEntityTranslationService service = service(true,
            "[{\"className\":\"Frobnicate\",\"name\":\"x\",\"attributes\":{}}]");
        assertTrue(service.translateDeviceDescription("AAS", "desc").isEmpty());
    }

    @Test
    @DisplayName("disabled feature throws LlmNotConfiguredException")
    void notConfiguredThrows() {
        LlmEntityTranslationService service = service(false, SERVICE_JSON);
        assertThrows(LlmNotConfiguredException.class,
            () -> service.translateRequirement("some requirement"));
    }

    @Test
    @DisplayName("translateRequirement extracts UseCaseRequirement / RequiredCapability / VerificationCriterion")
    void translatesRequirement() {
        String json = "[{\"className\":\"UseCaseRequirement\",\"name\":\"REQ-001\","
            + "\"attributes\":{\"description\":\"flowmeter diagnostics\"}},"
            + "{\"className\":\"RequiredCapability\",\"name\":\"Diag-Cap\","
            + "\"attributes\":{\"functionClass\":\"Diagnostic\"}},"
            + "{\"className\":\"VerificationCriterion\",\"name\":\"VC-1\","
            + "\"attributes\":{\"criterion\":\"NE107\"}}]";
        LlmEntityTranslationService service = service(true, json);
        List<CanonicalEntity> entities = service.translateRequirement("flowmeter diagnostics");
        assertEquals(3, entities.size());
        assertTrue(entities.stream().anyMatch(e -> e.className().equals("UseCaseRequirement")));
        assertTrue(entities.stream().anyMatch(e -> e.className().equals("RequiredCapability")));
        assertTrue(entities.stream().anyMatch(e -> e.className().equals("VerificationCriterion")));
    }
}
