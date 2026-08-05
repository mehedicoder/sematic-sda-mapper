package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.Capability;
import com.sda.mapper.hub.model.ClusterNode;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.DeploymentContext;
import com.sda.mapper.hub.model.NetworkZone;
import com.sda.mapper.hub.model.OfferedCapability;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.SafetyClass;
import com.sda.mapper.hub.model.Sensor;
import com.sda.mapper.hub.model.ServiceInstance;
import com.sda.mapper.hub.model.Skill;
import com.sda.mapper.hub.model.SkillParameter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * INFER stage: derives implicit canonical entities from the lifted model,
 * marking them as inferred. Sensors are derived per measured process variable,
 * capabilities per skill, and service instances, cluster nodes, network zones
 * and deployment contexts per communication binding.
 *
 * <p>Per the semantic mapping pipeline (paper, Step 4) every skill also yields
 * an inferred {@link OfferedCapability} whose functionClass, engineering unit
 * and nominal range are lifted from the skill's primary parameter (Axiom A4).
 * The capability is inferred - not parsed - and is consumed by the SPARQL
 * match stage.
 *
 * <p>Safety properties carried by other entities (for example a safety
 * integrity level or NE107 support) that are not covered by an explicit
 * {@link SafetyClass} yield an inferred safety class.
 */
@Service
public class InferenceService {

    /**
     * Attribute keys that carry safety-relevant values and may ground an
     * inferred {@link SafetyClass}.
     */
    private static final List<String> SAFETY_ATTRIBUTE_KEYS = List.of(
        "safetyIntegrityLevel", "safetyLevel", "safetyClass", "sil",
        "ne107", "NE107Support");

    /**
     * Derives inferred entities for the given model without mutating it.
     *
     * @param model the canonical model
     * @return the inferred entities
     */
    public List<CanonicalEntity> infer(CanonicalAssetModel model) {
        List<CanonicalEntity> inferred = new ArrayList<>();

        for (ProcessVariable variable : model.processVariables()) {
            inferred.add(new Sensor("SENSOR-" + variable.id(),
                variable.name() + " Sensor",
                variable.assetId(),
                variable.unit(),
                variable.sourceFileName(),
                true));
        }

        for (Skill skill : model.skills()) {
            inferred.add(new Capability("CAP-" + skill.id(),
                skill.name() + " Capability",
                "Realised by skill " + skill.name(),
                skill.sourceFileName(),
                true));
            SkillParameter primary = primaryParameter(model, skill.id());
            if (primary != null) {
                inferred.add(new OfferedCapability("OC-" + skill.id(),
                    skill.id(),
                    skill.name(),
                    primary.functionClass(),
                    primary.engineeringUnit(),
                    primary.nominalRangeMin(),
                    primary.nominalRangeMax(),
                    "",
                    "",
                    skill.sourceFileName(),
                    true));
            }
        }

        for (CommunicationBinding binding : model.communicationBindings()) {
            String host = endpointHost(binding.endpointUrl());
            inferred.add(new ServiceInstance("SI-" + binding.id(),
                binding.name(),
                binding.endpointUrl(),
                binding.sourceFileName(),
                true));
            inferred.add(new ClusterNode("NODE-" + host, host, host + ":4840", "edge",
                binding.sourceFileName(),
                true));
            inferred.add(new NetworkZone("ZONE-" + host, host + " zone",
                "Network zone of " + host, "",
                binding.sourceFileName(),
                true));
            inferred.add(new DeploymentContext("DC-" + binding.id(),
                binding.name() + " context",
                "Inferred from " + binding.endpointUrl(), "edge",
                binding.sourceFileName(),
                true));
        }

        inferred.addAll(inferSafetyClass(model));

        return inferred;
    }

    /**
     * Derives an inferred {@link SafetyClass} when safety properties are
     * present in the mapped data but no explicit safety class was parsed or
     * entered manually.
     *
     * @param model the canonical model
     * @return the inferred safety class entities, or an empty list
     */
    private static List<CanonicalEntity> inferSafetyClass(CanonicalAssetModel model) {
        if (!model.safetyClasses().isEmpty()) {
            return List.of();
        }
        return findSafetyLevel(model)
            .map(level -> new SafetyClass("SCL-inferred", "Inferred Safety Class",
                "Inferred from safety properties present in the mapped data",
                level, Optional.empty(), true))
            .map(List::<CanonicalEntity>of)
            .orElseGet(List::of);
    }

    /**
     * Searches every canonical entity attribute for a non-blank safety value,
     * normalising a bare {@code "true"} NE107 flag to {@code "NE107"}.
     *
     * @param model the canonical model
     * @return the first non-blank safety level, or empty when none exists
     */
    private static Optional<String> findSafetyLevel(CanonicalAssetModel model) {
        for (CanonicalEntity entity : model.allEntities()) {
            for (Map.Entry<String, Object> attribute : entity.attributes().entrySet()) {
                if (!SAFETY_ATTRIBUTE_KEYS.contains(attribute.getKey())) {
                    continue;
                }
                String value = attribute.getValue() == null ? "" : String.valueOf(attribute.getValue());
                if (value.isBlank() || "false".equalsIgnoreCase(value)) {
                    continue;
                }
                return Optional.of("true".equalsIgnoreCase(value) ? "NE107" : value);
            }
        }
        return Optional.empty();
    }

    /**
     * Picks the primary parameter of a skill for capability inference: the
     * first parameter carrying a functionClass, else the first one carrying an
     * engineering unit, else the first parameter of the skill.
     *
     * @param model   the canonical model
     * @param skillId the skill id
     * @return the primary parameter, or {@code null} when the skill has none
     */
    private static SkillParameter primaryParameter(CanonicalAssetModel model, String skillId) {
        List<SkillParameter> parameters = model.skillParameters().stream()
            .filter(p -> skillId.equals(p.skillId()))
            .toList();
        if (parameters.isEmpty()) {
            return null;
        }
        return parameters.stream()
            .filter(p -> p.functionClass() != null && !p.functionClass().isBlank())
            .findFirst()
            .or(() -> parameters.stream()
                .filter(p -> p.engineeringUnit() != null && !p.engineeringUnit().isBlank())
                .findFirst())
            .orElse(parameters.get(0));
    }

    /**
     * Extracts the host name from an endpoint URL, e.g. {@code pump01} from
     * {@code opc.tcp://pump01:4840}.
     *
     * @param endpointUrl the endpoint URL
     * @return the host name, or {@code "unknown"} when it cannot be determined
     */
    public static String endpointHost(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return "unknown";
        }
        String tail = endpointUrl;
        int scheme = endpointUrl.indexOf("://");
        if (scheme >= 0) {
            tail = endpointUrl.substring(scheme + 3);
        }
        int slash = tail.indexOf('/');
        if (slash >= 0) {
            tail = tail.substring(0, slash);
        }
        int colon = tail.indexOf(':');
        String host = colon >= 0 ? tail.substring(0, colon) : tail;
        return host.isBlank() ? "unknown" : host;
    }
}
