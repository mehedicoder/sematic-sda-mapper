package com.sda.mapper.reasoning;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Derives the ontology reasoning tree for one requirement match.
 *
 * <p>Walks the shared catalogue model along exactly the chain that
 * {@code queries/match-generic.rq} traverses &mdash; requirement &rarr;
 * {@code mapsToCapability} &rarr; required capability ({@code capabilityType})
 * &rarr; offered capability &rarr; {@code realisedBy} skill &rarr;
 * {@code isImplementedBy} service &rarr; binding / connector / recall / latency
 * / deployment context (on-plant network, SIL, determinism, network zone) and
 * the semantic-concept coverage check. Every hop is emitted as a
 * {@link ReasoningNode} with a PASS / FAIL / INFO status; the optional live
 * sink streams each node as it is created so the Mapping Hub UI can render the
 * tree while the MATCH stage runs.</p>
 *
 * <p>Status semantics mirror the SPARQL filters: a chain hop is only PASS/FAIL
 * when the requirement actually declares the constraint the query reads directly
 * ({@code sda:protocol}, {@code sda:accessMode}, {@code sda:minRecall},
 * {@code sda:maxLatency}, {@code sda:onPlantNetwork},
 * {@code sda:safetyIntegrityLevel}, {@code sda:determinismClass},
 * {@code sda:networkZone}); otherwise it is INFO (observed, not constrained).
 * Requirement-declared {@code hasConstraint} / {@code hasParameter} intents are
 * rendered as informational nodes without claiming pass/fail, keeping the tree
 * truthful about what the reasoner actually evaluated.</p>
 */
@org.springframework.stereotype.Component
public class ReasoningTreeBuilder {

    /** Shared instance namespace of the seed catalogue. */
    public static final String INST_NS = "http://sda-platform.org/instance#";
    /** Shared SDA ontology namespace. */
    public static final String SDA_NS = "http://sda-platform.org/ontology#";

    /**
     * Builds the reasoning tree for one requirement match.
     *
     * @param catalogModel   the shared catalogue model (offerings + seeded requirement individuals)
     * @param requirementId  the requirement identifier (e.g. {@code REQ-001})
     * @param capabilityType the required capability type, or {@code null} to resolve from the model
     * @param match          the reasoner match result for this requirement
     * @param liveSink       optional per-node sink used for live UI streaming
     * @return the derived reasoning tree
     */
    public ReasoningTree build(Model catalogModel, String requirementId, String capabilityType,
                               ServiceMatchResult match, Consumer<ReasoningNode> liveSink) {
        return build(catalogModel, requirementId, capabilityType, match, null, liveSink);
    }

    /**
     * Builds the reasoning tree for one requirement match inside a traced run.
     *
     * @param catalogModel   the shared catalogue model (offerings + seeded requirement individuals)
     * @param requirementId  the requirement identifier (e.g. {@code REQ-001})
     * @param capabilityType the required capability type, or {@code null} to resolve from the model
     * @param match          the reasoner match result for this requirement
     * @param runId          the pipeline run id, may be {@code null}
     * @param liveSink       optional per-node sink used for live UI streaming
     * @return the derived reasoning tree
     */
    public ReasoningTree build(Model catalogModel, String requirementId, String capabilityType,
                               ServiceMatchResult match, String runId,
                               Consumer<ReasoningNode> liveSink) {
        return new Ctx(catalogModel, requirementId, runId, liveSink).build(capabilityType, match);
    }

    /**
     * Builds the reasoning tree without live streaming.
     *
     * @param catalogModel   the shared catalogue model
     * @param requirementId  the requirement identifier
     * @param capabilityType the required capability type, or {@code null}
     * @param match          the reasoner match result
     * @return the derived reasoning tree
     */
    public ReasoningTree build(Model catalogModel, String requirementId, String capabilityType,
                               ServiceMatchResult match) {
        return build(catalogModel, requirementId, capabilityType, match, null);
    }

