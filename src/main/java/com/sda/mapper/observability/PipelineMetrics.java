package com.sda.mapper.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Pipeline SLI metrics exposed at {@code /actuator/prometheus}:
 * <ul>
 *   <li>{@code sda.pipeline.runs} — counter per run outcome</li>
 *   <li>{@code sda.pipeline.match.result} — counter for match hit / no-hit</li>
 *   <li>{@code sda.pipeline.stage.duration} — timer per stage (see {@link PipelineStageObserver})</li>
 *   <li>{@code sda.sparql.query} — timer per SPARQL query name (see {@link TracedSparqlExecutor})</li>
 * </ul>
 */
@Component
public class PipelineMetrics {

    private final MeterRegistry registry;

    /**
     * Creates the metrics helper.
     *
     * @param registry micrometer registry (Prometheus in production)
     */
    public PipelineMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Records the completion of one pipeline run.
     *
     * @param path  which entry path ran the pipeline ({@code hub} or {@code reasoning})
     * @param outcome {@code success} or {@code failure}
     */
    public void recordRun(String path, String outcome) {
        registry.counter("sda.pipeline.runs", "path", path, "outcome", outcome).increment();
    }

    /**
     * Records a match outcome.
     *
     * @param result {@code hit} or {@code no-hit}
     */
    public void recordMatchResult(String result) {
        registry.counter("sda.pipeline.match.result", "result", result).increment();
    }

    /**
     * Stops a stage timer started by {@link PipelineStageObserver}.
     *
     * @param stage      stage name
     * @param durationMs stage duration
     */
    public void recordStageDuration(String stage, long durationMs) {
        Timer.builder("sda.pipeline.stage.duration")
            .description("Pipeline stage duration")
            .tag("stage", stage)
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records a SPARQL query execution.
     *
     * @param queryName  query file name (low cardinality)
     * @param durationMs execution duration
     * @param resultCount number of rows returned
     */
    public void recordSparqlQuery(String queryName, long durationMs, int resultCount) {
        Timer.builder("sda.sparql.query")
            .description("SPARQL query execution duration")
            .tag("query", queryName)
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
        registry.counter("sda.sparql.results", "query", queryName).increment(resultCount);
    }

    /**
     * Records an LLM concept-suggestion request and its duration.
     *
     * @param outcome     {@code success} or {@code failure}
     * @param durationMs  time spent calling the LLM translation feature
     */
    public void recordLlmSuggestion(String outcome, long durationMs) {
        registry.counter("sda.pipeline.llm.suggestions", "outcome", outcome).increment();
        Timer.builder("sda.pipeline.llm.duration")
            .description("LLM concept suggestion duration")
            .tag("outcome", outcome)
            .register(registry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
