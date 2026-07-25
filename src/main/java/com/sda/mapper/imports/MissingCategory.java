package com.sda.mapper.imports;

/**
 * Categories of missing semantic information detected during import analysis.
 */
public enum MissingCategory {
    MISSING_COMMUNICATION_BINDING("Service has no CommunicationBinding", "CommunicationBinding"),
    MISSING_CAPABILITY("Service has no Capability mapping", "Capability"),
    MISSING_DEPLOYMENT_CONTEXT("No DeploymentContext with SIL, zone, or determinism", "DeploymentContext"),
    MISSING_SEMANTIC_ID("Parameter without IEC CDD IRDI identifier", "Semantic ID"),
    MISSING_ENGINEERING_UNIT("Value without UCUM engineering unit", "Engineering Unit"),
    MISSING_DEVICE_METADATA("Device without vendor, model, or version", "Device Metadata"),
    MISSING_DIGITAL_TWIN("Device without digital twin association", "Digital Twin"),
    DUPLICATE_ASSET("Asset already exists in ontology", "Duplicate Asset");

    private final String description;
    private final String displayName;

    MissingCategory(String description, String displayName) {
        this.description = description;
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }
}
