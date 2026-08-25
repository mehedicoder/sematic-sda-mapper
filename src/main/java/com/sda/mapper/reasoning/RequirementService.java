package com.sda.mapper.reasoning;

import com.sda.mapper.api.dto.MatchRequirementRequest;
import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
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
        requirement.addProperty(
            model.createProperty(SDA_NS + "requirementID"), requirementId);
        requirement.addProperty(
            model.createProperty(SDA_NS + "requirementName"),
            request.requirementName() != null ? request.requirementName() : requirementId);
        requirement.addProperty(
            model.createProperty(RDFS_LABEL),
            request.requirementName() != null ? request.requirementName() : requirementId);
        if (request.requirementDescription() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "requirementDescription"),
                request.requirementDescription());
        }
        requirement.addProperty(
            model.createProperty(SDA_NS + "lifecycleStatus"), "Defined");

        if (request.capabilityType() != null && !request.capabilityType().isBlank()) {
            Resource cap = model.createResource(
                INST_NS + "CAP-" + requirementId + "-Required");
            cap.addProperty(
                model.createProperty(RDF_TYPE),
                model.createResource(SDA_NS + "RequiredCapability"));
            cap.addProperty(
                model.createProperty(SDA_NS + "capabilityType"), request.capabilityType());
            cap.addProperty(
                model.createProperty(SDA_NS + "capabilityName"), request.capabilityType());
            cap.addProperty(
                model.createProperty(SDA_NS + "capabilityID"),
                "CAP-" + requirementId + "-001");
            requirement.addProperty(
                model.createProperty(SDA_NS + "mapsToCapability"), cap);
        }

        addLiteral(model, requirement, SDA_NS + "protocol", request.protocol());
        addLiteral(model, requirement, SDA_NS + "accessMode", request.accessMode());
        addLiteral(model, requirement, SDA_NS + "networkZone", request.networkZone());
        addLiteral(model, requirement, SDA_NS + "safetyIntegrityLevel",
            request.safetyIntegrityLevel());
        addLiteral(model, requirement, SDA_NS + "determinismClass",
            request.determinismClass());
        if (request.minRecall() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "minRecall"),
                model.createTypedLiteral(request.minRecall(), XSDDatatype.XSDdouble));
        }
        if (request.maxLatencyMs() != null) {
            requirement.addProperty(
                model.createProperty(SDA_NS + "maxLatency"),
                model.createTypedLiteral(request.maxLatencyMs(), XSDDatatype.XSDint));
        }

        log.info("Persisted requirement {} into ontology model", requirementId);
    }

    /**
     * Matches a stored requirement by ID using the generic matcher.
     */
    public ServiceMatchResult match(String requirementId) {
        return reasonerService.matchByRequirementId(requirementId);
    }

    private void addLiteral(Model model, Resource subject, String property, String value) {
        if (value != null && !value.isBlank()) {
            subject.addProperty(model.createProperty(property), value);
        }
    }

    private static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
}
