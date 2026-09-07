package com.sda.mapper.observability;

import com.sda.mapper.reasoning.ReasoningNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Reasoning trace publisher broadcasts nodes without breaking the pipeline")
class ReasoningTracePublisherTest {

    private static ReasoningNode node(String id) {
        return new ReasoningNode("run-1", "REQ-001", id,
            ReasoningNode.NodeType.REQUIREMENT, "label-" + id, null, null, null,
            ReasoningNode.INFO, 0, null, null, null);
    }

    @Test
    @DisplayName("Should deliver every published node to registered listeners")
    void shouldBroadcastNodesToListeners() {
        ReasoningTracePublisher publisher = new ReasoningTracePublisher();
        List<ReasoningNode> received = new ArrayList<>();
        publisher.addListener(received::add);

        publisher.nodeAdded(node("req"));
        publisher.nodeAdded(node("req-cap"));

        assertThat(received).hasSize(2);
        assertThat(received).extracting(ReasoningNode::id)
            .containsExactly("req", "req-cap");
    }

    @Test
    @DisplayName("Should stop delivering after a listener is removed")
    void shouldStopDeliveringAfterRemoval() {
        ReasoningTracePublisher publisher = new ReasoningTracePublisher();
        List<ReasoningNode> received = new ArrayList<>();
        Consumer<ReasoningNode> listener = received::add;
        publisher.addListener(listener);

        publisher.nodeAdded(node("req"));
        publisher.removeListener(listener);
        publisher.nodeAdded(node("req-cap"));

        assertThat(received).hasSize(1);
    }

    @Test
    @DisplayName("Should swallow listener failures so observability never breaks the pipeline")
    void shouldSwallowListenerFailures() {
        ReasoningTracePublisher publisher = new ReasoningTracePublisher();
        AtomicInteger deliveries = new AtomicInteger();
        publisher.addListener(n -> {
            throw new IllegalStateException("listener exploded");
        });
        publisher.addListener(n -> deliveries.incrementAndGet());

        assertThatCode(() -> publisher.nodeAdded(node("req"))).doesNotThrowAnyException();

        assertThat(deliveries.get()).isEqualTo(1);
    }
}