package com.sda.mapper.reasoning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExplanationService match explanation generation")
class ExplanationServiceTest {

    private ExplanationService service;

    private static final ServiceMatchResult MATCHED_RESULT = new ServiceMatchResult(
        true, "Flowmeter Diagnostics Service", "ghcr.io/sda/flowmeter-diag:1.0",
        "FlowRate Diagnostics", "OPC-UA", "read-only", "OPC-UA binding", "OPC-UA Gateway", "Plant Network",
        0.95, 45, "Matched", "Defined", null
    );

    private static final ServiceMatchResult NO_MATCH_RESULT =
        ServiceMatchResult.noMatch("No service matched inline parameters: protocol=MQTT, accessMode=read-only");

    @BeforeEach
    void setUp() {
        service = new ExplanationService();
    }

    @Test
    @DisplayName("Should return non-empty explanation for matched result")
    void shouldReturnNonEmptyExplanationForMatch() {
        String explanation = service.explain(MATCHED_RESULT);
        assertNotNull(explanation);
        assertFalse(explanation.isBlank(), "Explanation for matched result should not be blank");
    }

    @Test
    @DisplayName("Explanation should contain capability keyword")
    void shouldContainCapabilityKeyword() {
        String explanation = service.explain(MATCHED_RESULT);
        assertTrue(explanation.toLowerCase().contains("capability"),
            "Explanation should mention capability");
    }

    @Test
    @DisplayName("Explanation should contain protocol keyword")
    void shouldContainProtocolKeyword() {
        String explanation = service.explain(MATCHED_RESULT);
        assertTrue(explanation.toLowerCase().contains("protocol"),
            "Explanation should mention protocol");
    }

    @Test
    @DisplayName("Explanation should contain deployment context keyword")
    void shouldContainDeploymentContextKeyword() {
        String explanation = service.explain(MATCHED_RESULT);
        assertTrue(explanation.toLowerCase().contains("deployment context"),
            "Explanation should mention deployment context");
    }

    @Test
    @DisplayName("Explanation should contain recall and latency values")
    void shouldContainRecallAndLatency() {
        String explanation = service.explain(MATCHED_RESULT);
        assertTrue(explanation.contains("0.95"), "Explanation should contain recall value");
        assertTrue(explanation.contains("45"), "Explanation should contain latency value");
    }

    @Test
    @DisplayName("Should return explanation for no-match result")
    void shouldReturnExplanationForNoMatch() {
        String explanation = service.explain(NO_MATCH_RESULT);
        assertNotNull(explanation);
        assertTrue(explanation.contains("No Match Found"),
            "No-match explanation should contain 'No Match Found' header");
    }

    @Test
    @DisplayName("Should return message for null result")
    void shouldHandleNullResult() {
        String explanation = service.explain(null);
        assertNotNull(explanation);
        assertEquals("No result to explain.", explanation);
    }
}
