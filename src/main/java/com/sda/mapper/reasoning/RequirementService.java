package com.sda.mapper.reasoning;

import com.sda.mapper.api.dto.MatchRequirementRequest;
import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service for user-defined requirements.
 *
 * <p>Persists a {@code UseCaseRequirement} (with its capability and constraints)
 * into the live ontology model and delegates matching to
 * {@link SemanticReasonerService}. This is what lets a user enter a requirement
 * name/description/capability (via the UI or REST) and have it matched by the
 * generic matcher without any per-requirement query.
 */
@Service
public class RequirementService {

    private static final Logger log = LoggerFactory.getLogger(RequirementService.class);

    private static final String INST_NS = "http://sda-platform.org/instance#";
    private static final String SDA_NS = "http://sda-platform.org/ontology#";
    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    private final OntologyModelLoader ontologyModelLoader;
    private final SemanticReasonerService reasonerService;

    public RequirementService(OntologyModelLoader ontologyModelLoader,
                              SemanticReasonerService reasonerService) {
        this.ontologyModelLoader = ontologyModelLoader;
        this.reasonerService = reasonerService;
    }

    /**
     * Persists a requirement into the ontology model as RDF triples.
     * The {@code requirementId} is mandatory — the system never auto-generates
     * requirement IDs; requirements must be defined by the user.
     *
     * @param request the requirement fields
     */
    public void persist(MatchRequirementRequest request) {
        if (request.requirementId() == null || request.requirementId().isBlank()) {
            throw new IllegalArgumentException("requirementId is required");
        }
        String requirementId = request.requirementId();
        Model model = ontologyModelLoader.getModel();
        String resourceUri = INST_NS + requirementId;

        Resource requirement = model.createResource(resourceUri);
        requirement.addProperty(
            model.createProperty(RDF_TYPE),
            model.createResource(SDA_NS + "UseCaseRequirement"));
        setLiteral(model, requirement, SDA_NS + "requirementID", requirementId);
        String name = request.requirementName() != null ? request.requirementName() : requirementId;
        setLiteral(model, requirement, SDA_NS + "requirementName", name);
        setLiteral(model, requirement, RDFS_LABEL, name);
        setLiteral(model, requirement, SDA_NS + "requirementDescription", request.requirementDescription());
        setLiteral(model, requirement, SDA_NS + "lifecycleStatus", "Defined");

        if (request.capabilityType() != null && !request.capabilityType().isBlank()) {
            requirement.removeAll(model.createProperty(SDA_NS + "mapsToCapability"));
            Resource oldCap = model.getResource(INST_NS + "CAP-" + requirementId + "-Required");
            if (oldCap != null) {
                oldCap.removeProperties();
            }
            Resource cap = model.createResource(INST_NS + "CAP-" + requirementId + "-Required");
            cap.addProperty(model.createProperty(RDF_TYPE),
                model.createResource(SDA_NS + "RequiredCapability"));
            cap.addProperty(model.createProperty(SDA_NS + "capabilityType"), request.capabilityType());
            cap.addProperty(model.createProperty(SDA_NS + "capabilityName"), request.capabilityType());
            cap.addProperty(model.createProperty(SDA_NS + "capabilityID"), "CAP-" + requirementId + "-001");
            requirement.addProperty(model.createProperty(SDA_NS + "mapsToCapability"), cap);
        }

        setLiteral(model, requirement, SDA_NS + "protocol", request.protocol());
        setLiteral(model, requirement, SDA_NS + "accessMode", request.accessMode());
        setLiteral(model, requirement, SDA_NS + "networkZone", request.networkZone());
        setLiteral(model, requirement, SDA_NS + "safetyIntegrityLevel", request.safetyIntegrityLevel());
        setLiteral(model, requirement, SDA_NS + "determinismClass", request.determinismClass());
        if (request.minRecall() != null) {
            requirement.removeAll(model.createProperty(SDA_NS + "minRecall"));
            requirement.addProperty(model.createProperty(SDA_NS + "minRecall"),
                model.createTypedLiteral(request.minRecall(), XSDDatatype.XSDdouble));
        }
        if (request.maxLatencyMs() != null) {
            requirement.removeAll(model.createProperty(SDA_NS + "maxLatency"));
            requirement.addProperty(model.createProperty(SDA_NS + "maxLatency"),
                model.createTypedLiteral(request.maxLatencyMs(), XSDDatatype.XSDint));
        }

        log.info("Persisted requirement {} into ontology model", requirementId);
    }

