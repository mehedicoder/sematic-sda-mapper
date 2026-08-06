package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.DeploymentContext;
import com.sda.mapper.hub.model.OfferedCapability;
import com.sda.mapper.hub.model.RequiredCapability;
import com.sda.mapper.hub.model.Skill;
import com.sda.mapper.reasoning.SparqlExecutionException;
import com.sda.mapper.reasoning.SparqlQueryLoader;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MATCH stage (semantic): executes the SPARQL matching query
 * {@code queries/hub-match-offered-required.rq} against an in-memory ABox
 * instantiated from the canonical model plus the inferred entities.
 *
 * <p>Per the paper (Step 5) the query compares each inferred offered
 * {@link com.sda.mapper.hub.model.Capability} against each required
 * {@link com.sda.mapper.hub.model.RequiredCapability} across the capability
 * (semantic concept, engineering unit, nominal range, SIL), communication
 * (protocol, endpoint) and deployment (network zone) dimensions and projects
 * the matched skill, service and endpoint.</p>
 *
 * <p>The ABox is a deterministic, id-independent snapshot built with explicit
 * MVP heuristics until a dedicated Instantiate stage provides relation data:
 * every skill is implemented by every service, every service has every binding
 * and every deployment context deploys every service. This yields exact chains
 * for single-service/single-binding models such as the device-description
 * demo.</p>
 */
