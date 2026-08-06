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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Executes SPARQL matching queries against the CSS-SDA ontology model
 * to find services that satisfy given requirements.
 * <p>
 * Supports two matching modes:
 * <ul>
 *   <li><b>Mode A</b> — match by predefined requirement ID ({@code REQ-001}, {@code REQ-002}, {@code REQ-003})</li>
 *   <li><b>Mode B</b> — match by inline parameters (protocol, accessMode, recall threshold, etc.)</li>
 * </ul>
 */
@Service
public class SemanticReasonerService {

    private static final Logger log = LoggerFactory.getLogger(SemanticReasonerService.class);

    private static final String INST_NS = "http://www.example.org/instance#";
    private static final String SDA_NS = "http://sda-platform.org/ontology#";

    private static final Map<String, String> REQUIREMENT_QUERY_MAP = Map.of(
        "REQ-001", "queries/match-service.rq",
        "REQ-002", "queries/match-service-req002.rq",
        "REQ-003", "queries/match-service-req003.rq"
    );

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
     * <p>
     * For predefined requirements (REQ-001, REQ-002, REQ-003), uses dedicated
     * SPARQL queries (Mode A). For dynamically-created requirements, extracts
     * parameters from the ontology model and falls back to inline matching
     * (Mode B).
     *
     * @param requirementId the requirement ID (e.g. "REQ-001" or "REQ-a1b2c3d4")
     * @return a {@link ServiceMatchResult} with all matched service details
     */
    public ServiceMatchResult matchByRequirementId(String requirementId) {
        if (requirementId == null || requirementId.isBlank()) {
            return ServiceMatchResult.noMatch("UseCaseRequirement ID is null or blank");
        }

        // Mode A: predefined requirement with dedicated SPARQL query
        String queryPath = REQUIREMENT_QUERY_MAP.get(requirementId.toUpperCase());
        if (queryPath != null) {
            return matchPredefinedRequirement(requirementId, queryPath);
        }

        // Mode B fallback: extract parameters from ontology and use inline matching
        return matchDynamicRequirement(requirementId);
    }

