package com.sda.mapper.hub.model;

import java.util.List;

/**
 * A gap that blocks semantic completeness: an ontology class is missing while
 * at least one core relation ties it to an already covered class.
 *
 * @param className the missing ontology class name
 * @param layerId   the layer the class belongs to
 * @param reasons   human-readable reasons, e.g. {@code "UseCaseRequirement mapsToCapability Capability"}
 */
public record BlockingGap(String className, String layerId, List<String> reasons) {
}
