package com.sda.mapper.hub.model;

import java.util.Map;
import java.util.Optional;

/**
 * A single canonical entity in the semantic mapping hub model.
 *
 * <p>Every canonical entity corresponds to one class of the canonical CSS-SDA
 * TBox and carries provenance (source file, explicit vs inferred) plus a
 * uniform attribute map used by the detail panel and the manifest export.</p>
 */
public interface CanonicalEntity {

    /**
     * Returns the ontology class name this entity belongs to, e.g. {@code "Skill"}.
     *
     * @return the class name
     */
    String className();

    /**
     * Returns a stable identifier unique within the class, e.g. a node id or skill id.
     *
     * @return the identifier
     */
    String id();

    /**
     * Returns the display name of the entity.
     *
     * @return the name
     */
    String name();

    /**
     * Returns the name of the file this entity was lifted from, if known.
     *
     * @return the source file name
     */
    Optional<String> sourceFileName();

    /**
     * Returns whether this entity was derived by the inference stage rather than
     * lifted from an uploaded file or entered manually.
     *
     * @return {@code true} for inferred entities
     */
    boolean inferred();

    /**
     * Returns the domain attributes of this entity for display and export.
     *
     * @return an insertion-ordered attribute map
     */
    Map<String, Object> attributes();
}
