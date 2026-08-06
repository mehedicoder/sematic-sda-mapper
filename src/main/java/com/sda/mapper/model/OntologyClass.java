package com.sda.mapper.model;

/**
 * Immutable descriptor of an ontology class from the canonical CSS-SDA TBox.
 *
 * @param code    diagram numbering shown on the node (e.g. {@code "1.3"}, {@code "2b.4"});
 *                derived subclasses inherit the numbering of their numbered parent
 * @param name    class name as declared in the ontology (e.g. {@code "UseCaseRequirement"})
 * @param layerId identifier of the ontology layer this class belongs to (inherited for derived classes)
 * @param derived true when the class is a derived subclass of a numbered parent
 * @param parent  name of the numbered parent class for derived classes, {@code null} otherwise
 */
public record OntologyClass(String code, String name, String layerId, boolean derived, String parent) {
}
