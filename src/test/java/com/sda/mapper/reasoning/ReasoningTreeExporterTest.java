package com.sda.mapper.reasoning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sda.mapper.reasoning.ReasoningNode.NodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Reasoning tree exporter")
class ReasoningTreeExporterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ReasoningTreeExporter exporter = new ReasoningTreeExporter();

    private static ReasoningNode node(String id, String parentId, int depth, String status,
                                      NodeType type, String runId, String requirementId) {
        return new ReasoningNode(runId, requirementId, id, type, "label-" + id,
            null, null, null, status, depth, parentId, null, null);
    }

    private static ReasoningNode outcome(String runId, String requirementId, String status, String detail) {
        return new ReasoningNode(runId, requirementId, "outcome", NodeType.MATCH_OUTCOME,
            "Match derived", null, null, null, status, 1, "req", detail, null);
    }

    @Test
    @DisplayName("Should export a valid JSON document with metadata and tree count")
    void shouldExportJsonMetadata() throws Exception {
        ReasoningNode root = node("req", null, 0, ReasoningNode.INFO, NodeType.REQUIREMENT,
            "run-1", "REQ-1");
        ReasoningNode cap = node("req-cap", "req", 1, ReasoningNode.PASS, NodeType.SERVICE,
            "run-1", "REQ-1");
        ReasoningTree tree = new ReasoningTree(List.of(root, cap,
            outcome("run-1", "REQ-1", ReasoningNode.PASS, "requirement matched")));

        JsonNode rootNode = mapper.readTree(exporter.export(List.of(tree)));

        assertEquals("1.0", rootNode.path("exportVersion").asText());
        assertTrue(rootNode.path("generatedAt").asText().length() > 0);
        assertEquals(1, rootNode.path("treeCount").asInt());
        JsonNode treeNode = rootNode.path("trees").get(0);
        assertEquals(3, treeNode.path("nodeCount").asInt());
        assertEquals("REQ-1", treeNode.path("requirementId").asText());
        assertEquals("run-1", treeNode.path("runId").asText());
        assertEquals(ReasoningNode.PASS, treeNode.path("outcome").asText());
        assertEquals("requirement matched", treeNode.path("outcomeDetail").asText());
    }

    @Test
    @DisplayName("Should serialize every node with type, status, depth and parent link")
    void shouldSerializeAllNodes() throws Exception {
        ReasoningNode root = node("req", null, 0, ReasoningNode.INFO, NodeType.REQUIREMENT,
            "run-1", "REQ-1");
        ReasoningNode service = new ReasoningNode("run-1", "REQ-1", "service",
            NodeType.SERVICE, "Pump Condition Service", "http://example/service-1",
            "isImplementedBy", "value-1", ReasoningNode.PASS, 2, "req-cap", "evidence", 15L);

        JsonNode rootNode = mapper.readTree(exporter.export(List.of(
            new ReasoningTree(List.of(root, service)))));

        JsonNode nodes = rootNode.path("trees").get(0).path("nodes");
        assertEquals(2, nodes.size());

        JsonNode svc = nodes.get(1);
        assertEquals("service", svc.path("id").asText());
        assertEquals("SERVICE", svc.path("type").asText());
        assertEquals("Pump Condition Service", svc.path("label").asText());
        assertEquals("http://example/service-1", svc.path("iri").asText());
        assertEquals("isImplementedBy", svc.path("property").asText());
        assertEquals("value-1", svc.path("value").asText());
        assertEquals(ReasoningNode.PASS, svc.path("status").asText());
        assertEquals(2, svc.path("depth").asInt());
        assertEquals("req-cap", svc.path("parentId").asText());
        assertEquals("evidence", svc.path("detail").asText());
        assertEquals(15L, svc.path("durationMs").asLong());
    }

    @Test
    @DisplayName("Should handle empty and null tree collections")
    void shouldHandleEmptyTrees() throws Exception {
        JsonNode rootNode = mapper.readTree(exporter.export(List.of()));
        assertEquals(0, rootNode.path("treeCount").asInt());
        assertTrue(rootNode.path("trees").isArray());
        assertEquals(0, rootNode.path("trees").size());

        JsonNode nullNode = mapper.readTree(exporter.export(null));
        assertEquals(0, nullNode.path("treeCount").asInt());
    }

    @Test
    @DisplayName("Should omit outcome fields when no MATCH_OUTCOME node exists")
    void shouldOmitOutcomeWhenAbsent() throws Exception {
        ReasoningNode root = node("req", null, 0, ReasoningNode.INFO, NodeType.REQUIREMENT,
            "run-1", "REQ-1");

        JsonNode rootNode = mapper.readTree(exporter.export(List.of(
            new ReasoningTree(List.of(root)))));

        JsonNode treeNode = rootNode.path("trees").get(0);
        assertTrue(treeNode.path("outcome").isMissingNode());
        assertEquals(1, treeNode.path("nodeCount").asInt());
    }
}
