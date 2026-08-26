package com.sda.mapper.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Records pipeline stage transitions into the audit log and broadcasts them to
 * in-process listeners (used by the live UI console).
 *
 * <p>Audit write failures are logged at ERROR and swallowed: observability must
 * never abort a running pipeline.</p>
 */
@Component
public class PipelineAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(PipelineAuditPublisher.class);

    private final PipelineAuditRepository repository;
    private final List<Consumer<PipelineEvent>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates the publisher.
     *
     * @param repository audit row persistence
     */
    public PipelineAuditPublisher(PipelineAuditRepository repository) {
        this.repository = repository;
    }

    /**
     * Registers a live listener for stage transitions.
     *
     * @param listener consumer notified for every recorded event
     * @return this publisher, for fluent registration
     */
    public PipelineAuditPublisher addListener(Consumer<PipelineEvent> listener) {
        listeners.add(listener);
        return this;
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(Consumer<PipelineEvent> listener) {
        listeners.remove(listener);
    }

    /**
     * Records that a stage has started.
     *
     * @param runId         run correlation id
     * @param sessionId     session id, may be {@code null}
     * @param requirementId requirement id, may be {@code null}
     * @param stage         stage name
     */
    public void started(String runId, String sessionId, String requirementId, String stage) {
        record(new PipelineEvent(null, runId, sessionId, requirementId, stage,
            PipelineEvent.STARTED, Instant.now(), null, null));
    }

    /**
     * Records that a stage finished successfully.
     *
     * @param runId         run correlation id
     * @param sessionId     session id, may be {@code null}
     * @param requirementId requirement id, may be {@code null}
     * @param stage         stage name
     * @param durationMs    wall-clock duration of the stage
     * @param detail        human-readable summary
     */
    public void succeeded(String runId, String sessionId, String requirementId, String stage,
                          long durationMs, String detail) {
        record(new PipelineEvent(null, runId, sessionId, requirementId, stage,
            PipelineEvent.SUCCEEDED, Instant.now(), durationMs, detail));
    }

    /**
     * Records that a stage failed.
     *
     * @param runId         run correlation id
     * @param sessionId     session id, may be {@code null}
     * @param requirementId requirement id, may be {@code null}
     * @param stage         stage name
     * @param durationMs    wall-clock duration until failure
     * @param error         failure reason
     */
    public void failed(String runId, String sessionId, String requirementId, String stage,
                       long durationMs, String error) {
        record(new PipelineEvent(null, runId, sessionId, requirementId, stage,
            PipelineEvent.FAILED, Instant.now(), durationMs, error));
    }

    private void record(PipelineEvent event) {
        if (repository != null) {
            try {
                repository.insert(event);
            } catch (RuntimeException e) {
                log.error("Failed to persist pipeline audit event (stage={}, status={})",
                    event.stage(), event.status(), e);
            }
        }
        broadcast(event);
    }

    private void broadcast(PipelineEvent event) {
        for (Consumer<PipelineEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException e) {
                log.debug("Pipeline audit listener failed for stage {}", event.stage(), e);
            }
        }
    }
}
