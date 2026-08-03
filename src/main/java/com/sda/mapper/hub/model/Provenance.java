package com.sda.mapper.hub.model;

/**
 * Provenance of a canonical entity: the source file, the parser origin and the
 * pipeline stage that produced it.
 *
 * @param fileName name of the uploaded source file
 * @param origin   parser/format origin of the entity
 * @param stage    pipeline stage that produced the entity
 */
public record Provenance(String fileName, EntityOrigin origin, PipelineStage stage) {

    /**
     * Returns a human-readable provenance line.
     *
     * @return e.g. {@code "Found in pump-aas.json via AAS Parser → LIFT"}
     */
    public String describe() {
        return "Found in " + fileName + " via " + origin.label() + " \u2192 " + stage.label();
    }
}
