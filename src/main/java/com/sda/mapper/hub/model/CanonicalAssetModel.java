package com.sda.mapper.hub.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Accumulated canonical model of the mapping hub.
 *
 * <p>Holds the canonical entities discovered across all uploaded files,
 * grouped by ontology class. Merging is idempotent: entities with the same
 * class and id are kept only once, so supplementary uploads enrich the model
 * instead of replacing it.</p>
 */
public final class CanonicalAssetModel {

    private final List<Stakeholder> stakeholders = new ArrayList<>();
    private final List<UseCase> useCases = new ArrayList<>();
    private final List<UseCaseRequirement> requirements = new ArrayList<>();
    private final List<UseCaseRequirementParameter> requirementParameters = new ArrayList<>();
    private final List<UseCaseRequirementConstraint> requirementConstraints = new ArrayList<>();
    private final List<VerificationCriterion> verificationCriteria = new ArrayList<>();
    private final List<Capability> capabilities = new ArrayList<>();
    private final List<OfferedCapability> offeredCapabilities = new ArrayList<>();
    private final List<RequiredCapability> requiredCapabilities = new ArrayList<>();
    private final List<CapabilityMatch> capabilityMatches = new ArrayList<>();
    private final List<Skill> skills = new ArrayList<>();
    private final List<SkillParameter> skillParameters = new ArrayList<>();
    private final List<Service> services = new ArrayList<>();
    private final List<ServiceInstance> serviceInstances = new ArrayList<>();
    private final List<DeploymentContext> deploymentContexts = new ArrayList<>();
    private final List<ClusterNode> clusterNodes = new ArrayList<>();
    private final List<DeploymentPolicy> deploymentPolicies = new ArrayList<>();
    private final List<NetworkZone> networkZones = new ArrayList<>();
    private final List<SafetyClass> safetyClasses = new ArrayList<>();
    private final List<InformationModel> informationModels = new ArrayList<>();
    private final List<SemanticConcept> semanticConcepts = new ArrayList<>();
    private final List<CommunicationBinding> communicationBindings = new ArrayList<>();
    private final List<DataConnector> dataConnectors = new ArrayList<>();
    private final List<Asset> assets = new ArrayList<>();
    private final List<Sensor> sensors = new ArrayList<>();
    private final List<ProcessVariable> processVariables = new ArrayList<>();
    private final List<Observation> observations = new ArrayList<>();
    private final List<AutomationMLEntity> automationMLEntities = new ArrayList<>();

    /**
     * Adds an entity, ignoring duplicates with the same class and id.
     *
     * @param entity the canonical entity to add
     * @throws IllegalArgumentException when the class name is not a known ontology class
     */
    public void addEntity(CanonicalEntity entity) {
        if (entity instanceof ManualEntity manual) {
            entity = ManualEntityConverter.toTyped(manual);
        }
        switch (entity.className()) {
            case "Stakeholder" -> addUnique(stakeholders, (Stakeholder) entity);
            case "UseCase" -> addUnique(useCases, (UseCase) entity);
            case "UseCaseRequirement" -> addUnique(requirements, (UseCaseRequirement) entity);
            case "UseCaseRequirementParameter" -> addUnique(requirementParameters, (UseCaseRequirementParameter) entity);
            case "UseCaseRequirementConstraint" -> addUnique(requirementConstraints, (UseCaseRequirementConstraint) entity);
            case "VerificationCriterion" -> addUnique(verificationCriteria, (VerificationCriterion) entity);
            case "Capability" -> addUnique(capabilities, (Capability) entity);
            case "OfferedCapability" -> addUnique(offeredCapabilities, (OfferedCapability) entity);
            case "RequiredCapability" -> addUnique(requiredCapabilities, (RequiredCapability) entity);
            case "CapabilityMatch" -> addUnique(capabilityMatches, (CapabilityMatch) entity);
            case "Skill" -> addUnique(skills, (Skill) entity);
            case "SkillParameter" -> addUnique(skillParameters, (SkillParameter) entity);
            case "Service" -> addUnique(services, (Service) entity);
            case "ServiceInstance" -> addUnique(serviceInstances, (ServiceInstance) entity);
            case "DeploymentContext" -> addUnique(deploymentContexts, (DeploymentContext) entity);
            case "ClusterNode" -> addUnique(clusterNodes, (ClusterNode) entity);
            case "DeploymentPolicy" -> addUnique(deploymentPolicies, (DeploymentPolicy) entity);
            case "NetworkZone" -> addUnique(networkZones, (NetworkZone) entity);
            case "SafetyClass" -> addUnique(safetyClasses, (SafetyClass) entity);
            case "InformationModel" -> addUnique(informationModels, (InformationModel) entity);
            case "SemanticConcept" -> addUnique(semanticConcepts, (SemanticConcept) entity);
            case "CommunicationBinding" -> addUnique(communicationBindings, (CommunicationBinding) entity);
            case "DataConnector" -> addUnique(dataConnectors, (DataConnector) entity);
            case "Asset" -> addUnique(assets, (Asset) entity);
            case "Sensor" -> addUnique(sensors, (Sensor) entity);
            case "ProcessVariable" -> addUnique(processVariables, (ProcessVariable) entity);
            case "Observation" -> addUnique(observations, (Observation) entity);
            case "AutomationML" -> addUnique(automationMLEntities, (AutomationMLEntity) entity);
            default -> throw new IllegalArgumentException(
                "Unknown canonical class: " + entity.className());
        }
    }

