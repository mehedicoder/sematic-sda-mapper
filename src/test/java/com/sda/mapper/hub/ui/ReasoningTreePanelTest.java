package com.sda.mapper.hub.ui;

import com.sda.mapper.observability.ReasoningTracePublisher;
import com.sda.mapper.reasoning.ReasoningNode;
import com.sda.mapper.reasoning.ReasoningNode.NodeType;
import com.sda.mapper.reasoning.ReasoningTree;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Reasoning tree panel renders match chains and streams live nodes")
class ReasoningTreePanelTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new UI();
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    private static ReasoningNode node(String id, String parentId, int depth, String status, String runId) {
        return new ReasoningNode(runId, "REQ-1", id, NodeType.REQUIREMENT,
            "label-" + id, null, null, null, status, depth, parentId, null, null);
    }

    @Test
    @DisplayName("Should render the trees of the last pipeline run and count their nodes")
    void shouldRenderTreesFromPipelineResult() {
        ReasoningTreePanel panel = new ReasoningTreePanel(new ReasoningTracePublisher());

        ReasoningNode root = node("req", null, 0, ReasoningNode.INFO, "run-1");
        ReasoningNode cap = node("req-cap", "req", 1, ReasoningNode.INFO, "run-1");
        ReasoningNode service = new ReasoningNode("run-1", "REQ-1", "service",
            NodeType.SERVICE, "Pump Condition Service", null, "isImplementedBy", null,
            ReasoningNode.PASS, 2, "req-cap", null, null);
        panel.show(List.of(new ReasoningTree(List.of(root, cap, service))));

        assertEquals(3, panel.nodeCount(), "panel renders every tree node");
        assertEquals(List.of(root, cap, service), panel.getNodes());
        assertTrue(panel.getNodes().stream()
                .anyMatch(n -> n.type() == NodeType.MATCH_OUTCOME || n.type() == NodeType.SERVICE),
            "panel carries typed chain hops");
    }

    @Test
    @DisplayName("Should clear rendered nodes and stop live streaming")
    void shouldClear() {
        ReasoningTracePublisher publisher = new ReasoningTracePublisher();
        ReasoningTreePanel panel = new ReasoningTreePanel(publisher);
        ui.add(panel);

        panel.show(List.of(new ReasoningTree(List.of(
            node("req", null, 0, ReasoningNode.INFO, "run-1")))));
        assertEquals(1, panel.nodeCount());

        panel.clear();
        assertEquals(0, panel.nodeCount());

        publisher.nodeAdded(node("req-cap", "req", 1, ReasoningNode.INFO, "run-1"));
        assertEquals(0, panel.nodeCount(), "cleared panel ignores live nodes");
    }

    @Test
    @DisplayName("Should append live nodes streamed for the active run only")
    void shouldAppendLiveNodesForActiveRun() {
        ReasoningTracePublisher publisher = new ReasoningTracePublisher();
        ReasoningTreePanel panel = new ReasoningTreePanel(publisher);
        ui.add(panel);

        panel.show(List.of(new ReasoningTree(List.of(
            node("req", null, 0, ReasoningNode.INFO, "run-1")))));

        publisher.nodeAdded(node("req-cap", "req", 1, ReasoningNode.INFO, "run-1"));
        assertEquals(2, panel.nodeCount(), "live node from the active run is appended");

        publisher.nodeAdded(node("stale", "req", 1, ReasoningNode.INFO, "run-2"));
        assertEquals(2, panel.nodeCount(), "nodes from another run are ignored");

        panel.getNodes().stream()
            .filter(n -> n.id().equals("req-cap"))
            .findFirst()
            .ifPresent(n -> assertEquals("run-1", n.runId()));
    }

    @Test
    @DisplayName("Should stop receiving live nodes once detached from a UI")
    void shouldStopReceivingAfterDetach() {
        ReasoningTracePublisher publisher = new ReasoningTracePublisher();
        ReasoningTreePanel panel = new ReasoningTreePanel(publisher);
        ui.add(panel);
        panel.show(List.of(new ReasoningTree(List.of(
            node("req", null, 0, ReasoningNode.INFO, "run-1")))));

        ui.remove(panel);

        publisher.nodeAdded(node("req-cap", "req", 1, ReasoningNode.INFO, "run-1"));
        assertEquals(1, panel.nodeCount(), "detached panel must not receive live nodes");
    }
}