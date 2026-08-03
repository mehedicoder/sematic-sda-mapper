package com.sda.mapper.hub.model;

/**
 * The five pipeline stages of the semantic mapping hub:
 * Parse &rarr; Lift &rarr; Infer &rarr; Match &rarr; Manifest.
 */
public enum PipelineStage {
    PARSE("Parse"),
    LIFT("Lift"),
    INFER("Infer"),
    MATCH("Match"),
    MANIFEST("Manifest");

    private final String label;

    PipelineStage(String label) {
        this.label = label;
    }

    /**
     * Returns the human-readable stage label shown in the pipeline stepper.
     *
     * @return the stage label
     */
    public String label() {
        return label;
    }
}
