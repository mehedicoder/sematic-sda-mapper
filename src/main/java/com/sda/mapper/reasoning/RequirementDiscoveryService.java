package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Discovers all {@code sda:Requirement} instances from the loaded ontology model.
 * Used by PipelineView to dynamically populate the requirement filter checkboxes
 * instead of hardcoding requirement IDs.
 */
@Service
public class RequirementDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(RequirementDiscoveryService.class);

    private static final String QUERY_PATH = "queries/discover-requirements.rq";

    private final OntologyModelLoader ontologyModelLoader;
    private final SparqlQueryLoader sparqlQueryLoader;

    public RequirementDiscoveryService(OntologyModelLoader ontologyModelLoader,
                                        SparqlQueryLoader sparqlQueryLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
        this.sparqlQueryLoader = sparqlQueryLoader;
    }

    /**
     * Discovers all requirement IDs from the ontology model.
     *
     * @return list of requirement ID strings (e.g. "REQ-001", "REQ-002", ...)
     */
    public List<String> discoverRequirementIds() {
        List<String> ids = new ArrayList<>();

        if (!ontologyModelLoader.isLoaded()) {
            log.warn("Ontology not loaded — returning empty requirement list");
            return ids;
        }

        try {
            Model model = ontologyModelLoader.getModel();
            String queryStr = sparqlQueryLoader.loadQuery(QUERY_PATH);
            Query query = QueryFactory.create(queryStr);

            try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
                ResultSet rs = qe.execSelect();
                while (rs.hasNext()) {
                    QuerySolution sol = rs.next();
                    if (sol.contains("requirementId") && sol.get("requirementId").isLiteral()) {
                        String id = sol.getLiteral("requirementId").getString();
                        ids.add(id);
                    }
                }
            }

            log.info("Discovered {} requirements from ontology", ids.size());
        } catch (Exception e) {
            log.error("Failed to discover requirements from ontology", e);
        }

        return ids;
    }
}
