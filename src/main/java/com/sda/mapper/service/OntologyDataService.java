package com.sda.mapper.service;

import com.sda.mapper.model.OntologyClass;
import com.sda.mapper.model.OntologyLayer;
import com.sda.mapper.model.OntologyRelation;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Static data holder for the ontology visualizer, grounded in the canonical
 * {@code css-sda-unified.ttl} TBox: five layers, all 24 owl:Class definitions
 * and all 62 owl:ObjectProperty definitions with their declared relation kinds.
 */
@Service
public class OntologyDataService {

    private static final List<OntologyLayer> LAYERS = List.of(
        new OntologyLayer("L1", "L1 — Application & Requirements", "#f59e0b", "l1"),
        new OntologyLayer("L2", "L2 — Capability, Skill, Service", "#10b981", "l2"),
        new OntologyLayer("L2b", "L2b — Platform Infrastructure", "#8b5cf6", "l2b"),
        new OntologyLayer("L3", "L3 — Information & Connectivity", "#06b6d4", "l3"),
        new OntologyLayer("L4", "L4 — Physical Plant", "#ef4444", "l4")
    );

    private static final List<OntologyClass> CLASSES = List.of(
        new OntologyClass("1.1", "Stakeholder", "L1"),
        new OntologyClass("1.2", "UseCase", "L1"),
        new OntologyClass("1.3", "Requirement", "L1"),
        new OntologyClass("1.4", "RequirementParameter", "L1"),
        new OntologyClass("1.5", "RequirementConstraint", "L1"),
        new OntologyClass("1.6", "VerificationCriterion", "L1"),
        new OntologyClass("2.1", "Capability", "L2"),
        new OntologyClass("2.2", "Skill", "L2"),
        new OntologyClass("2.3", "SkillParameter", "L2"),
        new OntologyClass("2.4", "Service", "L2"),
        new OntologyClass("2.5", "ServiceInstance", "L2"),
        new OntologyClass("2b.1", "DeploymentContext", "L2b"),
        new OntologyClass("2b.2", "ClusterNode", "L2b"),
        new OntologyClass("2b.3", "DeploymentPolicy", "L2b"),
        new OntologyClass("2b.4", "NetworkZone", "L2b"),
        new OntologyClass("2b.5", "SafetyClass", "L2b"),
        new OntologyClass("3.1", "InformationModel", "L3"),
        new OntologyClass("3.2", "SemanticConcept", "L3"),
        new OntologyClass("3.3", "CommunicationBinding", "L3"),
        new OntologyClass("3.4", "DataConnector", "L3"),
        new OntologyClass("4.1", "Asset", "L4"),
        new OntologyClass("4.2", "Sensor", "L4"),
        new OntologyClass("4.3", "ProcessVariable", "L4"),
        new OntologyClass("4.4", "Observation", "L4")
    );