@Service
public class SemanticMatchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticMatchService.class);

    private static final String SDA_NS = "http://sda-platform.org/ontology#";
    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String MATCH_QUERY_PATH = "queries/hub-match-offered-required.rq";

    private final SparqlQueryLoader sparqlQueryLoader;

    /**
     * Creates the semantic matching service.
     *
     * @param sparqlQueryLoader loads the SPARQL match query from the classpath
     */
    public SemanticMatchService(SparqlQueryLoader sparqlQueryLoader) {
        this.sparqlQueryLoader = sparqlQueryLoader;
    }

    /**
     * Runs the SPARQL match over the canonical model and the inferred entities.
     *
     * @param model    the canonical model (parsed and lifted entities)
     * @param inferred the inferred entities (offered capabilities, sensors, ...)
     * @return the semantic capability matches, possibly empty
     * @throws SparqlExecutionException when the query cannot be loaded or executed
     */
    public List<CapabilityMatch> match(CanonicalAssetModel model, List<CanonicalEntity> inferred) {
        try {
            Abox abox = instantiate(model, inferred);
            String queryStr = sparqlQueryLoader.loadQuery(MATCH_QUERY_PATH);
            Query query = QueryFactory.create(queryStr);

            List<CapabilityMatch> matches = new ArrayList<>();
            try (QueryExecution qe = QueryExecutionFactory.create(query, abox.model())) {
                ResultSet rs = qe.execSelect();
                while (rs.hasNext()) {
                    matches.add(toMatch(rs.next(), abox.uriToId()));
                }
            }
            log.debug("Semantic match produced {} match(es)", matches.size());
            return matches;
        } catch (SparqlExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new SparqlExecutionException("Failed to execute semantic matching query", e);
        }
    }

    /**
     * Builds the ABox snapshot and the URI to canonical id mapping.
     *
     * <p>Instantiates the v2 vocabulary (see {@code demo-abox-v2.ttl} for the
     * canonical chains): an inferred {@code sda:Capability} realises a
     * {@code sda:Skill} that declares its semantics via
     * {@code requiresConcept} ({@code sda:SemanticConcept}) and
     * {@code hasSkillParameter} ({@code sda:SkillParameter}), while a required
     * {@code sda:UseCaseRequirement} expresses the demand via
     * {@code hasParameter} ({@code sda:UseCaseRequirementParameter}) and
     * {@code mapsToDeploymentContext} (SIL via {@code SafetyClass}, zone via
     * {@code NetworkZone}).</p>
     */
    private static Abox instantiate(CanonicalAssetModel model, List<CanonicalEntity> inferred) {
        Model m = ModelFactory.createDefaultModel();
        Map<String, String> uriToId = new LinkedHashMap<>();
        Map<String, Resource> skillBySkillId = new LinkedHashMap<>();
        Map<String, Resource> conceptByName = new LinkedHashMap<>();
        List<Resource> serviceResources = new ArrayList<>();
        List<Resource> bindingResources = new ArrayList<>();
        List<Resource> deploymentResources = new ArrayList<>();

        for (Skill skill : model.skills()) {
            Resource r = newResource(m, uriToId, "skill", skill.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "Skill"));
            skillBySkillId.put(skill.id(), r);
        }

        List<OfferedCapability> offeredCapabilities = inferred.stream()
            .filter(e -> e instanceof OfferedCapability)
            .map(e -> (OfferedCapability) e)
            .toList();
        for (OfferedCapability offered : offeredCapabilities) {
            Resource r = newResource(m, uriToId, "offered", offered.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "Capability"));
            addLabelIfNotBlank(m, r, offered.name());
            Resource skill = skillBySkillId.get(offered.skillId());
            if (skill != null) {
                r.addProperty(m.createProperty(SDA_NS + "realisedBy"), skill);
                Resource concept = conceptByName.computeIfAbsent(offered.functionClass(), fn -> {
                    Resource c = m.createResource(INST_NS + "concept_" + sanitize(fn));
                    c.addProperty(RDF.type, m.createResource(SDA_NS + "SemanticConcept"));
                    c.addProperty(m.createProperty(SDA_NS + "conceptName"), fn);
                    return c;
                });
                skill.addProperty(m.createProperty(SDA_NS + "requiresConcept"), concept);
                Resource offParam = newResource(m, uriToId, "offParam", offered.id());
                offParam.addProperty(RDF.type, m.createResource(SDA_NS + "SkillParameter"));
                addLiteralIfNotBlank(m, offParam, "paramName", offered.functionClass());
                addLiteralIfNotBlank(m, offParam, "paramUnit", offered.engineeringUnit());
                addLiteralIfNotBlank(m, offParam, "paramMinValue", offered.nominalRangeMin());
                addLiteralIfNotBlank(m, offParam, "paramMaxValue", offered.nominalRangeMax());
                skill.addProperty(m.createProperty(SDA_NS + "hasSkillParameter"), offParam);
            }
        }

        for (RequiredCapability required : model.requiredCapabilities()) {
            Resource r = newResource(m, uriToId, "required", required.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "UseCaseRequirement"));
            addLiteralIfNotBlank(m, r, "requirementID", required.id());
            addLabelIfNotBlank(m, r, required.name());
            addLiteralIfNotBlank(m, r, "protocol", required.protocol());

            Resource reqParam = newResource(m, uriToId, "reqParam", required.id());
            reqParam.addProperty(RDF.type, m.createResource(SDA_NS + "UseCaseRequirementParameter"));
            addLiteralIfNotBlank(m, reqParam, "parameterName", required.functionClass());
            addLiteralIfNotBlank(m, reqParam, "unit", required.engineeringUnit());
            addLiteralIfNotBlank(m, reqParam, "minValue", required.nominalRangeMin());
            addLiteralIfNotBlank(m, reqParam, "maxValue", required.nominalRangeMax());
            r.addProperty(m.createProperty(SDA_NS + "hasParameter"), reqParam);

            Resource reqDeployment = newResource(m, uriToId, "reqDeployment", required.id());
            reqDeployment.addProperty(RDF.type, m.createResource(SDA_NS + "DeploymentContext"));
            r.addProperty(m.createProperty(SDA_NS + "mapsToDeploymentContext"), reqDeployment);

            Resource reqSafety = newResource(m, uriToId, "reqSafety", required.id());
            reqSafety.addProperty(RDF.type, m.createResource(SDA_NS + "SafetyClass"));
            addIntIfParsable(m, reqSafety, "safetyIntegrityLevel", required.safetyIntegrityLevel());
            reqDeployment.addProperty(m.createProperty(SDA_NS + "contextConstrainedBy"), reqSafety);

            Resource reqZone = newResource(m, uriToId, "reqZone", required.id());
            reqZone.addProperty(RDF.type, m.createResource(SDA_NS + "NetworkZone"));
            addLiteralIfNotBlank(m, reqZone, "zoneName", required.networkZone());
            reqDeployment.addProperty(m.createProperty(SDA_NS + "locatedInZone"), reqZone);
        }

        for (com.sda.mapper.hub.model.Service service : model.services()) {
            Resource r = newResource(m, uriToId, "service", service.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "Service"));
            addLabelIfNotBlank(m, r, service.name());
            serviceResources.add(r);
        }

        for (CommunicationBinding binding : model.communicationBindings()) {
            Resource r = newResource(m, uriToId, "binding", binding.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "CommunicationBinding"));
            addLiteralIfNotBlank(m, r, "protocol", binding.protocol());
            addLiteralIfNotBlank(m, r, "bindingEndpointUri", binding.endpointUrl());
            bindingResources.add(r);
        }

        String offeredSil = offeredCapabilities.stream()
            .map(OfferedCapability::safetyIntegrityLevel)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse("");
        for (DeploymentContext context : model.deploymentContexts()) {
            Resource r = newResource(m, uriToId, "deployment", context.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "DeploymentContext"));
            Resource safety = newResource(m, uriToId, "safety", context.id());
            safety.addProperty(RDF.type, m.createResource(SDA_NS + "SafetyClass"));
            addIntIfParsable(m, safety, "safetyIntegrityLevel", offeredSil);
            r.addProperty(m.createProperty(SDA_NS + "contextConstrainedBy"), safety);
            Resource zone = newResource(m, uriToId, "zone", context.id());
            zone.addProperty(RDF.type, m.createResource(SDA_NS + "NetworkZone"));
            addLiteralIfNotBlank(m, zone, "zoneName", context.networkZone());
            r.addProperty(m.createProperty(SDA_NS + "locatedInZone"), zone);
            deploymentResources.add(r);
        }

        // MVP heuristics: chain every skill through every service, binding and
        // deployment context so single-service/single-binding models match.
        Property isImplementedBy = m.createProperty(SDA_NS + "isImplementedBy");
        for (Resource skill : skillBySkillId.values()) {
            for (Resource service : serviceResources) {
                skill.addProperty(isImplementedBy, service);
            }
        }
        Property hasBinding = m.createProperty(SDA_NS + "hasBinding");
        for (Resource service : serviceResources) {
            for (Resource binding : bindingResources) {
                service.addProperty(hasBinding, binding);
            }
        }
        Property deploysService = m.createProperty(SDA_NS + "deploysService");
        for (Resource service : serviceResources) {
            for (Resource deployment : deploymentResources) {
                deployment.addProperty(deploysService, service);
            }
        }

        return new Abox(m, uriToId);
    }

    private static Resource newResource(Model model, Map<String, String> uriToId,
                                        String prefix, String id) {
        String uri = INST_NS + prefix + "_" + uriToId.size();
        uriToId.put(uri, id);
        return model.createResource(uri);
    }

    private static void addLiteralIfNotBlank(Model model, Resource resource, String property, String value) {
        if (value != null && !value.isBlank()) {
            resource.addProperty(model.createProperty(SDA_NS + property), value);
        }
    }

    private static void addLabelIfNotBlank(Model model, Resource resource, String value) {
        if (value != null && !value.isBlank()) {
            resource.addProperty(RDFS.label, value);
        }
    }

    private static void addIntIfParsable(Model model, Resource resource, String property, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            resource.addProperty(model.createProperty(SDA_NS + property),
                model.createTypedLiteral(Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            log.debug("Skipping non-integer {} value '{}'", property, value);
        }
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static CapabilityMatch toMatch(QuerySolution sol, Map<String, String> uriToId) {
        String required = entityId(sol, "required", uriToId);
        String offered = entityId(sol, "offered", uriToId);
        return new CapabilityMatch(
            "MATCH-" + required + "-" + offered,
            required,
            offered,
            entityId(sol, "skill", uriToId),
            optionalLiteral(sol, "serviceName"),
            optionalLiteral(sol, "endpointUrl"),
            sol.contains("protocol") ? sol.getLiteral("protocol").getString() : "",
            optionalLiteral(sol, "networkZone"),
            optionalLiteral(sol, "sil"));
    }

    private static String entityId(QuerySolution sol, String varName, Map<String, String> uriToId) {
        RDFNode node = sol.get(varName);
        if (node != null && node.isResource()) {
            return uriToId.getOrDefault(node.asResource().getURI(), "");
        }
        return "";
    }

    private static String optionalLiteral(QuerySolution sol, String varName) {
        RDFNode node = sol.get(varName);
        if (node != null && node.isLiteral()) {
            return node.asLiteral().getString();
        }
        return "";
    }

    private record Abox(Model model, Map<String, String> uriToId) {
    }
}
