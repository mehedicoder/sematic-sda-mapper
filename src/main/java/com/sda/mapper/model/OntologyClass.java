package com.sda.mapper.model;

/**
 * Immutable descriptor of an ontology class from the canonical CSS-SDA TBox.
 *
 * @param code    diagram numbering shown on the node (e.g. {@code "1.3"}, {@code "2b.4"})
 * @param name    class name as declared in the ontology (e.g. {@code "Requirement"})
 * @param layerId identifier of the ontology layer this class belongs to
 */
public record OntologyClass(String code, String name, String layerId) {
}
