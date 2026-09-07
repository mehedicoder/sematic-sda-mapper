package com.sda.mapper.observability;

import com.sda.mapper.reasoning.ReasoningNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Broadcasts reasoning-tree node additions to in-process listeners (used by
 * the live Mapping Hub reasoning tree).
 *
 * <p>Mirrors {@link PipelineAuditPublisher}: notification failures are logged
 * at DEBUG and swallowed — observability must never change pipeline behaviour.</p>
 */
@Component
public class ReasoningTracePublisher {

    private static final Logger log = LoggerFactory.getLogger(ReasoningTracePublisher.class);

    private final List<Consumer<ReasoningNode>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registers a live listener for reasoning node additions.
     *
     * @param listener consumer notified for every emitted node
     * @return this publisher, for fluent registration
     */
    public ReasoningTracePublisher addListener(Consumer<ReasoningNode> listener) {
        listeners.add(listener);
        return this;
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(Consumer<ReasoningNode> listener) {
        listeners.remove(listener);
    }

    /**
     * Broadcasts one reasoning node to every registered listener.
     *
     * @param node the emitted reasoning node
     */
    public void nodeAdded(ReasoningNode node) {
        for (Consumer<ReasoningNode> listener : listeners) {
            try {
                listener.accept(node);
            } catch (RuntimeException e) {
                log.debug("Reasoning trace listener failed for node {}", node.id(), e);
            }
        }
    }
}