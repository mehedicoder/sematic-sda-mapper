package com.sda.mapper.service;

import com.sda.mapper.error.OntologyLoadingException;
import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyDatatypeProperty;
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
import java.util.stream.Stream;

/**
 * Grounded data holder for the ontology visualizer, derived at startup from the
 * v2 TBox ({@code ontology/sda_ontology_v2_structured.ttl}, namespace
 * {@code http://sda-platform.org/ontology#}). The five layers, the 24 numbered
 * owl:Class definitions, the 24 derived subclasses (via {@code rdfs:subClassOf})
 * and the labeled owl:ObjectProperty declarations with their class
 * domain/range are read from the loaded Jena model instead of being hardcoded.
 * Derived subclasses inherit their numbered parent's layer, diagram numbering
 * and declared relations.
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
    private final List<OntologyClass> subClasses;
    private final Map<String, String> parentByName;
    private final List<OntologyRelation> relations;
    private final List<OntologyDatatypeProperty> datatypeProperties;

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
        this.subClasses = deriveSubClasses(model, this.classes);
        this.parentByName = subClasses.stream()
            .collect(Collectors.toUnmodifiableMap(OntologyClass::name, OntologyClass::parent));
        this.relations = deriveRelations(model, this.classes);
        this.datatypeProperties = deriveDatatypeProperties(model, this.classes, this.subClasses);
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
                result.add(new OntologyClass(code, name, layerIdOf(layerLabel), false, null));
            });
        result.sort(Comparator.comparing(OntologyClass::code));
        return List.copyOf(result);
    }

    private static List<OntologyClass> deriveSubClasses(Model model, List<OntologyClass> classes) {
        Map<String, OntologyClass> resolved = new LinkedHashMap<>();
        classes.forEach(c -> resolved.put(c.name(), c));

        List<Statement> subClassStatements = new ArrayList<>();
        model.listStatements(null, RDFS.subClassOf, (RDFNode) null)
            .forEachRemaining(subClassStatements::add);

        List<OntologyClass> result = new ArrayList<>();
        boolean changed;
        do {
            changed = false;
            for (Statement st : subClassStatements) {
                String subName = localName(st.getSubject());
                RDFNode parentNode = st.getObject();
                if (subName == null || !parentNode.isResource()) {
                    continue;
                }
                String parentName = localName(parentNode.asResource());
                if (parentName == null || parentName.equals(subName)
                    || resolved.containsKey(subName)) {
                    continue;
                }
                OntologyClass parent = resolved.get(parentName);
                if (parent == null) {
                    continue;
                }
                resolved.put(subName, new OntologyClass(parent.code(), subName,
                    parent.layerId(), true, parent.name()));
                result.add(resolved.get(subName));
                changed = true;
            }
        } while (changed);

        result.sort(Comparator.comparing(OntologyClass::name));
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
                CORE_PROPERTIES.contains(label) ? "core" : "supporting", inverse, false));
        });
        result.sort(Comparator.comparing(OntologyRelation::label));
        return List.copyOf(result);
    }

    private static List<OntologyDatatypeProperty> deriveDatatypeProperties(
            Model model, List<OntologyClass> classes, List<OntologyClass> subClasses) {
        Set<String> knownClasses = Stream.concat(
                classes.stream(), subClasses.stream())
            .map(OntologyClass::name)
            .collect(Collectors.toSet());
        List<OntologyDatatypeProperty> result = new ArrayList<>();
        model.listStatements(null, RDF.type, OWL.DatatypeProperty).forEachRemaining(st -> {
            Resource property = st.getSubject();
            Resource domain = property.getPropertyResourceValue(RDFS.domain);
            Resource range = property.getPropertyResourceValue(RDFS.range);
            String name = localName(property);
            if (name == null || domain == null || range == null) {
                return;
            }
            String className = localName(domain);
            if (!knownClasses.contains(className)) {
                return;
            }
            String xsdType = "xsd:" + localName(range);
            result.add(new OntologyDatatypeProperty(name, className, xsdType));
        });
        result.sort(Comparator.comparing(OntologyDatatypeProperty::name));
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
     * Returns the numbered ontology classes derived from the {@code :classNumber}
     * annotations of the v2 TBox. Derived subclasses are excluded here and are
     * exposed via {@link #getSubClasses()} and {@link #getSubClassesOf(String)}.
     *
     * @return all 24 numbered classes in diagram order
     */
    public List<OntologyClass> getClasses() {
        return classes;
    }

    /**
     * Returns all derived subclasses grouped out of the {@code rdfs:subClassOf}
     * hierarchy of the v2 TBox.
     *
     * @return all derived subclasses sorted by name
     */
    public List<OntologyClass> getSubClasses() {
        return subClasses;
    }

    /**
     * Returns the derived subclasses of the given numbered parent.
     *
     * @param parentName name of the parent class (e.g. {@code "Skill"})
     * @return derived subclasses sorted by name, or an empty list if the parent has none
     */
    public List<OntologyClass> getSubClassesOf(String parentName) {
        return subClasses.stream()
            .filter(c -> parentName.equals(c.parent()))
            .sorted(Comparator.comparing(OntologyClass::name))
            .toList();
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
     * Returns every datatype property derived from the {@code owl:DatatypeProperty}
     * declarations of the v2 TBox with a known-class {@code rdfs:domain}.
     *
     * @return all datatype properties sorted by property name
     */
    public List<OntologyDatatypeProperty> getDatatypeProperties() {
        return datatypeProperties;
    }

    /**
     * Returns the datatype properties declared on the given class, derived from
     * the {@code owl:DatatypeProperty} declarations with a known-class
     * {@code rdfs:domain}.
     *
     * @param className class name (e.g. {@code "Skill"} or {@code "AASSkill"})
     * @return datatype properties of the class sorted by property name, or an
     *         empty list for unknown classes
     */
    public List<OntologyDatatypeProperty> getDatatypeProperties(String className) {
        return datatypeProperties.stream()
            .filter(p -> p.className().equals(className))
            .toList();
    }

    /**
     * Returns the relations that originate from the given class. Derived classes
     * inherit the relations declared on their parent (walking the
     * {@code subClassOf} chain); inherited relations are marked
     * {@code inherited = true}.
     *
     * @param className class name (e.g. {@code "Service"} or {@code "AASSkill"})
     * @return outgoing relations sorted by property name
     */
    public List<OntologyRelation> getRelationsFrom(String className) {
        List<OntologyRelation> declared = relations.stream()
            .filter(r -> r.from().equals(className))
            .toList();
        return Stream.concat(declared.stream(), inheritedOutgoing(className).stream())
            .sorted(Comparator.comparing(OntologyRelation::label))
            .toList();
    }

    /**
     * Returns the relations that target the given class. Derived classes inherit
     * the relations targeting their parent (walking the {@code subClassOf} chain);
     * inherited relations are marked {@code inherited = true}.
     *
     * @param className class name (e.g. {@code "Service"} or {@code "AASSkill"})
     * @return incoming relations sorted by property name
     */
    public List<OntologyRelation> getRelationsTo(String className) {
        List<OntologyRelation> declared = relations.stream()
            .filter(r -> r.to().equals(className))
            .toList();
        return Stream.concat(declared.stream(), inheritedIncoming(className).stream())
            .sorted(Comparator.comparing(OntologyRelation::label))
            .toList();
    }

    private List<OntologyRelation> inheritedOutgoing(String className) {
        String parent = parentByName.get(className);
        if (parent == null) {
            return List.of();
        }
        List<OntologyRelation> direct = relations.stream()
            .filter(r -> r.from().equals(parent))
            .map(r -> new OntologyRelation(className, r.to(), r.label(), r.kind(),
                r.inverse(), true))
            .toList();
        return Stream.concat(direct.stream(), inheritedOutgoing(parent).stream()).toList();
    }

    private List<OntologyRelation> inheritedIncoming(String className) {
        String parent = parentByName.get(className);
        if (parent == null) {
            return List.of();
        }
        List<OntologyRelation> direct = relations.stream()
            .filter(r -> r.to().equals(parent))
            .map(r -> new OntologyRelation(r.from(), className, r.label(), r.kind(),
                r.inverse(), true))
            .toList();
        return Stream.concat(direct.stream(), inheritedIncoming(parent).stream()).toList();
    }

    /**
     * Finds a class by its exact name, searching numbered classes first and then
     * derived subclasses.
     *
     * @param name class name (e.g. {@code "Service"} or {@code "AASSkill"})
     * @return the matching class, or empty if unknown
     */
    public Optional<OntologyClass> getClassByName(String name) {
        Optional<OntologyClass> found = classes.stream()
            .filter(c -> c.name().equals(name))
            .findFirst();
        if (found.isPresent()) {
            return found;
        }
        return subClasses.stream()
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