    /**
     * Replaces an existing entity (matched by class + id) with an updated one.
     * Used by the editable Application/Requirements Layer review flow: the user
     * changes field values and the edited entity is written back before the
     * pipeline is re-run.
     *
     * @param updated the updated canonical entity (same id and class as the one it replaces)
     */
    public void replaceEntity(CanonicalEntity updated) {
        removeById(updated.className(), updated.id());
        addEntity(updated);
    }

    private void removeById(String className, String id) {
        switch (className) {
            case "Stakeholder" -> stakeholders.removeIf(e -> e.id().equals(id));
            case "UseCase" -> useCases.removeIf(e -> e.id().equals(id));
            case "UseCaseRequirement" -> requirements.removeIf(e -> e.id().equals(id));
            case "UseCaseRequirementParameter" -> requirementParameters.removeIf(e -> e.id().equals(id));
            case "UseCaseRequirementConstraint" -> requirementConstraints.removeIf(e -> e.id().equals(id));
            case "VerificationCriterion" -> verificationCriteria.removeIf(e -> e.id().equals(id));
            case "Capability" -> capabilities.removeIf(e -> e.id().equals(id));
            case "OfferedCapability" -> offeredCapabilities.removeIf(e -> e.id().equals(id));
            case "RequiredCapability" -> requiredCapabilities.removeIf(e -> e.id().equals(id));
            case "Skill" -> skills.removeIf(e -> e.id().equals(id));
            case "SkillParameter" -> skillParameters.removeIf(e -> e.id().equals(id));
            case "Service" -> services.removeIf(e -> e.id().equals(id));
            case "ServiceInstance" -> serviceInstances.removeIf(e -> e.id().equals(id));
            case "DeploymentContext" -> deploymentContexts.removeIf(e -> e.id().equals(id));
            case "ClusterNode" -> clusterNodes.removeIf(e -> e.id().equals(id));
            case "DeploymentPolicy" -> deploymentPolicies.removeIf(e -> e.id().equals(id));
            case "NetworkZone" -> networkZones.removeIf(e -> e.id().equals(id));
            case "SafetyClass" -> safetyClasses.removeIf(e -> e.id().equals(id));
            case "InformationModel" -> informationModels.removeIf(e -> e.id().equals(id));
            case "SemanticConcept" -> semanticConcepts.removeIf(e -> e.id().equals(id));
            case "CommunicationBinding" -> communicationBindings.removeIf(e -> e.id().equals(id));
            case "DataConnector" -> dataConnectors.removeIf(e -> e.id().equals(id));
            case "Asset" -> assets.removeIf(e -> e.id().equals(id));
            case "Sensor" -> sensors.removeIf(e -> e.id().equals(id));
            case "ProcessVariable" -> processVariables.removeIf(e -> e.id().equals(id));
            case "Observation" -> observations.removeIf(e -> e.id().equals(id));
            case "AutomationML" -> automationMLEntities.removeIf(e -> e.id().equals(id));
            default -> { /* unknown class: nothing to remove */ }
        }
    }

