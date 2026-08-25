package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Executes SPARQL matching queries against the CSS-SDA ontology model
 * to find services that satisfy given requirements.
 * <p>
 * A single generic query ({@code queries/match-generic.rq}) serves every
 * requirement: it is parameterized by the requirement URI and reads the
 * requirement's capability and constraints from the model, so no per-requirement
 * query file is needed. This supports user-defined requirements as well as the
 * seeded paper use cases (REQ-001..004).
 * <p>
 * Two matching modes remain:
 * <ul>
 *   <li><b>Mode A</b> — match a stored requirement by ID
 *        ({@code REQ-001}, a user-created ID, ...)</li>
 *   <li><b>Mode B</b> — match by inline parameters (protocol, accessMode, ...)</li>
 * </ul>
 */
@Service
public class SemanticReasonerService {

    private static final Logger log = LoggerFactory.getLogger(SemanticReasonerService.class);

    private static final String INST_NS = "http://sda-platform.org/instance#";
    private static final String SDA_NS = "http://sda-platform.org/ontology#";

    private static final String GENERIC_QUERY = "queries/match-generic.rq";

    private final OntologyModelLoader ontologyModelLoader;
    private final SparqlQueryLoader sparqlQueryLoader;
    private final RDeployService rDeployService;

    public SemanticReasonerService(OntologyModelLoader ontologyModelLoader,
                                   SparqlQueryLoader sparqlQueryLoader,
                                   RDeployService rDeployService) {
        this.ontologyModelLoader = ontologyModelLoader;
        this.sparqlQueryLoader = sparqlQueryLoader;
        this.rDeployService = rDeployService;
    }

    /**
     * Matches a requirement against the ontology.
     *
     * @param requirementId the requirement ID (e.g. "REQ-001" or a user-created ID)
     * @return a {@link ServiceMatchResult} with all matched service details
     */
    public ServiceMatchResult matchByRequirementId(String requirementId) {
        if (requirementId == null || requirementId.isBlank()) {
            return ServiceMatchResult.noMatch("UseCaseRequirement ID is null or blank");
        }

        Model model = ontologyModelLoader.getModel();
        Resource reqResource = model.getResource(INST_NS + requirementId);
        boolean exists = reqResource.hasProperty(RDF.type,
            model.createResource(SDA_NS + "UseCaseRequirement"));
        if (!exists) {
            return ServiceMatchResult.noMatch("Unknown requirement: " + requirementId);
        }

        boolean hasCapability = reqResource.hasProperty(
            model.createProperty(SDA_NS + "mapsToCapability"));
        if (hasCapability) {
            return matchGeneric(requirementId);
        }
        // Legacy dynamic path: requirement described only by constraints
        // (protocol / accessMode / recall / latency / SIL / zone / determinism)
        // rather than a capability — match by those inline constraints.
        return matchInline(extractRequirementParams(reqResource), requirementId);
    }