    /**
     * Per-build mutable state. Kept as a separate context so the singleton
     * builder stays safe if two pipeline runs ever emit concurrently.
     */
    private static final class Ctx {

        private final Model model;
        private final String requirementId;
        private final String runId;
        private final Consumer<ReasoningNode> liveSink;
        private final List<ReasoningNode> nodes = new ArrayList<>();
        private int seq = 0;

        private Ctx(Model model, String requirementId, String runId,
                    Consumer<ReasoningNode> liveSink) {
            this.model = model;
            this.requirementId = requirementId;
            this.runId = runId;
            this.liveSink = liveSink;
        }

        private ReasoningTree build(String capabilityType, ServiceMatchResult match) {
            Resource req = findRequirement(model, requirementId);
            String reqLabel = req != null ? labelOf(req, "UseCaseRequirement")
                : (requirementId != null ? requirementId : "Requirement");
            emit(null, "req", ReasoningNode.NodeType.REQUIREMENT,
                reqLabel, uriOf(req), null, null, ReasoningNode.INFO, 0, null);

            declareConstraintIntents(req);

            Resource reqCap = req != null ? obj(req, "mapsToCapability") : null;
            String resolvedType = capabilityType != null && !capabilityType.isBlank()
                ? capabilityType
                : (reqCap != null ? lit(reqCap, "capabilityType") : null);

            emit("req", "req-cap", ReasoningNode.NodeType.REQUIRED_CAPABILITY,
                "RequiredCapability", uriOf(reqCap), "mapsToCapability", resolvedType,
                ReasoningNode.INFO, 1, null);

            if (resolvedType == null || resolvedType.isBlank()) {
                emit("req-cap", "req-type-missing", ReasoningNode.NodeType.CONSTRAINT,
                    "capabilityType", null, "capabilityType", null, ReasoningNode.FAIL, 2,
                    "no required capability type to match");
                finish(match);
                return new ReasoningTree(nodes);
            }

            Map<String, String> direct = directConstraints(req);
            List<Resource> offers = offeredCapabilities(resolvedType);

            if (offers.isEmpty()) {
                emit("req-cap", "offer-missing", ReasoningNode.NodeType.OFFERED_CAPABILITY,
                    "No offered capability", null, "capabilityType", resolvedType,
                    ReasoningNode.FAIL, 2, "no service advertises capability type '" + resolvedType + "'");
                finish(match);
                return new ReasoningTree(nodes);
            }

            boolean matched = match != null && match.matched();
            Resource matchedService = linkedServiceResource(match);
            boolean renderedMatch = false;
            int index = 0;
            for (Resource offer : offers) {
                index++;
                String offerKey = "offer-" + index;
                boolean isMatched = matched && !renderedMatch
                    && (matchedService == null || isSameService(matchedService, offer));
                if (isMatched || !matched) {
                    buildOffer(offerKey, offer, resolvedType, isMatched, direct, index);
                    if (isMatched) {
                        renderedMatch = true;
                    }
                } else {
                    emit("req-cap", offerKey, ReasoningNode.NodeType.OFFERED_CAPABILITY,
                        labelOf(offer, "OfferedCapability"), uriOf(offer), "capabilityType",
                        resolvedType, ReasoningNode.SKIPPED, 2,
                        "candidate considered — not selected by the reasoner (recall ordering)");
                }
            }

            finish(match);
            return new ReasoningTree(nodes);
        }

        private void finish(ServiceMatchResult match) {
            boolean matched = match != null && match.matched();
            emit("req", "outcome", ReasoningNode.NodeType.MATCH_OUTCOME,
                matched ? "Match derived" : "No match", null, null, null,
                matched ? ReasoningNode.PASS : ReasoningNode.FAIL, 1,
                match != null && match.explanation() != null ? match.explanation()
                    : "requirement resolved none of the offered capabilities");
        }

