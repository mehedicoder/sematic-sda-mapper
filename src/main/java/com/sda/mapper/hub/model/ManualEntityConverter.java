package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * Converts a generic {@link ManualEntity} (built by the manual-entry form)
 * into the concrete typed canonical record for its ontology class, so manual
 * data can be stored in the same typed lists as parsed and lifted entities.
 *
 * <p>Every class that {@code ManualEntryFormFactory} supports must have a
 * mapping here; unknown classes are rejected with an
 * {@link IllegalArgumentException}.</p>
 */
public final class ManualEntityConverter {

    private static final String MANUAL_SOURCE = "manual-entry";

    private ManualEntityConverter() {
    }

    /**
     * Converts a manual entity to its typed canonical record.
     *
     * @param manual the manual entity to convert
     * @return the typed record for the entity's ontology class
     * @throws IllegalArgumentException when the class has no manual mapping
     */
    public static CanonicalEntity toTyped(ManualEntity manual) {
        Map<String, Object> attributes = manual.attributes();
        String source = manual.sourceFileName().orElse(MANUAL_SOURCE);
        return switch (manual.className()) {
            case "Stakeholder" -> new Stakeholder(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "role"), text(attributes, "organization"),
                text(attributes, "contact"), Optional.of(source), false);
            case "UseCase" -> new UseCase(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "priority"),
                text(attributes, "businessGoal"),
                Optional.of(source), false);
            case "UseCaseRequirement", "Requirement" -> new UseCaseRequirement(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "priority"),
                text(attributes, "requirementID"), text(attributes, "lifecycleStatus"),
                text(attributes, "rationale"), text(attributes, "acceptanceCondition"),
                Optional.of(source), false);
            case "RequiredCapability" -> new RequiredCapability(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "functionClass"), text(attributes, "engineeringUnit"),
                text(attributes, "nominalRangeMin"), text(attributes, "nominalRangeMax"),
                text(attributes, "safetyIntegrityLevel"), text(attributes, "protocol"),
                text(attributes, "networkZone"), source);
            case "UseCaseRequirementParameter", "RequirementParameter" -> new UseCaseRequirementParameter(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "value"), text(attributes, "unit"),
                Optional.of(source), false);
            case "UseCaseRequirementConstraint", "RequirementConstraint" -> new UseCaseRequirementConstraint(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "expression"), text(attributes, "description"),
                Optional.of(source), false);
            case "VerificationCriterion" -> new VerificationCriterion(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "criterion"), text(attributes, "method"),
                Optional.of(source), false);
            case "Capability" -> new Capability(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), Optional.of(source), false);
            case "Skill" -> new Skill(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "capabilityId"),
                Optional.of(source), false);
            case "SkillParameter" -> new SkillParameter(
                text(attributes, "id"), text(attributes, "skillId"), text(attributes, "name"),
                text(attributes, "functionClass"), text(attributes, "engineeringUnit"),
                text(attributes, "nominalRangeMin"), text(attributes, "nominalRangeMax"),
                text(attributes, "dataType"), Optional.of(source), false);
            case "Service" -> new Service(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "protocol"),
                Optional.of(source), false);
            case "Asset" -> new Asset(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "manufacturer"), text(attributes, "serialNumber"),
                text(attributes, "assetKind"), Optional.of(source), false);
            case "Sensor" -> new Sensor(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "location"), text(attributes, "unit"),
                Optional.of(source), false);
            case "ProcessVariable" -> new ProcessVariable(
                text(attributes, "id"), text(attributes, "name"), text(attributes, "unit"),
                text(attributes, "nodeId"), text(attributes, "assetId"),
                Optional.of(source), false);
            case "Observation" -> new Observation(
                text(attributes, "id"), text(attributes, "processVariableId"),
                text(attributes, "sensorId"), text(attributes, "value"),
                text(attributes, "timestamp"), Optional.of(source), false);
            case "SemanticConcept" -> new SemanticConcept(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "iri"),
                Optional.of(source), false);
            case "InformationModel" -> new InformationModel(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "modelType"),
                Optional.of(source), false);
            case "CommunicationBinding" -> new CommunicationBinding(
                text(attributes, "id"), text(attributes, "name"), text(attributes, "protocol"),
                text(attributes, "endpointUrl"), text(attributes, "securityMode"),
                text(attributes, "securityPolicy"), Optional.of(source), false);
            case "DataConnector" -> new DataConnector(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "nodeId"),
                Optional.of(source), false);
            case "SafetyClass" -> new SafetyClass(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "level"),
                Optional.of(source), false);
            case "DeploymentPolicy" -> new DeploymentPolicy(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "description"), text(attributes, "replicaCount"),
                Optional.of(source), false);
            case "ServiceInstance" -> new ServiceInstance(
                text(attributes, "id"), text(attributes, "name"), "",
                Optional.of(source), false);
            case "DeploymentContext" -> new DeploymentContext(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "runtime"), "",
                Optional.of(source), false);
            case "ClusterNode" -> new ClusterNode(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "ipAddress"), text(attributes, "role"),
                Optional.of(source), false);
            case "NetworkZone" -> new NetworkZone(
                text(attributes, "id"), text(attributes, "name"),
                text(attributes, "subnet"), "",
                Optional.of(source), false);
            default -> throw new IllegalArgumentException(
                "No manual mapping for ontology class: " + manual.className());
        };
    }

    private static String text(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
