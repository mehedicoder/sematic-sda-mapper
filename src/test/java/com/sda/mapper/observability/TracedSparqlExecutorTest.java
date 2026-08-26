package com.sda.mapper.observability;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TracedSparqlExecutor records query metrics without changing results")
class TracedSparqlExecutorTest {

    @Test
    @DisplayName("Should record timer with query tag and result count for a successful query")
    void shouldRecordMetricsForResult() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TracedSparqlExecutor executor = new TracedSparqlExecutor(new PipelineMetrics(registry));

        List<String> result = executor.execute("queries/match-generic.rq", () -> List.of("a", "b", "c"), List::size);

        assertThat(result).containsExactly("a", "b", "c");
        Timer timer = registry.get("sda.sparql.query").tag("query", "queries/match-generic.rq").timer();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(registry.get("sda.sparql.results")
            .tag("query", "queries/match-generic.rq").counter().count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("Should record zero result count when a no-match result is returned")
    void shouldRecordZeroCountForNoMatch() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TracedSparqlExecutor executor = new TracedSparqlExecutor(new PipelineMetrics(registry));

        List<String> result = executor.execute("queries/hub-match-offered-required.rq", List::of, List::size);

        assertThat(result).isEmpty();
        assertThat(registry.get("sda.sparql.results")
            .tag("query", "queries/hub-match-offered-required.rq").counter().count()).isZero();
    }

    @Test
    @DisplayName("Should rethrow query failures unchanged and not record metrics")
    void shouldRethrowFailuresUnchanged() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TracedSparqlExecutor executor = new TracedSparqlExecutor(new PipelineMetrics(registry));
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> executor.execute("bad.rq", () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("boom");
        }, r -> 0))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("boom");

        assertThat(calls.get()).isEqualTo(1);
        assertThat(registry.find("sda.sparql.query").timers()).isEmpty();
    }
}