        /**
         * Renders the requirement-declared constraints and parameters as
         * informational intent nodes (the reasoner's declared evaluation target).
         */
        private void declareConstraintIntents(Resource req) {
            if (req == null) {
                return;
            }
            int i = 0;
            for (Resource constraint : list(req, "hasConstraint")) {
                i++;
                String type = lit(constraint, "constraintType");
                String expression = lit(constraint, "expression");
                emit("req", "declared-constraint-" + i, ReasoningNode.NodeType.CONSTRAINT,
                    "Declared constraint",
                    uriOf(constraint), "hasConstraint",
                    type != null ? type : "constraint",
                    ReasoningNode.INFO, 1,
                    expression != null ? expression : null);
            }
            int p = 0;
            for (Resource parameter : list(req, "hasParameter")) {
                p++;
                String name = lit(parameter, "parameterName");
                String min = lit(parameter, "minValue");
                String max = lit(parameter, "maxValue");
                StringBuilder target = new StringBuilder();
                if (name != null && !name.isBlank()) {
                    target.append(name).append(' ');
                }
                if (min != null) {
                    target.append("\u2265 ").append(min);
                } else if (max != null) {
                    target.append("\u2264 ").append(max);
                }
                emit("req", "declared-parameter-" + p, ReasoningNode.NodeType.CONSTRAINT,
                    "Declared target",
                    uriOf(parameter), "hasParameter",
                    target.toString(),
                    ReasoningNode.INFO, 1, null);
            }
        }

        private void buildOffer(String offerKey, Resource offer, String capabilityType,
                                boolean isMatched, Map<String, String> direct, int index) {
            emit("req-cap", offerKey, ReasoningNode.NodeType.OFFERED_CAPABILITY,
                labelOf(offer, "OfferedCapability"), uriOf(offer), "capabilityType",
                capabilityType, ReasoningNode.PASS, 2,
                isMatched ? "capability type matched" : null);

            Resource skill = obj(offer, "realisedBy");
            if (skill == null) {
                emit(offerKey, "offer-skill-missing-" + index, ReasoningNode.NodeType.SKILL,
                    "No skill", null, "realisedBy", null, ReasoningNode.FAIL, 3,
                    "offered capability is not realised by a skill");
                return;
            }
            emit(offerKey, "skill-" + index, ReasoningNode.NodeType.SKILL,
                labelOf(skill, "Skill"), uriOf(skill), "realisedBy", null, ReasoningNode.PASS, 3,
                "capability type matched to offered capability");

            Resource service = obj(skill, "isImplementedBy");
            if (service == null) {
                emit("skill-" + index, "skill-service-missing-" + index,
                    ReasoningNode.NodeType.SERVICE, "No service", null, "isImplementedBy", null,
                    ReasoningNode.FAIL, 4, "skill is not implemented by a service");
                return;
            }
            emit("skill-" + index, "service-" + index, ReasoningNode.NodeType.SERVICE,
                labelOf(service, "Service"), uriOf(service), "isImplementedBy", null,
                ReasoningNode.PASS, 4, "service implements the skill");

            buildService("service-" + index, service, direct, index);
            buildConceptCoverage("service-" + index, service, skill, index);
        }

