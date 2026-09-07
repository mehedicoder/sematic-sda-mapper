package com.sda.mapper.hub.ui;

import com.sda.mapper.observability.ReasoningTracePublisher;
import com.sda.mapper.reasoning.ReasoningNode;
import com.sda.mapper.reasoning.ReasoningTree;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.sda.mapper.reasoning.ReasoningNode.FAIL;
import static com.sda.mapper.reasoning.ReasoningNode.INFO;
import static com.sda.mapper.reasoning.ReasoningNode.PASS;
import static com.sda.mapper.reasoning.ReasoningNode.SKIPPED;

/**
 * Live ontology reasoning tree for the Mapping Hub.
 *
 * <p>Renders the MATCH-stage chain as a collapsible, colour-coded tree
 * (requirement &rarr; required capability &rarr; offered capability &rarr;
 * skill &rarr; service &rarr; binding / connector / deployment context / network
 * zone / safety class / semantic-concept coverage). Each hop shows its status
 * badge (PASS / FAIL / INFO on observed-but-unconstrained hops / SKIPPED on
 * rejected candidate offers).</p>
 *
 * <p>The panel subscribes to {@link ReasoningTracePublisher} while attached so
 * nodes stream in as the MATCH stage emits them; {@link #show(List)} replaces
 * the view with the authoritative tree from the last pipeline result so the
 * panel always reflects completed runs exactly.</p>
 */
public class ReasoningTreePanel extends VerticalLayout {

    /** Safety cap on nodes rendered in the tree. */
    static final int MAX_NODES = 2000;

    private final ReasoningTracePublisher publisher;
    private final Div treeBox;
    private final List<ReasoningNode> nodes = new ArrayList<>();
    private String activeRunId;

    private final Consumer<ReasoningNode> listener = this::onNode;

    /**
     * Creates the reasoning tree panel subscribeable to the given publisher.
     *
     * @param publisher the reasoning trace publisher (hub live-stream source)
     */
    public ReasoningTreePanel(ReasoningTracePublisher publisher) {
        this.publisher = publisher;
        addClassName("reasoning-tree-panel");
        setPadding(false);
        setSpacing(true);

        treeBox = new Div();
        treeBox.addClassName("reasoning-tree-box");
        treeBox.setWidthFull();

        Scroller scroller = new Scroller(treeBox);
        scroller.setWidthFull();
        scroller.setHeight("260px");
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        add(scroller);

        if (publisher != null) {
            publisher.addListener(listener);
        }
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        if (publisher != null) {
            publisher.removeListener(listener);
        }
        super.onDetach(detachEvent);
    }

    /**
     * Replaces the panel content with the reasoning trees of the last pipeline
     * run and pins live streaming to that run.
     *
     * @param trees the reasoning trees to render
     */
    public void show(List<ReasoningTree> trees) {
        nodes.clear();
        activeRunId = null;
        if (trees != null) {
            for (ReasoningTree tree : trees) {
                nodes.addAll(tree.nodes());
                if (activeRunId == null && !tree.roots().isEmpty()) {
                    activeRunId = tree.roots().get(0).runId();
                }
            }
        }
        render();
    }

    /**
     * Clears the panel and stops live streaming.
     */
    public void clear() {
        nodes.clear();
        activeRunId = null;
        render();
    }

    /**
     * Returns the number of currently rendered reasoning nodes.
     *
     * @return the node count
     */
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * Returns a copy of the currently rendered reasoning nodes.
     *
     * @return the nodes in emission order
     */
    public List<ReasoningNode> getNodes() {
        return List.copyOf(nodes);
    }

    private void onNode(ReasoningNode node) {
        if (activeRunId == null || node == null || !activeRunId.equals(node.runId())) {
            return;
        }
        getUI().ifPresent(ui -> {
            if (UI.getCurrent() == ui) {
                appendNode(node);
            } else {
                ui.access(() -> appendNode(node));
            }
        });
    }

    private void appendNode(ReasoningNode node) {
        if (nodes.size() >= MAX_NODES) {
            return;
        }
        nodes.add(node);
        render();
    }

    private void render() {
        treeBox.removeAll();
        if (nodes.isEmpty()) {
            Span empty = new Span("Run the pipeline to watch the ontology match chain derive here.");
            empty.addClassName("reasoning-tree-empty");
            treeBox.add(empty);
            return;
        }
        Map<String, List<ReasoningNode>> children = new LinkedHashMap<>();
        for (ReasoningNode node : nodes) {
            if (node.parentId() != null) {
                children.computeIfAbsent(node.parentId(), k -> new ArrayList<>()).add(node);
            }
        }
        for (ReasoningNode root : nodes) {
            if (root.parentId() == null) {
                renderBranch(children, root);
            }
        }
    }

    private void renderBranch(Map<String, List<ReasoningNode>> children, ReasoningNode node) {
        Div row = new Div();
        row.addClassName("reasoning-node");
        row.getStyle().set("margin-left", (node.depth() * 16) + "px");

        Div badge = new Div();
        badge.addClassName("reasoning-node-badge");
        badge.addClassName("reasoning-node-badge--" + statusClass(node.status()));
        badge.setText(node.status());

        Span label = new Span(nodeText(node));
        label.addClassName("reasoning-node-label");

        row.add(badge, label);
        treeBox.add(row);

        if (node.detail() != null && !node.detail().isBlank()) {
            Div detail = new Div();
            detail.addClassName("reasoning-node-detail");
            detail.getStyle().set("margin-left", (node.depth() * 16 + 20) + "px");
            detail.setText(node.detail());
            treeBox.add(detail);
        }

        for (ReasoningNode child : children.getOrDefault(node.id(), List.of())) {
            renderBranch(children, child);
        }
    }

    private static String nodeText(ReasoningNode node) {
        StringBuilder sb = new StringBuilder();
        switch (node.type()) {
            case CONSTRAINT -> sb.append("constraint");
            case MATCH_OUTCOME -> sb.append("outcome");
            default -> {
            }
        }
        if (sb.length() > 0) {
            sb.append(" · ");
        }
        if (node.property() != null && !node.property().isBlank()) {
            sb.append(node.property()).append(" → ");
        }
        if (node.label() != null && !node.label().isBlank()) {
            sb.append(node.label());
        }
        if (node.value() != null && !node.value().isBlank()) {
            sb.append(": ").append(node.value());
        }
        return sb.toString();
    }

    private static String statusClass(String status) {
        if (status == null) {
            return "info";
        }
        return switch (status) {
            case PASS -> "ok";
            case FAIL -> "error";
            case SKIPPED -> "skipped";
            case INFO -> "info";
            default -> "info";
        };
    }
}