    private static final List<OntologyRelation> RELATIONS = List.of(
        new OntologyRelation("Stakeholder", "UseCase", "definesUseCase", "supporting", null),
        new OntologyRelation("Stakeholder", "Requirement", "ownsRequirement", "supporting", null),
        new OntologyRelation("Stakeholder", "VerificationCriterion", "validates", "supporting", null),
        new OntologyRelation("UseCase", "Stakeholder", "initiatedBy", "supporting", "definesUseCase"),
        new OntologyRelation("UseCase", "Requirement", "hasRequirement", "supporting", null),
        new OntologyRelation("UseCase", "Asset", "targetsAsset", "supporting", null),
        new OntologyRelation("UseCase", "Requirement", "determines", "supporting", null),
        new OntologyRelation("Requirement", "RequirementParameter", "hasParameter", "core", null),
        new OntologyRelation("Requirement", "RequirementConstraint", "hasConstraint", "core", null),
        new OntologyRelation("Requirement", "VerificationCriterion", "verifiedBy", "core", null),
        new OntologyRelation("Requirement", "Capability", "mapsToCapability", "core", null),
        new OntologyRelation("Requirement", "InformationModel", "mapsToInformationModel", "supporting", null),
        new OntologyRelation("Requirement", "DeploymentContext", "mapsToDeploymentContext", "supporting", null),
        new OntologyRelation("RequirementParameter", "Requirement", "belongsToRequirement", "supporting", "hasParameter"),
        new OntologyRelation("RequirementConstraint", "Requirement", "constrains", "supporting", "hasConstraint"),
        new OntologyRelation("VerificationCriterion", "Requirement", "verifies", "supporting", "verifiedBy"),
        new OntologyRelation("VerificationCriterion", "Stakeholder", "validatedBy", "supporting", "validates"),
        new OntologyRelation("Capability", "Asset", "operatesOn", "supporting", null),
        new OntologyRelation("Capability", "Skill", "realisedBy", "core", null),
        new OntologyRelation("Capability", "Service", "exposedAs", "core", null),
        new OntologyRelation("Capability", "Requirement", "mapsTo", "core", null),
        new OntologyRelation("Skill", "Capability", "realises", "supporting", "realisedBy"),
        new OntologyRelation("Skill", "Service", "isImplementedBy", "core", null),
        new OntologyRelation("Skill", "SemanticConcept", "requiresConcept", "core", null),
        new OntologyRelation("Skill", "CommunicationBinding", "uses", "core", null),
        new OntologyRelation("SkillParameter", "Skill", "parameterisesSkill", "supporting", "hasParameter"),
        new OntologyRelation("Service", "Skill", "implementsSkill", "supporting", "isImplementedBy"),
        new OntologyRelation("Service", "CommunicationBinding", "hasBinding", "core", null),
        new OntologyRelation("Service", "DataConnector", "hasConnector", "core", null),
        new OntologyRelation("Service", "DeploymentContext", "isDeployedBy", "core", null),
        new OntologyRelation("Service", "Requirement", "satisfies", "core", null),
        new OntologyRelation("Service", "ServiceInstance", "instantiatedAs", "core", null),
        new OntologyRelation("ServiceInstance", "Service", "instantiates", "supporting", "instantiatedAs"),
        new OntologyRelation("ServiceInstance", "Capability", "resolves", "supporting", null),
        new OntologyRelation("ServiceInstance", "ClusterNode", "deployedOn", "supporting", null),
        new OntologyRelation("DeploymentContext", "ServiceInstance", "hostsService", "supporting", null),
        new OntologyRelation("DeploymentContext", "ClusterNode", "containsNode", "supporting", null),
        new OntologyRelation("DeploymentContext", "DeploymentPolicy", "hasPolicy", "supporting", null),
        new OntologyRelation("DeploymentContext", "NetworkZone", "locatedInZone", "supporting", null),
        new OntologyRelation("DeploymentContext", "SafetyClass", "constrainedBy", "supporting", null),
        new OntologyRelation("ClusterNode", "DeploymentContext", "partOf", "supporting", "containsNode"),
        new OntologyRelation("ClusterNode", "ServiceInstance", "hosts", "supporting", "deployedOn"),
        new OntologyRelation("ClusterNode", "NetworkZone", "locatedIn", "supporting", null),
        new OntologyRelation("DeploymentPolicy", "Service", "appliesTo", "supporting", null),
        new OntologyRelation("DeploymentPolicy", "ClusterNode", "scheduledOn", "supporting", null),
        new OntologyRelation("NetworkZone", "ClusterNode", "contains", "supporting", "locatedIn"),
        new OntologyRelation("NetworkZone", "DeploymentContext", "hostedIn", "supporting", "locatedInZone"),
        new OntologyRelation("SafetyClass", "DeploymentContext", "constrainsDeployment", "supporting", "constrainedBy"),
        new OntologyRelation("InformationModel", "SemanticConcept", "mappedTo", "supporting", null),
        new OntologyRelation("InformationModel", "Service", "usedBy", "supporting", null),
        new OntologyRelation("SemanticConcept", "ProcessVariable", "describesConcept", "supporting", null),
        new OntologyRelation("CommunicationBinding", "Service", "bindsService", "supporting", null),
        new OntologyRelation("DataConnector", "CommunicationBinding", "usesBinding", "supporting", null),
        new OntologyRelation("DataConnector", "ProcessVariable", "mapsVariable", "supporting", null),
        new OntologyRelation("Asset", "Sensor", "hasSensor", "supporting", null),
        new OntologyRelation("Asset", "DeploymentContext", "hasContext", "supporting", null),
        new OntologyRelation("Sensor", "ProcessVariable", "measures", "supporting", null),
        new OntologyRelation("Sensor", "Asset", "locatedOn", "supporting", "hasSensor"),
        new OntologyRelation("ProcessVariable", "SemanticConcept", "describedBy", "supporting", "describesConcept"),
        new OntologyRelation("ProcessVariable", "Sensor", "measuredBy", "supporting", "measures"),
        new OntologyRelation("Observation", "ProcessVariable", "observes", "supporting", null),
        new OntologyRelation("Observation", "Sensor", "producedBy", "supporting", null)
    );

    /**
     * Returns all ontology layers in display order.
     *
     * @return the five CSS-SDA layers
     */
    public List<OntologyLayer> getLayers() {
        return LAYERS;
    }

    /**
     * Returns all ontology classes.
     *
     * @return all 24 classes of the canonical TBox
     */
    public List<OntologyClass> getClasses() {
        return CLASSES;
    }

    /**
     * Returns the classes that belong to the given layer.
     *
     * @param layerId layer identifier (e.g. {@code "L1"})
     * @return classes of that layer in diagram order, or an empty list for unknown layers
     */
    public List<OntologyClass> getClassesByLayer(String layerId) {
        return CLASSES.stream()
            .filter(c -> c.layerId().equals(layerId))
            .toList();
    }

    /**
     * Returns all ontology relations.
     *
     * @return all 62 object property declarations of the canonical TBox
     */
    public List<OntologyRelation> getRelations() {
        return RELATIONS;
    }

    /**
     * Returns the relations that originate from the given class.
     *
     * @param className class name (e.g. {@code "Requirement"})
     * @return outgoing relations sorted by property name
     */
    public List<OntologyRelation> getRelationsFrom(String className) {
        return RELATIONS.stream()
            .filter(r -> r.from().equals(className))
            .sorted(Comparator.comparing(OntologyRelation::label))
            .toList();
    }

    /**
     * Returns the relations that target the given class.
     *
     * @param className class name (e.g. {@code "Requirement"})
     * @return incoming relations sorted by property name
     */
    public List<OntologyRelation> getRelationsTo(String className) {
        return RELATIONS.stream()
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
        return CLASSES.stream()
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
        return LAYERS.stream()
            .filter(l -> l.id().equals(id))
            .findFirst();
    }
}