    /**
     * Merges all entities of another model into this one, keeping duplicates out.
     *
     * @param other the model to merge from
     */
    public void merge(CanonicalAssetModel other) {
        mergeUnique(stakeholders, other.stakeholders);
        mergeUnique(useCases, other.useCases);
        mergeUnique(requirements, other.requirements);
        mergeUnique(requirementParameters, other.requirementParameters);
        mergeUnique(requirementConstraints, other.requirementConstraints);
        mergeUnique(verificationCriteria, other.verificationCriteria);
        mergeUnique(capabilities, other.capabilities);
        mergeUnique(offeredCapabilities, other.offeredCapabilities);
        mergeUnique(requiredCapabilities, other.requiredCapabilities);
        mergeUnique(capabilityMatches, other.capabilityMatches);
        mergeUnique(skills, other.skills);
        mergeUnique(skillParameters, other.skillParameters);
        mergeUnique(services, other.services);
        mergeUnique(serviceInstances, other.serviceInstances);
        mergeUnique(deploymentContexts, other.deploymentContexts);
        mergeUnique(clusterNodes, other.clusterNodes);
        mergeUnique(deploymentPolicies, other.deploymentPolicies);
        mergeUnique(networkZones, other.networkZones);
        mergeUnique(safetyClasses, other.safetyClasses);
        mergeUnique(informationModels, other.informationModels);
        mergeUnique(semanticConcepts, other.semanticConcepts);
        mergeUnique(communicationBindings, other.communicationBindings);
        mergeUnique(dataConnectors, other.dataConnectors);
        mergeUnique(assets, other.assets);
        mergeUnique(sensors, other.sensors);
        mergeUnique(processVariables, other.processVariables);
        mergeUnique(observations, other.observations);
    }

    /**
     * Returns the entities of the given ontology class.
     *
     * @param className the ontology class name
     * @return an immutable list of entities of that class, or an empty list for unknown classes
     */
    public List<CanonicalEntity> getEntitiesForClass(String className) {
        return switch (className) {
            case "Stakeholder" -> List.copyOf(stakeholders);
            case "UseCase" -> List.copyOf(useCases);
            case "UseCaseRequirement" -> List.copyOf(requirements);
            case "UseCaseRequirementParameter" -> List.copyOf(requirementParameters);
            case "UseCaseRequirementConstraint" -> List.copyOf(requirementConstraints);
            case "VerificationCriterion" -> List.copyOf(verificationCriteria);
            case "Capability" -> List.copyOf(capabilities);
            case "OfferedCapability" -> List.copyOf(offeredCapabilities);
            case "RequiredCapability" -> List.copyOf(requiredCapabilities);
            case "CapabilityMatch" -> List.copyOf(capabilityMatches);
            case "Skill" -> List.copyOf(skills);
            case "SkillParameter" -> List.copyOf(skillParameters);
            case "Service" -> List.copyOf(services);
            case "ServiceInstance" -> List.copyOf(serviceInstances);
            case "DeploymentContext" -> List.copyOf(deploymentContexts);
            case "ClusterNode" -> List.copyOf(clusterNodes);
            case "DeploymentPolicy" -> List.copyOf(deploymentPolicies);
            case "NetworkZone" -> List.copyOf(networkZones);
            case "SafetyClass" -> List.copyOf(safetyClasses);
            case "InformationModel" -> List.copyOf(informationModels);
            case "SemanticConcept" -> List.copyOf(semanticConcepts);
            case "CommunicationBinding" -> List.copyOf(communicationBindings);
            case "DataConnector" -> List.copyOf(dataConnectors);
            case "Asset" -> List.copyOf(assets);
            case "Sensor" -> List.copyOf(sensors);
            case "ProcessVariable" -> List.copyOf(processVariables);
            case "Observation" -> List.copyOf(observations);
            case "AutomationML" -> List.copyOf(automationMLEntities);
            default -> List.of();
        };
    }

    /**
     * Returns the stakeholders.
     *
     * @return an immutable list of stakeholders
     */
    public List<Stakeholder> stakeholders() {
        return List.copyOf(stakeholders);
    }

