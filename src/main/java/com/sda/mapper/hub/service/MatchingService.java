package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.AttributeBindings;
import com.sda.mapper.hub.model.BlockingGap;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.CoverageEntry;
import com.sda.mapper.hub.model.CoverageStatus;
import com.sda.mapper.hub.model.GapReport;
import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyRelation;
import com.sda.mapper.service.OntologyDataService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MATCH stage: computes per-class coverage of the canonical model against the
 * 24 ontology classes of the canonical TBox, plus blocking gaps for classes
 * that are missing while a core relation ties them to a covered class.
 *
 * <p>The required attributes per class are expressed as ontology datatype
 * property names and validated against the TBox at construction, so the
 * coverage rule can never drift from the ontology.</p>
 */
@Service
public class MatchingService {

    private final OntologyDataService ontologyDataService;
    private final Map<String, List<String>> requiredFields;

    /**
     * Creates the matching service over the grounded ontology catalog and
     * derives the per-class required attributes from the TBox.
     *
     * @param ontologyDataService the ontology data service with the 24 classes
     *                            and 89 labeled relations of the canonical TBox
     * @throws IllegalStateException when a required attribute is not declared
     *                               as a datatype property of its class
     */
    public MatchingService(OntologyDataService ontologyDataService) {
        this.ontologyDataService = ontologyDataService;
        this.requiredFields = deriveRequiredFields(ontologyDataService);
    }

    /**
     * Required ontology datatype properties per ontology class; a class with
     * explicit entities that miss any of these is reported as {@code PARTIAL}.
     * Each property name must be declared as a datatype property of the class
     * in the v2 TBox (verified at construction).
     */
    private static final Map<String, List<String>> REQUIRED_PROPERTY_NAMES = Map.ofEntries(
        Map.entry("Stakeholder", List.of("stakeholderName", "role")),
        Map.entry("UseCase", List.of("useCaseName")),
        Map.entry("UseCaseRequirement", List.of("title")),
        Map.entry("UseCaseRequirementParameter", List.of("parameterName")),
        Map.entry("UseCaseRequirementConstraint", List.of("expression")),
        Map.entry("VerificationCriterion", List.of("criterionDescription")),
        Map.entry("Capability", List.of("capabilityName")),
        Map.entry("Skill", List.of("skillName")),
        Map.entry("SkillParameter", List.of("paramName", "paramUnit")),
        Map.entry("Service", List.of("serviceName")),
        Map.entry("ServiceInstance", List.of("instanceID")),
        Map.entry("DeploymentContext", List.of("contextID")),
        Map.entry("ClusterNode", List.of("nodeID")),
        Map.entry("DeploymentPolicy", List.of("policyID")),
        Map.entry("NetworkZone", List.of("zoneName")),
        Map.entry("SafetyClass", List.of("classID", "safetyIntegrityLevel")),
        Map.entry("InformationModel", List.of("modelName")),
        Map.entry("SemanticConcept", List.of("conceptName", "semanticIRI")),
        Map.entry("CommunicationBinding", List.of("bindingEndpointUri", "protocol")),
        Map.entry("DataConnector", List.of("connectorID", "connectorSemanticIRI")),
        Map.entry("Asset", List.of("assetName")),
        Map.entry("Sensor", List.of("sensorID")),
        Map.entry("ProcessVariable", List.of("variableName", "variableID")),
        Map.entry("Observation", List.of("observationID", "value"))
    );

