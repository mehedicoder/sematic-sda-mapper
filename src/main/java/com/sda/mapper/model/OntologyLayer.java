package com.sda.mapper.model;

/**
 * Immutable descriptor of an ontology layer in the CSS-SDA hierarchy.
 *
 * @param id       stable layer identifier (e.g. {@code "L1"}, {@code "L2b"})
 * @param label    human-readable layer label shown in the UI
 * @param colorHex CSS color used to tint layer-specific nodes and pills
 * @param cssKey   CSS class suffix for layer-specific theming (e.g. {@code "l2b"})
 */
public record OntologyLayer(String id, String label, String colorHex, String cssKey) {
}
