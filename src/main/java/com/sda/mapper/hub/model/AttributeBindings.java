package com.sda.mapper.hub.model;

import java.util.Map;

/**
 * Maps canonical CSS-SDA ontology datatype property names to the Mapping Hub
 * attribute vocabulary used by the typed canonical records
 * ({@link CanonicalEntity#attributes()}), the manual-entry forms and the
 * coverage computation. Ontology properties without an explicit entry keep
 * their ontology name as the hub attribute key.
 *
 * <p>The hub pipeline consumes ontology property names (parsers, manual entry
 * and inference write them) but the typed records expose a small set of
 * familiar keys such as {@code "name"}. This table is the single projection
 * between the two so neither the ontology nor the hub can drift silently.</p>
 */
public final class AttributeBindings {

    private static final Map<String, String> ONTOLOGY_TO_HUB = Map.ofEntries(
        Map.entry("stakeholderName", "name"),
        Map.entry("useCaseName", "name"),
        Map.entry("title", "name"),
        Map.entry("parameterName", "name"),
        Map.entry("capabilityName", "name"),
        Map.entry("skillName", "name"),
        Map.entry("paramName", "name"),
        Map.entry("serviceName", "name"),
        Map.entry("instanceID", "name"),
        Map.entry("contextID", "name"),
        Map.entry("nodeID", "name"),
        Map.entry("policyID", "name"),
        Map.entry("zoneName", "name"),
        Map.entry("classID", "name"),
        Map.entry("modelName", "name"),
        Map.entry("conceptName", "name"),
        Map.entry("connectorID", "name"),
        Map.entry("assetName", "name"),
        Map.entry("sensorID", "name"),
        Map.entry("variableName", "name"),
        Map.entry("contactInfo", "contact"),
        Map.entry("criterionDescription", "criterion"),
        Map.entry("testMethod", "method"),
        Map.entry("deploymentMethod", "runtime"),
        Map.entry("safetyZone", "zone"),
        Map.entry("marginDeviceRole", "role"),
        Map.entry("iec62443Level", "securityLevel"),
        Map.entry("sensorType", "type"),
        Map.entry("paramUnit", "engineeringUnit"),
        Map.entry("engineeringUnit", "paramUnit"),
        Map.entry("semanticIRI", "iri"),
        Map.entry("bindingEndpointUri", "endpointUrl"),
        Map.entry("connectorSemanticIRI", "nodeId"),
        Map.entry("variableID", "nodeId"),
        Map.entry("observationID", "processVariableId"),
        Map.entry("deployStatus", "status"),
        Map.entry("safetyIntegrityLevel", "level")
    );

    private AttributeBindings() {
    }

    /**
     * Translates an ontology datatype property name to the hub attribute key.
     *
     * @param ontologyProperty the ontology property name
     * @return the hub attribute key, or the property name itself when unmapped
     */
    public static String hubKey(String ontologyProperty) {
        return ONTOLOGY_TO_HUB.getOrDefault(ontologyProperty, ontologyProperty);
    }
}
