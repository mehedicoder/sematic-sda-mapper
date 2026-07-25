package com.sda.mapper.imports;

/**
 * Supported file types for multi-file semantic import.
 */
public enum FileType {
    MTP_AML("MTP AML"),
    OPC_UA_DATASHEET("OPC UA Server Datasheet"),
    AAS("AAS Submodel"),
    AASX("AASX Package"),
    OPC_UA_NODESET("OPC UA NodeSet"),
    RDF_TURTLE("RDF/Turtle"),
    JSON_LD("JSON-LD"),
    YAML("YAML"),
    UNKNOWN("Unknown");

    private final String displayName;

    FileType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
