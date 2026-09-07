package com.sda.mapper.reasoning;

/**
 * One node of the live ontology reasoning tree.
 *
 * <p>Each node mirrors one hop of the semantic matching chain that
 * {@code queries/match-generic.rq} traverses (requirement &rarr; required
 * capability &rarr; offered capability &rarr; skill &rarr; service &rarr;
 * binding / connector / deployment context / network zone / safety class /
 * semantic-concept coverage). Nodes are streamed to the Mapping Hub UI as the
 * MATCH stage runs so the user can watch a match be derived through the
 * ontology.</p>
 *
 * @param runId         the pipeline run the node belongs to
 * @param requirementId the requirement the chain resolves
 * @param id            node id, unique within one tree
 * @param type          the SDA ontology concept class the node represents
 * @param label         human-readable node label (class or instance label)
 * @param iri           the RDF resource URI, may be {@code null} for derived nodes
 * @param property      the SDA predicate that led to this node (e.g. {@code mapsToCapability})
 * @param value         the observed literal value, may be {@code null}
 * @param status        {@link #INFO}, {@link #PASS}, {@link #FAIL} or {@link #SKIPPED}
 * @param depth         tree depth (root is 0)
 * @param parentId      parent node id, {@code null} for roots
 * @param detail        human-readable evidence or failure reason
 * @param durationMs    stage instrumentation duration, may be {@code null}
 */
public record ReasoningNode(
    String runId,
    String requirementId,
    String id,
    NodeType type,
    String label,
    String iri,
    String property,
    String value,
    String status,
    int depth,
    String parentId,
    String detail,
    Long durationMs
) {

    /** Informational node: constraint observed, no pass/fail claim. */
    public static final String INFO = "INFO";
    /** The chain step satisfies the requirement (or was required and present). */
    public static final String PASS = "PASS";
    /** The chain step breaks the requirement: this is why no match was derived. */
    public static final String FAIL = "FAIL";
    /** Candidate chain rejected by the reasoner's ordering. */
    public static final String SKIPPED = "SKIPPED";

    /**
     * The SDA ontology concept class a reasoning node renders.
     */
    public enum NodeType {
        REQUIREMENT,
        REQUIRED_CAPABILITY,
        OFFERED_CAPABILITY,
        SKILL,
        SERVICE,
        COMMUNICATION_BINDING,
        CONNECTOR,
        DEPLOYMENT_CONTEXT,
        NETWORK_ZONE,
        SAFETY_CLASS,
        SEMANTIC_CONCEPT,
        CONSTRAINT,
        MATCH_OUTCOME
    }
}