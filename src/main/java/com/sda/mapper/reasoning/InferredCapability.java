package com.sda.mapper.reasoning;

/**
 * Represents a capability derived from the CSS-SDA capability chain in the
 * ontology, i.e. an {@code sda:Capability} instance linked to an
 * {@code sda:Skill} instance via {@code sda:realisedBy}.
 *
 * @param capabilityName   human-readable capability name
 * @param functionClass    derived function class (capability name)
 * @param engineeringUnit  engineering unit string (not populated by v2 ontology)
 * @param nominalRangeMin  lower bound of the nominal range (not populated by v2 ontology)
 * @param nominalRangeMax  upper bound of the nominal range (not populated by v2 ontology)
 * @param sourceSkill      URI local name of the source Skill instance
 */
public record InferredCapability(String capabilityName, String functionClass,
                                  String engineeringUnit, double nominalRangeMin,
                                  double nominalRangeMax, String sourceSkill) {
}