    private ServiceMatchResult matchGeneric(String requirementId) {
        try {
            String queryStr = sparqlQueryLoader.loadQuery(GENERIC_QUERY)
                .replace("__REQ_ID__", requirementId);
            Query query = QueryFactory.create(queryStr);
            Model model = ontologyModelLoader.getModel();

            try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
                ResultSet rs = qe.execSelect();
                if (!rs.hasNext()) {
                    return ServiceMatchResult.noMatch(
                        "No service matched requirement " + requirementId);
                }

                QuerySolution sol = rs.next();
                ServiceMatchResult result = extractMatchResult(sol, requirementId);
                if (result.matched()) {
                    rDeployService.execute(requirementId, result.serviceName(),
                        result.bindingLabel(), result.connector(),
                        result.deploymentContext());
                    String lifecycleStatus = rDeployService.getLifecycleStatus(requirementId);
                    return new ServiceMatchResult(
                        result.matched(), result.serviceName(), result.containerImage(),
                        result.capabilityName(), result.skillName(), result.protocol(), result.accessMode(),
                        result.bindingLabel(), result.connector(), result.deploymentContext(),
                        result.recall(), result.latencyMs(), result.explanation(),
                        lifecycleStatus, requirementId,
                        result.safetyIntegrityLevel(), result.determinismClass(),
                        result.networkZone());
                }
                return result;
            }
        } catch (SparqlExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new SparqlExecutionException(
                "Failed to execute generic matching query for " + requirementId, e);
        }
    }

    /**
     * Matches services using inline parameters (Mode B).
     * Builds a dynamic SPARQL query from the provided parameters map.
     *
     * @param parameters key-value pairs for filtering: "protocol", "accessMode",
     *                   "minRecall", "maxLatency", "onPlantNetwork", "networkZone",
     *                   "safetyIntegrityLevel", "determinismClass", "functionClass",
     *                   "engineeringUnit"
     * @return a {@link ServiceMatchResult} with matched service details
     */
    public ServiceMatchResult matchInline(Map<String, String> parameters) {
        return matchInline(parameters, null);
    }

    private ServiceMatchResult matchInline(Map<String, String> parameters, String requirementId) {
        if (parameters == null) {
            parameters = Map.of();
        }
        if (parameters.isEmpty()) {
            log.info("No explicit matching parameters provided - using defaults");
        }

        String protocol = parameters.getOrDefault("protocol", "OPC-UA");
        String accessMode = parameters.getOrDefault("accessMode", "read-only");
        double minRecall = Double.parseDouble(
            parameters.getOrDefault("minRecall", "0.90"));
        int maxLatency = Integer.parseInt(
            parameters.getOrDefault("maxLatency", "60"));
        String onPlantNetwork = parameters.getOrDefault("onPlantNetwork", "true");
        String safetyIntegrityLevel = parameters.getOrDefault("safetyIntegrityLevel", null);
        String determinismClass = parameters.getOrDefault("determinismClass", null);
        String networkZone = parameters.getOrDefault("networkZone", null);

        String queryStr = buildInlineQuery(protocol, accessMode, minRecall,
            maxLatency, onPlantNetwork, safetyIntegrityLevel, determinismClass,
            networkZone);

        try {
            Model model = ontologyModelLoader.getModel();
            Query query = QueryFactory.create(queryStr);

            try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
                ResultSet rs = qe.execSelect();
                if (!rs.hasNext()) {
                    return ServiceMatchResult.noMatch(
                        "No service matched inline parameters: protocol=" + protocol
                            + ", accessMode=" + accessMode);
                }

                QuerySolution sol = rs.next();
                return extractMatchResult(sol, requirementId);
            }
        } catch (SparqlExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new SparqlExecutionException(
                "Failed to execute inline matching query", e);
        }
    }

    private Map<String, String> extractRequirementParams(Resource reqResource) {
        Model model = ontologyModelLoader.getModel();
        Map<String, String> params = new HashMap<>();
        putIfPresent(reqResource, "protocol", "protocol", params);
        putIfPresent(reqResource, "accessMode", "accessMode", params);
        putIfPresent(reqResource, "safetyIntegrityLevel", "safetyIntegrityLevel", params);
        putIfPresent(reqResource, "networkZone", "networkZone", params);
        putIfPresent(reqResource, "determinismClass", "determinismClass", params);

        Statement minR = reqResource.getProperty(model.createProperty(SDA_NS + "minRecall"));
        if (minR != null && minR.getObject().isLiteral()) {
            params.put("minRecall", minR.getLiteral().getString());
        }
        Statement maxL = reqResource.getProperty(model.createProperty(SDA_NS + "maxLatency"));
        if (maxL != null && maxL.getObject().isLiteral()) {
            params.put("maxLatency", maxL.getLiteral().getString());
        }
        return params;
    }

    private void putIfPresent(Resource resource, String ontologyProp, String paramKey,
                              Map<String, String> params) {
        Statement stmt = resource.getProperty(
            ontologyModelLoader.getModel().createProperty(SDA_NS + ontologyProp));
        if (stmt != null && stmt.getObject().isLiteral()) {
            params.put(paramKey, stmt.getObject().asLiteral().getString());
        }
    }

    private String buildInlineQuery(String protocol, String accessMode,
                                    double minRecall, int maxLatency,
                                    String onPlantNetwork,
                                    String safetyIntegrityLevel,
                                    String determinismClass,
                                    String networkZone) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX sda:  <http://sda-platform.org/ontology#>
            PREFIX :     <http://sda-platform.org/instance#>

            SELECT ?capability ?skill ?service ?binding ?protocol ?accessMode
                   ?recall ?latency ?label ?capLabel
                   ?connector ?connectorLabel ?deploy ?deployLabel
                   ?safetyIntegrityLevel ?determinismClass ?networkZone
            WHERE {
                ?service      a                  sda:Service ;
                             rdfs:label          ?label ;
                             sda:reportedRecall  ?recall ;
                             sda:reportedLatency ?latency .

                ?service      sda:hasBinding    ?binding .
                ?binding      sda:protocol        ?protocol ;
                               sda:accessMode      ?accessMode .

                ?service      sda:hasConnector   ?connector .

                ?connector    rdfs:label          ?connectorLabel .

                OPTIONAL {
                    ?capability  sda:realisedBy     ?skill .
                    ?skill       sda:isImplementedBy ?service .
                    ?capability  rdfs:label          ?capLabel .
                }

                FILTER (?protocol = "%s")
                FILTER (?accessMode = "%s")
                FILTER (?recall >= %s)
                FILTER (?latency <= %d)

                ?service      sda:isDeployedBy   ?deploy .
                ?deploy       rdfs:label          ?deployLabel ;
                               sda:onPlantNetwork  ?onPlantNetwork .
                FILTER (?onPlantNetwork = %s)
            """.formatted(protocol, accessMode, minRecall, maxLatency, onPlantNetwork));

        if (safetyIntegrityLevel != null) {
            sb.append("                ?deploy sda:contextConstrainedBy ?safetyClass .\n");
            sb.append("                ?safetyClass sda:safetyIntegrityLevel ?safetyIntegrityLevel .\n");
            sb.append("                FILTER (?safetyIntegrityLevel = \"%s\")\n".formatted(safetyIntegrityLevel));
        } else {
            sb.append("                OPTIONAL {\n");
            sb.append("                    ?deploy sda:contextConstrainedBy ?safetyClass .\n");
            sb.append("                    ?safetyClass sda:safetyIntegrityLevel ?safetyIntegrityLevel\n");
            sb.append("                }\n");
        }
        if (determinismClass != null) {
            sb.append("                ?deploy sda:determinismClass ?determinismClass .\n");
            sb.append("                FILTER (?determinismClass = \"%s\")\n".formatted(determinismClass));
        } else {
            sb.append("                OPTIONAL { ?deploy sda:determinismClass ?determinismClass }\n");
        }
        if (networkZone != null) {
            sb.append("                ?deploy sda:locatedInZone ?zone .\n");
            sb.append("                ?zone rdfs:label ?networkZone .\n");
            sb.append("                FILTER (?networkZone = \"%s\")\n".formatted(networkZone));
        } else {
            sb.append("                OPTIONAL {\n");
            sb.append("                    ?deploy sda:locatedInZone ?zone .\n");
            sb.append("                    ?zone rdfs:label ?networkZone\n");
            sb.append("                }\n");
        }

        sb.append("            }\n");
        sb.append("            ORDER BY DESC(?recall)\n");
        sb.append("            LIMIT 1");

        return sb.toString();
    }

    private ServiceMatchResult extractMatchResult(QuerySolution sol, String requirementId) {
        String protocol = getOptionalLiteral(sol, "protocol");
        String accessMode = getOptionalLiteral(sol, "accessMode");
        double recall = sol.contains("recall") ? sol.getLiteral("recall").getDouble() : 0.0;
        int latency = sol.contains("latency") ? sol.getLiteral("latency").getInt() : 0;

        String serviceName = null;
        String containerImage = null;
        String capabilityName = null;
        if (sol.contains("service") && sol.get("service").isResource()) {
            Resource serviceRes = sol.getResource("service");
            serviceName = getLocalName(serviceRes);
            var labelStmt = serviceRes.getProperty(RDFS.label);
            if (labelStmt != null) {
                serviceName = labelStmt.getString();
            }
        }

        if (sol.contains("capability") && sol.get("capability").isResource()) {
            Resource capRes = sol.getResource("capability");
            var capLabelStmt = capRes.getProperty(RDFS.label);
            if (capLabelStmt != null) {
                capabilityName = capLabelStmt.getString();
            } else {
                capabilityName = getLocalName(capRes);
            }
        }

        String explanation = "Matched service '%s' with %s protocol, recall=%.2f, latency=%dms"
            .formatted(serviceName, protocol, recall, latency);

        String skillName = null;
        if (sol.contains("skill") && sol.get("skill").isResource()) {
            Resource skillRes = sol.getResource("skill");
            var skillLabelStmt = skillRes.getProperty(RDFS.label);
            if (skillLabelStmt != null) {
                skillName = skillLabelStmt.getString();
            } else {
                skillName = getLocalName(skillRes);
            }
        }

        String bindingLabel = null;
        if (sol.contains("binding") && sol.get("binding").isResource()) {
            Resource bindingRes = sol.getResource("binding");
            var bindingLabelStmt = bindingRes.getProperty(RDFS.label);
            if (bindingLabelStmt != null) {
                bindingLabel = bindingLabelStmt.getString();
            } else {
                bindingLabel = getLocalName(bindingRes);
            }
        }

        String connector = getOptionalLiteral(sol, "connectorLabel");
        String deploymentContext = null;
        String deployLabel = getOptionalLiteral(sol, "deployLabel");
        deploymentContext = deployLabel;

        String safetyIntegrityLevel = getOptionalLiteral(sol, "safetyIntegrityLevel");
        String determinismClass = getOptionalLiteral(sol, "determinismClass");
        String networkZone = getOptionalLiteral(sol, "networkZone");

        return new ServiceMatchResult(true, serviceName, containerImage,
            capabilityName, skillName, protocol, accessMode, bindingLabel, connector, deploymentContext,
            recall, latency, explanation, "Defined", requirementId,
            safetyIntegrityLevel, determinismClass, networkZone);
    }

    private static String getLocalName(Resource resource) {
        String uri = resource.getURI();
        if (uri == null) return resource.toString();
        int hash = uri.lastIndexOf('#');
        if (hash >= 0) return uri.substring(hash + 1);
        int slash = uri.lastIndexOf('/');
        if (slash >= 0) return uri.substring(slash + 1);
        return uri;
    }

    private static String getOptionalLiteral(QuerySolution sol, String varName) {
        if (sol.contains(varName) && sol.get(varName).isLiteral()) {
            return sol.getLiteral(varName).getString();
        }
        return null;
    }

    private static String getOptionalResource(QuerySolution sol, String varName) {
        if (sol.contains(varName) && sol.get(varName).isResource()) {
            Resource res = sol.getResource(varName);
            String uri = res.getURI();
            if (uri != null) {
                int hash = uri.lastIndexOf('#');
                return hash >= 0 ? uri.substring(hash + 1) : uri;
            }
        }
        return null;
    }
}
