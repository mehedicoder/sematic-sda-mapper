package com.sda.mapper.hub.parser;

import com.sda.mapper.hub.model.AssetFileFormat;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.RequiredCapability;
import com.sda.mapper.hub.model.Stakeholder;
import com.sda.mapper.hub.model.UseCase;
import com.sda.mapper.hub.model.UseCaseRequirement;
import com.sda.mapper.hub.model.UseCaseRequirementConstraint;
import com.sda.mapper.hub.model.UseCaseRequirementParameter;
import com.sda.mapper.hub.model.VerificationCriterion;
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
import java.util.Optional;
import java.util.function.Function;

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
 * <p>A use-case bundle frequently carries more than the bare requirement: the
 * surrounding {@code UseCase}, {@code Stakeholder}, requirement
 * {@code UseCaseRequirementParameter} / {@code UseCaseRequirementConstraint}
 * children and the {@code VerificationCriterion}. All of those individuals are
 * lifted as well so the coverage ledger reflects the full uploaded package
 * instead of under-reporting those classes as {@code MISSING}.</p>
 *
 * <p>Property names are read with fallbacks so both the ontology-canonical
 * vocabulary ({@code sda:title} / {@code sda:description}) and the legacy
 * spellings ({@code sda:requirementName} / {@code sda:requirementDescription})
 * are accepted.</p>
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
            // Accept both the ontology-canonical :title / :description and the
            // legacy :requirementName / :requirementDescription spellings.
            String reqName = literalOrMulti(req, reqId,
                RDFS.label.getURI(), SDA_NS + "title", SDA_NS + "requirementName");
            String reqDesc = literalOrMulti(req, "",
                SDA_NS + "description", SDA_NS + "requirementDescription");
            String reqPriority = literal(req, SDA_NS + "priority");
            String reqLifecycle = literal(req, SDA_NS + "lifecycleStatus");
            String reqRationale = literal(req, SDA_NS + "rationale");
            String reqAcceptance = literal(req, SDA_NS + "acceptanceCondition");
            result.addEntity(new UseCaseRequirement(reqId, reqName, reqDesc, reqPriority,
                reqId, reqLifecycle, reqRationale, reqAcceptance, Optional.of(provenance), false));

            Statement maps = req.getProperty(mapsToCapability);
            if (maps != null && maps.getObject().isResource()) {
                Resource cap = maps.getObject().asResource();
                if (cap.hasProperty(RDF.type, requiredCapabilityType)) {
                    String capId = literalOrLocalName(cap, SDA_NS + "capabilityID", "capabilityName");
                    String capName = literalOrMulti(cap, capId,
                        RDFS.label.getURI(), SDA_NS + "title", SDA_NS + "capabilityName");
                    String capabilityType = literal(cap, SDA_NS + "capabilityType");
                    result.addEntity(new RequiredCapability(
                        capId, capName, capabilityType, "", "", "", "", "", "", provenance));
                }
            }
        }

        // Lift the remaining use-case-bundle individuals so coverage reflects the
        // full uploaded package rather than only the requirement and its capability.
        liftByType(result, model, provenance, "UseCase", r -> toUseCase(r, provenance));
        liftByType(result, model, provenance, "Stakeholder", r -> toStakeholder(r, provenance));
        liftByType(result, model, provenance, "UseCaseRequirementParameter", r -> toReqParameter(r, provenance));
        liftByType(result, model, provenance, "UseCaseRequirementConstraint", r -> toReqConstraint(r, provenance));
        liftByType(result, model, provenance, "VerificationCriterion", r -> toVerificationCriterion(r, provenance));

        return result;
    }

    private void liftByType(CanonicalAssetModel result, Model model, String provenance,
                            String type, Function<Resource, CanonicalEntity> mapper) {
        Resource typeResource = model.createResource(SDA_NS + type);
        model.listResourcesWithProperty(RDF.type, typeResource)
            .toList()
            .forEach(resource -> result.addEntity(mapper.apply(resource)));
    }

    private CanonicalEntity toUseCase(Resource r, String provenance) {
        return new UseCase(
            literal(r, SDA_NS + "useCaseID"),
            literalOrMulti(r, "", RDFS.label.getURI(), SDA_NS + "useCaseName", SDA_NS + "title"),
            literal(r, SDA_NS + "description"),
            literal(r, SDA_NS + "priority"),
            literal(r, SDA_NS + "businessGoal"),
            Optional.of(provenance), false);
    }

    private CanonicalEntity toStakeholder(Resource r, String provenance) {
        return new Stakeholder(
            literal(r, SDA_NS + "stakeholderID"),
            literalOrMulti(r, "", RDFS.label.getURI(), SDA_NS + "stakeholderName", SDA_NS + "title"),
            literal(r, SDA_NS + "role"),
            literal(r, SDA_NS + "organization"),
            literal(r, SDA_NS + "contactInfo"),
            Optional.of(provenance), false);
    }

    private CanonicalEntity toReqParameter(Resource r, String provenance) {
        return new UseCaseRequirementParameter(
            literal(r, SDA_NS + "parameterID"),
            literalOrMulti(r, "", RDFS.label.getURI(), SDA_NS + "parameterName",
                SDA_NS + "paramName", SDA_NS + "title"),
            literal(r, SDA_NS + "value"),
            literal(r, SDA_NS + "unit"),
            Optional.of(provenance), false);
    }

    private CanonicalEntity toReqConstraint(Resource r, String provenance) {
        return new UseCaseRequirementConstraint(
            literal(r, SDA_NS + "constraintID"),
            literalOrMulti(r, "", RDFS.label.getURI(), SDA_NS + "constraintType", SDA_NS + "title"),
            literal(r, SDA_NS + "expression"),
            literal(r, SDA_NS + "constraintDescription"),
            Optional.of(provenance), false);
    }

    private CanonicalEntity toVerificationCriterion(Resource r, String provenance) {
        return new VerificationCriterion(
            literal(r, SDA_NS + "criterionID"),
            literalOrMulti(r, "", RDFS.label.getURI(), SDA_NS + "criterionDescription", SDA_NS + "title"),
            literalOrMulti(r, "", SDA_NS + "passThreshold", SDA_NS + "criterionDescription"),
            literal(r, SDA_NS + "testMethod"),
            Optional.of(provenance), false);
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

    private static String literalOrMulti(Resource resource, String defaultValue, String... propertyUris) {
        for (String uri : propertyUris) {
            String value = literal(resource, uri);
            if (!value.isBlank()) {
                return value;
            }
        }
        return defaultValue;
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
