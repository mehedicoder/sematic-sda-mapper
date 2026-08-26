package com.sda.mapper.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Grafana pipeline dashboard JSON is valid and parseable")
class GrafanaDashboardJsonParseTest {

    @Test
    @DisplayName("Should parse the exported dashboard and contain the SLI panels")
    void shouldParseDashboardJson() throws IOException {
        Path dashboard = Path.of("docs", "grafana", "sda-mapper-pipeline-dashboard.json");
        assertThat(dashboard).as("exported dashboard file must exist").exists();

        JsonNode root = new ObjectMapper().readTree(dashboard.toFile());

        assertThat(root.path("title").asText()).contains("Pipeline Observability");
        JsonNode panels = root.path("panels");
        assertThat(panels.isArray() && panels.size() > 0)
            .as("dashboard must define at least one panel").isTrue();

            String expressions = allPromQlExpressions(root);
            assertThat(expressions)
                .contains("sda_pipeline_runs_total")
                .contains("sda_pipeline_match_result_total")
                .contains("sda_pipeline_stage_duration_seconds")
                .contains("sda_sparql_query_seconds");
    }

    private String allPromQlExpressions(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        if (node.isTextual() && node.asText().contains("sda_")) {
            sb.append(node.asText()).append('\n');
        }
        if (node.isContainerNode()) {
            node.forEach(child -> sb.append(allPromQlExpressions(child)));
        }
        return sb.toString();
    }
}
