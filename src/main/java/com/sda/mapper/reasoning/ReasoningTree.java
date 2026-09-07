package com.sda.mapper.reasoning;

import java.util.List;
import java.util.Objects;

/**
 * An immutable collection of {@link ReasoningNode}s describing how one
 * requirement was (or was not) matched through the ontology.
 *
 * <p>Nodes are stored in depth-first emission order; parent-child links are
 * expressed through {@link ReasoningNode#parentId()}.</p>
 */
public final class ReasoningTree {

    private final List<ReasoningNode> nodes;

    /**
     * Creates a reasoning tree from the given nodes.
     *
     * @param nodes the tree nodes in emission (depth-first) order
     */
    public ReasoningTree(List<ReasoningNode> nodes) {
        this.nodes = List.copyOf(nodes);
    }

    /**
     * Returns all nodes in emission order.
     *
     * @return immutable node list
     */
    public List<ReasoningNode> nodes() {
        return nodes;
    }

    /**
     * Returns the root nodes (one per resolved requirement).
     *
     * @return the root nodes
     */
    public List<ReasoningNode> roots() {
        return nodes.stream().filter(n -> n.parentId() == null).toList();
    }

    /**
     * Returns the direct children of the given parent node.
     *
     * @param parentId the parent node id
     * @return the child nodes in emission order
     */
    public List<ReasoningNode> childrenOf(String parentId) {
        return nodes.stream().filter(n -> Objects.equals(parentId, n.parentId())).toList();
    }

    /**
     * Returns the size of this tree.
     *
     * @return the number of nodes
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Whether this tree contains no nodes.
     *
     * @return {@code true} when empty
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}