    private ServiceMatchResult matchPredefinedRequirement(String requirementId, String queryPath) {
        try {
            Model model = ontologyModelLoader.getModel();
            String queryStr = sparqlQueryLoader.loadQuery(queryPath);
            Query query = QueryFactory.create(queryStr);

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
                "Failed to execute matching query for " + requirementId, e);
        }
    }

    /**
     * Matches a dynamically-created requirement by extracting its parameters
     * from the ontology model and using inline matching (Mode B).
     */
    private ServiceMatchResult matchDynamicRequirement(String requirementId) {
        Model model = ontologyModelLoader.getModel();
        String resourceUri = INST_NS + requirementId;
        Resource reqResource = model.getResource(resourceUri);

        if (!reqResource.hasProperty(
                org.apache.jena.vocabulary.RDF.type,
                model.createResource(SDA_NS + "UseCaseRequirement"))) {
            return ServiceMatchResult.noMatch("Unknown requirement: " + requirementId);
        }

        Map<String, String> params = extractRequirementParams(model, reqResource);

        log.info("Dynamic matching for {}: params={}", requirementId, params);
        ServiceMatchResult result = matchInline(params);

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
        return new ServiceMatchResult(
            false, null, null, null, null, null, null, null, null, null,
            0, 0, "No service matched dynamic requirement " + requirementId
                + " with params: " + params,
            null, requirementId, null, null, null);
    }

    /**
     * Extracts matching parameters from a requirement resource in the ontology.
     * Reads sda:protocol, sda:accessMode, sda:minRecall, sda:maxLatency,
     * sda:networkZone, sda:determinismClass, sda:safetyIntegrityLevel.
     */
    private Map<String, String> extractRequirementParams(Model model, Resource reqResource) {
        Map<String, String> params = new HashMap<>();
        putIfPresent(model, reqResource, params, "protocol", "protocol");
        putIfPresent(model, reqResource, params, "accessMode", "accessMode");
        putIfPresent(model, reqResource, params, "determinismClass", "determinismClass");

        // Extract recall/latency from constraints if present
        extractConstraintParam(model, reqResource, params, "latency", "maxLatency");
        extractConstraintParam(model, reqResource, params, "recall", "minRecall");

        return params;
    }

    private void putIfPresent(Model model, Resource resource,
                               Map<String, String> params, String ontologyProp, String paramKey) {
        Statement stmt = resource.getProperty(model.createProperty(SDA_NS + ontologyProp));
        if (stmt != null && stmt.getObject().isLiteral()) {
            params.put(paramKey, stmt.getObject().asLiteral().getString());
        }
    }

    private void extractConstraintParam(Model model, Resource reqResource,
                                         Map<String, String> params, String constraintType, String paramKey) {
        var constraints = reqResource.listProperties(
            model.createProperty(SDA_NS + "hasConstraint")).toList();
        for (var stmt : constraints) {
            if (stmt.getObject().isResource()) {
                Resource constraint = stmt.getObject().asResource();
                Statement typeStmt = constraint.getProperty(
                    model.createProperty(SDA_NS + "constraintType"));
                if (typeStmt != null && constraintType.equals(typeStmt.getString())) {
                    // v2: constraints carry their numeric threshold inside the
                    // expression (e.g. "reportedLatency <= 60"); extract the
                    // trailing number.
                    Statement exprStmt = constraint.getProperty(
                        model.createProperty(SDA_NS + "expression"));
                    if (exprStmt != null && exprStmt.getObject().isLiteral()) {
                        String expression = exprStmt.getObject().asLiteral().getString();
                        String number = expression.replaceAll(".*?(-?\\d+(\\.\\d+)?)\\s*$", "$1");
                        if (!number.equals(expression)) {
                            params.put(paramKey, number);
                        }
                    }
                }
            }
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
                return extractMatchResult(sol, null);
            }
        } catch (SparqlExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new SparqlExecutionException(
                "Failed to execute inline matching query", e);
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
            PREFIX :     <http://www.example.org/instance#>

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

        // Extract service name from ?service URI local name and rdfs:label
        String serviceName = null;
        String containerImage = null;
        String capabilityName = null;
        if (sol.contains("service") && sol.get("service").isResource()) {
            org.apache.jena.rdf.model.Resource serviceRes = sol.getResource("service");
            serviceName = getLocalName(serviceRes);
            // Look up rdfs:label from the model
            var labelStmt = serviceRes.getProperty(
                org.apache.jena.vocabulary.RDFS.label);
            if (labelStmt != null) {
                serviceName = labelStmt.getString();
            }
        }

        // Extract capability name from ?capability if present
        if (sol.contains("capability") && sol.get("capability").isResource()) {
            org.apache.jena.rdf.model.Resource capRes = sol.getResource("capability");
            var capLabelStmt = capRes.getProperty(
                org.apache.jena.vocabulary.RDFS.label);
            if (capLabelStmt != null) {
                capabilityName = capLabelStmt.getString();
            } else {
                capabilityName = getLocalName(capRes);
            }
        }

        String explanation = "Matched service '%s' with %s protocol, recall=%.2f, latency=%dms"
            .formatted(serviceName, protocol, recall, latency);

        // Extract skill name from ?skill if present
        String skillName = null;
        if (sol.contains("skill") && sol.get("skill").isResource()) {
            org.apache.jena.rdf.model.Resource skillRes = sol.getResource("skill");
            var skillLabelStmt = skillRes.getProperty(
                org.apache.jena.vocabulary.RDFS.label);
            if (skillLabelStmt != null) {
                skillName = skillLabelStmt.getString();
            } else {
                skillName = getLocalName(skillRes);
            }
        }

        // Extract binding label from ?binding
        String bindingLabel = null;
        if (sol.contains("binding") && sol.get("binding").isResource()) {
            org.apache.jena.rdf.model.Resource bindingRes = sol.getResource("binding");
            var bindingLabelStmt = bindingRes.getProperty(
                org.apache.jena.vocabulary.RDFS.label);
            if (bindingLabelStmt != null) {
                bindingLabel = bindingLabelStmt.getString();
            } else {
                bindingLabel = getLocalName(bindingRes);
            }
        }

        // Extract connector label from ?connectorLabel
        String connector = getOptionalLiteral(sol, "connectorLabel");

        // Extract deployment context label from ?deployLabel
        String deploymentContext = getOptionalLiteral(sol, "deployLabel");

        // Extract deployment context structured properties
        String safetyIntegrityLevel = getOptionalLiteral(sol, "safetyIntegrityLevel");
        String determinismClass = getOptionalLiteral(sol, "determinismClass");
        String networkZone = getOptionalLiteral(sol, "networkZone");

        return new ServiceMatchResult(true, serviceName, containerImage,
            capabilityName, skillName, protocol, accessMode, bindingLabel, connector, deploymentContext,
            recall, latency, explanation, "Defined", requirementId,
            safetyIntegrityLevel, determinismClass, networkZone);
    }

    private static String getLocalName(org.apache.jena.rdf.model.Resource resource) {
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
