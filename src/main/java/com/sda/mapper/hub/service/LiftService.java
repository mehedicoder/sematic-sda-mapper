package com.sda.mapper.hub.service;

import com.sda.mapper.hub.model.Asset;
import com.sda.mapper.hub.model.CanonicalAssetModel;
import com.sda.mapper.hub.model.CanonicalEntity;
import com.sda.mapper.hub.model.InformationModel;
import com.sda.mapper.hub.model.ProcessVariable;
import com.sda.mapper.hub.model.SemanticConcept;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * LIFT stage: derives higher-level canonical entities (semantic concepts and
 * information models) from the parsed asset data, e.g. a semantic concept per
 * exposed node and an information model per uploaded asset.
 */
@Service
public class LiftService {

    /**
     * Derives additional canonical entities from the given model without
     * mutating it. Derived entities are explicit (not flagged as inferred)
     * because they are directly declared by the uploaded data.
     *
     * @param model the parsed canonical model
     * @return the derived entities
     */
    public List<CanonicalEntity> lift(CanonicalAssetModel model) {
        List<CanonicalEntity> lifted = new ArrayList<>();
        Set<String> concepts = new LinkedHashSet<>();
        for (ProcessVariable variable : model.processVariables()) {
            String nodeId = variable.nodeId();
            if (nodeId != null && !nodeId.isBlank()) {
                concepts.add(conceptName(nodeId));
            }
        }
        for (String concept : concepts) {
            String iri = "http://sda-platform.org/concept/" + concept;
            lifted.add(new SemanticConcept("SC-" + concept, concept,
                "Lifted from exposed node " + concept, iri));
        }
        for (Asset asset : model.assets()) {
            lifted.add(new InformationModel("IM-" + asset.id(),
                asset.name() + " information model",
                "Lifted from asset " + asset.name(), "OPC UA"));
        }
        return lifted;
    }

    private static String conceptName(String nodeId) {
        int lastDot = nodeId.lastIndexOf('.');
        String tail = lastDot >= 0 ? nodeId.substring(lastDot + 1) : nodeId;
        int lastSep = tail.lastIndexOf('=');
        return (lastSep >= 0 ? tail.substring(lastSep + 1) : tail).trim();
    }
}