    /**
     * Reads a persisted requirement back into a {@link MatchRequirementRequest} so
     * its fields can be shown in editable UI controls. Returns {@code null} if no
     * such requirement exists in the model.
     */
    public MatchRequirementRequest readRequest(String requirementId) {
        Model model = ontologyModelLoader.getModel();
        Resource r = model.getResource(INST_NS + requirementId);
        if (!r.hasProperty(model.createProperty(RDF_TYPE),
                model.createResource(SDA_NS + "UseCaseRequirement"))) {
            return null;
        }
        String name = literal(r, SDA_NS + "requirementName");
        String description = literal(r, SDA_NS + "requirementDescription");
        String capabilityType = null;
        Statement capStmt = r.getProperty(model.createProperty(SDA_NS + "mapsToCapability"));
        if (capStmt != null && capStmt.getObject().isResource()) {
            capabilityType = literal(capStmt.getObject().asResource(), SDA_NS + "capabilityType");
        }
        return new MatchRequirementRequest(
            requirementId, name, description, capabilityType,
            null,
            literal(r, SDA_NS + "protocol"),
            literal(r, SDA_NS + "accessMode"),
            null,
            literal(r, SDA_NS + "safetyIntegrityLevel"),
            literal(r, SDA_NS + "networkZone"),
            literal(r, SDA_NS + "determinismClass"),
            typedDouble(r, SDA_NS + "minRecall"),
            typedInt(r, SDA_NS + "maxLatency"),
            null,
            null);
    }

    private static String literal(Resource r, String property) {
        Statement s = r.getProperty(r.getModel().createProperty(property));
        if (s == null) {
            return null;
        }
        RDFNode obj = s.getObject();
        return obj.isLiteral() ? ((Literal) obj).getString() : obj.toString();
    }

    private static Double typedDouble(Resource r, String property) {
        Statement s = r.getProperty(r.getModel().createProperty(property));
        if (s == null || !s.getObject().isLiteral()) {
            return null;
        }
        try {
            return ((Literal) s.getObject()).getDouble();
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer typedInt(Resource r, String property) {
        Statement s = r.getProperty(r.getModel().createProperty(property));
        if (s == null || !s.getObject().isLiteral()) {
            return null;
        }
        try {
            return ((Literal) s.getObject()).getInt();
        } catch (Exception e) {
            return null;
        }
    }

    private void setLiteral(Model model, Resource subject, String property, String value) {
        Property p = model.createProperty(property);
        subject.removeAll(p);
        if (value != null && !value.isBlank()) {
            subject.addProperty(p, value);
        }
    }

    /**
     * Matches a stored requirement by ID using the generic matcher.
     */
    public ServiceMatchResult match(String requirementId) {
        return reasonerService.matchByRequirementId(requirementId);
    }

    /**
     * Matches a requirement by ID, creating it on the fly when the ID is not yet
     * in the model but the caller supplied a capability type or constraints.
     *
     * <p>This lets a user enter an arbitrary requirement ID (e.g. {@code REQ-ABC})
     * together with a capability type / constraints and still obtain a match,
     * without first persisting it through a separate "create" step.</p>
     *
     * @param request the requirement id plus optional capability/constraints
     * @return the match result (no-match if the id is unknown and no spec given)
     */
    public ServiceMatchResult matchOrCreate(MatchRequirementRequest request) {
        String id = request.requirementId();
        if (id == null || id.isBlank()) {
            return ServiceMatchResult.noMatch("Requirement ID is required");
        }
        if (requirementExists(id)) {
            boolean hasSpec = (request.capabilityType() != null && !request.capabilityType().isBlank())
                || request.protocol() != null || request.accessMode() != null
                || request.minRecall() != null || request.maxLatencyMs() != null
                || request.networkZone() != null || request.safetyIntegrityLevel() != null
                || request.determinismClass() != null;
            // Re-persist when the caller supplied (possibly edited) values, so changes
            // made in the UI before running the match are applied; otherwise match as-is.
            if (hasSpec) {
                persist(request);
            }
            return match(id);
        }
        boolean hasSpec = (request.capabilityType() != null && !request.capabilityType().isBlank())
            || request.protocol() != null || request.accessMode() != null
            || request.minRecall() != null || request.maxLatencyMs() != null
            || request.networkZone() != null || request.safetyIntegrityLevel() != null
            || request.determinismClass() != null;
        if (!hasSpec) {
            return ServiceMatchResult.noMatch(
                "Unknown requirement: " + id + " — provide a capability type or constraints to match it");
        }
        persist(request);
        return match(id);
    }

    private boolean requirementExists(String id) {
        Model model = ontologyModelLoader.getModel();
        Resource r = model.getResource(INST_NS + id);
        return r.hasProperty(
            model.createProperty(RDF_TYPE),
            model.createResource(SDA_NS + "UseCaseRequirement"));
    }

    private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
}
