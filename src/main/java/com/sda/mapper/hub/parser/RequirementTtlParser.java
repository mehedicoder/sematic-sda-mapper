package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.RequiredCapability;
import com.sda.mapper.hub.model.UseCaseRequirement;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * Parses a supplementary requirement file (Turtle) into the canonical model.
 *
 * <p>The file expresses a {@code UseCaseRequirement} linked via
 * {@code sda:mapsToCapability} to a {@code RequiredCapability} whose
 * {@code sda:capabilityType} is the join key used by the Service Catalogue
 * matching in {@link com.sda.mapper.reasoning.SemanticReasonerService}. The
 * capability type is carried on the canonical {@link RequiredCapability} as its
 * {@code functionClass} so the existing MATCH stage can bridge it to the
 * catalogue without duplicating the SPARQL join.</p>
 *
 * <p>Only the requirement side of the model is read; offered capabilities remain
 * seeded in the shared catalogue. This keeps the MVP cross-product hazard away
 * from user-supplied data.</p>
 */
@Component
public class RequirementTtlParser implements CanonicalModelParser {

    private static final String SDA_NS = "http://sda-platform.org/ontology#";

    @Override
    public AssetFileFormat format() {
        return AssetFileFormat.REQUIREMENT_TTL;
    }

    @Override
    public CanonicalAssetModel parse(InputStream input, String fileName) {
        Model model = ModelFactory.createDefaultModel();
        model.read(input, null, "TURTLE");

        CanonicalAssetModel result = new CanonicalAssetModel();
        String provenance = fileName != null ? fileName : "requirement.ttl";

        Property mapsToCapability = model.createProperty(SDA_NS + "mapsToCapability");
        Resource requirementType = model.createResource(SDA_NS + "UseCaseRequirement");
        Resource requiredCapabilityType = model.createResource(SDA_NS + "RequiredCapability");

        List<Resource> requirements = model.listResourcesWithProperty(RDF.type, requirementType)
            .toList();
        for (Resource req : requirements) {
            String reqId = literalOrLocalName(req, SDA_NS + "requirementID", "requirementName");
            String reqName = literalOr(req, RDFS.label, SDA_NS + "requirementName", reqId);
            String reqDesc = literal(req, SDA_NS + "requirementDescription");

            result.addEntity(new UseCaseRequirement(reqId, reqName, reqDesc, "", provenance));

            Statement maps = req.getProperty(mapsToCapability);
            if (maps != null && maps.getObject().isResource()) {
                Resource cap = maps.getObject().asResource();
                if (cap.hasProperty(RDF.type, requiredCapabilityType)) {
                    String capId = literalOrLocalName(cap, SDA_NS + "capabilityID", "capabilityName");
                    String capName = literalOr(cap, RDFS.label, SDA_NS + "capabilityName", capId);
                    String capabilityType = literal(cap, SDA_NS + "capabilityType");
                    result.addEntity(new RequiredCapability(
                        capId, capName, capabilityType, "", "", "", "", "", "", provenance));
                }
            }
        }
        return result;
    }

    private static String literal(Resource resource, String propertyUri) {
        Statement statement = resource.getProperty(resource.getModel().createProperty(propertyUri));
        return statement != null && statement.getObject().isLiteral()
            ? statement.getLiteral().getString()
            : "";
    }

    private static String literalOr(Resource resource, Property preferred, String fallbackUri,
                                    String defaultValue) {
        String value = literal(resource, preferred.getURI());
        if (!value.isBlank()) {
            return value;
        }
        value = literal(resource, fallbackUri);
        return value.isBlank() ? defaultValue : value;
    }

    private static String literalOrLocalName(Resource resource, String propertyUri, String fallbackUri) {
        String value = literal(resource, propertyUri);
        if (!value.isBlank()) {
            return value;
        }
        value = literal(resource, fallbackUri);
        if (!value.isBlank()) {
            return value;
        }
        String local = localName(resource);
        return local != null ? local : "";
    }

    private static String localName(Resource resource) {
        String uri = resource.getURI();
        if (uri == null) {
            return null;
        }
        int idx = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
        return idx >= 0 && idx < uri.length() - 1 ? uri.substring(idx + 1) : uri;
    }
}
