package com.sda.mapper.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("Pipeline audit controller serves the historical timeline")
class PipelineAuditControllerTest {

    @Autowired
    private PipelineAuditController controller;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.sda.mapper.reasoning.SemanticReasonerService reasonerService;

    @BeforeEach
    void setUp() {
        try {
            jdbcTemplate.execute("DROP TABLE pipeline_audit_log");
        } catch (RuntimeException ignored) {
            // table may not exist yet
        }
        jdbcTemplate.execute("""
            CREATE TABLE pipeline_audit_log (
                id             BIGSERIAL PRIMARY KEY,
                run_id         VARCHAR(64)  NOT NULL,
                session_id     VARCHAR(64),
                requirement_id VARCHAR(255),
                stage          VARCHAR(32)  NOT NULL,
                status         VARCHAR(16)  NOT NULL,
                started_at     TIMESTAMP WITH TIME ZONE NOT NULL,
                duration_ms    BIGINT,
                detail         TEXT
            )
            """);
    }

    @AfterEach
    void tearDown() {
        try {
            jdbcTemplate.execute("DROP TABLE pipeline_audit_log");
        } catch (RuntimeException ignored) {
            // leave the shared H2 schema clean
        }
    }

    @Test
    @DisplayName("Should return the chronological timeline for a session filter")
    void shouldReturnTimelineForSession() throws Exception {
        PipelineAuditPublisher publisher =
            new PipelineAuditPublisher(new PipelineAuditRepository(jdbcTemplate));
        publisher.started("run-1", "sess-1", "REQ-001", "LIFT");
        Thread.sleep(5);
        publisher.succeeded("run-1", "sess-1", "REQ-001", "LIFT", 7, "Derived 3 entities");
        publisher.started("run-2", "sess-other", null, "LIFT");

        List<PipelineEvent> events = controller.find("sess-1", null, null, null);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).status()).isEqualTo(PipelineEvent.STARTED);
        assertThat(events.get(1).status()).isEqualTo(PipelineEvent.SUCCEEDED);
        assertThat(events.get(1).durationMs()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should return an empty list when nothing matches")
    void shouldReturnEmptyListWhenNoMatch() {
        assertThat(controller.find("missing-session", null, null, null)).isEmpty();
    }

    @Test
    @DisplayName("Should reject an out-of-range limit with a ProblemDetail error")
    void shouldRejectInvalidLimit() {
        assertThatThrownBy(() -> controller.find(null, null, null, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("limit must be between 1 and 1000");
        assertThatThrownBy(() -> controller.find(null, null, null, 1001))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should persist STARTED and SUCCEEDED rows with non-null run id when the reasoner matches")
    void shouldPersistReasonerMatchTransitions() {
        var result = reasonerService.matchByRequirementId("REQ-001");

        List<PipelineEvent> events = controller.find(null, "REQ-001", null, 50);

        assertThat(events).hasSize(2);
        assertThat(events).allSatisfy(event -> {
            assertThat(event.runId()).as("run_id must never be null").isNotBlank();
            assertThat(event.stage()).isEqualTo("MATCH");
        });
        assertThat(events.get(0).status()).isEqualTo(PipelineEvent.STARTED);
        assertThat(events.get(1).status()).isEqualTo(PipelineEvent.SUCCEEDED);
        if (result.matched()) {
            assertThat(events.get(1).detail()).contains(result.serviceName());
        }
    }
}
