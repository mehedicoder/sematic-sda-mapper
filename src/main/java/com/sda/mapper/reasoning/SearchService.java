package com.sda.mapper.reasoning;

import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service for searching ontology entities by text query.
 * Uses Apache Jena to search across rdfs:label, rdfs:comment, and data properties.
 */
@Service
public class SearchService {

    private final OntologyModelLoader ontologyModelLoader;

    public SearchService(OntologyModelLoader ontologyModelLoader) {
        this.ontologyModelLoader = ontologyModelLoader;
    }

    /**
     * Search ontology entities by text query and optional entity type filter.
     *
     * @param query      text to match against labels and descriptions (case-insensitive contains)
     * @param entityType optional filter — null means search all types
     * @return matching results sorted by similarity score descending
     */
    public List<SearchResult> search(String query, String entityType) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Model model = ontologyModelLoader.getModel();
        String lowerQuery = query.toLowerCase();
        List<SearchResult> results = new ArrayList<>();

        var subjects = model.listSubjects().toList();
        for (Resource resource : subjects) {
            if (!resource.isURIResource()) continue;

            String name = getLabel(resource);
            String type = getEntityType(resource, model);
            String description = getDescription(resource, model);
            String localName = getLocalName(resource);

            if (entityType != null && !entityType.isBlank() && !type.equalsIgnoreCase(entityType)) {
                continue;
            }

            double score = calculateScore(lowerQuery, name, description, localName);
            if (score > 0.0) {
                results.add(new SearchResult(name, type, description, localName, score));
            }
        }

        results.sort(Comparator.comparingDouble(SearchResult::similarityScore).reversed());
        return results;
    }

    private double calculateScore(String lowerQuery, String name,
                                   String description, String localName) {
        double score = 0.0;
        String lowerName = name.toLowerCase();
        String lowerDesc = description.toLowerCase();
        String lowerLocal = localName.toLowerCase();

        if (lowerName.contains(lowerQuery)) {
            score += 0.5;
            if (lowerName.startsWith(lowerQuery)) score += 0.2;
            if (lowerName.equals(lowerQuery)) score += 0.3;
        }
        if (lowerLocal.contains(lowerQuery)) {
            score += 0.3;
        }
        if (lowerDesc.contains(lowerQuery)) {
            score += 0.2;
        }

        return Math.min(score, 1.0);
    }

    private String getLabel(Resource resource) {
        Statement stmt = resource.getProperty(RDFS.label);
        if (stmt != null && stmt.getString() != null) {
            return stmt.getString();
        }
        return getLocalName(resource);
    }

    private String getEntityType(Resource resource, Model model) {
        var types = resource.listProperties(RDF.type).toList();
        for (Statement stmt : types) {
            if (stmt.getObject().isResource()) {
                return getLocalName(stmt.getObject().asResource());
            }
        }
        return "Resource";
    }

    private String getDescription(Resource resource, Model model) {
        Statement stmt = resource.getProperty(RDFS.comment);
        if (stmt != null && stmt.getString() != null) {
            return stmt.getString();
        }
        Statement descStmt = resource.getProperty(
                model.createProperty("http://www.example.org/sda#description"));
        if (descStmt != null && descStmt.getString() != null) {
            return descStmt.getString();
        }
        return "";
    }

    private String getLocalName(Resource resource) {
        String uri = resource.getURI();
        if (uri == null) return resource.toString();
        int hash = uri.lastIndexOf('#');
        if (hash >= 0) return uri.substring(hash + 1);
        int slash = uri.lastIndexOf('/');
        if (slash >= 0) return uri.substring(slash + 1);
        return uri;
    }
}