    private static Map<String, List<String>> deriveRequiredFields(OntologyDataService ontology) {
        Map<String, List<String>> required = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : REQUIRED_PROPERTY_NAMES.entrySet()) {
            String className = entry.getKey();
            List<String> hubKeys = new ArrayList<>();
            for (String propertyName : entry.getValue()) {
                boolean declared = ontology.getDatatypeProperties(className).stream()
                    .anyMatch(property -> property.name().equals(propertyName));
                if (!declared) {
                    throw new IllegalStateException(
                        "Required attribute for " + className + " references undeclared "
                            + "ontology datatype property: " + propertyName);
                }
                hubKeys.add(AttributeBindings.hubKey(propertyName));
            }
            required.put(className, List.copyOf(hubKeys));
        }
        return Map.copyOf(required);
    }

    /**
     * Computes the coverage report for the given model and inferred entities.
     *
     * @param model     the canonical model
     * @param inferred  the inferred entities
     * @return the coverage report over all 24 ontology classes
     */
    public GapReport compute(CanonicalAssetModel model, List<CanonicalEntity> inferred) {
        List<CoverageEntry> entries = new ArrayList<>();
        for (OntologyClass clazz : ontologyDataService.getClasses()) {
            entries.add(computeEntry(clazz, model, inferred));
        }
        List<BlockingGap> blockingGaps = computeBlockingGaps(entries);
        return new GapReport(entries, blockingGaps, coveragePercent(entries));
    }

    private CoverageEntry computeEntry(OntologyClass clazz, CanonicalAssetModel model,
                                       List<CanonicalEntity> inferred) {
        String className = clazz.name();
        List<CanonicalEntity> explicit = model.getEntitiesForClass(className);
        List<CanonicalEntity> inferredForClass = inferred.stream()
            .filter(e -> e.className().equals(className))
            .toList();

        if (explicit.isEmpty() && inferredForClass.isEmpty()) {
            return CoverageEntry.missing(className, clazz.layerId());
        }

        List<String> sources = explicit.stream()
            .flatMap(e -> e.sourceFileName().stream())
            .distinct()
            .toList();

        if (explicit.isEmpty()) {
            return new CoverageEntry(className, clazz.layerId(), CoverageStatus.INFERRED,
                sources, List.of());
        }

        List<String> missingFields = missingRequiredFields(className, explicit);
        CoverageStatus status = missingFields.isEmpty()
            ? CoverageStatus.PRESENT
            : CoverageStatus.PARTIAL;
        return new CoverageEntry(className, clazz.layerId(), status, sources, missingFields);
    }

    private List<String> missingRequiredFields(String className, List<CanonicalEntity> explicit) {
        List<String> required = requiredFields.getOrDefault(className, List.of());
        if (required.isEmpty()) {
            return List.of();
        }
        List<String> missing = new ArrayList<>();
        for (String field : required) {
            boolean satisfied = explicit.stream()
                .anyMatch(e -> fieldPresent(e, field));
            if (!satisfied) {
                missing.add(field);
            }
        }
        return missing;
    }

    private static boolean fieldPresent(CanonicalEntity entity, String field) {
        Object value = entity.attributes().get(field);
        return value != null && !String.valueOf(value).isBlank();
    }

    private List<BlockingGap> computeBlockingGaps(List<CoverageEntry> entries) {
        List<BlockingGap> gaps = new ArrayList<>();
        for (CoverageEntry entry : entries) {
            if (entry.status() != CoverageStatus.MISSING) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            for (OntologyRelation relation : ontologyDataService.getRelationsFrom(entry.className())) {
                if ("core".equals(relation.kind()) && isCovered(entries, relation.to())) {
                    reasons.add(relation.label() + " \u2192 " + relation.to());
                }
            }
            for (OntologyRelation relation : ontologyDataService.getRelationsTo(entry.className())) {
                if ("core".equals(relation.kind()) && isCovered(entries, relation.from())) {
                    reasons.add(relation.from() + " " + relation.label());
                }
            }
            if (!reasons.isEmpty()) {
                gaps.add(new BlockingGap(entry.className(), entry.layerId(), reasons));
            }
        }
        return gaps;
    }

    private static boolean isCovered(List<CoverageEntry> entries, String className) {
        return entries.stream()
            .filter(e -> e.className().equals(className))
            .anyMatch(e -> e.status() == CoverageStatus.PRESENT
                || e.status() == CoverageStatus.PARTIAL
                || e.status() == CoverageStatus.INFERRED);
    }

    private static double coveragePercent(List<CoverageEntry> entries) {
        double weighted = entries.stream().mapToDouble(MatchingService::weight).sum();
        return entries.isEmpty() ? 0.0 : Math.round(weighted / entries.size() * 1000.0) / 10.0;
    }

    private static double weight(CoverageEntry entry) {
        return switch (entry.status()) {
            case PRESENT -> 1.0;
            case PARTIAL, INFERRED -> 0.5;
            case MISSING -> 0.0;
        };
    }
}
