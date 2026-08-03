package com.sda.mapper.hub.model;

/**
 * Supported asset description file formats for the mapping hub.
 *
 * <p>JSON and XML variants of the same standard share an {@link EntityOrigin}
 * but are detected and parsed independently.</p>
 */
public enum AssetFileFormat {
    AAS_XML("AAS XML", "xml", "AAS Parser"),
    AAS_JSON("AAS JSON", "json", "AAS Parser"),
    MTP_AML("MTP AML", "aml", "MTP Parser"),
    OPCUA_JSON("OPC UA JSON", "json", "OPC UA Parser");

    private final String label;
    private final String extension;
    private final String parserName;

    AssetFileFormat(String label, String extension, String parserName) {
        this.label = label;
        this.extension = extension;
        this.parserName = parserName;
    }

    /**
     * Returns the human-readable format label.
     *
     * @return the format label
     */
    public String label() {
        return label;
    }

    /**
     * Returns the file extension of this format without a leading dot.
     *
     * @return the extension
     */
    public String extension() {
        return extension;
    }

    /**
     * Returns the parser name that handles this format.
     *
     * @return the parser name
     */
    public String parserName() {
        return parserName;
    }

    /**
     * Returns the origin of entities lifted from this format.
     *
     * @return the entity origin
     */
    public EntityOrigin origin() {
        return switch (this) {
            case AAS_XML, AAS_JSON -> EntityOrigin.AAS;
            case MTP_AML -> EntityOrigin.MTP;
            case OPCUA_JSON -> EntityOrigin.OPC_UA;
        };
    }
}