    /**
     * Returns the use cases.
     *
     * @return an immutable list of use cases
     */
    public List<UseCase> useCases() {
        return List.copyOf(useCases);
    }

    /**
     * Returns the requirements.
     *
     * @return an immutable list of requirements
     */
    public List<UseCaseRequirement> requirements() {
        return List.copyOf(requirements);
    }

    /**
     * Returns the requirement parameters.
     *
     * @return an immutable list of requirement parameters
     */
    public List<UseCaseRequirementParameter> requirementParameters() {
        return List.copyOf(requirementParameters);
    }

    /**
     * Returns the requirement constraints.
     *
     * @return an immutable list of requirement constraints
     */
    public List<UseCaseRequirementConstraint> requirementConstraints() {
        return List.copyOf(requirementConstraints);
    }

    /**
     * Returns the verification criteria.
     *
     * @return an immutable list of verification criteria
     */
    public List<VerificationCriterion> verificationCriteria() {
        return List.copyOf(verificationCriteria);
    }

    /**
     * Returns the capabilities.
     *
     * @return an immutable list of capabilities
     */
    public List<Capability> capabilities() {
        return List.copyOf(capabilities);
    }

    /**
     * Returns the offered capabilities.
     *
     * @return an immutable list of offered capabilities
     */
    public List<OfferedCapability> offeredCapabilities() {
        return List.copyOf(offeredCapabilities);
    }

    /**
     * Returns the required capabilities.
     *
     * @return an immutable list of required capabilities
     */
    public List<RequiredCapability> requiredCapabilities() {
        return List.copyOf(requiredCapabilities);
    }

    /**
     * Returns the semantic capability matches.
     *
     * @return an immutable list of capability matches
     */
    public List<CapabilityMatch> capabilityMatches() {
        return List.copyOf(capabilityMatches);
    }

    /**
     * Returns the skills.
     *
     * @return an immutable list of skills
     */
    public List<Skill> skills() {
        return List.copyOf(skills);
    }

    /**
     * Returns the skill parameters.
     *
     * @return an immutable list of skill parameters
     */
    public List<SkillParameter> skillParameters() {
        return List.copyOf(skillParameters);
    }

    /**
     * Returns the services.
     *
     * @return an immutable list of services
     */
    public List<Service> services() {
        return List.copyOf(services);
    }

    /**
     * Returns the service instances.
     *
     * @return an immutable list of service instances
     */
    public List<ServiceInstance> serviceInstances() {
        return List.copyOf(serviceInstances);
    }

    /**
     * Returns the deployment contexts.
     *
     * @return an immutable list of deployment contexts
     */
    public List<DeploymentContext> deploymentContexts() {
        return List.copyOf(deploymentContexts);
    }

    /**
     * Returns the cluster nodes.
     *
     * @return an immutable list of cluster nodes
     */
    public List<ClusterNode> clusterNodes() {
        return List.copyOf(clusterNodes);
    }

    /**
     * Returns the deployment policies.
     *
     * @return an immutable list of deployment policies
     */
    public List<DeploymentPolicy> deploymentPolicies() {
        return List.copyOf(deploymentPolicies);
    }

    /**
     * Returns the network zones.
     *
     * @return an immutable list of network zones
     */
    public List<NetworkZone> networkZones() {
        return List.copyOf(networkZones);
    }

    /**
     * Returns the safety classes.
     *
     * @return an immutable list of safety classes
     */
    public List<SafetyClass> safetyClasses() {
        return List.copyOf(safetyClasses);
    }

    /**
     * Returns the information models.
     *
     * @return an immutable list of information models
     */
    public List<InformationModel> informationModels() {
        return List.copyOf(informationModels);
    }

    /**
     * Returns the semantic concepts.
     *
     * @return an immutable list of semantic concepts
     */
    public List<SemanticConcept> semanticConcepts() {
        return List.copyOf(semanticConcepts);
    }

    /**
     * Returns the communication bindings.
     *
     * @return an immutable list of communication bindings
     */
    public List<CommunicationBinding> communicationBindings() {
        return List.copyOf(communicationBindings);
    }

