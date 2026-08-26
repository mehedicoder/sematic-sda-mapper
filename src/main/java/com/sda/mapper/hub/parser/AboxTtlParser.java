package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.AttributeBindings;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.ManualEntity;
import com.sda.mapper.hub.parser.ParsingException;
import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.service.OntologyDataService;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parser for raw SDA ABox Turtle files (service catalogue, asset descriptions,
 * edge-node topology).
 *
 * <p>Unlike the other asset parsers, an ABox TTL is not merely read as opaque
 * triples: each declared individual is projected onto the hub's
 * {@link CanonicalAssetModel} so that coverage, lift, inference and the
 * manifest entity listing reflect the uploaded supply/asset/edge individuals.
 * Subclass types (e.g. {@code OfferedCapability}, {@code AnalyticsService},
 * {@code EdgeContext}) are resolved to their canonical parent class
 * (e.g. {@code Capability}, {@code Service}, {@code DeploymentContext}) using
 * the ontology TBox, so coverage counts the individual under the right class.
 * The triples are also merged into the shared reasoning model by
 * {@code MappingHubService} so the semantic matcher can reason over them.</p>
 */
@Component
public class AboxTtlParser implements CanonicalModelParser {

    private final OntologyDataService ontologyDataService;

    @Autowired
    public AboxTtlParser(OntologyDataService ontologyDataService) {
        this.ontologyDataService = ontologyDataService;
    }

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.ABOX_TTL;
    }

    @Override
    public CanonicalAssetModel parse(InputStream input, String fileName) {
        Model raw = ModelFactory.createDefaultModel();
        try {
            raw.read(input, null, "TTL");
        } catch (Exception e) {
            throw new ParsingException(fileName, "Invalid ABox TTL: " + e.getMessage());
        }
        return project(raw, fileName);
    }

    /**
     * Projects every ABox individual in the given model into a canonical model,
     * resolving subclass types to their canonical parent class.
     *
     * @param raw      the parsed ABox model
     * @param fileName the originating upload file name (recorded for provenance)
     * @return the projected canonical model
     */
    public CanonicalAssetModel project(Model raw, String fileName) {
        CanonicalAssetModel model = new CanonicalAssetModel();
        raw.listSubjectsWithProperty(RDF.type).forEachRemaining(subject -> {
            if (!subject.isURIResource()) {
                return;
            }
            raw.listStatements(subject, RDF.type, (RDFNode) null).forEachRemaining(typeStmt -> {
                if (!typeStmt.getObject().isResource()) {
                    return;
                }
                String canonical = resolveCanonicalClass(typeStmt.getResource());
                if (canonical == null) {
                    return;
                }
                CanonicalEntity entity = toEntity(subject, canonical, fileName);
                if (entity != null) {
                    model.addEntity(entity);
                }
            });
        });
        return model;
    }

    private String resolveCanonicalClass(Resource type) {
        String local = localName(type);
        if (local == null) {
            return null;
        }
        Optional<OntologyClass> found = ontologyDataService.getClassByName(local);
        if (found.isEmpty()) {
            return null;
        }
        OntologyClass current = found.get();
        while (current.derived()) {
            Optional<OntologyClass> parent = ontologyDataService.getClassByName(current.parent());
            if (parent.isEmpty()) {
                break;
            }
            current = parent.get();
        }
        return current.name();
    }

    private CanonicalEntity toEntity(Resource subject, String canonicalClass, String fileName) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        String id = localName(subject);
        String name = id;
        var it = subject.listProperties();
        while (it.hasNext()) {
            Statement statement = it.next();
            if (!statement.getObject().isLiteral()) {
                continue;
            }
            String propertyName = localName(statement.getPredicate());
            if (propertyName == null) {
                continue;
            }
            Literal literal = statement.getLiteral();
            String value = literal.getLexicalForm();
            String hubKey = AttributeBindings.hubKey(propertyName);
            attributes.put(hubKey, value);
            attributes.put(propertyName, value);
            if ("name".equals(hubKey)) {
                name = value;
            }
        }
        attributes.put("id", id);
        attributes.put("name", name);
        return new ManualEntity(canonicalClass, id, name, attributes, fileName);
    }

    private static String localName(Resource resource) {
        String uri = resource.getURI();
        if (uri == null) {
            return null;
        }
        int idx = uri.lastIndexOf('#');
        return idx >= 0 ? uri.substring(idx + 1) : uri;
    }
}
