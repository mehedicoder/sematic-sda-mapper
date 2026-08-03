package com.sda.mapper.model;

/**
 * Immutable descriptor of a relation between two ontology classes.
 *
 * @param from    name of the source class (domain)
 * @param to      name of the target class (range)
 * @param label   object property name as declared in the ontology
 * @param kind    legend classification: {@code "core"} (R1–R3 / R-Deploy chain) or {@code "supporting"}
 * @param inverse name of the inverse object property, or {@code null} if none is declared
 */
public record OntologyRelation(String from, String to, String label, String kind, String inverse) {
}
