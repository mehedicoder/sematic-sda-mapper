package com.sda.mapper.observability;

import com.sda.mapper.reasoning.ServiceMatchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Match dimension tagger mirrors explanation dimensions")
class MatchDimensionTaggerTest {

    private ServiceMatchResult matchedResult() {
        return new ServiceMatchResult(
            true, "FlowmeterDiagnosticsService", "registry.local/flowmeter-diagnostics:1.0",
            "AssetConditionMonitoringCapability", "AssetDiagnosticsSkill", "OPC-UA", "read-only",
            "PumpBinding", "PumpConnector", "DEPLOY-OnPrem",
            0.94, 45, "The service matches...", null, "REQ-001", "SIL-2", "SoftRealTime", "plant-network", null);
    }

    @Test
    @DisplayName("Should tag all dimensions as pass for a matched service")
    void shouldPassAllDimensionsOnMatch() {
        Map<String, String> dims = new MatchDimensionTagger().dimensions(matchedResult());

        assertThat(dims).containsKeys("capability", "protocol", "accessMode",
            "deployment", "recall", "latency", "r2a-concept");
        assertThat(dims.values()).containsOnly("pass");
    }

    @Test
    @DisplayName("Should tag failing dimensions for a no-match result")
    void shouldFailDimensionsForNoMatch() {
        ServiceMatchResult noMatch = ServiceMatchResult.noMatch("No service satisfied the constraints");

        Map<String, String> dims = new MatchDimensionTagger().dimensions(noMatch);

        assertThat(dims).containsValue("fail");
        assertThat(new MatchDimensionTagger().failureSummary(noMatch))
            .startsWith("failed dimensions: ")
            .contains("capability");
    }

    @Test
    @DisplayName("Should return no failure summary when every dimension passes")
    void shouldReturnNullSummaryWhenNothingFailed() {
        assertThat(new MatchDimensionTagger().failureSummary(matchedResult())).isNull();
    }

    @Test
    @DisplayName("Should handle a null result without throwing")
    void shouldHandleNullResult() {
        assertThat(new MatchDimensionTagger().dimensions(null)).isEmpty();
    }
}
