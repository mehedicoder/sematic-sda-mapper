package com.sda.mapper.model;

/**
 * Immutable descriptor of a datatype property declared on an ontology class
 * in the canonical CSS-SDA TBox.
 *
 * @param name      property name as declared in the ontology (e.g. {@code "stakeholderName"})
 * @param className class the property is declared on (its {@code rdfs:domain})
 * @param xsdType   XML Schema range of the property (e.g. {@code "xsd:string"})
 */
public record OntologyDatatypeProperty(String name, String className, String xsdType) {
}
