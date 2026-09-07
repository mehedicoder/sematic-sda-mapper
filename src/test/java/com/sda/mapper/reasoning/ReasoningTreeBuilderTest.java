package com.sda.mapper.reasoning;

import com.sda.mapper.reasoning.ReasoningNode.NodeType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ReasoningTreeBuilder} walks the ontological match chain
 * (requirement &rarr; required capability &rarr; offered capability &rarr;
 * skill &rarr; service &rarr; binding / connector / recall / deployment context
 * &rarr; network zone / safety class / semantic-concept coverage) and emits
 * truthful status hops.
 */
@DisplayName("ReasoningTreeBuilder match-chain derivation")
class ReasoningTreeBuilderTest {

    private static final String SDA = "http://sda-platform.org/ontology#";
    private static final String INST = "http://sda-platform.org/instance#";

    @Test
    @DisplayName("Should walk the full chain and end in a PASS match-outcome")
    void matchedTreeWalksFullChainAndEndsInPass() {
        ReasoningTree tree = new ReasoningTreeBuilder().build(
            supplyModel(null, false), "REQ-047", null, matched("run-0"));

        assertEquals(1, tree.roots().size(), "one requirement root");
        assertEquals("REQ-047", tree.roots().get(0).requirementId());
        assertEquals(ReasoningNode.INFO, tree.roots().get(0).status());

        List<ReasoningNode> nodes = tree.nodes();
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.REQUIRED_CAPABILITY));
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.OFFERED_CAPABILITY
            && ReasoningNode.PASS.equals(n.status())));
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.SKILL
            && ReasoningNode.PASS.equals(n.status())));
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.SERVICE
            && "Pump Condition Service".equals(n.label())
            && ReasoningNode.PASS.equals(n.status())));
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.COMMUNICATION_BINDING));
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.CONNECTOR));
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.DEPLOYMENT_CONTEXT));
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.NETWORK_ZONE));
        assertTrue(nodes.stream()
                .anyMatch(n -> n.type() == NodeType.SEMANTIC_CONCEPT && ReasoningNode.PASS.equals(n.status())),
            "required concept must be covered via the connector's mapped variable");
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.MATCH_OUTCOME
            && ReasoningNode.PASS.equals(n.status())));
        assertTrue(nodes.stream().allMatch(n -> "REQ-047".equals(n.requirementId())),
            "every node belongs to the same requirement");
    }

    @Test
    @DisplayName("Should mark declared constraints PASS and observed-only hops INFO")
    void directConstraintHopsArePassObservedHopsAreInfo() {
        ReasoningTree tree = new ReasoningTreeBuilder().build(
            supplyModel(null, false), "REQ-047", null, matched("run-1"));

        List<ReasoningNode> nodes = tree.nodes();
        // minRecall 0.9 is declared; 0.95 reported -> PASS
        assertTrue(nodes.stream().anyMatch(n -> "reportedRecall".equals(n.property())
            && ReasoningNode.PASS.equals(n.status())));
        // no maxLatency declared; latency observed -> INFO
        assertTrue(nodes.stream().anyMatch(n -> "reportedLatency".equals(n.property())
            && ReasoningNode.INFO.equals(n.status())));
        // no networkZone declared on the requirement; observed zone -> INFO
        assertTrue(nodes.stream().anyMatch(n -> "networkZone".equals(n.property())
            && ReasoningNode.INFO.equals(n.status())));
    }

    @Test
    @DisplayName("Should truthfully FAIL a declared constraint the offer violates")
    void violatedDeclaredConstraintProducesFailHop() {
        // Requirement demands MODBUS-TCP but the binding only offers OPC-UA.
        ReasoningTree tree = new ReasoningTreeBuilder().build(
            supplyModel("MODBUS-TCP", false), "REQ-047", null, noMatch());

        List<ReasoningNode> nodes = tree.nodes();
        ReasoningNode protocol = nodes.stream()
            .filter(n -> "protocol".equals(n.property()) && n.type() == NodeType.CONSTRAINT)
            .findFirst()
            .orElseThrow();
        assertEquals(ReasoningNode.FAIL, protocol.status());
        assertTrue(nodes.stream().anyMatch(n -> n.type() == NodeType.MATCH_OUTCOME
            && ReasoningNode.FAIL.equals(n.status())),
            "no-match outcome is FAIL");
    }

    @Test
    @DisplayName("Should SKIP alternative candidate offers once the matched offer is selected")
    void alternativeOffersAreSkipped() {
        Model model = supplyModel(null, true);
        ReasoningTree tree = new ReasoningTreeBuilder().build(
            model, "REQ-047", null, matched("run-3"));

        List<ReasoningNode> skipped = tree.nodes().stream()
            .filter(n -> ReasoningNode.SKIPPED.equals(n.status()))
            .toList();
        assertEquals(1, skipped.size(), "exactly one alternative offer is skipped");
        assertEquals(NodeType.OFFERED_CAPABILITY, skipped.get(0).type());
        assertEquals("Alternative offer", skipped.get(0).label());

        assertTrue(tree.nodes().stream().anyMatch(n -> n.type() == NodeType.MATCH_OUTCOME
                && ReasoningNode.PASS.equals(n.status())),
            "the selected offer still derives a PASS outcome");
    }

    @Test
    @DisplayName("Should stream every emitted node through the live sink in emission order")
    void liveSinkReceivesAllNodesInOrder() {
        Model model = supplyModel(null, false);
        List<ReasoningNode> received = new ArrayList<>();

        ReasoningTree tree = new ReasoningTreeBuilder().build(
            model, "REQ-047", null, matched("run-9"), "run-9", received::add);

        assertEquals(tree.nodes().size(), received.size(), "sink sees every emitted node");
        assertEquals(tree.nodes(), received, "sink order equals tree emission order");
        assertTrue(received.stream().allMatch(n -> "run-9".equals(n.runId())),
            "sink nodes carry the traced run id");
    }

    // ════════════════════════════════════════════════════════════════════
    // Fixtures
    // ════════════════════════════════════════════════════════════════════

    private static ServiceMatchResult matched(String run) {
        return new ServiceMatchResult(
            true, "Pump Condition Service", null,
            "Flow monitoring offer", "NE107 diagnostics skill",
            "OPC-UA", "read-only", "OPC-UA binding", "OPC-UA",
            "DeploymentContext",
            0.95, 100, "matched", "Matched", "REQ-047",
            "SIL-2", "SoftRealTime", "OT-Zone-1", "opc.tcp://localhost:4840/sda");
    }

    private static ServiceMatchResult noMatch() {
        return ServiceMatchResult.noMatch("No service matched requirement REQ-047");
    }

    private static Model supplyModel(String requirementProtocol, boolean withAlternativeOffer) {
        Model m = ModelFactory.createDefaultModel();
        m.setNsPrefix("sda", SDA);
        m.setNsPrefix("", INST);

        Resource req = m.createResource(INST + "REQ-047");
        req.addProperty(RDF.type, m.createResource(SDA + "UseCaseRequirement"));
        req.addProperty(RDFS.label, "Pump health monitoring");
        req.addProperty(m.createProperty(SDA + "requirementID"), "REQ-047");
        if (requirementProtocol != null) {
            req.addProperty(m.createProperty(SDA + "protocol"), requirementProtocol);
        }
        req.addProperty(m.createProperty(SDA + "minRecall"), "0.9");

        Resource reqCap = m.createResource(INST + "REQ-047-cap");
        reqCap.addProperty(RDF.type, m.createResource(SDA + "RequiredCapability"));
        reqCap.addProperty(m.createProperty(SDA + "capabilityType"), "FlowMonitoring");
        req.addProperty(m.createProperty(SDA + "mapsToCapability"), reqCap);

        Resource offer = m.createResource(INST + "OFFER-1");
        offer.addProperty(RDF.type, m.createResource(SDA + "OfferedCapability"));
        offer.addProperty(RDFS.label, "Flow monitoring offer");
        offer.addProperty(m.createProperty(SDA + "capabilityType"), "FlowMonitoring");

        Resource skill = m.createResource(INST + "SKILL-1");
        skill.addProperty(RDF.type, m.createResource(SDA + "Skill"));
        skill.addProperty(RDFS.label, "NE107 diagnostics skill");
        offer.addProperty(m.createProperty(SDA + "realisedBy"), skill);

        Resource service = m.createResource(INST + "SVC-1");
        service.addProperty(RDF.type, m.createResource(SDA + "Service"));
        service.addProperty(RDFS.label, "Pump Condition Service");
        service.addProperty(m.createProperty(SDA + "reportedRecall"), "0.95");
        service.addProperty(m.createProperty(SDA + "reportedLatency"), "100");
        skill.addProperty(m.createProperty(SDA + "isImplementedBy"), service);

        Resource binding = m.createResource(INST + "BIND-1");
        binding.addProperty(RDF.type, m.createResource(SDA + "CommunicationBinding"));
        binding.addProperty(RDFS.label, "OPC-UA binding");
        binding.addProperty(m.createProperty(SDA + "protocol"), "OPC-UA");
        binding.addProperty(m.createProperty(SDA + "accessMode"), "read-only");
        service.addProperty(m.createProperty(SDA + "hasBinding"), binding);

        Resource connector = m.createResource(INST + "CONN-1");
        connector.addProperty(RDF.type, m.createResource(SDA + "DataConnector"));
        connector.addProperty(m.createProperty(SDA + "connectorType"), "OPC-UA");
        service.addProperty(m.createProperty(SDA + "hasConnector"), connector);

        Resource concept = m.createResource(INST + "CONCEPT-1");
        concept.addProperty(RDF.type, m.createResource(SDA + "SemanticConcept"));
        concept.addProperty(RDFS.label, "Flow rate concept");
        concept.addProperty(m.createProperty(SDA + "conceptID"), "cdd:FlowRate");
        skill.addProperty(m.createProperty(SDA + "requiresConcept"), concept);

        Resource variable = m.createResource(INST + "VAR-1");
        variable.addProperty(m.createProperty(SDA + "describedBy"), concept);
        connector.addProperty(m.createProperty(SDA + "mapsVariable"), variable);

        Resource deploy = m.createResource(INST + "DEPLOY-1");
        deploy.addProperty(RDF.type, m.createResource(SDA + "DeploymentContext"));
        deploy.addProperty(m.createProperty(SDA + "onPlantNetwork"), "true");
        service.addProperty(m.createProperty(SDA + "isDeployedBy"), deploy);

        Resource safety = m.createResource(INST + "SAFETY-1");
        safety.addProperty(RDF.type, m.createResource(SDA + "SafetyClass"));
        safety.addProperty(RDFS.label, "SIL-2");
        safety.addProperty(m.createProperty(SDA + "safetyIntegrityLevel"), "SIL-2");
        deploy.addProperty(m.createProperty(SDA + "contextConstrainedBy"), safety);

        Resource zone = m.createResource(INST + "ZONE-1");
        zone.addProperty(RDF.type, m.createResource(SDA + "NetworkZone"));
        zone.addProperty(m.createProperty(SDA + "zoneName"), "OT-Zone-1");
        deploy.addProperty(m.createProperty(SDA + "locatedInZone"), zone);

        if (withAlternativeOffer) {
            Resource offer2 = m.createResource(INST + "OFFER-2");
            offer2.addProperty(RDF.type, m.createResource(SDA + "OfferedCapability"));
            offer2.addProperty(RDFS.label, "Alternative offer");
            offer2.addProperty(m.createProperty(SDA + "capabilityType"), "FlowMonitoring");
            Resource skill2 = m.createResource(INST + "SKILL-2");
            skill2.addProperty(RDF.type, m.createResource(SDA + "Skill"));
            offer2.addProperty(m.createProperty(SDA + "realisedBy"), skill2);
            Resource service2 = m.createResource(INST + "SVC-2");
            service2.addProperty(RDF.type, m.createResource(SDA + "Service"));
            service2.addProperty(RDFS.label, "Other Service");
            skill2.addProperty(m.createProperty(SDA + "isImplementedBy"), service2);
        }
        return m;
    }
}