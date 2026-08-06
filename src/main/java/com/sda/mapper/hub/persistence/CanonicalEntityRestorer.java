package com.sda.mapper.hub.persistence;

import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.Capability;
import com.sda.mapper.hub.model.CapabilityMatch;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.ClusterNode;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.DataConnector;
import com.sda.mapper.hub.model.DeploymentContext;
import com.sda.mapper.hub.model.DeploymentPolicy;
import com.sda.mapper.hub.model.InformationModel;
import com.sda.mapper.hub.model.NetworkZone;
import com.sda.mapper.hub.model.Observation;
import com.sda.mapper.hub.model.OfferedCapability;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.RequiredCapability;
import com.sda.mapper.hub.model.Requirement;
import com.sda.mapper.hub.model.RequirementConstraint;
import com.sda.mapper.hub.model.RequirementParameter;
import com.sda.mapper.hub.model.SafetyClass;
import com.sda.mapper.hub.model.SemanticConcept;
import com.sda.mapper.hub.model.Sensor;
import com.sda.mapper.hub.model.Service;
import com.sda.mapper.hub.model.ServiceInstance;
import com.sda.mapper.hub.model.Skill;
import com.sda.mapper.hub.model.SkillParameter;
import com.sda.mapper.hub.model.Stakeholder;
import com.sda.mapper.hub.model.UseCase;
import com.sda.mapper.hub.model.VerificationCriterion;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.Optional;

/**
 * Reconstructs typed canonical records from persisted {@code canonical_entity}
 * rows. The record component names of every canonical entity match the stored
 * attribute keys, so a single reflective pass rebuilds the exact typed record.
 */
@Component
public class CanonicalEntityRestorer {

    /**
     * Rebuilds the typed canonical record for a persisted row.
     *
     * @param className      ontology class name, e.g. {@code "Skill"}
     * @param canonicalId    stable entity id
     * @param name           display name
     * @param sourceFileName source file, may be {@code null}
     * @param inferred       whether the entity was inferred
     * @param attributes     persisted attribute map
     * @return the reconstructed canonical entity
     */
    public CanonicalEntity restore(String className, String canonicalId, String name,
                                   String sourceFileName, boolean inferred,
                                   Map<String, Object> attributes) {
        Class<?> recordClass = recordClassFor(className);
        RecordComponent[] components = recordClass.getRecordComponents();
        Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            args[i] = resolveComponent(components[i], canonicalId, name, sourceFileName, inferred, attributes);
        }
        try {
            return (CanonicalEntity) recordClass.getConstructors()[0].newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to restore canonical entity of class " + className, e);
        }
    }

    private static Object resolveComponent(RecordComponent component, String canonicalId, String name,
                                           String sourceFileName, boolean inferred,
                                           Map<String, Object> attributes) {
        return switch (component.getName()) {
            case "id" -> canonicalId;
            case "name" -> name;
            case "sourceFileName" -> sourceFileName == null
                ? Optional.empty()
                : Optional.of(sourceFileName);
            case "inferred" -> inferred;
            default -> {
                Object value = attributes.get(component.getName());
                yield value == null ? "" : String.valueOf(value);
            }
        };
    }

    private static Class<?> recordClassFor(String className) {
        return switch (className) {
            case "Stakeholder" -> Stakeholder.class;
            case "UseCase" -> UseCase.class;
            case "Requirement" -> Requirement.class;
            case "RequirementParameter" -> RequirementParameter.class;
            case "RequirementConstraint" -> RequirementConstraint.class;
            case "VerificationCriterion" -> VerificationCriterion.class;
            case "Capability" -> Capability.class;
            case "OfferedCapability" -> OfferedCapability.class;
            case "RequiredCapability" -> RequiredCapability.class;
            case "CapabilityMatch" -> CapabilityMatch.class;
            case "Skill" -> Skill.class;
            case "SkillParameter" -> SkillParameter.class;
            case "Service" -> Service.class;
            case "ServiceInstance" -> ServiceInstance.class;
            case "DeploymentContext" -> DeploymentContext.class;
            case "ClusterNode" -> ClusterNode.class;
            case "DeploymentPolicy" -> DeploymentPolicy.class;
            case "NetworkZone" -> NetworkZone.class;
            case "SafetyClass" -> SafetyClass.class;
            case "InformationModel" -> InformationModel.class;
            case "SemanticConcept" -> SemanticConcept.class;
            case "CommunicationBinding" -> CommunicationBinding.class;
            case "DataConnector" -> DataConnector.class;
            case "Asset" -> Asset.class;
            case "Sensor" -> Sensor.class;
            case "ProcessVariable" -> ProcessVariable.class;
            case "Observation" -> Observation.class;
            default -> throw new IllegalArgumentException(
                "No canonical record for ontology class: " + className);
        };
    }
}