        private void buildService(String svcKey, Resource service, Map<String, String> direct, int index) {
            Resource binding = obj(service, "hasBinding");
            if (binding != null) {
                emit(svcKey, "binding-" + index, ReasoningNode.NodeType.COMMUNICATION_BINDING,
                    labelOf(binding, "CommunicationBinding"), uriOf(binding), "hasBinding",
                    lit(binding, "bindingEndpointUri"), ReasoningNode.INFO, 5, null);
                String protocol = lit(binding, "protocol");
                if (protocol != null) {
                    emit("binding-" + index, "binding-protocol-" + index,
                        ReasoningNode.NodeType.CONSTRAINT, "protocol", null, "protocol", protocol,
                        compareValue(direct.get("protocol"), protocol), 6,
                        constraintDetail(direct.containsKey("protocol"), protocol));
                }
                String accessMode = lit(binding, "accessMode");
                if (accessMode != null) {
                    emit("binding-" + index, "binding-access-" + index,
                        ReasoningNode.NodeType.CONSTRAINT, "accessMode", null, "accessMode", accessMode,
                        compareValue(direct.get("accessMode"), accessMode), 6,
                        constraintDetail(direct.containsKey("accessMode"), accessMode));
                }
            }

            Resource connector = obj(service, "hasConnector");
            if (connector != null) {
                emit(svcKey, "connector-" + index, ReasoningNode.NodeType.CONNECTOR,
                    labelOf(connector, "DataConnector"), uriOf(connector), "hasConnector",
                    lit(connector, "connectorType"), ReasoningNode.INFO, 5, null);
            }

            String recall = lit(service, "reportedRecall");
            if (recall != null) {
                String expected = direct.get("minRecall");
                boolean constrained = expected != null;
                emit(svcKey, "service-recall-" + index, ReasoningNode.NodeType.CONSTRAINT,
                    "reportedRecall", null, "reportedRecall", recall,
                    constrained ? (numericGte(recall, expected) ? ReasoningNode.PASS : ReasoningNode.FAIL)
                        : ReasoningNode.INFO, 5,
                    constrained ? "requirement demands recall \u2265 " + expected
                        : "observed, no recall constraint declared");
            }
            String latency = lit(service, "reportedLatency");
            if (latency != null) {
                String expected = direct.get("maxLatency");
                boolean constrained = expected != null;
                emit(svcKey, "service-latency-" + index, ReasoningNode.NodeType.CONSTRAINT,
                    "reportedLatency", null, "reportedLatency", latency,
                    constrained ? (numericLte(latency, expected) ? ReasoningNode.PASS : ReasoningNode.FAIL)
                        : ReasoningNode.INFO, 5,
                    constrained ? "requirement demands latency \u2264 " + expected
                        : "observed, no latency constraint declared");
            }

            Resource deploy = obj(service, "isDeployedBy");
            if (deploy != null) {
                emit(svcKey, "deploy-" + index, ReasoningNode.NodeType.DEPLOYMENT_CONTEXT,
                    labelOf(deploy, "DeploymentContext"), uriOf(deploy), "isDeployedBy", null,
                    ReasoningNode.INFO, 5, null);
                String onPlant = lit(deploy, "onPlantNetwork");
                if (onPlant != null) {
                    emit("deploy-" + index, "deploy-onplant-" + index,
                        ReasoningNode.NodeType.CONSTRAINT, "onPlantNetwork", null, "onPlantNetwork",
                        onPlant, compareBoolean(direct.get("onPlantNetwork"), onPlant), 6,
                        constraintDetail(direct.containsKey("onPlantNetwork"), onPlant));
                }
                String determinism = lit(deploy, "determinismClass");
                if (determinism != null) {
                    emit("deploy-" + index, "deploy-determinism-" + index,
                        ReasoningNode.NodeType.CONSTRAINT, "determinismClass", null,
                        "determinismClass", determinism,
                        compareValue(direct.get("determinismClass"), determinism), 6,
                        constraintDetail(direct.containsKey("determinismClass"), determinism));
                }
                Resource safety = obj(deploy, "contextConstrainedBy");
                if (safety != null) {
                    String sil = lit(safety, "safetyIntegrityLevel");
                    emit("deploy-" + index, "deploy-safety-" + index,
                        ReasoningNode.NodeType.SAFETY_CLASS, labelOf(safety, "SafetyClass"),
                        uriOf(safety), "contextConstrainedBy", sil, ReasoningNode.INFO, 6, null);
                    if (sil != null) {
                        emit("deploy-safety-" + index, "deploy-sil-" + index,
                            ReasoningNode.NodeType.CONSTRAINT, "safetyIntegrityLevel", null,
                            "safetyIntegrityLevel", sil,
                            compareValue(direct.get("safetyIntegrityLevel"), sil), 7,
                            constraintDetail(direct.containsKey("safetyIntegrityLevel"), sil));
                    }
                }
                Resource zone = obj(deploy, "locatedInZone");
                if (zone != null) {
                    String zoneName = zoneLabel(zone);
                    emit("deploy-" + index, "deploy-zone-" + index,
                        ReasoningNode.NodeType.NETWORK_ZONE, labelOf(zone, "NetworkZone"),
                        uriOf(zone), "locatedInZone", zoneName, ReasoningNode.INFO, 6, null);
                    if (zoneName != null) {
                        emit("deploy-zone-" + index, "deploy-zone-match-" + index,
                            ReasoningNode.NodeType.CONSTRAINT, "networkZone", null, "networkZone",
                            zoneName, compareValue(direct.get("networkZone"), zoneName), 7,
                            constraintDetail(direct.containsKey("networkZone"), zoneName));
                    }
                }
            }
        }

