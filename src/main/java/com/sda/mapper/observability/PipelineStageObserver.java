package com.sda.mapper.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Instruments one pipeline stage: opens a child observation (span), a stage timer,
 * and writes STARTED / SUCCEEDED / FAILED audit events.
 *
 * <p>Used with try-with-resources:</p>
 * <pre>{@code
 * try (PipelineStageObserver.Stage stage = observer.begin("LIFT", runId, sessionId, null)) {
 *     ... stage work ...
 *     stage.detail("Lifted 12 entities");
 * }
 * }</pre>
 *
 * <p>An exception thrown inside the block marks the stage FAILED on close and is
 * rethrown unchanged — the observer never alters pipeline behaviour.</p>
 */
@Component
public class PipelineStageObserver {

    private final ObservationRegistry observationRegistry;
    private final PipelineAuditPublisher auditPublisher;
    private final PipelineMetrics metrics;

    /**
     * Creates the stage observer.
     *
     * @param observationRegistry shared micrometer observation registry
     * @param auditPublisher      audit event publisher
     * @param metrics             pipeline SLI metrics
     */
    public PipelineStageObserver(ObservationRegistry observationRegistry,
                                 PipelineAuditPublisher auditPublisher,
                                 PipelineMetrics metrics) {
        this.observationRegistry = observationRegistry;
        this.auditPublisher = auditPublisher;
        this.metrics = metrics;
    }

    /**
     * Begins an instrumented stage; must be closed exactly once.
     *
     * @param stage         stage name (PARSE, INSTANTIATE, LIFT, INFER, MATCH, MANIFEST, ...)
     * @param runId         run correlation id
     * @param sessionId     session id, may be {@code null}
     * @param requirementId requirement id, may be {@code null}
     * @return the closeable stage handle
     */
    public Stage begin(String stage, UUID runId, UUID sessionId, String requirementId) {
        return new Stage(stage,
            runId == null ? null : runId.toString(),
            sessionId == null ? null : sessionId.toString(),
            requirementId);
    }

    /**
     * Handle for one executing stage.
     */
    public final class Stage implements AutoCloseable {

        private final String stage;
        private final String runId;
        private final String sessionId;
        private final String requirementId;
        private final long startNanos = System.nanoTime();
        private final Observation observation;
        private String detail;
        private Throwable error;

        private Stage(String stage, String runId, String sessionId, String requirementId) {
            this.stage = stage;
            this.runId = runId;
            this.sessionId = sessionId;
            this.requirementId = requirementId;
            this.observation = Observation.createNotStarted("sda.pipeline.stage", observationRegistry)
                .contextualName("pipeline " + stage)
                .lowCardinalityKeyValue("stage", stage);
            if (runId != null) {
                this.observation.highCardinalityKeyValue("run.id", runId);
            }
            this.observation.start();
            auditPublisher.started(runId, sessionId, requirementId, stage);
        }

        /**
         * Attaches a success summary recorded with the SUCCEEDED audit event.
         *
         * @param detailText human-readable summary of what the stage produced
         */
        public void detail(String detailText) {
            this.detail = detailText;
            observation.highCardinalityKeyValue("detail", detailText);
        }

        /**
         * Attaches a key-value attribute to this stage's span (e.g.
         * {@code match.dimension.capability = pass}).
         *
         * @param key   attribute name
         * @param value attribute value
         */
        public void attribute(String key, String value) {
            observation.highCardinalityKeyValue(key, value);
        }

        /**
         * Stops the observation, records metrics and writes the terminal audit event.
         * Exceptions set via {@link #error(Throwable)} produce FAILED rows; the error
         * itself must be rethrown by the caller.
         */
        @Override
        public void close() {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            metrics.recordStageDuration(stage, durationMs);
            try {
                observation.stop();
            } catch (RuntimeException e) {
                // never let observation teardown mask pipeline behaviour
            }
            if (error != null) {
                auditPublisher.failed(runId, sessionId, requirementId, stage, durationMs,
                    error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
            } else {
                auditPublisher.succeeded(runId, sessionId, requirementId, stage, durationMs, detail);
            }
        }

        /**
         * Marks the stage as failed; call together with rethrowing the exception.
         *
         * @param t the failure cause
         */
        public void error(Throwable t) {
            this.error = t;
            observation.error(t);
        }
    }
}
