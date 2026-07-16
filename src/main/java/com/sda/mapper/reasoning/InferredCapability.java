package com.sda.mapper.reasoning;

/**
 * Represents a capability inferred from Skill parameters in the CSS-SDA ontology.
 * Inferred when a {@code css:Skill} instance has {@code sda:engineeringUnit},
 * {@code sda:nominalRange_min}, and {@code sda:nominalRange_max} data properties.
 *
 * @param capabilityName   human-readable capability name
 * @param functionClass    derived function class (e.g. capabilityName of linked OfferedCapability)
 * @param engineeringUnit  engineering unit string (e.g. "l/min", "°C")
 * @param nominalRangeMin  lower bound of the nominal range
 * @param nominalRangeMax  upper bound of the nominal range
 * @param sourceSkill      URI local name of the source Skill instance
 */
public record InferredCapability(String capabilityName, String functionClass,
                                  String engineeringUnit, double nominalRangeMin,
                                  double nominalRangeMax, String sourceSkill) {
}
