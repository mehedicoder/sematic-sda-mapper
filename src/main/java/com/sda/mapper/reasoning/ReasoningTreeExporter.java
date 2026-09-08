package com.sda.mapper.reasoning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;

/**
 * Serializes {@link ReasoningTree}s to JSON so the derived match chain can be
 * exported and replayed / audited outside the live UI.
 *
 * <p>The JSON mirrors the on-screen tree: every node (in emission order) with
 * its type, status, property, value, depth, parent link and evidence detail,
 * plus a flat summary of the outcome per requirement.</p>
 */
public final class ReasoningTreeExporter {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Serializes the given reasoning trees to a pretty-printed JSON document.
     *
     * @param trees the reasoning trees to export
     * @return the JSON string
     */
    public String export(List<ReasoningTree> trees) {
        ObjectNode root = mapper.createObjectNode();
        root.put("exportVersion", "1.0");
        root.put("generatedAt", Instant.now().toString());
        root.put("treeCount", trees == null ? 0 : trees.size());

        ArrayNode treesNode = root.putArray("trees");
        if (trees != null) {
            for (ReasoningTree tree : trees) {
                treesNode.add(treeToJson(tree));
            }
        }
        return write(root);
    }

    private ObjectNode treeToJson(ReasoningTree tree) {
        ObjectNode out = mapper.createObjectNode();
        out.put("nodeCount", tree.size());

        ArrayNode nodesArray = out.putArray("nodes");
        for (ReasoningNode node : tree.nodes()) {
            nodesArray.add(nodeToJson(node));
        }

        if (!tree.isEmpty()) {
            ReasoningNode root = tree.roots().isEmpty() ? null : tree.roots().get(0);
            ReasoningNode outcome = outcomeNode(tree);
            if (root != null) {
                out.put("requirementId", root.requirementId() == null ? "" : root.requirementId());
                out.put("runId", root.runId() == null ? "" : root.runId());
            }
            if (outcome != null) {
                out.put("outcome", outcome.status());
                if (outcome.detail() != null) {
                    out.put("outcomeDetail", outcome.detail());
                }
            }
        }
        return out;
    }

    private ObjectNode nodeToJson(ReasoningNode node) {
        ObjectNode out = mapper.createObjectNode();
        out.put("id", node.id());
        out.put("type", node.type().name());
        putIfPresent(out, "label", node.label());
        putIfPresent(out, "iri", node.iri());
        putIfPresent(out, "property", node.property());
        putIfPresent(out, "value", node.value());
        out.put("status", node.status());
        out.put("depth", node.depth());
        putIfPresent(out, "parentId", node.parentId());
        putIfPresent(out, "detail", node.detail());
        if (node.durationMs() != null) {
            out.put("durationMs", node.durationMs());
        }
        return out;
    }

    private ReasoningNode outcomeNode(ReasoningTree tree) {
        for (ReasoningNode node : tree.nodes()) {
            if (node.type() == ReasoningNode.NodeType.MATCH_OUTCOME) {
                return node;
            }
        }
        return null;
    }

    private void putIfPresent(ObjectNode node, String field, String value) {
        if (value != null) {
            node.put(field, value);
        }
    }

    private String write(ObjectNode root) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize reasoning tree", e);
        }
    }
}
