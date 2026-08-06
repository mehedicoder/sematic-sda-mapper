package com.sda.mapper.service;

import com.sda.mapper.error.OntologyLoadingException;
import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyLayer;
import com.sda.mapper.model.OntologyRelation;
import com.sda.mapper.ontology.OntologyModelLoader;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Grounded data holder for the ontology visualizer, derived at startup from the
 * v2 TBox ({@code ontology/sda_ontology_v2_structured.ttl}, namespace
 * {@code http://sda-platform.org/ontology#}). The five layers, the 24 numbered
 * owl:Class definitions and the labeled owl:ObjectProperty declarations with
 * their class domain/range are read from the loaded Jena model instead of being
 * hardcoded.
 */
@Service
public class OntologyDataService {

    private static final String SDA_NS = "http://sda-platform.org/ontology#";
    private static final String DEFAULT_ONTOLOGY_PATH = "ontology/sda_ontology_v2_structured.ttl";

    /**
     * Display order and fixed palette for the five CSS-SDA layers.
     */
    private static final List<String> LAYER_ORDER = List.of("L1", "L2", "L2b", "L3", "L4");
    private static final Map<String, String> LAYER_COLORS = Map.of(
        "L1", "#f59e0b",
        "L2", "#10b981",
        "L2b", "#8b5cf6",
        "L3", "#06b6d4",
        "L4", "#ef4444"
    );

    /**
     * Object properties on the R1–R3 / R-Deploy / R-Verify matching chain
     * (paper Steps 4–5); everything else is classified as {@code supporting}.
     */
    private static final Set<String> CORE_PROPERTIES = Set.of(
        "mapsToCapability", "realisedBy", "isImplementedBy", "exposedAs", "mapsTo",
        "hasParameter", "hasConstraint", "constrains", "verifiedBy", "satisfies",
        "requiresConcept", "mapsVariable", "describedBy",
        "usesBinding", "usesConnector", "hasBinding", "hasConnector",
        "isDeployedBy", "deploysService", "mapsToDeploymentContext",
        "contextConstrainedBy", "locatedInZone"
    );

    private final List<OntologyLayer> layers;
    private final List<OntologyClass> classes;
    private final List<OntologyRelation> relations;

    /**
     * Creates the data service over the application's loaded ontology model.
     *
     * @param loader the ontology model loader (already loaded at startup)
     */
    @Autowired
    public OntologyDataService(OntologyModelLoader loader) {
        this(loader.getModel());
    }

    /**
     * Creates the data service over the default v2 TBox for standalone use.
     */
    public OntologyDataService() {
        this(loadDefaultModel());
    }

    private OntologyDataService(Model model) {
        this.layers = deriveLayers(model);
        this.classes = deriveClasses(model);
        this.relations = deriveRelations(model, this.classes);
    }

    private static Model loadDefaultModel() {
        Model model = ModelFactory.createDefaultModel();
        ClassPathResource resource = new ClassPathResource(DEFAULT_ONTOLOGY_PATH);
        try (InputStream in = resource.getInputStream()) {
            model.read(in, null, "TTL");
        } catch (IOException e) {
            throw new OntologyLoadingException(
                "Failed to read ontology file: " + DEFAULT_ONTOLOGY_PATH, e);
        }
        return model;
    }

    private static List<OntologyLayer> deriveLayers(Model model) {
        Map<String, String> labelById = new LinkedHashMap<>();
        model.listStatements(null, model.createProperty(SDA_NS + "ontologyLayer"), (RDFNode) null)
            .forEachRemaining(st -> {
                String label = st.getObject().asLiteral().getString();
                labelById.putIfAbsent(layerIdOf(label), label);
            });
        List<OntologyLayer> result = new ArrayList<>();
        for (String id : LAYER_ORDER) {
            String label = labelById.getOrDefault(id, id);
            result.add(new OntologyLayer(id, label, LAYER_COLORS.get(id),
                id.toLowerCase(Locale.ROOT)));
        }
        return List.copyOf(result);
    }

    private static List<OntologyClass> deriveClasses(Model model) {
        List<OntologyClass> result = new ArrayList<>();
        model.listStatements(null, model.createProperty(SDA_NS + "classNumber"), (RDFNode) null)
            .forEachRemaining(st -> {
                Resource cls = st.getSubject();
                String code = st.getObject().asLiteral().getString();
                String name = cls.getRequiredProperty(RDFS.label)
                    .getObject().asLiteral().getString();
                Statement layerStmt = cls.getProperty(
                    model.createProperty(SDA_NS + "ontologyLayer"));
                String layerLabel = layerStmt == null
                    ? "" : layerStmt.getObject().asLiteral().getString();
                result.add(new OntologyClass(code, name, layerIdOf(layerLabel)));
            });
        result.sort(Comparator.comparing(OntologyClass::code));
        return List.copyOf(result);
    }

    private static List<OntologyRelation> deriveRelations(Model model, List<OntologyClass> classes) {
        Set<String> knownClasses = classes.stream()
            .map(OntologyClass::name)
            .collect(Collectors.toSet());
        List<OntologyRelation> result = new ArrayList<>();
        model.listStatements(null, RDF.type, OWL.ObjectProperty).forEachRemaining(st -> {
            Resource property = st.getSubject();
            Statement labelStmt = property.getProperty(RDFS.label);
            if (labelStmt == null) {
                return;
            }
            String label = labelStmt.getObject().asLiteral().getString();
            Resource domain = property.getPropertyResourceValue(RDFS.domain);
            Resource range = property.getPropertyResourceValue(RDFS.range);
            if (domain == null || range == null) {
                return;
            }
            String from = localName(domain);
            String to = localName(range);
            if (!knownClasses.contains(from) || !knownClasses.contains(to)) {
                return;
            }
            String inverse = null;
            Resource inverseRes = property.getPropertyResourceValue(OWL.inverseOf);
            if (inverseRes != null) {
                inverse = localName(inverseRes);
            }
            result.add(new OntologyRelation(from, to, label,
                CORE_PROPERTIES.contains(label) ? "core" : "supporting", inverse));
        });
        result.sort(Comparator.comparing(OntologyRelation::label));
        return List.copyOf(result);
    }

    private static String localName(Resource resource) {
        String uri = resource.getURI();
        if (uri == null) {
            return null;
        }
        int idx = uri.lastIndexOf('#');
        return idx >= 0 ? uri.substring(idx + 1) : uri;
    }

    /**
     * Maps a {@code :ontologyLayer} annotation label to a stable layer id,
     * e.g. {@code "Layer 2b — Platform Infrastructure"} to {@code "L2b"}.
     */
    private static String layerIdOf(String layerLabel) {
        var matcher = java.util.regex.Pattern.compile("Layer\\s+([0-9A-Za-z]+)").matcher(layerLabel);
        return matcher.find() ? "L" + matcher.group(1) : "Unknown";
    }

    /**
     * Returns all ontology layers in display order.
     *
     * @return the five CSS-SDA layers
     */
    public List<OntologyLayer> getLayers() {
        return layers;
    }

    /**
     * Returns all ontology classes derived from the {@code :classNumber}
     * annotations of the v2 TBox.
     *
     * @return all 24 numbered classes in diagram order
     */
    public List<OntologyClass> getClasses() {
        return classes;
    }

    /**
     * Returns the classes that belong to the given layer.
     *
     * @param layerId layer identifier (e.g. {@code "L1"})
     * @return classes of that layer in diagram order, or an empty list for unknown layers
     */
    public List<OntologyClass> getClassesByLayer(String layerId) {
        return classes.stream()
            .filter(c -> c.layerId().equals(layerId))
            .toList();
    }

    /**
     * Returns all ontology relations derived from the labeled
     * {@code owl:ObjectProperty} declarations of the v2 TBox.
     *
     * @return all labeled object property declarations with class domain/range
     */
    public List<OntologyRelation> getRelations() {
        return relations;
    }

    /**
     * Returns the relations that originate from the given class.
     *
     * @param className class name (e.g. {@code "UseCaseRequirement"})
     * @return outgoing relations sorted by property name
     */
    public List<OntologyRelation> getRelationsFrom(String className) {
        return relations.stream()
            .filter(r -> r.from().equals(className))
            .sorted(Comparator.comparing(OntologyRelation::label))
            .toList();
    }

    /**
     * Returns the relations that target the given class.
     *
     * @param className class name (e.g. {@code "UseCaseRequirement"})
     * @return incoming relations sorted by property name
     */
    public List<OntologyRelation> getRelationsTo(String className) {
        return relations.stream()
            .filter(r -> r.to().equals(className))
            .sorted(Comparator.comparing(OntologyRelation::label))
            .toList();
    }

    /**
     * Finds a class by its exact name.
     *
     * @param name class name (e.g. {@code "Service"})
     * @return the matching class, or empty if unknown
     */
    public Optional<OntologyClass> getClassByName(String name) {
        return classes.stream()
            .filter(c -> c.name().equals(name))
            .findFirst();
    }

    /**
     * Finds a layer by its identifier.
     *
     * @param id layer identifier (e.g. {@code "L2b"})
     * @return the matching layer, or empty if unknown
     */
    public Optional<OntologyLayer> getLayerById(String id) {
        return layers.stream()
            .filter(l -> l.id().equals(id))
            .findFirst();
    }
}
