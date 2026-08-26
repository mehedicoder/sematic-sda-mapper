package com.sda.mapper.observability;

import org.springframework.stereotype.Component;

import java.util.function.ToIntFunction;

/**
 * Wraps SPARQL query execution with observability: a {@code sda.sparql.query}
 * timer tagged with the query file name, a result-count metric, and a
 * {@code sparql.result.count} attribute on the current span.
 *
 * <p>The executor never changes query semantics — it only measures.</p>
 */
@Component
public class TracedSparqlExecutor {

    private final PipelineMetrics metrics;

    /**
     * Creates the executor.
     *
     * @param metrics pipeline SLI metrics
     */
    public TracedSparqlExecutor(PipelineMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Executes the supplied query work and records duration, query name and result count.
     *
     * @param <T>         the result type (e.g. a list of matches or a match result)
     * @param queryName   low-cardinality query identifier (query resource file name)
     * @param work        the query execution
     * @param resultCount extracts the number of produced rows from the result
     * @return the unmodified query result
     */
    public <T> T execute(String queryName, PipelineQuery<T> work, ToIntFunction<T> resultCount) {
        long startNanos = System.nanoTime();
        T result = work.run();
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
        int count;
        try {
            count = resultCount.applyAsInt(result);
        } catch (RuntimeException e) {
            count = 0;
        }
        metrics.recordSparqlQuery(queryName, durationMs, count);
        return result;
    }

    /**
     * Query execution unit, allowed to throw checked exceptions.
     *
     * @param <T> result type
     */
    @FunctionalInterface
    public interface PipelineQuery<T> {
        /**
         * Runs the query.
         *
         * @return the query result
         * @throws Exception on execution failure
         */
        T run();
    }
}
