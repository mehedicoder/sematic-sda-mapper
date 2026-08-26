package com.sda.mapper.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Pipeline audit publisher persists transitions and broadcasts to listeners")
class PipelineAuditPublisherTest {

    private static final String DDL = """
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
        """;

    private EmbeddedDatabase database;
    private JdbcTemplate jdbcTemplate;
    private PipelineAuditRepository repository;
    private PipelineAuditPublisher publisher;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .generateUniqueName(true)
            .build();
        jdbcTemplate = new JdbcTemplate(database);
        jdbcTemplate.execute(DDL);
        repository = new PipelineAuditRepository(jdbcTemplate);
        publisher = new PipelineAuditPublisher(repository);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    @DisplayName("Should persist STARTED and SUCCEEDED rows for one stage transition")
    void shouldPersistStageTransition() {
        publisher.started("run-1", "session-1", null, "LIFT");
        publisher.succeeded("run-1", "session-1", null, "LIFT", 12, "Derived 3 entities");

        List<PipelineEvent> events = repository.find(null, null, "run-1", 100);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).status()).isEqualTo(PipelineEvent.STARTED);
        assertThat(events.get(0).stage()).isEqualTo("LIFT");
        assertThat(events.get(1).status()).isEqualTo(PipelineEvent.SUCCEEDED);
        assertThat(events.get(1).durationMs()).isEqualTo(12);
        assertThat(events.get(1).detail()).isEqualTo("Derived 3 entities");
    }

    @Test
    @DisplayName("Should record FAILED status with the error reason as detail")
    void shouldRecordFailedTransition() {
        publisher.started("run-2", null, "REQ-001", "MATCH");
        publisher.failed("run-2", null, "REQ-001", "MATCH", 5, "SPARQL exploded");

        PipelineEvent event = repository.find(null, "REQ-001", "run-2", 10)
            .stream()
            .filter(e -> PipelineEvent.FAILED.equals(e.status()))
            .findFirst()
            .orElseThrow();

        assertThat(event.detail()).isEqualTo("SPARQL exploded");
        assertThat(event.durationMs()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should broadcast every recorded event to live listeners")
    void shouldBroadcastToListeners() {
        List<PipelineEvent> received = new CopyOnWriteArrayList<>();
        publisher.addListener(received::add);

        publisher.started("run-3", null, null, "INFER");
        publisher.succeeded("run-3", null, null, "INFER", 1, "ok");

        assertThat(received).hasSize(2);
        assertThat(received.stream().map(PipelineEvent::status))
            .containsExactly(PipelineEvent.STARTED, PipelineEvent.SUCCEEDED);
    }

    @Test
    @DisplayName("Should swallow persistence failures so the pipeline never breaks")
    void shouldSwallowPersistenceFailures() {
        PipelineAuditPublisher broken = new PipelineAuditPublisher(
            new PipelineAuditRepository(new JdbcTemplate(database)));

        // Drop the table so every insert fails.
        jdbcTemplate.execute("DROP TABLE pipeline_audit_log");

        List<PipelineEvent> received = new ArrayList<>();
        broken.addListener(received::add);

        assertThatCode(() -> {
            broken.started("run-4", null, null, "PARSE");
            broken.succeeded("run-4", null, null, "PARSE", 1, "ok");
        }).doesNotThrowAnyException();

        assertThat(received).hasSize(2);
    }

    @Test
    @DisplayName("Should filter the timeline by session and order it chronologically")
    void shouldFilterBySessionOrdered() throws Exception {
        publisher.started("run-a", "sess-9", null, "LIFT");
        Thread.sleep(5);
        publisher.started("run-b", "sess-9", null, "LIFT");
        publisher.started("run-c", "sess-other", null, "LIFT");

        List<PipelineEvent> events = repository.find("sess-9", null, null, 100);

        assertThat(events).extracting(PipelineEvent::runId)
            .containsExactly("run-a", "run-b");
    }
}