        private void buildConceptCoverage(String parentKey, Resource service, Resource skill, int index) {
            int i = 0;
            for (Resource concept : list(skill, "requiresConcept")) {
                i++;
                String conceptKey = "concept-" + index + "-" + i;
                boolean covered = isConceptCovered(service, concept);
                emit(parentKey, conceptKey, ReasoningNode.NodeType.SEMANTIC_CONCEPT,
                    labelOf(concept, "SemanticConcept"), uriOf(concept), "requiresConcept",
                    lit(concept, "conceptID"), covered ? ReasoningNode.PASS : ReasoningNode.FAIL, 5,
                    null);
                emit(conceptKey, "concept-coverage-" + index + "-" + i,
                    ReasoningNode.NodeType.CONSTRAINT, "semantic coverage", null,
                    "mapsVariable \u2192 describedBy", null,
                    covered ? ReasoningNode.PASS : ReasoningNode.FAIL, 6,
                    covered ? "provided via a service connector's mapped process variable"
                        : "no service connector maps a variable described by the required concept");
            }
        }

        private boolean isConceptCovered(Resource service, Resource concept) {
            for (Resource connector : list(service, "hasConnector")) {
                for (Resource variable : list(connector, "mapsVariable")) {
                    if (concept.equals(obj(variable, "describedBy"))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isSameService(Resource expected, Resource offer) {
            if (expected == null) {
                return false;
            }
            Resource skill = obj(offer, "realisedBy");
            if (skill == null) {
                return false;
            }
            Resource service = obj(skill, "isImplementedBy");
            return service != null && expected.equals(service);
        }

        /**
         * Resolves the matched service resource from the match result by comparing
         * the matched service label against every offered capability's service label.
         */
        private Resource linkedServiceResource(ServiceMatchResult match) {
            if (match == null || !match.matched() || match.serviceName() == null) {
                return null;
            }
            Resource type = model.createResource(SDA_NS + "OfferedCapability");
            List<Resource> services = new ArrayList<>();
            model.listSubjectsWithProperty(RDF.type, type).forEachRemaining(offer -> {
                if (!offer.isURIResource()) {
                    return;
                }
                Resource skill = obj(offer, "realisedBy");
                if (skill == null) {
                    return;
                }
                Resource service = obj(skill, "isImplementedBy");
                if (service != null) {
                    services.add(service);
                }
            });
            for (Resource service : services) {
                if (match.serviceName().equals(labelOf(service, null))) {
                    return service;
                }
            }
            return services.isEmpty() ? null : services.get(0);
        }

        private Resource findRequirement(Model model, String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            Resource direct = model.getResource(INST_NS + id);
            if (direct.hasProperty(RDF.type, model.createResource(SDA_NS + "UseCaseRequirement"))) {
                return direct;
            }
            StmtIterator it = model.listStatements(null,
                model.createProperty(SDA_NS + "requirementID"), id);
            try {
                if (it.hasNext()) {
                    Resource res = it.next().getSubject();
                    if (res.isURIResource()) {
                        return res;
                    }
                }
            } finally {
                it.close();
            }
            return null;
        }

        private List<Resource> offeredCapabilities(String capabilityType) {
            List<Resource> offers = new ArrayList<>();
            Resource type = model.createResource(SDA_NS + "OfferedCapability");
            model.listSubjectsWithProperty(RDF.type, type).forEachRemaining(offer -> {
                if (offer.isURIResource()
                        && capabilityType != null
                        && capabilityType.equals(lit(offer, "capabilityType"))) {
                    offers.add(offer);
                }
            });
            return offers;
        }

        private Map<String, String> directConstraints(Resource req) {
            Map<String, String> c = new HashMap<>();
            if (req == null) {
                return c;
            }
            for (String key : new String[] {
                "protocol", "accessMode", "safetyIntegrityLevel", "determinismClass",
                "networkZone", "onPlantNetwork", "minRecall", "maxLatency"}) {
                String value = lit(req, key);
                if (value != null && !value.isBlank()) {
                    c.put(key, value);
                }
            }
            return c;
        }

        private String constraintDetail(boolean constrained, String value) {
            return constrained ? "requirement demands '" + value + "'"
                : "observed, no '" + value + "' constraint declared";
        }

        private String compareValue(String expected, String actual) {
            if (expected == null) {
                return ReasoningNode.INFO;
            }
            return expected.equals(actual) ? ReasoningNode.PASS : ReasoningNode.FAIL;
        }

        private String compareBoolean(String expected, String actual) {
            if (expected == null) {
                return ReasoningNode.INFO;
            }
            return Boolean.parseBoolean(expected) == Boolean.parseBoolean(actual)
                ? ReasoningNode.PASS : ReasoningNode.FAIL;
        }

        private boolean numericGte(String actual, String expected) {
            try {
                return Double.parseDouble(actual) >= Double.parseDouble(expected);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private boolean numericLte(String actual, String expected) {
            try {
                return Double.parseDouble(actual) <= Double.parseDouble(expected);
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private void emit(String parentId, String id, ReasoningNode.NodeType type, String label,
                          String iri, String property, String value, String status, int depth,
                          String detail) {
            ReasoningNode node = new ReasoningNode(runId, requirementId, id, type, label, iri,
                property, value, status, depth, parentId, detail, null);
            nodes.add(node);
            if (liveSink != null) {
                liveSink.accept(node);
            }
        }

        private Resource obj(Resource s, String property) {
            if (s == null) {
                return null;
            }
            var st = s.getProperty(model.createProperty(SDA_NS + property));
            return st != null && st.getObject().isResource() ? st.getObject().asResource() : null;
        }

        private List<Resource> list(Resource s, String property) {
            if (s == null) {
                return List.of();
            }
            return model.listObjectsOfProperty(s, model.createProperty(SDA_NS + property))
                .filterKeep(RDFNode::isResource)
                .mapWith(RDFNode::asResource)
                .toList();
        }

        private String lit(Resource s, String property) {
            if (s == null) {
                return null;
            }
            var st = s.getProperty(model.createProperty(SDA_NS + property));
            return st != null && st.getObject().isLiteral()
                ? st.getObject().asLiteral().getString() : null;
        }

        private String labelOf(Resource r, String fallback) {
            if (r == null) {
                return fallback;
            }
            var st = r.getProperty(RDFS.label);
            if (st != null && st.getObject().isLiteral()) {
                String label = st.getObject().asLiteral().getString();
                if (label != null && !label.isBlank()) {
                    return label;
                }
            }
            String local = r.getLocalName();
            if (local != null && !local.isBlank()) {
                return local;
            }
            return fallback;
        }

        private String uriOf(Resource r) {
            return r != null ? r.getURI() : null;
        }

        private String zoneLabel(Resource zone) {
            String label = lit(zone, "zoneName");
            if (label == null || label.isBlank()) {
                var st = zone.getProperty(RDFS.label);
                if (st != null && st.getObject().isLiteral()) {
                    label = st.getObject().asLiteral().getString();
                }
            }
            return label;
        }
    }
}