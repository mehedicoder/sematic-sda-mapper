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
    private static final String SDA_NS = "http://www.example.org/sda#";
    private static final String CSS_NS = "http://www.example.org/css#";

    private static final Map<String, String> REQUIREMENT_QUERY_MAP = Map.of(
        "REQ-001", "queries/match-service.rq",
        "REQ-002", "queries/match-service-req002.rq",
        "REQ-003", "queries/match-service-req003.rq"
    );

    private final OntologyModelLoader ontologyModelLoader;
    private final SparqlQueryLoader sparqlQueryLoader;

    public SemanticReasonerService(OntologyModelLoader ontologyModelLoader,
                                   SparqlQueryLoader sparqlQueryLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
        this.sparqlQueryLoader = sparqlQueryLoader;
    }

    /**
     * Matches a predefined requirement against the ontology.
     * Loads the appropriate SPARQL query for the requirement ID
     * and executes it against the loaded model.
     *
     * @param requirementId the requirement ID (e.g. "REQ-001")
     * @return a {@link ServiceMatchResult} with all matched service details
     */
    public ServiceMatchResult matchByRequirementId(String requirementId) {
        if (requirementId == null || requirementId.isBlank()) {
            return ServiceMatchResult.noMatch("Requirement ID is null or blank");
        }

        String queryPath = REQUIREMENT_QUERY_MAP.get(requirementId.toUpperCase());
        if (queryPath == null) {
            return ServiceMatchResult.noMatch("Unknown requirement ID: " + requirementId);
        }

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
                return extractMatchResult(sol);
            }
        } catch (SparqlExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new SparqlExecutionException(
                "Failed to execute matching query for " + requirementId, e);
        }
    }

    /**
     * Matches services using inline parameters (Mode B).
     * Builds a dynamic SPARQL query from the provided parameters map.
     *
     * @param parameters key-value pairs for filtering: "protocol", "accessMode",
     *                   "minRecall", "maxLatency", "onPlantNetwork", "networkZone"
     * @return a {@link ServiceMatchResult} with matched service details
     */
    public ServiceMatchResult matchInline(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return ServiceMatchResult.noMatch("No matching parameters provided");
        }

        String protocol = parameters.getOrDefault("protocol", "OPC-UA");
        String accessMode = parameters.getOrDefault("accessMode", "read-only");
        double minRecall = Double.parseDouble(
            parameters.getOrDefault("minRecall", "0.90"));
        int maxLatency = Integer.parseInt(
            parameters.getOrDefault("maxLatency", "60"));
        String onPlantNetwork = parameters.getOrDefault("onPlantNetwork", "true");

        String queryStr = buildInlineQuery(protocol, accessMode, minRecall,
            maxLatency, onPlantNetwork);

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
                return extractMatchResult(sol);
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
                                     String onPlantNetwork) {
        return """
            PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            PREFIX css:  <http://www.example.org/css#>
            PREFIX sda:  <http://www.example.org/sda#>
            PREFIX :     <http://www.example.org/instance#>

            SELECT ?capability ?skill ?service ?binding ?protocol ?accessMode
                   ?recall ?latency ?image ?label ?capLabel
            WHERE {
                ?service      a                  sda:Service ;
                             rdfs:label          ?label ;
                             sda:reportedRecall  ?recall ;
                             sda:reportedLatency ?latency .

                ?service      sda:usesBinding    ?binding .
                ?binding      sda:protocol        ?protocol ;
                              sda:accessMode      ?accessMode .

                OPTIONAL { ?service sda:image ?image }
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
                ?deploy       sda:onPlantNetwork  ?onPlantNetwork .
                FILTER (?onPlantNetwork = %s)
            }
            ORDER BY DESC(?recall)
            LIMIT 1
            """.formatted(protocol, accessMode, minRecall, maxLatency, onPlantNetwork);
    }

    private ServiceMatchResult extractMatchResult(QuerySolution sol) {
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
            // Look up sda:image from the model
            var imageStmt = serviceRes.getProperty(
                ontologyModelLoader.getModel().createProperty(SDA_NS + "image"));
            if (imageStmt != null) {
                containerImage = imageStmt.getString();
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

        return new ServiceMatchResult(true, serviceName, containerImage,
            capabilityName, protocol, accessMode, null, null,
            recall, latency, explanation);
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
