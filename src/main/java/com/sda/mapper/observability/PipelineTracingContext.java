package com.sda.mapper.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Scope;
import org.slf4j.MDC;

/**
 * Opens a correlated pipeline run scope: one root observation per run, OpenTelemetry
 * baggage entries ({@code run.id}, {@code session.id}, {@code requirement.id}) and
 * MDC values so every log line inside the run carries the correlation ids.
 *
 * <p>Stage observations created by {@link PipelineStageObserver} while a run scope is
 * open are parented to the run observation automatically via the registry's thread-local
 * context, yielding one trace with one child span per stage.</p>
 */
public class PipelineTracingContext {

    /** MDC key for the pipeline run id. */
    public static final String MDC_RUN_ID = "runId";
    /** MDC key for the mapping hub session id. */
    public static final String MDC_SESSION_ID = "sessionId";
    /** MDC key for the requirement id. */
    public static final String MDC_REQUIREMENT_ID = "requirementId";

    private final ObservationRegistry observationRegistry;

    /**
     * Creates the context.
     *
     * @param observationRegistry shared micrometer observation registry
     */
    public PipelineTracingContext(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    /**
     * A closeable pipeline run scope; closing restores the previous MDC/baggage state
     * and stops the root observation.
     */
    public interface RunScope extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Opens a run scope for one pipeline execution.
     *
     * @param runId         run correlation id
     * @param sessionId     session id, may be {@code null}
     * @param requirementId requirement id, may be {@code null}
     * @return the scope to close when the run ends
     */
    public RunScope openRun(String runId, String sessionId, String requirementId) {
        // Baggage and MDC are established BEFORE the observation starts so the
        // root span is created inside the baggage context and every observer
        // already sees the correlation ids.
        var baggageScope = Baggage.builder()
            .put("run.id", runId == null ? "" : runId)
            .put("session.id", sessionId == null ? "" : sessionId)
            .put("requirement.id", requirementId == null ? "" : requirementId)
            .build()
            .storeInContext(io.opentelemetry.context.Context.current())
            .makeCurrent();

        MDC.put(MDC_RUN_ID, runId);
        if (sessionId != null) {
            MDC.put(MDC_SESSION_ID, sessionId);
        }
        if (requirementId != null) {
            MDC.put(MDC_REQUIREMENT_ID, requirementId);
        }

        Observation run = Observation.createNotStarted("sda.pipeline.run", observationRegistry)
            .contextualName("pipeline run " + runId)
            .highCardinalityKeyValue("run.id", runId);
        if (sessionId != null && !sessionId.isBlank()) {
            run.lowCardinalityKeyValue("session.id", sessionId);
        }
        if (requirementId != null && !requirementId.isBlank()) {
            run.lowCardinalityKeyValue("requirement.id", requirementId);
        }
        run.start();
        // Opening the scope makes the run observation current on this thread so
        // stage observations nest underneath it as child spans.
        Observation.Scope scope = run.openScope();

        return () -> {
            try {
                scope.close();
                run.stop();
            } finally {
                try {
                    baggageScope.close();
                } finally {
                    MDC.remove(MDC_RUN_ID);
                    MDC.remove(MDC_SESSION_ID);
                    MDC.remove(MDC_REQUIREMENT_ID);
                }
            }
        };
    }
}
