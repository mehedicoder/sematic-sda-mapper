package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.Capability;
import com.sda.mapper.hub.model.ClusterNode;
import com.sda.mapper.hub.model.CommunicationBinding;
import com.sda.mapper.hub.model.DeploymentContext;
import com.sda.mapper.hub.model.NetworkZone;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.Sensor;
import com.sda.mapper.hub.model.ServiceInstance;
import com.sda.mapper.hub.model.Skill;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * INFER stage: derives implicit canonical entities from the lifted model,
 * marking them as inferred. Sensors are derived per measured process variable,
 * capabilities per skill, and service instances, cluster nodes, network zones
 * and deployment contexts per communication binding.
 */
@Service
public class InferenceService {

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

        return inferred;
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
