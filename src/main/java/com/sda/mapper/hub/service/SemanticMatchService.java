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
 * <p>Per the paper (Step 5) the query compares each inferred
 * {@link OfferedCapability} against each {@link RequiredCapability} across the
 * capability (functionClass, engineering unit, nominal range, SIL),
 * communication (protocol, endpoint) and deployment (network zone) dimensions
 * and projects the matched skill, service and endpoint.</p>
 *
 * <p>The ABox is a deterministic, id-independent snapshot built with explicit
 * MVP heuristics until a dedicated Instantiate stage provides relation data:
 * every skill is implemented by every service, every service uses every binding
 * and is deployed in every deployment context. This yields exact chains for
 * single-service/single-binding models such as the device-description demo.</p>
 */
@Service
public class SemanticMatchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticMatchService.class);

    private static final String SDA_NS = "https://example.org/ontology/sda#";
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
     */
    private static Abox instantiate(CanonicalAssetModel model, List<CanonicalEntity> inferred) {
        Model m = ModelFactory.createDefaultModel();
        Map<String, String> uriToId = new LinkedHashMap<>();
        Map<String, Resource> skillBySkillId = new LinkedHashMap<>();
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
            r.addProperty(RDF.type, m.createResource(SDA_NS + "OfferedCapability"));
            addLiteralIfNotBlank(m, r, "functionClass", offered.functionClass());
            addLiteralIfNotBlank(m, r, "engineeringUnit", offered.engineeringUnit());
            addDoubleIfParsable(m, r, "nominalRange_min", offered.nominalRangeMin());
            addDoubleIfParsable(m, r, "nominalRange_max", offered.nominalRangeMax());
            addLiteralIfNotBlank(m, r, "safetyIntegrityLevel", offered.safetyIntegrityLevel());
            Resource skill = skillBySkillId.get(offered.skillId());
            if (skill != null) {
                r.addProperty(m.createProperty(SDA_NS + "realisedBy"), skill);
            }
        }

        for (RequiredCapability required : model.requiredCapabilities()) {
            Resource r = newResource(m, uriToId, "required", required.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "RequiredCapability"));
            addLiteralIfNotBlank(m, r, "functionClass", required.functionClass());
            addLiteralIfNotBlank(m, r, "engineeringUnit", required.engineeringUnit());
            addDoubleIfParsable(m, r, "nominalRange_min", required.nominalRangeMin());
            addDoubleIfParsable(m, r, "nominalRange_max", required.nominalRangeMax());
            addLiteralIfNotBlank(m, r, "safetyIntegrityLevel", required.safetyIntegrityLevel());
            addLiteralIfNotBlank(m, r, "protocol", required.protocol());
            addLiteralIfNotBlank(m, r, "networkZone", required.networkZone());
        }

        for (com.sda.mapper.hub.model.Service service : model.services()) {
            Resource r = newResource(m, uriToId, "service", service.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "Service"));
            if (service.name() != null && !service.name().isBlank()) {
                r.addProperty(RDFS.label, service.name());
            }
            serviceResources.add(r);
        }

        for (CommunicationBinding binding : model.communicationBindings()) {
            Resource r = newResource(m, uriToId, "binding", binding.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "CommunicationBinding"));
            addLiteralIfNotBlank(m, r, "protocol", binding.protocol());
            addLiteralIfNotBlank(m, r, "endpointUrl", binding.endpointUrl());
            bindingResources.add(r);
        }

        for (DeploymentContext context : model.deploymentContexts()) {
            Resource r = newResource(m, uriToId, "deployment", context.id());
            r.addProperty(RDF.type, m.createResource(SDA_NS + "DeploymentContext"));
            addLiteralIfNotBlank(m, r, "networkZone", context.networkZone());
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
        Property uses = m.createProperty(SDA_NS + "uses");
        for (Resource service : serviceResources) {
            for (Resource binding : bindingResources) {
                service.addProperty(uses, binding);
            }
        }
        Property isDeployedBy = m.createProperty(SDA_NS + "isDeployedBy");
        for (Resource service : serviceResources) {
            for (Resource deployment : deploymentResources) {
                service.addProperty(isDeployedBy, deployment);
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

    private static void addDoubleIfParsable(Model model, Resource resource, String property, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            resource.addProperty(model.createProperty(SDA_NS + property),
                model.createTypedLiteral(Double.parseDouble(value.trim())));
        } catch (NumberFormatException e) {
            log.debug("Skipping non-numeric {} value '{}'", property, value);
        }
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