    /**
     * Returns the data connectors.
     *
     * @return an immutable list of data connectors
     */
    public List<DataConnector> dataConnectors() {
        return List.copyOf(dataConnectors);
    }

    /**
     * Returns the assets.
     *
     * @return an immutable list of assets
     */
    public List<Asset> assets() {
        return List.copyOf(assets);
    }

    /**
     * Returns the sensors.
     *
     * @return an immutable list of sensors
     */
    public List<Sensor> sensors() {
        return List.copyOf(sensors);
    }

    /**
     * Returns the process variables.
     *
     * @return an immutable list of process variables
     */
    public List<ProcessVariable> processVariables() {
        return List.copyOf(processVariables);
    }

    /**
     * Returns the observations.
     *
     * @return an immutable list of observations
     */
    public List<Observation> observations() {
        return List.copyOf(observations);
    }

    /**
     * Returns the AutomationML (CAEX) entities lifted from uploaded AutomationML files.
     *
     * @return an immutable list of AutomationML entities
     */
    public List<AutomationMLEntity> automationMLEntities() {
        return List.copyOf(automationMLEntities);
    }

    /**
     * Returns the number of entities for the given ontology class.
     *
     * @param className the ontology class name
     * @return the entity count, or {@code 0} for unknown classes
     */
    public int countForClass(String className) {
        return getEntitiesForClass(className).size();
    }

    /**
     * Returns all entities flattened in ontology layer order (L1 to L4).
     *
     * @return an immutable list of all entities
     */
    public List<CanonicalEntity> allEntities() {
        List<CanonicalEntity> result = new ArrayList<>();
        result.addAll(stakeholders);
        result.addAll(useCases);
        result.addAll(requirements);
        result.addAll(requirementParameters);
        result.addAll(requirementConstraints);
        result.addAll(verificationCriteria);
        result.addAll(capabilities);
        result.addAll(offeredCapabilities);
        result.addAll(requiredCapabilities);
        result.addAll(capabilityMatches);
        result.addAll(skills);
        result.addAll(skillParameters);
        result.addAll(services);
        result.addAll(serviceInstances);
        result.addAll(deploymentContexts);
        result.addAll(clusterNodes);
        result.addAll(deploymentPolicies);
        result.addAll(networkZones);
        result.addAll(safetyClasses);
        result.addAll(informationModels);
        result.addAll(semanticConcepts);
        result.addAll(communicationBindings);
        result.addAll(dataConnectors);
        result.addAll(assets);
        result.addAll(sensors);
        result.addAll(processVariables);
            result.addAll(observations);
            result.addAll(automationMLEntities);
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns whether the model contains no entities at all.
     *
     * @return {@code true} when the model is empty
     */
    public boolean isEmpty() {
        return allEntities().isEmpty();
    }

    /**
     * Removes all entities from the model.
     */
    public void clear() {
        stakeholders.clear();
        useCases.clear();
        requirements.clear();
        requirementParameters.clear();
        requirementConstraints.clear();
        verificationCriteria.clear();
        capabilities.clear();
        offeredCapabilities.clear();
        requiredCapabilities.clear();
        capabilityMatches.clear();
        skills.clear();
        skillParameters.clear();
        services.clear();
        serviceInstances.clear();
        deploymentContexts.clear();
        clusterNodes.clear();
        deploymentPolicies.clear();
        networkZones.clear();
        safetyClasses.clear();
        informationModels.clear();
        semanticConcepts.clear();
        communicationBindings.clear();
        dataConnectors.clear();
        assets.clear();
        sensors.clear();
        processVariables.clear();
        observations.clear();
    }

    /**
     * Returns a normalized class name usable as a key (lowercased, punctuation stripped).
     *
     * @param className the ontology class name
     * @return the normalized key
     */
    public static String keyOf(String className) {
        return className.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static <T extends CanonicalEntity> void addUnique(List<T> list, T entity) {
        boolean exists = list.stream().anyMatch(e -> e.id().equals(entity.id()));
        if (!exists) {
            list.add(entity);
        }
    }

    private static <T extends CanonicalEntity> void mergeUnique(List<T> target, List<T> source) {
        for (T entity : source) {
            addUnique(target, entity);
        }
    }
}
