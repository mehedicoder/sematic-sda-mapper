package com.sda.mapper.ui.catalog;

import com.sda.mapper.ontology.OntologyModelLoader;
import com.sda.mapper.reasoning.SparqlExecutionException;
import com.sda.mapper.reasoning.SparqlQueryLoader;
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
 * Executes a SPARQL query against the CSS-SDA ontology to extract
 * all registered services for the Service Catalog view.
 */
@Service
public class ServiceCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ServiceCatalogService.class);

    private static final String CATALOG_QUERY_PATH = "queries/service-catalog.rq";

    private final OntologyModelLoader ontologyModelLoader;
    private final SparqlQueryLoader sparqlQueryLoader;

    public ServiceCatalogService(OntologyModelLoader ontologyModelLoader,
                                  SparqlQueryLoader sparqlQueryLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
        this.sparqlQueryLoader = sparqlQueryLoader;
    }

    /**
     * Loads all services from the ontology catalog.
     *
     * @return list of {@link ServiceCatalogEntry} for every registered service
     * @throws SparqlExecutionException if the SPARQL query fails
     */
    public List<ServiceCatalogEntry> loadCatalog() {
        Model model = ontologyModelLoader.getModel();
        String queryStr = sparqlQueryLoader.loadQuery(CATALOG_QUERY_PATH);
        Query query = QueryFactory.create(queryStr);

        List<ServiceCatalogEntry> entries = new ArrayList<>();

        try (QueryExecution qe = QueryExecutionFactory.create(query, model)) {
            ResultSet rs = qe.execSelect();
            while (rs.hasNext()) {
                QuerySolution sol = rs.next();
                entries.add(extractEntry(sol));
            }
        }

        log.info("Service catalog loaded: {} entries", entries.size());
        return entries;
    }

    private ServiceCatalogEntry extractEntry(QuerySolution sol) {
        return new ServiceCatalogEntry(
            getString(sol, "serviceLabel"),
            getString(sol, "capabilityLabel"),
            getString(sol, "skillLabel"),
            getString(sol, "protocol"),
            getString(sol, "accessMode"),
            getString(sol, "securityMode"),
            getDouble(sol, "recall"),
            getInt(sol, "latency"),
            getString(sol, "image"),
            getString(sol, "connectorLabel"),
            getString(sol, "informationModel"),
            getString(sol, "deployLabel"),
            getString(sol, "sil"),
            getString(sol, "determinism"),
            getString(sol, "networkZone")
        );
    }

    private static String getString(QuerySolution sol, String var) {
        if (sol.contains(var) && sol.get(var).isLiteral()) {
            return sol.getLiteral(var).getString();
        }
        return null;
    }

    private static double getDouble(QuerySolution sol, String var) {
        if (sol.contains(var) && sol.get(var).isLiteral()) {
            return sol.getLiteral(var).getDouble();
        }
        return 0.0;
    }

    private static int getInt(QuerySolution sol, String var) {
        if (sol.contains(var) && sol.get(var).isLiteral()) {
            return sol.getLiteral(var).getInt();
        }
        return 0;
    }
}
