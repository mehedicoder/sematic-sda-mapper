package com.sda.mapper.hub.model;

/**
 * Origin of a canonical entity within the mapping hub, used for provenance display.
 */
public enum EntityOrigin {
    PARSED("Parsed from uploaded file"),
    LIFTED("Lifted into CSS-SDA vocabulary"),
    INFERRED("Inferred by semantic pipeline"),
    IMPORTED("Imported"),
    AAS("AAS Parser"),
    MTP("MTP Parser"),
    OPC_UA("OPC UA Parser"),
    AML("AutomationML Parser"),
    MANUAL("Manual Entry");

    private final String label;

    EntityOrigin(String label) {
        this.label = label;
    }

    /**
     * Returns the human-readable parser/origin label.
     *
     * @return the origin label
     */
    public String label() {
        return label;
    }
}
