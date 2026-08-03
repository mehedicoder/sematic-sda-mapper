package com.sda.mapper.hub.model;

/**
 * Coverage status of an ontology class within the mapping hub.
 *
 * <p>Statuses form a hierarchy from strongest to weakest:
 * {@code PRESENT} &gt; {@code PARTIAL} &gt; {@code INFERRED} &gt; {@code MISSING}.</p>
 */
public enum CoverageStatus {
    /** At least one explicit entity for the class was uploaded and is complete. */
    PRESENT,
    /** The class is only covered by entities derived by the inference stage. */
    INFERRED,
    /** Explicit entities exist but required fields are missing or blank. */
    PARTIAL,
    /** No entity of this class is known. */
    MISSING
}
