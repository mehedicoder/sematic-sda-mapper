package com.sda.mapper.hub.service;

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
import java.util.List;
import java.util.Map;

/**
 * MATCH stage: computes per-class coverage of the canonical model against the
 * 24 ontology classes of the canonical TBox, plus blocking gaps for classes
 * that are missing while a core relation ties them to a covered class.
 */
@Service
public class MatchingService {

    private final OntologyDataService ontologyDataService;

    /**
     * Creates the matching service over the grounded ontology catalog.
     *
     * @param ontologyDataService the ontology data service with the 24 classes
     *                            and 62 relations of the canonical TBox
     */
    public MatchingService(OntologyDataService ontologyDataService) {
        this.ontologyDataService = ontologyDataService;
    }

    /**
     * Required attribute keys per ontology class; a class with explicit
     * entities that miss any of these is reported as {@code PARTIAL}.
     */
    private static final Map<String, List<String>> REQUIRED_FIELDS = Map.ofEntries(
        Map.entry("Stakeholder", List.of("name", "role")),
        Map.entry("UseCase", List.of("name")),
        Map.entry("Requirement", List.of("name")),
        Map.entry("RequirementParameter", List.of("name", "value")),
        Map.entry("RequirementConstraint", List.of("name", "expression")),
        Map.entry("VerificationCriterion", List.of("name", "criterion")),
        Map.entry("Capability", List.of("name")),
        Map.entry("Skill", List.of("name")),
        Map.entry("SkillParameter", List.of("name", "engineeringUnit")),
        Map.entry("Service", List.of("name")),
        Map.entry("ServiceInstance", List.of("endpointUrl")),
        Map.entry("DeploymentContext", List.of("name")),
        Map.entry("ClusterNode", List.of("name")),
        Map.entry("DeploymentPolicy", List.of("name")),
        Map.entry("NetworkZone", List.of("name")),
        Map.entry("SafetyClass", List.of("name", "level")),
        Map.entry("InformationModel", List.of("name")),
        Map.entry("SemanticConcept", List.of("name", "iri")),
        Map.entry("CommunicationBinding", List.of("endpointUrl", "protocol")),
        Map.entry("DataConnector", List.of("name", "nodeId")),
        Map.entry("Asset", List.of("name")),
        Map.entry("Sensor", List.of("name", "unit")),
        Map.entry("ProcessVariable", List.of("name", "nodeId")),
        Map.entry("Observation", List.of("processVariableId", "value"))
    );

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
        List<String> required = REQUIRED_FIELDS.getOrDefault(className, List.of());
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
